package com.moriak.schednote.enums

import androidx.annotation.IntRange
import androidx.annotation.StringRes
import com.moriak.schednote.R
import com.moriak.schednote.enums.Day.*
import java.util.Calendar.*

/**
 * Dni v týždni
 * [MON] pondelok
 * [TUE] utorok
 * [WED] streda
 * [THU] štvrtok
 * [FRI] piatok
 * [SAT] sobota
 * [SUN] nedeľa
 *
 * @property value číslo dňa v týždni
 * @property res Odkaz na reťazec s názvom dňa
 */
enum class Day(val value: Int, @StringRes val res: Int) {
    MON(MONDAY, R.string.mon),
    TUE(TUESDAY, R.string.tue),
    WED(WEDNESDAY, R.string.wed),
    THU(THURSDAY, R.string.thu),
    FRI(FRIDAY, R.string.fri),
    SAT(SATURDAY, R.string.sat),
    SUN(SUNDAY, R.string.sun);

    companion object {
        /**
         * Z čísla dňa získať enum reprezentujúci daný deň
         * @param n číslo dňa v týždni
         * @return [Day] Výsledný deň.
         */
        operator fun get(n: Int): Day = values().find { it.value == n }
            ?: throw IndexOutOfBoundsException("Out of week-range!")
    }

    /**
     * Vypočíta, o koľko dní sa musím posunúť dopredu
     * od momentu: tento deň, čas [currentDayMinutes] (v dňových minútach)
     * do momentu: deň [nextDay], čas [desiredDayMinutes] (v dňových minútach)
     * @param currentDayMinutes
     * @param nextDay
     * @param desiredDayMinutes
     * @return Počet dní do nastatia momentu: deň [nextDay], čas: [desiredDayMinutes]
     */
    fun untilNextTime(
        @IntRange(from = 0, to = 1439) currentDayMinutes: Int,
        nextDay: Day,
        @IntRange(from = 0, to = 1439) desiredDayMinutes: Int
    ): Int = when {
        nextDay.value != value -> (nextDay.value - value).let { if (it < 0) it + 7 else it }
        currentDayMinutes >= desiredDayMinutes -> 7
        else -> 0
    }
}