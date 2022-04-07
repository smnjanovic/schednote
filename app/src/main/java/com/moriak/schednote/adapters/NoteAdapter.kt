package com.moriak.schednote.adapters

import android.os.Bundle
import android.text.Editable
import android.text.Selection
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.moriak.schednote.R
import com.moriak.schednote.TextCursorTracer
import com.moriak.schednote.data.Note
import com.moriak.schednote.data.Subject
import com.moriak.schednote.databinding.NoteItemBinding
import com.moriak.schednote.storage.Prefs.Settings.dateFormat
import com.moriak.schednote.storage.Prefs.Settings.timeFormat

/**
 * Adapter spravuje zoznam úloh
 */
class NoteAdapter: CustomAdapter<Note?, NoteItemBinding>() {
    /**
     * @property ID kľúč pre uchovanie ID upravovanej úlohy
     * @property SUB_ID kľúč pre uchovanie ID predmetu, ku ktorému sa upravovaná úloha viaže
     * @property SUB_ABB kľúč pre uchovanie skratky predmetu, ku ktorému sa upravovaná úloha viaže
     * @property SUB_NAME kľúč pre uchovanie názvu predmetu, ku ktorému sa upravovaná úloha viaže
     * @property INFO kľúč pre uchovanie popisu upravovanej úlohy
     * @property WHEN kľúč pre uchovanie termínu upravovanej úlohy
     * @property CURSOR_START kľúč pre uchovanie začiatku kurzora upravovaného textu
     * @property CURSOR_END kľúč pre uchovanie konca kurzora upravovaného textu
     * @property ACTION_DELETE Označenie pre pokus o odstránenie úlohy
     * @property ACTION_EDIT_START Označanie pre pokus o zahájenie zmeny úlohy
     * @property ACTION_EDIT_STOP Označanie pre pokus o vrátenie zmien úlohy
     * @property ACTION_EDIT_SAVE Označanie pre pokus o uloženie zmien v úlohe
     * @property ACTION_SELECT_SUBJECT Označanie pre pokus o zmenu predmetu, ku ktorému sa úloha viaže
     * @property ACTION_SELECT_DEADLINE Označanie pre pokus o zmenu termínu úlohy
     */
    companion object {
        const val ID = "ID"
        const val SUB_ID = "SUB_ID"
        const val SUB_ABB = "SUB_ABB"
        const val SUB_NAME = "SUB_NAME"
        const val INFO = "INFO"
        const val WHEN = "WHEN"
        const val CURSOR_START = "CURSOR_START"
        const val CURSOR_END = "CURSOR_END"
        const val ACTION_DELETE = 2000
        const val ACTION_EDIT_START = 2001
        const val ACTION_EDIT_STOP = 2002
        const val ACTION_EDIT_SAVE = 2003
        const val ACTION_SELECT_SUBJECT = 2004
        const val ACTION_SELECT_DEADLINE = 2005
    }

    private val clickAction: View.OnClickListener = View.OnClickListener {
        triggerItemAction((it.tag as NoteHolder).adapterPosition, extras, when(it.id) {
            R.id.ni_select_date -> ACTION_SELECT_DEADLINE
            R.id.ni_select_sub -> ACTION_SELECT_SUBJECT
            R.id.edit -> ACTION_EDIT_START
            R.id.cancel -> ACTION_EDIT_STOP
            R.id.save -> ACTION_EDIT_SAVE
            R.id.delete -> ACTION_DELETE
            else -> 0
        })
    }

    private val onRewrite = object : TextCursorTracer("^[^\\s](\\s|.){0,255}$".toRegex()) {
        private val lTrimRgx = "^([\\s]*)(.|\\s)*$".toRegex()
        override fun onCursorChanged(range: IntRange) {
            extras.putInt(CURSOR_START, range.first)
            extras.putInt(CURSOR_END, range.last)
        }
        override fun afterValidTextChange(s: Editable) = extras.putString(INFO, "$s")
        override fun afterInvalidTextChange(s: Editable) {
            if (s.isEmpty()) afterValidTextChange(s)
            else when (action) {
                TextAction.ADDED -> s.delete(st, en)
                TextAction.REMOVED -> if (st == 0) s.delete(st, lTrimRgx.replace(s, "$1").length)
                TextAction.REPLACED -> {
                    val left = if (st == 0) s.replace(lTrimRgx, "$1").length else 0
                    val exceed = (s.length - 256 - left).coerceIn(0..en - st)
                    s.replace(st, en, s.substring(st + left, en - exceed))
                }
            }
            // ak je format stale zly, nahradit upravenym textom (ak bude prilis dlhy, predosle prikazy v tejto znovu zavolanej funkcii ho skratia)
            if (s.isNotEmpty() && !s.matches(format!!))
                s.replace(0, s.length, s.replace("\\s+".toRegex(), " ").trim())
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        NoteHolder(NoteItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun compare(a: Note?, b: Note?): Int {
        var cmp: Int = (a == null).compareTo(b == null)
        if (a != null && b != null) {
            cmp = (a.deadline ?: Long.MAX_VALUE).compareTo(b.deadline ?: Long.MAX_VALUE)
            if (cmp == 0) cmp = a.sub.abb.compareTo(b.sub.abb)
            if (cmp == 0) cmp = a.info.compareTo(b.info)
            if (cmp == 0) cmp = a.id.compareTo(b.id)
        }
        return cmp
    }

    override fun bundleToItem(bundle: Bundle): Note? = when(val id = bundle.getLong(ID, -1L)) {
        -1L -> null
        else -> {
            val subId = bundle.get(SUB_ID) as Long? ?: -1L
            val subAbb = bundle.get(SUB_ABB) as String? ?: ""
            val subName = bundle.get(SUB_NAME) as String? ?: ""
            val info = bundle.get(INFO) as String? ?: ""
            val deadline = bundle.get(WHEN) as Long?
            Note(id, Subject(subId, subAbb, subName), info, deadline)
        }
    }

    /**
     * Vytvorí objekt typu Note pre novú položku, ktorá by použítím metódy [bundleToItem] vyšla null.
     * @param bundle dáta pre úlohu.
     * @return úloha
     */
    fun newItemFromBundle(bundle: Bundle): Note {
        val subId = bundle.get(SUB_ID) as Long? ?: -1L
        val subAbb = bundle.get(SUB_ABB) as String? ?: ""
        val subName = bundle.get(SUB_NAME) as String? ?: ""
        val info = bundle.get(INFO) as String? ?: ""
        val deadline = bundle.get(WHEN) as Long?
        return Note(-1L, Subject(subId, subAbb, subName), info, deadline)
    }

    override fun itemToBundle(item: Note?, bundle: Bundle) {
        bundle.putLong(ID, item?.id ?: -1L)
        item?.sub?.let {
            bundle.putLong(SUB_ID, it.id)
            bundle.putString(SUB_ABB, it.abb)
            bundle.putString(SUB_NAME, it.name)
        }
        bundle.putString(INFO, item?.info ?: "")
        item?.deadline?.let { bundle.putLong(WHEN, it) } ?: bundle.remove(WHEN)
    }

    /**
     * Blok v tejto inštancii vizualizuje jednu položku zo zoznamu (úlohu)
     */
    inner class NoteHolder(b: NoteItemBinding): CustomViewHolder(b) {
        override fun bind(pos: Int) {
            val editing = extras.getLong(ID, -1L) == (items[pos]?.id ?: -1L)

            // viditelnost prvkov
            binding.niEditable.visibility = if (editing) VISIBLE else GONE
            binding.niSelectDate.visibility = if (editing) VISIBLE else GONE
            binding.niSelectSub.visibility = if (editing) VISIBLE else GONE
            binding.niTools.cancel.visibility = if (editing) VISIBLE else GONE
            binding.niTools.save.visibility = if (editing) VISIBLE else GONE
            binding.niReadable.visibility = if (editing) GONE else VISIBLE
            binding.niTools.edit.visibility = if (editing) GONE else VISIBLE
            binding.niTools.delete.visibility = if (editing) GONE else VISIBLE

            //značky
            binding.niEditable.tag = this
            binding.niSelectDate.tag = this
            binding.niSelectSub.tag = this
            binding.niTools.cancel.tag = this
            binding.niTools.save.tag = this
            binding.niReadable.tag = this
            binding.niTools.edit.tag = this
            binding.niTools.delete.tag = this

            //udalosti
            binding.niEditable.addTextChangedListener(onRewrite)
            binding.niSelectDate.setOnClickListener(clickAction)
            binding.niSelectSub.setOnClickListener(clickAction)
            binding.niTools.cancel.setOnClickListener(clickAction)
            binding.niTools.save.setOnClickListener(clickAction)
            binding.niReadable.setOnClickListener(clickAction)
            binding.niTools.edit.setOnClickListener(clickAction)
            binding.niTools.delete.setOnClickListener(clickAction)

            binding.niSub.text = (if (editing) extras.getString(SUB_ABB) else item?.sub?.abb) ?: "?"
            val deadline = if (editing) extras.get(WHEN) as Long? else item?.deadline
            binding.niDeadline.text = deadline?.let {
                "${dateFormat.getFormat(it)} ${timeFormat.getFormat(it)}"
            }

            if (editing) {
                binding.niEditable.setText(extras.getString(INFO))
                val st = extras.getInt(CURSOR_START, item?.info?.length ?: 0)
                val en = extras.getInt(CURSOR_END, st)
                Selection.setSelection(binding.niEditable.text, st, en)
                onRewrite.setSpan(binding.niEditable.text)
                binding.niEditable.requestFocus()
            } else binding.niReadable.text = item?.info
        }
    }
}