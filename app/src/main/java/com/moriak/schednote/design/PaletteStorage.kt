package com.moriak.schednote.design

import com.moriak.schednote.App
import com.moriak.schednote.settings.ColorGroup
import com.moriak.schednote.settings.Prefs
import java.util.*

/**
 * V tejto triede možno zjednodušene pristupovať k farbám alebo k zoznamu všetkých farieb
 */
class PaletteStorage {
    companion object {
        /**
         * Načítanie zoznamu všetkých farebných skupín
         * @return [IntArray] zoznam ID-ečiek farieb
         */
        fun keys(): IntArray = ArrayList<Int>().apply {
            for (c in ColorGroup.values()) add(-c.ordinal)
            for ((id, _) in App.data.colors()) add(id)
        }.toIntArray()

        /**
         * Získanie farby
         * @param key označenie farby
         * @param palette Farebný objekt v ktorom farbu treba nastaviť
         */
        fun getColor(key: Int, palette: Palette) {
            if (key < 1) Prefs.settings.getColor(ColorGroup.values()[-key], palette)
            else App.data.color(key, palette)
        }

        /**
         * Získanie popisu farby
         * @param key označenie farby
         * @return Popis farby
         */
        fun getString(key: Int): String? =
            if (key < 1) ColorGroup.values()[-key].toString() else App.data.lessonType(key)
                ?.toString()
    }

    private val colors = TreeMap<Int, Palette>().also {
        for (cg in ColorGroup.values()) it[-cg.ordinal] = Prefs.settings.getColor(cg)
        it.putAll(App.data.colors())
    }

    /**
     * Získanie farby s kľúčom [key]
     * @param key kľúč
     * @return Farebný objekt [Palette]
     */
    operator fun get(key: Int) = colors[key]

    /**
     * Získanie farby
     * @param key označenie farby
     * @param value Farebný objekt [Palette]
     */
    operator fun set(key: Int, value: Palette) {
        colors[key]?.set(value) ?: let { colors[key] = value }
    }

    /**
     * Uloženie zmeny danej farby
     * @param key kľúč k cieľovej farbe
     */
    fun save(key: Int) {
        if (key < 1) Prefs.settings.setColor(ColorGroup.values()[-key], colors[key]!!)
        else App.data.setColor(key, colors[key]!!)
    }
}
