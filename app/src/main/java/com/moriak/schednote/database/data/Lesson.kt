package com.moriak.schednote.database.data

import android.text.Editable
import android.text.TextWatcher
import com.moriak.schednote.other.Day
import com.moriak.schednote.settings.Prefs
import com.moriak.schednote.settings.Regularity


data class Lesson(
    val id: Long,
    override val regularity: Regularity,
    override val day: Day,
    override val time: IntRange,
    val type: Int,
    val sub: Subject,
    val room: String?
) : ScheduleEvent {
    init {
        ScheduleEvent.rangeCheck(time)
    }

    companion object {
        const val room_limit = 20
        fun roomValid(editable: Editable?) = editable
            ?.trimStart()
            ?.replace("\\s+".toRegex(), " ")
            ?.let { if (it.length > room_limit) it.substring(0, room_limit) else it }
    }

    object RoomWatcher : TextWatcher {
        private val invalid = "(\\s\\s+)|(^\\s+)".toRegex()
        private var st = 0
        private var en = 0
        override fun afterTextChanged(s: Editable?) {
            if (s == null) return
            if (s.length > 20 || s.contains(invalid)) s.delete(st, en)
            if (s.length > 20 || s.contains(invalid))
                s.replace(0, s.length, s.trimStart().replace("\\s+".toRegex(), " "))
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            st = start
            en = start + count
        }

    }

    override fun isEqual(scheduleEvent: ScheduleEvent?): Boolean =
        scheduleEvent is Lesson && sub == scheduleEvent.sub && type == scheduleEvent.type && room == scheduleEvent.room

    override fun toString(): String = String.format("%s  — %s%s %s",
        sub.abb,
        day.toString(),
        regularity
            .odd?.let { if (it) " I." else " II." } ?: "",
        Prefs.settings.lessonTimeFormat.rangeFormat(time))
}