package com.moriak.schednote.enums

import androidx.annotation.StringRes
import com.moriak.schednote.R
import com.moriak.schednote.enums.DateFormat.Order.*
import com.moriak.schednote.enums.DateFormat.Split.*
import java.util.*
import java.util.Calendar.*

/**
 * Formát dátumu
 * @property order poradie dňa mesiaca a roku
 * @property split oddeľovač dňa mesiaca a roku
 */
enum class DateFormat(val order: Order, val split: Split) {
    YMD_STROKE(YMD, STROKE),
    DMY_STROKE(DMY, STROKE),
    MDY_STROKE(MDY, STROKE),
    YMD_DOT(YMD, DOT),
    DMY_DOT(DMY, DOT),
    MDY_DOT(MDY, DOT),
    YMD_DASH(YMD, DASH),
    DMY_DASH(DMY, DASH),
    MDY_DASH(MDY, DASH),
    YMD_SPACE(YMD, SPACE),
    DMY_SPACE(DMY, SPACE),
    MDY_SPACE(MDY, SPACE);

    /**
     * Oddeľovač dňa, mesiaca a roku
     * @property separator Znak, ktorý rozdeľuje deň, mesiac a rok
     */
    enum class Split(val separator: Char, @StringRes val res: Int) {
        STROKE('/', R.string.stroke),
        DOT('.', R.string.dot),
        DASH('-', R.string.dash),
        SPACE(' ', R.string.space);
    }

    /**
     * Poradie dňa mesiaca a roku
     */
    enum class Order {
        YMD { override fun getArray(y: Int, m: Int, d: Int): IntArray = intArrayOf(y, m + 1, d) },
        DMY { override fun getArray(y: Int, m: Int, d: Int): IntArray = intArrayOf(d, m + 1, y) },
        MDY { override fun getArray(y: Int, m: Int, d: Int): IntArray = intArrayOf(m + 1, d, y) };

        private companion object { private val cal: Calendar by lazy { getInstance() } }

        protected abstract fun getArray(y: Int, m: Int, d: Int): IntArray

        /**
         * Získa zoradené pole s údajmi: deň, mesiac, rok.
         * @param millis čas v milisekundách z ktorého tieto údaje možno získať
         * @return pole 3 čísel (deň, mesiac, rok) v určitom poradí
         */
        fun getArray(millis: Long): IntArray = cal.let {
            it.timeInMillis = millis
            getArray(it.get(YEAR), it.get(MONTH), it.get(DAY_OF_MONTH))
        }
    }

    /**
     * Výpis dátumu v špecifickom formáte
     * @param millis čas v milisekundách na základe ktorého získam deň, mesiac a rok.
     * @return Textová reprezentácia dátumu
     */
    fun getFormat(millis: Long) = order.getArray(millis).joinToString("${split.separator}")
}