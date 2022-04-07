package com.moriak.schednote.fragments.of_main

import android.content.ContentValues
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.moriak.schednote.*
import com.moriak.schednote.adapters.NoteAdapter
import com.moriak.schednote.adapters.NoteAdapter.Companion.ACTION_DELETE
import com.moriak.schednote.adapters.NoteAdapter.Companion.ACTION_EDIT_SAVE
import com.moriak.schednote.adapters.NoteAdapter.Companion.ACTION_EDIT_START
import com.moriak.schednote.adapters.NoteAdapter.Companion.ACTION_EDIT_STOP
import com.moriak.schednote.adapters.NoteAdapter.Companion.ACTION_SELECT_DEADLINE
import com.moriak.schednote.adapters.NoteAdapter.Companion.ACTION_SELECT_SUBJECT
import com.moriak.schednote.adapters.NoteAdapter.Companion.CURSOR_END
import com.moriak.schednote.adapters.NoteAdapter.Companion.CURSOR_START
import com.moriak.schednote.adapters.NoteAdapter.Companion.ID
import com.moriak.schednote.adapters.NoteAdapter.Companion.INFO
import com.moriak.schednote.adapters.NoteAdapter.Companion.SUB_ABB
import com.moriak.schednote.adapters.NoteAdapter.Companion.SUB_ID
import com.moriak.schednote.adapters.NoteAdapter.Companion.SUB_NAME
import com.moriak.schednote.adapters.NoteAdapter.Companion.WHEN
import com.moriak.schednote.contracts.PermissionContract
import com.moriak.schednote.data.Note
import com.moriak.schednote.data.Subject
import com.moriak.schednote.databinding.NotesBinding
import com.moriak.schednote.dialogs.DateTimeDialog
import com.moriak.schednote.enums.PermissionHandler.CALENDAR
import com.moriak.schednote.enums.Redirection.Companion.EXTRA_NOTE_CATEGORY
import com.moriak.schednote.enums.Redirection.Companion.EXTRA_NOTE_ID
import com.moriak.schednote.enums.Redirection.Companion.detectRedirection
import com.moriak.schednote.enums.Redirection.NOTES
import com.moriak.schednote.enums.TimeCategory
import com.moriak.schednote.fragments.ListSubActivity
import com.moriak.schednote.interfaces.NoteCategory
import com.moriak.schednote.notifications.ReminderSetter
import com.moriak.schednote.storage.Prefs.States.lastNoteCategory
import com.moriak.schednote.storage.SQLite
import com.moriak.schednote.views.OptionStepper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import android.provider.CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL as C_LVL
import android.provider.CalendarContract.Calendars.CALENDAR_DISPLAY_NAME as C_NAME
import android.provider.CalendarContract.Calendars.CONTENT_URI as C_URI
import android.provider.CalendarContract.Calendars._ID as C_ID
import android.provider.CalendarContract.Events.CALENDAR_ID as E_ID
import android.provider.CalendarContract.Events.CONTENT_URI as E_URI
import android.provider.CalendarContract.Events.DTEND as E_EN
import android.provider.CalendarContract.Events.DTSTART as E_ST
import android.provider.CalendarContract.Events.EVENT_TIMEZONE as E_TIMEZONE
import android.provider.CalendarContract.Events.TITLE as E_TITLE
import java.lang.System.currentTimeMillis as now
import kotlin.arrayOf as arr

/**
 * Fragment zobrazí zoznam úloh vyhovujúcich danej kategórií a umožní tento zoznam spravovať.
 */
class NoteList : ListSubActivity<Note?, NoteAdapter, NotesBinding>(4) {
    private companion object {
        private const val DIALOG = "DATE_TIME"
        private val cal by lazy { Calendar.getInstance() }
    }

    override val adapter = NoteAdapter()
    override val adapterView by lazy { binding.noteList }
    private val launcher = registerForActivityResult(PermissionContract) {
        App.toast(if (it) R.string.permission_granted else CALENDAR.rationale, true)
    }
    private val subjects = ArrayList<Subject>()
    private val categories = ArrayList<NoteCategory>()
    private val clickListener = View.OnClickListener {
        when(it) {
            binding.clearAllLabel -> {
                setBundle(adapter.extras, null)
                adapter.clearItems()
                adapter.insertItem(null)
                ReminderSetter.clearNotesOfCategory(requireContext(), category)
            }
            binding.outOfCalendar -> CALENDAR.allowMe(requireContext(), launcher) { fromCal() }
            binding.toTheCalendar -> CALENDAR.allowMe(requireContext(), launcher) { toCal() }
        }
    }

    private var category: NoteCategory get() = NoteCategory[lastNoteCategory]
        set(value) {
            lastNoteCategory = value.id
        }

    private fun itemById(n: Note?, data: Any?): Boolean = (n?.id ?: -1L) == data

    private fun findCurrentItemPosition(): Int {
        val id: Long = adapter.extras.getLong(ID, -1L)
        return if (id == -1L) adapter.itemCount - 1
        else adapter.findIndexOf(this::itemById, id)
    }

    private fun onSubjectSelected(sub: Any?) {
        val pos = findCurrentItemPosition()
        if (pos > -1) {
            adapter.extras.putLong(SUB_ID, (sub as Subject).id)
            adapter.extras.putString(SUB_ABB, sub.abb)
            adapter.extras.putString(SUB_NAME, sub.name)
            adapter.notifyItemChanged(pos)
        }
    }

    private fun onDateSelected(date: Long?) {
        val pos = findCurrentItemPosition()
        if (pos > -1) {
            val now = now()
            when {
                date == null -> adapter.extras.remove(WHEN)
                date > now -> adapter.extras.putLong(WHEN, date)
                else -> App.toast(R.string.time_out)
            }
            if (date?.let { it <= now } != true) adapter.notifyItemChanged(pos)
        }
    }

    private fun activateDialogConfirmFn(f: Fragment?) = (f as DateTimeDialog)
        .setOnConfirm(this::onDateSelected)

    private fun loadInput() {
        subjects.clear()
        subjects.addAll(SQLite.subjects())
        categories.clear()
        categories.addAll(TimeCategory.values())
        categories.addAll(subjects)
    }

    private fun setBundle(output: Bundle, note: Note?): Bundle = output.apply {
        output.putLong(ID, note?.id ?: -1L)
        (note?.sub ?: if (category is Subject) category as Subject else null)?.let { sub ->
            output.putLong(SUB_ID, sub.id)
            output.putString(SUB_ABB, sub.abb)
            output.putString(SUB_NAME, sub.name)
        }
        output.putString(INFO, note?.info ?: "")
        note?.deadline?.let { output.putLong(WHEN, it) } ?: output.remove(WHEN)
        remove(CURSOR_START)
        remove(CURSOR_END)
    }

    private fun loadByCategory(cat: Any?) {
        if (category != cat) category = cat as NoteCategory
        adapter.clearItems()
        var notes: List<Note> = SQLite.notes(category)
        if (category is Subject) notes = notes.filter { note ->
            note.deadline?.let { it <= now() } == true
        }
        adapter.putItems(notes)
        //vkladaci prvok
        setBundle(adapter.extras, null)
        adapter.insertItem(null)
    }

    private fun belongs(note: Note): Boolean = when (category) {
        is Subject -> (category as Subject).id == note.sub.id
        TimeCategory.ALL -> true
        TimeCategory.TIMELESS -> note.deadline == null
        TimeCategory.LATE -> note.deadline?.let { it <= now() } ?: false
        TimeCategory.UPCOMING -> note.deadline?.let { it > now() } ?: false
        TimeCategory.TODAY -> note.deadline?.let { it in cal.now.today } ?: false
        TimeCategory.TOMORROW -> note.deadline?.let { it in cal.now.tomorrow } ?: false
        TimeCategory.IN_WEEK -> note.deadline?.let { it in cal.now.forWeek } ?: false
        TimeCategory.IN_MONTH -> note.deadline?.let { it in cal.now.forMonth } ?: false
        else -> false
    }

    private fun editAction(pos: Int, note: Note) {
        if (!belongs(note)) {
            adapter.deleteItem(pos)
            App.toast(R.string.outcast_note)
        } else adapter.updateItem(note, pos)
    }

    private fun noteToString(note: Note): String = "schednote [${note.sub.abb}]: ${note.info}"

    private fun noteFromString(str: String, ms: Long?): Note? {
        if (ms?.let { it <= now() } == true) return null
        val format = "^schednote *\\[([a-zA-ZÀ-ž0-9]{1,5})]: *([^\\s](\\s|.){0,255})(\\s|.)*$".toRegex()
        if (!str.matches(format)) return null
        val abb = format.replace(str, "$1")
        val info = format.replace(str, "$2")
        val sub = SQLite.subject(abb) ?: Subject(SQLite.addSubject(abb, abb), abb, abb)
        return Note(-1L, sub, info, ms)
    }

    private fun getCalendars(): ArrayList<Pair<Int, String>> {
        val list = ArrayList<Pair<Int, String>>()
        CALENDAR.ifAllowed(requireContext()) {
            val projection = arr(C_ID, C_NAME)
            val selection = "$C_LVL = 700"
            requireContext().contentResolver.query(C_URI, projection, selection, null, null)?.use {
                while (it.moveToNext()) list.add(it.getInt(0) to it.getString(1))
            }
        }
        return list
    }

    private fun fromCal() {
        CALENDAR.ifAllowed(requireContext()) {
            CoroutineScope(Default).launch {
                val query = "$E_TITLE GLOB 'schednote*' AND $E_ST > ${now()} AND $E_EN IS NOT NULL"
                val projection = arr(E_TITLE, E_ST, E_ID)
                requireContext().contentResolver.query(E_URI, projection, query, (null), (null))?.use { c ->
                    val notes = ArrayList<Note>()
                    while (c.moveToNext()) {
                        val note = noteFromString(c.getString(0), c.getLong(1)) ?: continue
                        if (!SQLite.hasSimilarNote(note)) notes.add(note)
                    }
                    ReminderSetter.addNotes(requireContext(), notes)
                }
                withContext(Main) {
                    if (lifecycle.currentState == Lifecycle.State.RESUMED) {
                        loadInput()
                        loadByCategory(category)
                        binding.noteGroups.index = categories.indexOf(category)
                    }
                    App.toast(R.string.notes_received_from_calendar)
                }
            }
        }
    }

    private fun toCal() {
        val cList = getCalendars()
        when (cList.size) {
            0 -> App.toast(R.string.no_editable_calendar_available)
            1 -> CoroutineScope(Default).launch { addToCalendar(requireContext(), cList[0].first) }
            else -> AlertDialog.Builder(requireContext())
                .setItems(cList.map { it.second }.toTypedArray()) { dialog, which ->
                    CoroutineScope(Default).launch { addToCalendar(requireContext(), cList[which].first) }
                    dialog.dismiss()
                }
                .show()
        }
    }

    private suspend fun addToCalendar(context: Context, calId: Int) {
        context.contentResolver.delete(E_URI, "LOWER($E_TITLE) GLOB 'schednote*'", null)
        val notes = SQLite.notes(TimeCategory.UPCOMING)
        for (note in notes) {
            val cv = ContentValues()
            cv.put(E_ID, calId)
            cv.put(E_TITLE, noteToString(note))
            cv.put(E_ST, "${note.deadline}")
            cv.put(E_EN, "${note.deadline!! + 300000}")
            cv.put(E_TIMEZONE, TimeZone.getDefault().id.toString())
            context.contentResolver.insert(E_URI, cv)
        }
        withContext(Main) { App.toast(R.string.notes_sent_to_calendar) }
    }

    override fun firstLoad(): List<Note?> {
        if (detectRedirection(activity?.intent) == NOTES) {
            requireActivity().intent.extras?.let { bdl ->
                category = NoteCategory[bdl.getLong(EXTRA_NOTE_CATEGORY, lastNoteCategory)]
                val id = bdl.getLong(EXTRA_NOTE_ID, -1L)
                binding.noteList.post {
                    val pos = adapter.findIndexOf(this::itemById, id)
                    if (pos > -1) binding.noteList.scrollToPosition(pos)
                }
            }
            activity?.intent?.data = null
            activity?.intent = null
        }
        else {
            binding.noteList.post {
                binding.noteList.scrollToPosition(adapter.itemCount - 1)
            }
        }
        setBundle(adapter.extras, null)
        return arrayListOf<Note?>(null).also { it.addAll(0, SQLite.notes(category)) }
    }

    override fun onItemAction(pos: Int, data: Bundle, code: Int) {
        when (code) {
            ACTION_SELECT_SUBJECT -> if (subjects.isEmpty()) App.toast(R.string.no_subjects)
            else AlertDialog.Builder(requireContext())
                .setItems(subjects.map { it.toString() }.toTypedArray()) { _, which ->
                    onSubjectSelected(subjects[which])
                }
                .setNegativeButton(R.string.abort, fun(_, _) {})
                .show()
            ACTION_SELECT_DEADLINE -> showDialog(DIALOG,
                activateDialogConfirmFn(DateTimeDialog(adapter.getItemAt(pos)?.deadline)))
            ACTION_DELETE -> adapter.getItemAt(pos)?.let {
                if (ReminderSetter.unsetNote(requireContext(), it)) adapter.deleteItem(pos)
            }
            ACTION_EDIT_START -> {
                val oldPos = findCurrentItemPosition()
                val oldItem = adapter.bundleToItem(data) ?: adapter.newItemFromBundle(data)
                setBundle(data, adapter.getItemAt(pos))
                adapter.notifyItemChanged(pos)
                ReminderSetter.setNote(requireContext(), oldItem) { editAction(oldPos, it) }?.let {
                    if (oldPos < adapter.itemCount - 1 || it != R.string.note_no_description) App.toast(it)
                    adapter.getItemAt(oldPos)?.let { _ -> adapter.notifyItemChanged(oldPos) }
                        ?: adapter.deleteItem(oldPos)
                }
            }
            ACTION_EDIT_SAVE -> {
                val item = adapter.bundleToItem(data) ?: adapter.newItemFromBundle(data)
                ReminderSetter.setNote(requireContext(), item) { note ->
                    setBundle(data, null)
                    editAction(pos, note)
                    adapter.insertItem(null)
                }?.let {
                    if (it != R.string.note_without_subject_link) App.toast(it)
                    else adapter.triggerItemAction(pos, data, ACTION_SELECT_SUBJECT)
                }
            }
            ACTION_EDIT_STOP -> {
                val isNew = adapter.getItemAt(pos) == null
                setBundle(data, null)
                adapter.notifyItemChanged(pos)
                if (!isNew) adapter.insertItem(null)
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.setTitle(R.string.notes)
    }

    override fun makeBinder(inflater: LayoutInflater, container: ViewGroup?) =
        NotesBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        loadInput()
        super.onViewCreated(view, savedInstanceState)
        findFragment<DateTimeDialog>(DIALOG)?.let(this::activateDialogConfirmFn)
        binding.noteGroups.setOptions(categories)
        binding.noteGroups.index = categories.indexOf(category)
        binding.noteGroups.setOnChange(this::loadByCategory)
        binding.noteGroups.setFormat(object: OptionStepper.Format {
            override fun getItemDescription(item: Any?): String = when (item) {
                is TimeCategory -> requireContext().getString(item.res)
                is Subject -> item.toString()
                else -> ""
            }
        })
        binding.clearAllLabel.setOnClickListener(clickListener)
        binding.toTheCalendar.setOnClickListener(clickListener)
        binding.outOfCalendar.setOnClickListener(clickListener)
    }
}