package com.moriak.schednote.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.database.data.Subject
import com.moriak.schednote.database.data.Subject.CREATOR.validAbb
import com.moriak.schednote.database.data.Subject.CREATOR.validName
import kotlinx.android.synthetic.main.subject.view.*

class SubjectAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val items = App.data.subjects()

    // udalost sa musi vykonať
    private var showDialog = fun(_: Subject) = Unit

    /**
     * Pred úpravou alebo vložením
     * @param fn funkcia nastavuje skrytú funkciu, ktorá má byť zodpovedná za vytvorenie dialógu
     */
    fun setShowDialog(fn: (sub: Subject) -> Unit) {
        showDialog = fn
    }

    // bude pouzita metoda toast ak sa vyskytuju nejake chyby
    private fun isValid(abb: String, name: String): Boolean = (validAbb(abb) ?: validName(name))
        ?.also { App.toast(it) }?.let { false } ?: true

    fun insertItem(abb: String, name: String): Boolean {
        if (!isValid(abb, name)) return false
        if (App.data.subject(abb) != null) {
            App.toast(R.string.subject_exists)
            return false
        }
        val id: Long = App.data.addSubject(abb, name)
        if (id == -1L) throw RuntimeException("Item could not be inserted!")

        items.add(App.data.subject(id)!!)
        val oldPos: Int = items.lastIndex
        notifyItemInserted(oldPos)

        val newPos = App.data.subjectIndex(id)
        if (newPos != oldPos) {
            items.add(newPos, items.removeAt(oldPos))
            notifyItemMoved(oldPos, newPos)
        }
        return true
    }

    fun updateItem(id: Long, abb: String, name: String): Boolean {
        if (!isValid(abb, name)) return false

        App.data.subject(abb)?.let { sub ->
            if (sub.id != id) {
                App.toast(R.string.subject_exists)
                return false
            }
        }

        val oldPos = App.data.subjectIndex(id)
        val edit = App.data.editSubject(id, abb, name)
        if (edit != 1) throw RuntimeException("There should be only one record updated!")
        items[oldPos] = App.data.subject(id)!!
        notifyItemChanged(oldPos)

        val newPos = App.data.subjectIndex(id)
        if (newPos != oldPos) {
            items.add(newPos, items.removeAt(oldPos))
            notifyItemMoved(oldPos, newPos)
        }
        return true
    }

    private fun deleteItem(pos: Int): Boolean {
        if (pos !in items.indices) return false
        val del = App.data.deleteSubject(items[pos].id)
        if (del != 1) throw RuntimeException("There should have been just one record to remove!")
        items.removeAt(pos)
        notifyItemRemoved(pos)
        return true
    }

    private val onUpdate = View.OnClickListener { (it.tag as SubjectHolder).item?.let(showDialog) }
    private val onDelete =
        View.OnClickListener { deleteItem((it.tag as SubjectHolder).adapterPosition) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        SubjectHolder(LayoutInflater.from(parent.context).inflate(R.layout.subject, parent, false))

    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
        (holder as SubjectHolder).bind()

    inner class SubjectHolder(view: View) : RecyclerView.ViewHolder(view) {
        val item get() = if (adapterPosition in items.indices) items[adapterPosition] else null
        fun bind() {
            itemView.abb_field.text = item?.abb
            itemView.name_field.text = item?.name
            itemView.sub_edit_btn.tag = this
            itemView.sub_del_btn.tag = this
            itemView.missed_notes.text = App.data.missedNotes(items[adapterPosition].id).toString()
            itemView.incoming_notes.text =
                App.data.incomingNotes(items[adapterPosition].id).toString()
            itemView.sub_edit_btn.setOnClickListener(onUpdate)
            itemView.sub_del_btn.setOnClickListener(onDelete)
        }
    }
}