package com.moriak.schednote.adapters

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.database.data.Note
import com.moriak.schednote.database.data.NoteCategory
import com.moriak.schednote.database.data.Subject
import com.moriak.schednote.settings.Prefs
import kotlinx.android.synthetic.main.note.view.*
import java.util.*

class NoteAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val subjects = App.data.subjects()
    private val items = ArrayList<Note?>()
    private lateinit var category: NoteCategory

    private var editPos: Int? = null
    private var editId: Long? = null
    private var editSub: Subject? = null
        set(value) {
            App.log("editSub changed from $editSub to $value")
            field = value
        }
    private var editInfo: String? = null
    private var editDeadline: Long? = null

    /**
     * Položka je aktívna, keď je upravovaná.
     * Keď je položka nová, musí mať rozrobené zmeny
     */
    val activePosition
        get() = if (editId?.let { it > -1L } != false) editPos
        else (editInfo ?: editDeadline)?.let { editPos }
    private var activeHolder: NoteHolder? = null

    private fun keepChanges(holder: NoteHolder) {
        editPos = holder.adapterPosition
        editId = holder.id
        editInfo = holder.info
        editSub = holder.subject
        editDeadline = holder.millis
    }

    private lateinit var onDateTimeSetAttempt: (Int, Long?) -> Unit
    private lateinit var findViewHolder: (Int) -> NoteHolder

    private val nextSubject = View.OnClickListener {
        val holder = it.tag as NoteHolder
        val index = subjects.indexOf(holder.subject)
        holder.subject = subjects[if (index + 1 < subjects.size) index + 1 else 0]
    }

    private val previousSubject = View.OnClickListener {
        val holder = it.tag as NoteHolder
        val index = subjects.indexOf(holder.subject)
        holder.subject = subjects[if (index - 1 >= 0) index - 1 else subjects.lastIndex]
    }

    private val onSetCalendar = View.OnClickListener { v ->
        val holder = when (v.tag) {
            is NoteHolder -> v.tag as NoteHolder
            else -> (v.parent as ViewGroup).set_date.tag as NoteHolder
        }
        onDateTimeSetAttempt(holder.adapterPosition, holder.millis)
    }

    private val onUnsetCalendar = View.OnClickListener {
        val holder = it.tag as NoteHolder
        holder.millis = null
        if (holder.id > -1L && !holder.isEditing) App.data.changeNoteDeadline(holder.id, null)
    }

    private val onEditStart = View.OnClickListener { v ->
        clearChanges()
        activeHolder?.let { holder ->
            val err = holder.save()
            if (err != null) {
                if (holder.id > -1L) App.toast(err)
                holder.stopEditing()
            } else removeOutcast(holder.adapterPosition)?.let { App.toast(it) }
        }
        val holder = v.tag as NoteHolder
        holder.startEditing()
        activeHolder = holder
    }

    private val onRewrite = object : TextWatcher {
        private var st = 0
        private var en = 0
        override fun afterTextChanged(s: Editable?) {
            if (s?.contains(Note.validFormat) == false) s.delete(st, en)
            if (s?.contains(Note.validFormat) == false) s.clear()
            editInfo = s?.toString()
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            st = start
            en = start + count
        }
    }

    private val onEditEnd = View.OnClickListener { v ->
        val h = v.tag as NoteHolder
        h.save()?.let { App.toast(it) } ?: let {
            removeOutcast(h.adapterPosition)?.let { App.toast(it) }
            addNewHolder()
        }
    }
    private val onRemove = View.OnClickListener { v ->
        val holder = v.tag as NoteHolder
        val pos = holder.adapterPosition
        if (editPos == pos) {
            clearChanges()
            activeHolder = null
        }
        App.data.removeNote(holder.id)
        items.removeAt(pos)
        notifyItemRemoved(pos)
        addNewHolder()
    }

    fun setOnFindViewHolder(fn: (Int) -> NoteHolder) {
        findViewHolder = fn
    }

    fun setOnDateTimeSetAttempt(fn: (Int, Long?) -> Unit) {
        onDateTimeSetAttempt = fn
    }

    fun removeOutcast(position: Int): Int? {
        if (position !in items.indices || editPos == position) return null
        val id = items[position]?.id ?: -1L
        if (id > -1L && !App.data.noteBelongsToCategory(items[position]!!.id, category)) {
            items.removeAt(position)
            notifyItemRemoved(position)
            return R.string.outcast_note
        }
        return null
    }

    private fun addNewHolder() {
        if ((items.isEmpty() || items.last() != null) && App.data.hasSubjects()) {
            items.add(null)
            editPos = items.lastIndex
            editId = -1L
            editSub = null
            editInfo = null
            editDeadline = null
            notifyItemInserted(items.lastIndex)
        }
    }

    fun clear() {
        App.data.clearNotesOfCategory(category)
        activeHolder = null
        clearChanges()
        val range = items.indices
        items.clear()
        notifyItemRangeRemoved(range.first, range.count())
        addNewHolder()
    }

    fun loadCategory(cat: NoteCategory) {
        if (!this::category.isInitialized || category != cat) {
            category = cat
            items.clear()
            items.addAll(App.data.notes(cat))
            addNewHolder()
            notifyDataSetChanged()
        }
    }

    fun reload() {
        clearChanges()
        items.clear()
        items.addAll(App.data.notes(category))
        addNewHolder()
        notifyDataSetChanged()
    }

    private fun clearChanges() {
        editPos = null
        editId = null
        editInfo = null
        editSub = null
        editDeadline = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        NoteHolder(LayoutInflater.from(parent.context).inflate(R.layout.note, parent, false))

    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
        (holder as NoteHolder).bind()

    override fun getItemId(position: Int): Long =
        if (position in items.indices) items[position]?.id ?: -1L else -1L

    fun indexOfNote(id: Long) =
        if (id == -1L) items.lastIndex else items.indexOfFirst { it?.id == id }

    fun reloadSubjects() {
        subjects.clear()
        subjects.addAll(App.data.subjects())
    }

    inner class NoteHolder(view: View) : RecyclerView.ViewHolder(view) {

        init {
            App.log("created holder")
        }

        private val note
            get() =
                if (adapterPosition in items.indices) items[adapterPosition]
                else throw Exception("ViewHolder is no longer bound to the list of notes!")

        val id get() = note?.id ?: -1
        var info
            get() = if (editMode) itemView.editable_text.text?.toString() else note?.description
            set(value) {
                if (editMode) itemView.editable_text.setText(value)
                else itemView.readable_text.text = value
            }

        var millis
            get() = itemView.date_label.tag as Long?
            set(value) {
                itemView.date_label.tag = value
                itemView.date_label.text = value?.let { Prefs.settings.getDateTimeString(it) }
                itemView.unset_date.visibility = millis?.let { VISIBLE } ?: GONE
                if (!editMode) items[adapterPosition] = Note(id, subject, info ?: "", value)
                else editDeadline = value
            }

        var subject
            get() = itemView.note_title.tag as Subject
            set(value) {
                itemView.note_title.tag = value
                itemView.note_title.text = value.abb
                itemView.counter.text = if (value in subjects) String.format(
                    "%d/%d",
                    subjects.indexOf(value) + 1,
                    subjects.size
                )
                else null
                if (editMode) editSub = value
            }

        private var editMode: Boolean = false
            set(value) {
                itemView.counter.visibility = if (value) VISIBLE else GONE
                itemView.previous.visibility = if (value) VISIBLE else GONE
                itemView.next.visibility = if (value) VISIBLE else GONE
                itemView.editable_text_container.visibility = if (value) VISIBLE else GONE
                itemView.save_btn.visibility = if (value) VISIBLE else GONE

                itemView.readable_text.visibility = if (value) GONE else VISIBLE
                itemView.edit_btn.visibility = if (value) GONE else VISIBLE
                field = value
            }

        val isEditing: Boolean get() = editMode

        fun bind() {
            editMode = (note?.id ?: -1L) == editId

            App.log("bound holder at $adapterPosition")

            itemView.previous.tag = this
            itemView.next.tag = this
            itemView.set_date.tag = this
            itemView.readable_text.tag = this
            itemView.unset_date.tag = this
            itemView.save_btn.tag = this
            itemView.edit_btn.tag = this
            itemView.del_btn.tag = this

            itemView.previous.setOnClickListener(previousSubject)
            itemView.next.setOnClickListener(nextSubject)
            itemView.set_date.setOnClickListener(onSetCalendar)
            itemView.date_label.setOnClickListener(onSetCalendar)
            itemView.unset_date.setOnClickListener(onUnsetCalendar)
            itemView.save_btn.setOnClickListener(onEditEnd)
            itemView.edit_btn.setOnClickListener(onEditStart)
            itemView.readable_text.setOnClickListener(onEditStart)
            itemView.del_btn.setOnClickListener(onRemove)
            itemView.editable_text.addTextChangedListener(onRewrite)

            subject = (if (editMode) editSub else note?.sub)
                ?: (if (category is Subject) category as Subject else subjects.first())
            info = if (editMode) editInfo else note?.description
            millis = if (editMode) editDeadline else note?.deadline

            if (editMode) activeHolder = this
        }

        fun startEditing() {
            editMode = true
            itemView.editable_text.setText(note?.description)
            keepChanges(this)
        }

        fun stopEditing() {
            clearChanges()
            val pos = adapterPosition
            if (pos in items.indices) {
                editMode = false
                if (items[pos] == null) {
                    items.removeAt(pos)
                    notifyItemRemoved(pos)
                } else {
                    itemView.editable_text.text?.clear()
                    itemView.readable_text.text = note?.description
                    millis = note!!.deadline
                    subject = note!!.sub
                }
            }
        }

        @StringRes
        private fun errorPresence(): Int? {
            if (itemView.note_title.tag !is Subject)
                return R.string.subject_required
            (itemView.date_label.tag as Long?)?.let {
                if (it <= System.currentTimeMillis())
                    return R.string.time_out
            }
            return Note.validDescription(itemView.editable_text.text?.toString())
        }

        fun save(): Int? = if (!editMode) null else errorPresence() ?: null.also {
            val new = note == null
            val newId = if (new) App.data.addNote(subject.id, info!!, millis) else id
            if (newId == -1L) throw Exception("Note could not be inserted!")
            val count = if (new) 1 else App.data.updateNote(note!!.id, subject.id, info!!, millis)
            if (count != 1) throw Exception("Note could not be updated!")
            items[adapterPosition] = Note(newId, subject, info!!, millis)
            stopEditing()
        }
    }
}