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
import com.moriak.schednote.databinding.ReminderSetterBinding
import com.moriak.schednote.fragments.SubActivity
import com.moriak.schednote.notifications.ReminderSetter
import com.moriak.schednote.storage.Prefs.Notifications.reminderAdvanceInMinutes
import com.moriak.schednote.storage.Prefs.Notifications.reminderEnabled

/**
 * V tomto fragmente uživateľ vypne / zapne oznámenia úloh a môže určiť
 * v akom časovom predstihu (v minútach) sa budú zjavovať.
 */
class ReminderSettings : SubActivity<ReminderSetterBinding>() {
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
        binding.confirmChange.visibility = View.VISIBLE
    }

    private val confirm = View.OnClickListener { v ->
        val adv = (binding.dayAdvance.value * 24 + binding.hourAdvance.value) *
                60 + binding.minuteAdvance.value
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

    override fun makeBinder(inflater: LayoutInflater, container: ViewGroup?) =
        ReminderSetterBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.dayAdvance.maxValue = 7
        binding.hourAdvance.maxValue = 23
        binding.minuteAdvance.maxValue = 59
        var vAdvance = savedInstanceState?.getInt(MINUTE_ADVANCE)
            ?: reminderAdvanceInMinutes
        binding.minuteAdvance.value = vAdvance % 60; vAdvance /= 60
        binding.hourAdvance.value = vAdvance % 24; vAdvance /= 24
        binding.dayAdvance.value = vAdvance.coerceIn(0..7)
        val format = NumberPicker.Formatter { String.format("%02d", it) }
        binding.hourAdvance.setFormatter(format)
        binding.minuteAdvance.setFormatter(format)

        binding.confirmChange.visibility = savedInstanceState?.getInt(TO_CONFIRM, View.GONE) ?: View.GONE
        binding.advanceEnabled.isChecked = reminderEnabled

        binding.advanceEnabled.setOnCheckedChangeListener(enableDisable)
        binding.dayAdvance.setOnValueChangedListener(changeAdvance)
        binding.hourAdvance.setOnValueChangedListener(changeAdvance)
        binding.minuteAdvance.setOnValueChangedListener(changeAdvance)
        binding.confirmChange.setOnClickListener(confirm)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val adv = (binding.dayAdvance.value * 24 + binding.hourAdvance.value) * 60 + binding.minuteAdvance.value
        outState.putInt(MINUTE_ADVANCE, adv)
        outState.putInt(TO_CONFIRM, binding.confirmChange.visibility)
    }
}