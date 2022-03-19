package com.moriak.schednote.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import com.moriak.schednote.R
import com.moriak.schednote.data.LessonTime
import com.moriak.schednote.databinding.LessonDurationSetterBinding
import com.moriak.schednote.storage.Prefs

/**
 * Dialógové okna nastavenia trvania hodiny a nasledujúcej prestávky.
 */
class LessonDurationSetter : CustomBoundDialog<LessonDurationSetterBinding> {
    private companion object { private const val STORAGE = "STORAGE" }

    override val positiveButton = ActionButton(R.string.confirm) { onConfirm(order, lesDur, breakDur) }
    override val negativeButton = ActionButton(R.string.abort) {}
    private var order: Int
    private var lesDur: Int
    private var breakDur: Int
    private var onConfirm: (Int, Int, Int) -> Unit = fun(_, _, _) = Unit

    constructor() {
        order = -1
        lesDur = Prefs.States.lastSetLessonDuration
        breakDur = Prefs.States.lastSetBreakDuration
    }

    constructor(lt: LessonTime?) {
        order = lt?.order ?: -1
        lesDur = lt?.lessonDuration ?: Prefs.States.lastSetLessonDuration
        breakDur = lt?.breakDuration ?: Prefs.States.lastSetBreakDuration
    }

    /**
     * Nastavenie čo sa má stať po potvrdení vybraných hodnôt
     * @param fn metóda, ktorá sa vykoná po potvrdení hodnôt
     */
    fun setOnConfirm(fn: (Int, Int, Int) -> Unit) { onConfirm = fn }

    override fun setupBinding(inflater: LayoutInflater) = LessonDurationSetterBinding.inflate(inflater)

    override fun setupContent(saved: Bundle?) {
        saved?.getIntArray(STORAGE)?.let {
            order = it[0]
            lesDur = it[1]
            breakDur = it[2]
        }

        binding.label.text = when (order) {
            -1 -> binding.root.context.getString(R.string.new_lesson)
            else -> "${binding.root.context.getString(R.string.lesson)} $order"
        }
        binding.lessonDur.minValue = 1
        binding.lessonDur.maxValue = 120
        binding.lessonDur.value = lesDur
        binding.lessonDur.setOnValueChangedListener { _, _, newVal -> lesDur = newVal }

        binding.breakDur.minValue = 1
        binding.breakDur.maxValue = 45
        binding.breakDur.value = breakDur
        binding.breakDur.setOnValueChangedListener { _, _, newVal -> breakDur = newVal }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putIntArray(STORAGE, intArrayOf(order, lesDur, breakDur))
    }
}