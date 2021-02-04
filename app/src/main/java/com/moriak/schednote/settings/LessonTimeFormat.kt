package com.moriak.schednote.settings

import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.database.data.LessonTime
import com.moriak.schednote.settings.LessonTimeFormat.*

/**
 * Trieda určuje ako sa budú jednotlivé hodiný prezentované.
 * @property ORDER_FROM_0 Hodiny budú prezentované radovým číslom. Rozvrh začína nultou hodinou.
 * @property ORDER_FROM_1 Hodiny budú prezentované radovým číslom. Rozvrh začína prvou hodinou.
 * @property START_TIME Hodiny budú prezentované časom, kedy začnú
 */
enum class LessonTimeFormat {
    ORDER_FROM_0, ORDER_FROM_1, START_TIME;

    companion object {
        /**
         * Získanie inštancie, ktorej atribút [name] sa bude zhodovať s argumentom [key].
         * @param key Reťazec porovnávaný s atribútom [name] každej inštancie [LessonTimeFormat], kým sa teda nenájde zhoda
         * @return Vráti inštanciu [LessonTimeFormat] vyhovujúcu hodnote [key]. Môže byť null.
         */
        operator fun get(key: String?) = when (key) {
            ORDER_FROM_0.name -> ORDER_FROM_0
            ORDER_FROM_1.name -> ORDER_FROM_1
            START_TIME.name -> START_TIME
            else -> null
        }
    }

    /**
     * Získanie popisu [lessonOrder]-tej hodiny podľa osobitného predpisu inštancie [LessonTimeFormat]
     * @return Textový popis začiatku [lessonOrder]-tej hodiny
     */
    fun startFormat(lessonOrder: Int) = when (this) {
        ORDER_FROM_0 -> "${lessonOrder - 1}."
        ORDER_FROM_1 -> "$lessonOrder."
        START_TIME -> Prefs.settings.getTimeString(
            Prefs.settings.earliestMinute + App.data.lessonStart(
                lessonOrder
            )
        )
    }

    /**
     * Získanie popisu [lessonOrder]-tej hodiny podľa osobitného predpisu inštancie [LessonTimeFormat]
     * @return Textový popis konca [lessonOrder]-tej hodiny
     */
    fun endFormat(lessonOrder: Int) = when (this) {
        ORDER_FROM_0 -> "${lessonOrder - 1}."
        ORDER_FROM_1 -> "$lessonOrder."
        START_TIME -> Prefs.settings.getTimeString(
            Prefs.settings.earliestMinute + App.data.lessonEnd(
                lessonOrder
            )
        )
    }

    /**
     * Výpis časového úseku rozvrhu vo formáte predpísanom inštanciami [LessonTimeFormat].
     * @param range Obsahuje rozsah hodín [LessonTime].
     * @return Textový popis časového úseku rozvrhu
     */
    fun rangeFormat(range: IntRange): String = when (this) {
        ORDER_FROM_0 -> "${range.first - 1}.  — ${range.last - 1}."
        ORDER_FROM_1 -> "${range.first}.  — ${range.last}."
        START_TIME -> {
            val earliest = Prefs.settings.earliestMinute
            val min = App.data.scheduleRangeToMinuteRange(range) ?: 0..0
            val st = Prefs.settings.getTimeString(earliest + min.first)
            val en = Prefs.settings.getTimeString(earliest + min.last)
            "$st — $en"
        }
    }

    override fun toString(): String = App.str(
        when (this) {
            ORDER_FROM_0 -> R.string.start_lessons_from_0
            ORDER_FROM_1 -> R.string.start_lessons_from_1
            START_TIME -> R.string.show_start_time_of_lessons
        }
    )
}
