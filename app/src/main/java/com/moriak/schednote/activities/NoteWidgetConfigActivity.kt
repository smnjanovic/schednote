package com.moriak.schednote.activities

import android.R.layout.simple_list_item_1
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID
import android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import com.moriak.schednote.databinding.NoteWidgetConfigureBinding
import com.moriak.schednote.enums.TimeCategory
import com.moriak.schednote.interfaces.NoteCategory
import com.moriak.schednote.storage.Prefs.Widgets.setNoteWidgetCategory
import com.moriak.schednote.storage.SQLite
import com.moriak.schednote.widgets.NoteWidget
import com.moriak.schednote.widgets.NoteWidget.Companion.updateAppWidget

/**
 * Aktivita slúži na nastavenie konfigurácie vznikajúceho widgetu [NoteWidget].
 */
class NoteWidgetConfigActivity : CustomBoundActivity<NoteWidgetConfigureBinding>() {
    private data class Item(val cat: NoteCategory, val description: String) {
        override fun toString(): String = description
    }

    private var appWidgetId = INVALID_APPWIDGET_ID
    private var confirm = View.OnClickListener {
        setNoteWidgetCategory(appWidgetId, (binding.categoryChoice.selectedItem as Item).cat.id)
        updateAppWidget(this, AppWidgetManager.getInstance(this), appWidgetId)
        setResult(RESULT_OK, Intent().putExtra(EXTRA_APPWIDGET_ID, appWidgetId))
        finish()
    }

    override fun onCreateBinding() = NoteWidgetConfigureBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        setResult(RESULT_CANCELED)
        appWidgetId = intent.getIntExtra(EXTRA_APPWIDGET_ID, INVALID_APPWIDGET_ID)
        if (appWidgetId == INVALID_APPWIDGET_ID) return finish()
        super.onCreate(savedInstanceState)

        val items = ArrayList<Item>()
        items.addAll(TimeCategory.values().map { Item(it, getString(it.res)) })
        items.addAll(SQLite.subjects().map { Item(it, it.toString()) })
        binding.categoryChoice.adapter = ArrayAdapter(this, simple_list_item_1, items)
        binding.categoryConfirm.setOnClickListener(confirm)
    }
}