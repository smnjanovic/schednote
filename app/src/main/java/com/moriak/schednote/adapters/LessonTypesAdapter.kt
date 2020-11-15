package com.moriak.schednote.adapters

import android.content.Context
import android.text.Editable
import android.text.Selection
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.database.data.LessonType
import kotlinx.android.synthetic.main.lesson_type_data.view.*
import java.util.*


/**
 * Adapter umožňuje spravovať zoznam typov hodín.
 */
class LessonTypesAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private val itemToAdd = LessonType(-1, "")
    }

    private val items = App.data.lessonTypes().apply { add(itemToAdd) }

    private var activeHolder: LessonTypeHolder? = null
    private var editId: Int = -1
    private var editName: String? = null
    private var cursorStart: Int = 0
    private var cursorEnd: Int = 0

    private val focusSwitch = View.OnFocusChangeListener { v, hasFocus ->
        if (v is EditText) {
            val imm: InputMethodManager =
                App.ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            if (!hasFocus) imm.hideSoftInputFromWindow(v.windowToken, 0)
            else imm.showSoftInput(v, 0)
        }
    }
    private val onEditStart = View.OnClickListener { v ->
        finishEditing()
        val holder = v.tag as LessonTypeHolder
        // ak zacinam upravovat nieco ine odstranim pridavaci view holder
        if (items.lastIndex != holder.adapterPosition && items.last().id == -1) {
            items.removeAt(items.lastIndex)
            notifyItemRemoved(items.size)
        }
        holder.startEditing()
    }
    private val onEdit = object : TextWatcher {
        private var st = 0
        private var en = 0
        override fun afterTextChanged(s: Editable?) {
            if (s != null) {
                //oprava najnovsej chyby
                if (s.length > LessonType.limit) s.delete(st, en)
                if (s.contains(LessonType.invalidName)) s.delete(st, en)
                //oprava pozostalych chyb
                if (s.contains(LessonType.invalidName)) s.replace(0, s.length, s
                    .replace("([^a-zA-ZÀ-ž0-9 ]+)".toRegex(), "")
                    .replace("[ ][ ]+".toRegex(), " ")
                    .replace("^[0-9 ]+".toRegex(), "")
                    .let {
                        if (it.length > LessonType.limit) it.substring(
                            0,
                            LessonType.limit
                        ) else it
                    })
                editName = s.toString()
                cursorStart = Selection.getSelectionStart(s)
                cursorEnd = Selection.getSelectionStart(s)
            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            st = start
            en = start + count
        }
    }

    private val onCursorChange = View.OnTouchListener { v, motion ->
        if (v is EditText && motion.action == MotionEvent.ACTION_UP) {
            v.post {
                cursorStart = Selection.getSelectionStart(v.text)
                cursorEnd = Selection.getSelectionEnd(v.text)
            }
        }
        false
    }
    private val onEditEnd = View.OnClickListener { v ->
        (v.tag as LessonTypeHolder).save()?.let { App.toast(it) } ?: let {
            if (items.size < LessonType.MAX_COUNT && items.last().id != -1) {
                items.add(itemToAdd)
                notifyItemInserted(items.lastIndex)
            }
        }
    }
    private val onDelete = View.OnClickListener {
        (it.tag as LessonTypeHolder).delete()
        if (items.size < LessonType.MAX_COUNT && items.lastOrNull()?.id != -1) {
            items.add(itemToAdd)
            notifyItemInserted(items.lastIndex)
        }
    }

    private fun notifyEditing(holder: LessonTypeHolder?) {
        activeHolder = holder?.let { if (it.adapterPosition in items.indices) it else null }
        val item = holder?.adapterPosition?.let { items[it] }
        editId = item?.id ?: -1
        editName = item?.name
        cursorStart = item?.name?.length ?: 0
        cursorEnd = cursorStart
    }

    private fun finishEditing() {
        activeHolder?.save()?.let {
            App.toast(it)
            activeHolder!!.stopEditing()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonTypeHolder =
        LessonTypeHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.lesson_type_data, parent, false)
        )

    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
        (holder as LessonTypeHolder).bind()

    inner class LessonTypeHolder(view: View) : RecyclerView.ViewHolder(view) {
        private var editMode: Boolean = false
            set(value) {
                itemView.type_name.visibility = if (value) GONE else VISIBLE
                itemView.type_name_container.visibility = if (value) VISIBLE else GONE
                itemView.edit.visibility = if (value) GONE else VISIBLE
                itemView.save.visibility = if (value) VISIBLE else GONE
                field = value
            }

        /**
         * Vyskladanie view holdera
         */
        fun bind() {
            val item = items[adapterPosition]

            editMode = item.id == editId
            if (editMode) {
                activeHolder = this
                itemView.type_name_editable.setText(editName ?: item.name)
                itemView.type_name_editable.requestFocus()
                itemView.type_name_editable.text?.let {
                    val rng = 0..it.length
                    val st = cursorStart.coerceIn(rng)
                    val en = cursorEnd.coerceIn(rng)
                    Selection.setSelection(it, st, en)
                }
            } else itemView.type_name.text = item.name

            itemView.type_name.tag = this
            itemView.edit.tag = this
            itemView.save.tag = this
            itemView.delete.tag = this

            itemView.type_name_editable.onFocusChangeListener = focusSwitch
            itemView.type_name_editable.addTextChangedListener(onEdit)
            itemView.type_name_editable.setOnTouchListener(onCursorChange)
            itemView.type_name.setOnClickListener(onEditStart)
            itemView.edit.setOnClickListener(onEditStart)
            itemView.save.setOnClickListener(onEditEnd)
            itemView.delete.setOnClickListener(onDelete)
        }

        /**
         * Pokus o uloženie vykonaných zmien
         * @return pri úspešnom uložení vráti null, inak vráti id textového zdroja popisujúceho konkrétny problém
         */
        fun save(): Int? {
            if (editMode && adapterPosition in items.indices) {
                val isNew = editId == -1
                if (LessonType.isNameValid(itemView.type_name_editable.text)) {
                    val loc = Locale.getDefault()
                    val text =
                        itemView.type_name_editable.text?.toString()?.trim()?.toLowerCase(loc) ?: ""
                    val twin =
                        items.find { it.id != editId && it.name.trim().toLowerCase(loc) == text }

                    val id = when {
                        twin != null -> return if (isNew) R.string.lesson_type_exists
                        else {
                            App.data.joinLessonTypes(twin.id, editId)
                            delete()
                            null
                        }
                        isNew -> App.data.insertLessonType(text)
                        else -> App.data.renameLessonType(editId, text)
                            .let { if (it == 1) editId else -1 }
                    }
                    if (id > -1) {
                        notifyEditing(null)
                        val oldPos = adapterPosition
                        items[oldPos] = App.data.lessonType(id)!!
                        notifyItemChanged(oldPos)

                        // uprava poradia
                        val newPos = App.data.lessonTypeIndex(id)
                        items.add(newPos, items.removeAt(oldPos))
                        notifyItemMoved(oldPos, newPos)
                    }
                } else return if (isNew) R.string.lesson_type_insert_error else R.string.lesson_type_update_error
            }
            return null
        }

        /**
         * Začnem premenovávať typ hodiny
         */
        fun startEditing() {
            if (adapterPosition in items.indices) {
                editMode = true
                itemView.type_name_editable.setText(items[adapterPosition].name)
                itemView.type_name_editable.requestFocus()
                itemView.type_name_editable.text?.let {
                    Selection.setSelection(
                        it,
                        it.length,
                        it.length
                    )
                }
                notifyEditing(this)
            }
        }

        /**
         * Ukončím úpravu (bez uloženia).
         */
        fun stopEditing() {
            itemView.type_name_editable.clearFocus()
            if (adapterPosition in items.indices) {
                editMode = false
                itemView.type_name.text = items[adapterPosition].name
                notifyEditing(null)
            }
        }

        /**
         * Odstránim úlohu na ktorú sa viaže tento viewHolder
         */
        fun delete() {
            itemView.type_name_editable.clearFocus()
            val oldPos = adapterPosition
            if (oldPos in items.indices) {
                val removed = items.removeAt(oldPos).id
                App.data.deleteLessonType(removed)
                notifyItemRemoved(oldPos)
                if (editId == removed) notifyEditing(null)
            }
        }
    }
}