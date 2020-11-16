package com.moriak.schednote.other

import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.database.data.NoteCategory
import com.moriak.schednote.other.TimeCategory.*

/**
 * [TimeCategory] predstavuje niektoré prevažne časovo orientované kategórie úloh
 * @property ALL výber všetkých úloh
 * @property TIMELESS výber úloh bez konečného termínu
 * @property LATE výber neplatných úloh, ktorých čas na splnenie už vypršal
 * @property TODAY výber úloh, ktoré treba stihnúť odovzať ešte dnes
 * @property TOMORROW výber úloh, ktoré treba stihnúť odovzať už zajtra
 * @property IN_WEEK výber úloh, ktoré treba stihnúť odovzať v tomto týždni
 */
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