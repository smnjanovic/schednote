package com.moriak.schednote.dialogs

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.DialogFragment
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.database.data.LessonTime
import com.moriak.schednote.settings.Prefs
import kotlinx.android.synthetic.main.lesson_duration_setter.view.*

/**
 * Dialog nastavenia trvania hodiny a prestávky po nej.
 */
class LessonDurationSetter : DialogFragment() {
    companion object {
        private const val ORDER = "ORDER"
        private const val L_DUR = "L_DUR"
        private const val B_DUR = "B_DUR"
    }

    private lateinit var root: View
    private var onConfirm: (Int, Int, Int) -> Unit = fun(_, _, _) = Unit
    private val bundle = Bundle()

    /**
     * Nastavenie vybraných hodnôt
     * @param order poradie hodiny [LessonTime]
     * @param lDur trvanie hodiny [LessonTime]
     * @param bDur trvanie prestávky po nej
     */
    fun setValues(order: Int, lDur: Int, bDur: Int) {
        this.order = order
        lesDur = lDur
        breakDur = bDur
    }

    /**
     * Nastavenie čo sa má stať po potvrdení vybraných hodnôt
     * @param fn metóda, ktorá sa vykoná po potvrdení hodnôt
     */
    fun setOnConfirm(fn: (Int, Int, Int) -> Unit) {
        onConfirm = fn
    }

    /**
     * Nastavenie predvolených hodnôt. Dialóg vkladá ďaľšiu hodinu [LessonTime] do rozvrhu
     */
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

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        activity ?: throw (RuntimeException("Activity was destroyed!"))
        savedInstanceState?.let { bundle.putAll(it) }
        return AlertDialog.Builder(activity)
            .setView(buildView())
            .setPositiveButton(R.string.confirm) { _, _ -> onConfirm(order, lesDur, breakDur) }
            .setNegativeButton(R.string.abort, fun(_: DialogInterface, _: Int) = Unit)
            .create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putAll(bundle)
    }

    @SuppressLint("InflateParams")
    private fun buildView(): View {
        root = LayoutInflater.from(context).inflate(R.layout.lesson_duration_setter, null, false)
        root.label.text =
            if (order == -1) App.str(R.string.new_lesson) else App.str(R.string.lesson) + " $order"

        root.lesson_dur.minValue = 1
        root.lesson_dur.maxValue = 120
        root.lesson_dur.value = lesDur
        root.lesson_dur.setOnValueChangedListener { _, _, newVal -> lesDur = newVal }

        root.break_dur.minValue = 1
        root.break_dur.maxValue = 45
        root.break_dur.value = breakDur
        root.break_dur.setOnValueChangedListener { _, _, newVal -> breakDur = newVal }
        return root
    }
}