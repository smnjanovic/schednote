package com.moriak.schednote.menu

import com.moriak.schednote.fragments.of_alarm.AlarmClockSetter
import com.moriak.schednote.fragments.of_alarm.ReminderSetter
import com.moriak.schednote.menu.AlarmCategory.ALARM
import com.moriak.schednote.menu.AlarmCategory.REMINDER

/**
 * pomocou [AlarmCategory] je možné vybrať si, či nastavovať notifikácie k úloham alebo budíky k rozvrhu
 * @property REMINDER Položka sa sústreďuje na notifikácie k upozorneniam
 * @property ALARM Položka sa sústreďuje na budíky k rozvrhu
 *
 * @property fragmentClass jedná sa o fragment, ktorý bude načítaný v pod fragmente [AlarmClockSetter]
 */
enum class AlarmCategory {
    REMINDER, ALARM;

    companion object {
        /**
         * Výber na základe názvu inštancie [AlarmCategory]
         * @param str
         * @return [AlarmCategory]
         */
        operator fun get(str: String?): AlarmCategory? {
            if (str == null) return null
            for (v in values())
                if (v.name == str)
                    return v
            return null
        }

        /**
         * Výber inštancie na základe zvolenej triedy
         * @param cls
         * @return [AlarmCategory]
         */
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