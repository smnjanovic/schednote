package com.moriak.schednote.fragments.of_main

import android.content.Context
import android.os.Bundle
import android.view.View
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.adapters.SubjectAdapter
import com.moriak.schednote.data.Subject
import com.moriak.schednote.dialogs.SubjectEditDialog
import com.moriak.schednote.fragments.ListSubActivity
import com.moriak.schednote.storage.SQLite
import com.moriak.schednote.widgets.NoteWidget
import com.moriak.schednote.widgets.ScheduleWidget
import kotlinx.android.synthetic.main.list_layout.view.*

/**
 * Tento fragment obsahuje abecedný zoznam a správu predmetov.
 */
class SubjectList : ListSubActivity<Subject>(R.layout.list_layout, R.id.list, R.id.empty, 4) {
    private companion object { private const val EDIT_DIALOG = "EDIT_DIALOG" }

    override val adapter = SubjectAdapter()

    private val onResult = object: SubjectEditDialog.ResultHandler {
        override fun onResult(subject: Subject?, isNew: Boolean, error: Int?) {
            when {
                subject == null -> App.toast(error!!)
                isNew -> adapter.insertItem(subject)
                else -> {
                    val pos = adapter.findIndexOf(fun(sub, sid) = sub.id == sid,  subject.id)
                    if (pos == -1) throw Exception("Item unavailable!")
                    adapter.updateItem(subject, pos)
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.setTitle(R.string.subjects)
    }

    override fun onItemAction(pos: Int, data: Bundle, code: Int) = when (code) {
        SubjectAdapter.ACTION_EDIT -> showDialog(EDIT_DIALOG, SubjectEditDialog(adapter.getItemAt(pos), onResult))
        SubjectAdapter.ACTION_DELETE -> when (SQLite.deleteSubject(adapter.getItemAt(pos).id)) {
            1 -> {
                adapter.deleteItem(pos)
                ScheduleWidget.update(requireContext())
                NoteWidget.update(requireContext())
            }
            else -> throw Exception("Undefined behaviour while removing subject from SQL.")
        }
        else -> Unit
    }

    override fun firstLoad(): List<Subject> = SQLite.subjects()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        findFragment<SubjectEditDialog>(EDIT_DIALOG)?.resultHandler = onResult
        view.empty.setText(R.string.no_subjects)
        view.list_add.setOnClickListener {
            showDialog(EDIT_DIALOG, SubjectEditDialog(null, onResult))
        }
    }
}