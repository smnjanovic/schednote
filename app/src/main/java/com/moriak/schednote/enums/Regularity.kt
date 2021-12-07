package com.moriak.schednote.enums

import androidx.annotation.StringRes
import com.moriak.schednote.R
import com.moriak.schednote.enums.Regularity.*

/**
 * 2-týždenný rozvrh sa delí na párny a nepárny týždeň. Niektoré hodiny
 * sú len v párnom [EVEN] týždni, niektoré len v nepárnom [ODD] týždni,
 * iné zas v každom týždni [EVERY].
 *
 * 1-týždenný rozvrh nikdy nebude používať tieto inštancie: [ODD], [EVEN].
 *
 * @property res odkaz na popis významu inštancie
 */
enum class Regularity(@StringRes val res: Int, private val label: String) {
    EVEN(R.string.even_week, "II."), ODD(R.string.odd_week, "I."), EVERY(R.string.every_week, "");
    override fun toString(): String = label
}