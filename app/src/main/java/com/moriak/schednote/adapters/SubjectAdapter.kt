package com.moriak.schednote.adapters

import android.os.Bundle
import android.view.View
import com.moriak.schednote.R
import com.moriak.schednote.data.Subject
import com.moriak.schednote.storage.SQLite
import com.moriak.schednote.enums.TimeCategory
import kotlinx.android.synthetic.main.edit_tools.view.*
import kotlinx.android.synthetic.main.subject.view.*

/**
 * Adapter spravuje zoznam predmetov
 */
class SubjectAdapter : CustomAdapter<Subject>(R.layout.subject) {
    /**
     * @property ACTION_EDIT Označenie pokusu o úpravu predmetu
     * @property ACTION_DELETE Označenie pokusu o odstránenie predmetu
     */
    companion object {
        private const val SUB_ID = "SUB_ID"
        private const val SUB_ABB = "SUB_ABB"
        private const val SUB_NAME = "SUB_NAME"
        const val ACTION_EDIT = 1998
        const val ACTION_DELETE = 1999
    }

    private val clickEvent = View.OnClickListener {
        triggerItemAction((it.tag as SubjectHolder).adapterPosition, extras, when (it.id) {
            R.id.edit -> ACTION_EDIT
            R.id.delete -> ACTION_DELETE
            else -> 0
        })
    }

    override fun compare(a: Subject, b: Subject): Int = a.abb.compareTo(b.abb)

    override fun instantiateViewHolder(v: View): CustomViewHolder = SubjectHolder(v)

    override fun bundleToItem(bundle: Bundle): Subject {
        val id = bundle.get(SUB_ID) as Long? ?: throw Exception("Missing subject ID!")
        val abb = bundle.getString(SUB_ABB) ?: throw Exception("Missing subject ABB!")
        val name = bundle.getString(SUB_NAME) ?: throw Exception("Missing subject NAME!")
        return Subject(id, abb, name)
    }

    override fun itemToBundle(item: Subject, bundle: Bundle) {
        bundle.putLong(SUB_ID, item.id)
        bundle.putString(SUB_ABB, item.abb)
        bundle.putString(SUB_NAME, item.name)
    }

    /**
     * Blok v tejto inštancii vizualizuje jednu položku zo zoznamu (predmet)
     */
    inner class SubjectHolder(view: View) : CustomAdapter<Subject>.CustomViewHolder(view) {
        override fun bind(pos: Int) {
            itemView.si_abb.text = item?.abb
            itemView.si_name.text = item?.name
            itemView.si_missed_notes.text = SQLite.notes(TimeCategory.LATE, item!!).count().toString()
            itemView.si_upcoming_notes.text = SQLite.notes(TimeCategory.UPCOMING, item!!).count().toString()
            itemView.save.visibility = View.GONE
            itemView.cancel.visibility = View.GONE
            itemView.edit.tag = this
            itemView.edit.setOnClickListener(clickEvent)
            itemView.delete.tag = this
            itemView.delete.setOnClickListener(clickEvent)
        }
    }
}