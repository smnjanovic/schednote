package com.moriak.schednote.fragments.of_main

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.NumberPicker
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.dialogs.DateDialog
import com.moriak.schednote.settings.Prefs
import kotlinx.android.synthetic.main.semester.view.*
import java.util.Calendar.*

/**
 * Vo fragmente možno nastaviť dátum začiatku semestra a jeho trvanie. Ak semester práve beží
 * program bude počítať koľký je semestrálny týžden a úloham bude možné nastaviť úlohy na n-tý týždeň
 * bez znalosti presného dátumu
 */
class SemesterFragment : SubActivity() {
    private companion object {
        private const val START = "START"
    }

    private val onSwitch = CompoundButton.OnCheckedChangeListener { switch, enabled ->
        if (enabled && !Prefs.settings.semesterValid) switch.isChecked = false
        else if (!enabled && Prefs.settings.semesterValid) {
            Prefs.settings.semesterStart = null
            Prefs.settings.semesterWeekCount = 0
            view?.week_count?.value = 0
            view?.semester_start?.text = null
            view?.semester_start?.tag = null
            view?.info?.text = getInfo()
        }
    }

    private val onWeekCountChange = NumberPicker.OnValueChangeListener { _, _, newVal ->
        Prefs.settings.semesterWeekCount = newVal
        val valid = Prefs.settings.semesterValid
        val checked = view!!.semester_enabled.isChecked
        if (valid.xor(checked)) view!!.semester_enabled.isChecked = valid
        view!!.info.text = getInfo()
    }

    private val onDateClick = View.OnClickListener {
        DateDialog.newInstance(it.tag as Long?, setDate).show(fragmentManager!!, START)
    }

    private val setDate = fun(millis: Long?) {
        millis?.let {
            App.cal.timeInMillis = millis
            val days = Prefs.settings.workWeek.workDay
            val step = days.find { it.value == App.cal.get(DAY_OF_WEEK) }?.let { -1 } ?: 1
            while (days.first().value != App.cal.get(DAY_OF_WEEK)) App.cal.add(DAY_OF_YEAR, step)
            App.cal.set(HOUR_OF_DAY, 0)
            App.cal.set(MINUTE, 0)
            App.cal.set(SECOND, 0)
            App.cal.set(MILLISECOND, 0)
        }
        val ms = millis?.let { App.cal.timeInMillis }
        Prefs.settings.semesterStart = ms
        view?.semester_enabled?.isChecked = Prefs.settings.semesterValid
        view?.semester_start?.tag = ms
        view?.semester_start?.text = ms?.let { _ ->
            Prefs.settings.dateOrder.getFormat(Prefs.settings.dateSeparator, ms)
        } ?: App.str(R.string.choose_date)
        view?.info?.text = getInfo()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.setTitle(R.string.semester)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.semester, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setDate(Prefs.settings.semesterStart)
        view.semester_enabled.isChecked = Prefs.settings.semesterValid
        view.semester_enabled.setOnCheckedChangeListener(onSwitch)
        view.semester_start.setOnClickListener(onDateClick)
        view.week_count.apply {
            minValue = 0
            maxValue = 52
            value = Prefs.settings.semesterWeekCount
            setOnValueChangedListener(onWeekCountChange)
        }
        view.info.text = getInfo()
        findFragment(START, DateDialog::class.java)?.setOnConfirm(setDate)
    }

    private fun getInfo(): String {
        val start = Prefs.settings.semesterStart ?: return App.str(R.string.semester_never_started)
        val weekCount = Prefs.settings.semesterWeekCount
        val end = App.cal.let {
            it.timeInMillis = start
            App.cal.add(DAY_OF_MONTH, weekCount * 7)
            App.cal.timeInMillis
        }
        val now = System.currentTimeMillis()
        if (now < start) return App.str(R.string.semester_never_started)
        if (now >= end) return App.str(R.string.semester_over)
        return App.str(R.string.week) + ": ${(now - start) / (7 * 24 * 60 * 60 * 1000) + 1}"
    }
}