package com.moriak.schednote.widgets

import android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID
import android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.moriak.schednote.R
import com.moriak.schednote.data.Note
import com.moriak.schednote.data.Subject
import com.moriak.schednote.enums.TimeCategory
import com.moriak.schednote.interfaces.NoteCategory
import com.moriak.schednote.storage.Prefs
import com.moriak.schednote.storage.Prefs.Settings.dateFormat
import com.moriak.schednote.storage.Prefs.Settings.timeFormat
import com.moriak.schednote.storage.SQLite
import com.moriak.schednote.widgets.NoteWidgetService.NoteWidgetFactory

/**
 * Trieda pristupuje k vzdialenému Adaptéru [NoteWidgetFactory].
 */
class NoteWidgetService : RemoteViewsService() {
    companion object {
        const val NOTE_ACTION = "ITEM_ACTION"
        const val NOTE = "ITEM"
        const val REMOVAL = 1
        const val REDIRECTION = 2
    }
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = NoteWidgetFactory(applicationContext, intent)

    /**
     * Trieda ktora nacitava úlohy
     * @property context Kontext - nesmie byť null
     * @param intent Intent - nesmie byť null
     */
    class NoteWidgetFactory(private val context: Context, intent: Intent) : RemoteViewsFactory {
        private val wid = intent.getIntExtra(EXTRA_APPWIDGET_ID, INVALID_APPWIDGET_ID)
        private val category = NoteCategory[Prefs.Widgets.getNoteWidgetCategory(wid)]
        private val items: ArrayList<Note> = ArrayList()

        override fun getViewAt(pos: Int): RemoteViews = RemoteViews(
            context.packageName,
            R.layout.delete_only_note
        ).apply {
            //vyplnenie obsahu
            setTextViewText(R.id.note_category, items[pos].sub.abb)
            setTextViewText(R.id.note_deadline, items[pos].deadline?.let{
                dateFormat.getFormat(it) + " " + timeFormat.getFormat(it)
            } ?: "")
            setTextViewText(R.id.note_description, items[pos].info)
            //odstranovaci intent
            setOnClickFillInIntent(R.id.note_removal, Intent().putExtra(NOTE_ACTION, REMOVAL).putExtra(NOTE, items[pos].id))
            //presuvaci intent
            setOnClickFillInIntent(R.id.note_description, Intent().putExtra(NOTE_ACTION, REDIRECTION).putExtra(NOTE, items[pos].id))
        }

        override fun onCreate() {}
        override fun getLoadingView(): RemoteViews? = null
        override fun getItemId(position: Int) = items[position].id
        override fun onDataSetChanged() {
            items.clear()
            var notes: List<Note> = SQLite.notes(category)
            if (category is Subject) notes = notes.filter { note ->
                note.deadline?.let { it <= System.currentTimeMillis() } == true
            }
            items.addAll(notes)
        }

        override fun hasStableIds() = true
        override fun getCount() = items.size
        override fun getViewTypeCount() = 1
        override fun onDestroy() {}
    }
}
