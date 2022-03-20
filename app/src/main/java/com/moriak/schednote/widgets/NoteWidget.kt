package com.moriak.schednote.widgets

import android.app.AlarmManager
import android.app.AlarmManager.RTC_WAKEUP
import android.app.PendingIntent
import android.app.PendingIntent.getBroadcast
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.*
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.data.Subject
import com.moriak.schednote.enums.Redirection.Companion.EXTRA_NOTE_CATEGORY
import com.moriak.schednote.enums.Redirection.Companion.EXTRA_NOTE_ID
import com.moriak.schednote.enums.Redirection.NOTES
import com.moriak.schednote.enums.TimeCategory
import com.moriak.schednote.enums.TimeCategory.TODAY
import com.moriak.schednote.interfaces.NoteCategory
import com.moriak.schednote.nextMidnight
import com.moriak.schednote.notifications.ReminderSetter.unsetNote
import com.moriak.schednote.storage.Prefs.Widgets
import com.moriak.schednote.storage.SQLite
import com.moriak.schednote.widgets.NoteWidgetService.Companion.NOTE
import java.util.*

/**
 * Widget tohoto typu zobrazuje zoznam úloh podľa nakonfigurovanej kategórie
 */
class NoteWidget : AppWidgetProvider() {
    companion object {
        private const val ITEM_ACTION = "ACTION_REMOVAL"
        private const val TARGET_CATEGORY = "TARGET_CATEGORY"
        private const val widgetUpdateRequest = -2000

        private fun all(context: Context) = getInstance(context)
            .getAppWidgetIds(ComponentName(context, NoteWidget::class.java)) ?: intArrayOf()

        private fun makeIntent(context: Context) =
            Intent(ACTION_APPWIDGET_UPDATE, null, context, NoteWidget::class.java)
            .putExtra(EXTRA_APPWIDGET_IDS, all(context))

        /**
         * Aktualizovať všetky widgety, ktoré sú k dispozícii
         *
         * @param context nepovinný údaj
         */
        fun update(context: Context) = context.sendBroadcast(makeIntent(context))

        private fun updatePI(context: Context): PendingIntent =
            getBroadcast(context, widgetUpdateRequest, makeIntent(context), 0)

        /**
         * Aktualizuje sa konkrétny widget
         *
         * @param context Context pre potreby práce s layoutom widgetu
         * @param manager Manážer, spravuje widget
         * @param id ID widgetu, ktorý sa práve upravuje
         */
        fun updateAppWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val cat = NoteCategory[Widgets.getNoteWidgetCategory(id)]
            val catId = if (cat is Subject) cat.id else -(cat as TimeCategory).ordinal.toLong()

            val adapterIntent = Intent(context, NoteWidgetService::class.java)
                .putExtra(EXTRA_APPWIDGET_ID, id)
                .putExtra(TARGET_CATEGORY, catId)
                .also { it.data = Uri.parse(it.toUri(Intent.URI_INTENT_SCHEME)) }

            val itemAction = getBroadcast(context, 1,
                Intent(ITEM_ACTION, Uri.parse("note_widget://$id"), context, NoteWidget::class.java)
                    .putExtra(EXTRA_APPWIDGET_ID, id), 0)
            val openPIntent = NOTES.prepare(context, 0, true, Uri.parse("category:/$catId")) {
                putLong(EXTRA_NOTE_CATEGORY, catId)
            }


            val views = RemoteViews(context.packageName, R.layout.note_widget)
            views.setTextViewText(R.id.note_widget_title, when (cat) {
                is Subject -> cat.name
                is TimeCategory -> context.getString(cat.res)
                else -> ""
            })
            views.setOnClickPendingIntent(R.id.note_widget_title, openPIntent)
            views.setRemoteAdapter(R.id.widget_note_list, adapterIntent)
            views.setEmptyView(R.id.widget_note_list, R.id.widget_empty_msg)
            views.setPendingIntentTemplate(R.id.widget_note_list, itemAction)
            manager.updateAppWidget(id, views)
            manager.notifyAppWidgetViewDataChanged(id, R.id.widget_note_list)
        }
    }

    /**
     * Aktualizujú sa všetky widgety so zoznamami úloh a naplánuje sa ďaľšia aktualizácia
     */
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) updateAppWidget(context, manager, id)
        val alarm = context.getSystemService(ALARM_SERVICE) as AlarmManager
        val nextTime = SQLite.notes(TODAY).firstOrNull()?.deadline ?: Calendar.getInstance().nextMidnight
        alarm.setExact(RTC_WAKEUP, nextTime, updatePI(context))
    }

    /**
     * Odstránením widgetu treba aj z pamäte odstrániť nastavenú konfiguráciu o widgete
     * @param context
     * @param appWidgetIds
     */
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) Widgets.setNoteWidgetCategory(appWidgetId, null)
    }

    /**
     * Odstránením posledného widgetu odvolám naplánovanú aktualizáciu widgetov
     * @param context
     */
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.getSystemService(AlarmManager::class.java).cancel(updatePI(context))
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return super.onReceive(context, intent)
        if (intent?.action == ITEM_ACTION) {
            val widget = intent.getIntExtra(EXTRA_APPWIDGET_ID, 0)
            val itemAction = intent.getIntExtra(NoteWidgetService.NOTE_ACTION, 0)
            val item = intent.getLongExtra(NOTE, -1L)
            when (itemAction) {
                NoteWidgetService.REMOVAL -> unsetNote(context, SQLite.note(item) ?: return)
                NoteWidgetService.REDIRECTION -> NOTES.redirect(context, true) {
                    putLong(EXTRA_NOTE_CATEGORY, Widgets.getNoteWidgetCategory(widget))
                    putLong(EXTRA_NOTE_ID, item)
                    App.log(get(EXTRA_NOTE_CATEGORY))
                    App.log(get(EXTRA_NOTE_ID))
                }
            }
        }
        else super.onReceive(context, intent)
    }
}