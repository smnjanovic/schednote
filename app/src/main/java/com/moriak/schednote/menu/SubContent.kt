package com.moriak.schednote.menu

import com.moriak.schednote.R
import com.moriak.schednote.fragments.of_main.*

enum class SubContent {
    SCHEDULE, SUBJECTS, NOTES, ALARMS, SEMESTER;

    companion object {
        fun giveEnum(pName: String?): SubContent {
            for (e in values())
                if (e.name == pName)
                    return e
            return SCHEDULE
        }

        fun giveEnum(res: Int): SubContent {
            for (e in values())
                if (e.resId == res)
                    return e
            return SCHEDULE
        }
    }

    val resId: Int
        get() = when (this) {
            SCHEDULE -> R.id.schedule
            SUBJECTS -> R.id.subjects
            NOTES -> R.id.notes
            ALARMS -> R.id.alarms
            SEMESTER -> R.id.semester
        }
    val fragment
        get() = when (this) {
            SCHEDULE -> ScheduleFragment()
            SUBJECTS -> SubjectsFragment()
            NOTES -> NotesFragment()
            ALARMS -> AlarmsFragment()
            SEMESTER -> SemesterFragment()
        }
}