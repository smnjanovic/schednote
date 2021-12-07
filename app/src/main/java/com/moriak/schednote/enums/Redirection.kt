package com.moriak.schednote.enums

import android.app.Activity
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.moriak.schednote.activities.AlarmToneActivity
import com.moriak.schednote.activities.MainActivity
import com.moriak.schednote.activities.Settings
import com.moriak.schednote.enums.Redirection.*
import com.moriak.schednote.interfaces.ISubContent

/**
 * Inštancie tejto triedy zobrazujú alebo umožňujú zobrazenie danej časti aplikácie.
 *
 * [MAIN] Zobrazenie hlavnej aktivity [MainActivity].
 * [LESSON_SCHEDULE] Zobrazenie editora rozvrhu v hlavnej aktivite [MainActivity].
 * [LESSON_TYPES] Zobrazenie zoznamu kategórií hodín v hlavnej aktivite [MainActivity].
 * [TIME_SCHEDULE] Zobrazenie zoznamu časových blokov rozvrhu v hlavnej aktivite [MainActivity].
 * [DESIGN] Zobrazenie editora dizajnu rozvrhu v hlavnej aktivite [MainActivity].
 * [SUBJECTS] Zobrazenie zoznamu predmetov v hlavnej aktivite [MainActivity].
 * [NOTES] Zobrazenie zoznamu úloh v hlavnej aktivite [MainActivity].
 * [REMINDERS] Zobrazenie nastavenia upozornení v hlavnej aktivite [MainActivity].
 * [ALARM_CLOCKS] Zobrazenie nastavenia budíkov v hlavnej aktivite [MainActivity].
 * [SEMESTER] Zobrazenie nastavenia semestra v hlavnej aktivite [MainActivity].
 * [SETTINGS] Zobrazenie nastavení  v hlavnej aktivite [Settings].
 * [ALARM_TONE] Zobrazenie tónov budenia v aktivite [AlarmToneActivity].
 */
enum class Redirection(
    private val activity: Class<out Activity>,
    val iSubContent: ISubContent? = null
) {
    MAIN(MainActivity::class.java, null),
    LESSON_SCHEDULE(MainActivity::class.java, ScheduleDisplay.LESSON_SCHEDULE),
    LESSON_TYPES(MainActivity::class.java, ScheduleDisplay.LESSON_TYPES),
    TIME_SCHEDULE(MainActivity::class.java, ScheduleDisplay.TIME_SCHEDULE),
    DESIGN(MainActivity::class.java, ScheduleDisplay.DESIGN),
    SUBJECTS(MainActivity::class.java, SubContent.SUBJECTS),
    NOTES(MainActivity::class.java, SubContent.NOTES),
    REMINDERS(MainActivity::class.java, AlarmCategory.REMINDER),
    ALARM_CLOCKS(MainActivity::class.java, AlarmCategory.ALARM),
    SEMESTER(MainActivity::class.java, SubContent.SEMESTER),
    SETTINGS(Settings::class.java),
    ALARM_TONE(AlarmToneActivity::class.java);

    /**
     * @property EXTRA_DESIGN_COLOR_GROUP údaj určený na odloženie údaja o farebnej skupine.
     * @property EXTRA_NOTE_ID údaj určený na odloženie ID úlohy.
     * @property EXTRA_NOTE_CATEGORY údaj určený na odloženie kategórie úloh.
     */
    companion object {
        val EXTRA_DESIGN_COLOR_GROUP = "${Redirection::class.java.canonicalName}.EXTRA_DESIGN_COLOR_GROUP"
        val EXTRA_NOTE_ID = "${Redirection::class.java.canonicalName}.EXTRA_NOTE_ID"
        val EXTRA_NOTE_CATEGORY = "${Redirection::class.java.canonicalName}.EXTRA_NOTE_CATEGORY"

        /**
         * Zistiť, čije  úmyslom intentu [i] presmerovať sa na inú aktivitu / fragment.
         * @param i kontrolovaný intent
         * @return [Redirection], ak tento intent zodpovedá požiadavkám niektorej inštancie [Redirection], inak null.
         */
        fun detectRedirection(i: Intent?) = i?.action?.let { a -> values().find { it.action == a } }
    }

    private val action = "${javaClass.canonicalName}.$name"

    /**
     * Vytvorí pending intent schopný, ktorý je schopný spustiť aktivitu
     */
    fun prepare(context: Context, code: Int, launch: Boolean, putExtras: (Bundle.()->Unit)? = null): PendingIntent {
        val intent = Intent(action, null, context, activity).addFlags(when (launch) {
            true -> Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            false -> Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
        if (putExtras != null) intent.putExtras(Bundle().also(putExtras))
        return PendingIntent.getActivity(context, code, intent, FLAG_UPDATE_CURRENT)
    }

    fun redirect(context: Context, launch: Boolean, putExtras: (Bundle.()->Unit)? = null) =
        prepare(context, 0, launch, putExtras).send()
}