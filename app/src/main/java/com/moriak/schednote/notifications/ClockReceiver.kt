package com.moriak.schednote.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.os.Build
import com.moriak.schednote.App
import com.moriak.schednote.other.Day
import com.moriak.schednote.settings.Prefs
import com.moriak.schednote.settings.Regularity
import java.util.*

class ClockReceiver : BroadcastReceiver() {
    companion object {
        private const val ALARM_CLOCK = "ALARM_CLOCK"
        private const val SNOOZE = "SNOOZE"
        private const val STOP = "STOP"
        const val ALARM_ID = "ALARM_ID"
        private val cal by lazy { Calendar.getInstance() }

        private fun getAlarmId(day: Day, reg: Regularity): Int =
            (reg.odd?.let { if (it) 1 else 0 } ?: 2) * 10 + day.value

        fun detectDay(id: Int): Day = when (id % 10) {
            in 1..7 -> Day[id % 10]
            else -> throw Exception("The day doesn't exist!")
        }

        fun detectRegularity(id: Int): Regularity = when (id / 10) {
            0 -> Regularity.EVEN
            1 -> Regularity.ODD
            2 -> Regularity.EVERY
            else -> throw Exception("The day of the alarm doesn't exist!")
        }

        private fun getAlarmPIntent(id: Int, act: String?): PendingIntent {
            val intent = Intent(App.ctx, ClockReceiver::class.java)
            intent.action = act
            intent.putExtra(ALARM_ID, id)
            return PendingIntent.getBroadcast(App.ctx, id, intent, FLAG_UPDATE_CURRENT)
        }

        private fun getNextTime(day: Day, reg: Regularity): Long {
            fun nextWeek(n: Int = 1) = cal.add(Calendar.DAY_OF_YEAR, 7 * n)

            val minutesOfDay = Prefs.notifications.getAlarm(day, reg)
            cal.timeInMillis = System.currentTimeMillis()
            while (cal.get(Calendar.DAY_OF_WEEK) != day.value) cal.add(Calendar.DAY_OF_YEAR, 1)
            if (reg != Regularity.EVERY) {
                val isOdd = Regularity.isWeekOdd(cal.timeInMillis)
                if (isOdd && reg == Regularity.EVEN || !isOdd && reg == Regularity.ODD) nextWeek()
            }

            cal.set(Calendar.HOUR_OF_DAY, minutesOfDay / 60)
            cal.set(Calendar.MINUTE, minutesOfDay % 60)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            if (cal.timeInMillis <= System.currentTimeMillis()) nextWeek(if (reg == Regularity.EVERY) 1 else 2)
            return cal.timeInMillis
        }

        private fun setBroadcast(day: Day, reg: Regularity, time: Long) {
            val pendingIntent = getAlarmPIntent(getAlarmId(day, reg), ALARM_CLOCK)
            val alarm = App.ctx.getSystemService(ALARM_SERVICE) as AlarmManager
            alarm.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent)
        }

        private fun cancelBroadcast(day: Day, reg: Regularity) =
            (App.ctx.getSystemService(ALARM_SERVICE) as AlarmManager)
                .cancel(getAlarmPIntent(getAlarmId(day, reg), ALARM_CLOCK))

        /**
         * Nastaví čas upozornenia na blížiaci sa termín dokončenia úlohy alebo upozornenie odvolá
         * @param day deň upozornenia
         * @param reg Výber medzi párnym / nepárnym / každým týždňom, kedy sa budík má opakovať
         * @param enabled Určuje, či je účelom budík zapnúť alebo vypnúť
         */
        fun enableAlarmClock(day: Day, reg: Regularity, enabled: Boolean = true) =
            (if (enabled) setBroadcast(day, reg, getNextTime(day, reg)) else cancelBroadcast(
                day,
                reg
            ))

        fun snooze(day: Day, reg: Regularity) = setBroadcast(
            day,
            reg,
            System.currentTimeMillis().let { it - it % 60000 } + Prefs.settings.snooze * 60000)

        fun getSnoozePIntent(day: Day, reg: Regularity) =
            getAlarmPIntent(getAlarmId(day, reg), SNOOZE)

        fun getStopPIntent(day: Day, reg: Regularity) = getAlarmPIntent(getAlarmId(day, reg), STOP)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val id = intent?.getIntExtra(ALARM_ID, 0) ?: 0
        when (intent?.action) {
            ALARM_CLOCK -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    context!!.startForegroundService(AlarmClockService.getIntent(context, id))
                else context!!.startService(AlarmClockService.getIntent(context, id))
            }
            SNOOZE -> {
                context!!.stopService(Intent(context, AlarmClockService::class.java))
                snooze(detectDay(id), detectRegularity(id))
            }
            STOP -> {
                context!!.stopService(Intent(context, AlarmClockService::class.java))
                enableAlarmClock(detectDay(id), detectRegularity(id))
            }
        }


        /*context!!.setTheme(R.style.AppTheme)
        val id = intent.getIntExtra(ALARM_ID, -1)
        val day = detectDay(id)
        val reg = detectRegularity(id)
        val dayMinutes = intent.getIntExtra(MINUTES_OF_DAY, 360)

        // zobrazenie notifikacie

        val nextTime = getNextTime(day, reg, dayMinutes)

        //val activityIntent = AlarmClockActivity.getStartIntent(context, day, reg, dayMinutes)
        //val pendingIntent = PendingIntent.getActivity(context, 0, activityIntent, FLAG_UPDATE_CURRENT)

        val alarmClock = NotificationCompat.Builder(context, ALARM_CLOCK)
            .setSmallIcon(R.drawable.ic_schednote)
            .setContentTitle(App.str(R.string.alarm_clock))
            .setContentText("${detectDay(id)} ${Prefs.settings.getTimeString(nextTime)}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            //.setFullScreenIntent(pendingIntent, true)
            .build()

        // nastavenie zvonenia
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            NotificationManagerCompat.from(context).getNotificationChannel(ALARM_CLOCK)?.setSound (
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )



        // upozornenie

        NotificationManagerCompat.from(context).notify(id, alarmClock)


        // nastavenie rovnakeho upozornenia o tyzden alebo 2
        //editAlarmClock(day, reg, minutesOfDay)

        */
    }
}