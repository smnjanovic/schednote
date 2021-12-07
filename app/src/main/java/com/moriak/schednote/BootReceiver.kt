package com.moriak.schednote

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.moriak.schednote.notifications.AlarmClockSetter
import com.moriak.schednote.notifications.ReminderSetter

/**
 * Po reštarte zariadenia sú všetky upozornenia, ktoré táto aplikácia mala nastavené, zrušené.
 * Táto trieda ich po reštarte všetky obnoví
 */
class BootReceiver : BroadcastReceiver() {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
            AlarmClockSetter.setAlarms(context)
            ReminderSetter.enableReminders(context, true)
    }
}