package com.moriak.schednote.settings

import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.settings.Prefs.settings
import java.util.Calendar.DAY_OF_WEEK
import java.util.Calendar.DAY_OF_YEAR

enum class Regularity(val odd: Boolean?) {
    EVERY(null), EVEN(false), ODD(true);

    companion object {
        val values
            get() = when {
                settings.dualWeekSchedule -> arrayOf(EVERY, EVEN, ODD)
                else -> arrayOf(EVERY)
            }

        operator fun get(value: Boolean?) = value?.let { if (it) ODD else EVEN } ?: EVERY

        fun isWeekOdd(millis: Long = System.currentTimeMillis()): Boolean {
            App.cal.timeInMillis = millis
            val today: Int = App.cal.get(DAY_OF_YEAR)
            App.cal.set(DAY_OF_YEAR, 1)
            App.cal.add(
                DAY_OF_YEAR,
                (settings.workWeek.days.first().value - App.cal.get(DAY_OF_WEEK)).let { if (it < 1) it + 7 else it })
            val firstDayOfWeek: Int = App.cal.get(DAY_OF_YEAR)
            return ((today - firstDayOfWeek) / 7 + 1) % 2 == 1
        }

        val currentWeek get() = if (!settings.dualWeekSchedule) EVERY else if (isWeekOdd()) ODD else EVEN

    }

    override fun toString(): String = App.str(
        when (this) {
            EVERY -> R.string.every_week
            EVEN -> R.string.even_week
            ODD -> R.string.odd_week
        }
    )
}