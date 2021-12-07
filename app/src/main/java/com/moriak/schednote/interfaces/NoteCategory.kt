package com.moriak.schednote.interfaces

import com.moriak.schednote.enums.TimeCategory
import com.moriak.schednote.storage.SQLite

/**
 * Objekty tohto typu budú slúžiť na označenie kategórie zoznamu úloh
 * @property id Identifikátor kategórie úloh
 */
interface NoteCategory {
    val id: Long
    companion object {
        operator fun get(category: Long): NoteCategory = when {
            category < 1 -> TimeCategory.values()[-category.toInt()]
            else -> SQLite.subject(category) ?: TimeCategory.ALL
        }
    }
}