package com.moriak.schednote.dialogs

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.adapters.ScheduleAdapter
import com.moriak.schednote.settings.Prefs
import kotlinx.android.synthetic.main.lesson_duration_setter.view.*

class LessonDurationSetter : DialogFragment() {
    companion object {
        private const val ORDER = "ORDER"
        private const val L_DUR = "L_DUR"
        private const val B_DUR = "B_DUR"
    }

    private var adapter: ScheduleAdapter? = null
    private val bundle = Bundle()

    fun setValues(order: Int, lDur: Int, bDur: Int) {
        this.order = order
        lesDur = lDur
        breakDur = bDur
    }

    fun setDefault() {
        order = -1
        lesDur = Prefs.states.lastSetLessonDuration
        breakDur = Prefs.states.lastSetBreakDuration
    }

    private var order
        get() = bundle.getInt(ORDER, -1)
        set(value) = bundle.putInt(ORDER, value)
    private var lesDur
        get() = bundle.getInt(L_DUR, Prefs.states.lastSetLessonDuration)
        set(value) = bundle.putInt(L_DUR, value)
    private var breakDur
        get() = bundle.getInt(B_DUR, Prefs.states.lastSetBreakDuration)
        set(value) = bundle.putInt(B_DUR, value)

    fun setAdapter(pAdapter: ScheduleAdapter) {
        adapter = pAdapter
    }

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?) = activity?.let {
        savedInstanceState?.let { bundle.putAll(it) }
        AlertDialog.Builder(activity).apply {
            val v =
                LayoutInflater.from(context).inflate(R.layout.lesson_duration_setter, null, false)

            v.label.text =
                if (order == -1) App.str(R.string.new_lesson) else App.str(R.string.lesson) + " $order"

            v.lesson_dur.minValue = 1
            v.lesson_dur.maxValue = 120
            v.lesson_dur.value = lesDur
            v.lesson_dur.setOnValueChangedListener { _, _, newVal -> lesDur = newVal }

            v.break_dur.minValue = 1
            v.break_dur.maxValue = 45
            v.break_dur.value = breakDur
            v.break_dur.setOnValueChangedListener { _, _, newVal -> breakDur = newVal }

            setView(v)
            setPositiveButton(R.string.confirm) { _, _ ->
                adapter?.let {
                    if (order > -1) it.update(order, lesDur, breakDur)
                    else it.insert(lesDur, breakDur)
                    Prefs.states.lastSetLessonDuration = lesDur
                    Prefs.states.lastSetBreakDuration = breakDur
                }
            }
            setNegativeButton(R.string.abort, fun(_: DialogInterface, _: Int) = Unit)
        }.create()
    } ?: throw (RuntimeException("Activity was destroyed!"))

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putAll(bundle)
    }
}