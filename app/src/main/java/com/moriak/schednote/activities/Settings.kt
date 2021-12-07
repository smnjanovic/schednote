package com.moriak.schednote.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import com.moriak.schednote.R
import com.moriak.schednote.dialogs.DateTimeFormatDialog
import com.moriak.schednote.enums.DateFormat
import com.moriak.schednote.enums.LessonTimeFormat
import com.moriak.schednote.enums.TimeFormat
import com.moriak.schednote.enums.WorkWeek
import com.moriak.schednote.notifications.AlarmClockSetter
import com.moriak.schednote.storage.Prefs.Settings
import com.moriak.schednote.views.OptionStepper
import com.moriak.schednote.widgets.ScheduleWidget
import kotlinx.android.synthetic.main.activity_settings.*
import java.lang.System.currentTimeMillis as now

/**
 * V tejto aktivite si užívateľ prispôsobuje nastavenia aplikácie podľa potrieb
 */
class Settings : ShakeCompatActivity() {
    companion object {
        private const val DATE_TIME_FORMAT = "DATE_TIME_FORMAT"
        var changed: Boolean = false
    }
    object ChangeReactor : OptionStepper.OnChange, CompoundButton.OnCheckedChangeListener {
        override fun onChange(v: View?, item: Any?) {
            when (v?.id) {
                R.id.workweek_choice -> {
                    Settings.workWeek = item as WorkWeek
                    AlarmClockSetter.setAlarms(v.context)
                    ScheduleWidget.update(v.context)
                }
                R.id.les_label_choice -> {
                    Settings.lessonTimeFormat = item as LessonTimeFormat
                    ScheduleWidget.update(v.context)
                }
                R.id.snooze_choice -> {
                    Settings.snoozeTime = item as Int
                }
                R.id.dtf_choice -> {
                    Settings.dateFormat = (item as DateTimeFormat).d
                    Settings.timeFormat = item.t
                    if (Settings.lessonTimeFormat == LessonTimeFormat.START_TIME)
                        ScheduleWidget.update(v.context)
                }
            }
            changed = true
        }

        override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
            when (buttonView?.id) {
                R.id.dualWeekEnabled -> {
                    AlarmClockSetter.switchDualWeek(buttonView.context, isChecked)
                    ScheduleWidget.update(buttonView.context)
                }
                R.id.voice_cmd_enabled -> {
                    Settings.shakeEventEnabled = isChecked
                }
            }
            changed = true
        }
    }

    private data class DateTimeFormat(val d: DateFormat, val t: TimeFormat)
    private class OptionDescriptor(private val context: Context): OptionStepper.Format {
        override fun getItemDescription(item: Any?): String = when(item) {
            is WorkWeek -> item.getDescription(context)
            is LessonTimeFormat -> context.getString(item.res)
            is DateTimeFormat -> now().let { "${item.d.getFormat(it)} ${item.t.getFormat(it)}" }
            is Int -> "$item min"
            else -> ""
        }

    }

    private val optionFormat = OptionDescriptor(this)
    private val workWeeks = WorkWeek.values()
    private val lessonLabeling = LessonTimeFormat.values()
    private val dateTimeFormat: List<DateTimeFormat> = ArrayList()
    private val snooze = arrayOf(5, 10, 15, 20, 30, 45)

    init {
        for (dateFormat in DateFormat.values())
            for (timeFormat in TimeFormat.values())
                (dateTimeFormat as ArrayList).add(DateTimeFormat(dateFormat, timeFormat))
    }

    private fun confirmDateTimeFormat (dateFormat: DateFormat, timeFormat: TimeFormat) {
        Settings.dateFormat = dateFormat
        Settings.timeFormat = timeFormat
        if (Settings.lessonTimeFormat == LessonTimeFormat.START_TIME) ScheduleWidget.update(this)
        dtf_choice.index = dateTimeFormat.indexOfFirst {
            it.d == dateFormat && it.t == timeFormat
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // nastavit data
        workweek_choice.setOptions(workWeeks)
        les_label_choice.setOptions(lessonLabeling)
        snooze_choice.setOptions(snooze)
        dtf_choice.setOptions(dateTimeFormat)

        // nastavit format
        workweek_choice.setFormat(optionFormat)
        les_label_choice.setFormat(optionFormat)
        snooze_choice.setFormat(optionFormat)
        dtf_choice.setFormat(optionFormat)

        // vyplnit konfiguraciu
        dualWeekEnabled.isChecked = Settings.dualWeekSchedule
        voice_cmd_enabled.isChecked = Settings.shakeEventEnabled
        alarm_tune_name.text = Settings.alarmTone.label.replace("^(.*)\\..*$".toRegex(), "$1")
        workweek_choice.index = workWeeks.indexOf(Settings.workWeek)
        les_label_choice.index = lessonLabeling.indexOf(Settings.lessonTimeFormat)
        snooze_choice.index = snooze.indexOf(Settings.snoozeTime).coerceAtLeast(0)
        dtf_choice.index = dateTimeFormat.indexOfFirst {
            it.d == Settings.dateFormat && it.t == Settings.timeFormat
        }

        // reakcie na zmeny
        dualWeekEnabled.setOnCheckedChangeListener(ChangeReactor)
        voice_cmd_enabled.setOnCheckedChangeListener(ChangeReactor)
        workweek_choice.setOnChange(ChangeReactor)
        les_label_choice.setOnChange(ChangeReactor)
        snooze_choice.setOnChange(ChangeReactor)
        dtf_choice.setOnChange(ChangeReactor)

        // dialog nastavenia formatu datumu a casu
        dtf_choice.setOnClickListener {
            val dialog = DateTimeFormatDialog()
            dialog.setOnConfirm(this::confirmDateTimeFormat)
            dialog.show(supportFragmentManager, DATE_TIME_FORMAT)
        }

        // odkaz na aktivitu kde sa nastavuje ton zvonenia
        alarm_tone.setOnClickListener {
            startActivity(Intent(this@Settings, AlarmToneActivity::class.java))
        }

        // obnovenie funkcie, ktora sa ma vykonat po potvrdeni zmien v nastaveni formatu datumu a casu
        (supportFragmentManager.findFragmentByTag(DATE_TIME_FORMAT) as DateTimeFormatDialog?)
            ?.setOnConfirm(this::confirmDateTimeFormat)
    }
}