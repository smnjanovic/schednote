package com.moriak.schednote.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.NumberPicker
import com.moriak.schednote.R
import com.moriak.schednote.databinding.MinuteAdvanceBinding
import com.moriak.schednote.storage.Prefs.States.lastScheduleStartAdvance

/**
 * Nastavenie časového predstihu budenia pred začiatkom vyučovania
 */
class AlarmClockAdvance : CustomBoundDialog<MinuteAdvanceBinding>() {
    private companion object { private const val MA = "MINUTES_ADVANCE" }

    private var minuteAdvance: Int = lastScheduleStartAdvance
    private var confirm = fun() {}
    override val message: Int = R.string.alarm_advance_hint
    override val negativeButton = ActionButton(R.string.abort) {}
    override val positiveButton = ActionButton(R.string.confirm) {
        lastScheduleStartAdvance = minuteAdvance
        confirm()
    }

    /**
     * Nastavenie, čo sa má stať, keď potrvrdím zmeny
     * @param fn Metóda, ktorá sa vykoná po potvrdení zmien
     */
    fun setOnConfirm(fn: () -> Unit) { confirm = fn }

    override fun setupBinding(inflater: LayoutInflater) = MinuteAdvanceBinding.inflate(inflater)

    override fun setupContent(saved: Bundle?) {
        saved?.let { minuteAdvance = it.getInt(MA) }

        binding.hourAdvance.maxValue = 12
        binding.hourAdvance.minValue = 0
        binding.hourAdvance.value = minuteAdvance / 60

        binding.minuteAdvance.maxValue = 59
        binding.minuteAdvance.minValue = 0
        binding.minuteAdvance.value = minuteAdvance % 60

        val change = NumberPicker.OnValueChangeListener { picker, old, new ->
            minuteAdvance = binding.hourAdvance.value * 60 + binding.minuteAdvance.value
            if (picker == binding.minuteAdvance)
                if (old == 59 && new == 0)
                    if (binding.hourAdvance.value == binding.hourAdvance.maxValue)
                        binding.hourAdvance.value = binding.hourAdvance.minValue
                    else binding.hourAdvance.value += 1
                else if (old == 0 && new == 59)
                    if (binding.hourAdvance.value == binding.hourAdvance.minValue)
                        binding.hourAdvance.value = binding.hourAdvance.maxValue
                    else binding.hourAdvance.value -= 1
        }

        binding.minuteAdvance.setOnValueChangedListener(change)
        binding.minuteAdvance.setFormatter { String.format("%02d", it) }
        binding.hourAdvance.setOnValueChangedListener(change)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(MA, minuteAdvance)
    }
}