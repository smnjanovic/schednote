package com.moriak.schednote.notifications

import android.app.AlarmManager
import android.app.AlarmManager.RTC_WAKEUP
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.other.Redirection
import com.moriak.schednote.settings.Prefs

/**
 * K úloham pripnutých k predmetom sa vzťahuju notifikácie na bežiace na pozadí
 */
class NoteReminder : BroadcastReceiver() {
    companion object {
        private const val ACTION_NOTIFY = "ACTION_NOTIFY"
        private const val NOTE_ID = "NOTE_ID"
        private const val NOTE_CHANNEL_ID = "NOTE_CHANNEL_ID"
        private const val CHANNEL_NOTE = "CHANNEL_NOTE"
        private const val CHANNEL_NOTE_DESC = "Note reminder channel!"
        private const val CHANNEL_NOTE_GROUP = "CHANNEL_NOTE_GROUP"
        private const val TITLE = "TITLE"
        private const val CONTENT = "CONTENT"
        private const val NOTE_WHEN = "WHEN"
        private const val TIME = "TIME"

        /**
         * Prevod ID Poznamky typu long na ID notifikacie typu Int. Aky pretecie kapacita, pocita sa
         * znova od 0.
         *
         * @param id [Long] ID of the Note
         * @return [Int] ID of the notification
         */
        fun noteIdToInt(id: Long) = (id % Int.MAX_VALUE).toInt()

        private fun getNotificationPIntent(
            note: Long,
            subject: String,
            content: String,
            deadline: Long?
        ) = PendingIntent
            .getBroadcast(
                App.ctx, noteIdToInt(note), Intent(App.ctx, NoteReminder::class.java)
                    .setAction(ACTION_NOTIFY)
                    .putExtra(NOTE_ID, note)
                    .putExtra(TIME, deadline)
                    .putExtra(TITLE, subject)
                    .putExtra(CONTENT, content)
                    .putExtra(NOTE_WHEN, deadline), 0
            )

        /**
         * Vytvorí sa kanál, v ktorom budu zoskupene notifikácie na úlohy
         */
        fun createNoteReminderChannel() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = App.ctx.getSystemService(NotificationManager::class.java)!!
                val channel = NotificationChannel(
                    NOTE_CHANNEL_ID,
                    CHANNEL_NOTE,
                    NotificationManager.IMPORTANCE_HIGH
                )
                channel.description = CHANNEL_NOTE_DESC
                manager.createNotificationChannel(channel)
            }
        }

        /**
         * Nastaví čas upozornenia na blížiaci sa termín dokončenia úlohy alebo upozornenie odvolá
         * @param note ID úlohy, na ktorú treba upozorniť
         * @param subject Skratka názvu predmetu
         * @param content Popis úlohy
         * @param deadline Ak hodnota [deadline] je null notifikácia sa zruší, inak sa nastaví na daný čas v milisekundách
         */
        fun editNotification(note: Long, subject: String, content: String, deadline: Long?) {
            val notificationIntent = getNotificationPIntent(note, subject, content, deadline)
            val alarm = App.ctx.getSystemService(ALARM_SERVICE) as AlarmManager
            if (deadline == null) alarm.cancel(notificationIntent)
            else alarm.setExact(
                RTC_WAKEUP,
                deadline - Prefs.notifications.reminderAdvanceInMinutes * 60000,
                notificationIntent
            )
        }

    }

    /**
     * Zobrazenie notifikacie
     *
     * @param context
     * @param intent
     */
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_NOTIFY -> {
                val noteId = intent.getLongExtra(NOTE_ID, -1L)
                val request = noteIdToInt(noteId)
                val title = intent.getStringExtra(TITLE)!!
                val content = intent.getStringExtra(CONTENT)!!
                val millis = intent.getLongExtra(NOTE_WHEN, System.currentTimeMillis())

                val newIntent = Redirection.NOTES.makeIntent(context)
                    .putExtra(Redirection.EXTRA_NOTE_ID, noteId)
                val openIntent = PendingIntent.getActivity(
                    context,
                    request,
                    newIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
                val notification = NotificationCompat
                    .Builder(context, NOTE_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_schednote)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(openIntent)
                    .setAutoCancel(true)
                    .setGroup(CHANNEL_NOTE_GROUP)
                    .setGroupSummary(true)
                    .setWhen(millis)
                    .build()
                NotificationManagerCompat.from(context).notify(request, notification)
            }
        }
    }
}