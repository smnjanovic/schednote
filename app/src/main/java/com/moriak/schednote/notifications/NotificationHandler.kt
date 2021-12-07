package com.moriak.schednote.notifications

import android.app.Activity
import android.app.AlarmManager
import android.app.AlarmManager.RTC_WAKEUP
import android.app.Notification.DEFAULT_ALL
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.PendingIntent.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat.*
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.moriak.schednote.R.drawable.ic_schednote
import com.moriak.schednote.activities.LockScreenAlarmActivity
import com.moriak.schednote.data.Note
import com.moriak.schednote.enums.AlarmClockBit
import com.moriak.schednote.enums.Redirection.Companion.EXTRA_NOTE_ID
import com.moriak.schednote.enums.Redirection.NOTES
import com.moriak.schednote.notifications.AlarmClockSetter.getAlarm
import com.moriak.schednote.notifications.NotificationHandler.UniqueIntent.*
import com.moriak.schednote.storage.Prefs.Notifications.reminderAdvanceInMinutes
import com.moriak.schednote.storage.Prefs.Notifications.reminderEnabled
import com.moriak.schednote.storage.Prefs.Settings.snoozeTime
import com.moriak.schednote.storage.Prefs.Settings.timeFormat
import com.moriak.schednote.storage.SQLite
import android.app.PendingIntent as PI
import com.moriak.schednote.R.drawable.ic_alarm_off_black_24dp as stop_icon
import com.moriak.schednote.R.drawable.ic_snooze_black_24dp as snooze_icon
import com.moriak.schednote.R.string.alarm_clock as res_ac
import com.moriak.schednote.R.string.notes as res_notes
import com.moriak.schednote.R.string.snooze as snoozeRes
import com.moriak.schednote.R.string.stop as stopRes
import java.lang.System.currentTimeMillis as now

/**
 * Úlohou objektu [NotificationHandler] je spravovať všetky oznámenia.
 */
object NotificationHandler {
    private interface DataReader { val key: String }
    private abstract class DataAnalyzer<T>(override val key: String): DataReader {
        protected abstract fun str(tag: T): String
        abstract fun getTag(str: String): T?
        inline fun <reified M> ifMatch(intent: Intent?, callback: (M)->Unit) {
            val str = intent?.dataString ?: return
            if (str.length <= key.length || str.substring(key.indices) != key) return
            getTag(str.substring(key.length))?.let{ if (it is M) callback(it) }
        }
        fun makeIntent(context: Context, tag: T, action: String, cls: Class<out Any>) =
            Intent(action, "$key${str(tag)}".toUri(), context, cls)
    }
    private object AlarmData: DataAnalyzer<AlarmClockBit>("alarm_clock://") {
        override fun str(tag: AlarmClockBit): String = tag.name
        override fun getTag(str: String): AlarmClockBit? = AlarmClockBit[str]
    }
    private object ReminderData: DataAnalyzer<Note>("reminder://") {
        override fun str(tag: Note): String = tag.id.toString()
        override fun getTag(str: String): Note? = if (!str.matches("^-?[0-9]+$".toRegex())) null
        else SQLite.note(str.toLong())
    }
    private enum class UniqueIntent (
        private val action: String,
        private val data: DataReader,
        private val cls: Class<out Any>
    ) {
        F_SCR("ALARM_CLOCK_ACTION_FULLSCREEN", AlarmData, LockScreenAlarmActivity::class.java),
        WAKE_UP("ALARM_CLOCK_ACTION_WAKE_UP", AlarmData, AlarmClockReceiver::class.java),
        SNOOZE("ALARM_CLOCK_ACTION_SNOOZE", AlarmData, AlarmClockReceiver::class.java),
        STOP("ALARM_CLOCK_ACTION_STOP", AlarmData, AlarmClockReceiver::class.java),
        REMIND("REMINDER_ACTION_REMIND", ReminderData, ReminderReceiver::class.java);
        inline fun <reified T> ifMatch(intent: Intent?, callback: (T)->Unit) {
            if (intent?.action == action) {
                if (data is AlarmData) data.ifMatch(intent, callback)
                if (data is ReminderData) data.ifMatch(intent, callback)
            }
        }
        fun intent(context: Context, tag: Any): Intent = when (data) {
            is AlarmData -> data.makeIntent(context, tag as AlarmClockBit, action, cls)
            is ReminderData -> data.makeIntent(context, tag as Note, action, cls)
            else -> throw RuntimeException("Unreachable exception!!!")
        }
        fun pIntent(context: Context, tag: Any) = when {
            Activity::class.java.isAssignableFrom(cls) -> PI::getActivity
            BroadcastReceiver::class.java.isAssignableFrom(cls) -> PI::getBroadcast
            else -> throw ClassCastException("Unreachable exception!!!")
        }(context, ordinal, intent(context, tag), FLAG_UPDATE_CURRENT)!!
    }
    const val AC_CHANNEL = "ALARM_CLOCK_CHANNEL"
    const val R_CHANNEL = "REMINDER_CHANNEL"

    private fun <T> alarm(context: Context, fn: AlarmManager.()->T) = context
        .getSystemService(AlarmManager::class.java).fn()

    private fun <T> alarm(context: Context, ui: UniqueIntent, data: Any, fn: AlarmManager.(PI)->T) =
        alarm(context) { fn(ui.pIntent(context, data)) }

    private fun createChannel(ctx: Context, chID: String, @StringRes name: Int) {
        if (SDK_INT >= O) {
            val nm = ctx.getString(name)
            val ch = NotificationChannel(chID, nm, IMPORTANCE_HIGH)
            ch.description = nm
            ctx.getSystemService(NotificationManager::class.java)?.createNotificationChannel(ch)
        }
    }

    fun canSetExactAlarm(context: Context): Boolean = alarm(context) {
        if (SDK_INT >= 31) canScheduleExactAlarms() else true
    }

    /**
     * Funkcia je zodpovedná za [spustenie | zastavenie] melódie budíka.
     * @param context
     * @param enable keď true, melódia sa spustí, inak sa zastaví
     */
    fun blare(context: Context, enable: Boolean) {
        val intent = Intent(context, AlarmClockRingTone::class.java)
        if (enable) context.startService(intent) else context.stopService(intent)
    }

    /**
     * Tento objekt naplánuje udalosti týkajúce budenia
     * @return true, ak zrušenie alebo naplánovanie udalosti dopadlo úspešne
     */
    object AlarmClock {
        private fun AlarmManager.setBroadcast(context: Context, pi: PI, ms: Long?): Boolean {
            var success = true
            ms?.let {
                success = canSetExactAlarm(context)
                if (success) setExactAndAllowWhileIdle(RTC_WAKEUP, it, pi)
            } ?: cancel(pi)
            return success
        }

        private fun snoozeTime(min: Int) = now().let { it - it % 60000 } + 60000 * min

        private fun notify(ctx: Context, acb: AlarmClockBit) {
            createChannel(ctx, AC_CHANNEL, res_ac)
            LockScreenAlarmActivity.active = true
            val ntf = Builder(ctx, AC_CHANNEL)
                .setSmallIcon(ic_schednote)
                .setContentTitle(ctx.getString(res_ac))
                .setContentText(acb.toString(timeFormat, getAlarm(acb.day, acb.reg)))
                .setSilent(true)
                .setPriority(PRIORITY_HIGH)
                .setVisibility(VISIBILITY_PUBLIC)
                .setCategory(CATEGORY_ALARM)
                .setDefaults(DEFAULT_ALL)
                .setAutoCancel(true)
                .setSilent(true)
                .setFullScreenIntent(F_SCR.pIntent(ctx, acb), true)
                .addAction(stop_icon, ctx.getString(stopRes), STOP.pIntent(ctx, acb))
                .addAction(snooze_icon, ctx.getString(snoozeRes), SNOOZE.pIntent(ctx, acb))
                .build()
            NotificationManagerCompat.from(ctx).notify(acb.ordinal, ntf)
        }

        /**
         * Aktivuje alebo deaktivuje upozornenie na budík
         * @param ctx
         * @param acb budík
         * @param ms čas budenia. Keď je null, upozornenie na budík sa deaktivuje.
         */
        fun setAlarmClock(ctx: Context, acb: AlarmClockBit, ms: Long?) = alarm(ctx, WAKE_UP, acb) {
            setBroadcast(ctx, it, ms)
        }

        /**
         * Odloží sa budík [acb] o niekoľko minút.
         * @param ctx
         * @param acb konkrétny budík
         */
        fun snooze(ctx: Context, acb: AlarmClockBit) = SNOOZE.pIntent(ctx, acb).send()

        /**
         * Zastaví sa budík a naplánuje sa ďalší o 1-2 týždne.
         * @param ctx
         * @param acb budík
         */
        fun stop(ctx: Context, acb: AlarmClockBit) = STOP.pIntent(ctx, acb).send()

        /**
         * Funkcia, ktorá zareaguje na správu k danému budíku a na základe nej sa udeje 1 z 3 scenárov:
         *
         * 1. Spustí sa budík
         * 2. Odloží sa budík o niekoľko minút
         * 3. Zastaví sa budík a naplánuje sa ďalší o 1-2 týždne
         *
         * @param context
         * @param intent obsah správy
         */
        fun handleReceiver(context: Context, intent: Intent?) {
            WAKE_UP.ifMatch<AlarmClockBit>(intent) { bit ->
                alarm(context, SNOOZE, bit) { setBroadcast(context, it, snoozeTime(1) - 5000) }
                notify(context, bit)
                blare(context, true)
            }
            SNOOZE.ifMatch<AlarmClockBit>(intent) { bit ->
                alarm(context, SNOOZE, bit) { setBroadcast(context, it, null) }
                alarm(context, WAKE_UP, bit) { setBroadcast(context, it, snoozeTime(snoozeTime)) }
                blare(context, false)
                dismiss(context, bit)
            }
            STOP.ifMatch<AlarmClockBit>(intent) { bit ->
                alarm(context, SNOOZE, bit) { setBroadcast(context, it, null) }
                AlarmClockSetter.enableAlarm(context, bit.day, bit.reg, true)
                blare(context, false)
                dismiss(context, bit)
            }
        }

        /**
         * Notifikáciu budíku sa skryje
         * @param ctx
         * @param acb Budík
         */
        fun dismiss(ctx: Context, acb: AlarmClockBit) = NotificationManagerCompat.from(ctx).cancel(acb.ordinal)

        /**
         * Aktivita, ktorá by mala byť zodpovedná za prebudenie používateľa,
         * vykoná svoju funkciu [fn], ak bola naozaj spustená za účelom
         * zobudenia používateľa. To môže byť dokázané obsahom intentu [intent].
         * @param intent
         * @param fn
         */
        fun handleActivity(intent: Intent?, fn: (AlarmClockBit) -> Unit) = F_SCR.ifMatch(intent, fn)
    }

    /**
     * Tento objekt spravuje oznámenia týkajúce sa lennadchadzajúcich úloh s konečným termínom
     */
    object Reminder {
        /**
         * Nastaví alebo zruší pripomienku na blížiaci sa termín úlohy [Note].
         * @param ctx
         * @param note
         * @param enable Ak true, tak nastaví, inak zruší pripomienku
         * @return true, ak sa pripomienka naplánovala alebo zrušila s úspechom
         */
        fun setReminder(ctx: Context, note: Note, enable: Boolean): Boolean {
            val success = !enable || reminderEnabled && canSetExactAlarm(ctx)
            if (success) {
                alarm(ctx, REMIND, note) { pi ->
                    if (!enable || note.deadline == null) cancel(pi)
                    else setExactAndAllowWhileIdle(RTC_WAKEUP, note.deadline - reminderAdvanceInMinutes, pi)
                }
            }
            return success
        }

        /**
         * Nastaví alebo zruší pripomienky na zoznam úloh [notes].
         * @param ctx
         * @param notes
         * @param enable ak true, tak nastaví, inak zruší pripomienky na úlohy [notes].
         * @return true, ak naplánovanie alebo zrušenie pripomienky bolo úspešné
         */
        fun setReminders(ctx: Context, notes: List<Note>, enable: Boolean) = alarm(ctx) {
            val success = !enable || reminderEnabled && canSetExactAlarm(ctx)
            if (success) notes.forEach { setReminder(ctx, it, enable) }
            success
        }

        /**
         * Funkcia je zodpovedná za spracovanie správy upozorňujúcej nablížiaci sa termín úlohy.
         * Zariadenie na to upozorní používateľa prostredníctvom notifikácie.
         * @param ctx
         * @param intent
         */
        fun handleMessage(ctx: Context, intent: Intent?) {
            REMIND.ifMatch<Note>(intent) { note ->
                createChannel(ctx, R_CHANNEL, res_notes)
                NotificationManagerCompat.from(ctx).notify(note.id.toInt(), Builder(ctx, R_CHANNEL)
                    .setSmallIcon(ic_schednote)
                    .setContentTitle(note.sub.abb)
                    .setContentText(note.info)
                    .setPriority(PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(NOTES.prepare(ctx, note.id.toInt(), true) {
                        putLong(EXTRA_NOTE_ID, note.id)
                    })
                    .setChannelId(R_CHANNEL)
                    .build())
            }
        }

        /**
         * Zruší notifikáciu úlohy [note]
         * @param context kontext
         * @param note úloha
         */
        fun hideNotification(context: Context, note: Note) =
            NotificationManagerCompat.from(context).cancel(note.id.toInt())
    }
}