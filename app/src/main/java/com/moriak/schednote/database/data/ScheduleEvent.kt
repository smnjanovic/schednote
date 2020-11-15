package com.moriak.schednote.database.data

import com.moriak.schednote.other.Day
import com.moriak.schednote.settings.Regularity

/**
 * Rozvrhová udalosť reprezentuje vyhradený čas v rozvrhu.
 * @property regularity Údaj, či sa daná udalosť opakuje každý týždeň, alebo každý druhý párny alebo nepárny týždeň
 * @property day Deň udalosti
 * @property time Rozsah poradí časových jednotiek rozvrhu [LessonTime]
 */
interface ScheduleEvent {
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

    val regularity: Regularity
    val day: Day
    val time: IntRange

    /**
     * Porovnanie, či sa jedná o rovnakú udalosť
     * @param other iná udalosť ktorej vybrané vlastnosti stavy sú porovnávané s týmto objektom
     */
    fun isEqual(other: ScheduleEvent?): Boolean
}