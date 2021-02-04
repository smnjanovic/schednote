package com.moriak.schednote.database.data

import com.moriak.schednote.other.Day
import com.moriak.schednote.settings.Regularity

/**
 * Rozvrhová udalosť reprezentuje vyhradený čas v rozvrhu.
 * @property regularity Údaj, či sa daná udalosť opakuje každý týždeň, alebo každý druhý párny alebo nepárny týždeň
 * @property day Deň udalosti
 * @property time Rozsah poradí časových jednotiek rozvrhu [LessonTime]
 */
abstract class ScheduleEvent(pReg: Regularity, pDay: Day, pTime: IntRange) {
    class InvalidTimeRangeException(range: IntRange) : Exception("Invalid Time Range $range!")
    companion object {
        /**
         * Kontrola, či daný časový rozsah má uveriteľné hodnoty
         * @param range kontrolovaný rozsah hodín v ich poradových číslach
         */
        fun rangeCheck(range: IntRange) {
            if (range.first < 0 || range.count() == 0) throw InvalidTimeRangeException(range)
        }
    }

    val regularity: Regularity = pReg
    val day: Day = pDay
    private val st: Int = pTime.first
    private var en: Int = pTime.last
    val time: IntRange get() = st..en

    /**
     * Porovnanie, či sa jedná o rovnakú udalosť
     * @param other iná udalosť ktorej vybrané vlastnosti stavy sú porovnávané s týmto objektom
     */
    abstract fun isEqual(other: ScheduleEvent?): Boolean
    operator fun inc() = also { en++ }
    operator fun dec(): ScheduleEvent = also { if (en > st) en-- }
    fun isAfter(other: ScheduleEvent?) =
        other?.let { other.day == day && other.time.last + 1 == st } ?: false

    fun isBefore(other: ScheduleEvent?) =
        other?.let { other.day == day && other.time.first - 1 == en } ?: false
}