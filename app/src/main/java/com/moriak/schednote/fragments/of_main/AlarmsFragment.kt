package com.moriak.schednote.fragments.of_main

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.menu.AlarmCategory
import com.moriak.schednote.settings.Prefs
import kotlinx.android.synthetic.main.alarms.view.*

/**
 * V tento fragment má vnorené ďalšie fragmenty súvisiace s upozorneniami (Budíky a notifikácie)
 */
class AlarmsFragment : SubActivity() {
    private companion object {
        private val transparent = Color.parseColor("#00000000")
        private val inactiveText = App.ctx.resources.getColor(R.color.textColor, null)
        private val activeButton = App.ctx.resources.getColor(R.color.colorPrimary, null)
        private const val CONTENT = "CONTENT"
    }

    private val choice = View.OnClickListener { v -> activeTab = v.tag as AlarmCategory }
    private var activeTab = Prefs.states.lastAlarmCategory
        set(value) {
            if (field != value) setInactive(field)
            field = value
            setActive(field)
            Prefs.states.lastAlarmCategory = field
            attachFragment(
                R.id.alarm_set_zone,
                requireFragment(CONTENT, field.fragmentClass.asSubclass(SubActivity::class.java)),
                CONTENT
            )
        }
    private var hasSavedState: Boolean = false

    private fun getButton(category: AlarmCategory) = when (category) {
        AlarmCategory.ALARM -> view?.alarms
        AlarmCategory.REMINDER -> view?.reminders
    }

    private fun setActive(category: AlarmCategory) {
        val btn = getButton(category)
        btn?.foreground?.setTint(activeButton)
        btn?.setTextColor(activeButton)
    }

    private fun setInactive(category: AlarmCategory) {
        val btn = getButton(category)
        btn?.foreground?.setTint(transparent)
        btn?.setTextColor(inactiveText)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) =
        inflater.inflate(R.layout.alarms, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        hasSavedState = false
        super.onViewCreated(view, savedInstanceState)
        savedInstanceState ?: forceAttachFragment(
            R.id.alarm_set_zone,
            Prefs.states.lastAlarmCategory.fragmentClass.newInstance() as SubActivity,
            CONTENT
        )
        setActive(Prefs.states.lastAlarmCategory)
        view.alarms.tag = AlarmCategory.ALARM
        view.reminders.tag = AlarmCategory.REMINDER

        view.alarms.setOnClickListener(choice)
        view.reminders.setOnClickListener(choice)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        hasSavedState = true
    }

    override fun removeAllSubFragments() = removeFragment(CONTENT, SubActivity::class.java)
}