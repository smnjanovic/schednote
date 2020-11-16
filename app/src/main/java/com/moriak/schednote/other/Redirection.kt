package com.moriak.schednote.other

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import com.moriak.schednote.App
import com.moriak.schednote.activities.AlarmToneActivity
import com.moriak.schednote.activities.MainActivity
import com.moriak.schednote.activities.Settings
import com.moriak.schednote.fragments.of_alarm.AlarmClockSetter
import com.moriak.schednote.fragments.of_alarm.ReminderSetter
import com.moriak.schednote.fragments.of_main.NotesFragment
import com.moriak.schednote.fragments.of_main.SemesterFragment
import com.moriak.schednote.fragments.of_main.SubjectsFragment
import com.moriak.schednote.fragments.of_schedule.DesignEditor
import com.moriak.schednote.fragments.of_schedule.LessonTimesList
import com.moriak.schednote.fragments.of_schedule.LessonTypesList
import com.moriak.schednote.fragments.of_schedule.ScheduleEditor
import com.moriak.schednote.other.Redirection.*

/**
 * [Redirection] vytvára intent použiteľný na presmerovávanie sa medzi aktivitami alebo prepínanie sa medzi fragmentmi.
 *
 * @property LESSON_SCHEDULE Presmerovanie sa do aktivity [MainActivity] a prepnutie sa do fragmentu [ScheduleEditor]
 * @property LESSON_TYPES Presmerovanie sa do aktivity [MainActivity] a prepnutie sa do fragmentu [LessonTypesList]
 * @property TIME_SCHEDULE Presmerovanie sa do aktivity [MainActivity] a prepnutie sa do fragmentu [LessonTimesList]
 * @property DESIGN Presmerovanie sa do aktivity [MainActivity] a prepnutie sa do fragmentu [DesignEditor]
 * @property SUBJECTS Presmerovanie sa do aktivity [MainActivity] a prepnutie sa do fragmentu [SubjectsFragment]
 * @property NOTES Presmerovanie sa do aktivity [MainActivity] a prepnutie sa do fragmentu [NotesFragment]
 * @property REMINDERS Presmerovanie sa do aktivity [MainActivity] a prepnutie sa do fragmentu [ReminderSetter]
 * @property ALARM_CLOCKS Presmerovanie sa do aktivity [MainActivity] a prepnutie sa do fragmentu [AlarmClockSetter]
 * @property SEMESTER Presmerovanie sa do aktivity [MainActivity] a prepnutie sa do fragmentu [SemesterFragment]
 * @property SETTINGS Presmerovanie sa do aktivity [Settings]
 * @property ALARM_TONE Presmerovanie sa do aktivity [AlarmToneActivity]
 *
 * @property action Každý enum prvok vykonáva inú akciu
 */
enum class Redirection(
    val activity: Class<out Activity>,
    val fragment: Class<out Fragment>? = null
) {
    LESSON_SCHEDULE(MainActivity::class.java, ScheduleEditor::class.java),
    LESSON_TYPES(MainActivity::class.java, LessonTypesList::class.java),
    TIME_SCHEDULE(MainActivity::class.java, LessonTimesList::class.java),
    DESIGN(MainActivity::class.java, DesignEditor::class.java),
    SUBJECTS(MainActivity::class.java, SubjectsFragment::class.java),
    NOTES(MainActivity::class.java, NotesFragment::class.java),
    REMINDERS(MainActivity::class.java, ReminderSetter::class.java),
    ALARM_CLOCKS(MainActivity::class.java, AlarmClockSetter::class.java),
    SEMESTER(MainActivity::class.java, SemesterFragment::class.java),
    SETTINGS(Settings::class.java),
    ALARM_TONE(AlarmToneActivity::class.java);

    companion object {
        private val prefix = Redirection::class.java.canonicalName!!
        val EXTRA_DESIGN_COLOR_GROUP = "$prefix.EXTRA_DESIGN_COLOR_GROUP"
        val EXTRA_NOTE_ID = "$prefix.EXTRA_NOTE_ID"
        val EXTRA_NOTE_CATEGORY = "$prefix.EXTRA_NOTE_CATEGORY"

        /**
         * Zistiť, čije  úmyslom intentu [i] presmerovať sa na inú aktivitu / fragment.
         * @param i kontrolovaný intent
         * @return [Redirection], ak tento intent zodpovedá požiadavkám niektorej inštancie [Redirection], inak null.
         */
        fun detectRedirection(i: Intent?): Redirection? {
            if (i?.action == null) return null
            for (r in values())
                if (r.action == i.action)
                    return r
            return null
        }
    }

    val action = javaClass.canonicalName!! + ".$name"

    /**
     * Tvorba intentu na presmerovanie
     * @param context Context
     * @param launch false, ak nejaká aktivita už beží, inak true
     * @return Vráti [Intent], ktorý je určný na presmerovanie sa do inej aktivity alebo prepnutie fragmentov
     */
    fun makeIntent(context: Context = App.ctx, launch: Boolean = true) =
        Intent(context, activity).setAction(action)
            .addFlags(
                if (!launch) Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                else Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
}