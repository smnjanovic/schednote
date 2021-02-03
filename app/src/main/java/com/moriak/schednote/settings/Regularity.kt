package com.moriak.schednote.settings

import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.settings.Prefs.settings
import com.moriak.schednote.settings.Regularity.*
import com.moriak.schednote.settings.Regularity.Companion.currentWeek
import java.util.Calendar.DAY_OF_WEEK
import java.util.Calendar.DAY_OF_YEAR

/**
 * Táto trieda reprezentuje interval opakovania sa rozvrhu, ktorý môže byť týždenný alebo 2-týždenný.
 * Pri 2-týždennom rozvrhu sa určuje párnosť týždňa
 *
 * @property EVERY Každý týždeň
 * @property ODD Nepárny týždeň
 * @property EVEN Párný týždeň
 *
 * @property odd Zisťuje sa, čo sa jedná o nepárny týždeň
 *  [EVEN] = false, pretože nevráti nepárny týždeň
 *  [ODD] = true, pretože ukazuje na nepárny týždeň
 *  [EVERY] = null, pretože sa jedná o ľubovoľný týždeň ktorý môže a nemusí byť nepárny
 * @property currentWeek Keď nie je nastavený 2-týždenný rozvrh okamžite vráti hodnotu [EVERY], inak
 * sa vypočíta, či je tento týždeň párny [EVEN] alebo nepárny [ODD] na základe súčasného nastavenia pracovného týždňa
 */
enum class Regularity(val odd: Boolean?) {
    EVERY(null), EVEN(false), ODD(true);

    /**
     * Statický objekt [Regularity.Companion]
     *
     * @property values Na základe súčasných nastavení vráti pole dostupných pohľadov na týždeň,
     *  napr. keď v objekte [Prefs] bol vypnutý pohľad na 2-týždenný rozvrh, pole nebude obsahovať prvky [EVEN] a [ODD].
     */
    companion object {
        val values
            get() = when {
                settings.dualWeekSchedule -> arrayOf(EVERY, EVEN, ODD)
                else -> arrayOf(EVERY)
            }

        /**
         * Porovná sa ktorá z inštancii [Regularity] má parameter [odd] zhodný z argumentom [value] a takú inštanciu metóda vráti
         * @param value hodnota, ktorá sa musí zhodovať s atribútom [odd] práve jednej z inštancii [Regularity]
         * @return inštancia [Regularity] vyhovujúca hodnote [value]
         */
        operator fun get(value: Boolean?) = value?.let { if (it) ODD else EVEN } ?: EVERY

        /**
         * Výpočet párnosti alebo nepárnosti týždňa dátumu [millis] uvedeného v milisekundách
         * @param millis
         */
        fun isWeekOdd(millis: Long = System.currentTimeMillis()): Boolean {
            App.cal.timeInMillis = millis
            val today: Int = App.cal.get(DAY_OF_YEAR)
            App.cal.set(DAY_OF_YEAR, 1)
            App.cal.add(
                DAY_OF_YEAR,
                (settings.workWeek.workDay.first().value - App.cal.get(DAY_OF_WEEK)).let { if (it < 1) it + 7 else it })
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