package com.moriak.schednote.fragments.of_alarm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.NumberPicker
import androidx.appcompat.app.AlertDialog
import com.moriak.schednote.R
import com.moriak.schednote.fragments.SubActivity
import com.moriak.schednote.notifications.ReminderSetter
import com.moriak.schednote.storage.Prefs.Notifications.reminderAdvanceInMinutes
import com.moriak.schednote.storage.Prefs.Notifications.reminderEnabled
import kotlinx.android.synthetic.main.reminder_setter.*
import kotlinx.android.synthetic.main.reminder_setter.view.*

/**
 * V tomto fragmente uživateľ vypne / zapne oznámenia úloh a môže určiť
 * v akom časovom predstihu (v minútach) sa budú zjavovať.
 */
class ReminderSettings : SubActivity() {
    private companion object {
        private const val TO_CONFIRM = "TO_CONFIRM"
        private const val MINUTE_ADVANCE = "MINUTE_ADVANCE"
    }

    private val enableDisable = CompoundButton.OnCheckedChangeListener { v, isChecked ->
        if (isResumed) {
            reminderEnabled = isChecked
            ReminderSetter.enableReminders(v.context, isChecked)
        }
    }

    private val changeAdvance = NumberPicker.OnValueChangeListener { _, _, _ ->
        view?.confirm_change?.visibility = View.VISIBLE
    }

    private val confirm = View.OnClickListener { v ->
        val adv = (day_advance.value * 24 + hour_advance.value) * 60 + minute_advance.value
        if (reminderAdvanceInMinutes != adv) {
            reminderAdvanceInMinutes = adv
            if (!ReminderSetter.enableReminders(v.context, true)) AlertDialog.Builder(v.context)
                .setMessage(R.string.cant_set_exact_alarm)
                .setPositiveButton(R.string.permission_grant) { _, _ ->
                    if (android.os.Build.VERSION.SDK_INT >= 31) {
                        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                    }
                }
                .setNegativeButton(R.string.abort) { _, _ -> }
                .show()
        }
        v.visibility = View.GONE
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.setTitle(R.string.reminders)
    }

    override fun onCreateView(inf: LayoutInflater, par: ViewGroup?, saved: Bundle?): View = inf
        .inflate(R.layout.reminder_setter, par, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        day_advance.maxValue = 7
        hour_advance.maxValue = 23
        minute_advance.maxValue = 59
        var vAdvance = savedInstanceState?.getInt(MINUTE_ADVANCE)
            ?: reminderAdvanceInMinutes
        minute_advance.value = vAdvance % 60; vAdvance /= 60
        hour_advance.value = vAdvance % 24; vAdvance /= 24
        day_advance.value = vAdvance.coerceIn(0..7)
        val format = NumberPicker.Formatter { String.format("%02d", it) }
        hour_advance.setFormatter(format)
        minute_advance.setFormatter(format)

        confirm_change.visibility = savedInstanceState?.getInt(TO_CONFIRM, View.GONE) ?: View.GONE
        advance_enabled.isChecked = reminderEnabled

        advance_enabled.setOnCheckedChangeListener(enableDisable)
        day_advance.setOnValueChangedListener(changeAdvance)
        hour_advance.setOnValueChangedListener(changeAdvance)
        minute_advance.setOnValueChangedListener(changeAdvance)
        confirm_change.setOnClickListener(confirm)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val adv = (day_advance.value * 24 + hour_advance.value) * 60 + minute_advance.value
        outState.putInt(MINUTE_ADVANCE, adv)
        outState.putInt(TO_CONFIRM, requireView().confirm_change.visibility)
    }
}