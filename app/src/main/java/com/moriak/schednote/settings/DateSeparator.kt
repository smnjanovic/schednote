package com.moriak.schednote.settings

import com.moriak.schednote.App
import com.moriak.schednote.R

enum class DateSeparator(val separator: Char) {
    STROKE('/'),
    DOT('.'),
    DASH('-'),
    SPACE(' ');

    companion object {
        operator fun get(name: String) = when (name) {
            STROKE.name -> STROKE
            DOT.name -> DOT
            DASH.name -> DASH
            SPACE.name -> SPACE
            else -> throw IllegalArgumentException("Illegal separator!")
        }
    }

    override fun toString(): String = App.str(
        when (this) {
            STROKE -> R.string.stroke
            DOT -> R.string.dot
            DASH -> R.string.dash
            SPACE -> R.string.space
        }
    )
}