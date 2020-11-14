package com.moriak.schednote

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.moriak.schednote.settings.Prefs

class BootReceiver : BroadcastReceiver() {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        if (Prefs.notifications.alarmsEnabled) Prefs.notifications.resetAlarms()
        App.data.enableNoteNotifications(Prefs.notifications.reminderEnabled)
    }
}