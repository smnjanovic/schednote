package com.moriak.schednote.settings

import android.content.Context
import android.content.SharedPreferences
import com.moriak.schednote.App
import com.moriak.schednote.database.data.NoteCategory
import com.moriak.schednote.database.data.Subject
import com.moriak.schednote.design.Palette
import com.moriak.schednote.fragments.of_schedule.DesignEditor
import com.moriak.schednote.menu.AlarmCategory
import com.moriak.schednote.menu.ScheduleDisplay
import com.moriak.schednote.menu.SubContent
import com.moriak.schednote.notifications.ClockReceiver
import com.moriak.schednote.other.Day
import com.moriak.schednote.other.TimeCategory
import com.moriak.schednote.settings.Prefs.firstVisit
import com.moriak.schednote.settings.Prefs.notifications
import com.moriak.schednote.settings.Prefs.settings
import com.moriak.schednote.settings.Prefs.states
import com.moriak.schednote.settings.Prefs.widgets
import com.moriak.schednote.widgets.ScheduleWidget
import java.util.Calendar.DAY_OF_MONTH

/**
 * Objekt slúži na zjednodušenie zápisu a čítania dát zo súborov zdielaných preferencií [SharedPreferences].
 * Je tu niekoľko vnorených tried, pričom každá z nich reprezentuje jeden samostatný súbor preferencií
 * s nejakými hodnotami. Vďaka getterom a setterom atribútov inštancií týchto tried si nemusím pamätať
 * kľúč pod akým ukladám hodnoty a už nie som ani obmedzený na hodnoty primitívnych typov alebo typu
 * String a StringSet.
 *
 * Tento súbor je znovupoužiteľný pre budúce projekty vyvíjané v androide.
 *
 * @property firstVisit Slúži na zistenie, či bola aktivita, fragment zobrazené prvý krát alebo či sa konkétna vec vykonanáva prvý krát
 * @property states Uchováva informáciu o poslednom zobrazenom obsahu, posledných zadávaných hodnotách a posledných úprav grafických prvkov
 * @property settings Táto trieda uchováva informácie o prispôsobení aplikácie
 * @property widgets Obsahuje konfigurácie widgetov
 * @property notifications Uchováva nastavenia upozornení
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
    private const val BREAK_DUR = "BREAK_DUR"
    private const val LESSON_DUR = "LESSON_DUR"
    private const val DESIGN_BG_IMAGE = "DESIGN_BG_IMAGE"
    private const val DESIGN_IMG_FIT = "DESIGN_IMG_FIT"
    private const val DESIGN_IMG_POS = "DESIGN_IMG_POS"
    private const val DESIGN_TABLE_Y = "DESIGN_TABLE_Y"
    private const val LESSON_LABELING = "LESSON_LABELING"
    private const val DATE_ORDER = "DATE_ORDER"
    private const val TIME_FORMAT = "TIME_FORMAT"
    private const val DATE_SEPARATOR = "DATE_SEPARATOR"
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

    val firstVisit = FirstVisit()
    val states = States()
    val settings = Settings()
    val widgets = Widgets()
    val notifications = Notifications()

    /**
     * Umožňuje ukladanie a získavanie preferencii cez gettery a settery cez jednoduchšie metódy
     * dostupné z tejto abstraktnej triedy
     */
    abstract class PrefsControl {
        private val prefs by lazy {
            App.ctx.getSharedPreferences(
                this.javaClass.name,
                Context.MODE_PRIVATE
            )
        }

        private fun <T> restore(fn: (SharedPreferences) -> T): T = fn(prefs)
        private fun <T> store(fn: (SharedPreferences.Editor) -> T) =
            prefs.edit().also { fn(it) }.apply()

        /**
         * Zjednodušené ukladanie hodnôt typu [T]. Typ [T] nesmie byť nič iné ako primitívny typ
         * Reťazec alebo rad reťazcov (Set).
         *
         * @param key značka pod ktorou sa hodnota uloží
         * @param value Vkladaná hodnota typu [T]. Keď je null, prvok s označením [key] bude odstránený
         * @throws ClassCastException Keď nenulová hodnota [value] je iných typov, ako:
         * [Boolean], [Float], [Int], [Long], [String], [Set<String>].
         */
        protected fun <T> store(key: String, value: T?) = store {
            when (value) {
                is Set<*> -> it.putStringSet(key, value.filterIsInstance<String?>().toSet())
                is String -> it.putString(key, value)
                is Int -> it.putInt(key, value)
                is Float -> it.putFloat(key, value)
                is Long -> it.putLong(key, value)
                is Boolean -> it.putBoolean(key, value)
                null -> it.remove(key)
                else -> throw ClassCastException("Type isn't allowed in SharedPreferences!")
            }
        }

        /**
         * Zjednodušené ukladanie hodnôt typu [T]. Typ [T] nesmie byť nič iné ako primitívny typ
         * Reťazec alebo rad reťazcov (Set).
         *
         * @param key značka pod ktorou sa hodnota nájde
         * @param default predvolená hodnota, ak pod značkou [key] žiadna hodnota neexistuje
         * @throws ClassCastException Príčiny:
         *  Keď nenulová hodnota [default] je iných typov, ako: [Boolean], [Float], [Int], [Long], [String], [Set<String>]
         *  Keď je pod kľúčom [key] vložená hodnota iného typu ako [T]
         */
        protected fun <T> restore(key: String, default: T? = null): T? = restore {
            val reason = "No value of such type can be in SharedPreferences!"
            @Suppress("UNCHECKED_CAST")
            when (default) {
                null -> it.all[key] as T?
                is Set<*> -> {
                    default.find { s -> s !is String? }?.let { throw ClassCastException(reason) }
                    it.getStringSet(key, default as Set<String>) as T
                }
                is String -> it.getString(key, default) as T
                is Int -> it.getInt(key, default) as T
                is Float -> it.getFloat(key, default) as T
                is Long -> it.getLong(key, default) as T
                is Boolean -> it.getBoolean(key, default) as T
                else -> throw ClassCastException(reason)
            }
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
     * @property tableY Pozícia tabuľky rozvrhu na plátne
     * @property bgImage Obrázok na pozadí plátna
     * @property bgImageFit Transformácia obrázku na plátne
     * @property bgImagePos Pozícia obrázku
     */
    class States : PrefsControl() {
        var lastMenuChoice: SubContent
            get() = SubContent.giveEnum(restore<String>(LAST_MENU_CHOICE))
            set(value) = store(LAST_MENU_CHOICE, value.name)
        var lastScheduleDisplay: ScheduleDisplay
            get() = ScheduleDisplay[restore<String>(LAST_SCHEDULE_DISPLAY)]
                ?: ScheduleDisplay.TIME_SCHEDULE
            set(value) = store(LAST_SCHEDULE_DISPLAY, value.name)
        var lastAlarmCategory: AlarmCategory
            get() = AlarmCategory[restore<String>(LAST_ALARM_CATEGORY)] ?: AlarmCategory.REMINDER
            set(value) = store(LAST_ALARM_CATEGORY, value.name)
        var lastSetLessonDuration
            get() = restore(LESSON_DUR, 45)!!
            set(value) = store(LESSON_DUR, value)
        var lastSetBreakDuration
            get() = restore(BREAK_DUR, 10)!!
            set(value) = store(BREAK_DUR, value)
        var lastScheduleStartAdvance
            get() = restore(SCHEDULE_START_ADVANCE, 0)!!
            set(value) = store(SCHEDULE_START_ADVANCE, value)
        var tableY
            get() = restore(DESIGN_TABLE_Y, 0F)!!
            set(value) = store(DESIGN_TABLE_Y, value)

        var bgImage
            get() = restore<String>(DESIGN_BG_IMAGE)
            set(value) = store(DESIGN_BG_IMAGE, value)
        var bgImageFit
            get() = DesignEditor.ImgFit[restore<String>(DESIGN_IMG_FIT)]
            set(value) = store(DESIGN_IMG_FIT, value?.toString())
        var bgImagePos
            get() = restore(DESIGN_IMG_POS, 0F)!!
            set(value) = store(DESIGN_IMG_POS, value)
    }

    /**
     * Trieda uchováva informáciu o tom, ktoré veci robí užívateľ prvý krát. Napr. koľký krát
     * zobrazil layout nejakého fragmentu / aktivity
     *
     * @property alarmTune Údáva informáciu o tom, či aktivita nastavení tónov budenia bola navštívená
     */
    class FirstVisit : PrefsControl() {
        private var timeSchedule: Boolean
            get() = restore(FIRST_TIME_SCHEDULE, true)!!
            set(value) = store(FIRST_TIME_SCHEDULE, value)
        private var lessonTypes: Boolean
            get() = restore(FIRST_LESSON_TYPES, true)!!
            set(value) = store(FIRST_LESSON_TYPES, value)
        private var lessonSchedule: Boolean
            get() = restore(FIRST_LESSON_SCHEDULE, true)!!
            set(value) = store(FIRST_LESSON_SCHEDULE, value)
        private var design: Boolean
            get() = restore(FIRST_DESIGN, true)!!
            set(value) = store(FIRST_DESIGN, value)
        private var subjects: Boolean
            get() = restore(FIRST_SUBJECTS, true)!!
            set(value) = store(FIRST_SUBJECTS, value)
        private var notes: Boolean
            get() = restore(FIRST_NOTES, true)!!
            set(value) = store(FIRST_NOTES, value)
        private var reminders: Boolean
            get() = restore(FIRST_REMINDERS, true)!!
            set(value) = store(FIRST_REMINDERS, value)
        private var alarmClocks: Boolean
            get() = restore(FIRST_ALARM_CLOCKS, true)!!
            set(value) = store(FIRST_ALARM_CLOCKS, value)
        private var semester: Boolean
            get() = restore(FIRST_SEMESTER, true)!!
            set(value) = store(FIRST_SEMESTER, value)
        var alarmTune: Boolean
            get() = restore(FIRST_ALARM_TUNE, true)!!
            set(value) = store(FIRST_ALARM_TUNE, value)

        private fun wasVisited(display: ScheduleDisplay) = when (display) {
            ScheduleDisplay.DESIGN -> design
            ScheduleDisplay.LESSON_SCHEDULE -> lessonSchedule
            ScheduleDisplay.TIME_SCHEDULE -> timeSchedule
            ScheduleDisplay.LESSON_TYPES -> lessonTypes
        }

        private fun wasVisited(alarmCategory: AlarmCategory) = when (alarmCategory) {
            AlarmCategory.REMINDER -> reminders
            AlarmCategory.ALARM -> alarmClocks
        }

        /**
         * Zistím či daný fragment alebo aktivita boli už navštívené
         * @param subContent Inštancia, ktorý vyberie, ktorý fragment treba skontrolovať
         * @return true, ak je aktivita načítaná prvý krát
         */
        fun wasVisited(subContent: SubContent) = when (subContent) {
            SubContent.SCHEDULE -> wasVisited(states.lastScheduleDisplay)
            SubContent.SUBJECTS -> subjects
            SubContent.NOTES -> notes
            SubContent.ALARMS -> wasVisited(states.lastAlarmCategory)
            SubContent.SEMESTER -> semester
        }

        private fun notifyVisited(display: ScheduleDisplay) = when (display) {
            ScheduleDisplay.DESIGN -> design = false
            ScheduleDisplay.LESSON_SCHEDULE -> lessonSchedule = false
            ScheduleDisplay.TIME_SCHEDULE -> timeSchedule = false
            ScheduleDisplay.LESSON_TYPES -> lessonTypes = false
        }

        private fun notifyVisited(alarmCategory: AlarmCategory) = when (alarmCategory) {
            AlarmCategory.REMINDER -> reminders = false
            AlarmCategory.ALARM -> alarmClocks = false
        }

        /**
         * Fragment vybraný objektom [subContent] označím ako už zobrazený. Keď má tento fragment
         * vnorené ďalšie fragmenty, posledný navštívený alebo predvolený z nich sa nastavia ako
         * už zobrazené.
         * @param subContent Inštancia, ktorá určuje fragment, ktorý sa označí ako zobrazený
         */
        fun notifyVisited(subContent: SubContent) = when (subContent) {
            SubContent.SCHEDULE -> notifyVisited(states.lastScheduleDisplay)
            SubContent.SUBJECTS -> subjects = false
            SubContent.NOTES -> notes = false
            SubContent.ALARMS -> notifyVisited(states.lastAlarmCategory)
            SubContent.SEMESTER -> semester = false
        }
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
     * @property dateOrder Nastavenie poradia prvkov dátumu: rok, mesiac, deň
     * @property dateSeparator Nastavenie oddeľovača prvkov dátumu
     * @property timeFormat Nastavenie formátu času
     * @property alarmTone Nastavenie tónu budenia
     * @property snooze Nastavenie času na odklad budenia
     * @property semesterValid Overenie, či je semester ešte platný, teda či má začiatok a či už neskončil
     * @property shakeEventEnabled Ak je táto funkcia zapnutá, po zatrasení zariadenia sa zjavi
     * dialóg, v ktorom po stlačení mikrofónu dať aplikácii príkaz, čo sa má spraviť
     */
    class Settings : PrefsControl() {
        var workWeek: WorkWeek
            get() = restore<String>(WORK_WEEK)?.let { WorkWeek[it] } ?: WorkWeek.MON_FRI
            set(value) {
                if (workWeek != value) {
                    store(WORK_WEEK, value.name)
                    notifications.alarmsEnabled = notifications.alarmsEnabled
                }
                ScheduleWidget.update()
            }
        var earliestMinute: Int
            get() = restore(EARLIEST_MINUTE, 7 * 60)!!
            set(value) {
                store(EARLIEST_MINUTE, value.coerceIn(0 until 24 * 60))
                if (lessonTimeFormat == LessonTimeFormat.START_TIME) ScheduleWidget.update()
            }
        var dualWeekSchedule: Boolean
            get() = restore(DUAL_WEEK_ODDITY, false)!!
            set(dual) {
                if (dual != dualWeekSchedule) {
                    store(DUAL_WEEK_ODDITY, dual)
                    notifications.alarmsEnabled = notifications.alarmsEnabled
                }
                ScheduleWidget.update()
            }
        var lessonTimeFormat: LessonTimeFormat
            get() = LessonTimeFormat[restore<String>(LESSON_LABELING)]
                ?: LessonTimeFormat.ORDER_FROM_1
            set(value) {
                store(LESSON_LABELING, value.name)
                ScheduleWidget.update()
            }
        var semesterStart: Long?
            get() = restore<Long>(SEMESTER_START)
            set(value) = store(SEMESTER_START, value)
        var semesterWeekCount: Int
            get() = restore(SEMESTER_WEEK_COUNT, 0)!!
            set(value) = store(SEMESTER_WEEK_COUNT, value)
        var dateOrder: DateOrder
            get() = restore<String>(DATE_ORDER)?.let { DateOrder[it] } ?: DateOrder.DMY
            set(value) = store(DATE_ORDER, value.name)
        var dateSeparator: DateSeparator
            get() = restore<String>(DATE_SEPARATOR)?.let { DateSeparator[it] } ?: DateSeparator.DOT
            set(value) = store(DATE_SEPARATOR, value.name)
        var timeFormat: TimeFormat
            get() = TimeFormat[restore(TIME_FORMAT, 24)!!]
            set(value) {
                store(TIME_FORMAT, value.hourFormat)
                if (lessonTimeFormat == LessonTimeFormat.START_TIME) ScheduleWidget.update()
            }

        var alarmTone: AlarmTone
            get(): AlarmTone {
                val id = restore(ALARM_TONE, -1L)!!
                val tone = AlarmTone(id)
                if (tone.id != id) store(ALARM_TONE, -1L)
                return tone
            }
            set(value) {
                store(ALARM_TONE, value.id)
            }

        var snooze: Int
            get() = restore(ALARM_SNOOZE, 10)!!
            set(value) = store(ALARM_SNOOZE, value)

        val semesterValid: Boolean
            get() = if (semesterStart != null && semesterWeekCount in 1..52) {
                System.currentTimeMillis() < App.cal.let {
                    it.timeInMillis = semesterStart!!
                    App.cal.add(DAY_OF_MONTH, semesterWeekCount * 7)
                    App.cal.timeInMillis
                }
            } else false

        var shakeEventEnabled: Boolean
            get() = restore(SHAKE_EVENT_ENABLED, true)!!
            set(value) = store(SHAKE_EVENT_ENABLED, value)

        /**
         * Načítanie farby
         * @param group Farebná skupina
         * @param color Inštancia do ktorej sa výsledná farba uloží
         * @return [color]
         */
        fun getColor(group: ColorGroup, color: Palette = Palette()): Palette =
            restore<String>(group.name)?.let { color.ahex(it) } ?: color.set(group.default)

        /**
         * Uloženie farby
         * @param group Farebná skupina
         * @param color Farba, ktorú sa snažím uložiť
         */
        fun setColor(group: ColorGroup, color: Palette) {
            store(group.name, color.ahex)
            ScheduleWidget.update()
        }

        /**
         * Zistiť, koľký je semestrálny týždeň v čase dátumu uvedeného v milisekundách [ms]
         * @param ms Dátum uvedený v milisekundách
         * @return Poradie semestrálneho týždňa
         */
        fun semesterWeek(ms: Long): Int = when {
            !semesterValid -> 0
            else -> ((ms - semesterStart!!) / (7 * 24 * 60 * 60 * 1000)).toInt()
                .coerceIn(0 until semesterWeekCount)
        }

        /**
         * Výpis času podľa nastaveného formátu zobrazenia času
         * @param dayMinutes uplynulé minúty za deň
         * @return Čas v nastavenom formáte
         */
        fun getTimeString(dayMinutes: Int): String = timeFormat
            .getFormat(dayMinutes / 60, dayMinutes % 60)

        /**
         * Výpis dátumu a času podľa nastaveného formátu
         * @param millis dátum udávaný v milisekundách
         * @return Dátum a čas v nastavenom formáte
         */
        fun getDateTimeString(millis: Long) = String.format(
            "%s %s", dateOrder
                .getFormat(dateSeparator, millis), timeFormat.getFormat(millis)
        )
    }

    /**
     * Pristupuje ku konfiguráciam widgetov
     */
    class Widgets : PrefsControl() {
        /**
         * Zisťuje cieľovú kategóriu poznámkového widgetu
         * @param widgetId ID widgetu
         * @return Kategória ktorú treba načítať
         */
        fun getNoteWidgetCategory(widgetId: Int): NoteCategory {
            val catId = restore<Long>(NOTE_WIDGET + widgetId) ?: return TimeCategory.ALL
            return if (catId > 0L) App.data.subject(catId) ?: TimeCategory.ALL
            else TimeCategory.values().find { -it.ordinal.toLong() == catId } ?: TimeCategory.ALL
        }

        /**
         * Nastaví konfigurovanému widgetu poznámok kategóriu poznámok, ktoré má zobrazovať
         * @param widgetId ID widgetu ktorého kategóriu sa snažím uložiť
         * @param noteCategory Kategóriu, ktorú widgetu nastavím. Hodnota null zmaže informáciu o widgete
         */
        fun setNoteWidgetCategory(widgetId: Int, noteCategory: NoteCategory?) {
            store(NOTE_WIDGET + widgetId, noteCategory?.let {
                when (it) {
                    is TimeCategory -> -it.ordinal.toLong()
                    is Subject -> it.id
                    else -> null
                }
            })
        }
    }

    /**
     * Uchováva nastavenia upozornení
     *
     * @property reminderAdvanceInMinutes Predstih upozornenia na dokončenie úlohy
     * @property reminderEnabled true, ak sú upozornenia na úlohy zapnuté
     * @property alarmsEnabled true, ak je aspoň 1 budík zapnutý
     */
    class Notifications : PrefsControl() {
        var reminderAdvanceInMinutes: Int
            get() = restore(REMINDER_ADVANCE, 0)!!
            set(value) = store(REMINDER_ADVANCE, value)

        var reminderEnabled: Boolean
            get() = restore(REMINDER_ENABLED, false)!!
            set(value) = store(REMINDER_ENABLED, value)

        private var bits: Int
            get() = restore(ALARMS, 0)!!
            set(value) = store(ALARMS, value)

        var alarmsEnabled: Boolean
            get() = bits > 0
            set(value) {
                bits = 0
                if (value) {
                    var ww = 0
                    for (day in settings.workWeek.workDay) ww = ww.or(1.shl(day.value - 1))
                    bits = if (settings.dualWeekSchedule) bits.or(
                        ww.shl(7).or(ww)
                    ) else bits.or(ww.shl(14))
                }
                reEnableAlarm()
            }

        /**
         * Obnova budíkov
         */
        fun reEnableAlarm() {
            for (reg in Regularity.values())
                for (day in Day.values())
                    ClockReceiver.enableAlarmClock(day, reg, isAlarmEnabled(day, reg))
        }

        private fun key(reg: Regularity, day: Day): Int =
            1.shl((reg.odd?.let { if (it) 1 else 0 } ?: 2) * 7 + day.value - 1)

        private fun alarmSetKey(day: Day, regularity: Regularity) =
            ALARM_TIME + key(regularity, day)

        private fun setAlarm(day: Day, regularity: Regularity, dayMinutes: Int) {
            if (dayMinutes !in 0 until 24 * 60) throw Exception("Invalid time data $dayMinutes!")
            store(alarmSetKey(day, regularity), dayMinutes)
            if (isAlarmEnabled(day, regularity))
                ClockReceiver.enableAlarmClock(day, regularity)
        }

        /**
         * Nastavenie času budíka
         * @param day Deň budenia
         * @param regularity Interval opakovania budenia
         * @param hour24format čas v hodinách (24)
         * @param minute čas v minútach
         */
        fun setAlarm(day: Day, regularity: Regularity, hour24format: Int, minute: Int) =
            setAlarm(day, regularity, hour24format * 60 + minute)

        /**
         * Získanie času budenia
         * @param day Deň
         * @param reg Interval opakovania budenia
         * @return Čas budenia v minútach dňa
         */
        fun getAlarm(day: Day, reg: Regularity) = restore(alarmSetKey(day, reg), 360)!!

        /**
         * Zapnutie budíka
         * @param day Deň budenia
         * @param reg Pravidelnosť budenia
         * @param enable Zapnúť = true, Vypnúť = false
         */
        fun enableAlarm(day: Day, reg: Regularity, enable: Boolean) {
            bits = if (enable) bits.or(key(reg, day))
            else bits.and(key(reg, day).xor(Int.MAX_VALUE))
            ClockReceiver.enableAlarmClock(day, reg, enable)
        }

        /**
         * Zistiť, či konkrétny budík je zapnutý alebo nie
         * @param day deň budenia
         * @param reg interval opakovania budenia
         * @return true, ak je Zapnutý
         */
        fun isAlarmEnabled(day: Day, reg: Regularity): Boolean = key(reg, day).and(bits) > 0

        /**
         * Nastaviť všetky budíky v určitom predstihu, pred začiatkom vyučovania
         */
        fun setAlarmsBySchedule() {
            for (reg in Regularity.values()) {
                val regValid = (reg == Regularity.EVERY).xor(settings.dualWeekSchedule)
                val busyDays = App.data.getBusyDays(reg, settings.workWeek)
                for ((day, busy) in busyDays) {
                    val time = App.data.firstLessonStart(day, reg) - states.lastScheduleStartAdvance
                    setAlarm(day, reg, time.coerceIn(0 until 24 * 60))
                    enableAlarm(day, reg, regValid && busy)
                }
            }
        }
    }
}