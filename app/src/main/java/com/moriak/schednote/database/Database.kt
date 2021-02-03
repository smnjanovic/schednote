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
import com.moriak.schednote.database.tables.ScheduleRange.BREAK_DUR
import com.moriak.schednote.database.tables.ScheduleRange.LESSON_DUR
import com.moriak.schednote.database.tables.ScheduleRange.ORDER
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
            val first = workDay.first().value
            val last = workDay.last().value
            return when (workDay.size) {
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

    /**
     * Načítanie predmetu z databázy
     * @param id ID predmetu
     * @return [Subject] Vráti null, ak predmet s takým ID neexistuje
     */
    fun subject(id: Long) =
        one("SELECT $SUB_ID, $ABB, $SUB_NAME FROM $Subjects WHERE $SUB_ID = $id;") {
            Subject(getLong(0), getString(1), getString(2))
        }

    /**
     * Načítanie predmetu z databázy
     * @param abb Skratka predmetu
     * @return [Subject] Vráti null, ak predmet s takou skratkou neexistuje
     */
    fun subject(abb: String) = one(
        "SELECT $SUB_ID, $ABB, $SUB_NAME FROM $Subjects WHERE $ABB LIKE ?;",
        abb.trim().toUpperCase(Locale.ROOT)
    ) {
        Subject(getLong(0), getString(1), getString(2))
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
    fun hasSubjects() =
        one("SELECT COUNT(*) FROM $Subjects") { moveToFirst() && getInt(0) > 0 } ?: false

    // casove bloky rozvrhu

    /**
     * Kontrola, či je rozvrh zložený z nejakých hodín
     * @return [Boolean] true, ak je do rozvrhu možné vkladať hodiny
     */
    fun isScheduleSet() =
        one("""SELECT COUNT(*) FROM $ScheduleRange;""") { moveToFirst() && getInt(0) > 0 } ?: false

    /**
     * Získanie denného časového rozvrhu, teda zoznamu objektov, ktoré popisujú koľko daná hodina trvá,
     * koľko trvá prestávka po nej a v akom poradí tá hodina je. Od toho sa dá odvodiť, kedy hodina
     * začne a skončí.
     * @return Zoznam hodín ako najmenších jednotiek rozvrhu
     */
    fun lessonTimes() = all(
        "SELECT $ORDER, $LESSON_DUR, $BREAK_DUR FROM $ScheduleRange ORDER BY $ORDER"
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
        SELECT COALESCE(MIN($START), 0), COALESCE(MAX($START + $DUR), 1)
        FROM $Lessons
        WHERE ${workWeek.sql} AND ${regularity.sql}
    """
    ) {
        getInt(0) until getInt(1)
    }!!

    /**
     * Zistenie začiatku hodiny
     * @return vráti čas začiatku hodiny v minútach meraných od začiadku rozvrhu
     */
    fun lessonStart(order: Int): Int {
        val sql =
            "SELECT COALESCE(SUM($LESSON_DUR + $BREAK_DUR),0) FROM $ScheduleRange WHERE $ORDER < $order"
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
            "SELECT MIN($START) FROM $Lessons WHERE $DAY = ${day.value} AND ${regularity.sql}"
        val start =
            "SELECT SUM($LESSON_DUR + $BREAK_DUR) FROM $ScheduleRange WHERE $ORDER < ($order);"
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
        all("SELECT $DAY FROM $Lessons WHERE ${reg.sql} AND ${workWeek.sql} GROUP BY $DAY") {
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
                "FROM $ScheduleRange WHERE $ORDER < $order"
        val dur = "SELECT $LESSON_DUR AS dur FROM $ScheduleRange WHERE $ORDER = $order"
        return one("SELECT start + dur FROM (($st), ($dur))") { getInt(0) } ?: 0
    }

    /**
     * Prevedie rozsah poradí hodín do rozsahu minút začiatku prvej a konca druhej hodiny [LessonTime] meraných od počiatku rozvrhu
     * @param range rozsah poradí vyučovacích hodín (od 1)
     * @return rozsah minút meraných od počiatku rozvrhu
     */
    fun scheduleRangeToMinuteRange(range: IntRange): IntRange? = one(
        """
        SELECT a, b + c FROM
        (SELECT COALESCE(SUM($LESSON_DUR + $BREAK_DUR), 0) AS a FROM $ScheduleRange WHERE $ORDER < ${range.first}),
        (SELECT COALESCE(SUM($LESSON_DUR + $BREAK_DUR), 0) AS b FROM $ScheduleRange WHERE $ORDER < ${range.last}),
        (SELECT $LESSON_DUR AS c FROM $ScheduleRange WHERE $ORDER = ${range.last});
    """
    ) {
        getInt(0)..getInt(1)
    }

    /**
     * celkový súčet minút trvania rozvrhu bez hodiny [LessonTime] ktorá je v poradí [except].
     * @param except poradie hodiny, ktorá bude pri súčte ignorovaná
     * @return výsledný súčet dĺžky rozvrhu v minútach meraných od počiatku rozvrhu
     */
    fun scheduleDuration(except: Int = -1) = one(
        """
        SELECT SUM($LESSON_DUR + $BREAK_DUR)
        FROM $ScheduleRange
        WHERE $ORDER != $except
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
                "$ORDER = $id"
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
        val count = delete(ScheduleRange, "$ORDER = $order")
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
    fun hasLessonTypes() =
        one("SELECT COUNT(*) FROM $LessonTypes;") { moveToFirst() && getInt(0) > 0 } ?: false

    /**
     * Načítanie abecedného zoznamu typov hodín
     * @return abecedný zoznam typov hodín
     */
    fun lessonTypes() = all("SELECT $TYPE, $TYPE_NAME FROM $LessonTypes ORDER BY $TYPE_NAME;") {
        LessonType(getInt(0), getString(1))
    }

    /**
     * Získanie dát o časovej jednotke v na tejto pozícii [order]
     * @param order Pozícia hodiny
     * @return Časová jednotka rozvrhu [LessonTime]
     */
    fun lessonType(order: Int) =
        one("SELECT $TYPE, $TYPE_NAME FROM $LessonTypes WHERE $TYPE = $order;") {
            LessonType(getInt(0), getString(1))
        }

    /**
     * Zlúčenie typov hodín
     * @param keptType typ, ktorý sa zachová
     * @param lostType typ, ktorý zanikne
     */
    fun joinLessonTypes(keptType: Int, lostType: Int) {
        update(Lessons, values { put(TYPE, keptType) }, "$TYPE = $lostType")
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
     * Načíta zoradený zoznam rozvrhových udalostí.
     * @param workWeek Pracovný týždeň
     * @param regularity pravidelnosť týždňa
     * @return zoradený zoznam udalostí rozvrhu
     */
    fun fullSchedule(workWeek: WorkWeek, regularity: Regularity): ArrayList<ScheduleEvent> {
        val range = scheduleRange(workWeek, regularity)

        val list: ArrayList<ScheduleEvent> = all(
            """
            SELECT a.$LES_ID, a.$WEEK_ODDITY, a.$DAY, a.$START, a.$START + a.$DUR,
                a.$TYPE, b.$SUB_ID, b.$ABB, b.$SUB_NAME, a.$ROOM
            FROM $Lessons a JOIN $Subjects b ON a.$SUB_ID = b.$SUB_ID
            WHERE ${regularity.sql.replace(WEEK_ODDITY, "a.$WEEK_ODDITY")}
            AND ${workWeek.sql.replace(DAY, "a.$DAY")}
            ORDER BY CASE
            WHEN a.$DAY BETWEEN ${workWeek.workDay.first().value} AND 7
            THEN 0 ELSE 1 END, a.$DAY, a.$START;
        """
        ) {
            val time = getInt(3) until getInt(4)
            val reg = Regularity[getIntOrNull(1)?.let { it == 1 }]
            val sub = Subject(getLong(6), getString(7), getString(8))
            val room = if (isNull(9)) null else getString(9)
            Lesson(getLong(0), reg, Day[getInt(2)], time, getInt(5), sub, room)
        }
        if (list.size == 0) {
            for (d in workWeek.workDay) list.add(Free(regularity, d, 0..0))
        }
        val days = workWeek.workDay
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

    /**
     * Nastavenie hodiny [Lesson] - udalosti rozvrhu.
     * @param id ID hodiny, ktorá je upravovaná alebo -1, ak je vkladaná nová hodina
     * @param regularity Pravidelnosť týždňa
     * @param day Deň
     * @param time Rozsah poradí časových jednotiek rozvrhu [LessonTime]
     * @param type Typ vyučovacej hodiny
     * @param subject Predmet
     * @param room Miestnosť
     */
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

    /**
     * Odstránenie vyučovacej hodiny [Lesson] z rozvrhu
     * @param id ID hodiny
     * @return počet odstránených záznamov
     */
    fun deleteLesson(id: Long): Int {
        val result = delete(Lessons, "$LES_ID = $id")
        ScheduleWidget.update()
        return result
    }

    /**
     * Vyprázdnenie celého rozvrhu
     * @return počet odstránených záznamov
     */
    fun clearSchedule(): Int {
        val result = delete(Lessons)
        ScheduleWidget.update()
        return result
    }

    /**
     * Odstránenie hodín v danom časovom úseku rozvrhu
     * @param day Deň
     * @param clearTime Rozsah poradí časových jednotiek rozvrhu [LessonTime] - čas ktorý je nutné uvoľniť
     * @param regularity Výber týždňa podľa párnosti, nepárnosti alebo všetky
     * @return Vráti počet odstránených záznamov
     */
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
            ORDER BY CASE WHEN n.$DEADLINE IS NULL THEN 1 ELSE 0 END, n.$DEADLINE, n.$SUB_ID;
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

    /**
     * Odstránenie nadbytočných dát. Patria sem hodiny mimo pracovného dňa, hodiny pre párne
     * alebo nepárne týždne, keď nie je povolený 2-týždenný rozvrh, úlohy, ktorých čas splnenia
     * už vypršal a predmety, ktore nie sú použité v rozvrhu a nie sú k nim zaznamenané žiadne úlohy.
     *
     * @return počet odstránených záznamov z tabuliek v databáze
     */
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