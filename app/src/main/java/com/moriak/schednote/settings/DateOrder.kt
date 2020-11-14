package com.moriak.schednote.settings

import com.moriak.schednote.App
import java.util.*
import java.util.Calendar.*

enum class DateOrder {
    YMD, DMY, MDY;

    companion object {
        operator fun get(name: String) = when (name) {
            YMD.name -> YMD
            DMY.name -> DMY
            MDY.name -> MDY
            else -> throw IllegalArgumentException("Order of year, month and day of month was not recognized!")
        }
    }

    fun getFormat(sep: DateSeparator, millis: Long): String =
        getFormat(sep, App.cal.apply { timeInMillis = millis })

    fun getFormat(sep: DateSeparator, cal: Calendar): String = String.format(
        "%02d%c%02d%c%02d",
        when (this) {
            YMD -> cal.get(YEAR)
            DMY -> cal.get(DAY_OF_MONTH)
            MDY -> cal.get(MONTH) + 1
        },
        sep.separator,
        if (this == MDY) cal.get(DAY_OF_MONTH) else cal.get(MONTH) + 1,
        sep.separator,
        if (this == YMD) cal.get(DAY_OF_MONTH) else cal.get(YEAR)
    )
}