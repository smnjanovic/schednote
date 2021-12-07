package com.moriak.schednote.activities

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
import android.os.Build.VERSION_CODES.O_MR1
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.moriak.schednote.R
import com.moriak.schednote.enums.AlarmClockBit
import com.moriak.schednote.enums.Redirection
import com.moriak.schednote.notifications.AlarmClockSetter
import com.moriak.schednote.notifications.NotificationHandler.AlarmClock
import com.moriak.schednote.notifications.NotificationHandler.AlarmClock.dismiss
import com.moriak.schednote.notifications.NotificationHandler.AlarmClock.snooze
import com.moriak.schednote.notifications.NotificationHandler.AlarmClock.stop
import com.moriak.schednote.storage.Prefs.Settings.timeFormat
import kotlinx.android.synthetic.main.activity_active_alarm.*
import android.view.WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON as SCR_LOCK
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON as SCR_ON

/**
 * Aktivita zobrazí budík s ponukou  zastaviť ho alebo odložiť o pár minúť
 */
class LockScreenAlarmActivity : AppCompatActivity() {
    companion object { var active: Boolean = false }
    private var bit: AlarmClockBit? = null
    private val onClick = View.OnClickListener { v ->
        bit?.let {
            if (v == ac_snooze_img) snooze(this, it)
            else if (v == ac_stop_img) stop(this, it)
            bit = null
        }
        leave()
    }
    private val handler = Handler(Looper.getMainLooper())

    private fun shine(bool: Boolean) {
        if (SDK_INT >= O_MR1) {
            setShowWhenLocked(bool)
            setTurnScreenOn(bool)
        } else (if (bool) window::addFlags else window::clearFlags)(SCR_LOCK or SCR_ON)
        if (bool && SDK_INT >= O) (getSystemService(KEYGUARD_SERVICE) as KeyguardManager)
            .requestDismissKeyguard(this, null)
    }

    private fun intentAction(intent: Intent?) {
        if (active) {
            bit = null
            AlarmClock.handleActivity(intent) { bit = it }
        }
    }

    private fun leave() {
        active = false
        if (isTaskRoot) Redirection.MAIN.redirect(this, true) else finish()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intentAction(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (active) {
            shine(true)
            supportActionBar?.hide()
            actionBar?.hide()
            setContentView(R.layout.activity_active_alarm)
            ac_snooze_img.setOnClickListener(onClick)
            ac_stop_img.setOnClickListener(onClick)
            intentAction(intent)
            handler.postDelayed({ leave() }, 55000)
            bit?.let { bit ->
                ac_reg.text = bit.reg.toString()
                ac_day.text = bit.day.toString()
                ac_time.text = timeFormat.getFormat(AlarmClockSetter.getAlarm(bit.day, bit.reg))
                dismiss(this, bit)
            } ?: leave()
        }
        else leave()
    }

    override fun onDestroy() {
        super.onDestroy()
        shine(false)
        handler.removeCallbacksAndMessages(null)
        bit?.let { snooze(this, it) }
        bit = null
    }
}