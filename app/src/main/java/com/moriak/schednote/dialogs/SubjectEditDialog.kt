package com.moriak.schednote.dialogs

import android.os.Bundle
import android.text.Editable
import android.text.Selection
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.annotation.StringRes
import com.moriak.schednote.R.string.*
import com.moriak.schednote.TextCursorTracer
import com.moriak.schednote.TextCursorTracer.TextAction.*
import com.moriak.schednote.data.Subject
import com.moriak.schednote.databinding.SubEditBinding
import com.moriak.schednote.dialogs.SubjectEditDialog.FocusText.*
import com.moriak.schednote.storage.SQLite
import com.moriak.schednote.widgets.NoteWidget
import com.moriak.schednote.widgets.ScheduleWidget

/**
 * V tomto dialógovom fragmente sa menia údaje o predmete (skratka a názov).
 */
class SubjectEditDialog(subject: Subject? = null, handler: ResultHandler? = null) : CustomBoundDialog<SubEditBinding>() {
    private companion object {
        private const val STORAGE = "STORAGE"
        private val ABB_REGEX = "^[a-zA-ZÀ-ž0-9]{1,5}$".toRegex()
        private val NAME_REGEX = "^[a-zA-ZÀ-ž0-9][a-zA-ZÀ-ž0-9\\- ]{0,47}$".toRegex()
    }

    private var id: Long = subject?.id ?: -1L
    private var abb: String = subject?.abb ?: ""
    private var name: String = subject?.name ?: ""
    private var focusText: FocusText = NONE
    private var selectionRange = 0..0
    var resultHandler: ResultHandler? = handler
    override val negativeButton: ActionButton? by lazy { ActionButton(abort) {} }
    override val positiveButton: ActionButton? by lazy {
        ActionButton(if (id != -1L) edit else insert, this::confirm)
    }
    private val focusChange = FocusChange()
    private val abbWatcher = AbbWatcher()
    private val nameWatcher = NameWatcher()
    private fun confirm() {
        var resNew = false
        var resErr = 0

        if (!abb.matches(ABB_REGEX) || !name.matches(NAME_REGEX)) resErr = invalid_format
        else if (SQLite.subject(abb)?.let { it.id != id } == true) resErr = subject_exists
        else if (id > -1L) {
            if (SQLite.editSubject(id, abb, name) != 1) throw Exception("Failed to edit subject!")
            ScheduleWidget.update(requireContext())
            NoteWidget.update(requireContext())
        }
        else {
            id = SQLite.addSubject(abb, name)
            if (id == -1L) throw Exception("Failed to insert subject")
            resNew = true
        }

        resultHandler?.onResult(Subject(id, abb, name), resNew, resErr)
    }

    override fun setupBinding(inflater: LayoutInflater) = SubEditBinding.inflate(inflater)

    override fun setupContent(saved: Bundle?) {
        saved?.getStringArray(STORAGE)?.let {
            id = it[0].toLong()
            abb = it[1]
            name = it[2]
            focusText = FocusText.values()[it[3].toInt()]
            selectionRange = it[4].toInt()..it[5].toInt()
        }
    }

    override fun onStart() {
        super.onStart()
        binding.subAbb.setText(abb)
        binding.subName.setText(name)

        when (focusText) {
            ABB -> binding.subAbb
            NAME -> binding.subName
            NONE -> null
        }?.let {
            it.requestFocus()
            it.setSelection(selectionRange.first, selectionRange.last)
        }

        binding.subAbb.addTextChangedListener(abbWatcher)
        binding.subName.addTextChangedListener(nameWatcher)

        abbWatcher.setSpan(binding.subAbb.text)
        nameWatcher.setSpan(binding.subName.text)

        binding.subAbb.onFocusChangeListener = focusChange
        binding.subName.onFocusChangeListener = focusChange
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArray(STORAGE, arrayOf(
            id.toString(), abb, name,
            focusText.ordinal.toString(),
            selectionRange.first.toString(),
            selectionRange.last.toString()
        ))
    }

    interface ResultHandler {
        fun onResult(subject: Subject?, isNew: Boolean, @StringRes error: Int?)
    }

    private enum class FocusText { NONE, NAME, ABB }

    private inner class FocusChange: View.OnFocusChangeListener {
        override fun onFocusChange(editText: View?, focused: Boolean) {
            if (focused) {
                focusText = when (editText?.id) {
                    binding.subAbb.id -> ABB
                    binding.subName.id -> NAME
                    else -> NONE
                }
                val s = (editText as EditText).text ?: return
                selectionRange = Selection.getSelectionStart(s)..Selection.getSelectionEnd(s)
            }
        }
    }

    private inner class AbbWatcher: TextCursorTracer(ABB_REGEX) {
        override fun onCursorChanged(range: IntRange) { selectionRange = range }
        override fun afterValidTextChange(s: Editable) { abb = "$s".uppercase() }
        override fun afterInvalidTextChange(s: Editable) {
            if (s.isEmpty()) afterValidTextChange(s) else s.delete(st, en)
        }
    }

    private inner class NameWatcher: TextCursorTracer(NAME_REGEX) {
        private val helpRgx = "^([^a-zA-ZÀ-ž0-9]*)(.{0,48}).*$".toRegex()
        override fun onCursorChanged(range: IntRange) { selectionRange = range }
        override fun afterValidTextChange(s: Editable) {
            val t = s.toString()
            name = when (t.length) {
                0, 1 -> t.uppercase()
                else -> t.substring(0, 1).uppercase() + t.substring(1).lowercase()
            }
        }
        override fun afterInvalidTextChange(s: Editable) {
            if (s.isEmpty()) afterValidTextChange(s)
            else when (action) {
                REMOVED -> s.delete(0, s.replace(helpRgx, "$1").length)
                ADDED -> s.delete(st, en)
                REPLACED -> if (st > 0) s.delete(st, en) else s.delete(0, s.replace(helpRgx, "$1").length)
            }
            if (!s.matches(format!!)) s.replace(0, s.length, s.replace(helpRgx, "$2"))
        }
    }
}