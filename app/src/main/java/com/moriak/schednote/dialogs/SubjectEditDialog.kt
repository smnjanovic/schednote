package com.moriak.schednote.dialogs

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.DialogFragment
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.adapters.SubjectAdapter
import com.moriak.schednote.database.data.Subject
import kotlinx.android.synthetic.main.sub_edit.view.*

class SubjectEditDialog : DialogFragment() {
    companion object {
        const val ID = "ID"
        const val ABB = "ABB"
        const val NAME = "NAME"

        fun newInstance(sub: Subject, adapter: SubjectAdapter): SubjectEditDialog {
            App.data.subject(sub.id)
                ?: throw IllegalArgumentException("No such subject in database!")
            val dialog = SubjectEditDialog()
            dialog.id = sub.id
            dialog.abb = sub.abb
            dialog.name = sub.name
            dialog.setAdapter(adapter)
            return dialog
        }

        fun newInstance(adapter: SubjectAdapter): SubjectEditDialog =
            SubjectEditDialog().apply { setAdapter(adapter) }


    }

    private var bundle = Bundle()
    private lateinit var adapter: SubjectAdapter
    private var v: View? = null

    private var id: Long = -1
    private var abb: String = ""
    private var name: String = ""

    fun setAdapter(pAdapter: SubjectAdapter) {
        adapter = pAdapter
    }

    @SuppressLint("InflateParams")
    override fun onCreateDialog(saved: Bundle?) = activity?.let { activity ->
        val builder = AlertDialog.Builder(activity)
        v = LayoutInflater.from(context).inflate(R.layout.sub_edit, null, false)
        saved?.let {
            bundle.putAll(saved)
            id = it.getLong(ID, -1)
            abb = it.getString(ABB, "")
            name = it.getString(name, "")
        }
        v!!.sub_abb.addTextChangedListener(Subject.AbbWatcher)
        v!!.sub_name.addTextChangedListener(Subject.NameWatcher)
        v!!.sub_abb.setText(abb)
        v!!.sub_name.setText(name)
        builder.setView(v)

        builder.setPositiveButton(if (id > 0) R.string.edit else R.string.insert) { _, _ ->
            abb = v?.sub_abb?.text.toString()
            name = v?.sub_name?.text.toString()
            if (id > 0) adapter.updateItem(id, abb, name)
            else adapter.insertItem(abb, name)
        }
        builder.setNegativeButton(R.string.abort) { _, _ -> }
        builder.create()
    } ?: throw(NullPointerException("Activity must have been destroyed!"))

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(ID, id)
        outState.putString(ABB, v?.sub_abb?.text.toString())
        outState.putString(NAME, v?.sub_name?.text.toString())
    }
}