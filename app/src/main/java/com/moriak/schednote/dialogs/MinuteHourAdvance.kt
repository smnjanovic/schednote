package com.moriak.schednote.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.NumberPicker
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.moriak.schednote.R
import com.moriak.schednote.settings.Prefs
import kotlinx.android.synthetic.main.minute_advance.view.*

class MinuteHourAdvance : DialogFragment() {
    private companion object {
        private const val MINUTES_ADVANCE = "MINUTES_ADVANCE"
    }

    private var minuteAdvance: Int = Prefs.states.lastScheduleStartAdvance
    private var confirm = fun(_: Int) {}

    fun setOnConfirm(fn: (Int) -> Unit) {
        confirm = fn
    }

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        savedInstanceState?.let { minuteAdvance = it.getInt(MINUTES_ADVANCE) }

        val view: View =
            LayoutInflater.from(requireContext()).inflate(R.layout.minute_advance, null, false)
        view.hour_advance.maxValue = 12
        view.hour_advance.minValue = 0
        view.hour_advance.value = minuteAdvance / 60

        view.minute_advance.maxValue = 59
        view.minute_advance.minValue = 0
        view.minute_advance.value = minuteAdvance % 60

        val change = NumberPicker.OnValueChangeListener { picker, old, new ->
            minuteAdvance = view.hour_advance.value * 60 + view.minute_advance.value
            if (picker == view.minute_advance)
                if (old == 59 && new == 0)
                    if (view.hour_advance.value == view.hour_advance.maxValue) view.hour_advance.value =
                        view.hour_advance.minValue
                    else view.hour_advance.value += 1
                else if (old == 0 && new == 59)
                    if (view.hour_advance.value == view.hour_advance.minValue) view.hour_advance.value =
                        view.hour_advance.maxValue
                    else view.hour_advance.value -= 1
        }

        view.minute_advance.setOnValueChangedListener(change)
        view.minute_advance.setFormatter { String.format("%02d", it) }
        view.hour_advance.setOnValueChangedListener(change)

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .setMessage(R.string.alarm_advance_hint)
            .setPositiveButton(R.string.confirm) { _, _ -> confirm(minuteAdvance) }
            .setNegativeButton(R.string.abort) { _, _ -> }
            .create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(MINUTES_ADVANCE, minuteAdvance)
    }
}