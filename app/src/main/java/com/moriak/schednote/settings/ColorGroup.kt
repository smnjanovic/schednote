package com.moriak.schednote.settings

import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.design.Palette

enum class ColorGroup {
    BACKGROUND, TABLE_HEAD, FREE;

    val default
        get() = when (this) {
            BACKGROUND -> Palette.ahex("#FFFFFFFF")
            TABLE_HEAD -> Palette.resource(R.color.juicy)
            FREE -> Palette.ahsl(35, 0, 0, 0)
        }

    override fun toString(): String = App.str(
        when (this) {
            BACKGROUND -> R.string.background
            TABLE_HEAD -> R.string.header
            FREE -> R.string.free
        }
    )
}