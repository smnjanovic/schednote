package com.moriak.schednote.enums

import android.content.Context
import com.moriak.schednote.enums.Day.*
import com.moriak.schednote.enums.WorkWeek.*

/**
 * Reprezentuje pracovný týždeň. Nie všade sa pracuje alebo uči od pondelka do piatku.
 * Všetky možné pracovné dni sveta ktoré existujú možno nájsť tu:
 * https://en.wikipedia.org/wiki/Workweek_and_weekend.
 * [SAT_WED] sobota - streda
 * [SAT_THU] sobota - štvrtok
 * [SUN_WED] nedeľa - streda
 * [MON_THU] pondelok - štvrtok
 * [MON_FRI] pondelok - piatok
 * [MON_SAT] pondelok - sobota
 * @property workDays pole pracovných dní
 * @property weekend pole dní víkendu
 */
enum class WorkWeek(val workDays: Array<Day>, val weekend: Array<Day>) {
    SAT_WED(arrayOf(SAT, SUN, MON, TUE, WED), arrayOf(THU, FRI)),
    SAT_THU(arrayOf(SAT, SUN, MON, TUE, WED, THU), arrayOf(FRI)),
    SUN_WED(arrayOf(SUN, MON, TUE, WED), arrayOf(THU, FRI, SAT)),
    MON_THU(arrayOf(MON, TUE, WED, THU), arrayOf(FRI, SAT, SUN)),
    MON_FRI(arrayOf(MON, TUE, WED, THU, FRI), arrayOf(SAT, SUN)),
    MON_SAT(arrayOf(MON, TUE, WED, THU, FRI, SAT), arrayOf(SUN));
    fun getDescription(context: Context): String = "${context.getString(workDays.first().res)} - ${context.getString(workDays.last().res)}"
}