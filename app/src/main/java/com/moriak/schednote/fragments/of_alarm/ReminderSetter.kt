package com.moriak.schednote.fragments.of_alarm

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.NumberPicker
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.database.data.Note
import com.moriak.schednote.fragments.of_main.SubActivity
import com.moriak.schednote.notifications.NoteReminder
import com.moriak.schednote.other.TimeCategory
import com.moriak.schednote.settings.Prefs
import kotlinx.android.synthetic.main.reminder_setter.view.*

/**
 * V tomto fragmente uživateľ nastavuje predstih upozornenia na úlohy, môže upozornenia vypnúť aj zapnúť
 */
class ReminderSetter : SubActivity(), AlarmDisplay {
    private companion object {
        private const val TO_CONFIRM = "TO_CONFIRM"
        private const val MINUTE_ADVANCE = "MINUTE_ADVANCE"
    }

    private var NumberPicker.range
        get() = minValue..maxValue
        set(range) {
            minValue = range.first
            maxValue = range.last
        }

    private var dig2 = NumberPicker.Formatter { String.format("%02d", it) }
    private lateinit var notes: ArrayList<Note>

    private val enableDisable = CompoundButton.OnCheckedChangeListener { _, isChecked ->
        App.data.enableNoteNotifications(isChecked)
    }

    private val changeAdvance = NumberPicker.OnValueChangeListener { _, _, _ ->
        view?.confirm_change?.visibility = View.VISIBLE
    }

    private val confirm = View.OnClickListener {
        val adv = advance
        if (Prefs.notifications.reminderAdvanceInMinutes != adv) {
            Prefs.notifications.reminderAdvanceInMinutes = adv
            if (Prefs.notifications.reminderEnabled) {
                val switch = view!!.advance_enabled
                switch.isEnabled = false
                App.data.enableNoteNotifications()
                switch.isEnabled = true
            }
        }
        it.visibility = View.GONE
    }

    private val advance
        get() = view?.let { v -> (v.day_advance.value * 24 + v.hour_advance.value) * 60 + v.minute_advance.value }
            ?: 0

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.setTitle(R.string.reminders)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        inflater.inflate(R.layout.reminder_setter, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.apply {
            notes = App.data.notes(TimeCategory.ALL)
            day_advance.range = 0..7
            hour_advance.range = 0..23
            minute_advance.range = 0..59
            var advance = savedInstanceState?.getInt(MINUTE_ADVANCE)
                ?: Prefs.notifications.reminderAdvanceInMinutes
            minute_advance.value = advance % 60; advance /= 60
            hour_advance.value = advance % 24; advance /= 24
            day_advance.value = advance.coerceIn(0..7)
            hour_advance.setFormatter(dig2)
            minute_advance.setFormatter(dig2)

            confirm_change.visibility =
                savedInstanceState?.getInt(TO_CONFIRM, View.GONE) ?: View.GONE
            advance_enabled.isChecked = Prefs.notifications.reminderEnabled

            advance_enabled.setOnCheckedChangeListener(enableDisable)
            day_advance.setOnValueChangedListener(changeAdvance)
            hour_advance.setOnValueChangedListener(changeAdvance)
            minute_advance.setOnValueChangedListener(changeAdvance)
            confirm_change.setOnClickListener(confirm)
            NoteReminder.createNoteReminderChannel()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(MINUTE_ADVANCE, advance)
        outState.putInt(TO_CONFIRM, view!!.confirm_change.visibility)
    }
}