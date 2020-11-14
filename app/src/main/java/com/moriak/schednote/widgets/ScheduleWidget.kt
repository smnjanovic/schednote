package com.moriak.schednote.widgets

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.widget.RemoteViews
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.design.ScheduleIllustrator
import com.moriak.schednote.other.Redirection
import com.moriak.schednote.settings.Prefs
import com.moriak.schednote.settings.Regularity
import com.moriak.schednote.settings.WorkWeek
import java.util.*

/**
 * Implementation of App Widget functionality.
 */
class ScheduleWidget : AppWidgetProvider() {
    companion object {
        private const val broadcast = -1000
        private const val redirection = -1001

        private fun newIntent(context: Context): Intent {
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, ScheduleWidget::class.java))
            val intent = Intent(App.ctx, ScheduleWidget::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            return intent
        }

        fun update(context: Context = App.ctx) = context.sendBroadcast(newIntent(context))
        private fun updatePI(context: Context = App.ctx) =
            PendingIntent.getBroadcast(context, broadcast, newIntent(context), 0)!!
    }

    private val cal = Calendar.getInstance()

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val workWeek = Prefs.settings.workWeek
        val nextUpdate = followingWeekend(workWeek, Regularity.currentWeek)
        val reg = Regularity[Regularity.isWeekOdd(nextUpdate)]

        for (id in ids) {
            val views = RemoteViews(context.packageName, R.layout.schedule_widget)
            views.setImageViewBitmap(
                R.id.table_image,
                ScheduleIllustrator.drawSchedule(workWeek, reg)
            )
            views.setOnClickPendingIntent(
                R.id.table_image, PendingIntent.getActivity(
                    context,
                    redirection - id,
                    Redirection.TIME_SCHEDULE.makeIntent(context),
                    FLAG_UPDATE_CURRENT
                )
            )
            manager.updateAppWidget(id, views)
        }

        val alarm = context.getSystemService(ALARM_SERVICE) as AlarmManager
        val pi = updatePI(context)
        if (Prefs.settings.dualWeekSchedule) alarm.setExact(
            AlarmManager.RTC_WAKEUP,
            nextUpdate,
            pi
        ) else alarm.cancel(pi)
    }

    override fun onDisabled(context: Context) =
        (context.getSystemService(ALARM_SERVICE) as AlarmManager).cancel(updatePI(context))

    private fun followingWeekend(ww: WorkWeek, reg: Regularity): Long {
        val lessonRange = App.data.scheduleRange(ww, reg)
        val duration = App.data.scheduleRangeToMinuteRange(lessonRange)?.last
        val lastMinute = duration?.let { it + Prefs.settings.earliestMinute } ?: 24 * 60 - 1
        cal.timeInMillis = System.currentTimeMillis()
        cal.set(Calendar.HOUR_OF_DAY, lastMinute / 60)
        cal.set(Calendar.MINUTE, lastMinute % 60)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val dayDif =
            (ww.days.last().value - cal.get(Calendar.DAY_OF_WEEK)).let { if (it < 1) it + 7 else it }
        if (dayDif < 7 || cal.timeInMillis <= System.currentTimeMillis()) cal.add(
            Calendar.DAY_OF_YEAR,
            dayDif
        )
        return cal.timeInMillis
    }
}