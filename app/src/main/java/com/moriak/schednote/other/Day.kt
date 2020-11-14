package com.moriak.schednote.other

import com.moriak.schednote.App
import com.moriak.schednote.R
import java.util.Calendar.*

enum class Day(val value: Int) {
    MON(MONDAY), TUE(TUESDAY), WED(WEDNESDAY), THU(THURSDAY), FRI(FRIDAY), SAT(SATURDAY), SUN(SUNDAY);

    companion object {
        operator fun get(n: Int) = when (n) {
            MONDAY -> MON
            TUESDAY -> TUE
            WEDNESDAY -> WED
            THURSDAY -> THU
            FRIDAY -> FRI
            SATURDAY -> SAT
            SUNDAY -> SUN
            else -> throw IndexOutOfBoundsException("Out of week-range!")
        }
    }

    override fun toString() = App.str(
        when (this) {
            MON -> R.string.mon
            TUE -> R.string.tue
            WED -> R.string.wed
            THU -> R.string.thu
            FRI -> R.string.fri
            SAT -> R.string.sat
            SUN -> R.string.sun
        }
    )
}