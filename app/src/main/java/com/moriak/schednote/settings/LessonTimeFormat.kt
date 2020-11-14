package com.moriak.schednote.settings

import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.database.data.ScheduleEvent

enum class LessonTimeFormat {
    ORDER_FROM_0, ORDER_FROM_1, START_TIME;

    companion object {
        operator fun get(string: String?) = when (string) {
            ORDER_FROM_0.name -> ORDER_FROM_0
            ORDER_FROM_1.name -> ORDER_FROM_1
            START_TIME.name -> START_TIME
            else -> null
        }

        fun timeFormat(minuteSinceScheduleStart: Int): String = Prefs.settings
            .getTimeString(minuteSinceScheduleStart + Prefs.settings.earliestMinute)
    }

    fun startFormat(lessonOrder: Int) = when (this) {
        ORDER_FROM_0 -> "${lessonOrder - 1}."
        ORDER_FROM_1 -> "$lessonOrder."
        START_TIME -> timeFormat(App.data.lessonStart(lessonOrder))
    }

    fun endFormat(lessonOrder: Int) = when (this) {
        ORDER_FROM_0 -> "${lessonOrder - 1}."
        ORDER_FROM_1 -> "$lessonOrder."
        START_TIME -> timeFormat(App.data.lessonEnd(lessonOrder))
    }

    fun rangeFormat(range: IntRange): String {
        return when (this) {
            ORDER_FROM_0 -> "${range.first - 1}.  — ${range.last - 1}."
            ORDER_FROM_1 -> "${range.first}.  — ${range.last}."
            START_TIME -> App.data.scheduleRangeToMinuteRange(range)?.let {
                "${timeFormat(it.first)} — ${timeFormat(it.last)}"
            } ?: throw ScheduleEvent.InvalidTimeRangeException(range)
        }
    }

    override fun toString(): String {
        return App.str(
            when (this) {
                ORDER_FROM_0 -> R.string.start_lessons_from_0
                ORDER_FROM_1 -> R.string.start_lessons_from_1
                START_TIME -> R.string.show_start_time_of_lessons
            }
        )
    }
}
