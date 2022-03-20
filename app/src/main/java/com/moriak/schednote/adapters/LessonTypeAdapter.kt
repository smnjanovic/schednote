package com.moriak.schednote.adapters

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.moriak.schednote.Palette
import com.moriak.schednote.R
import com.moriak.schednote.TextCursorTracer
import com.moriak.schednote.data.LessonType
import com.moriak.schednote.databinding.LessonTypeDataBinding

/**
 * Adaptér spravuje zoznam kategórii vyučovacích hodín.
 */
class LessonTypeAdapter : CustomAdapter<LessonType?, LessonTypeDataBinding>() {
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return LessonTypesHolder(LessonTypeDataBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }
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
    inner class LessonTypesHolder(b: LessonTypeDataBinding): CustomViewHolder(b) {
        override fun bind(pos: Int) {
            val palette = item?.color ?: Palette()
            binding.line.background.setTint(palette.color)
            binding.ltReadable.setTextColor(palette.contrast)
            binding.ltEditable.setTextColor(palette.contrast)
            binding.ltTools.edit.drawable.setTint(palette.contrast)
            binding.ltTools.save.drawable.setTint(palette.contrast)
            binding.ltTools.delete.drawable.setTint(palette.contrast)

            val editing = extras.getInt(ID, -1) == (item?.id ?: -1)
            binding.ltReadable.visibility = if (editing) GONE else VISIBLE
            binding.ltEditable.visibility = if (editing) VISIBLE else GONE
            binding.ltTools.edit.visibility = if (editing) GONE else VISIBLE
            binding.ltTools.save.visibility = if (editing) VISIBLE else GONE
            binding.ltEditable.addTextChangedListener(onRewrite)
            if (editing) {
                val st = extras.getInt(CURSOR_START)
                val en = extras.getInt(CURSOR_END)
                binding.ltEditable.setText(extras.getString(NAME))
                binding.ltEditable.setSelection(st, en)
                binding.ltEditable.requestFocus()
            } else binding.ltReadable.text = item?.name
            binding.ltReadable.tag = this
            binding.ltTools.cancel.visibility = GONE
            binding.ltTools.edit.tag = this
            binding.ltTools.save. tag = this
            binding.ltTools.delete.tag = this
            binding.ltReadable.setOnClickListener(clickAction)
            binding.ltTools.edit.setOnClickListener(clickAction)
            binding.ltTools.save.setOnClickListener(clickAction)
            binding.ltTools.delete.setOnClickListener(clickAction)
        }
    }
}