package com.moriak.schednote.fragments.of_main

import android.Manifest.permission.READ_CALENDAR
import android.Manifest.permission.WRITE_CALENDAR
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.provider.CalendarContract
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.get
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.adapters.NoteAdapter
import com.moriak.schednote.database.data.Note
import com.moriak.schednote.database.data.NoteCategory
import com.moriak.schednote.database.data.Subject
import com.moriak.schednote.design.ItemTopSpacing
import com.moriak.schednote.design.RecyclerViewEmptySupport
import com.moriak.schednote.dialogs.DateTimeDialog
import com.moriak.schednote.notifications.NoteReminder
import com.moriak.schednote.other.Redirection
import com.moriak.schednote.other.TimeCategory
import kotlinx.android.synthetic.main.notes.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList

/**
 * Fragment načítava zoznam úloh podľa vybranej kategórie. Úlohy
 * je tu možné uložiť do kalendára a spätne ich z neho aj načítať
 */
class NotesFragment : SubActivity() {
    private companion object {
        private const val DATE_TIME = "DATE_TIME"
        private const val SCROLL = "SCROLL"

        private const val REQUEST_CALENDAR_ACCESS = 1000

        private fun RecyclerViewEmptySupport.holder(pos: Int) =
            if (pos in 0 until adapter!!.itemCount) findViewHolderForAdapterPosition(pos) as NoteAdapter.NoteHolder
            else throw IndexOutOfBoundsException("There's no such index: $pos.")
    }

    private val adapter = NoteAdapter()
    private val subjects = ArrayList<Subject>()
    private val categories = ArrayList<NoteCategory>()
    private lateinit var catAdapt: ArrayAdapter<NoteCategory>
    private val categoryChoice = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) =
            adapter.loadCategory(TimeCategory.ALL)

        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) =
            adapter.loadCategory(parent!!.selectedItem as NoteCategory)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        NoteReminder.createNoteReminderChannel()
        activity?.setTitle(R.string.notes)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.notes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val onConfirm = fun(position: Int, millis: Long) {
            if (millis <= System.currentTimeMillis()) App.toast(R.string.time_out)
            else {
                val id = adapter.getItemId(position)
                val holder = view.note_list.holder(position)
                holder.millis = millis
                if (id > -1L && !holder.isEditing) App.data.changeNoteDeadline(id, millis)
                adapter.removeOutcast(position)?.let { App.toast(it) }
            }
        }

        if (savedInstanceState == null) {
            subjects.clear()
            subjects.addAll(App.data.subjects())
            categories.clear()
            categories.addAll(TimeCategory.values())
            categories.addAll(subjects)
            catAdapt =
                ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, categories)
        }

        view.note_groups.adapter = catAdapt
        view.note_groups.onItemSelectedListener = categoryChoice

        // nastavit vyber podla intentu ak je poziadavka zobrazit kategoriu alebo konkretnu ulohu
        if (savedInstanceState == null) {
            val redirect: Redirection? = Redirection.detectRedirection(activity?.intent)
            if (redirect == Redirection.NOTES) {
                val note: Long? = activity?.intent?.let {
                    if (!it.hasExtra(Redirection.EXTRA_NOTE_ID)) null
                    else it.getLongExtra(Redirection.EXTRA_NOTE_ID, -1L)
                }

                val cat: NoteCategory? = when {
                    activity!!.intent!!.hasExtra(Redirection.EXTRA_NOTE_CATEGORY) -> {
                        val catId =
                            activity!!.intent!!.getLongExtra(Redirection.EXTRA_NOTE_CATEGORY, -1L)
                        if (catId < 1) TimeCategory.values()[-catId.toInt()]
                        else App.data.subject(catId)
                    }
                    note != null -> App.data.detectNoteCategory(note)
                    else -> null
                }

                val catPos = categories.indexOf(cat)
                if (catPos > -1) view.note_groups.setSelection(catPos)
            }
        }

        adapter.setOnDateTimeSetAttempt { pos, deadline ->
            val dateTime = DateTimeDialog()
            dateTime.storeItemPositionAndDate(pos, deadline)
            dateTime.setOnConfirm(onConfirm)
            dateTime.setOnConfirm(onConfirm)
            dateTime.show(fragmentManager!!, DATE_TIME)
        }

        view.note_list.layoutManager = LinearLayoutManager(requireContext())
            .apply { onRestoreInstanceState(savedInstanceState) }
        view.note_list.setEmptyView(view.no_notes)
        view.note_list.addItemDecoration(ItemTopSpacing(App.dp(4)))
        view.note_list.adapter = adapter

        view.clear_all_label.setOnClickListener { adapter.clear() }

        findFragment(DATE_TIME, DateTimeDialog::class.java)?.setOnConfirm(onConfirm)

        view.to_the_calendar.setOnClickListener { sendNotesToTheCalendar() }
        view.out_of_calendar.setOnClickListener { loadNotesFromTheCalendar() }
    }

    override fun onResume() {
        super.onResume()
        val v = view ?: return
        v.post {
            if (activity?.intent?.hasExtra(Redirection.EXTRA_NOTE_ID) == true) {
                val pos = adapter.indexOfNote(
                    activity!!.intent!!.getLongExtra(
                        Redirection.EXTRA_NOTE_ID,
                        -1L
                    )
                )
                if (pos > -1) v.note_list.scrollToPosition(pos)
                else adapter.activePosition?.let { v.note_list.scrollToPosition(it) }
                activity!!.intent!!.removeExtra(Redirection.EXTRA_NOTE_ID)
            } else adapter.activePosition?.let { v.note_list.scrollToPosition(it) }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(SCROLL, view?.note_groups?.onSaveInstanceState())
    }

    /**
     * @return true, ak aplikácia už povolenia na čítanie a zápis do kalendára už mala
     */
    private fun requestPermission(): Boolean {
        val ctx = requireContext()
        val granted =
            ctx.checkSelfPermission(READ_CALENDAR) == PERMISSION_GRANTED && ctx.checkSelfPermission(
                READ_CALENDAR
            ) == PERMISSION_GRANTED
        if (!granted) requestPermissions(
            arrayOf(READ_CALENDAR, WRITE_CALENDAR),
            REQUEST_CALENDAR_ACCESS
        )
        return granted
    }

    private fun loadNotesFromTheCalendar() {
        if (requestPermission()) {
            CoroutineScope(Default).launch {
                App.ctx.contentResolver.query(
                    CalendarContract.Events.CONTENT_URI,
                    arrayOf(CalendarContract.Events.TITLE, CalendarContract.Events.DTSTART),
                    """
                        ${CalendarContract.Events.TITLE} GLOB 'schednote [\[]*]:*'
                        AND ${CalendarContract.Events.DTSTART} IS NOT NULL
                        AND ${CalendarContract.Events.DTEND} IS NOT NULL
                        AND ${CalendarContract.Events.DTSTART} > ${System.currentTimeMillis()}
                    """,
                    null,
                    null
                )?.use { curs ->
                    val notes = ArrayList<Note>()
                    while (curs.moveToNext()) {
                        val title = curs.getString(0)!!
                        val start = curs.getLong(1)
                        val abb = title.replace("(\\[.+]).*".toRegex(), "$1")
                            .replace(".*(\\[.+])".toRegex(), "$1")
                            .replace("[\\[\\]]".toRegex(), "")
                        val des = title.replace("^schednote \\[[^]]+]:(.*)".toRegex(), "$1").trim()
                        val sub = App.data.subject(abb) ?: let {
                            if (Subject.validAbb(abb) != null) null
                            else Subject(App.data.addSubject(abb, abb), abb, abb)
                        } ?: continue

                        if (Note.validDescription(des) == null && start > System.currentTimeMillis()) {
                            notes.find { n ->
                                n.sub == sub && n.deadline == start && n.description.trim()
                                    .toLowerCase(
                                        Locale.getDefault()
                                    ) == des.trim().toLowerCase(Locale.getDefault())
                            } ?: let {
                                val note = Note(-1L, sub, des, start)
                                if (!App.data.hasSimilarNote(note)) notes.add(note)
                            }
                        }
                    }
                    App.data.insertMultipleNotes(notes)
                }

                withContext(Main) {
                    if (lifecycle.currentState == Lifecycle.State.RESUMED) {
                        adapter.reload()
                        val cat =
                            view?.note_groups?.selectedItem as NoteCategory? ?: return@withContext

                        subjects.clear()
                        subjects.addAll(App.data.subjects())
                        categories.clear()
                        categories.addAll(TimeCategory.values())
                        categories.addAll(App.data.subjects())
                        adapter.reloadSubjects()
                        view!!.note_groups.setSelection(categories.indexOf(cat))
                    }
                    App.toast(R.string.notes_received_from_calendar, Gravity.CENTER)
                }
            }
        }
    }

    private fun sendNotesToTheCalendar() {
        if (requestPermission()) {
            val ids = ArrayList<Int>()
            val names = ArrayList<String>()

            App.ctx.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                arrayOf(
                    CalendarContract.Calendars._ID,
                    CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
                ),
                "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} = 700",
                null,
                null
            )?.use {
                while (it.moveToNext()) {
                    ids.add(it.getInt(0))
                    names.add(it.getString(1))
                }
            }

            when (ids.size) {
                0 -> App.toast(R.string.no_editable_calendar_available, Gravity.CENTER)
                1 -> CoroutineScope(Default).launch { addToCalendar(ids[0]) }
                else -> AlertDialog.Builder(requireContext())
                    .setItems(names.toTypedArray()) { dialog, which ->
                        val calendar =
                            ((dialog as AlertDialog).listView[which] as AppCompatTextView).text.toString()
                        CoroutineScope(Default).launch { addToCalendar(calendar) }
                        dialog.dismiss()
                    }.create().show()
            }
        }
    }

    private suspend fun addToCalendar(displayName: String) {
        App.ctx.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(CalendarContract.Calendars._ID),
            "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} LIKE ?",
            arrayOf(displayName),
            null
        )?.use {
            if (it.moveToFirst()) addToCalendar(it.getInt(0))
        }
    }

    private suspend fun addToCalendar(calId: Int) {
        val notes = App.data.incomingDeadlines()
        for (note in notes) {
            val cr = App.ctx.contentResolver
            val uri = CalendarContract.Events.CONTENT_URI!!
            val tit = CalendarContract.Events.TITLE
            val st = CalendarContract.Events.DTSTART
            val en = CalendarContract.Events.DTEND

            val cv = ContentValues()
            cv.put(tit, "schednote [${note.sub.abb}]: ${note.description}")
            cv.put(st, note.deadline.toString())
            cv.put(en, note.deadline.toString())
            cv.put(CalendarContract.Events.CALENDAR_ID, calId)
            cv.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)

            val selection = "$tit GLOB 'schednote [[]' || ? || ']:*' " +
                    "AND UPPER($tit) GLOB '*]:*' || ? || '*' " +
                    "AND $st = ${note.deadline ?: Long.MAX_VALUE} AND $st = $en " +
                    "AND ${CalendarContract.Events.CALENDAR_ID} = $calId"
            val args =
                arrayOf(note.sub.abb, note.description.trim().toUpperCase(Locale.getDefault()))

            // zabránenie vzniku duplicit
            cr.delete(uri, selection, args)
            cr.insert(uri, cv)
        }

        withContext(Main) {
            App.toast(R.string.notes_sent_to_calendar, Gravity.CENTER)
        }
    }
}