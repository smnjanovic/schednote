package com.moriak.schednote.menu

import com.moriak.schednote.fragments.of_schedule.DesignEditor
import com.moriak.schednote.fragments.of_schedule.LessonTimesList
import com.moriak.schednote.fragments.of_schedule.LessonTypesList
import com.moriak.schednote.fragments.of_schedule.ScheduleEditor

enum class ScheduleDisplay {
    DESIGN, LESSON_SCHEDULE, TIME_SCHEDULE, LESSON_TYPES;

    companion object {
        operator fun get(str: String?): ScheduleDisplay? {
            if (str == null) return null
            for (v in values())
                if (v.name == str)
                    return v
            return null
        }

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