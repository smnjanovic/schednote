package com.moriak.schednote.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import com.moriak.schednote.R
import com.moriak.schednote.dialogs.DateTimeFormatDialog
import com.moriak.schednote.settings.*
import com.moriak.schednote.settings.Prefs.settings
import kotlinx.android.synthetic.main.activity_settings.*

class Settings : ShakeCompatActivity() {
    private companion object {
        private const val DATE_TIME_FORMAT = "DATE_TIME_FORMAT"
    }

    private data class Trio(
        val order: DateOrder,
        val sep: DateSeparator,
        val timeFormat: TimeFormat
    ) {
        companion object {
            fun getAll() = ArrayList<Trio>().apply {
                val formats = TimeFormat.values()
                val separators = DateSeparator.values()
                val orders = DateOrder.values()
                for (timeFormat in formats)
                    for (sep in separators)
                        for (order in orders)
                            add(Trio(order, sep, timeFormat))
            }.toTypedArray()
        }

        override fun equals(other: Any?): Boolean = other is Trio && other.order == order
                && other.sep == sep && other.timeFormat == timeFormat

        override fun hashCode(): Int =
            31 * (31 * order.hashCode() + sep.hashCode()) + timeFormat.hashCode()

        override fun toString(): String = System.currentTimeMillis().let {
            String.format("%s %s", order.getFormat(sep, it), timeFormat.getFormat(it))
        }
    }

    private class Snooze(val snooze: Int) {
        override fun toString(): String = "$snooze m"
    }

    private val workWeeks = WorkWeek.values()
    private val lessonLabeling = LessonTimeFormat.values()
    private val dateTimeFormat = Trio.getAll()
    private val snooze = Array(6, fun(index) = Snooze((index + 1) * 5))

    private var lessonLabelingIndex = 0
    private var workWeeksIndex = 0
    private var dateTimeFormatIndex = 0
    private var snoozeIndex: Int = 0

    private val switchWorkWeek = View.OnClickListener {
        workWeeksIndex = arraySwitch(workWeeksIndex, workWeeks.size, it.tag as Int)
        val result = workWeeks[workWeeksIndex]
        settings.workWeek = result
        work_week_value.text = "$result"
    }

    private val switchLessonLabeling = View.OnClickListener {
        lessonLabelingIndex = arraySwitch(lessonLabelingIndex, lessonLabeling.size, it.tag as Int)
        val result = lessonLabeling[lessonLabelingIndex]
        settings.lessonTimeFormat = result
        lesson_format_value.text = "$result"
    }

    private val switchDateTimeFormat = View.OnClickListener {
        dateTimeFormatIndex = arraySwitch(dateTimeFormatIndex, dateTimeFormat.size, it.tag as Int)
        val current = dateTimeFormat[dateTimeFormatIndex]
        settings.dateOrder = current.order
        settings.dateSeparator = current.sep
        settings.timeFormat = current.timeFormat
        datetime_format_value.text = settings.getDateTimeString(System.currentTimeMillis())
    }

    private val confirmDateTimeFormat =
        fun(order: DateOrder, separator: DateSeparator, timeFormat: TimeFormat) {
            settings.dateOrder = order
            settings.dateSeparator = separator
            settings.timeFormat = timeFormat
            dateTimeFormatIndex = dateTimeFormat.indexOf(Trio(order, separator, timeFormat))
            datetime_format_value.text = dateTimeFormat[dateTimeFormatIndex].toString()
        }

    private val switchSnooze = View.OnClickListener {
        snoozeIndex = arraySwitch(snoozeIndex, snooze.size, it.tag as Int)
        val s = snooze[snoozeIndex]
        settings.snooze = s.snooze
        snooze_minutes.text = "$s"
    }

    private val dualWeekSetter = CompoundButton.OnCheckedChangeListener { _, enabled ->
        settings.dualWeekSchedule = enabled
    }

    private val voiceCommandSwitch = CompoundButton.OnCheckedChangeListener { _, ch ->
        settings.shakeEventEnabled = ch
    }

    private fun arraySwitch(from: Int, size: Int, step: Int): Int {
        var next = from + step
        if (next !in 0 until size) next %= size
        if (next < 0) next += size
        return next
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        previous_lesson_format.tag = -1
        previous_work_week.tag = -1
        previous_datetime_format.tag = -1
        previous_snooze.tag = -1

        next_lesson_format.tag = 1
        next_work_week.tag = 1
        next_datetime_format.tag = 1
        next_snooze.tag = 1

        previous_lesson_format.setOnClickListener(switchLessonLabeling)
        previous_work_week.setOnClickListener(switchWorkWeek)
        previous_datetime_format.setOnClickListener(switchDateTimeFormat)
        previous_snooze.setOnClickListener(switchSnooze)

        next_lesson_format.setOnClickListener(switchLessonLabeling)
        next_work_week.setOnClickListener(switchWorkWeek)
        next_datetime_format.setOnClickListener(switchDateTimeFormat)
        next_snooze.setOnClickListener(switchSnooze)

        dualWeekEnabled.setOnCheckedChangeListener(dualWeekSetter)
        voice_cmd_enabled.setOnCheckedChangeListener(voiceCommandSwitch)
        datetime_format_value.setOnClickListener {
            val dialog = DateTimeFormatDialog()
            dialog.setOnConfirm(confirmDateTimeFormat)
            dialog.show(supportFragmentManager, DATE_TIME_FORMAT)
        }
        alarm_tone.setOnClickListener {
            startActivity(
                Intent(
                    this@Settings,
                    AlarmTuneActivity::class.java
                )
            )
        }

        (supportFragmentManager.findFragmentByTag(DATE_TIME_FORMAT) as DateTimeFormatDialog?)?.setOnConfirm(
            confirmDateTimeFormat
        )
    }

    override fun onResume() {
        super.onResume()

        lessonLabelingIndex = lessonLabeling.indexOf(settings.lessonTimeFormat)
        workWeeksIndex = workWeeks.indexOf(settings.workWeek)
        dateTimeFormatIndex = dateTimeFormat.indexOf(
            Trio(
                settings.dateOrder,
                settings.dateSeparator,
                settings.timeFormat
            )
        )
        snoozeIndex = snooze.find { it.snooze == settings.snooze }?.let { snooze.indexOf(it) } ?: 1

        dualWeekEnabled.isChecked = settings.dualWeekSchedule
        voice_cmd_enabled.isChecked = settings.shakeEventEnabled
        lesson_format_value.text = lessonLabeling[lessonLabelingIndex].toString()
        work_week_value.text = workWeeks[workWeeksIndex].toString()
        datetime_format_value.text = dateTimeFormat[dateTimeFormatIndex].toString()
        snooze_minutes.text = snooze[snoozeIndex].toString()
        alarm_tune_name.text = settings.alarmTone.label
    }
}
