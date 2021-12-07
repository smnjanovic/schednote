package com.moriak.schednote.fragments.of_main

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.NumberPicker
import com.moriak.schednote.R
import com.moriak.schednote.dialogs.DateTimeDialog
import com.moriak.schednote.dialogs.DateTimeDialog.Companion.FLAG_DATE
import com.moriak.schednote.fragments.SubActivity
import com.moriak.schednote.setClean
import com.moriak.schednote.storage.Prefs.Settings.dateFormat
import com.moriak.schednote.storage.Prefs.Settings.semesterStart
import com.moriak.schednote.storage.Prefs.Settings.semesterValid
import com.moriak.schednote.storage.Prefs.Settings.semesterWeek
import com.moriak.schednote.storage.Prefs.Settings.semesterWeekCount
import com.moriak.schednote.storage.Prefs.Settings.workWeek
import kotlinx.android.synthetic.main.semester.view.*
import java.util.*
import java.util.Calendar.DAY_OF_WEEK
import java.util.Calendar.HOUR_OF_DAY
import kotlin.math.abs

/**
 * Vo fragmente možno nastaviť dátum začiatku semestra a jeho trvanie. Ak semester práve beží
 * program bude počítať koľký je semestrálny týžden a úloham bude možné nastaviť úlohy na n-tý týždeň
 * bez znalosti presného dátumu
 */
class SemesterSettings : SubActivity() {
    private companion object {
        private const val START = "START"
        private val cal: Calendar by lazy { Calendar.getInstance() }
    }

    private val onSwitch = CompoundButton.OnCheckedChangeListener { btn, checked ->
        if (checked && !semesterValid) btn.isChecked = false
        else if (!checked && semesterValid) {
            semesterStart = null
            semesterWeekCount = 0
            view?.week_count?.value = 0
            view?.semester_start?.text = null
            view?.info?.text = getSemesterInfo()
        }
    }

    private val onWeekCountChange = NumberPicker.OnValueChangeListener { _, _, newVal ->
        semesterWeekCount = newVal
        val st = semesterStart
        val wc = semesterWeekCount
        requireView().semester_enabled.isChecked = semesterValid
        semesterStart = st
        semesterWeekCount = wc
        requireView().info.text = getSemesterInfo()
    }

    private val onDateClick = View.OnClickListener {
        showDialog(START, DateTimeDialog(semesterStart, FLAG_DATE).setOnConfirm(this::setDate))
    }

    private fun setDate(millis: Long?) {
        semesterStart = millis?.let { _ ->
            cal.timeInMillis = millis
            val dif = workWeek.workDays.first().value - cal.get(DAY_OF_WEEK)
            cal.add(DAY_OF_WEEK, -abs(dif))
            cal.setClean(HOUR_OF_DAY, 0)
            cal.timeInMillis
        }
        view?.semester_enabled?.isChecked = semesterValid

        view?.semester_start?.let { v ->
            semesterStart?.let { ms -> v.text = dateFormat.getFormat(ms) }
                ?: v.setText(R.string.choose_date)
        }
        view?.info?.text = getSemesterInfo()
    }

    private fun getSemesterInfo(): String = semesterStart?.let { _ ->
        when (val week = semesterWeek) {
            in 1..semesterWeekCount -> "${getString(R.string.week)}: $week"
            else -> getString(if (week < 1) R.string.semester_never_started else R.string.semester_over)
        }
    } ?: getString(R.string.choose_date)

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.setTitle(R.string.semester)
    }

    override fun onCreateView(inf: LayoutInflater, par: ViewGroup?, saved: Bundle?): View = inf
        .inflate(R.layout.semester, par, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setDate(semesterStart)
        view.semester_enabled.isChecked = semesterValid
        view.semester_enabled.setOnCheckedChangeListener(onSwitch)
        view.semester_start.setOnClickListener(onDateClick)
        view.week_count.apply {
            minValue = 0
            maxValue = 52
            value = semesterWeekCount
            setOnValueChangedListener(onWeekCountChange)
        }
        view.info.text = getSemesterInfo()
        findFragment<DateTimeDialog>(START)?.setOnConfirm(this::setDate)
    }

}