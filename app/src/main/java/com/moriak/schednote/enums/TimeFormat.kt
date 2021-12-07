package com.moriak.schednote.enums

import androidx.annotation.IntRange
import androidx.annotation.StringRes
import com.moriak.schednote.R
import com.moriak.schednote.dayMinutes
import com.moriak.schednote.enums.TimeFormat.H12
import com.moriak.schednote.enums.TimeFormat.H24
import java.util.*

/**
 * Formát času
 * [H12] 12 hodinový formát času
 * [H24] 24 hodinový formát času
 * @property res ID reťazca, ktorý popisuje formát času
 */
enum class TimeFormat(@StringRes val res: Int) {
    H12(R.string.time_format_12) {
        override fun time(dMin: Int) = super.time(if (dMin < 720) dMin else dMin - 720)
        override fun getFormat(dayMinute: Int): String = "${time(dayMinute)} ${when (dayMinute) {
            in 0 until 720 -> if (dayMinute == 0) "M" else "am"
            else -> if (dayMinute == 720) "N" else "pm"
        }}"
    }, H24(R.string.time_format_24) {
        override fun getFormat(dayMinute: Int): String = time(dayMinute)
    };
    private companion object { private val cal: Calendar by lazy { Calendar.getInstance() } }

    protected open fun time(dMin: Int) = "${dMin / 60}:${dMin / 10 % 6}${dMin % 10}"

    /**
     * Získať textovú formu času časového úseku udávaného v milisekundách
     * @param millis Dátum v milisekundách
     * @return Textom vyjadrený čas
     */
    fun getFormat(millis: Long) = getFormat(cal.apply { timeInMillis = millis }.dayMinutes)

    /**
     * Získať textovú formu času časového úseku udávaného v milisekundách
     * @param dayMinute počet minút od polnoci
     * @return Textom vyjadrený čas
     */
    abstract fun getFormat(@IntRange(from = 0, to = 1439) dayMinute: Int): String
}