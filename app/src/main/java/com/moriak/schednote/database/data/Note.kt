package com.moriak.schednote.database.data

import com.moriak.schednote.R

/**
 * Trieda na uchovávanie dát o úlohach
 * @property id Id úlohy
 * @property sub Predmet, ktorému úloha patrí
 * @property description Popis úlohy
 * @property deadline Dátum vypršania úlohy (v milisekundách). Môže byť null
 */
data class Note(
    val id: Long,
    val sub: Subject,
    val description: String,
    val deadline: Long? = null
) {
    /**
     * @property limit maximálny počet znakov úlohy
     * @property validFormat správny formát úlohy - bez zbytočných medzier
     */
    companion object {
        const val limit: Long = 256
        val validFormat = "^[^\\s](\\s|.)*$".toRegex()

        /**
         * Overenie správnosti formátu úlohy
         */
        fun validDescription(description: String?) = when {
            description == null || description.trim().isEmpty() -> R.string.note_no_description
            description.length > limit -> R.string.note_description_length_exceeded
            !description.matches(validFormat) -> R.string.note_invalid_format
            else -> null
        }
    }
}