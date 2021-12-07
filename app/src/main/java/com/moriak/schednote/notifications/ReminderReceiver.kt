package com.moriak.schednote.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.moriak.schednote.notifications.NotificationHandler.Reminder.handleMessage

/**
 * Objekt prijíma správy o blížiacich sa termínoch úloh a posiela upozornia užívateľovi
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = handleMessage(context, intent)
}