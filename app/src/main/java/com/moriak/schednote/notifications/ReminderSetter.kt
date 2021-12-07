package com.moriak.schednote.notifications

import android.content.Context
import androidx.annotation.StringRes
import com.moriak.schednote.R
import com.moriak.schednote.data.Note
import com.moriak.schednote.enums.TimeCategory.UPCOMING
import com.moriak.schednote.interfaces.NoteCategory
import com.moriak.schednote.storage.SQLite
import com.moriak.schednote.widgets.NoteWidget
import java.lang.System.currentTimeMillis as now

/**
 * [ReminderSetter] slúži ako organizátor úloh.
 */
object ReminderSetter {
    private enum class EditResult(@StringRes val res: Int?) {
        SUCCESS(null),
        NO_SUBJECT(R.string.note_without_subject_link),
        TIME_OUT(R.string.time_out),
        NO_DESCRIPTION(R.string.note_no_description),
        TOO_LONG(R.string.note_description_length_exceeded)
    }
    private data class SaveResult(val resultState: EditResult, val resultId: Long)
    private fun fail(state: EditResult) = SaveResult(state, -1L)
    private fun succeed(note: Long) = SaveResult(EditResult.SUCCESS, note)

    private fun setNote(id: Long, subject: Long, info: String, deadline: Long?): SaveResult {
        val sub = SQLite.subject(subject) ?: return fail(EditResult.NO_SUBJECT)
        if (info.trim().isEmpty()) return fail(EditResult.NO_DESCRIPTION)
        if (info.length > 255) return fail(EditResult.TOO_LONG)
        deadline?.let { if (it <= now()) return fail(EditResult.TIME_OUT) }
        val resultId = when {
            id == 1L -> SQLite.addNote(sub.id, info, deadline)
            SQLite.updateNote(id, sub.id, info, deadline) == 1 -> id
            else -> SQLite.addNote(sub.id, info, deadline)
        }
        if (resultId == -1L) throw RuntimeException("An error occured while modifying notes!")
        return succeed(resultId)
    }

    private fun note(id: Long, note: Note): Note {
        if (id == note.id) return note
        return Note(id, note.sub, note.info, note.deadline)
    }

    /**
     * Pokúsi sa vytvoriť úlohu, uložiť ju do databázy a nastaviť k nej pripomienku,
     * ak sú upozornenia na pripomienky zapnuté.
     * @param context Ľubovoľný kontext
     * @param note Úloha, ktorú sa snažím nastaviť
     * @param onSuccess Funkcia, ktorá sa má vykonať po úspešnom nastavení úlohy
     * @return Po úspešnom nastavení úlohy vráti null, inak vráti odkaz na text popisujúci
     * dôvod neúspechu vytvorenia úlohy.
     */
    @StringRes fun setNote(context: Context, note: Note, onSuccess: (Note)->Unit): Int? {
        val result = setNote(note.id, note.sub.id, note.info, note.deadline)
        return result.resultState.res ?: null.also {
            val newNote = note(result.resultId, note)
            onSuccess(newNote)
            NoteWidget.update(context)
            NotificationHandler.Reminder.setReminder(context, note, true)
        }
    }

    /**
     * Pokúsi sa odstrániť danú úlohu z databázy a zrušiť naplánované upozornenie.
     * @param context Ľubovoľný kontext
     * @param note Úloha na odstránenie
     * @return true, ak sa úlohu podarilo odstrániť.
     */
    fun unsetNote(context: Context, note: Note): Boolean {
        val deleted = SQLite.removeNote(note.id) > 0
        NotificationHandler.Reminder.hideNotification(context, note)
        if (deleted) {
            NotificationHandler.Reminder.setReminder(context, note, true)
            NoteWidget.update(context)
        }
        return deleted
    }

    /**
     * Pridanie viacerých úloh.
     * @param context Ľubovoľný kontext
     * @param notes zoznam úloh na vloženie
     */
    fun addNotes(context: Context, notes: List<Note>) {
        if (SQLite.insertMultipleNotes(notes) == 0) return
        NotificationHandler.Reminder.setReminders(context, notes.filter { n ->
            n.deadline?.let { it > now() } == true
        }, true)
        NoteWidget.update(context)
    }

    /**
     * Odstráni všetky úlohy patriace do kategórie [category] z databázy, a k nim naplánované upozornenia.
     * @param context Ľubovoľný kontext
     * @param category kategória úloh
     */
    fun clearNotesOfCategory(context: Context, category: NoteCategory) {
        NotificationHandler.Reminder.setReminders(context, SQLite.notes(UPCOMING, category), false)
        if (SQLite.clearNotesOfCategory(category) > 0) NoteWidget.update(context)
    }

    /**
     * naplánuje alebo zruší upozornenia na všetky nadchádzajúce úlohy
     * @param context
     * @param enable Ak true, tak naplánuje, inak zruší upozornenia na všetky nadchádzajúce úlohy
     */
    fun enableReminders(context: Context, enable: Boolean = true): Boolean =
        NotificationHandler.Reminder.setReminders(context, SQLite.notes(UPCOMING), enable)
}