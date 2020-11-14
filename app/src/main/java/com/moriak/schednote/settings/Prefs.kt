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
import com.moriak.schednote.widgets.ScheduleWidget
import java.util.Calendar.DAY_OF_MONTH

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

    // ukladanie a ziskavanie preferencii cez gettery a settery cez jednoduchsie metody dostupne z abstraktnej triedy
    abstract class PrefsControl {
        private val prefs by lazy {
            App.ctx.getSharedPreferences(
                this.javaClass.name,
                Context.MODE_PRIVATE
            )
        }

        private fun <T> restore(fn: (SharedPreferences) -> T): T = fn(prefs)
        private fun <T> store(fn: (SharedPreferences.Editor) -> T) =
            prefs.edit().let { fn(it); it.apply() }

        protected fun <T> store(underKey: String, value: T) = store {
            when (value) {
                is String? -> it.putString(underKey, value)
                is Int -> it.putInt(underKey, value)
                is Float -> it.putFloat(underKey, value)
                is Long -> it.putLong(underKey, value)
                is Boolean -> it.putBoolean(underKey, value)
                else -> throw ClassCastException("Type isn't allowed in SharedPreferences!")
            }
        }

        protected fun has(underKey: String) = prefs.contains(underKey)
        protected fun clear(underKey: String) = store { it.remove(underKey) }

        //protected fun store(underKey: String, value: Set<String>?) = store { it.putStringSet(underKey, value) }
        //protected fun stringSet(underKey: String, default: Set<String>? = null): MutableSet<String>? = restore { it.getStringSet(underKey, default) }
        protected fun string(underKey: String, default: String? = null) =
            restore { it.getString(underKey, default) }

        protected fun int(underKey: String, default: Int = 0) =
            restore { it.getInt(underKey, default) }

        protected fun float(underKey: String, default: Float = 0F) =
            restore { it.getFloat(underKey, default) }

        protected fun long(underKey: String, default: Long = 0L) =
            restore { it.getLong(underKey, default) }

        protected fun bool(underKey: String, default: Boolean = false) =
            restore { it.getBoolean(underKey, default) }
    }

    class States : PrefsControl() {
        var lastMenuChoice: SubContent
            get() = SubContent.giveEnum(string(LAST_MENU_CHOICE))
            set(value) = store(LAST_MENU_CHOICE, value.name)
        var lastScheduleDisplay: ScheduleDisplay
            get() = ScheduleDisplay[string(LAST_SCHEDULE_DISPLAY)] ?: ScheduleDisplay.TIME_SCHEDULE
            set(value) = store(LAST_SCHEDULE_DISPLAY, value.name)
        var lastAlarmCategory: AlarmCategory
            get() = AlarmCategory[string(LAST_ALARM_CATEGORY)] ?: AlarmCategory.REMINDER
            set(value) = store(LAST_ALARM_CATEGORY, value.name)
        var lastSetLessonDuration
            get() = int(LESSON_DUR, 45)
            set(value) = store(LESSON_DUR, value)
        var lastSetBreakDuration
            get() = int(BREAK_DUR, 10)
            set(value) = store(BREAK_DUR, value)
        var lastScheduleStartAdvance
            get() = int(SCHEDULE_START_ADVANCE, 0)
            set(value) = store(SCHEDULE_START_ADVANCE, value)
        var tableY
            get() = float(DESIGN_TABLE_Y, 0F)
            set(value) = store(DESIGN_TABLE_Y, value)

        var bgImage
            get() = string(DESIGN_BG_IMAGE)
            set(value) {
                store(DESIGN_BG_IMAGE, value)
            }
        var bgImageFit
            get() = DesignEditor.ImgFit[string(DESIGN_IMG_FIT)]
            set(value) = store(DESIGN_IMG_FIT, value?.toString())
        var bgImagePos
            get() = float(DESIGN_IMG_POS, 0F)
            set(value) = store(DESIGN_IMG_POS, value)
    }

    class FirstVisit : PrefsControl() {
        private var timeSchedule: Boolean
            get() = bool(FIRST_TIME_SCHEDULE, true)
            set(value) = store(FIRST_TIME_SCHEDULE, value)
        private var lessonTypes: Boolean
            get() = bool(FIRST_LESSON_TYPES, true)
            set(value) = store(FIRST_LESSON_TYPES, value)
        private var lessonSchedule: Boolean
            get() = bool(FIRST_LESSON_SCHEDULE, true)
            set(value) = store(FIRST_LESSON_SCHEDULE, value)
        private var design: Boolean
            get() = bool(FIRST_DESIGN, true)
            set(value) = store(FIRST_DESIGN, value)
        private var subjects: Boolean
            get() = bool(FIRST_SUBJECTS, true)
            set(value) = store(FIRST_SUBJECTS, value)
        private var notes: Boolean
            get() = bool(FIRST_NOTES, true)
            set(value) = store(FIRST_NOTES, value)
        private var reminders: Boolean
            get() = bool(FIRST_REMINDERS, true)
            set(value) = store(FIRST_REMINDERS, value)
        private var alarmClocks: Boolean
            get() = bool(FIRST_ALARM_CLOCKS, true)
            set(value) = store(FIRST_ALARM_CLOCKS, value)
        private var semester: Boolean
            get() = bool(FIRST_SEMESTER, true)
            set(value) = store(FIRST_SEMESTER, value)
        var alarmTune: Boolean
            get() = bool(FIRST_ALARM_TUNE, true)
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

        fun notifyVisited(subContent: SubContent) = when (subContent) {
            SubContent.SCHEDULE -> notifyVisited(states.lastScheduleDisplay)
            SubContent.SUBJECTS -> subjects = false
            SubContent.NOTES -> notes = false
            SubContent.ALARMS -> notifyVisited(states.lastAlarmCategory)
            SubContent.SEMESTER -> semester = false
        }
    }

    class Settings : PrefsControl() {
        var workWeek: WorkWeek
            get() = string(WORK_WEEK)?.let { WorkWeek[it] } ?: WorkWeek.MON_FRI
            set(value) {
                if (workWeek != value) {
                    store(WORK_WEEK, value.name)
                    notifications.alarmsEnabled = notifications.alarmsEnabled
                }
                ScheduleWidget.update()
            }
        var earliestMinute: Int
            get() = int(EARLIEST_MINUTE, 7 * 60)
            set(value) {
                store(EARLIEST_MINUTE, value.coerceIn(0 until 24 * 60))
                if (lessonTimeFormat == LessonTimeFormat.START_TIME) ScheduleWidget.update()
            }
        var dualWeekSchedule: Boolean
            get() = bool(DUAL_WEEK_ODDITY, false)
            set(dual) {
                if (dual != dualWeekSchedule) {
                    store(DUAL_WEEK_ODDITY, dual)
                    notifications.alarmsEnabled = notifications.alarmsEnabled
                }
                ScheduleWidget.update()
            }
        var lessonTimeFormat: LessonTimeFormat
            get() = LessonTimeFormat[string(LESSON_LABELING)] ?: LessonTimeFormat.ORDER_FROM_1
            set(value) {
                store(LESSON_LABELING, value.name)
                ScheduleWidget.update()
            }
        var semesterStart: Long?
            get() = if (has(SEMESTER_START)) long(SEMESTER_START) else null
            set(value) = value?.let { store(SEMESTER_START, it) } ?: clear(SEMESTER_START)
        var semesterWeekCount: Int
            get() = int(SEMESTER_WEEK_COUNT, 0)
            set(value) = store(SEMESTER_WEEK_COUNT, value)
        var dateOrder: DateOrder
            get() = string(DATE_ORDER)?.let { DateOrder[it] } ?: DateOrder.DMY
            set(value) = store(DATE_ORDER, value.name)
        var dateSeparator: DateSeparator
            get() = string(DATE_SEPARATOR)?.let { DateSeparator[it] } ?: DateSeparator.DOT
            set(value) = store(DATE_SEPARATOR, value.name)
        var timeFormat: TimeFormat
            get() = TimeFormat[int(TIME_FORMAT, 24)]
            set(value) {
                store(TIME_FORMAT, value.hourFormat)
                if (lessonTimeFormat == LessonTimeFormat.START_TIME) ScheduleWidget.update()
            }

        var alarmTone: AlarmTone
            get(): AlarmTone {
                val id = long(ALARM_TONE, -1L)
                val tone = AlarmTone(id)
                if (tone.id != id) store(ALARM_TONE, -1L)
                return tone
            }
            set(value) {
                store(ALARM_TONE, value.id)
            }

        var snooze: Int
            get() = int(ALARM_SNOOZE, 10)
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
            get() = bool(SHAKE_EVENT_ENABLED, true)
            set(value) = store(SHAKE_EVENT_ENABLED, value)

        fun getColor(group: ColorGroup, color: Palette = Palette()): Palette =
            string(group.name)?.let { color.ahex(it) } ?: color.set(group.default)

        fun setColor(group: ColorGroup, color: Palette) {
            store(group.name, color.ahex)
            ScheduleWidget.update()
        }

        fun semesterWeek(ms: Long): Int = when {
            !semesterValid -> 0
            else -> ((ms - semesterStart!!) / (7 * 24 * 60 * 60 * 1000)).toInt()
                .coerceIn(0 until semesterWeekCount)
        }

        private fun getTimeString(millis: Long): String = timeFormat.getFormat(millis)
        fun getTimeString(hour24format: Int, minute: Int): String =
            timeFormat.getFormat(hour24format, minute)

        fun getTimeString(dayMinutes: Int): String =
            timeFormat.getFormat(dayMinutes / 60, dayMinutes % 60)

        private fun getDateString(millis: Long) = dateOrder.getFormat(dateSeparator, millis)
        fun getDateTimeString(millis: Long) =
            String.format("%s %s", getDateString(millis), getTimeString(millis))
    }

    class Widgets : PrefsControl() {
        /**
         * Zisťuje cieľovú kategóriu poznámkového widgetu
         * @param widgetId ID widgetu
         * @return null, ak widget alebo kategória už neexistujú alebo informácia o tom chýba
         */
        fun getNoteWidgetCategory(widgetId: Int): NoteCategory? {
            val key = NOTE_WIDGET + widgetId
            if (!has(key)) return null
            val catId = long(NOTE_WIDGET + widgetId)
            return if (catId < 1L) TimeCategory.values()
                .find { -it.ordinal.toLong() == catId } else App.data.subject(catId)
        }

        /**
         * Nastaví konfigurovanému widgetu poznámok kategóriu poznámok, ktoré má zobrazovať
         * @param widgetId ID widgetu ktorého kategóriu sa snažím uložiť
         * @param noteCategory Kategóriu, ktorú widgetu nastavím. Hodnota null zmaže informáciu o widgete
         */
        fun setNoteWidgetCategory(widgetId: Int, noteCategory: NoteCategory?) =
            when (noteCategory) {
                null -> clear(NOTE_WIDGET + widgetId)
                is TimeCategory -> store(NOTE_WIDGET + widgetId, -noteCategory.ordinal.toLong())
                is Subject -> store(NOTE_WIDGET + widgetId, noteCategory.id)
                else -> Unit
            }
    }

    class Notifications : PrefsControl() {
        var reminderAdvanceInMinutes: Int
            get() = int(REMINDER_ADVANCE)
            set(value) = store(REMINDER_ADVANCE, value)

        var reminderEnabled: Boolean
            get() = bool(REMINDER_ENABLED, false)
            set(value) = store(REMINDER_ENABLED, value)

        private var bits: Int
            get() = int(ALARMS, 0)
            set(value) = store(ALARMS, value)

        var alarmsEnabled: Boolean
            get() = bits > 0
            set(value) {
                bits = 0
                if (value) {
                    var ww = 0
                    for (day in settings.workWeek.days) ww = ww.or(1.shl(day.value - 1))
                    bits = if (settings.dualWeekSchedule) bits.or(
                        ww.shl(7).or(ww)
                    ) else bits.or(ww.shl(14))
                }
                resetAlarms()
            }

        fun resetAlarms() {
            for (reg in Regularity.values())
                for (day in Day.values())
                    ClockReceiver.enableAlarmClock(day, reg, isAlarmEnabled(day, reg))
        }

        private fun ac(reg: Regularity, day: Day): Int =
            1.shl((reg.odd?.let { if (it) 1 else 0 } ?: 2) * 7 + day.value - 1)

        private fun alarmSetKey(day: Day, regularity: Regularity) = ALARM_TIME + ac(regularity, day)

        private fun setAlarm(day: Day, regularity: Regularity, dayMinutes: Int) {
            if (dayMinutes !in 0 until 24 * 60) throw Exception("Invalid time data $dayMinutes!")
            store(alarmSetKey(day, regularity), dayMinutes)
            if (isAlarmEnabled(day, regularity))
                ClockReceiver.enableAlarmClock(day, regularity)
        }

        fun setAlarm(day: Day, regularity: Regularity, hour24format: Int, minute: Int) =
            setAlarm(day, regularity, hour24format * 60 + minute)

        fun getAlarm(day: Day, regularity: Regularity) = int(alarmSetKey(day, regularity), 360)
        fun enableAlarm(day: Day, regularity: Regularity, enable: Boolean) {
            bits = if (enable) bits.or(ac(regularity, day)) else bits.and(
                ac(
                    regularity,
                    day
                ).xor(Int.MAX_VALUE)
            )
            ClockReceiver.enableAlarmClock(day, regularity, enable)
        }

        fun isAlarmEnabled(day: Day, regularity: Regularity): Boolean =
            ac(regularity, day).and(bits) > 0

        fun setAlarmsBySchedule() {
            for (reg in Regularity.values()) {
                val regValid = (reg == Regularity.EVERY).xor(settings.dualWeekSchedule)
                val busyDays = App.data.getBusyDays(reg, settings.workWeek)
                for ((day, busy) in busyDays) {
                    setAlarm(
                        day,
                        reg,
                        (App.data.firstLessonStart(
                            day,
                            reg
                        ) - states.lastScheduleStartAdvance).coerceIn(0 until 24 * 60)
                    )
                    enableAlarm(day, reg, regValid && busy)
                }
            }
        }
    }
}
