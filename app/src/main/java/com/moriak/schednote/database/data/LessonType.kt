package com.moriak.schednote.database.data

import android.text.Editable

/**
 * Trieda uchováva dáta o type hodiny
 * @property id id typu hodiny
 * @property name názov typu hodiny
 */
data class LessonType(val id: Int, val name: String) {
    /**
     * @property MAX_COUNT Maximálny počet typov hodín
     * @property limit Maximálny počet znakov v názve typu hodiny
     * @property invalidName neplatný formát názvu typu hodiny
     */
    companion object {
        const val MAX_COUNT = 5
        const val limit = 28
        val invalidName = "(^[^a-zA-ZÀ-ž])|([^a-zA-ZÀ-ž0-9 ])|([ ][ ]+)".toRegex()

        /**
         * Kontrola správnosti formátu názvu typu hodiny
         * @param name Kontrolovaný text
         */
        fun isNameValid(name: Editable?) =
            name?.let { it.isNotEmpty() && !it.toString().matches(invalidName) } ?: false
    }

    override fun toString(): String = name
}