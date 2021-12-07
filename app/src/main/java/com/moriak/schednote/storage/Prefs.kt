package com.moriak.schednote.storage

import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.net.toUri
import com.moriak.schednote.AlarmTone
import com.moriak.schednote.App
import com.moriak.schednote.Palette
import com.moriak.schednote.enums.*
import com.moriak.schednote.storage.Prefs.FirstVisit.alarmClocks
import com.moriak.schednote.storage.Prefs.FirstVisit.alarmTune
import com.moriak.schednote.storage.Prefs.FirstVisit.design
import com.moriak.schednote.storage.Prefs.FirstVisit.lessonSchedule
import com.moriak.schednote.storage.Prefs.FirstVisit.lessonTypes
import com.moriak.schednote.storage.Prefs.FirstVisit.notes
import com.moriak.schednote.storage.Prefs.FirstVisit.reminders
import com.moriak.schednote.storage.Prefs.FirstVisit.semester
import com.moriak.schednote.storage.Prefs.FirstVisit.subjects
import com.moriak.schednote.storage.Prefs.FirstVisit.timeSchedule
import com.moriak.schednote.storage.Prefs.Notifications.bits
import com.moriak.schednote.storage.Prefs.Notifications.reminderAdvanceInMinutes
import com.moriak.schednote.storage.Prefs.Notifications.reminderEnabled
import com.moriak.schednote.storage.Prefs.Settings.alarmTone
import com.moriak.schednote.storage.Prefs.Settings.dateFormat
import com.moriak.schednote.storage.Prefs.Settings.dualWeekSchedule
import com.moriak.schednote.storage.Prefs.Settings.earliestMinute
import com.moriak.schednote.storage.Prefs.Settings.lessonTimeFormat
import com.moriak.schednote.storage.Prefs.Settings.semesterStart
import com.moriak.schednote.storage.Prefs.Settings.semesterValid
import com.moriak.schednote.storage.Prefs.Settings.semesterWeek
import com.moriak.schednote.storage.Prefs.Settings.semesterWeekCount
import com.moriak.schednote.storage.Prefs.Settings.shakeEventEnabled
import com.moriak.schednote.storage.Prefs.Settings.snoozeTime
import com.moriak.schednote.storage.Prefs.Settings.timeFormat
import com.moriak.schednote.storage.Prefs.Settings.workWeek
import com.moriak.schednote.storage.Prefs.States.bgImage
import com.moriak.schednote.storage.Prefs.States.bgImageFit
import com.moriak.schednote.storage.Prefs.States.bgImagePos
import com.moriak.schednote.storage.Prefs.States.lastAlarmCategory
import com.moriak.schednote.storage.Prefs.States.lastMenuChoice
import com.moriak.schednote.storage.Prefs.States.lastScheduleDisplay
import com.moriak.schednote.storage.Prefs.States.lastScheduleStartAdvance
import com.moriak.schednote.storage.Prefs.States.lastSetBreakDuration
import com.moriak.schednote.storage.Prefs.States.lastSetLessonDuration
import com.moriak.schednote.storage.Prefs.States.schedulePos
import com.moriak.schednote.views.WallpaperView.ImageAngle
import com.moriak.schednote.views.WallpaperView.ImageFit
import java.lang.System.currentTimeMillis as now

/**
 *
 * Objekt [Prefs] zoskupuje objekty, ktoré pristupujú k pamäti [SharedPreferences].
 * Pri zápise a získavaní údajov sa nevyžaduje použitie kľúča a možno zapisovať
 * a získavať objekty viacerých typov.
 */
object Prefs {
    private const val FIRST_TIME_SCHEDULE = "FIRST_TIME_SCHEDULE"
    private const val FIRST_LESSON_TYPES = "FIRST_LESSON_TYPES"
    private const val FIRST_LESSON_SCHEDULE = "FIRST_LESSON_SCHEDULE"
    private const val FIRST_DESIGN = "FIRST_DESIGN"
    private const val FIRST_SUBJECTS = "FIRST_SUBJECTS"
    private const val FIRST_NOTES = "FIRST_NOTES"
    private const val FIRST_REMINDERS = "FIRST_REMINDERS"
    private const val FIRST_ALARM_CLOCKS = "FIRST_ALARM_CLOCKS"
    private const val FIRST_SEMESTER = "FIRST_SEMESTER"
    private const val FIRST_ALARM_TUNE = "FIRST_ALARM_TUNE"
    private const val LAST_ALARM_CATEGORY = "LAST_ALARM_CATEGORY"
    private const val LAST_MENU_CHOICE = "LAST_MENU_CHOICE"
    private const val LAST_SCHEDULE_DISPLAY = "LAST_SCHEDULE_DISPLAY"
    private const val LAST_NOTE_CATEGORY = "LAST_NOTE_CATEGORY"
    private const val BREAK_DUR = "BREAK_DUR"
    private const val LESSON_DUR = "LESSON_DUR"
    private const val DESIGN_BG_IMAGE = "DESIGN_BG_IMAGE"
    private const val DESIGN_IMG_DEG = "DESIGN_IMG_DEG"
    private const val DESIGN_IMG_FIT = "DESIGN_IMG_FIT"
    private const val DESIGN_IMG_POS = "DESIGN_IMG_POS"
    private const val DESIGN_TABLE_Y = "DESIGN_TABLE_Y"
    private const val LESSON_LABELING = "LESSON_LABELING"
    private const val DATE_FORMAT = "DATE_FORMAT"
    private const val TIME_FORMAT = "TIME_FORMAT"
    private const val ALARM_TONE = "ALARM_TUNE"
    private const val ALARM_SNOOZE = "ALARM_SNOOZE"
    private const val ALARM_TIME = "ALARM_TIME_"
    private const val ALARMS = "ALARMS"
    private const val REMINDER_ADVANCE = "REMINDER_ADVANCE"
    private const val REMINDER_ENABLED = "REMINDER_ENABLED"
    private const val DUAL_WEEK_ODDITY = "DUAL_WEEK_ODDITY"
    private const val EARLIEST_MINUTE = "EARLIEST_MINUTE"
    private const val SEMESTER_START = "SEMESTER_START"
    private const val SEMESTER_WEEK_COUNT = "SEMESTER_WEEK_COUNT"
    private const val WORK_WEEK = "WORK_WEEK"
    private const val NOTE_WIDGET = "NOTE_WIDGET"
    private const val SHAKE_EVENT_ENABLED = "SHAKE_EVENT_ENABLED"
    private const val SCHEDULE_START_ADVANCE = "SCHEDULE_START_ADVANCE"
    private const val WEEK_LENGTH = 604800000L

    /**
     * Objekt obsahuje funkcie, ktoré odbremeňujú používateľa od nutnosti vytvárať inštancie
     * [SharedPreferences] pre čítanie a [SharedPreferences.Editor] pre zápis. Do funkcie
     * vstupuje callback funkcia, do ktorej táto (už vytvorená) inštancia vstupuje ako callback.
     * funkcia podľa vloženého typu vie, akú funkciu vstupnej inštancie má zavolať pre zápis alebo
     * zisk konkrétnych dát.
     *
     * Chyby typu [ClassCastException], ktoré môžu vzniknúť pri zápise alebo získaní údaja,
     * ktorý je iného typu ako sa očakáva, sú už ošetrené.
     */
    abstract class PrefsControl {
        class InvalidTypeException : ClassCastException("Class type `T` not supported!")

        /*
         * Pod kanonickým názvom enum triedy je uložené pole jej inštancií, ktoré vo funkcii
         * [unsafeRestore] umožnia získať inštanciu [Enum], ktorej hodnota atribútu [Enum.ordinal]
         * sa zhoduje s číslom získaným z [SharedPreferences], pod nejakým kľúčom.
         */
        private val enumMap: Map<String, Array<out Enum<*>>> = mapOf(
            AlarmCategory::class.java.canonicalName to AlarmCategory.values(),
            DateFormat::class.java.canonicalName to DateFormat.values(),
            LessonTimeFormat::class.java.canonicalName to LessonTimeFormat.values(),
            ScheduleDisplay::class.java.canonicalName to ScheduleDisplay.values(),
            SubContent::class.java.canonicalName to SubContent.values(),
            TimeFormat::class.java.canonicalName to TimeFormat.values(),
            WorkWeek::class.java.canonicalName to WorkWeek.values(),
            ImageFit::class.java.canonicalName to ImageFit.values(),
            ImageAngle::class.java.canonicalName to ImageAngle.values()
        )
        private val sp get() = App.ctx.getSharedPreferences(javaClass.canonicalName, MODE_PRIVATE)

        protected fun <T> restore(fn: SharedPreferences.() -> T): T = sp.fn()
        protected fun store(fn: SharedPreferences.Editor.() -> SharedPreferences.Editor) = sp.edit().fn().apply()

        /**
         * Získa pole inštancií [Enum] ktoré sú typu triedy, ktorej kanonický názov je zhodný
         * zo vstupným reťazcom [str].
         * @param [str], kľúč pod ktorým získam pole hodnôt typu [Enum].
         * @throws InvalidTypeException Pole enumov sa nepodarilo získať.
         */
        protected fun findEnums(str: String?) = enumMap[str] ?: throw InvalidTypeException()

        /**
         * Získanie enum z úložiska [SharedPreferences] pod kľúčom [key].
         * @param key
         * @return inštancia typu [T], ktorý je potomkom inštancie [Enum]
         * @throws InvalidTypeException inštanciu typu [Enum] sa nepodarilo získať
         */
        protected inline fun <reified T> getEnum(key: String): T = restore {
            val enums = findEnums(T::class.java.canonicalName)
            enums[getInt(key, 0).coerceIn(enums.indices)] as T
        }

        /**
         * Porovnávanie hodnôt [Class.getCanonicalName] ojektov typov [T] a [U]
         * @return true, ak sa jedná presne o rovnaké typy
         */
        protected inline fun <reified T, reified U> exactlySame() =
            T::class.java.canonicalName == U::class.java.canonicalName

        /**
         * Získava údaj z trvalého úložiska aplikácie typu [T] pod kľúčom [key].
         * Chyba, ktorá nastane, keď pod kľúčom [T] je uložený údaj iného typu
         * ako [T], tu nie je ošetrená!
         * @param key Kľúč, pod ktorým je údaj dostupný
         * @return vráti uložený údaj alebo predvolenú hodnotu typu [T]
         * @throws ClassCastException pod kľúčom [T] je uložený údaj typu iného ako [T].
         * @throws InvalidTypeException zo získaných dát nie je možné vytvoriť údaj typu [T].
         * typ [T] môže zastupovať typy [Boolean], [Int], [Float], [Long], [String],
         * [Uri], [Set] a [Enum].
         *
         * Pre úspešnosť zísania hotnoty typu enum je nutné do atribútu [enumMap] uložiť
         * pole inštancií triedy typu [T] zastupujúcej [Enum] pod kľúčom v tvare jej
         * kanonického názvu [Class.getCanonicalName].
         */
        protected inline fun <reified T> unsafeRestore(key: String): T = restore {
            when {
                exactlySame<T, Boolean>() -> getBoolean(key, false) as T
                exactlySame<T, Int>() -> getInt(key, 0) as T
                exactlySame<T, Float>() -> getFloat(key, 0F) as T
                exactlySame<T, Long>() -> getLong(key, 0L) as T
                exactlySame<T, String>() -> getString(key, null) as T
                exactlySame<T, Uri>() -> getString(key, null)?.toUri() as T
                exactlySame<T, Set<String>>() -> getStringSet(key, null) as T
                T::class.java.isEnum -> getEnum(key)
                else -> throw InvalidTypeException()
            }
        }

        private fun <T> unsafeStore(key: String, value: T) = store {
            when (value) {
                null -> remove(key)
                is Boolean -> putBoolean(key, value)
                is Int -> putInt(key, value)
                is Float -> putFloat(key, value)
                is Long -> putLong(key, value)
                is String -> putString(key, value)
                is Uri -> putString(key, value.toString())
                is Set<*> -> putStringSet(key, value.map { "$it" }.toMutableSet())
                is Enum<*> -> putInt(key, value.ordinal)
                else -> throw InvalidTypeException()
            }
        }

        /**
         * Zísanie údaja typu [T] z pamäte [SharedPreferences].
         * @param key kľúč pod ktorým bude údaj uložený
         * @return údaj typu [T]
         * @throws InvalidTypeException keď zo získaneho údaja nie je možné vytvoriť objekt typu [T]
         */
        protected inline fun <reified T> restore(key: String): T = try {
            unsafeRestore(key)
        } catch (cce: ClassCastException) {
            store { remove(key) }
            unsafeRestore(key)
        }

        /**
         * Zísanie údaja typu [T] z pamäte [SharedPreferences]. Ak v pamäti taký údaj ešte nie je
         * uložený alebo nie je typu [T], tak vráti predvolený údaj [default].
         * @param key kľúč pod ktorým bude údaj uložený
         * @param default predvolený údaj
         * @return údaj typu [T]
         * @throws InvalidTypeException keď zo získaneho údaja nie je možné vytvoriť objekt typu [T]
         */
        protected inline fun <reified T> restore(key: String, default: T): T = restore {
            when (default) {
                null, is Boolean, is Int, is Float, is Long, is String, is Uri, is Set<*>, is Enum<*> -> {
                    if (contains(key)) try {
                        unsafeRestore(key)
                    } catch (cce: ClassCastException) {
                        if (cce is InvalidTypeException) throw cce
                        default
                    }
                    else default
                }
                else -> throw InvalidTypeException()
            }
        }

        /**
         * Uloží údaj typu [T] do pamäte [SharedPreferences] pod kľúčom [key].
         * @param key kľúč
         * @param value hodnota
         * @throws InvalidTypeException hodnota [value] je typu [T], ktorý nie je určený
         * na zápis do pamäte [SharedPreferences]
         */
        protected fun <T> store(key: String, value: T) = try {
            unsafeStore(key, value)
        } catch (cce: ClassCastException) {
            if (cce is InvalidTypeException) throw cce
            unsafeStore(key, null)
            unsafeStore(key, value)
        }
    }

    /**
     * Uchováva informáciu o poslednom zobrazenom obsahu, posledných zadávaných hodnotách a posledných úprav grafických prvkov
     *
     * @property lastMenuChoice Posledný zobrazený obsah v hlavnej aktivite (1. úroveň)
     * @property lastScheduleDisplay Posledný zobrazený obsah v hlavnej aktivite (2. úroveň - Rozvrh)
     * @property lastAlarmCategory Posledný zobrazený obsah v hlavnej aktivite (2. úroveň - Upozornenia)
     * @property lastSetLessonDuration Naposledy nastavená dĺžka hodiny
     * @property lastSetBreakDuration Naposledy nastavená dĺžka prestávky
     * @property lastScheduleStartAdvance Naposledy nastavený predstih budenia pred začiatkom vyučovania
     * @property schedulePos Pozícia tabuľky rozvrhu na plátne
     * @property bgImage Obrázok na pozadí plátna
     * @property bgImageFit Transformácia obrázku na plátne
     * @property bgImagePos Pozícia obrázku
     */
    object States : PrefsControl() {
        var lastMenuChoice: SubContent
            get() = restore(LAST_MENU_CHOICE, SubContent.SCHEDULE)
            set(value) = store(LAST_MENU_CHOICE, value)
        var lastScheduleDisplay: ScheduleDisplay
            get() = restore(LAST_SCHEDULE_DISPLAY, ScheduleDisplay.TIME_SCHEDULE)
            set(value) = store(LAST_SCHEDULE_DISPLAY, value)
        var lastAlarmCategory: AlarmCategory
            get() = restore(LAST_ALARM_CATEGORY, AlarmCategory.REMINDER)
            set(value) = store(LAST_ALARM_CATEGORY, value)
        var lastSetLessonDuration: Int
            get() = restore(LESSON_DUR, 45)
            set(value) = store(LESSON_DUR, value)
        var lastSetBreakDuration: Int
            get() = restore(BREAK_DUR, 10)
            set(value) = store(BREAK_DUR, value)
        var lastScheduleStartAdvance: Int
            get() = restore(SCHEDULE_START_ADVANCE)
            set(value) = store(SCHEDULE_START_ADVANCE, value)
        var schedulePos: Float
            get() = restore(DESIGN_TABLE_Y)
            set(value) = store(DESIGN_TABLE_Y, value)
        var bgImage: Uri?
            get() = restore(DESIGN_BG_IMAGE)
            set(value) = store(DESIGN_BG_IMAGE, value)
        var bgImageFit: ImageFit
            get() = restore(DESIGN_IMG_FIT, ImageFit.COVER)
            set(value) = store(DESIGN_IMG_FIT, value)
        var bgImageAngle: ImageAngle
            get() = restore(DESIGN_IMG_DEG, ImageAngle.ANGLE_360)
            set(value) = store(DESIGN_IMG_DEG, value)
        var bgImagePos: Float
            get() = restore(DESIGN_IMG_POS)
            set(value) = store(DESIGN_IMG_POS, value)
        var lastNoteCategory: Long
            get() = restore(LAST_NOTE_CATEGORY)
            set(value) = store(LAST_NOTE_CATEGORY, value)
    }

    /**
     * V tomto objekte zisťujeme, či daný fragment alebo aktivita boli užívateľovi zobrazené 1.-krát
     *
     * @property timeSchedule: Bol fragment nastavenia časového rozvrhu navštívený prvý krát?
     * @property lessonTypes: Bol fragment správy kategórií hodín navštívený prvý krát?
     * @property lessonSchedule: Bol fragment nastavenia rozvrhu hodín navštívený prvý krát?
     * @property design: Bol fragment nastavenia dizajnu rozvrhu hodín navštívený prvý krát?
     * @property subjects: Bol fragment správy predmetov navštívený prvý krát?
     * @property notes: Bol fragment správy úloh navštívený prvý krát?
     * @property reminders: Bol fragment nastavenia upozornení k úloham navštívený prvý krát?
     * @property alarmClocks: Bol fragment nastavenia budenia navštívený prvý krát?
     * @property semester: Bol fragment semester navštívený prvý krát?
     * @property alarmTune Bola aktivita nastavení tónov budenia navštívená prvý krát?
     */
    object FirstVisit : PrefsControl() {
        private fun firstVisit(key: String): Boolean =
            restore(key, true).also { if (it) store(key, false) }
        val timeSchedule: Boolean get() = firstVisit(FIRST_TIME_SCHEDULE)
        val lessonTypes: Boolean get() = firstVisit(FIRST_LESSON_TYPES)
        val lessonSchedule: Boolean get() = firstVisit(FIRST_LESSON_SCHEDULE)
        val design: Boolean get() = firstVisit(FIRST_DESIGN)
        val subjects: Boolean get() = firstVisit(FIRST_SUBJECTS)
        val notes: Boolean get() = firstVisit(FIRST_NOTES)
        val reminders: Boolean get() = firstVisit(FIRST_REMINDERS)
        val alarmClocks: Boolean get() = firstVisit(FIRST_ALARM_CLOCKS)
        val semester: Boolean get() = firstVisit(FIRST_SEMESTER)
        val alarmTune: Boolean get() = firstVisit(FIRST_ALARM_TUNE)
    }

    /**
     * Táto trieda uchováva informácie o prispôsobení aplikácie
     *
     * @property workWeek Pracovný týždeň
     * @property earliestMinute Začiatok rozvrhu
     * @property dualWeekSchedule Vypnutie / zapnutie 2-týždenného rozvrhu
     * @property lessonTimeFormat Nastavenie formátu zobrazovania času hodín
     * @property semesterStart Začiatok semestra - dátum v milisekundách - vždy prvý pracovný týždeň
     * @property semesterWeekCount Trvanie semestra v týždňoch
     * @property semesterWeek Zistí koľký je teraz týždeň od začiatku semestra
     * @property dateFormat Nastavenie formátu dátumu
     * @property timeFormat Nastavenie formátu času
     * @property alarmTone Nastavenie tónu budenia
     * @property snoozeTime Nastavenie času na odklad budenia
     * @property semesterValid Overenie, či je semester ešte platný, teda či má začiatok a či už neskončil
     * @property shakeEventEnabled Ak je táto funkcia zapnutá, po zatrasení zariadenia sa zjavi
     * dialóg, v ktorom po stlačení mikrofónu dať aplikácii príkaz, čo sa má spraviť
     */
    object Settings : PrefsControl() {
        var workWeek: WorkWeek
            get() = restore(WORK_WEEK, WorkWeek.MON_FRI)
            set(value) = store(WORK_WEEK, value)
        var earliestMinute: Int
            get() = restore(EARLIEST_MINUTE, 420)
            set(value) = store(EARLIEST_MINUTE, value.coerceIn(0 until 24 * 60))
        var dualWeekSchedule: Boolean
            get() = restore(DUAL_WEEK_ODDITY, false)
            set(dual) = store(DUAL_WEEK_ODDITY, dual)
        var lessonTimeFormat: LessonTimeFormat
            get() = restore(LESSON_LABELING, LessonTimeFormat.START_TIME)
            set(value) = store(LESSON_LABELING, value)
        var semesterStart: Long?
            get() = restore(SEMESTER_START, null)
            set(value) = store(SEMESTER_START, value)
        var semesterWeekCount: Int
            get() = restore(SEMESTER_WEEK_COUNT, 0)
            set(value) = store(SEMESTER_WEEK_COUNT, value)
        val semesterWeek get() = ((semesterStart?.let { now() - it } ?: 0L) / WEEK_LENGTH + 1).toInt()
        var dateFormat: DateFormat
            get() = restore(DATE_FORMAT, DateFormat.DMY_DOT)
            set(value) = store(DATE_FORMAT, value)
        var timeFormat: TimeFormat
            get() = restore(TIME_FORMAT, TimeFormat.H24)
            set(value) = store(TIME_FORMAT, value)

        var alarmTone: AlarmTone
            get(): AlarmTone = AlarmTone.seek(restore<Uri?>(ALARM_TONE, null))
            set(value) = store<Uri?>(ALARM_TONE, value.uri)

        var snoozeTime: Int
            get() = restore(ALARM_SNOOZE, 10)
            set(value) = store(ALARM_SNOOZE, value)

        val semesterValid: Boolean get() = semesterWeekCount > 0 && semesterStart?.let {
            now() < semesterWeekCount * WEEK_LENGTH + it
        } ?: false

        var shakeEventEnabled: Boolean
            get() = restore(SHAKE_EVENT_ENABLED, true)
            set(value) = store(SHAKE_EVENT_ENABLED, value)

        /**
         * Načítanie farby
         * @param group Farebná skupina
         * @param color Inštancia do ktorej sa výsledná farba uloží
         * @return [color]
         */
        fun getColor(group: ColorGroup, color: Palette = Palette()): Palette =
            restore<Int?>(group.name, null)?.let { color.setColor(it) } ?: color

        /**
         * Uloženie farby
         * @param group Farebná skupina
         * @param color Farba, ktorú sa snažím uložiť
         */
        fun setColor(group: ColorGroup, color: Palette) = store(group.name, color.color)
    }

    /**
     * Nastavuje konfiguráciu widgetov
     */
    object Widgets : PrefsControl() {
        /**
         * Zisťuje cieľovú kategóriu poznámkového widgetu
         * @param widgetId ID widgetu
         * @return Kategória ktorú treba načítať
         */
        fun getNoteWidgetCategory(widgetId: Int): Long = restore(NOTE_WIDGET + widgetId)

        /**
         * Nastaví konfigurovanému widgetu poznámok kategóriu poznámok, ktoré má zobrazovať
         * @param id ID widgetu ktorého kategóriu sa snažím uložiť
         * @param category Kategóriu, ktorú widgetu nastavím. Hodnota null zmaže informáciu o widgete
         */
        fun setNoteWidgetCategory(id: Int, category: Long?) = store(NOTE_WIDGET + id, category)

    }

    /**
     * Uchováva nastavenia upozornení
     *
     * @property reminderEnabled true, ak sú upozornenia na úlohy zapnuté, inak false
     * @property reminderAdvanceInMinutes Predstih upozornenia na dokončenie úlohy
     * @property bits Postupnosť bitovo reprezentovaných budíkov, pričom 1 znamená, že budík je zapnutý.
     */
    object Notifications : PrefsControl() {
        /**
         * Zisťuje a mení časy budenia jednotlivých budíkov
         */
        object AlarmTimes {
            private val array: IntArray by lazy {
                restore<String?>(ALARM_TIME)?.split(";")?.map { it.toInt() }
                    ?.toIntArray() ?: IntArray(21) { 360 }
            }

            /**
             * Nastavenie budíka
             * @param key Budík, ktorý treba nastaviť
             * @param value čas v minútach v dni
             */
            operator fun set(key: AlarmClockBit, value: Int) {
                array[key.ordinal] = value
                store(ALARM_TIME, array.joinToString(";"))
            }

            /**
             * Zístenie času budenia
             * @param key Budík, ktorý nás zaujíma
             * @return čas budenia v dňových sekundách
             */
            operator fun get(key: AlarmClockBit): Int = array[key.ordinal]
        }
        var reminderEnabled: Boolean get() = restore(REMINDER_ENABLED, true)
            set(value) = store(REMINDER_ENABLED, value)

        var reminderAdvanceInMinutes: Int get() = restore(REMINDER_ADVANCE, 0)
            set(value) = store(REMINDER_ADVANCE, value)

        var bits: Int get() = restore(ALARMS)
            set(value) = store(ALARMS, value)
    }
}