package com.moriak.schednote.menu

import com.moriak.schednote.fragments.of_main.ScheduleFragment
import com.moriak.schednote.fragments.of_schedule.DesignEditor
import com.moriak.schednote.fragments.of_schedule.LessonTimesList
import com.moriak.schednote.fragments.of_schedule.LessonTypesList
import com.moriak.schednote.fragments.of_schedule.ScheduleEditor
import com.moriak.schednote.menu.ScheduleDisplay.*

/**
 * pomocou [ScheduleDisplay] je možné vybrať si, či nastavovať notifikácie k úloham alebo budíky k rozvrhu
 * @property DESIGN Položka sa sústreďuje na úpravy dizajnu rozvrrhu
 * @property LESSON_SCHEDULE Položka sa sústreďuje na náhľad a zmeny v rozvrhu
 * @property TIME_SCHEDULE Položka sa sústreďuje hodiny ako najmenšie časové jednotky rozvrhu
 * @property LESSON_TYPES Položka sa sústreďuje na zobrazenie a manipuláciu s typmi hodín
 *
 * @property fragmentClass jedná sa o fragment, ktorý bude načítaný v pod fragmente [ScheduleFragment]
 */

enum class ScheduleDisplay {
    DESIGN, LESSON_SCHEDULE, TIME_SCHEDULE, LESSON_TYPES;

    companion object {
        /**
         * Výber na základe názvu inštancie [ScheduleDisplay]
         * @param str
         * @return [ScheduleDisplay]
         */
        operator fun get(str: String?): ScheduleDisplay? {
            if (str == null) return null
            for (v in values())
                if (v.name == str)
                    return v
            return null
        }

        /**
         * Výber inštancie na základe zvolenej triedy
         * @param cls
         * @return [ScheduleDisplay]
         */
        operator fun get(cls: Class<*>?): ScheduleDisplay? = when (cls?.canonicalName) {
            DesignEditor::class.java.canonicalName -> DESIGN
            ScheduleEditor::class.java.canonicalName -> LESSON_SCHEDULE
            LessonTimesList::class.java.canonicalName -> TIME_SCHEDULE
            LessonTypesList::class.java.canonicalName -> LESSON_TYPES
            else -> null
        }
    }

    val fragmentClass
        get() = when (this) {
            DESIGN -> DesignEditor::class.java
            LESSON_SCHEDULE -> ScheduleEditor::class.java
            TIME_SCHEDULE -> LessonTimesList::class.java
            LESSON_TYPES -> LessonTypesList::class.java
        }
}