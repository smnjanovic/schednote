package com.moriak.schednote.activities

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.database.data.NoteCategory
import com.moriak.schednote.other.TimeCategory
import com.moriak.schednote.settings.Prefs
import com.moriak.schednote.widgets.NoteWidget
import kotlinx.android.synthetic.main.note_widget_configure.*

/**
 * Aktivita slúži na nastavenie konfigurácie widgetu [NoteWidget].
 */
class NoteWidgetConfigActivity : Activity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var confirm = View.OnClickListener {
        Prefs.widgets.setNoteWidgetCategory(
            appWidgetId,
            category_choice.selectedItem as NoteCategory
        )
        val appWidgetManager = AppWidgetManager.getInstance(this)
        NoteWidget.updateAppWidget(
            this,
            appWidgetManager,
            appWidgetId
        )
        // vratit rovnaky widget id
        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)
        finish()
    }

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        // prednastaveny vysledok
        setResult(RESULT_CANCELED)
        appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )

        // ak aktivita nezacala pridanim widgetu, bude okamzite ukoncena
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContentView(R.layout.note_widget_configure)
        category_choice.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            ArrayList<NoteCategory>().apply {
                addAll(TimeCategory.values())
                addAll(App.data.subjects())
            })
        category_confirm.setOnClickListener(confirm)
    }
}