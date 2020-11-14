package com.moriak.schednote.settings

import com.moriak.schednote.App
import com.moriak.schednote.R
import java.util.*

enum class TimeFormat(val hourFormat: Int) {
    H12(12), H24(24);

    companion object {
        operator fun get(hour: Int): TimeFormat = when (hour) {
            H12.hourFormat -> H12
            H24.hourFormat -> H24
            else -> throw IllegalArgumentException("No such format exists!")
        }
    }

    override fun toString(): String = App.str(
        when (this) {
            H12 -> R.string.time_format_12
            H24 -> R.string.time_format_24
        }
    )

    fun getFormat(millis: Long) = getFormat(App.cal.apply { timeInMillis = millis })
    fun getFormat(calendar: Calendar) =
        getFormat(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))

    fun getFormat(hour24: Int, minute: Int): String = when (this) {
        H12 -> String.format(
            "%d:%02d %s", (hour24 % 12).let { if (it == 0) 12 else it }, minute, when {
                hour24 in 1..11 || hour24 == 0 && minute > 0 -> "AM"
                hour24 in 13..23 || hour24 == 12 && minute > 0 -> "PM"
                hour24 == 12 -> "N"
                else -> "M"
            }
        )
        H24 -> String.format("%d:%02d", hour24, minute)
    }
}