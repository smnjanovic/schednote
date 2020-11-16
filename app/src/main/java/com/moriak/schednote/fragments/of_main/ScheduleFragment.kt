package com.moriak.schednote.fragments.of_main

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.menu.ScheduleDisplay
import com.moriak.schednote.settings.Prefs
import kotlinx.android.synthetic.main.schedule.view.*

/**
 * Fragment má vnorené ďalšie fragmenty ktoré súvisia s rozvrhom. Vždy je zobrazený iba 1 z nich
 */
class ScheduleFragment : SubActivity() {
    private companion object {
        private val transparent = Color.parseColor("#00000000")
        private val inactiveText = App.ctx.resources.getColor(R.color.textColor, null)
        private val activeButton = App.ctx.resources.getColor(R.color.colorPrimary, null)
        private const val CONTENT = "CONTENT"
    }

    private val choice = View.OnClickListener { v -> activeTab = v.tag as ScheduleDisplay }
    private var activeTab = Prefs.states.lastScheduleDisplay
        set(value) {
            if (field != value) setInactive(field)
            field = value
            setActive(field)
            Prefs.states.lastScheduleDisplay = field
            attachFragment(
                R.id.schedule_part,
                requireFragment(CONTENT, field.fragmentClass.asSubclass(SubActivity::class.java)),
                CONTENT
            )
        }
    private var hasSavedState: Boolean = false

    private fun getButton(display: ScheduleDisplay) = when (display) {
        ScheduleDisplay.DESIGN -> view?.sched_view_btn
        ScheduleDisplay.LESSON_SCHEDULE -> view?.sched_edit_btn
        ScheduleDisplay.TIME_SCHEDULE -> view?.lesson_set_btn
        ScheduleDisplay.LESSON_TYPES -> view?.les_types_btn
    }

    private fun setActive(display: ScheduleDisplay) {
        val btn = getButton(display)
        btn?.foreground?.setTint(activeButton)
        btn?.setTextColor(activeButton)
    }

    private fun setInactive(display: ScheduleDisplay) {
        val btn = getButton(display)
        btn?.foreground?.setTint(transparent)
        btn?.setTextColor(inactiveText)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        saved: Bundle?
    ): View {
        return inflater.inflate(R.layout.schedule, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        hasSavedState = false
        super.onViewCreated(view, savedInstanceState)
        savedInstanceState ?: forceAttachFragment(
            R.id.schedule_part,
            Prefs.states.lastScheduleDisplay.fragmentClass.newInstance() as SubActivity,
            CONTENT
        )
        setActive(Prefs.states.lastScheduleDisplay)
        view.sched_view_btn.tag = ScheduleDisplay.DESIGN
        view.sched_edit_btn.tag = ScheduleDisplay.LESSON_SCHEDULE
        view.lesson_set_btn.tag = ScheduleDisplay.TIME_SCHEDULE
        view.les_types_btn.tag = ScheduleDisplay.LESSON_TYPES

        view.les_types_btn.setOnClickListener(choice)
        view.sched_view_btn.setOnClickListener(choice)
        view.sched_edit_btn.setOnClickListener(choice)
        view.lesson_set_btn.setOnClickListener(choice)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        hasSavedState = true
    }

    override fun removeAllSubFragments() = removeFragment(CONTENT, SubActivity::class.java)
}
