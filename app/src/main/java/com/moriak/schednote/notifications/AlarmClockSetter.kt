package com.moriak.schednote.notifications

import android.content.Context
import com.moriak.schednote.App
import com.moriak.schednote.enums.AlarmClockBit
import com.moriak.schednote.enums.AlarmClockBit.Companion.getBit
import com.moriak.schednote.enums.Day
import com.moriak.schednote.enums.Regularity
import com.moriak.schednote.enums.WorkWeek
import com.moriak.schednote.getNextTime
import com.moriak.schednote.notifications.NotificationHandler.AlarmClock.setAlarmClock
import com.moriak.schednote.now
import com.moriak.schednote.storage.Prefs.Notifications.AlarmTimes
import com.moriak.schednote.storage.Prefs.Notifications.bits
import com.moriak.schednote.storage.Prefs.Settings.dateFormat
import com.moriak.schednote.storage.Prefs.Settings.dualWeekSchedule
import com.moriak.schednote.storage.Prefs.Settings.earliestMinute
import com.moriak.schednote.storage.Prefs.Settings.timeFormat
import com.moriak.schednote.storage.Prefs.Settings.workWeek
import com.moriak.schednote.storage.Prefs.States.lastScheduleStartAdvance
import com.moriak.schednote.storage.SQLite
import java.util.*

/**
 * [AlarmClockSetter] slúži ako organizátor budíkov.
 */
object AlarmClockSetter {
    private val cal by lazy { Calendar.getInstance() }
    private fun getAlarms(ww: WorkWeek): Int = when (ww) {
        WorkWeek.SAT_WED -> 0x13E7CF
        WorkWeek.SAT_THU -> 0x17EFDF
        WorkWeek.SUN_WED -> 0x03C78F
        WorkWeek.MON_THU -> 0x078F1E
        WorkWeek.MON_FRI -> 0x0F9F3E
        WorkWeek.MON_SAT -> 0x1FBF7E
    }

    private fun getAlarms(reg: Regularity): Int = when (reg) {
        Regularity.EVEN -> 0x00007F
        Regularity.ODD -> 0x003F80
        Regularity.EVERY -> 0x1FC000
    }

    private fun getAlarms(day: Day): Int = when (day) {
        Day.MON -> 0x008102
        Day.TUE -> 0x010204
        Day.WED -> 0x020408
        Day.THU -> 0x040810
        Day.FRI -> 0x081020
        Day.SAT -> 0x102040
        Day.SUN -> 0x004081
    }

    private fun min2ms(day: Day, reg: Regularity) = when {
        isEnabled(day, reg) -> cal.now.getNextTime(day, reg, workWeek, AlarmTimes[getBit(day, reg)])
        else -> null
    }

    private fun forAll(alarms: Int, fn: AlarmClockBit.(enabled: Boolean)->Unit) {
        AlarmClockBit.values().forEach { it.fn(alarms and it.id > 0) }
    }

    private fun scheduleAlarms(): Int = when (dualWeekSchedule) {
        true -> getAlarms(Regularity.ODD) or getAlarms(Regularity.EVEN)
        false -> getAlarms(Regularity.EVERY)
    } and getAlarms(workWeek)

    /**
     * [Aktivuje | Deaktivuje] upozornenia na všetky nastavené budíky v danom týždni
     * @param context
     * @param reg týždeň
     * @param enable ak true, tak aktivovať, inak deaktivovať
     */
    fun enableAlarms(context: Context, reg: Regularity, enable: Boolean): Boolean {
        if (isEnabled(reg) xor enable) {
            val alarms = getAlarms(workWeek) and getAlarms(reg)
            bits = if (enable) alarms or bits else alarms.inv() and bits
            return setAlarms(context)
        }
        return true
    }

    /**
     * [Aktivuje | Deaktivuje] budík.
     * @param context
     * @param day Deň
     * @param reg Týždeň
     * @param enable Ak true, tak aktivovať, inak deaktivovať budík
     */
    fun enableAlarm(context: Context, day: Day, reg: Regularity, enable: Boolean): Boolean {
        val alarm = getAlarms(day) and getAlarms(reg)
        bits = if (enable) alarm or bits else alarm.inv() and bits
        val ms = min2ms(day, reg)
        val success = setAlarmClock(context, getBit(day, reg), ms)
        ms?.let { _ -> App.toast("${dateFormat.getFormat(ms)} ${timeFormat.getFormat(ms)}") }
        return success
    }

    /**
     * Nastaviť čas budenia a aktivovať budík
     * @param context
     * @param day Deň
     * @param reg Týždeň
     * @param dayMinutes Čas v dňových minútach
     */
    fun setAlarm(context: Context, day: Day, reg: Regularity, dayMinutes: Int): Boolean {
        AlarmTimes[getBit(day, reg)] = dayMinutes
        return enableAlarm(context, day, reg, true)
    }

    /**
     * Získať čas budenia v dňových minútach
     * @param day Deň
     * @param reg Týždeň
     * @return čas budenia v dňových minútach
     */
    fun getAlarm(day: Day, reg: Regularity): Int = AlarmTimes[getBit(day, reg)]

    /**
     * Aktivuje upozornenia na všetky zapnuté budíky
     * @param context
     */
    fun setAlarms(context: Context): Boolean {
        val success = NotificationHandler.canSetExactAlarm(context)
        if (success) {
            bits = bits and scheduleAlarms()
            forAll(bits) { setAlarmClock(context, this, min2ms(day, reg)) }
        }
        return success
    }

    /**
     * Nastaví budíky podľa rozvrhu hodín
     * @param context
     */
    fun setAlarmsBySchedule(context: Context): Boolean {
        val success = NotificationHandler.canSetExactAlarm(context)
        if (success) {
            bits = scheduleAlarms()
            forAll(bits) {
                if (it) {
                    val sqlStart = SQLite.firstLessonStart(day, reg)
                    if (sqlStart == null) bits = bits and (getAlarms(day) and getAlarms(reg)).inv()
                    else AlarmTimes[this] = sqlStart + earliestMinute - lastScheduleStartAdvance
                    setAlarmClock(context, this, min2ms(day, reg))
                }
            }
        }
        return success
    }

    /**
     * Zistí, či sú nejaké budíky zapnuté
     * @return true, ak je aspoň 1 budík zapnutý, inak false
     */
    fun isEnabled(): Boolean = bits > 0

    /**
     * Zistí, či sú nejaké budíky zapnuté v danom týždni
     * @param reg týždeň
     * @return true, ak je aspoň 1 budík zapnutý, inak false
     */
    fun isEnabled(reg: Regularity): Boolean {
        return bits and getAlarms(reg) > 0
    }

    /**
     * Zistí či je budík pre deň [day] a týždeň [reg] zapnutý.
     * @return true, ak je budík zapnutý.
     */
    fun isEnabled(day: Day, reg: Regularity) = getAlarms(day) and getAlarms(reg) and bits > 0

    /**
     * Umožní alebo znemožní rozdelenie rozvrhu na párny alebo nepárny.
     * Ak sú dochvíľné alarmy pre túto aplikáciu povolené, tak sa upravia
     * @param context
     * @param isDual true, ak umožní rozdelenie rozvrhu na párny alebo nepárny, inak false.
     */
    fun switchDualWeek(context: Context, isDual: Boolean) {
        val wasEnabled = isEnabled()
        dualWeekSchedule = isDual
        if (wasEnabled) {
            if (isDual) {
                val week = bits and getAlarms(Regularity.EVERY) shr 14
                bits = week shl 7 or week
                workWeek.workDays.forEach {
                    val min = AlarmTimes[getBit(it, Regularity.EVERY)]
                    AlarmTimes[getBit(it, Regularity.EVEN)] = min
                    AlarmTimes[getBit(it, Regularity.ODD)] = min
                }
            }
            else {
                val even = bits and getAlarms(Regularity.EVEN)
                val odd = bits and getAlarms(Regularity.ODD) shr 7
                bits = even or odd shl 14
                workWeek.workDays.forEach {
                    AlarmTimes[getBit(it, Regularity.EVERY)] =
                        AlarmTimes[getBit(it, Regularity.EVEN)]
                        .coerceAtMost(AlarmTimes[getBit(it, Regularity.ODD)])
                }
            }
            setAlarms(context)
        }
    }
}