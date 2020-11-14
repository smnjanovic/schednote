package com.moriak.schednote.other

import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.database.data.NoteCategory

enum class TimeCategory : NoteCategory {
    ALL, TIMELESS, LATE, TODAY, TOMORROW, IN_WEEK;

    override fun toString(): String = App.str(
        when (this) {
            ALL -> R.string.all
            TIMELESS -> R.string.timeless
            LATE -> R.string.late
            TODAY -> R.string.today
            TOMORROW -> R.string.tomorrow
            IN_WEEK -> R.string.in_week
        }
    )
}