package com.moriak.schednote.enums

import android.app.Activity
import android.content.Context
import androidx.annotation.StringRes
import com.moriak.schednote.R.string.*
import com.moriak.schednote.activities.MainActivity
import com.moriak.schednote.interfaces.IColorGroup
import com.moriak.schednote.interfaces.NoteCategory
import com.moriak.schednote.notifications.AlarmClockSetter
import com.moriak.schednote.storage.SQLite
import com.moriak.schednote.widgets.NoteWidget
import java.util.*

/**
 * [Command] predstavuje množinu príkazov, ktoré možno vysloviť
 *
 * @property redirection Obsah na načítanie alebo null
 * @property cmdRes odkaz na znenie príkazu
 * @property cmdDescRes odkaz na popis príkazu
 * @property tag doplňujúci údaj k príkazu [Command]. Môže byť null.
 * @property next Nasledujúci príkaz
 * @property prev Predošlý príkaz
 */
enum class Command(val redirection: Redirection?, @StringRes val cmdRes: Int, @StringRes val cmdDescRes: Int) {
    TIME_SCHEDULE(Redirection.TIME_SCHEDULE, cmd_unique_time_schedule, cmd_desc_time_schedule),
    LESSON_TYPES(Redirection.LESSON_TYPES, cmd_unique_lesson_types, cmd_desc_lesson_types),
    LESSON_SCHEDULE(Redirection.LESSON_SCHEDULE, cmd_unique_lesson_schedule, cmd_desc_lesson_schedule),
    SCHEDULE_DESIGN(Redirection.DESIGN, cmd_unique_design, cmd_desc_design),
    SUBJECTS(Redirection.SUBJECTS, cmd_unique_subjects, cmd_desc_subjects),
    NOTES(Redirection.NOTES, cmd_unique_notes, cmd_desc_notes),
    REMINDERS(Redirection.REMINDERS, cmd_unique_reminders, cmd_desc_reminders),
    ALARM_CLOCKS(Redirection.ALARM_CLOCKS, cmd_unique_alarm_clock, cmd_desc_alarm_clocks),
    SEMESTER(Redirection.SEMESTER, cmd_unique_semester, cmd_desc_semester),
    SETTINGS(Redirection.SETTINGS, cmd_unique_settings, cmd_desc_settings),
    ALARM_TONE(Redirection.ALARM_TONE, cmd_unique_alarm_tone, cmd_desc_alarm_tone),
    LESSON_TYPE(Redirection.DESIGN, cmd_unique_lesson_type, cmd_desc_lesson_type) {
        override fun identify(context: Context, str: String): Boolean {
            tag = ColorGroup.values().find { said(it.describe(context)) == said(str) }
                ?: SQLite.lessonTypes().find { said(it.name) == said(str) } ?: return false
            return true
        }

        override fun commit(activity: Activity) = redirection?.redirect(activity, false) {
            putInt(Redirection.EXTRA_DESIGN_COLOR_GROUP, (tag as IColorGroup).id)
        } ?: Unit
    },
    NOTE_CATEGORY(Redirection.NOTES, cmd_unique_note_category, cmd_desc_note_category) {
        override fun identify(context: Context, str: String): Boolean {
            tag = TimeCategory.values().find { said(context.getString(it.res)) == said(str) }
                ?: SQLite.subjects().find { said(it.name) == said(str) || said(it.abb) == said(str) }
                        ?: return false
            return true
        }

        override fun commit(activity: Activity) = redirection?.redirect(activity, false) {
            putLong(Redirection.EXTRA_NOTE_CATEGORY, (tag as NoteCategory).id)
        } ?: Unit
    },
    CLEAN_UP(null, cmd_unique_clean_up, cmd_desc_clean_up) {
        override fun commit(activity: Activity) {
            SQLite.clearGarbageData()
            NoteWidget.update(activity)
            if (activity is MainActivity) activity.refreshWhenCleanedUpAndNecessary()
        }
    },
    ADAPT_ALARMS_TO_SCHEDULE(null, cmd_unique_adapt_alarms_to_schedule, cmd_desc_adapt_alarms_by_schedule) {
        override fun commit(activity: Activity) {
            AlarmClockSetter.setAlarmsBySchedule(activity)
            if (activity is MainActivity) activity.refreshWhenAlarmClocksSetAndNecessary()
        }
    };

    companion object {
        private fun said(value: String) = value.trim().toLowerCase(Locale.ROOT)
        /**
         * Identifikuje príkaz na základe počutého výrazu
         * @param str Počutý výraz
         * @return rozpoznaný príkaz alebo null
         */
        fun identifyCommand(context: Context, str: String): Command? = values().find {
            it.identify(context, str)
        }
    }

    var tag: Any? = null
    val next by lazy { values().let { it[(ordinal + 1) % it.size] } }
    val prev by lazy { values().let { it[if (ordinal == 0) it.lastIndex else ordinal - 1] } }

    protected open fun identify(context: Context, str: String) = said(context.getString(cmdRes)) == said(str)

    /**
     * Vykoná sa príkaz
     * @param activity Aktivita, v ktorej bol príkaz zadaný
     */
    open fun commit(activity: Activity) = redirection?.redirect(activity, false) ?: Unit
}
