package com.moriak.schednote.database.data

import com.moriak.schednote.R

data class Note(
    val id: Long,
    val sub: Subject,
    val description: String,
    val deadline: Long? = null
) {
    companion object {
        const val limit: Long = 256
        val validFormat = "^[^\\s](\\s|.)*$".toRegex()

        fun validDescription(description: String?) = when {
            description == null || description.trim().isEmpty() -> R.string.note_no_description
            description.length > limit -> R.string.note_description_length_exceeded
            !description.matches(validFormat) -> R.string.note_invalid_format
            else -> null
        }
    }
}