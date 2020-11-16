package com.moriak.schednote.dialogs

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.DialogFragment
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.adapters.SubjectAdapter
import com.moriak.schednote.database.data.Subject
import kotlinx.android.synthetic.main.sub_edit.view.*

/**
 * Dialóg je špecialne určený na úpravu a tvorbu Predmetov a následne aktualizovanie adaptéra, z ktorého je predmet upravovaný.
 */
class SubjectEditDialog : DialogFragment() {
    companion object {
        private const val ID = "ID"
        private const val ABB = "ABB"
        private const val NAME = "NAME"

        /**
         * Vytvorenie dialógu, v ktorom dochádza k manipulácii s existujúcim predmetom alebo tvorba nového predmetu.
         * @param sub Upravovaný predmet
         * @param adapter Adapter v ktorom sa zmeny aplikujú
         * @return Dialóg na manipuláciu s predmetom
         */
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

        /**
         * Vytvorenie dialógu, v ktorom dochádza k manipulácii s existujúcim predmetom alebo tvorba nového predmetu.
         * @param adapter Adapter v ktorom sa zmeny aplikujú
         * @return Dialóg na manipuláciu s predmetom
         */
        fun newInstance(adapter: SubjectAdapter): SubjectEditDialog =
            SubjectEditDialog().apply { setAdapter(adapter) }
    }

    private var bundle = Bundle()
    private lateinit var adapter: SubjectAdapter
    private var v: View? = null

    private var id: Long = -1
    private var abb: String = ""
    private var name: String = ""

    /**
     * Nastavenie adaptéra, v ktorom sa zmena z dialógu aplikuje
     */
    fun setAdapter(pAdapter: SubjectAdapter) {
        adapter = pAdapter
    }

    @SuppressLint("InflateParams")
    override fun onCreateDialog(saved: Bundle?) = activity?.let { activity ->
        val builder = AlertDialog.Builder(activity)
        v = LayoutInflater.from(context).inflate(R.layout.sub_edit, null, false)
        saved?.let {
            bundle.putAll(saved)
            id = it.getLong(ID, id)
            abb = it.getString(ABB, abb)
            name = it.getString(NAME, name)
        }

        v!!.sub_abb.addTextChangedListener(object : TextWatcher {
            private var st = 0
            private var en = 0
            private val bad = "[^${Subject.l}${Subject.d}]".toRegex()

            override fun afterTextChanged(s: Editable?) {
                if (s != null) {
                    if (s.length > Subject.abb_limit) s.delete(st, en)
                    if (s.contains(bad)) s.delete(st, en)
                    if (s.contains(bad)) s.replace(0, s.length, s.replace(bad, ""))
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                st = start
                en = start + count
            }
        })

        v!!.sub_name.addTextChangedListener(object : TextWatcher {
            private var st = 0
            private var en = 0
            private val illegal =
                "(^[^a-zA-ZÀ-ž0-9])|([^a-zA-ZÀ-ž0-9 ])|([ ][ ]+)|(^[ ]+)".toRegex()

            override fun afterTextChanged(s: Editable?) {
                if (s != null) {
                    if (s.length > Subject.name_limit) s.delete(st, en)
                    if (s.contains(illegal)) s.delete(st, en)
                    if (s.contains(illegal)) {
                        val fix = s.replace("[^a-zA-ZÀ-ž0-9 ]+".toRegex(), "")
                        s.replace(0, s.length, fix.trimStart())
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                st = start
                en = start + count
            }
        })

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