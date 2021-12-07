package com.moriak.schednote.widgets

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Bitmap.Config.ARGB_8888
import android.graphics.Canvas
import android.graphics.Point
import android.os.Build
import android.view.WindowManager
import android.widget.RemoteViews
import com.moriak.schednote.R
import com.moriak.schednote.data.Lesson
import com.moriak.schednote.enums.Redirection
import com.moriak.schednote.enums.Regularity
import com.moriak.schednote.enums.WorkWeek
import com.moriak.schednote.getNextTime
import com.moriak.schednote.getRegularity
import com.moriak.schednote.interfaces.IColorGroup.Companion.getGroups
import com.moriak.schednote.now
import com.moriak.schednote.storage.Prefs.Settings.dualWeekSchedule
import com.moriak.schednote.storage.Prefs.Settings.lessonTimeFormat
import com.moriak.schednote.storage.Prefs.Settings.workWeek
import com.moriak.schednote.storage.SQLite
import com.moriak.schednote.views.ScheduleView
import com.moriak.schednote.views.ScheduleView.ScheduleIllustrator
import java.util.*

/**
 * Úlohou tohto typu widgetu je zobraziť rozvrh.
 *
 * Ak je nastavený 2-týždenný rozvrh, tak začiatkom každého víkendu sa widget s rozvrh aktualizuje
 * aby zobrazoval najaktuálnejší widget
 */
class ScheduleWidget : AppWidgetProvider() {
    companion object {
        private const val broadcast = -1000
        private const val redirection = -1001

        private fun newIntent(context: Context): Intent {
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, ScheduleWidget::class.java))
            val intent = Intent(context, ScheduleWidget::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            return intent
        }

        /**
         * Aktualizuje všetky widgety s rozvrhom
         * @param context
         */
        fun update(context: Context) = context.sendBroadcast(newIntent(context))
        private fun updatePI(context: Context) =
            PendingIntent.getBroadcast(context, broadcast, newIntent(context), 0)!!
    }

    private val cal = Calendar.getInstance()

    private fun makeBitmap(context: Context, i: ScheduleIllustrator, ww: WorkWeek, reg: Regularity): Bitmap {
        i.setWorkWeek(ww)
        getGroups().forEach { i.setTypeColor(it.id, it.color.color, it.color.contrast) }
        i.setColumnFormat(object: ScheduleView.ColumnNameFormat {
            override fun getColumnDescription(col: Int): String = lessonTimeFormat.startFormat(col)
        })

        // pridanie hodin
        val fn = fun(l: Lesson) = i.addLesson(l.day, l.time, l.type, l.sub.abb, l.room)
        SQLite.getLessons(ww, reg).forEach(fn)
        if (reg != Regularity.EVERY) SQLite.getLessons(ww, Regularity.EVERY).forEach(fn)

        val wm = context.getSystemService(WindowManager::class.java)
        i.scaleScheduleByWidth(when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> wm.currentWindowMetrics.bounds.width()
            else -> @Suppress("DEPRECATION") Point().apply(wm.defaultDisplay::getRealSize).x
        })
        val bitmap = Bitmap.createBitmap(i.schW, i.schH, ARGB_8888)
        val canvas = Canvas(bitmap)
        i.drawSchedule(canvas)
        return bitmap
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val openPendingIntent = Redirection.LESSON_SCHEDULE.prepare(context, redirection, true)

        val reg = cal.now.getRegularity(workWeek, dualWeekSchedule)
        val illustrator = ScheduleIllustrator()
        val bitmap = makeBitmap(context, illustrator, workWeek, reg)

        for (id in ids) {
            val views = RemoteViews(context.packageName, R.layout.schedule_widget)
            views.setImageViewBitmap(R.id.table_image, bitmap)
            views.setOnClickPendingIntent(R.id.table_image, openPendingIntent)
            manager.updateAppWidget(id, views)
        }

        val alarm = context.getSystemService(ALARM_SERVICE) as AlarmManager
        val pi = updatePI(context)
        if (!dualWeekSchedule) alarm.cancel(pi)
        else {
            val ms = cal.now.getNextTime(workWeek.weekend.first(), Regularity.EVERY, workWeek, 0)
            alarm.setExact(AlarmManager.RTC_WAKEUP, ms, pi)
        }
    }

    /**
     * Po odstránení posledného widgetu odvolať naplánovanú aktualizáciu widgetov
     * @param context
     */
    override fun onDisabled(context: Context) =
        (context.getSystemService(ALARM_SERVICE) as AlarmManager).cancel(updatePI(context))
}