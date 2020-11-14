package com.moriak.schednote.adapters

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.database.data.Note
import com.moriak.schednote.other.TimeCategory
import com.moriak.schednote.settings.Prefs
import com.moriak.schednote.widgets.NoteWidget

/**
 * Trieda pristupuje k vzdialenemu Adapteru pod krycim nazvom RemoteViewsFactory
 */
class NoteWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        NoteWidgetFactory(applicationContext, intent)

    /**
     * Trieda ktora nacitava úlohy
     * @property context Kontext - nesmie byť null
     * @param intent Intent - nesmie byť null
     */
    class NoteWidgetFactory(private val context: Context, intent: Intent) : RemoteViewsFactory {
        private val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        private val category = Prefs.widgets.getNoteWidgetCategory(appWidgetId)
        private val items: ArrayList<Note> = ArrayList()

        override fun getViewAt(pos: Int): RemoteViews {
            return RemoteViews(context.packageName, R.layout.delete_only_note).also { remote ->
                //vyplnenie obsahu
                remote.setTextViewText(R.id.note_category, items[pos].sub.abb)
                remote.setTextViewText(
                    R.id.note_deadline,
                    items[pos].deadline?.let { Prefs.settings.getDateTimeString(it) } ?: "")
                remote.setTextViewText(R.id.note_description, items[pos].description)
                //odstranovaci intent
                remote.setOnClickFillInIntent(
                    R.id.note_removal, Intent().putExtra(
                        NoteWidget.REMOVE_NOTE, items[pos].id
                    )
                )
            }
        }

        override fun onCreate() {}
        override fun getLoadingView(): RemoteViews? = null
        override fun getItemId(position: Int) = items[position].id
        override fun onDataSetChanged() {
            items.clear()
            items.addAll(App.data.notes(category ?: TimeCategory.ALL))
        }

        override fun hasStableIds() = true
        override fun getCount() = items.size
        override fun getViewTypeCount() = 1
        override fun onDestroy() {}
    }
}
