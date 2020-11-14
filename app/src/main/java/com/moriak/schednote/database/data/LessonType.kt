package com.moriak.schednote.database.data

import android.text.Editable

data class LessonType(val id: Int, val name: String) {
    companion object {
        const val MAX_COUNT = 5
        const val limit = 28
        val invalidName = "(^[^a-zA-ZÀ-ž])|([^a-zA-ZÀ-ž0-9 ])|([ ][ ]+)".toRegex()
        fun isNameValid(name: Editable?) =
            name?.let { it.isNotEmpty() && !it.toString().matches(invalidName) } ?: false
    }

    override fun toString(): String = name
}