package com.moriak.schednote.adapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.moriak.schednote.R
import com.moriak.schednote.data.Subject
import com.moriak.schednote.databinding.SubjectBinding
import com.moriak.schednote.enums.TimeCategory
import com.moriak.schednote.storage.SQLite

/**
 * Adapter spravuje zoznam predmetov
 */
class SubjectAdapter : CustomAdapter<Subject, SubjectBinding>() {
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        SubjectHolder(SubjectBinding.inflate(LayoutInflater.from(parent.context), parent, false))

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
    inner class SubjectHolder(b: SubjectBinding) : CustomViewHolder(b) {
        override fun bind(pos: Int) {
            binding.siAbb.text = item?.abb
            binding.siName.text = item?.name
            binding.siMissedNotes.text = SQLite.notes(TimeCategory.LATE, item!!).count().toString()
            binding.siUpcomingNotes.text = SQLite.notes(TimeCategory.UPCOMING, item!!).count().toString()
            binding.siTools.save.visibility = View.GONE
            binding.siTools.cancel.visibility = View.GONE
            binding.siTools.edit.tag = this
            binding.siTools.edit.setOnClickListener(clickEvent)
            binding.siTools.delete.tag = this
            binding.siTools.delete.setOnClickListener(clickEvent)
        }
    }
}