package com.moriak.schednote.storage

import android.database.Cursor
import androidx.core.database.getIntOrNull
import com.moriak.schednote.*
import com.moriak.schednote.data.*
import com.moriak.schednote.enums.Day
import com.moriak.schednote.enums.Regularity
import com.moriak.schednote.enums.TimeCategory
import com.moriak.schednote.enums.WorkWeek
import com.moriak.schednote.interfaces.NoteCategory
import com.moriak.schednote.storage.LessonData.LES_ID
import com.moriak.schednote.storage.LessonData.ROOM
import com.moriak.schednote.storage.LessonTypes.COLOR
import com.moriak.schednote.storage.LessonTypes.TYPE
import com.moriak.schednote.storage.LessonTypes.TYPE_NAME
import com.moriak.schednote.storage.Notes.DEADLINE
import com.moriak.schednote.storage.Notes.NOTE
import com.moriak.schednote.storage.Notes.NOTE_ID
import com.moriak.schednote.storage.Schedule.DAY
import com.moriak.schednote.storage.Schedule.REG
import com.moriak.schednote.storage.ScheduleRange.BREAK_DUR
import com.moriak.schednote.storage.ScheduleRange.LESSON_DUR
import com.moriak.schednote.storage.ScheduleRange.LES_NO
import com.moriak.schednote.storage.Subjects.ABB
import com.moriak.schednote.storage.Subjects.SUB_ID
import java.util.*
import com.moriak.schednote.storage.Subjects.NAME as SUB_NAME
import java.lang.System.currentTimeMillis as now

/**
 * Trieda spravuje dáta z SQLite databázy.
 */

object SQLite : SQLiteControl() {
    private val cal by lazy { Calendar.getInstance() }
    private val LongRange.sql get() = "BETWEEN $first AND $last"
    private fun Cursor.getLongOrNull(column: Int) = if (isNull(column)) null else getLong(column)

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
        return one(sql, abb.trim().uppercase()) {
            Subject(getLong(0), getString(1), getString(2))
        }
    }

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
    fun addSubject(abb: String, name: String): Long = insert(Subjects) { put(ABB, abb); put(SUB_NAME, name) }

    /**
     * Úprava predmetu v databázy
     * @param abb Nová skratka predmetu
     * @param name Nový názov predmetu
     * @return Vráti počet vykonaných zmien
     */
    fun editSubject(id: Long, abb: String, name: String): Int = update(Subjects, "$SUB_ID = $id") {
        put(ABB, abb)
        put(SUB_NAME, name)
    }

    /**
     * Odstránenie predmetu z databázy
     * @param id ID predmetu
     * @return Vráti počet odstránených záznamov
     */
    fun deleteSubject(id: Long): Int = delete(Subjects, "$SUB_ID = $id")

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
    fun firstLessonStart(day: Day, regularity: Regularity): Int? {
        val reg = when (regularity) {
            Regularity.EVEN -> "$REG != 1"
            Regularity.ODD -> "$REG != 0"
            Regularity.EVERY -> "$REG = 2"
        }
        val order = "SELECT MIN($LES_NO) FROM $Schedule WHERE $DAY = ${day.value} AND $reg"
        val start = "SELECT SUM($LESSON_DUR + $BREAK_DUR) FROM $ScheduleRange WHERE $LES_NO < ($order)"
        return one(start) { getIntOrNull(0) }
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
     * Upraviť čas hodiny [LessonTime]. Čas musí byť upravený tak, aby rozvrh netrval dlhšie ako 1 deň.
     * @param id ID časovej jednotky rozvrhu [LessonTime]
     * @param lessonDuration trvanie hodiny
     * @param breakDuration trvanie prestávky po hodine
     * @return vráti počet upravených záznamov
     */
    fun updateLessonTime(id: Int, lessonDuration: Int, breakDuration: Int): Int = update(ScheduleRange, "$LES_NO = $id") {
        put(LESSON_DUR, lessonDuration)
        put(BREAK_DUR, breakDuration)
    }

    /**
     * Odstráni zvolenú časovú jednotku rozvrhu aj s tými, ktoré po nej nasledujú
     * Tým sa vymažú aj predmety z rozvrhu, ktoré v danom čase prebiehali
     * @param order Poradie hodiny [LessonTime]
     */
    fun removeFromSchedule(order: Int): Int = delete(ScheduleRange, "$LES_NO = $order")

    /**
     * Pridanie časovej jednotky rozvrhu [LessonTime]
     * @param lessonDuration trvanie hodiny
     * @param breakDuration trvanie prestávky po hodine
     * @return Vráti poradie vloženej hodiny [LessonTime] alebo -1, ak pokus o vloženie zlyhal
     */
    fun insertIntoSchedule(lessonDuration: Int, breakDuration: Int): Int = insert(ScheduleRange) {
        put(LESSON_DUR, lessonDuration)
        put(BREAK_DUR, breakDuration)
    }.toInt()

    /**
     * Načítanie abecedného zoznamu typov hodín
     * @return abecedný zoznam typov hodín
     */
    fun lessonTypes() = all("SELECT $TYPE, $TYPE_NAME, $COLOR FROM $LessonTypes ORDER BY $TYPE_NAME;") {
        LessonType(getInt(0), getString(1), Palette(getInt(2)))
    }

    /**
     * Získanie dát o type vyučovania s názvom [name]
     * @param name Názov typu
     * @return Typ vyučovacej hodiny [LessonType]
     */
    fun lessonType(name: String) = one("SELECT $TYPE, $TYPE_NAME, $COLOR " +
            "FROM $LessonTypes WHERE UPPER($TYPE_NAME) LIKE UPPER(?)", name) {
        LessonType(getInt(0), getString(1), Palette(getInt(2)))
    }

    /**
     * Získanie dát o type vyučovania s ID [id]
     * @param id Názov typu
     * @return Typ vyučovacej hodiny [LessonType]
     */
    fun lessonType(id: Int) = one("SELECT $TYPE, $TYPE_NAME, $COLOR " +
            "FROM $LessonTypes WHERE $TYPE = $id") {
        LessonType(getInt(0), getString(1), Palette(getInt(2)))
    }

    /**
     * Vloženie nového typu hodiny
     * @param name Názov typu hodiny
     * @return id vloženého typu alebo -1 po neúspešnom vložení
     */
    fun insertLessonType(name: String) = insert(LessonTypes) { put(TYPE_NAME, name) }.toInt()

    /**
     * Premenovanie typu
     * @param id ID typu, ktorého sa to týka
     * @param name Nový názov typu
     * @return počet zmenených riadkov v tabuľke
     */
    fun renameLessonType(id: Int, name: String) = update(LessonTypes, "$TYPE = $id") { put(TYPE_NAME, name) }

    /**
     * Odstránenie typu hodiny s ID [id]
     * @param id
     * @return počet odstránených záznamov
     */
    fun deleteLessonType(id: Int): Int = delete(LessonTypes, "$TYPE = $id")

    /**
     * Načíta vyučovacie hodiny
     * @param workWeek Pracovný týždeň
     * @param regularity Párnosť týždňa (každý/párny/nepárny)
     * @return zoznam vyučovacích hodín
     */
    fun getLessons(workWeek: WorkWeek, regularity: Regularity): List<Lesson> {
        val st = "st"
        val en = "en"

        val reg = "$REG = ${regularity.ordinal}"
        val sql = """
SELECT sch.$DAY, sch.$st, sch.$en, ld.$TYPE, sub.$SUB_ID, sub.$ABB, sub.$SUB_NAME, ld.$ROOM
FROM (
    SELECT $DAY, $LES_ID, $st, MIN($en) AS $en
    FROM (
        SELECT a.$REG, a.$DAY, a.$LES_ID, a.$LES_NO AS $st, b.$LES_NO as $en
        FROM $Schedule a
        JOIN $Schedule b
        ON a.$DAY = b.$DAY AND a.$LES_ID = b.$LES_ID AND a.$LES_NO <= b.$LES_NO
        WHERE a.$reg
        AND b.$reg
        AND a.$LES_ID NOT IN (
            SELECT $LES_ID
            FROM $Schedule
            WHERE $LES_ID = a.$LES_ID
            AND $DAY = a.$DAY
            AND $LES_NO + 1 = a.$LES_NO
            AND $reg
        )
        AND b.$LES_ID NOT IN (
            SELECT $LES_ID
            FROM $Schedule
            WHERE $LES_ID = b.$LES_ID
            AND $DAY = b.$DAY
            AND $LES_NO - 1 = b.$LES_NO
            AND $reg
        )
    )
    WHERE $DAY NOT IN (${workWeek.weekend.map{ it.value }.joinToString(", ")})
    GROUP BY $DAY, $LES_ID, $st
) sch
JOIN $LessonData ld
ON ld.$LES_ID = sch.$LES_ID
JOIN $Subjects sub
ON sub.$SUB_ID = ld.$SUB_ID"""

        return all(sql) {
            val day = Day[getInt(0)]
            val range = getInt(1)..getInt(2)
            val type = getInt(3)
            val sub = Subject(getLong(4), getString(5), getString(6))
            val room = getString(7)
            Lesson(regularity, day, range, type, sub, room)
        }
    }

    fun clearUnusedLessons() = delete(LessonData, "$LES_ID NOT IN (SELECT $LES_ID FROM $Schedule)")

    /**
     * Uvoľnenie miesta v rozvrhu
     * @param day
     * @param time
     * @param regularity
     */
    fun clearSchedule(day: Day, time: IntRange, regularity: Regularity) {
        val cond = "$DAY = ${day.value} AND $LES_NO BETWEEN ${time.first} AND ${time.last}"
        val reg = when(regularity) {
            Regularity.ODD -> " AND $REG != 0"
            Regularity.EVEN -> " AND $REG != 1"
            Regularity.EVERY -> ""
        }
        delete(Schedule, cond + reg)
    }

    /**
     * Pridanie hodiny do rozvrhu
     * @param lesson Vyučovacia hodina
     */
    fun setLesson(lesson: Lesson) {
        clearSchedule(lesson.day, lesson.time, lesson.reg)
        val found = "SELECT $LES_ID FROM $LessonData WHERE $TYPE = ${lesson.type}" +
                " AND $SUB_ID = ${lesson.sub.id} AND $ROOM LIKE ?"
        val id = one(found, lesson.room ?: "") { getLong(0) }
            ?: insert(LessonData) {
                put(TYPE, lesson.type)
                put(SUB_ID, lesson.sub.id)
                put(ROOM, lesson.room)
            }
        transaction("INSERT INTO $Schedule ($REG, $DAY, $LES_NO, $LES_ID) VALUES(?, ?, ?, ?)") {
            bindLong(1, lesson.reg.ordinal.toLong())
            bindLong(2, lesson.day.value.toLong())
            bindLong(4, id)
            for (t in lesson.time) {
                bindLong(3, t.toLong())
                executeInsert()
            }
        }
    }

    /**
     * Vyprázdnenie celého rozvrhu
     * @return počet odstránených záznamov
     */
    fun clearSchedule(regularity: Regularity? = null): Int = delete(
        Schedule, when (regularity) {
        Regularity.ODD -> "$REG != 0"
        Regularity.EVEN -> "$REG != 1"
        else -> "1=1"
    }).also { if (it > 0) clearUnusedLessons() }

    /**
     * Získanie údajov o farbe nastavenej pre daný typ hodiny
     * @param id ID typu hodiny
     * @param palette Objekt, do ktorého bude farba uložená
     */
    fun getTypeColor(id: Int, palette: Palette) = one("SELECT $COLOR FROM $LessonTypes WHERE $TYPE = $id;") {
        palette.setColor(getInt(0))
    } ?: palette

    /**
     * Nastavenie farby pre daný typ hodiny
     * @param type ID typu hodiny
     * @param color Farba, ktorá sa aplikuje
     * @return počet zmenených záznamov
     */
    fun setColor(type: Int, color: Palette): Int = update(LessonTypes, "$TYPE = $type") { put(COLOR, color.color) }

    private fun noteCategoryWhere(category: NoteCategory, prefix: String = "") = when (category) {
        TimeCategory.TIMELESS -> "$prefix$DEADLINE IS NULL"
        TimeCategory.TODAY -> "$prefix$DEADLINE ${cal.now.today.sql}"
        TimeCategory.TOMORROW -> "$prefix$DEADLINE ${cal.now.tomorrow.sql}"
        TimeCategory.IN_WEEK -> "$prefix$DEADLINE ${cal.now.forWeek.sql}"
        TimeCategory.IN_MONTH -> "$prefix$DEADLINE ${cal.now.forMonth.sql}"
        TimeCategory.LATE -> "$prefix$DEADLINE < ${now()}"
        TimeCategory.UPCOMING -> "$prefix$DEADLINE > ${now()}"
        is Subject -> "$prefix$SUB_ID = ${category.id}"
        else -> "1 = 1"
    }

    /**
     * Vloží sa množina úloh
     * @param notes zoznam úloh
     */
    fun insertMultipleNotes(notes: List<Note>): Int {
        var counter = 0
        if (notes.isNotEmpty()) {
            transaction("INSERT INTO $Notes ($SUB_ID, $NOTE, $DEADLINE) VALUES (?, ?, ?)") {
                for (note in notes) {
                    bindLong(1, note.sub.id)
                    bindString(2, note.info)
                    note.deadline?.let { bindLong(3, note.deadline) } ?: bindNull(3)
                    if (executeInsert() != -1L) counter++
                }
            }
        }
        return counter
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
    """, note.info) { true } ?: false

    /**
     * Vráti úlohu s id [id]
     * @param id
     */
    fun note(id: Long): Note? = if (id == -1L) null else one("""
                SELECT n.$NOTE_ID, n.$SUB_ID, s.$ABB, s.$SUB_NAME, n.$NOTE, n.$DEADLINE
                FROM $Notes n JOIN $Subjects s ON n.$SUB_ID = s.$SUB_ID
                WHERE n.$NOTE_ID = $id
            """) {
        val sub = Subject(getLong(1), getString(2), getString(3))
        Note(getLong(0), sub, getString(4), getLongOrNull(5))
    }

    /**
     * Vráti zoznam úloh vyhovujúcich danej kategórii
     * @param categories Zoznam kategórii, ktoré musia platiť súčasne
     * @return zoznam úloh
     */
    fun notes(vararg categories: NoteCategory): ArrayList<Note> {
        val subjects = TreeMap<Long, Subject>()
        val where = if (categories.count() == 0) noteCategoryWhere(TimeCategory.ALL, "n.")
        else categories.joinToString( " AND ") { noteCategoryWhere(it, "n.") }
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
     * Vymazať všetky úlohy vyhovujúce kategórii [category]
     * @param category Kategória úlohy
     * @return počet odstránených záznamov
     */
    fun clearNotesOfCategory(category: NoteCategory): Int = delete(Notes, noteCategoryWhere(category))

    /**
     * Pridať úlohu
     * @param sub Predmet
     * @param info Popis úlohy
     * @param deadline Termín do uplynutia platnosti v milisekundách. Môže byť null
     * @return ID vloženej úlohy alebo -1 po neúspešnom vložení
     */
    fun addNote(sub: Long, info: String, deadline: Long?): Long = insert(Notes) {
        put(SUB_ID, sub)
        put(NOTE, info)
        put(DEADLINE, deadline)
    }

    /**
     * Upraviť úlohu
     * @param id ID úlohy
     * @param sub Predmet
     * @param info Popis úlohy
     * @param deadline Termín do uplynutia platnosti v milisekundách. Môže byť null
     * @return počet zmených záznamov
     */
    fun updateNote(id: Long, sub: Long, info: String, deadline: Long?): Int = update(Notes, "$NOTE_ID = $id") {
        put(SUB_ID, sub)
        put(NOTE, info)
        put(DEADLINE, deadline)
    }

    /**
     * Odstránenie úlohy
     * @param id ID úlohy, ktorú chcem odstrániť
     * @return počet odstránených záznamov
     */
    fun removeNote(id: Long): Int = delete(Notes, "$NOTE_ID = $id")

    /**
     * Odstránenie nadbytočných dát. Patria sem hodiny mimo pracovného dňa, hodiny pre párne
     * alebo nepárne týždne, keď nie je povolený 2-týždenný rozvrh, úlohy, ktorých čas splnenia
     * už vypršal a predmety, ktore nie sú použité v rozvrhu a nie sú k nim zaznamenané žiadne úlohy.
     *
     * @return počet odstránených záznamov z tabuliek v databáze
     */
    fun clearGarbageData(): Int {
        val ww = "$DAY IN (${Prefs.Settings.workWeek.weekend.map { it.value }.joinToString()})"
        var deleted = delete(Schedule, ww)
        deleted += if (Prefs.Settings.dualWeekSchedule) 0 else delete(Schedule, "$REG < 2")
        deleted += clearUnusedLessons()

        // na oneskorene poznamky uz upozornenie necaka
        deleted += delete(Notes, "$DEADLINE <= ${now()}")
        deleted +=  delete(
            Subjects, """$SUB_ID NOT IN (
            SELECT $SUB_ID FROM $LessonData GROUP BY $SUB_ID
            UNION ALL SELECT $SUB_ID FROM $Notes GROUP BY $SUB_ID
        )"""
        )
        return deleted
    }
}