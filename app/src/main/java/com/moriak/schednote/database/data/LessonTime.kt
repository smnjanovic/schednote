package com.moriak.schednote.database.data

import com.moriak.schednote.database.data.LessonTime.Companion

/**
 * Trieda uchováva informácie o najmenšej jednotke rozvrhu: vyučovacej hodiny
 * @property Companion.MAX_LESSON_COUNT maximálny počet vyučovacích hodín v rozvrhu
 * @property order Poradie hodiny (od 1)
 * @property lessonDuration trvanie hodiny
 * @property breakDuration trvanie prestávky po hodine
 */
data class LessonTime(val order: Int, var lessonDuration: Int, var breakDuration: Int) {
    companion object {
        const val MAX_LESSON_COUNT = 18
    }
}