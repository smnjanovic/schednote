package com.moriak.schednote.data

import com.moriak.schednote.Palette
import com.moriak.schednote.interfaces.IColorGroup

/**
 * Trieda uchováva dáta o type hodiny
 * @property id identifikácia typu hodiny a farebnej skupiny
 * @property name názov typu hodiny
 */
data class LessonType(override val id: Int, val name: String, override val color: Palette): IColorGroup {
    override fun toString(): String = name
}