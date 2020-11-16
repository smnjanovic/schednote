package com.moriak.schednote.settings

import com.moriak.schednote.App
import com.moriak.schednote.settings.DateOrder.*
import java.util.Calendar.*

/**
 * Poradie dňa mesiaca a roka v textovej reprezentácii dátumu
 *
 * @property YMD Rok mesiac deň
 * @property DMY Deň mesiac rok
 * @property MDY Mesiac deň rok
 */
enum class DateOrder {
    YMD, DMY, MDY;

    companion object {
        /**
         * Získanie inštancie [DateOrder] na základe zhody jej názvu [name] s argumentom [key]
         * @param key Porovnávaný výraz
         * @return [DateOrder]
         * @throws IllegalArgumentException Keď žiadna inštancia [DateOrder] nemá atribút [name] zhodný s argumentom [key]
         */
        operator fun get(key: String) = when (key) {
            YMD.name -> YMD
            DMY.name -> DMY
            MDY.name -> MDY
            else -> throw IllegalArgumentException("Order of year, month and day of month was not recognized!")
        }
    }


    /**
     * Získanie textového formátu dátumu
     * @param sep Oddeľovač dňa, mesiaca a roku
     * @param millis dátum udávaný v milisekundách
     * @return Textový popis dátumu
     */
    fun getFormat(sep: DateSeparator, millis: Long): String {
        App.cal.timeInMillis = millis
        val p1 = when (this) {
            YMD -> App.cal.get(YEAR)
            DMY -> App.cal.get(DAY_OF_MONTH)
            MDY -> App.cal.get(MONTH) + 1
        }
        val p2 = if (this == MDY) App.cal.get(DAY_OF_MONTH) else App.cal.get(MONTH) + 1
        val p3 = if (this == YMD) App.cal.get(DAY_OF_MONTH) else App.cal.get(YEAR)
        return String.format("%02d%c%02d%c%02d", p1, sep.separator, p2, sep.separator, p3)
    }
}