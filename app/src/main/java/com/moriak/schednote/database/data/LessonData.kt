package com.moriak.schednote.database.data

import com.moriak.schednote.App
import com.moriak.schednote.settings.LessonTimeFormat
import com.moriak.schednote.settings.Prefs

data class LessonData(
    val order: Int,
    var lessonDuration: Int,
    var breakDuration: Int,
    private val start: Int,
    private val end: Int
) {
    companion object {
        const val MAX_LESSON_COUNT = 18
    }

    constructor(order: Int, lessonDuration: Int, breakDuration: Int) : this(
        order,
        lessonDuration,
        breakDuration,
        App.data.lessonStart(order),
        App.data.lessonEnd(order)
    )

    val startFormat
        get() = when (Prefs.settings.lessonTimeFormat) {
            LessonTimeFormat.ORDER_FROM_0 -> "${order - 1}."
            LessonTimeFormat.ORDER_FROM_1 -> "$order."
            LessonTimeFormat.START_TIME -> LessonTimeFormat.timeFormat(start)
        }
    val endFormat
        get() = when (Prefs.settings.lessonTimeFormat) {
            LessonTimeFormat.ORDER_FROM_0 -> "${order - 1}."
            LessonTimeFormat.ORDER_FROM_1 -> "$order."
            LessonTimeFormat.START_TIME -> LessonTimeFormat.timeFormat(end)
        }
}