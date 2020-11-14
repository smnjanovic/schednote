package com.moriak.schednote.dialogs

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.adapters.ScheduleAdapter
import com.moriak.schednote.settings.Prefs
import kotlinx.android.synthetic.main.time_setter.view.*

class ScheduleStartSetter : DialogFragment() {
    companion object {
        private const val STORED_VALUE = "STORED_VALUE"
    }

    private var affectedView: TextView? = null
    private var affectedAdapter: ScheduleAdapter? = null
    private var storedValue = Prefs.settings.earliestMinute

    fun setAffectedView(textView: TextView) {
        affectedView = textView
    }

    fun setAffectedAdapter(pAdapter: ScheduleAdapter) {
        affectedAdapter = pAdapter
    }

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = activity?.let { activity ->
        savedInstanceState?.let { storedValue = it.getInt(STORED_VALUE) }

        AlertDialog.Builder(activity).apply {
            val v = LayoutInflater.from(context).inflate(R.layout.time_setter, null, false)
            val max = 24 * 60 - 1 - App.data.scheduleDuration()

            v.hour_setter.minValue = 0
            v.hour_setter.maxValue = max / 60
            v.minute_setter.minValue = 0
            v.minute_setter.maxValue =
                if (v.hour_setter.value == v.hour_setter.maxValue) max % 60 else 59

            v.hour_setter.value = storedValue / 60
            v.minute_setter.value = storedValue % 60

            v.minute_setter.setFormatter { String.format("%02d", it) }
            v.hour_setter.setOnValueChangedListener { picker, _, newVal ->
                v.minute_setter.maxValue = if (newVal == picker.maxValue) max % 60 else 59
                storedValue = v.hour_setter.value * 60 + v.minute_setter.value
            }
            v.minute_setter.setOnValueChangedListener { _, _, _ ->
                storedValue = v.hour_setter.value * 60 + v.minute_setter.value
            }
            setView(v)

            setPositiveButton(R.string.confirm) { _, _ ->
                if (storedValue !in 0..max) throw (IndexOutOfBoundsException(
                    "Attempted to exceed the duration " +
                            "of the day in minutes! Try setting a lower value or remove some lessons!"
                ))
                Prefs.settings.earliestMinute = storedValue
                affectedView?.text =
                    Prefs.settings.getTimeString(v.hour_setter.value, v.minute_setter.value)
                affectedAdapter?.let { it.notifyItemRangeChanged(0, it.itemCount) }
            }
            setNegativeButton(R.string.abort) { _, _ -> Unit }
        }.create()
    } ?: throw (Exception("Activity was destroyed!"))

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STORED_VALUE, storedValue)
    }
}
