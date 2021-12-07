package com.moriak.schednote.enums

import androidx.annotation.IdRes
import com.moriak.schednote.R
import com.moriak.schednote.enums.ScheduleDisplay.*
import com.moriak.schednote.fragments.SubActivity
import com.moriak.schednote.fragments.of_schedule.DesignEditor
import com.moriak.schednote.fragments.of_schedule.LessonTimeList
import com.moriak.schednote.fragments.of_schedule.LessonTypeList
import com.moriak.schednote.fragments.of_schedule.ScheduleEditor
import com.moriak.schednote.interfaces.ISubContent
import com.moriak.schednote.storage.Prefs

/**
 * Inštancie tejto triedy predstavujú obsah týkajúci sa rozvrhu.
 * [DESIGN] Položka sa sústreďuje na úpravy dizajnu rozvrrhu
 * [LESSON_SCHEDULE] Položka sa sústreďuje na náhľad a zmeny v rozvrhu
 * [TIME_SCHEDULE] Položka sa sústreďuje hodiny ako najmenšie časové jednotky rozvrhu
 * [LESSON_TYPES] Položka sa sústreďuje na zobrazenie a manipuláciu s typmi hodín
 */

enum class ScheduleDisplay(
    @IdRes override val button: Int,
    override val fragmentClass: Class<out SubActivity>
): ISubContent {
    DESIGN(R.id.sched_view_btn, DesignEditor::class.java),
    LESSON_SCHEDULE(R.id.sched_edit_btn, ScheduleEditor::class.java),
    TIME_SCHEDULE(R.id.lesson_set_btn, LessonTimeList::class.java),
    LESSON_TYPES(R.id.les_types_btn, LessonTypeList::class.java);
    override val parent: ISubContent? get() = SubContent.SCHEDULE
    override fun remember() { Prefs.States.lastScheduleDisplay = this }
    companion object: ISubContent.ISubContentCompanion {
        @IdRes override val container: Int = R.id.schedule_part
        override val values: Array<out ISubContent> = values()
        override val layout: Int = R.layout.schedule
        override val lastSet: ISubContent get() = Prefs.States.lastScheduleDisplay
    }
}