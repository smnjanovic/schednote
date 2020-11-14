package com.moriak.schednote.menu

import com.moriak.schednote.fragments.of_alarm.AlarmClockSetter
import com.moriak.schednote.fragments.of_alarm.ReminderSetter

enum class AlarmCategory {
    REMINDER, ALARM;

    companion object {
        operator fun get(str: String?): AlarmCategory? {
            if (str == null) return null
            for (v in values())
                if (v.name == str)
                    return v
            return null
        }

        operator fun get(cls: Class<*>?): AlarmCategory? = when (cls?.canonicalName) {
            ReminderSetter::class.java.canonicalName -> REMINDER
            AlarmClockSetter::class.java.canonicalName -> ALARM
            else -> null
        }
    }

    val fragmentClass
        get() = when (this) {
            REMINDER -> ReminderSetter::class.java
            ALARM -> AlarmClockSetter::class.java
        }
}