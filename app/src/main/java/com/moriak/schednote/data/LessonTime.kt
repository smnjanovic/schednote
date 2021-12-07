package com.moriak.schednote.data

/**
 * Trieda uchováva informácie o najmenšej jednotke rozvrhu: vyučovacej hodiny
 * @property order Poradie hodiny (od 1)
 * @property lessonDuration trvanie hodiny v minútach
 * @property breakDuration trvanie nasledujúcej prestávky v minútach
 */
data class LessonTime(val order: Int, var lessonDuration: Int, var breakDuration: Int)