package com.moriak.schednote.dialogs

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.DialogFragment
import com.moriak.schednote.storage.Prefs.States.lastSetBreakDuration
import com.moriak.schednote.storage.Prefs.States.lastSetLessonDuration
import com.moriak.schednote.R
import com.moriak.schednote.data.LessonTime
import kotlinx.android.synthetic.main.lesson_duration_setter.view.*

/**
 * Dialógové okna nastavenia trvania hodiny a nasledujúcej prestávky.
 */
class LessonDurationSetter : DialogFragment {
    private companion object { private const val STORAGE = "STORAGE" }

    private var order: Int
    private var lesDur: Int
    private var breakDur: Int

    private var onConfirm: (Int, Int, Int) -> Unit = fun(_, _, _) = Unit
    private lateinit var root: View

    constructor() {
        order = -1
        lesDur = lastSetLessonDuration
        breakDur = lastSetBreakDuration
    }

    constructor(lt: LessonTime?) {
        order = lt?.order ?: -1
        lesDur = lt?.lessonDuration ?: lastSetLessonDuration
        breakDur = lt?.breakDuration ?: lastSetBreakDuration
    }

    private fun buildView(): View {
        @SuppressLint("InflateParams")
        root = LayoutInflater.from(context).inflate(R.layout.lesson_duration_setter, null, false)
        val labelRes = if (order == -1) R.string.new_lesson else R.string.lesson
        @SuppressLint("SetTextI18n")
        root.label.text = "${root.context.getString(labelRes)}${if (order == -1) "" else " $order"}"

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

    /**
     * Nastavenie čo sa má stať po potvrdení vybraných hodnôt
     * @param fn metóda, ktorá sa vykoná po potvrdení hodnôt
     */
    fun setOnConfirm(fn: (Int, Int, Int) -> Unit) { onConfirm = fn }

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        savedInstanceState?.getIntArray(STORAGE)?.let {
            order = it[0]
            lesDur = it[1]
            breakDur = it[2]
        }
        return AlertDialog.Builder(activity)
            .setView(buildView())
            .setPositiveButton(R.string.confirm) { _, _ -> onConfirm(order, lesDur, breakDur) }
            .setNegativeButton(R.string.abort, fun(_: DialogInterface, _: Int) = Unit)
            .create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putIntArray(STORAGE, intArrayOf(order, lesDur, breakDur))
    }
}