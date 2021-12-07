package com.moriak.schednote.notifications

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes.*
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.IBinder
import com.moriak.schednote.storage.Prefs.Settings.alarmTone

/**
 * Prehráva zvuk na pozadí počas budenia
 */
class AlarmClockRingTone : Service() {
    private var mp: MediaPlayer? = null

    private fun changeMusic(uri: Uri?) {
        mp?.stop()
        mp?.release()
        mp = uri?.let { _ ->
            val aa = Builder().setContentType(CONTENT_TYPE_MUSIC).setFlags(FLAG_AUDIBILITY_ENFORCED)
                .setUsage(USAGE_NOTIFICATION_RINGTONE).build()
            val am = baseContext.getSystemService(AUDIO_SERVICE) as AudioManager
            MediaPlayer.create(baseContext, uri, null, aa, am.generateAudioSessionId())
        }
        mp?.start()
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_NOT_STICKY.also {
        changeMusic(alarmTone.uri)
    }

    override fun onDestroy() {
        changeMusic(null)
    }
}