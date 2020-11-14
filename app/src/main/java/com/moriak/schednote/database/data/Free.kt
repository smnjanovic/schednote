package com.moriak.schednote.database.data

import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.other.Day
import com.moriak.schednote.settings.Prefs
import com.moriak.schednote.settings.Regularity

data class Free(
    override val regularity: Regularity,
    override val day: Day,
    override val time: IntRange
) :
    ScheduleEvent {
    init {
        ScheduleEvent.rangeCheck(time)
    }

    override fun toString(): String {
        val format = Prefs.settings.lessonTimeFormat
        val reg = regularity.odd?.let { if (it) " I." else " II." } ?: ""
        return "${App.str(R.string.free)} — $day$reg ${format.rangeFormat(time)}"
    }

    override fun isEqual(scheduleEvent: ScheduleEvent?): Boolean = scheduleEvent is Free
}