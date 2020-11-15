package com.moriak.schednote.database.data

import com.moriak.schednote.App
import com.moriak.schednote.settings.LessonTimeFormat
import com.moriak.schednote.settings.Prefs

/**
 * Trieda uchováva informácie o najmenšej jednotke rozvrhu: vyučovacej hodiny
 * @property order Poradie hodiny (od 1)
 * @property lessonDuration trvanie hodiny
 * @property breakDuration trvanie prestávky po hodine
 * @property start Čas začiatku hodiny v minútach od poslednej polnoci
 * @property end Čas konca hodiny v minútach od poslednej polnoci
 * @property startFormat Textový popis začiatku hodiny
 * @property endFormat Textový popis konca hodiny
 */
data class LessonTime(
    val order: Int,
    var lessonDuration: Int,
    var breakDuration: Int,
    private val start: Int,
    private val end: Int
) {
    /**
     * @property MAX_LESSON_COUNT maximálny počet vyučovacích hodín v rozvrhu
     */
    companion object {
        const val MAX_LESSON_COUNT = 18
    }

    /**
     * @param order Poradie hodiny (od 1)
     * @param lessonDuration trvanie hodiny
     * @param breakDuration trvanie prestávky po hodine
     */
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