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
import com.moriak.schednote.database.tables.LessonData.LES_ID
import com.moriak.schednote.database.tables.LessonData.ROOM
import com.moriak.schednote.database.tables.LessonTypes.TYPE
import com.moriak.schednote.database.tables.LessonTypes.TYPE_NAME
import com.moriak.schednote.database.tables.Notes.DEADLINE
import com.moriak.schednote.database.tables.Notes.NOTE
import com.moriak.schednote.database.tables.Notes.NOTE_ID
import com.moriak.schednote.database.tables.Schedule.DAY
import com.moriak.schednote.database.tables.Schedule.REG
import com.moriak.schednote.database.tables.ScheduleRange.BREAK_DUR
import com.moriak.schednote.database.tables.ScheduleRange.LESSON_DUR
import com.moriak.schednote.database.tables.ScheduleRange.LES_NO
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

    private val Regularity.sql: String get() = sql()
    private fun Regularity.sql(col: String = REG): String {
        return when (this) {
            Regularity.EVEN -> "$col != 1"
            Regularity.ODD -> "$col != 0"
            Regularity.EVERY -> "$col = 2"
        }
    }

    private val WorkWeek.sql: String get() = sql()
    private fun WorkWeek.sql(col: String = DAY) =
        "$col NOT IN (${weekend.map { it.value }.joinToString()})"

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

    private fun Cursor.getLongOrNull(column: Int) = if (isNull(column)) null else getLong(column)

    private fun trimAndCut(s: String, l: Int) = s.trim()
        .let { if (it.length > l) it.substring(0, l) else it }

    // predmety

    /**
     * Načítanie predmetu z databázy
     * @param id ID predmetu
     * @return [Subject] Vráti null, ak predmet s takým ID neexistuje
     */
    fun subject(id: Long): Subject? {
        val sql = "SELECT $SUB_ID, $ABB, $SUB_NAME FROM $Subjects WHERE $SUB_ID = $id"
        return one(sql) { Subject(getLong(0), getString(1), getString(2)) }
    }

    /**
     * Načítanie predmetu z databázy
     * @param abb Skratka predmetu
     * @return [Subject] Vráti null, ak predmet s takou skratkou neexistuje
     */
    fun subject(abb: String): Subject? {
        val sql = "SELECT $SUB_ID, $ABB, $SUB_NAME FROM $Subjects WHERE $ABB LIKE ?"
        return one(sql, abb.trim().toUpperCase(Locale.ROOT)) {
            Subject(getLong(0), getString(1), getString(2))
        }
    }

    /**
     * Získanie abecedného umiestnenia predmetu
     * @param id ID predmetu
     * @return Vráti index, v ktorom sa má predmet nachádzať alebo -1, ak taký predmet neexistuje
     */
    fun subjectIndex(id: Long) = one(
        """
        SELECT CASE WHEN EXISTS (SELECT 1 FROM $Subjects WHERE $SUB_ID = $id) THEN COUNT(*) ELSE -1 END
        FROM $Subjects WHERE $ABB < (SELECT $ABB FROM $Subjects WHERE $SUB_ID = $id);
    """
    ) { getInt(0) } ?: -1

    /**
     * Získanie abecedného zoznamu všetkých predmetov
     * @return výsledný zoznam
     */
    fun subjects() = all("SELECT $SUB_ID, $ABB, $SUB_NAME FROM $Subjects ORDER BY $ABB") {
        Subject(getLong(0), getString(1), getString(2))
    }

    /**
     * Pridanie predmetu do databázy
     * @param abb Skratka predmetu
     * @param name Celý názov predmetu
     * @return Vráti id nového predmetu alebo -1, ak vloženie neprebehlo úspešne
     */
    fun addSubject(abb: String, name: String): Long = insert(Subjects, values {
        put(ABB, trimAndCut(abb, Subject.abb_limit))
        put(SUB_NAME, trimAndCut(name, Subject.name_limit))
    })

    /**
     * Úprava predmetu v databázy
     * @param abb Nová skratka predmetu
     * @param name Nový názov predmetu
     * @return Vráti počet vykonaných zmien
     */
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

    /**
     * Odstránenie predmetu z databázy
     * @param id ID predmetu
     * @return Vráti počet odstránených záznamov
     */
    fun deleteSubject(id: Long): Int {
        val count = delete(Subjects, "$SUB_ID = $id")
        if (count > 0) {
            ScheduleWidget.update()
            NoteWidget.update()
        }
        return count
    }

    /**
     * Kontrola, či sa v databáze nachádzajú nejaké predmety
     * @return [Boolean] true, ak nejaké predmety existujú
     */
    fun hasSubjects() = one("SELECT COUNT(*) FROM $Subjects") {
        moveToFirst() && getInt(0) > 0
    } ?: false

    // casove bloky rozvrhu

    /**
     * Kontrola, či je rozvrh zložený z nejakých hodín
     * @return [Boolean] true, ak je do rozvrhu možné vkladať hodiny
     */
    fun isScheduleSet() = one("""SELECT COUNT(*) FROM $ScheduleRange;""") {
        moveToFirst() && getInt(0) > 0
    } ?: false

    /**
     * Získanie denného časového rozvrhu, teda zoznamu objektov, ktoré popisujú koľko daná hodina trvá,
     * koľko trvá prestávka po nej a v akom poradí tá hodina je. Od toho sa dá odvodiť, kedy hodina
     * začne a skončí.
     * @return Zoznam hodín ako najmenších jednotiek rozvrhu
     */
    fun lessonTimes() = all(
        "SELECT $LES_NO, $LESSON_DUR, $BREAK_DUR FROM $ScheduleRange ORDER BY $LES_NO"
    ) {
        LessonTime(getInt(0), getInt(1), getInt(2))
    }

    /**
     * Zistí najskôrší začiatok a najneskorší koniec vyučovania v danom období
     * @param workWeek Pracovný týždeň
     * @param regularity Výber párneho, nepárneho alebo každého týždňa
     * @return [IntRange] Vráti poradie prvej a poslednej hodiny [Lesson] (poradie začína od 1).
     * Pokiaľ žiadne hodiny v danom období neexistujú vráti obidve nuly.
     */
    fun scheduleRange(workWeek: WorkWeek, regularity: Regularity) = one(
        """
        SELECT COALESCE(MIN($LES_NO), 0), COALESCE(MAX($LES_NO), 0)
        FROM $Schedule
        WHERE ${workWeek.sql} AND ${regularity.sql}
    """
    ) { getInt(0)..getInt(1) }!!


    /**
     * Zistenie začiatku hodiny
     * @return vráti čas začiatku hodiny v minútach meraných od začiadku rozvrhu
     */
    fun lessonStart(order: Int): Int {
        val sql = "SELECT COALESCE(SUM($LESSON_DUR + $BREAK_DUR),0)" +
                " FROM $ScheduleRange WHERE $LES_NO < $order"
        return one(sql) { getInt(0) } ?: 0
    }

    /**
     * Zistenie začiatku hodiny
     * @param day Deň
     * @param regularity pravidelnosť týždňa
     * @return vráti čas začiatku prvej vyučovacej hodiny v minútach meraných od počiatku rozvrhu
     * @see Regularity
     */
    fun firstLessonStart(day: Day, regularity: Regularity): Int {
        val order =
            "SELECT MIN($LES_NO) FROM $Schedule WHERE $DAY = ${day.value} AND ${regularity.sql}"
        val start =
            "SELECT SUM($LESSON_DUR + $BREAK_DUR) FROM $ScheduleRange WHERE $LES_NO < ($order)"
        return Prefs.settings.earliestMinute + (one(start) { getInt(0) } ?: 0)
    }

    /**
     * Vráti strom, ktorého kľúči sú dni [Day] a ich hodnoty [Boolean] hovoria o tom,
     * či v danom dni prebieha aspoň 1 vyučovacia hodina
     * @param reg pravidelnosť týždňa
     * @param workWeek pracovný deň
     * @return Strom hodnôt [TreeMap]<[Day], [Boolean]>
     */
    fun getBusyDays(reg: Regularity, workWeek: WorkWeek): TreeMap<Day, Boolean> {
        val busy = TreeMap<Day, Boolean>().apply {
            for (day in Day.values())
                put(day, false)
        }
        all("SELECT $DAY FROM $Schedule WHERE ${reg.sql} AND ${workWeek.sql} GROUP BY $DAY") {
            busy[Day[getInt(0)]] = true
        }
        return busy
    }

    /**
     * Zistenie konca hodiny
     * @param order poradie hodiny
     * @return vráti čas konca hodiny [LessonTime] v minútach meraných od počiatku rozvrhu alebo 0, ak hodina v tomto poradí neexistuje
     */
    fun lessonEnd(order: Int): Int {
        val st = "SELECT COALESCE(SUM($LESSON_DUR + $BREAK_DUR),0) AS start " +
                "FROM $ScheduleRange WHERE $LES_NO < $order"
        val dur =
            "SELECT $LESSON_DUR AS dur FROM $ScheduleRange WHERE $LES_NO = $order UNION ALL SELECT 0"
        return one("SELECT start + dur FROM (($st), ($dur))") { getInt(0) } ?: 0
    }

    /**
     * Prevedie rozsah poradí hodín do rozsahu minút začiatku prvej a konca druhej hodiny [LessonTime] meraných od počiatku rozvrhu
     * @param range rozsah poradí vyučovacích hodín (od 1)
     * @return rozsah minút meraných od počiatku rozvrhu
     */
    fun scheduleRangeToMinuteRange(range: IntRange): IntRange? = one(
        """SELECT a, b + c FROM
        (SELECT COALESCE(SUM($LESSON_DUR + $BREAK_DUR), 0) AS a FROM $ScheduleRange WHERE $LES_NO < ${range.first}),
        (SELECT COALESCE(SUM($LESSON_DUR + $BREAK_DUR), 0) AS b FROM $ScheduleRange WHERE $LES_NO < ${range.last}),
        (SELECT $LESSON_DUR AS c FROM $ScheduleRange WHERE $LES_NO = ${range.last})"""
    )
    { getInt(0)..getInt(1) }

    /**
     * celkový súčet minút trvania rozvrhu bez hodiny [LessonTime] ktorá je v poradí [except].
     * @param except poradie hodiny, ktorá bude pri súčte ignorovaná
     * @return výsledný súčet dĺžky rozvrhu v minútach meraných od počiatku rozvrhu
     */
    fun scheduleDuration(except: Int = -1) = one(
        """
        SELECT SUM($LESSON_DUR + $BREAK_DUR)
        FROM $ScheduleRange
        WHERE $LES_NO != $except
    """
    ) {
        getInt(0)
    } ?: 0


    /**
     * Upraviť čas hodiny [LessonTime]. Čas musí byť upravený tak, aby rozvrh netrval dlhšie ako 1 deň.
     * @param id ID časovej jednotky rozvrhu [LessonTime]
     * @param lessonDuration trvanie hodiny
     * @param breakDuration trvanie prestávky po hodine
     * @return vráti počet upravených záznamov
     */
    fun updateLessonTime(id: Int, lessonDuration: Int, breakDuration: Int): Int {
        val result =
            if (scheduleDuration(id) + lessonDuration + breakDuration + Prefs.settings.earliestMinute >= 24 * 60) 0
            else update(
                ScheduleRange,
                values { put(LESSON_DUR, lessonDuration); put(BREAK_DUR, breakDuration) },
                "$LES_NO = $id"
            )
        if (Prefs.settings.lessonTimeFormat == LessonTimeFormat.START_TIME) ScheduleWidget.update()
        return result
    }

    /**
     * Odstráni zvolenú časovú jednotku rozvrhu aj s tými, ktoré po nej nasledujú
     * Tým sa vymažú aj predmety z rozvrhu, ktoré v danom čase prebiehali
     * @param order Poradie hodiny [LessonTime]
     */
    fun removeFromSchedule(order: Int): Int {
        val count = delete(ScheduleRange, "$LES_NO = $order")
        ScheduleWidget.update()
        return count
    }

    /**
     * Pridanie časovej jednotky rozvrhu [LessonTime]
     * @param lessonDuration trvanie hodiny
     * @param breakDuration trvanie prestávky po hodine
     * @return Vráti poradie vloženej hodiny [LessonTime] alebo -1, ak pokus o vloženie zlyhal
     */
    fun insertIntoSchedule(lessonDuration: Int, breakDuration: Int): Int {
        val newId =
            if (scheduleDuration() + lessonDuration + breakDuration + Prefs.settings.earliestMinute >= 24 * 60) -1
            else insert(
                ScheduleRange,
                values { put(LESSON_DUR, lessonDuration); put(BREAK_DUR, breakDuration) }).toInt()
        ScheduleWidget.update()
        return newId
    }

    // typy hodín

    /**
     * @return [Boolean] true, ak existujú nejaké typy hodín
     */
    fun hasLessonTypes() = one("SELECT COUNT(*) FROM $LessonTypes;") {
        moveToFirst() && getInt(0) > 0
    } ?: false

    /**
     * Načítanie abecedného zoznamu typov hodín
     * @return abecedný zoznam typov hodín
     */
    fun lessonTypes() = all("SELECT $TYPE, $TYPE_NAME FROM $LessonTypes ORDER BY $TYPE_NAME;") {
        LessonType(getInt(0), getString(1))
    }

    /**
     * Získanie dát o časovej jednotke v na tejto pozícii [lesNo]
     * @param lesNo Pozícia hodiny
     * @return Časová jednotka rozvrhu [LessonTime]
     */
    fun lessonType(lesNo: Int): LessonType? {
        val sql = "SELECT $TYPE, $TYPE_NAME FROM $LessonTypes WHERE $TYPE = $lesNo"
        return one(sql) { LessonType(getInt(0), getString(1)) }
    }

    /**
     * Zlúčenie typov hodín
     * @param keptType typ, ktorý sa zachová
     * @param lostType typ, ktorý zanikne
     */
    fun joinLessonTypes(keptType: Int, lostType: Int) {
        update(LessonData, values { put(TYPE, keptType) }, "$TYPE = $lostType")
        delete(Colors, "$TYPE = $lostType")
        deleteLessonType(lostType)
        ScheduleWidget.update()
    }

    /**
     * Zistenie pozície (od 0) typu hodiny s ID [id] v abecednom zozname typov hodín.
     * @return poradie hodiny v abecednom zozname alebo -1, ak taká hodina neexistuje
     */
    fun lessonTypeIndex(id: Int) = one(
        """
        SELECT CASE WHEN EXISTS(SELECT 1 FROM $LessonTypes WHERE $TYPE = $id) THEN COUNT(*) ELSE -1 END
        FROM $LessonTypes WHERE $TYPE_NAME < (SELECT $TYPE_NAME FROM $LessonTypes WHERE $TYPE = $id);
    """
    ) { getInt(0) } ?: -1

    /**
     * Vloženie nového typu hodiny
     * @param name Názov typu hodiny
     * @return id vloženého typu alebo -1 po neúspešnom vložení
     */
    fun insertLessonType(name: String) =
        insert(LessonTypes, values { put(TYPE_NAME, name) }).toInt()

    /**
     * Premenovanie typu
     * @param id ID typu, ktorého sa to týka
     * @param name Nový názov typu
     * @return počet zmenených riadkov v tabuľke
     */
    fun renameLessonType(id: Int, name: String) =
        update(LessonTypes, values { put(TYPE_NAME, name) }, "$TYPE = $id")

    /**
     * Odstránenie typu hodiny s ID [id]
     * @param id
     * @return počet odstránených záznamov
     */
    fun deleteLessonType(id: Int): Int {
        val result = delete(LessonTypes, "$TYPE = $id")
        ScheduleWidget.update()
        return result
    }

    /**
     * Načíta zoznam vyučovacích hodín rozdelených podľa dní
     * @param workWeek Typ pracovného týždňa
     * @param regularity pravidelnosť týždňa (každý/párny/nepárny)
     * @return zoznamy vyučovacích hodín roztriedené na dni
     */
    fun getLessons(
        workWeek: WorkWeek,
        regularity: Regularity
    ): TreeMap<Day, ArrayList<ScheduleEvent>> {
        var previous: Lesson? = null
        val se = TreeMap<Day, ArrayList<ScheduleEvent>>()
            .apply { workWeek.workDays.forEach { put(it, ArrayList()) } }
        val sql = """
            SELECT sc.$DAY, sc.$LES_NO, le.$TYPE, le.$SUB_ID, su.$ABB, su.$SUB_NAME, le.$ROOM
            FROM $Schedule sc JOIN $LessonData le ON sc.$LES_ID = le.$LES_ID
            JOIN $Subjects su ON (le.$SUB_ID = su.$SUB_ID)
            WHERE ${regularity.sql("sc.$REG")} AND ${workWeek.sql("sc.$DAY")}
            ORDER BY sc.$DAY, sc.$LES_NO
        """
        all(sql) {
            val day = Day[getInt(0)]
            val n = getInt(1)
            val type = getInt(2)
            val sub = Subject(getLong(3), getString(4), getString(5))
            val room = getString(6)
            val les = Lesson(-1L, regularity, day, n..n, type, sub, room)
            val same = les.isAfter(previous) && les.isEqual(previous)
            if (same) previous?.inc() else se[day]!!.add(les)
            if (!same) previous = les
            null
        }
        // vypisat a pozret
        App.log("schedule:")
        for ((d, l) in se) {
            App.log("$d: $l, ${l.map { it.time }}")
        }
        return se
    }

    private fun clearUnusedLessons() =
        delete(LessonData, "$LES_ID NOT IN (SELECT $LES_ID FROM $Schedule)")

    /**
     * Pridanie alebo uvolnenie hodiny
     * @param evt Rozvrhová udalosť (voľno alebo hodina)
     */
    fun setScheduleEvent(evt: ScheduleEvent) {
        if (evt is Lesson) {
            val selCond = "$TYPE = ${evt.type} AND $SUB_ID = ${evt.sub.id} AND $ROOM LIKE ?"
            val sel = "SELECT $LES_ID FROM $LessonData WHERE $selCond"
            val id = one(sel, evt.room ?: "") { getLong(0) } ?: insert(LessonData, values {
                put(TYPE, evt.type)
                put(SUB_ID, evt.sub.id)
                put(ROOM, evt.room)
            })
            transaction("INSERT INTO $Schedule ($REG, $DAY, $LES_NO, $LES_ID) VALUES(?, ?, ?, ?)") {
                bindLong(1, evt.regularity.int.toLong())
                bindLong(2, evt.day.value.toLong())
                bindLong(4, id)
                for (t in evt.time) {
                    bindLong(3, t.toLong())
                    executeInsert()
                }
            }
        } else {
            val cond =
                "$DAY = ${evt.day.value} AND $LES_NO BETWEEN ${evt.time.first} AND ${evt.time.last}"
            delete(
                Schedule, when (evt.regularity) {
                    Regularity.EVERY -> cond
                    Regularity.EVEN -> "$cond AND $REG != 1"
                    Regularity.ODD -> "$cond AND $REG != 0"
                }
            )
        }
        ScheduleWidget.update()
        clearUnusedLessons()
    }

    /**
     * Vyprázdnenie celého rozvrhu
     * @return počet odstránených záznamov
     */
    fun clearSchedule(): Int {
        val result = delete(LessonData)
        if (result > 0) ScheduleWidget.update()
        return result
    }

    /**
     * Získanie údajov o farbe nastavenej pre daný typ hodiny
     * @param id ID typu hodiny
     * @param palette Objekt, do ktorého bude farba uložená
     */
    fun color(id: Int, palette: Palette) {
        one("SELECT $A, $H, $S, $L FROM $Colors WHERE $TYPE = $id;") {
            palette.ahsl(getInt(0), getInt(1), getInt(2), getInt(3))
        } ?: palette.resourceColor(R.color.colorPrimary)
    }

    /**
     * Vráti strom obsahujúci IDečka typov hodín ako kľúče a farby ako hodnoty
     * @param treeMap Ak nie je null, tak do tohoto stromu sa budú zapisovať výsledné hodnoty, inak sa vytvorí nový strom
     * @return vráti strom výsledných hodnôt
     */
    fun colors(treeMap: TreeMap<Int, Palette>? = null): TreeMap<Int, Palette> =
        (treeMap?.apply { clear() } ?: TreeMap<Int, Palette>()).also { map ->
            all("SELECT l.$TYPE, c.$A, c.$H, c.$S, c.$L FROM $LessonTypes l LEFT JOIN $Colors c ON c.$TYPE = l.$TYPE;") {
                map[getInt(0)] = if (isNull(1)) Palette.resource(R.color.colorPrimary)
                else Palette.ahsl(getInt(1), getInt(2), getInt(3), getInt(4))
                null
            }
        }

    /**
     * Nastavenie farby pre daný typ hodiny
     * @param type ID typu hodiny
     * @param color Farba, ktorá sa aplikuje
     * @return počet zmenených záznamov
     */
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

    /**
     * Aktivovať notifikácie všetkých nadchádzajúcich úloh s konečným termínom
     * @param isEnabled Ak true, nastavia sa upozornenia na úlohy v určenom predstihu, inak sa všetky notifikácie vypnú
     */
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

    /**
     * Vloží sa množina úloh
     * @param notes zoznam úloh
     */
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
            NoteWidget.update()
        }
    }

    /**
     * Kontrola, či rovnaká úloha už existuje
     * @param note vzor
     * @return [Boolean] true, ak existuje
     */
    fun hasSimilarNote(note: Note) = one("""
        SELECT $SUB_ID, $NOTE, $DEADLINE
        FROM $Notes
        WHERE $SUB_ID = ${note.sub.id}
        AND UPPER(TRIM($NOTE)) LIKE UPPER(TRIM(?))
        AND ${note.deadline?.let { "$DEADLINE = $it" } ?: "$DEADLINE IS NULL"}
    """, note.description) { true } ?: false

    /**
     * Načítanie zoznamu úloh s termínom dokončenia, ktorého čas ešte nevypršal
     * @return zoznam úloh
     */
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

    /**
     * Vráti zoznam úloh vyhovujúcich danej kategórii
     * @param noteCategory Kategória
     * @return zoznam úloh
     */
    fun notes(noteCategory: NoteCategory): ArrayList<Note> {
        val subjects = TreeMap<Long, Subject>()
        val where = noteCategoryWhere(noteCategory)
            .replace("(($SUB_ID)|($DEADLINE))".toRegex(), "n.$1")
        return all(
            """
            SELECT n.$NOTE_ID, n.$SUB_ID, s.$ABB, s.$SUB_NAME, n.$NOTE, n.$DEADLINE
            FROM $Notes n JOIN $Subjects s ON n.$SUB_ID = s.$SUB_ID
            WHERE $where
            ORDER BY CASE WHEN n.$DEADLINE IS NULL THEN 1 ELSE 0 END, n.$DEADLINE, s.$ABB, n.$NOTE, n.$NOTE_ID
            LIMIT 100;
        """
        ) {
            val sub = subjects[getLong(1)]
                ?: Subject(getLong(1), getString(2), getString(3))
                    .also { s -> subjects[s.id] = s }
            Note(getLong(0), sub, getString(4), getLongOrNull(5))
        }
    }

    /**
     * Kontrola či je úloha s ID [id] súčasťou kategórie [category].
     * @param id ID úlohy
     * @param category Kategória úlohy
     * @return true, ak hodina existuje a patrí do kategórie [category]
     */
    fun noteBelongsToCategory(id: Long, category: NoteCategory): Boolean = one(
        "SELECT 1 FROM $Notes WHERE $NOTE_ID = $id AND ${noteCategoryWhere(category)}"
    ) { true } ?: false

    /**
     * Vymazať všetky úlohy vyhovujúce kategórii [category]
     * @param category Kategória úlohy
     * @return počet odstránených záznamov
     */
    fun clearNotesOfCategory(category: NoteCategory): Int {
        val count = delete(Notes, noteCategoryWhere(category))
        if (count > 0) NoteWidget.update()
        return count
    }

    /**
     * * Vložiť alebo upraviť úlohu
     * @param id ID úlohy (-1 pre vloženie, 1..Inf pre upravu)
     * @param sub Predmet
     * @param info Popis úlohy
     * @param deadline Termín do uplynutia platnosti v milisekundách. Môže byť null
     * @return počet zmených záznamov
     */
    fun setNote(id: Long, sub: Long, info: String, deadline: Long?): Long {
        val input = values { put(SUB_ID, sub); put(NOTE, info); put(DEADLINE, deadline) }
        val vId = when {
            id > 0 && update(Notes, input, "$NOTE_ID = $id") == 1 -> id
            id == -1L -> insert(Notes, input)
            else -> -1L
        }
        if (vId > -1L) {
            NoteReminder.editNotification(vId, subject(sub)!!.abb, info, deadline)
            NoteWidget.update()
        }
        return vId
    }

    /**
     * Pridať úlohu
     * @param sub Predmet
     * @param info Popis úlohy
     * @param deadline Termín do uplynutia platnosti v milisekundách. Môže byť null
     * @return ID vloženej úlohy alebo -1 po neúspešnom vložení
     */
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

    /**
     * Upraviť úlohu
     * @param id ID úlohy
     * @param sub Predmet
     * @param info Popis úlohy
     * @param deadline Termín do uplynutia platnosti v milisekundách. Môže byť null
     * @return počet zmených záznamov
     */
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

    /**
     * Zmeniť termín platnosti úlohy
     * @param id ID úlohy
     * @param deadline nový termín úlohy v milisekundách. Môže byť null.
     * @return počet zmenených záznamov
     */
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

    /**
     * Získanie počtu neplatných úloh, ktorých čas už vypršal
     * @return výsledný počet
     */
    fun missedNotes(subject: Long) = one(
        "SELECT COUNT(*) FROM $Notes WHERE $SUB_ID = $subject AND $DEADLINE IS NOT NULL AND $DEADLINE <= $now"
    ) {
        getInt(0)
    }!!

    /**
     * Získanie počtu platných úloh
     * @return výsledný počet
     */
    fun incomingNotes(subject: Long) = one(
        "SELECT COUNT(*) FROM $Notes WHERE $SUB_ID = $subject AND ($DEADLINE IS NULL OR $DEADLINE > ${now})"
    ) {
        getInt(0)
    }!!

    /**
     * Odstránenie úlohy
     * @param id ID úlohy, ktorú chcem odstrániť
     * @return počet odstránených záznamov
     */
    fun removeNote(id: Long): Int {
        val count = delete(Notes, "$NOTE_ID = $id")
        if (count > 0) {
            NoteReminder.editNotification(id, "", "", null)
            NoteWidget.update()
        }
        return count
    }

    /**
     * Určenie kategórie úlohy. Prednosť majú časové kategórie. Ak úloha s ID [id] neexustuje vyberie sa všetko
     * @param id ID úlohy
     * @return Kategória [NoteCategory]
     */
    fun detectNoteCategory(id: Long): NoteCategory = one(
        "SELECT $DEADLINE, $SUB_ID FROM $Notes WHERE $NOTE_ID = $id"
    ) {
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

    /**
     * Odstránenie nadbytočných dát. Patria sem hodiny mimo pracovného dňa, hodiny pre párne
     * alebo nepárne týždne, keď nie je povolený 2-týždenný rozvrh, úlohy, ktorých čas splnenia
     * už vypršal a predmety, ktore nie sú použité v rozvrhu a nie sú k nim zaznamenané žiadne úlohy.
     *
     * @return počet odstránených záznamov z tabuliek v databáze
     */
    fun clearGarbageData(): Int {
        val lessons = delete(Schedule, "NOT(${Prefs.settings.workWeek.sql})")
        val reg = if (Prefs.settings.dualWeekSchedule) 0 else delete(Schedule, "$REG < 2")
        clearUnusedLessons()

        // na oneskorene poznamky uz upozornenie necaka
        val notes = delete(Notes, "$DEADLINE IS NOT NULL AND $DEADLINE <= $now")
        val subjects = delete(
            Subjects, """$SUB_ID NOT IN (
            SELECT $SUB_ID FROM $LessonData GROUP BY $SUB_ID
            UNION ALL SELECT $SUB_ID FROM $Notes GROUP BY $SUB_ID
        )"""
        )
        if (notes + subjects > 0) NoteWidget.update()
        return lessons + reg + notes + subjects
    }

    /**
     * Naplánovať čas ďalšej aktualizácie widgetov s úloh. Aktualizácia prebehne o polnoci alebo skôr
     * ak sa v rámci dňa blíži konečný termín platnosti úlohy
     *
     * @return vráti dátum a čas v milisekundách, kedy bude ďaľšia aktualizácia widgetov úloh
     */
    fun scheduleNextWidgetUpdate() = one(
        "SELECT MIN($DEADLINE) FROM $Notes WHERE $DEADLINE > ${System.currentTimeMillis()};"
    ) {
        getLongOrNull(0)?.coerceAtMost(App.now.nextMidnight + 60000)
    } ?: App.now.nextMidnight
}