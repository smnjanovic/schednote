package com.moriak.schednote.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.*
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.design.Palette
import com.moriak.schednote.dialogs.DateTimeDialog.Display.*
import com.moriak.schednote.other.Day
import com.moriak.schednote.settings.Prefs.settings
import com.moriak.schednote.settings.TimeFormat
import kotlinx.android.synthetic.main.datetime_picker.view.*
import java.util.Calendar.*

/**
 * Dialóg nastavenia dátumu a času úlohy v adapteri.
 */
class DateTimeDialog : DialogFragment() {
    private companion object {
        private const val POS = "POS"
        private const val MILLIS = "MILLIS"
        private const val DISPLAY = "DISPLAY"
    }

    private enum class Display(val value: Int) {
        CALENDAR(0), CLOCK(1), SEMESTER(2);

        companion object {
            operator fun get(int: Int) = when (int) {
                CALENDAR.value -> CALENDAR
                CLOCK.value -> CLOCK
                SEMESTER.value -> SEMESTER
                else -> throw IllegalArgumentException("No such value for enum Display!")
            }

            @ColorInt
            private val highlightColor: Int

            @ColorInt
            private val regularColor: Int

            init {
                val palette = Palette.resource(R.color.colorPrimaryDark)
                highlightColor = palette.luminance((palette.luminance + 15).coerceAtMost(100)).color
                regularColor = palette.luminance(25).saturation(30).color
            }
        }

        fun affectVisibility(view: View?) {
            view ?: return
            view.date_setter.visibility = if (this == CALENDAR) VISIBLE else GONE
            view.time_setter.visibility = if (this == CLOCK) VISIBLE else GONE
            view.semester_setter.visibility = if (this == SEMESTER) VISIBLE else GONE

            val timeBtn = view.timeBtn.compoundDrawables.iterator()
            val dateBtn = view.dateBtn.compoundDrawables.iterator()
            val semBtn = view.semester_btn.compoundDrawables.iterator()
            while (dateBtn.hasNext()) dateBtn.next()
                ?.setTint(if (this == CALENDAR) highlightColor else regularColor)
            while (timeBtn.hasNext()) timeBtn.next()
                ?.setTint(if (this == CLOCK) highlightColor else regularColor)
            while (semBtn.hasNext()) semBtn.next()
                ?.setTint(if (this == SEMESTER) highlightColor else regularColor)
        }
    }

    private var onConfirm = fun(_: Int, _: Long) {}
    private lateinit var root: View

    private var pos = -1
    private var year: Int
    private var month: Int
    private var day: Int
    private var hour: Int
    private var minute: Int
    private var display: Display = CALENDAR
        set(value) {
            field = value
            if (value == SEMESTER)
                if (settings.semesterValid && this::root.isInitialized) computeSemesterDate(
                    root.week,
                    root.day,
                    root.date_setter
                )
                else throw IllegalArgumentException("Invalid semester!")
        }

    init {
        App.now.apply {
            year = get(YEAR)
            month = get(MONTH)
            day = get(DAY_OF_MONTH)
            hour = get(HOUR_OF_DAY)
            minute = get(MINUTE)
        }
    }

    private val calendar
        get() = App.cal.apply {
            timeInMillis = 0
            set(year, month, day, hour, minute)
        }
    private lateinit var weeks: Array<Int>
    private lateinit var days: Array<Day>

    private val semesterChange = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {}
        override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
            computeSemesterDate(root.week, root.day, root.date_setter)
        }
    }
    private val dateChange = CalendarView.OnDateChangeListener { _, y, m, d ->
        if (display == CALENDAR) {
            year = y
            month = m
            day = d
            setSemesterValues()
        }
    }
    private val timeChange = TimePicker.OnTimeChangedListener { _, h, m ->
        hour = h
        minute = m
    }

    private fun setSemesterValues() {
        if (settings.semesterValid) {
            val currDay = calendar.get(DAY_OF_WEEK)
            val days = settings.workWeek.days
            root.week.setSelection(settings.semesterWeek(calendar.timeInMillis))
            root.day.setSelection(days.find { currDay == it.value }?.let { days.indexOf(it) } ?: 0)
        }
    }

    private fun computeSemesterDate(weekSel: Spinner, daySel: Spinner, cal: CalendarView) {
        if (weekSel.adapter.count == 0 || daySel.adapter.count == 0 || display != SEMESTER) return
        App.cal.timeInMillis = settings.semesterStart!!
        App.cal.add(DAY_OF_YEAR, 7 * (weekSel.selectedItem as Int - 1))
        while ((daySel.selectedItem as Day).value != App.cal.get(DAY_OF_WEEK))
            App.cal.add(DAY_OF_YEAR, 1)

        year = App.cal.get(YEAR)
        month = App.cal.get(MONTH)
        day = App.cal.get(DAY_OF_MONTH)
        cal.date = calendar.timeInMillis
    }

    /**
     * Uloženie pozície položky v adapteri a doteraz nastaveného dátumu
     * @param itemPos pozícia úlohy v adapteri
     * @param millis Dátum v milisekundách. Môže byť null.
     */
    fun storeItemPositionAndDate(itemPos: Int, millis: Long? = null) {
        pos = itemPos
        App.cal.apply {
            timeInMillis = millis ?: System.currentTimeMillis()
            year = get(YEAR)
            month = get(MONTH)
            day = get(DAY_OF_MONTH)
            hour = get(HOUR_OF_DAY)
            minute = get(MINUTE)
        }
    }

    /**
     * Určuje, čo sa má stať po potvrdení zmien
     * @param fn algoritmus, ktorý sa má vykonať
     */
    fun setOnConfirm(fn: (Int, Long) -> Unit) {
        onConfirm = fn
    }

    @SuppressLint("InflateParams")
    private fun buildView(): View {
        root = LayoutInflater.from(App.ctx).inflate(R.layout.datetime_picker, null, false)
        days = settings.workWeek.days
        weeks = if (settings.semesterValid) (1..settings.semesterWeekCount).toList()
            .toTypedArray() else arrayOf()
        root.week.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, weeks)
        root.day.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, days)

        root.time_setter.hour = hour
        root.time_setter.minute = minute
        root.date_setter.date = calendar.timeInMillis
        setSemesterValues()

        val mode = View.OnClickListener {
            display = it.tag as Display
            display.affectVisibility(root)
        }

        root.date_setter.setOnDateChangeListener(dateChange)
        root.time_setter.setOnTimeChangedListener(timeChange)
        root.time_setter.setIs24HourView(settings.timeFormat == TimeFormat.H24)
        root.week.onItemSelectedListener = semesterChange
        root.day.onItemSelectedListener = semesterChange

        root.dateBtn.tag = CALENDAR
        root.timeBtn.tag = CLOCK
        root.semester_btn.tag = SEMESTER
        root.semester_btn.visibility = if (settings.semesterValid) VISIBLE else GONE
        root.dateBtn.setOnClickListener(mode)
        root.timeBtn.setOnClickListener(mode)
        root.semester_btn.setOnClickListener(mode)

        root.post { display.affectVisibility(root) }
        return root
    }

    override fun onCreateDialog(saved: Bundle?): Dialog {
        saved?.let {
            storeItemPositionAndDate(it.getInt(POS, -1), it.getLong(MILLIS))
            display = Display[it.getInt(DISPLAY)]
        }
        return AlertDialog.Builder(requireContext())
            .setView(buildView())
            .setPositiveButton(R.string.confirm) { _, _ -> onConfirm(pos, calendar.timeInMillis) }
            .setNegativeButton(R.string.abort) { _, _ -> }
            .create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(MILLIS, calendar.timeInMillis)
        outState.putInt(POS, pos)
        outState.putInt(DISPLAY, display.value)
    }
}