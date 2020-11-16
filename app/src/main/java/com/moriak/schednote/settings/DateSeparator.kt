package com.moriak.schednote.settings

import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.settings.DateSeparator.*

/**
 * Oddeľovač dňa, mesiaca a roka v dátume v textovom formáte
 *
 * @property separator Znak, ktorý rozdeľuje deň, mesiac a rok
 *
 * @property STROKE '/'
 * @property DOT '.'
 * @property DASH '-'
 * @property SPACE '/'
 */
enum class DateSeparator(val separator: Char) {
    STROKE('/'),
    DOT('.'),
    DASH('-'),
    SPACE(' ');

    companion object {
        /**
         * @return Oddeľovač [DateSeparator] s atribútom [name] zhodným s argumentom [key]
         * @throws IllegalArgumentException Keď sa atribút [name] žiadnej inštancii [DateSeparator] nerovná s argumentom [key]
         */
        operator fun get(key: String) = when (key) {
            STROKE.name -> STROKE
            DOT.name -> DOT
            DASH.name -> DASH
            SPACE.name -> SPACE
            else -> throw IllegalArgumentException("Illegal separator!")
        }
    }

    /**
    @return Slovný popis oddeľovača
     */
    override fun toString(): String = App.str(
        when (this) {
            STROKE -> R.string.stroke
            DOT -> R.string.dot
            DASH -> R.string.dash
            SPACE -> R.string.space
        }
    )
}