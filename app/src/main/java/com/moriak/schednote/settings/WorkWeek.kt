package com.moriak.schednote.settings

import com.moriak.schednote.other.Day
import com.moriak.schednote.other.Day.*
import com.moriak.schednote.settings.WorkWeek.*

/**
 * Reprezentuje pracovný týždeň. Nie všade sa pracuje alebo uči od pondelka do piatku.
 * Všetky možné pracovné dni sveta ktoré existujú možno nájsť tu: https://en.wikipedia.org/wiki/Workweek_and_weekend
 *
 * @property SAT_WED sobota - streda
 * @property SAT_THU sobota - štvrtok
 * @property SUN_WED nedeľa - streda
 * @property MON_THU pondelok - štvrtok
 * @property MON_FRI pondelok - piatok
 * @property MON_SAT pondelok - sobota
 *
 * @property workDay Zoradené pole dní pracovného týždňa
 */
enum class WorkWeek(val workDay: Array<Day>, val weekend: Array<Day>) {
    SAT_WED(arrayOf(SAT, SUN, MON, TUE, WED), arrayOf(THU, FRI)),
    SAT_THU(arrayOf(SAT, SUN, MON, TUE, WED, THU), arrayOf(FRI)),
    SUN_WED(arrayOf(SUN, MON, TUE, WED), arrayOf(THU, FRI, SAT)),
    MON_THU(arrayOf(MON, TUE, WED, THU), arrayOf(FRI, SAT, SUN)),
    MON_FRI(arrayOf(MON, TUE, WED, THU, FRI), arrayOf(SAT, SUN)),
    MON_SAT(arrayOf(MON, TUE, WED, THU, FRI, SAT), arrayOf(SUN));

    companion object {
        /**
         * Identifikácia pracovného týždňa na základe názvu inštancie [WorkWeek].
         * @param name Názov, podľa ktorého identifikujem pracovný týždeň
         * @throws IllegalStateException Keď sa žiadna z enum inštancií nevolá [name]!
         */
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

    override fun toString() = "${workDay.firstOrNull()} - ${workDay.lastOrNull()}"
}