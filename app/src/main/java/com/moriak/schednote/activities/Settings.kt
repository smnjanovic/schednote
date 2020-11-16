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

/**
 * V tejto aktivite si žiak alebo študent prispôsobuje nastavenia podľa potrieb
 */
class Settings : ShakeCompatActivity() {
    private companion object {
        private const val DATE_TIME_FORMAT = "DATE_TIME_FORMAT"
    }

    /**
     * Tajná trieda bude využitá pri striedaní možných spôsobov zobrazenia dátumu a času
     * @param order Poradie dňa, mesiaca a roku
     * @param sep Oddeľovač dňa, mesiaca a roku
     * @param timeFormat 12 alebo 24 hodinový časový formát
     */
    private data class Trio(
        val order: DateOrder,
        val sep: DateSeparator,
        val timeFormat: TimeFormat
    ) {
        companion object {
            /**
             * Získam všetky možné kombinácie formátu dátumu: poradie, oddeľovať, časový formát
             */
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

    private val workWeeks = WorkWeek.values()
    private val lessonLabeling = LessonTimeFormat.values()
    private val dateTimeFormat = Trio.getAll()
    private val snooze = Array(6, fun(index) = (index + 1) * 5)

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
        settings.snooze = s
        snooze_minutes.text = ("$s m")
    }

    private val dualWeekSetter = CompoundButton.OnCheckedChangeListener { _, enabled ->
        settings.dualWeekSchedule = enabled
    }

    private val voiceCommandSwitch = CompoundButton.OnCheckedChangeListener { _, ch ->
        settings.shakeEventEnabled = ch
    }

    /**
     * Posunutie sa o [step] indexov v poli obsahujucom [size] prvkov zacinajuc od indexu [from]
     * vo funkcii dôjde k zabráneniu prekročenia hranice indexov
     */
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

    /**
     * Nastavia sa všetky udalosti, ktoré sa majú vykonať pri zmene niektorého z nastavení
     */
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
            startActivity(Intent(this@Settings, AlarmToneActivity::class.java))
        }

        (supportFragmentManager.findFragmentByTag(DATE_TIME_FORMAT) as DateTimeFormatDialog?)
            ?.setOnConfirm(confirmDateTimeFormat)
    }

    /**
     * podľa súčasných nastavení sa zobrazia predvolené hodnoty, ktoré môže uživateľ meniť
     */
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
        snoozeIndex = snooze.find { it == settings.snooze }?.let { snooze.indexOf(it) } ?: 1

        dualWeekEnabled.isChecked = settings.dualWeekSchedule
        voice_cmd_enabled.isChecked = settings.shakeEventEnabled
        lesson_format_value.text = lessonLabeling[lessonLabelingIndex].toString()
        work_week_value.text = workWeeks[workWeeksIndex].toString()
        datetime_format_value.text = dateTimeFormat[dateTimeFormatIndex].toString()
        snooze_minutes.text = snooze[snoozeIndex].toString()
        alarm_tune_name.text = settings.alarmTone.label.replace("^(.*)\\..*$".toRegex(), "$1")
    }
}
