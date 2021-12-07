package com.moriak.schednote.enums

import androidx.annotation.StringRes
import com.moriak.schednote.R.string.*
import com.moriak.schednote.enums.TimeCategory.*
import com.moriak.schednote.interfaces.NoteCategory

/**
 * Kategória, podľa ktorej sa filtruje zoznam úloh
 * [ALL] všetky úlohy
 * [TIMELESS] úlohy bez termínu
 * [LATE] oneskorené / staré úlohy
 * [UPCOMING] nadchádzajúce úlohy
 * [TODAY] dnešné úlohy
 * [TOMORROW] úlohy na zajtra
 * [IN_WEEK] úlohy na nasledujúcich 7 dní.
 * [IN_MONTH] úlohy na nasledujúcich 28-31 dní
 * @property res Odkaz na popis časovej kategórie
 */
enum class TimeCategory(@StringRes val res: Int) : NoteCategory {
    ALL(all), TIMELESS(timeless), LATE(late), UPCOMING(upcoming),
    TODAY(today), TOMORROW(tomorrow), IN_WEEK(in_week), IN_MONTH(in_month);
    override val id: Long = -ordinal.toLong()
}