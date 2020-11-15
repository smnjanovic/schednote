package com.moriak.schednote.dialogs

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.DialogFragment
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.settings.Prefs
import kotlinx.android.synthetic.main.time_setter.view.*

/**
 * Dialóg nastaví čas kedy študentom alebo žiakom začína nultá alebo prvá hodina
 */
class ScheduleStartSetter : DialogFragment() {
    companion object {
        private const val STORED_VALUE = "STORED_VALUE"
    }

    private val max = 24 * 60 - 1 - App.data.scheduleDuration()
    private var storedValue = Prefs.settings.earliestMinute.coerceIn(0..max)

    private lateinit var root: View
    private var onConfirm: (Int) -> Unit = fun(_) = Unit

    fun setOnConfirm(fn: (Int) -> Unit) {
        onConfirm = fn
    }

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity ?: throw (Exception("Activity was destroyed!"))
        savedInstanceState?.let { storedValue = it.getInt(STORED_VALUE).coerceIn(0..max) }
        return AlertDialog.Builder(activity)
            .setView(setUpView())
            .setPositiveButton(R.string.confirm) { _, _ -> onConfirm(storedValue) }
            .setNegativeButton(R.string.abort) { _, _ -> }
            .create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STORED_VALUE, storedValue)
    }

    @SuppressLint("InflateParams")
    private fun setUpView(): View {
        root = LayoutInflater.from(context).inflate(R.layout.time_setter, null, false)

        root.hour_setter.minValue = 0
        root.hour_setter.maxValue = max / 60
        root.minute_setter.minValue = 0
        root.minute_setter.maxValue =
            if (root.hour_setter.value == root.hour_setter.maxValue) max % 60 else 59

        root.hour_setter.value = storedValue / 60
        root.minute_setter.value = storedValue % 60

        root.minute_setter.setFormatter { String.format("%02d", it) }
        root.hour_setter.setOnValueChangedListener { picker, _, newVal ->
            root.minute_setter.maxValue = if (newVal == picker.maxValue) max % 60 else 59
            storedValue = root.hour_setter.value * 60 + root.minute_setter.value
        }
        root.minute_setter.setOnValueChangedListener { _, _, _ ->
            storedValue = root.hour_setter.value * 60 + root.minute_setter.value
        }

        return root
    }
}
