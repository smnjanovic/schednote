package com.moriak.schednote.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.CalendarView.OnDateChangeListener
import android.widget.TimePicker.OnTimeChangedListener
import com.moriak.schednote.R
import com.moriak.schednote.databinding.DatetimePickerBinding
import com.moriak.schednote.enums.Day
import com.moriak.schednote.enums.TimeFormat.H24
import com.moriak.schednote.storage.Prefs.Settings.semesterStart
import com.moriak.schednote.storage.Prefs.Settings.semesterValid
import com.moriak.schednote.storage.Prefs.Settings.semesterWeekCount
import com.moriak.schednote.storage.Prefs.Settings.timeFormat
import com.moriak.schednote.storage.Prefs.Settings.workWeek
import java.util.Calendar.*

/**
 * Dialóg nastavenia dátumu a času. Počas semestrálneho obdobia možno dátum
 * nastaviť aj relatívne zadaním semestrálneho týždňa a pracovného dňa.
 */
class DateTimeDialog(
    millis: Long? = null,
    private var flags: Int = FLAG_DATE or FLAG_SEMESTER or FLAG_TIME
) : CustomBoundDialog<DatetimePickerBinding>() {
    companion object {
        private const val MS = "STORAGE"
        const val FLAG_TIME = 1
        const val FLAG_SEMESTER = 2
        const val FLAG_DATE = 4
    }

    private data class Item<T>(val data: T, val description: String = data.toString()) {
        override fun toString() = description
    }

    override val positiveButton = ActionButton(R.string.confirm) { onConfirm(cal.timeInMillis) }
    override val neutralButton = ActionButton(R.string.delete) { onConfirm(null) }
    override val negativeButton = ActionButton(R.string.abort) {}
    private val change = object: OnDateChangeListener, OnTimeChangedListener, OnItemSelectedListener {
        override fun onSelectedDayChange(view: CalendarView, y: Int, m: Int, d: Int) {
            if (mode and FLAG_DATE > 0) {
                year = y
                month = m
                day = d
                if (flags and FLAG_SEMESTER > 0 && semesterValid) {
                    binding.week.setSelection(week)
                    var index = workWeek.workDays.indexOf(Day[dow])
                    if (index == -1) index = workWeek.workDays.lastIndex
                    binding.day.setSelection(index)
                }
            }
        }

        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            if (mode and FLAG_SEMESTER > 0) {
                when (val newSel = (parent?.selectedItem as Item<*>?)?.data) {
                    is Day -> day += newSel.value - dow
                    is Int -> day += 7 * (newSel - week)
                }
                binding.dateSetter.date = cal.timeInMillis
            }
        }

        override fun onTimeChanged(view: TimePicker?, h: Int, m: Int) {
            if (mode and FLAG_TIME > 0) {
                hour = h
                minute = m
            }
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {}
    }
    private var mode: Int = Integer.highestOneBit((flags and 7).coerceAtLeast(1))
        set(value) {
            field = value
            if (value and FLAG_SEMESTER > 0 && isBound) {
                val wasWeekend = Day[dow] in workWeek.weekend
                fun Spinner.fn() = change.onItemSelected(this, selectedView,
                    selectedItemPosition, selectedItemId)
                binding.day.fn()
                binding.week.fn()
                if (wasWeekend) day -= 7
            }
        }
    private val cal = getInstance()
    private var year: Int get() = cal.get(YEAR); set(value) = cal.set(YEAR, value)
    private var month: Int get() = cal.get(MONTH); set(value) = cal.set(MONTH, value)
    private var day: Int get() = cal.get(DAY_OF_MONTH); set(value) = cal.set(DAY_OF_MONTH, value)
    private val dow: Int get() = cal.get(DAY_OF_WEEK)
    private val week: Int get() = semesterStart?.let{
        val dif = (cal.timeInMillis - it) / 1000 / 60 / 60 / 24 / 7
        if (dif in 0 until semesterWeekCount) dif.toInt()
        else null
    } ?: 0
    private var hour: Int get() = cal.get(HOUR_OF_DAY); set(value) = cal.set(HOUR_OF_DAY, value)
    private var minute: Int get() = cal.get(MINUTE); set(value) = cal.set(MINUTE, value)
    private var onConfirm = fun(_: Long?) {}

    init { cal.timeInMillis = millis ?: System.currentTimeMillis() }

    private fun affectVisibility(pTab: Int) {
        if (mode != pTab) mode = pTab
        binding.dateSetter.visibility = if (pTab and FLAG_DATE > 0) VISIBLE else GONE
        binding.timeSetter.visibility = if (pTab and FLAG_TIME > 0) VISIBLE else GONE
        binding.semesterSetter.visibility = if (pTab and FLAG_SEMESTER > 0) VISIBLE else GONE
        binding.dateBtn.alpha = if (pTab and FLAG_DATE > 0) 1.0F else 0.55F
        binding.timeBtn.alpha = if (pTab and FLAG_TIME > 0) 1.0F else 0.55F
        binding.semesterBtn.alpha = if (pTab and FLAG_SEMESTER > 0) 1.0F else 0.55F
    }

    /**
     * Určuje, čo sa má vykonať so zvoleným dátumom a časom
     * @param fn funkcia s 1 parametrom s vyslednym datumom
     * @return ten istý fragment, v ktorom bola táto metóda volaná
     */
    fun setOnConfirm(fn: (Long?) -> Unit) = also { onConfirm = fn }

    override fun setupBinding(inflater: LayoutInflater) = DatetimePickerBinding.inflate(inflater)

    override fun setupContent(saved: Bundle?) {
        saved?.getLongArray(MS)?.let {
            cal.timeInMillis = it[0]
            flags = it[1].toInt()
            mode = it[2].toInt()
        }

        val days = workWeek.workDays.map { Item(it, getString(it.res)) }
        val weeks = (0 until if (semesterValid) semesterWeekCount else 0).map {
            Item(it, "${it + 1}")
        }
        binding.week.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, weeks)
        binding.day.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, days)

        binding.timeSetter.hour = hour
        binding.timeSetter.minute = minute
        binding.dateSetter.date = cal.timeInMillis

        if (semesterValid) {
            binding.week.setSelection(week)
            var index = workWeek.workDays.indexOf(Day[dow])
            if (index == -1) index = workWeek.workDays.lastIndex
            binding.day.setSelection(index)
        }

        val chooseMode = View.OnClickListener { v ->
            if (v.tag != mode) affectVisibility(v.tag as Int)
            else if (v.tag == FLAG_DATE) binding.dateSetter.date = System.currentTimeMillis()
            else if (v.tag == FLAG_TIME) {
                val oldMs = cal.timeInMillis
                cal.timeInMillis = System.currentTimeMillis()
                val h = hour
                val m = minute
                cal.timeInMillis = oldMs
                binding.timeSetter.hour = h
                binding.timeSetter.minute = m
            }
            else if (v.tag == FLAG_SEMESTER && semesterValid) {
                cal.timeInMillis = System.currentTimeMillis()
                val isWeekend = Day[dow] in workWeek.weekend
                binding.week.setSelection(if (isWeekend) week - 1 else week)
                binding.day.setSelection(if (isWeekend) workWeek.workDays.lastIndex
                else workWeek.workDays.indexOf(Day[dow]))
            }
        }

        binding.dateSetter.setOnDateChangeListener(change)
        binding.timeSetter.setOnTimeChangedListener(change)
        binding.timeSetter.setIs24HourView(timeFormat == H24)
        binding.week.onItemSelectedListener = change
        binding.day.onItemSelectedListener = change

        binding.dateBtn.tag = FLAG_DATE
        binding.timeBtn.tag = FLAG_TIME
        binding.semesterBtn.tag = FLAG_SEMESTER

        binding.dateBtn.visibility = if (flags and FLAG_DATE > 0) VISIBLE else GONE
        binding.semesterBtn.visibility = if (flags and FLAG_SEMESTER > 0 && semesterValid) VISIBLE else GONE
        binding.timeBtn.visibility = if (flags and FLAG_TIME > 0) VISIBLE else GONE

        binding.dateBtn.setOnClickListener(chooseMode)
        binding.timeBtn.setOnClickListener(chooseMode)
        binding.semesterBtn.setOnClickListener(chooseMode)

        binding.root.post { affectVisibility(mode) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLongArray(MS, longArrayOf(cal.timeInMillis, flags.toLong(), mode.toLong()))
    }
}