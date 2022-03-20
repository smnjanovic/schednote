package com.moriak.schednote.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import com.moriak.schednote.R
import com.moriak.schednote.databinding.ActivitySettingsBinding
import com.moriak.schednote.dialogs.DateTimeFormatDialog
import com.moriak.schednote.enums.DateFormat
import com.moriak.schednote.enums.LessonTimeFormat
import com.moriak.schednote.enums.TimeFormat
import com.moriak.schednote.enums.WorkWeek
import com.moriak.schednote.notifications.AlarmClockSetter
import com.moriak.schednote.storage.Prefs.Settings
import com.moriak.schednote.views.OptionStepper
import com.moriak.schednote.widgets.ScheduleWidget
import java.lang.System.currentTimeMillis as now

/**
 * V tejto aktivite si užívateľ prispôsobuje nastavenia aplikácie podľa potrieb
 */
class Settings : ShakeCompatActivity<ActivitySettingsBinding>() {
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
        binding.dtfChoice.index = dateTimeFormat.indexOfFirst {
            it.d == dateFormat && it.t == timeFormat
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreateBinding() = ActivitySettingsBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // nastavit data
        binding.workweekChoice.setOptions(workWeeks)
        binding.lesLabelChoice.setOptions(lessonLabeling)
        binding.snoozeChoice.setOptions(snooze)
        binding.dtfChoice.setOptions(dateTimeFormat)

        // nastavit format
        binding.workweekChoice.setFormat(optionFormat)
        binding.lesLabelChoice.setFormat(optionFormat)
        binding.snoozeChoice.setFormat(optionFormat)
        binding.dtfChoice.setFormat(optionFormat)

        binding.alarmSnooze

        // vyplnit konfiguraciu
        binding.dualWeekEnabled.isChecked = Settings.dualWeekSchedule
        binding.voiceCmdEnabled.isChecked = Settings.shakeEventEnabled
        binding.alarmTuneName.text = Settings.alarmTone.label.replace("^(.*)\\..*$".toRegex(), "$1")
        binding.workweekChoice.index = workWeeks.indexOf(Settings.workWeek)
        binding.lesLabelChoice.index = lessonLabeling.indexOf(Settings.lessonTimeFormat)
        binding.snoozeChoice.index = snooze.indexOf(Settings.snoozeTime).coerceAtLeast(0)
        binding.dtfChoice.index = dateTimeFormat.indexOfFirst {
            it.d == Settings.dateFormat && it.t == Settings.timeFormat
        }

        // reakcie na zmeny
        binding.dualWeekEnabled.setOnCheckedChangeListener(ChangeReactor)
        binding.voiceCmdEnabled.setOnCheckedChangeListener(ChangeReactor)
        binding.workweekChoice.setOnChange(ChangeReactor)
        binding.lesLabelChoice.setOnChange(ChangeReactor)
        binding.snoozeChoice.setOnChange(ChangeReactor)
        binding.dtfChoice.setOnChange(ChangeReactor)

        // dialog nastavenia formatu datumu a casu
        binding.dtfChoice.setOnClickListener {
            val dialog = DateTimeFormatDialog()
            dialog.setOnConfirm(this::confirmDateTimeFormat)
            dialog.show(supportFragmentManager, DATE_TIME_FORMAT)
        }

        // odkaz na aktivitu kde sa nastavuje ton zvonenia
        binding.alarmTone.setOnClickListener {
            startActivity(Intent(this@Settings, AlarmToneActivity::class.java))
        }

        // obnovenie funkcie, ktora sa ma vykonat po potvrdeni zmien v nastaveni formatu datumu a casu
        (supportFragmentManager.findFragmentByTag(DATE_TIME_FORMAT) as DateTimeFormatDialog?)
            ?.setOnConfirm(this::confirmDateTimeFormat)
    }
}