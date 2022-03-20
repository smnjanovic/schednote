package com.moriak.schednote.enums

import androidx.annotation.IdRes
import com.moriak.schednote.R
import com.moriak.schednote.enums.AlarmCategory.ALARM
import com.moriak.schednote.enums.AlarmCategory.REMINDER
import com.moriak.schednote.fragments.SubActivity
import com.moriak.schednote.fragments.of_alarm.AlarmClockList
import com.moriak.schednote.fragments.of_alarm.ReminderSettings
import com.moriak.schednote.interfaces.ISubContent
import com.moriak.schednote.storage.Prefs

/**
 * Inštancie tejto triedy reprezentujú obsah týkajúci sa oznámení.
 * [REMINDER] Položka sa sústreďuje na notifikácie k upozorneniam
 * [ALARM] Položka sa sústreďuje na budíky
 */
enum class AlarmCategory(
    @IdRes override val button: Int,
    override val fragmentClass: Class<out SubActivity<*>>
): ISubContent {
    REMINDER(R.id.reminders, ReminderSettings::class.java),
    ALARM(R.id.alarms, AlarmClockList::class.java);
    override val parent: ISubContent? get() = SubContent.ALARMS
    override fun remember() { Prefs.States.lastAlarmCategory = this }
    companion object: ISubContent.ISubContentCompanion {
        override val container: Int = R.id.alarm_set_zone
        override val values: Array<out ISubContent> = values()
        override val lastSet: ISubContent get() = Prefs.States.lastAlarmCategory
    }
}