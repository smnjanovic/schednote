package com.moriak.schednote.data

import com.moriak.schednote.interfaces.NoteCategory

/**
 * Trieda uchováva dáta o predmete.
 * @property id predmetu / kategórie
 * @property abb skratka predmetu
 * @property name celý názov predmetu
 */
data class Subject(override val id: Long, var abb: String, var name: String) : NoteCategory {
    override fun toString() = "$abb — $name"
}