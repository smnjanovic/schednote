package com.moriak.schednote.settings

import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.design.Palette
import com.moriak.schednote.settings.ColorGroup.*

/**
 * Pri nastavovaní dizajnu rozvrhu okrem buniek rôznych typov hodín možno upravovať farby aj iných prvkov.
 * Inštancie tejto triedy budú využívané na prístup k zapamätaným farbám, ktoré nesúvisia s typom hodín.
 *
 * @property BACKGROUND farbu pozadia
 * @property TABLE_HEAD farbu hlavičky tabuľky
 * @property FREE farbu bunky, ktorá reprezentuje voľno
 *
 * @property default Pre prípad, že niektorá farba ešte nebola nastavená, táto vlastnosť poskytuje predvolenú farbu ako náhradu.
 */
enum class ColorGroup {
    BACKGROUND, TABLE_HEAD, FREE;

    val default
        get() = when (this) {
            BACKGROUND -> Palette.ahex("#FFFFFFFF")
            TABLE_HEAD -> Palette.resource(R.color.juicy)
            FREE -> Palette.ahsl(35, 0, 0, 0)
        }

    /**
     * @return Popis farebnej skupiny
     */
    override fun toString(): String = App.str(
        when (this) {
            BACKGROUND -> R.string.background
            TABLE_HEAD -> R.string.header
            FREE -> R.string.free
        }
    )
}