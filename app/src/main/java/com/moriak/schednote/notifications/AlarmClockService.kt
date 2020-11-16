package com.moriak.schednote.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.other.Day
import com.moriak.schednote.settings.Prefs
import com.moriak.schednote.settings.Regularity

/**
 * Služba funguje ako budík, ktorý možno nastaviť len cez tlačidlá notifikácie. Trvá maximálne
 * minútu, potom bude automaticky odložený o určitý čas na neskôr.
 */
class AlarmClockService : Service() {
    companion object {
        private const val WAKE_UP = "WAKE_UP"
        private const val ALARM_CLOCK = "ALARM_CLOCK"
        private const val ALARM_CLOCK_DESC = "Alarm Clock Channel"

        /**
         * Získanie intentu určeného na zapnutie služby na popredí
         * @param context
         * @param alarmId
         * @return Vráti intent použitý na spustenie služby na popredí
         */
        fun getIntent(context: Context?, alarmId: Int): Intent =
            Intent(context, AlarmClockService::class.java).apply {
                action = ALARM_CLOCK
                putExtra(ClockReceiver.ALARM_ID, alarmId)
            }
    }

    private lateinit var ring: MediaPlayer
    private lateinit var handler: Handler
    private lateinit var day: Day
    private lateinit var reg: Regularity

    private fun createAlarmChannel(): NotificationChannel? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = App.ctx.getSystemService(NotificationManager::class.java)!!
            val channel =
                NotificationChannel(ALARM_CLOCK, WAKE_UP, NotificationManager.IMPORTANCE_HIGH)
            channel.description = ALARM_CLOCK_DESC
            manager.createNotificationChannel(channel)
            return channel
        }
        return null
    }

    private fun createNotification(day: Day, reg: Regularity) =
        NotificationCompat.Builder(this, ALARM_CLOCK)
            .setSmallIcon(R.drawable.ic_schednote)
            .setContentTitle(App.str(R.string.alarm_clock))
            .setContentText(
                "$day ${
                    Prefs.settings.getTimeString(
                        Prefs.notifications.getAlarm(
                            day,
                            reg
                        )
                    )
                }"
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(Notification.DEFAULT_ALL)
            .addAction(
                R.drawable.ic_alarm_off_black_24dp,
                App.str(R.string.stop),
                ClockReceiver.getStopPIntent(day, reg)
            )
            .addAction(
                R.drawable.ic_snooze_black_24dp,
                App.str(R.string.snooze),
                ClockReceiver.getSnoozePIntent(day, reg)
            )
            .setNotificationSilent()
            .build()

    private fun stop() {
        ring.stop()
        ring.reset()
        handler.removeCallbacksAndMessages(null)
        stopForeground(true)
        stopSelf()
    }

    /**
     * Zobrazenie notifikácie a spustenie hudby, ktorá bude hrať najdlhšie minútu, potom sa
     * budík sám odloží na neskôr. Notifikáciu možno zavreť buď odkladom alebo zastavením budíka.
     * Tým sa zastaví aj hudba
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ring = MediaPlayer.create(this, Prefs.settings.alarmTone.uri)
        ring.setScreenOnWhilePlaying(true)
        ring.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setLegacyStreamType(AudioManager.STREAM_ALARM)
                .build()
        )
        ring.isLooping = true
        ring.setVolume(1.0F, 1.0F)
        handler = Handler(Looper.myLooper()!!)
        ring.start()

        val id = intent!!.getIntExtra(ClockReceiver.ALARM_ID, 0)
        day = ClockReceiver.detectDay(id)
        reg = ClockReceiver.detectRegularity(id)

        handler.postDelayed({
            stop()
            ClockReceiver.snooze(day, reg)
        }, 60000)

        createAlarmChannel()
        startForeground(1, createNotification(day, reg))
        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? = null

    /**
     * Zastavenie hudby a zrušenie notifkácie, ukončenie služby
     */
    override fun onDestroy() {
        stop()
        super.onDestroy()
    }
}