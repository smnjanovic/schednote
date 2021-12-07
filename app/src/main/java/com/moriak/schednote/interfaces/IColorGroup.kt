package com.moriak.schednote.interfaces

import com.moriak.schednote.Palette
import com.moriak.schednote.enums.ColorGroup
import com.moriak.schednote.storage.SQLite

/**
 * Dizajn rozvrhu sa skladá z niekoľkých zoskupených častí, ktoré majú spoločné sfarbenie
 * @property id identifikácia farebnej skupiny
 * @property color paleta s dátami o farbe
 */
interface IColorGroup {
    val id: Int
    val color: Palette
    companion object {
        fun getGroups() = arrayOf<IColorGroup>() + ColorGroup.values() + SQLite.lessonTypes()
        operator fun get(id: Int) = if (id < 1) ColorGroup.values()[-id] else SQLite.lessonType(id)
    }
}