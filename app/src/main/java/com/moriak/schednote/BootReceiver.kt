package com.moriak.schednote

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.moriak.schednote.settings.Prefs

/**
 * Po reštarte zariadenia sú všetky upozornenia, ktoré táto aplikácia mala nastavené, zrušené.
 * Táto trieda je ich po reštarte obnoví, pokiaľ majú byť zapnuté
 */
class BootReceiver : BroadcastReceiver() {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        if (Prefs.notifications.alarmsEnabled) Prefs.notifications.reEnableAlarm()
        App.data.enableNoteNotifications(Prefs.notifications.reminderEnabled)
    }
}