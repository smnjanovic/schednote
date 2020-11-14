package com.moriak.schednote.database

import android.database.Cursor
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.database.data.*
import com.moriak.schednote.database.tables.*
import com.moriak.schednote.database.tables.Colors.A
import com.moriak.schednote.database.tables.Colors.H
import com.moriak.schednote.database.tables.Colors.L
import com.moriak.schednote.database.tables.Colors.S
import com.moriak.schednote.database.tables.LessonTypes.TYPE
import com.moriak.schednote.database.tables.LessonTypes.TYPE_NAME
import com.moriak.schednote.database.tables.Lessons.DAY
import com.moriak.schednote.database.tables.Lessons.DUR
import com.moriak.schednote.database.tables.Lessons.LES_ID
import com.moriak.schednote.database.tables.Lessons.ROOM
import com.moriak.schednote.database.tables.Lessons.START
import com.moriak.schednote.database.tables.Lessons.WEEK_ODDITY
import com.moriak.schednote.database.tables.Notes.DEADLINE
import com.moriak.schednote.database.tables.Notes.NOTE
import com.moriak.schednote.database.tables.Notes.NOTE_ID
import com.moriak.schednote.database.tables.Schedule.BREAK_DUR
import com.moriak.schednote.database.tables.Schedule.LESSON_DUR
import com.moriak.schednote.database.tables.Schedule.ORDER
import com.moriak.schednote.database.tables.Subjects.ABB
import com.moriak.schednote.database.tables.Subjects.SUB_ID
import com.moriak.schednote.design.Palette
import com.moriak.schednote.notifications.NoteReminder
import com.moriak.schednote.other.Day
import com.moriak.schednote.other.TimeCategory.*
import com.moriak.schednote.settings.LessonTimeFormat
import com.moriak.schednote.settings.Prefs
import com.moriak.schednote.settings.Regularity
import com.moriak.schednote.settings.WorkWeek
import com.moriak.schednote.widgets.NoteWidget
import com.moriak.schednote.widgets.ScheduleWidget
import java.util.*
import kotlin.concurrent.thread
import com.moriak.schednote.database.tables.Subjects.NAME as SUB_NAME

/**
 * Trieda manipuluje s databazou SQLite. (prijem, tvorba, aktualizacia a odstranenie dat)
 */

class Database : DBControl() {

    private val now get() = System.currentTimeMillis()

    private val Regularity.sql: String
        get() = "($WEEK_ODDITY IS NULL" + (odd?.let { " OR $WEEK_ODDITY = ${if (it) 1 else 0})" }
            ?: ")")

    private val WorkWeek.sql: String
        get() {
            val first = days.first().value
            val last = days.last().value
            return when (days.size) {
                in 1..5 -> DAY + " " + if (first < last) "BETWEEN $first AND $last" else "NOT BETWEEN ${last + 1} AND ${first - 1}"
                6 -> "$DAY != ${if (first > last) last + 1 else if (first == 1) 7 else 1}"
                else -> "1 = 1"
            }
        }

    private val Calendar.now: Long get() = System.currentTimeMillis().also { timeInMillis = it }
    private val Calendar.nextMidnight: Long
        get() {
            add(Calendar.DAY_OF_YEAR, 1)
            set(get(Calendar.YEAR), get(Calendar.MONTH), get(Calendar.DAY_OF_MONTH), 0, 0, 0)
            return timeInMillis - timeInMillis % 1000
        }
    private val Calendar.nextWeek: Long get() = also { add(Calendar.DAY_OF_YEAR, 7) }.timeInMillis
    private val Calendar.today: LongRange get() = now until nextMidnight
    private val Calendar.tomorrow: LongRange get() = now.let { nextMidnight until nextMidnight }
    private val Calendar.thisWeek: LongRange get() = now until nextWeek
    private val LongRange.sql get() = "BETWEEN $first AND $last"

    private fun Cursor.getIntOrNull(column: Int) = if (isNull(column)) null else getInt(column)
    private fun Cursor.getLongOrNull(column: Int) = if (isNull(column)) null else getLong(column)

    private fun trimAndCut(s: String, l: Int) =
        s.trim().let { if (it.length > l) it.substring(0, l) else it }

    // predmety

    fun subject(id: Long) =
        one("SELECT $SUB_ID, $ABB, $SUB_NAME FROM $Subjects WHERE $SUB_ID = $id;") {
            Subject(getLong(0), getString(1), getString(2))
        }

    fun subject(abb: String) = one(
        "SELECT $SUB_ID, $ABB, $SUB_NAME FROM $Subjects WHERE $ABB LIKE ?;",
        abb.trim().toUpperCase(Locale.ROOT)
    ) {
        Subject(getLong(0), getString(1), getString(2))
    }

    fun subjectIndex(id: Long) = one(
        """
        SELECT CASE WHEN EXISTS (SELECT 1 FROM $Subjects WHERE $SUB_ID = $id) THEN COUNT(*) ELSE -1 END
        FROM $Subjects WHERE $ABB < (SELECT $ABB FROM $Subjects WHERE $SUB_ID = $id);
    """
    ) { getInt(0) } ?: -1

    fun subjects() = all("SELECT $SUB_ID, $ABB, $SUB_NAME FROM $Subjects ORDER BY $ABB") {
        Subject(getLong(0), getString(1), getString(2))
    }

    fun addSubject(abb: String, name: String): Long = insert(Subjects, values {
        put(ABB, trimAndCut(abb, Subject.abb_limit))
        put(SUB_NAME, trimAndCut(name, Subject.name_limit))
    })

    fun editSubject(id: Long, abb: String, name: String): Int {
        val count = update(Subjects, values {
            put(ABB, trimAndCut(abb, Subject.abb_limit))
            put(SUB_NAME, trimAndCut(name, Subject.name_limit))
        }, "$SUB_ID = $id")
        if (count > 0) {
            ScheduleWidget.update()
            NoteWidget.update()

        }
        return count
    }

    fun deleteSubject(id: Long): Int {
        val count = delete(Subjects, "$SUB_ID = $id")
        if (count > 0) {
            ScheduleWidget.update()
            NoteWidget.update()
        }
        return count
    }

    fun hasSubjects() = one("SELECT 1 FROM $Subjects") { true } ?: false

    // casove bloky rozvrhu

    fun isScheduleSet() = one("""SELECT * FROM $Schedule;""") { true } ?: false

    fun timetable() = all(
        """
        SELECT a.$ORDER, a.$LESSON_DUR, a.$BREAK_DUR,
        COALESCE(SUM(b.$LESSON_DUR + b.$BREAK_DUR), 0) AS st,
        COALESCE(SUM(b.$LESSON_DUR + b.$BREAK_DUR), 0) + a.$LESSON_DUR AS en
        FROM $Schedule a LEFT JOIN $Schedule b ON b.$ORDER < a.$ORDER
        GROUP BY a.$ORDER
        ORDER BY a.$ORDER
        
    """
    ) {
        LessonData(getInt(0), getInt(1), getInt(2), getInt(3), getInt(4))
    }

    fun scheduleRange(workWeek: WorkWeek, regularity: Regularity) = one(
        """
        SELECT COALESCE(MIN($START), 0), COALESCE(MAX($START + $DUR), 1)
        FROM $Lessons
        WHERE ${workWeek.sql} AND ${regularity.sql}
    """
    ) { getInt(0) until getInt(1) }!!

    fun lessonStart(order: Int) = one(
        """
        SELECT COALESCE(SUM($LESSON_DUR + $BREAK_DUR),0)
        FROM $Schedule
        WHERE $ORDER < $order
    """
    ) { getInt(0) } ?: 0

    fun firstLessonStart(day: Day, regularity: Regularity): Int {
        val order =
            "SELECT MIN($START) FROM $Lessons WHERE $DAY = ${day.value} AND ${regularity.sql}"
        val start = "SELECT SUM($LESSON_DUR + $BREAK_DUR) FROM $Schedule WHERE $ORDER < ($order);"
        return Prefs.settings.earliestMinute + (one(start) { getInt(0) } ?: 0)
    }

    fun getBusyDays(reg: Regularity, workWeek: WorkWeek): TreeMap<Day, Boolean> {
        val busy = TreeMap<Day, Boolean>().apply {
            for (day in Day.values())
                put(day, false)
        }
        all("SELECT $DAY FROM $Lessons WHERE ${reg.sql} AND ${workWeek.sql} GROUP BY $DAY") {
            busy[Day[getInt(
                0
            )]] = true
        }
        return busy
    }

    fun lessonEnd(order: Int) = one(
        """
        SELECT start + dur FROM (
            (SELECT SUM($LESSON_DUR + $BREAK_DUR) AS start FROM $Schedule WHERE $ORDER < $order),
            (SELECT $LESSON_DUR AS dur FROM $Schedule WHERE $ORDER = $order)
        );
    """
    ) { getInt(0) } ?: 0

    fun scheduleRangeToMinuteRange(range: IntRange): IntRange? = one(
        """
        SELECT a, b + c FROM
        (SELECT COALESCE(SUM($LESSON_DUR + $BREAK_DUR), 0) AS a FROM $Schedule WHERE $ORDER < ${range.first}),
        (SELECT COALESCE(SUM($LESSON_DUR + $BREAK_DUR), 0) AS b FROM $Schedule WHERE $ORDER < ${range.last}),
        (SELECT $LESSON_DUR AS c FROM $Schedule WHERE $ORDER = ${range.last});
    """
    ) { getInt(0)..getInt(1) }

    fun scheduleDuration(except: Int = -1) = one(
        """
        SELECT SUM($LESSON_DUR + $BREAK_DUR)
        FROM $Schedule
        WHERE $ORDER != $except
    """
    ) { getInt(0) } ?: 0

    fun updateSchedule(id: Int, lessonDuration: Int, breakDuration: Int): Int {
        val result =
            if (scheduleDuration(id) + lessonDuration + breakDuration + Prefs.settings.earliestMinute >= 24 * 60) 0
            else update(
                Schedule,
                values { put(LESSON_DUR, lessonDuration); put(BREAK_DUR, breakDuration) },
                "$ORDER = $id"
            )
        if (Prefs.settings.lessonTimeFormat == LessonTimeFormat.START_TIME) ScheduleWidget.update()
        return result
    }

    fun removeFromSchedule(id: Int): Int {
        val count = delete(Schedule, "$ORDER = $id")
        ScheduleWidget.update()
        return count
    }

    fun insertIntoSchedule(lessonDuration: Int, breakDuration: Int): Int {
        val newId =
            if (scheduleDuration() + lessonDuration + breakDuration + Prefs.settings.earliestMinute >= 24 * 60) -1
            else insert(
                Schedule,
                values { put(LESSON_DUR, lessonDuration); put(BREAK_DUR, breakDuration) }).toInt()
        ScheduleWidget.update()
        return newId
    }

    // typy hodín

    fun hasLessonTypes() = one("SELECT 1 FROM $LessonTypes;") { true } ?: false

    fun lessonTypes() = all("SELECT $TYPE, $TYPE_NAME FROM $LessonTypes ORDER BY $TYPE_NAME;")
    { LessonType(getInt(0), getString(1)) }

    fun lessonType(id: Int) = one("SELECT $TYPE, $TYPE_NAME FROM $LessonTypes WHERE $TYPE = $id;")
    { LessonType(getInt(0), getString(1)) }

    fun joinLessonTypes(keptType: Int, lostType: Int) {
        update(Lessons, values { put(TYPE, keptType) }, "$TYPE = $lostType")
        delete(Colors, "$TYPE = $lostType")
        deleteLessonType(lostType)
        ScheduleWidget.update()
    }

    fun lessonTypeIndex(id: Int) = one(
        """
        SELECT CASE WHEN EXISTS(SELECT 1 FROM $LessonTypes WHERE $TYPE = $id) THEN COUNT(*) ELSE -1 END
        FROM $LessonTypes WHERE $TYPE_NAME < (SELECT $TYPE_NAME FROM $LessonTypes WHERE $TYPE = $id);
    """
    ) { getInt(0) } ?: -1

    fun insertLessonType(name: String) =
        insert(LessonTypes, values { put(TYPE_NAME, name) }).toInt()

    fun renameLessonType(id: Int, name: String) =
        update(LessonTypes, values { put(TYPE_NAME, name) }, "$TYPE = $id")

    fun deleteLessonType(id: Int): Int {
        val result = delete(LessonTypes, "$TYPE = $id")
        ScheduleWidget.update()
        return result
    }

    fun fullSchedule(workWeek: WorkWeek, regularity: Regularity): ArrayList<ScheduleEvent> {
        val range = scheduleRange(workWeek, regularity)
        val list: ArrayList<ScheduleEvent> = all(
            """
            SELECT a.$LES_ID, a.$WEEK_ODDITY, a.$DAY, a.$START, a.$START + a.$DUR, a.$TYPE, b.$SUB_ID, b.$ABB, b.$SUB_NAME, a.$ROOM
            FROM $Lessons a JOIN $Subjects b ON a.$SUB_ID = b.$SUB_ID
            WHERE ${
                regularity.sql.replace(
                    WEEK_ODDITY,
                    "a.$WEEK_ODDITY"
                )
            } AND ${workWeek.sql.replace(DAY, "a.$DAY")}
            ORDER BY CASE WHEN a.$DAY BETWEEN ${workWeek.days.first().value} AND 7 THEN 0 ELSE 1 END, a.$DAY, a.$START;
        """
        ) {
            val time = getInt(3) until getInt(4)
            val reg = Regularity[getIntOrNull(1)?.let { it == 1 }]
            val sub = Subject(getLong(6), getString(7), getString(8))
            val room = if (isNull(9)) null else getString(9)
            Lesson(getLong(0), reg, Day[getInt(2)], time, getInt(5), sub, room)
        }
        val days = workWeek.days
        for (i in (0..list.size).reversed()) {
            val l1 = if (i > 0) list[i - 1] else null
            val l2 = if (i < list.size) list[i] else null

            when {
                // v rozvrhu neexistuje ziadna hodina, tento cyklus sa vykonal naposledy
                l1 == null && l2 == null -> for (d in days) list.add(Free(regularity, d, 0..0))
                // jedná sa o poslednú hodinu. Je nutné medzi ňou a koncom rozvrhu vyplniť medzeru
                l1 != null && l2 == null -> {
                    if (l1.time.last < range.last) list.add(
                        Free(
                            regularity,
                            l1.day,
                            l1.time.last + 1..range.last
                        )
                    )
                    for (d in days.indexOf(l1.day) + 1 until days.size) list.add(
                        Free(
                            regularity,
                            days[d],
                            range
                        )
                    )
                }
                // jedná sa o prvu hodinu. Je nutné medzi zaciatkom rozvrhu a ňou vyplniť medzeru
                l1 == null && l2 != null -> {
                    if (l2.time.first > range.first) list.add(
                        0,
                        Free(regularity, l2.day, range.first until l2.time.first)
                    )
                    for (d in 0 until days.indexOf(l2.day)) list.add(
                        d,
                        Free(regularity, days[d], range)
                    )
                }
                // Odtiaľto už vždy existuje dvojica hodín. Dve po sebe iduce hodiny su kazda iny den. Vyplnit medzeru
                l1!!.day != l2!!.day -> {
                    val d1 = days.indexOf(l1.day)
                    val d2 = days.indexOf(l2.day)
                    if (range.first < l2.time.first) list.add(
                        i,
                        Free(regularity, l2.day, range.first until l2.time.first)
                    )
                    for (d in d1 + 1 until d2) list.add(i, Free(regularity, Day[d], range))
                    if (l1.time.last < range.last) list.add(
                        i,
                        Free(regularity, l1.day, l1.time.last + 1..range.last)
                    )
                }
                // Odtiaľto už je dvojica hodín v rovnaký deň. Medzi 2 hodinami je medzera
                l1.time.last + 1 < l2.time.first -> {
                    list.add(i, Free(regularity, l1.day, l1.time.last + 1 until l2.time.first))
                }
                // Hodiny nasleduju tesne po sebe, ale sú rovnaké! Zlúčiť!
                l1 is Lesson && l2 is Lesson && l1.sub == l2.sub && l1.room == l2.room && l1.type == l2.type -> {
                    val event: ScheduleEvent = Lesson(
                        -1L,
                        regularity,
                        l1.day,
                        l1.time.first..l2.time.last,
                        l1.type,
                        l1.sub,
                        l1.room
                    )
                    list.removeAt(i)
                    list.removeAt(i - 1)
                    list.add(i - 1, event)
                }
            }
        }
        return list
    }

    fun setLesson(
        id: Long,
        regularity: Regularity,
        day: Day,
        time: IntRange,
        type: Int,
        subject: Long,
        room: String?
    ): Long {
        val v = values {
            put(DAY, day.value)
            put(START, time.first)
            put(DUR, time.count())
            put(WEEK_ODDITY, regularity.odd)
            put(TYPE, type)
            put(SUB_ID, subject)
            put(ROOM, room)
        }
        val resId =
            if (id > 0 && update(Lessons, v, "$LES_ID = $id") > 0) id else insert(Lessons, v)
        ScheduleWidget.update()
        return resId
    }

    fun deleteLesson(id: Long): Int {
        val result = delete(Lessons, "$LES_ID = $id")
        ScheduleWidget.update()
        return result
    }

    fun clearSchedule(): Int {
        val result = delete(Lessons)
        ScheduleWidget.update()
        return result
    }

    fun clearSchedule(day: Day, clearTime: IntRange, regularity: Regularity): Int {
        val result = delete(
            Lessons, """
            $DAY = ${day.value} AND ${regularity.sql} AND (
                $START BETWEEN ${clearTime.first} AND ${clearTime.last}
                OR $START + $DUR - 1 BETWEEN ${clearTime.first} AND ${clearTime.last}
                OR ${clearTime.first} BETWEEN $START AND $START + $DUR - 1
            )
        """
        )
        ScheduleWidget.update()
        return result
    }

    fun color(id: Int, palette: Palette) {
        one("SELECT $A, $H, $S, $L FROM $Colors WHERE $TYPE = $id;") {
            palette.ahsl(getInt(0), getInt(1), getInt(2), getInt(3))
        } ?: palette.resourceColor(R.color.colorPrimary)
    }

    fun colors(treeMap: TreeMap<Int, Palette>? = null): TreeMap<Int, Palette> =
        (treeMap?.apply { clear() } ?: TreeMap<Int, Palette>()).also { map ->
            all("SELECT l.$TYPE, c.$A, c.$H, c.$S, c.$L FROM $LessonTypes l LEFT JOIN $Colors c ON c.$TYPE = l.$TYPE;") {
                map[getInt(0)] = if (isNull(1)) Palette.resource(R.color.colorPrimary)
                else Palette.ahsl(getInt(1), getInt(2), getInt(3), getInt(4))
                null
            }
        }


    fun setColor(type: Int, color: Palette): Int {
        val id = replace(Colors, values {
            put(TYPE, type)
            put(A, color.alpha)
            put(H, color.hue)
            put(S, color.saturation)
            put(L, color.luminance)
        }).toInt()
        ScheduleWidget.update()
        return id
    }

    private fun noteCategoryWhere(category: NoteCategory) = when (category) {
        ALL -> "1=1"
        TIMELESS -> "$DEADLINE IS NULL"
        LATE -> "$DEADLINE < $now"
        TODAY -> DEADLINE + " " + App.cal.today.sql
        TOMORROW -> DEADLINE + " " + App.cal.tomorrow.sql
        IN_WEEK -> DEADLINE + " " + App.cal.thisWeek.sql
        is Subject -> "$SUB_ID = ${category.id}"
        else -> "1 = 1"
    }

    fun enableNoteNotifications(isEnabled: Boolean = true) = thread {
        Prefs.notifications.reminderEnabled = isEnabled
        val notes = incomingDeadlines()
        for (note in notes)
            NoteReminder.editNotification(
                note.id,
                note.sub.abb,
                note.description,
                if (isEnabled) note.deadline else null
            )
    }.run()

    fun insertMultipleNotes(notes: ArrayList<Note>) {
        if (notes.isNotEmpty()) {
            transaction("INSERT INTO $Notes ($SUB_ID, $NOTE, $DEADLINE) VALUES (?, ?, ?)") {
                for (note in notes) {
                    bindLong(1, note.sub.id)
                    bindString(2, note.description)
                    note.deadline?.let { bindLong(3, note.deadline) } ?: bindNull(3)
                    if (executeInsert() > -1L)
                        NoteReminder.editNotification(
                            note.id,
                            note.sub.abb,
                            note.description,
                            note.deadline
                        )
                }
            }
        }
    }

    fun hasSimilarNote(note: Note) = one("""
        SELECT $SUB_ID, $NOTE, $DEADLINE
        FROM $Notes
        WHERE $SUB_ID = ${note.sub.id}
        AND UPPER(TRIM($NOTE)) LIKE UPPER(TRIM(?))
        AND ${note.deadline?.let { "$DEADLINE = $it" } ?: "$DEADLINE IS NULL"}
    """, note.description) { true } ?: false

    fun incomingDeadlines(): ArrayList<Note> {
        val map = TreeMap<Long, Subject>()
        return all(
            """
            SELECT n.$NOTE_ID, n.$SUB_ID, s.$ABB, s.$SUB_NAME, n.$NOTE, n.$DEADLINE
            FROM $Notes n JOIN $Subjects s ON n.$SUB_ID = s.$SUB_ID
            WHERE $DEADLINE IS NOT NULL AND $DEADLINE > $now;
        """
        ) {
            val subId = getLong(1)
            val sub =
                map[subId] ?: Subject(subId, getString(2), getString(3)).also { map[subId] = it }
            Note(getLong(0), sub, getString(4), getLongOrNull(5))
        }
    }

    fun notes(noteCategory: NoteCategory): ArrayList<Note> {
        val subjects = TreeMap<Long, Subject>()
        val where = noteCategoryWhere(noteCategory)
            .replace("(($SUB_ID)|($DEADLINE))".toRegex(), "n.$1")
        return all(
            """
            SELECT n.$NOTE_ID, n.$SUB_ID, s.$ABB, s.$SUB_NAME, n.$NOTE, n.$DEADLINE
            FROM $Notes n JOIN $Subjects s ON n.$SUB_ID = s.$SUB_ID
            WHERE $where
            ORDER BY CASE WHEN n.$DEADLINE IS NULL THEN 1 ELSE 0 END, n.$DEADLINE, n.$SUB_ID;
        """
        ) {
            val sub = subjects[getLong(1)]
                ?: Subject(getLong(1), getString(2), getString(3))
                    .also { s -> subjects[s.id] = s }
            Note(getLong(0), sub, getString(4), getLongOrNull(5))
        }
    }

    fun noteBelongsToCategory(id: Long, category: NoteCategory): Boolean =
        one("SELECT 1 FROM $Notes WHERE $NOTE_ID = $id AND ${noteCategoryWhere(category)}") { true }
            ?: false

    fun clearNotesOfCategory(category: NoteCategory): Int {
        val count = delete(Notes, noteCategoryWhere(category))
        if (count > 0) NoteWidget.update()
        return count
    }

    fun addNote(sub: Long, info: String, deadline: Long?): Long {
        val id = insert(Notes, values {
            put(SUB_ID, sub)
            put(NOTE, info)
            put(DEADLINE, deadline)
        })
        if (id > -1L) {
            NoteReminder.editNotification(id, subject(sub)!!.abb, info, deadline)
            NoteWidget.update()
        }
        return id
    }

    fun updateNote(id: Long, sub: Long, info: String, deadline: Long?): Int {
        val count = update(Notes, values {
            put(SUB_ID, sub)
            put(NOTE, info)
            put(DEADLINE, deadline)
        }, "$NOTE_ID = $id")
        if (count > 0) {
            NoteReminder.editNotification(id, subject(sub)!!.abb, info, deadline)
            NoteWidget.update()
        }
        return count
    }

    fun changeNoteDeadline(id: Long, deadline: Long?): Int {
        val note = one(
            """
            SELECT n.$SUB_ID, s.$ABB, s.$SUB_NAME, n.$NOTE
            FROM $Notes n JOIN $Subjects s ON n.$SUB_ID = s.$SUB_ID
            WHERE $NOTE_ID = $id;
        """
        ) {
            val sub = Subject(getLong(0), getString(1), getString(2))
            Note(id, sub, getString(3), deadline)
        } ?: return 0

        val count = update(Notes, values { put(DEADLINE, deadline) }, "$NOTE_ID = $id")
        if (count > 0) {
            NoteReminder.editNotification(id, note.sub.abb, note.description, deadline)
            NoteWidget.update()
        }
        return count
    }

    fun missedNotes(subject: Long) =
        one("SELECT COUNT(*) FROM $Notes WHERE $SUB_ID = $subject AND $DEADLINE IS NOT NULL AND $DEADLINE <= $now") {
            getInt(0)
        }!!

    fun incomingNotes(subject: Long) =
        one("SELECT COUNT(*) FROM $Notes WHERE $SUB_ID = $subject AND ($DEADLINE IS NULL OR $DEADLINE > ${now})") {
            getInt(0)
        }!!

    fun removeNote(id: Long): Int {
        val count = delete(Notes, "$NOTE_ID = $id")
        if (count > 0) {
            NoteReminder.editNotification(id, "", "", null)
            NoteWidget.update()
        }
        return count
    }

    fun detectNoteCategory(id: Long): NoteCategory =
        one("SELECT $DEADLINE, $SUB_ID FROM $Notes WHERE $NOTE_ID = $id") {
            val ms = getLongOrNull(0)
            when {
                ms == null -> TIMELESS
                ms < now -> LATE
                ms in App.cal.today -> TODAY
                ms in App.cal.tomorrow -> TOMORROW
                ms in App.cal.thisWeek -> IN_WEEK
                else -> subject(getLong(1))!!
            }
        } ?: ALL

    fun clearGarbageData(): Int {
        val lessons = delete(Lessons, "NOT(${Prefs.settings.workWeek.sql})")
        val regularity =
            if (Prefs.settings.dualWeekSchedule) 0 else delete(Lessons, "$WEEK_ODDITY IS NOT NULL")
        // na oneskorene poznamky uz upozornenie necaka
        val notes = delete(Notes, "$DEADLINE IS NOT NULL AND $DEADLINE <= $now")
        val subjects = delete(
            Subjects, """$SUB_ID NOT IN (
            SELECT $SUB_ID FROM $Lessons GROUP BY $SUB_ID
            UNION ALL SELECT $SUB_ID FROM $Notes GROUP BY $SUB_ID
        )"""
        )
        if (notes + subjects > 0) NoteWidget.update()
        return lessons + regularity + notes + subjects
    }

    fun scheduleNextWidgetUpdate() = one(
        "SELECT MIN($DEADLINE) FROM $Notes WHERE " +
                "$DEADLINE > ${System.currentTimeMillis()};"
    ) {
        getLongOrNull(0)?.coerceAtMost(App.now.nextMidnight + 60000)
    }
}