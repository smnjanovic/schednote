package com.moriak.schednote.widgets

import android.app.AlarmManager
import android.app.AlarmManager.RTC_WAKEUP
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.adapters.NoteWidgetService
import com.moriak.schednote.database.data.Subject
import com.moriak.schednote.other.Redirection
import com.moriak.schednote.other.TimeCategory
import com.moriak.schednote.settings.Prefs

/**
 * Trieda spravuje widget so zoznamom úloh
 */
class NoteWidget : AppWidgetProvider() {
    /**
     * @property REMOVE_NOTE Akcia pre intent, ktorý vymaže úlohu cez widget
     */
    companion object {
        const val REMOVE_NOTE = "NOTE_REMOVAL"
        private const val ACTION_REMOVAL = "ACTION_REMOVAL"
        private const val TARGET_CATEGORY = "TARGET_CATEGORY"
        private const val widgetUpdateRequest = -2000

        private fun all(context: Context = App.ctx) = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, NoteWidget::class.java)) ?: intArrayOf()

        /**
         * Aktualizovať všetky widgety, ktoré sú k dispozícii
         *
         * @param context nepovinný údaj
         */
        fun update(context: Context = App.ctx) = context.sendBroadcast(
            Intent(App.ctx, NoteWidget::class.java)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, all(context))
        )

        private fun scheduledUpdate(context: Context = App.ctx): PendingIntent {
            val intent = Intent(App.ctx, NoteWidget::class.java)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, all(context))
            return PendingIntent.getBroadcast(context, widgetUpdateRequest, intent, 0)
        }

        /**
         * Nastavenie času nasledujúcej aktualizácie widgetov. Buď je to nasledujúca polnoc
         * alebo počiatočný termín úlohy ktorej čas práve vypršal
         *
         * @param context nepovinný údaj
         */
        fun setNextUpdateTime(context: Context = App.ctx) {
            val pendingIntent = scheduledUpdate(context)
            val alarm = App.ctx.getSystemService(ALARM_SERVICE) as AlarmManager
            val nextTime = App.data.scheduleNextWidgetUpdate()
            alarm.setExact(RTC_WAKEUP, nextTime, pendingIntent)
        }

        private fun disableNextUpdateTime(context: Context = App.ctx) =
            (context.getSystemService(ALARM_SERVICE) as AlarmManager).cancel(scheduledUpdate(context))

        /**
         * Aktualizuje sa konkrétny widget
         *
         * @param context Context pre potreby práce s layoutom widgetu
         * @param manager Manážer, spravuje widget
         * @param id ID widgetu, ktorý sa práve upravuje
         */
        fun updateAppWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val cat = Prefs.widgets.getNoteWidgetCategory(id)
                ?: TimeCategory.ALL.also { Prefs.widgets.setNoteWidgetCategory(id, it) }
            val catId = if (cat is Subject) cat.id else -(cat as TimeCategory).ordinal.toLong()

            val adapterIntent = Intent(context, NoteWidgetService::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra(TARGET_CATEGORY, catId)
                .also { it.data = Uri.parse(it.toUri(Intent.URI_INTENT_SCHEME)) }

            val deleteIntent = PendingIntent.getBroadcast(
                context, 0,
                Intent(context, NoteWidget::class.java).setAction(ACTION_REMOVAL), 0
            )

            val openAppIntent = PendingIntent.getActivity(
                context,
                0,
                Redirection.NOTES.makeIntent(context)
                    .putExtra(Redirection.EXTRA_NOTE_CATEGORY, catId),
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            val views = RemoteViews(context.packageName, R.layout.note_widget)
            views.setTextViewText(
                R.id.note_widget_title,
                if (cat is Subject) cat.name else cat.toString()
            )
            views.setOnClickPendingIntent(R.id.note_widget_title, openAppIntent)
            views.setRemoteAdapter(R.id.widget_note_list, adapterIntent)
            views.setEmptyView(R.id.widget_note_list, R.id.widget_empty_msg)
            views.setPendingIntentTemplate(R.id.widget_note_list, deleteIntent)
            manager.updateAppWidget(id, views)
            manager.notifyAppWidgetViewDataChanged(id, R.id.widget_note_list)
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) updateAppWidget(context, manager, id)
        setNextUpdateTime(context)
    }

    /**
     * Odstránením widgetu treba aj z pamäte odstrániť nastavenú konfiguráciu o widgete
     * @param context
     * @param appWidgetIds
     */
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds)
            Prefs.widgets.setNoteWidgetCategory(appWidgetId, null)
    }

    /**
     * Odstránením posledného widgetu odvolám naplánovanú aktualizáciu widgetov
     * @param context
     */
    override fun onDisabled(context: Context?) {
        super.onDisabled(context)
        disableNextUpdateTime(context ?: App.ctx)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            ACTION_REMOVAL -> App.data.removeNote(intent.getLongExtra(REMOVE_NOTE, -1L))
            else -> super.onReceive(context, intent)
        }
    }
}