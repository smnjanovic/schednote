package com.moriak.schednote.database.data

import com.moriak.schednote.other.Day
import com.moriak.schednote.settings.Regularity

interface ScheduleEvent {
    class InvalidTimeRangeException(range: IntRange) : Exception("Invalid Time Range $range!")
    companion object {
        fun rangeCheck(range: IntRange) {
            if (range.first < 0 || range.count() == 0) throw InvalidTimeRangeException(range)
        }
    }

    val regularity: Regularity
    val day: Day
    val time: IntRange

    fun isEqual(scheduleEvent: ScheduleEvent?): Boolean
}