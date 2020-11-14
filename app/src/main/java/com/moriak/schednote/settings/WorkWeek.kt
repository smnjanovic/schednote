package com.moriak.schednote.settings

import com.moriak.schednote.other.Day
import com.moriak.schednote.other.Day.*

enum class WorkWeek(val days: Array<Day>) {
    SUN_WED(arrayOf(SUN, MON, TUE, WED)),
    MON_THU(arrayOf(MON, TUE, WED, THU)),
    SAT_WED(arrayOf(SAT, SUN, MON, TUE, WED)),
    MON_FRI(arrayOf(MON, TUE, WED, THU, FRI)),
    SAT_THU(arrayOf(SAT, SUN, MON, TUE, WED, THU)),
    MON_SAT(arrayOf(MON, TUE, WED, THU, FRI, SAT));

    companion object {
        operator fun get(name: String?): WorkWeek = when (name) {
            SAT_WED.name -> SAT_WED
            SAT_THU.name -> SAT_THU
            SUN_WED.name -> SUN_WED
            MON_THU.name -> MON_THU
            MON_FRI.name -> MON_FRI
            MON_SAT.name -> MON_SAT
            else -> throw IllegalArgumentException()
        }
    }

    override fun toString() = "${days.firstOrNull()} - ${days.lastOrNull()}"
}