package com.moriak.schednote.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.moriak.schednote.notifications.NotificationHandler.AlarmClock.handleReceiver

/**
 * Objekt prijme správu o budíku, na základe ktorej budík nastaví, zastaví, odloží alebo naň upozorní.
 */
class AlarmClockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) = handleReceiver(context, intent)
}