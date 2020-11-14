package com.moriak.schednote.design

import com.moriak.schednote.App
import com.moriak.schednote.settings.ColorGroup
import com.moriak.schednote.settings.Prefs
import java.util.*

class PaletteStorage {
    companion object {
        fun keys(): IntArray = ArrayList<Int>().apply {
            for (c in ColorGroup.values()) add(-c.ordinal)
            for ((id, _) in App.data.colors()) add(id)
        }.toIntArray()

        fun getColor(key: Int, palette: Palette) {
            if (key < 1) Prefs.settings.getColor(ColorGroup.values()[-key], palette)
            else App.data.color(key, palette)
        }

        fun getString(key: Int): String? =
            if (key < 1) ColorGroup.values()[-key].toString() else App.data.lessonType(key)
                ?.toString()
    }

    private val colors = TreeMap<Int, Palette>().also {
        for (cg in ColorGroup.values()) it[-cg.ordinal] = Prefs.settings.getColor(cg)
        it.putAll(App.data.colors())
    }

    operator fun get(key: Int) = colors[key]

    operator fun set(key: Int, value: Palette) {
        colors[key]?.set(value) ?: let { colors[key] = value }
    }

    fun keys(): IntArray = colors.keys.sorted().toIntArray()

    fun save(key: Int) {
        if (key < 1) Prefs.settings.setColor(ColorGroup.values()[-key], colors[key]!!)
        else App.data.setColor(key, colors[key]!!)
    }
}
