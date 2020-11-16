package com.moriak.schednote.other

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.annotation.StringRes
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.other.Command.*
import com.moriak.schednote.settings.ColorGroup
import java.util.*

/**
 * [Command] predstavuje množinu príkazov, ktoré možno vysloviť
 *
 * @property CLEAN_UP Odstránia sa nadbytočné dáta z aplikácie (Oneskorené úlohy, hodiny mimo pracovných dní, nepoužité predmety, ...)
 * @property TIME_SCHEDULE Presmerovanie sa pomocou [Redirection.TIME_SCHEDULE]
 * @property LESSON_TYPES Presmerovanie sa pomocou [Redirection.LESSON_TYPES]
 * @property LESSON_TYPE Presmerovanie sa pomocou [Redirection.DESIGN] a prejsť do režimu úprav vybranej množiny kontajnerov
 * @property LESSON_SCHEDULE Presmerovanie sa pomocou [Redirection.LESSON_SCHEDULE]
 * @property DESIGN Presmerovanie sa pomocou [Redirection.DESIGN]
 * @property SUBJECTS Presmerovanie sa pomocou [Redirection.SUBJECTS]
 * @property NOTE_CATEGORY Presmerovanie sa pomocou [Redirection.NOTES] a nastavenie špecifickej kategórie
 * @property NOTES Presmerovanie sa pomocou [Redirection.NOTES]
 * @property REMINDERS Presmerovanie sa pomocou [Redirection.REMINDERS]
 * @property ADAPT_ALARMS_TO_SCHEDULE Nastavenie budíkov na každý pracovný deň v naposledy nastavenom predstihu pred začiatkom hodiny
 * @property ALARM_CLOCKS Presmerovanie sa pomocou [Redirection.ALARM_CLOCKS]
 * @property SEMESTER Presmerovanie sa pomocou [Redirection.SEMESTER]
 * @property SETTINGS Presmerovanie sa pomocou [Redirection.SETTINGS]
 * @property ALARM_TONE Presmerovanie sa pomocou [Redirection.ALARM_TONE]
 *
 * @property spanned Formátovaný popis príkazu
 * @property tag Ľubovoľný doplnkový objekt, využiteľný pre niektorý inštancie [Command]
 * @property prev Predošlý príkaz
 * @property next Nasledujúci príkaz
 */
enum class Command(
    val redirection: Redirection?,
    @StringRes private val cmdRes: Int,
    @StringRes private val cmdDescRes: Int
) {
    CLEAN_UP(null, R.string.cmd_unique_clean_up, R.string.cmd_desc_clean_up),
    TIME_SCHEDULE(
        Redirection.TIME_SCHEDULE,
        R.string.cmd_unique_time_schedule,
        R.string.cmd_desc_time_schedule
    ),
    LESSON_TYPES(
        Redirection.LESSON_TYPES,
        R.string.cmd_unique_lesson_types,
        R.string.cmd_desc_lesson_types
    ),
    LESSON_TYPE(Redirection.DESIGN, R.string.cmd_unique_lesson_type, R.string.cmd_desc_lesson_type),
    LESSON_SCHEDULE(
        Redirection.LESSON_SCHEDULE,
        R.string.cmd_unique_lesson_schedule,
        R.string.cmd_desc_lesson_schedule
    ),
    DESIGN(Redirection.DESIGN, R.string.cmd_unique_design, R.string.cmd_desc_design),
    SUBJECTS(Redirection.SUBJECTS, R.string.cmd_unique_subjects, R.string.cmd_desc_subjects),
    NOTE_CATEGORY(
        Redirection.NOTES,
        R.string.cmd_unique_note_category,
        R.string.cmd_desc_note_category
    ),
    NOTES(Redirection.NOTES, R.string.cmd_unique_notes, R.string.cmd_desc_notes),
    REMINDERS(Redirection.REMINDERS, R.string.cmd_unique_reminders, R.string.cmd_desc_reminders),
    ADAPT_ALARMS_TO_SCHEDULE(
        null,
        R.string.cmd_unique_adapt_alarms_to_schedule,
        R.string.cmd_desc_adapt_alarms_by_schedule
    ),
    ALARM_CLOCKS(
        Redirection.ALARM_CLOCKS,
        R.string.cmd_unique_alarm_clock,
        R.string.cmd_desc_alarm_clocks
    ),
    SEMESTER(Redirection.SEMESTER, R.string.cmd_unique_semester, R.string.cmd_desc_semester),
    SETTINGS(Redirection.SETTINGS, R.string.cmd_unique_settings, R.string.cmd_desc_settings),
    ALARM_TONE(
        Redirection.ALARM_TONE,
        R.string.cmd_unique_alarm_tone,
        R.string.cmd_desc_alarm_tone
    );

    private val cmdWord get() = App.str(cmdRes)
    private val cmdDesc get() = App.str(cmdDescRes)

    companion object {
        /**
         * Identifikuje príkaz na základe počutého výrazu
         * @param str Počutý výraz
         * @return rozpoznaný príkaz alebo null
         */
        fun identifyCommand(str: String): Command? {
            fun said(value: String) = value.trim().toLowerCase(Locale.ROOT)
            val heard = said(str)

            for (v in values())
                if (said(v.cmdWord) == heard)
                    return v

            for (t in App.data.lessonTypes())
                if (heard == said(t.toString()))
                    return LESSON_TYPE.also { it.tag = t.id }

            for (c in ColorGroup.values())
                if (heard == said(c.toString()))
                    return LESSON_TYPE.also { it.tag = -c.ordinal }

            for (tc in TimeCategory.values())
                if (heard == said(tc.toString()))
                    return NOTE_CATEGORY.also { it.tag = tc }

            for (sub in App.data.subjects())
                if (heard == said(sub.name) || heard == said(sub.abb))
                    return NOTE_CATEGORY.also { it.tag = sub }

            return null
        }

        /**
         * Prístup k hodnote [Command] na určitej pozícii
         * @param n ordinal
         * @return [Command] alebo null
         */
        operator fun get(n: Int): Command? =
            values().let { v -> if (n in v.indices) v[n] else null }
    }

    val spanned by lazy {
        SpannableString("$cmdWord\n$cmdDesc").apply {
            val color = App.res.getColor(R.color.textColor, null)
            setSpan(StyleSpan(Typeface.BOLD), 0, cmdWord.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(
                ForegroundColorSpan(color),
                length - cmdDesc.length,
                length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }
    var tag: Any? = null

    val next by lazy { values().let { it[(ordinal + 1) % it.size] } }
    val prev by lazy { values().let { it[if (ordinal == 0) it.lastIndex else ordinal - 1] } }
}
