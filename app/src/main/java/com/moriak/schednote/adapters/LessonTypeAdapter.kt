package com.moriak.schednote.adapters

import android.os.Bundle
import android.text.Editable
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import com.moriak.schednote.Palette
import com.moriak.schednote.R
import com.moriak.schednote.TextCursorTracer
import com.moriak.schednote.data.LessonType
import kotlinx.android.synthetic.main.edit_tools.view.*
import kotlinx.android.synthetic.main.lesson_type_data.view.*


/**
 * Adaptér spravuje zoznam kategórii vyučovacích hodín.
 */
class LessonTypeAdapter : CustomAdapter<LessonType?>(R.layout.lesson_type_data) {
    /**
     * @property MAX_COUNT Maximálny počet typov hodín
     * @property ID Kľúč pre ID upravovanej ketegórie vyučovvacích hodín
     * @property NAME Kľúč pre názov upravovanej ketegórie vyučovvacích hodín
     * @property CURSOR_START Kľúč pre začiatok kurzora upravovaného textu
     * @property CURSOR_END Kľúč pre začiatok kurzora upravovaného textu
     * @property ACTION_EDIT Označenie pokusu o zahájenie úpravy kategórie vyučovvacích hodín
     * @property ACTION_SAVE Označenie pokusu o uloženie zmien kategórie vyučovvacích hodín
     * @property ACTION_DELETE Označenie pokusu o odstránenie kategórie vyučovvacích hodín
     */
    companion object {
        const val MAX_COUNT = 5
        const val ID = "LT_ID"
        const val NAME = "LT_NAME"
        const val COLOR = "LT_COLOR"
        const val CURSOR_START = "LT_CURSOR_START"
        const val CURSOR_END = "LT_CURSOR_END"
        const val ACTION_EDIT = 1995
        const val ACTION_SAVE = 1996
        const val ACTION_DELETE = 1997
    }

    private val clickAction = View.OnClickListener {
        val h = it.tag as LessonTypesHolder
        triggerItemAction(h.adapterPosition, extras, when (it.id) {
            R.id.lt_readable, R.id.edit -> ACTION_EDIT
            R.id.save -> ACTION_SAVE
            R.id.delete -> ACTION_DELETE
            else -> 0
        })
    }

    private val onRewrite = object: TextCursorTracer("^[a-zA-ZÀ-ž0-9][a-zA-ZÀ-ž0-9 ]{0,23}$".toRegex()) {
        private val helpRgx = "^([^a-zA-ZÀ-ž0-9]*)([a-zA-ZÀ-ž0-9 ]{0,24})(.*)$".toRegex()
        override fun onCursorChanged(range: IntRange) {
            extras.putInt(CURSOR_START, range.first)
            extras.putInt(CURSOR_END, range.last)
        }
        override fun afterValidTextChange(s: Editable) = extras.putString(NAME, s.toString())
        override fun afterInvalidTextChange(s: Editable) {
            if (action == TextAction.ADDED || action == TextAction.REPLACED) s.delete(st, en)
            else if (st == 0) s.delete(0, s.replace(helpRgx, "$1").length)
            if (!s.matches(format!!)) s.replace(0, s.length, s.replace(helpRgx, "$2"))
        }
    }

    override fun compare(a: LessonType?, b: LessonType?): Int {
        var cmp = (a == null).compareTo(b == null)
        if (a != null && b != null) {
            cmp = a.name.compareTo(b.name)
            if (cmp == 0) a.id.compareTo(b.id)
        }
        return cmp
    }
    override fun instantiateViewHolder(v: View): CustomViewHolder = LessonTypesHolder(v)
    override fun bundleToItem(bundle: Bundle): LessonType? = when (val id = bundle.getInt(ID, -1)) {
        -1 -> null
        else -> {
            val name = bundle.getString(NAME, "")
            val color = Palette(bundle.getInt(COLOR))
            LessonType(id, name, color)
        }
    }

    override fun itemToBundle(item: LessonType?, bundle: Bundle) {
        bundle.putInt(ID, item?.id ?: -1)
        bundle.putString(NAME, item?.name ?: "")
        bundle.putInt(COLOR, item?.color?.color ?: 0)
    }

    /**
     * Objekt vizualizuje položku zoznamu (kategóriu vyučovacích hodín)
     */
    inner class LessonTypesHolder(v: View): CustomViewHolder(v) {
        override fun bind(pos: Int) {
            val palette = item?.color ?: Palette()
            itemView.line.background.setTint(palette.color)
            itemView.lt_readable.setTextColor(palette.contrast)
            itemView.lt_editable.setTextColor(palette.contrast)
            itemView.edit.drawable.setTint(palette.contrast)
            itemView.save.drawable.setTint(palette.contrast)
            itemView.delete.drawable.setTint(palette.contrast)

            val editing = extras.getInt(ID, -1) == (item?.id ?: -1)
            itemView.lt_readable.visibility = if (editing) GONE else VISIBLE
            itemView.lt_editable.visibility = if (editing) VISIBLE else GONE
            itemView.edit.visibility = if (editing) GONE else VISIBLE
            itemView.save.visibility = if (editing) VISIBLE else GONE
            itemView.lt_editable.addTextChangedListener(onRewrite)
            if (editing) {
                val st = extras.getInt(CURSOR_START)
                val en = extras.getInt(CURSOR_END)
                itemView.lt_editable.setText(extras.getString(NAME))
                itemView.lt_editable.setSelection(st, en)
                itemView.lt_editable.requestFocus()
            } else itemView.lt_readable.text = item?.name
            itemView.lt_readable.tag = this
            itemView.cancel.visibility = GONE
            itemView.edit.tag = this
            itemView.save. tag = this
            itemView.delete.tag = this
            itemView.lt_readable.setOnClickListener(clickAction)
            itemView.edit.setOnClickListener(clickAction)
            itemView.save.setOnClickListener(clickAction)
            itemView.delete.setOnClickListener(clickAction)
        }
    }
}