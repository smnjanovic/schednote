package com.moriak.schednote.fragments.of_schedule

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.TextCursorTracer
import com.moriak.schednote.data.Lesson
import com.moriak.schednote.data.LessonType
import com.moriak.schednote.data.Subject
import com.moriak.schednote.enums.Day
import com.moriak.schednote.enums.Redirection
import com.moriak.schednote.enums.Regularity
import com.moriak.schednote.enums.Regularity.*
import com.moriak.schednote.fragments.SubActivity
import com.moriak.schednote.getRegularity
import com.moriak.schednote.interfaces.IColorGroup
import com.moriak.schednote.storage.Prefs.Settings.dualWeekSchedule
import com.moriak.schednote.storage.Prefs.Settings.lessonTimeFormat
import com.moriak.schednote.storage.Prefs.Settings.workWeek
import com.moriak.schednote.storage.SQLite
import com.moriak.schednote.views.OptionStepper
import com.moriak.schednote.views.RangeView
import com.moriak.schednote.views.ScheduleView
import com.moriak.schednote.widgets.ScheduleWidget
import kotlinx.android.synthetic.main.schedule_editor.*
import kotlinx.android.synthetic.main.schedule_editor.view.*
import java.util.*

/**
 * Fragment umožňuje modifikovať rozvrh hodín. Musia k tomu však nejaké dáta už existovať.
 * Inak sa zobrazia odkazy na fragmenty, v ktorých tie dáta možno vytvoriť.
 */
class ScheduleEditor : SubActivity() {
    private companion object { private const val STORED_VALUES = "STORED_VALUES" }

    private enum class EditTools { WHEN, WHAT, NOTHING }

    private class RoomWatcher: TextCursorTracer("^[a-zA-ZÀ-ž0-9][a-zA-ZÀ-ž0-9 ]{0,19}".toRegex()) {
        override fun onCursorChanged(range: IntRange) {}
        override fun afterValidTextChange(s: Editable) {}
        override fun afterInvalidTextChange(s: Editable) {
            if (s.isEmpty()) afterValidTextChange(s)
            else when (action) {
                TextAction.ADDED -> s.delete(st, en)
                TextAction.REPLACED -> s.delete(st, en)
                TextAction.REMOVED -> s.delete(0, "^([^a-zA-ZÀ-ž0-9]*)(.|\\s)*$"
                    .toRegex().replace(s, "$1").length)
            }
        }
    }

    private inner class Event: RangeView.RangeChangeListener, OptionStepper.OnChange, View.OnTouchListener, View.OnClickListener, View.OnFocusChangeListener {
        var scheduleMoving = false

        private fun getLessonExtreme(v: ScheduleView, les: Lesson, block: Int, jmp: Int): Int {
            var ex = block
            while (true)
                if ((v.getTag(les.day, ex + jmp) as Lesson?)?.let {
                        it.reg == les.reg && it.type == les.type
                                && it.sub.id == les.sub.id && it.room == les.room
                    } == true) ex += jmp
                else break
            return ex
        }

        private fun onLessonChoice(v: ScheduleView, x: Float, y: Float) {
            v.detectSchedulePart(x, y)?.let { pair ->
                var les = v.getTag(pair.first, pair.second) as Lesson?
                if (les != null) {
                    val time = getLessonExtreme(v, les, pair.second, -1)..
                            getLessonExtreme(v, les, pair.second, 1)
                    if (les.time != time) les = Lesson(les, time)
                }
                editing = les
            }
        }

        override fun onChange(v: View?, item: Any?) {
            when (v?.id) {
                R.id.regularity_choice, R.id.day_choice, R.id.subject_choice -> describeLesson()
            }
        }

        override fun onChange(range: IntRange) {
            time_reader.text = lessonTimeFormat.rangeFormat(range)
            describeLesson()
        }

        override fun onSet(range: IntRange) {
            time_reader.text = lessonTimeFormat.rangeFormat(range)
        }

        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            scheduleMoving = false
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> scheduleMoving = false
                MotionEvent.ACTION_MOVE -> scheduleMoving = true
                MotionEvent.ACTION_UP -> if (!scheduleMoving) {
                    onLessonChoice(v as ScheduleView, event.x, event.y)
                    v.performClick()
                }
            }
            return true
        }

        override fun onClick(v: View?) = when (v?.id) {
            R.id.sch_when_btn -> editTools = EditTools.WHEN
            R.id.sch_what_btn -> editTools = EditTools.WHAT
            R.id.confirm -> {
                val les = Lesson(
                    regularity_choice.option as Regularity,
                    day_choice.option as Day,
                    time_choice.value,
                    (lesson_type_choice.option as LessonType).id,
                    subject_choice.option as Subject,
                    room.text?.toString()
                )

                if (regularityBelongs(editing?.reg)) free(editing!!.day, editing!!.time)
                editing?.let { SQLite.clearSchedule(it.day, it.time, it.reg) }
                if (regularityBelongs(les.reg)) addLesson(les)
                SQLite.setLesson(les)
                ScheduleWidget.update(v.context)
                App.toast(editing?.let { R.string.lesson_updated } ?: R.string.lesson_inserted)
                editing = null
            }
            R.id.abort_or_free -> {
                if (editing == null) {
                    val reg = regularity_choice.option as Regularity
                    val day = day_choice.option as Day
                    val time = time_choice.value

                    if (regularityBelongs(reg)) free(day, time)
                    SQLite.clearSchedule(day, time, reg)
                    App.toast(R.string.schedule_freed)
                }
                else editing = null
            }
            R.id.delete_or_empty_out -> {
                if (editing != null) {
                    if (regularityBelongs(editing!!.reg)) free(editing!!.day, editing!!.time)
                    SQLite.clearSchedule(editing!!.day, editing!!.time, editing!!.reg)
                    App.toast(R.string.lesson_removed)
                    editing = null
                }
                else {
                    if (regularityBelongs(regularity_choice.option as Regularity)) {
                        sv.clear()
                        sv.clearTags()
                    }
                    SQLite.clearSchedule(regularity)
                    ScheduleWidget.update(v.context)
                    App.toast(R.string.schedule_emptied)
                }
            }
            R.id.odd -> regularity = ODD
            R.id.even -> regularity = EVEN
            R.id.rnm_subjects_required -> Redirection.SUBJECTS.redirect(v.context, false)
            R.id.rnm_lesson_times_required -> Redirection.TIME_SCHEDULE.redirect(v.context, false)
            R.id.rnm_lesson_types_required -> Redirection.LESSON_TYPES.redirect(v.context, false)
            else -> throw RuntimeException("Unexpected clickEvent")
        }

        override fun onFocusChange(v: View?, hasFocus: Boolean) {
            if (hasFocus) return
            ContextCompat.getSystemService(v?.context ?: return, InputMethodManager::class.java)
                ?.hideSoftInputFromWindow(v.windowToken, 0)
        }
    }

    private inner class Format: ScheduleView.ColumnNameFormat, OptionStepper.Format {
        override fun getColumnDescription(col: Int): String = lessonTimeFormat.startFormat(col)

        override fun getItemDescription(item: Any?): String = when (item) {
            is Regularity -> requireContext().getString(item.res)
            is Day -> requireContext().getString(item.res)
            is LessonType -> item.name
            is Subject -> item.toString()
            else -> ""
        }
    }

    private val eventListener = Event()
    private val format = Format()

    private val roomWatcher = RoomWatcher()
    private var editTools: EditTools = EditTools.NOTHING; set(value) {
        field = value
        view?.let {
            if (value != EditTools.WHAT) it.room.clearFocus()
            it.sch_when.visibility = if (value == EditTools.WHEN) View.VISIBLE else View.GONE
            it.sch_what.visibility = if (value == EditTools.WHAT) View.VISIBLE else View.GONE
            it.sch_when_btn.alpha = if (value != EditTools.WHAT) 1F else 0.55F
            it.sch_what_btn.alpha = if (value != EditTools.WHEN) 1F else 0.55F
        }
    }

    private val regularities = when (dualWeekSchedule) {
        true -> arrayOf(EVERY, EVEN, ODD)
        else -> arrayOf(EVERY)
    }
    private val days = workWeek.workDays
    private val lessonTimes = SQLite.lessonTimes()
    private val lessonTypes = SQLite.lessonTypes()
    private val subjects = SQLite.subjects()


    private var regularity: Regularity = EVERY
        set(value) {
            field = value
            view?.let {
                it.odd.visibility = if (value == EVERY) View.GONE else View.VISIBLE
                it.even.visibility = it.odd.visibility
                it.regularity_descriptor.visibility = it.even.visibility
                it.odd.alpha = if (value == ODD) 1F else 0.55F
                it.even.alpha = if (value == EVEN) 1F else 0.55F
                loadSchedule()
                it.regularity_descriptor.setText(value.res)
            }
        }
    private var editing: Lesson? = null; set(value) {
        field = value
        if (value != null) {
            regularity_choice.index = regularities.indexOf(value.reg)
            day_choice.index = days.indexOf(value.day)
            time_choice.value = value.time
            lesson_type_choice.index = lessonTypes.indexOfFirst { it.id == value.type }
            subject_choice.index = subjects.indexOfFirst { it.id == value.sub.id }
            room.setText(value.room)
        }
        confirm.setText(value?.let { R.string.edit } ?: R.string.insert)
        abort_or_free.setText(value?.let { R.string.abort } ?: R.string.free)
        delete_or_empty_out.setText(value?.let { R.string.delete } ?: R.string.to_empty)
        describeLesson()
    }

    private fun describeLesson() {
        view?.lesson_description?.text = editing?.let {
            StringBuilder().append(getString(R.string.lesson))
                .append(": ").append(it.sub.abb)
                .append(" — ").append(getString(it.day.res))
                .append(" ").append(it.reg.toString())
                .append(" ").append(lessonTimeFormat.rangeFormat(it.time))
                .append("\n").append(getString(R.string.edits))
                .append(": ").append((subject_choice.option as Subject).abb)
                .append(" — ").append(getString((day_choice.option as Day).res))
                .append(" ").append(regularity_choice.option as Regularity)
                .append(" ").append(lessonTimeFormat.rangeFormat(time_choice.value))
                .toString()
        } ?: StringBuilder().append(getString(R.string.lesson))
            .append(": ").append((subject_choice.option as Subject).abb)
            .append(" — ").append(getString((day_choice.option as Day).res))
            .append(" ").append(regularity_choice.option as Regularity)
            .append(" ").append(lessonTimeFormat.rangeFormat(time_choice.value))
            .toString()
    }

    private fun regularityBelongs(reg: Regularity?): Boolean = when (regularity) {
        EVEN -> reg == EVERY || reg == EVEN
        ODD -> reg == EVERY || reg == ODD
        EVERY -> reg == EVERY
    }

    private fun addLesson(les: Lesson) {
        sv.addLesson(les.day, les.time, les.type, les.sub.abb, les.room)
        sv.setTag(les.day, les.time, les)
    }

    private fun free(day: Day, time: IntRange) {
        sv.free(day, time)
        sv.setTag(day, time, null)
    }

    private fun loadSchedule() {
        sv.clear()
        sv.clearTags()
        SQLite.getLessons(workWeek, regularity).forEach(this::addLesson)
        if (regularity != EVERY) SQLite.getLessons(workWeek, EVERY).forEach(this::addLesson)
    }

    private fun toStorage(): Array<String?> = arrayOf(
        editing?.reg?.ordinal?.toString(),
        editing?.day?.value?.toString(),
        editing?.time?.first?.toString(),
        editing?.time?.last?.toString(),
        editing?.type?.toString(),
        editing?.sub?.id?.toString(),
        editing?.room,
        editTools.ordinal.toString(),
        "${regularity.ordinal}",
        "${regularity_choice.index}",
        "${day_choice.index}",
        "${time_choice.value.first}",
        "${time_choice.value.last}",
        "${lesson_type_choice.index}",
        "${subject_choice.index}",
        room.text?.toString()
    )

    private fun fromStorage(arr: Array<String?>) {
        fun int(i: Int) = arr[i]!!.toInt()
        fun long(i: Int) = arr[i]!!.toLong()
        if (arr[0] != null) {
            val reg = Regularity.values()[int(0)]
            val sub = subjects.find { it.id == long(5) }!!
            editing = Lesson(reg, Day[int(1)], int(2)..int(3), int(4), sub, arr[6])
        }
        editTools = EditTools.values()[int(7)]
        regularity = Regularity.values()[int(8)]
        regularity_choice.index = int(9)
        day_choice.index = int(10)
        time_choice.value = int(11)..int(12)
        lesson_type_choice.index = int(13)
        subject_choice.index = int(14)
        room.setText(arr[15])
    }

    private fun ifRequirementsMet(saved: Bundle?) {
        requirements_met.visibility = View.VISIBLE
        requirements_not_met.visibility = View.GONE

        // data pred obnovou alebo inicializaciou
        regularity_choice.setOptions(regularities)
        day_choice.setOptions(days)
        time_choice.extreme = lessonTimes.count().let { if (it > 0) 1..it else 0..0 }
        lesson_type_choice.setOptions(lessonTypes)
        subject_choice.setOptions(subjects)
        sv.setWorkWeek(workWeek)

        if (saved != null) saved.getStringArray(STORED_VALUES)?.let(this::fromStorage)
        else regularity = Calendar.getInstance().getRegularity(workWeek, dualWeekSchedule)
        loadSchedule()
        describeLesson()

        // udalosti
        sv.setOnTouchListener(eventListener)
        sch_when_btn.setOnClickListener(eventListener)
        sch_what_btn.setOnClickListener(eventListener)
        confirm.setOnClickListener(eventListener)
        abort_or_free.setOnClickListener(eventListener)
        delete_or_empty_out.setOnClickListener(eventListener)
        odd.setOnClickListener(eventListener)
        even.setOnClickListener(eventListener)
        regularity_choice.setOnChange(eventListener)
        day_choice.setOnChange(eventListener)
        lesson_type_choice.setOnChange(eventListener)
        subject_choice.setOnChange(eventListener)
        time_choice.setRangeChangeListener(eventListener)
        room.addTextChangedListener(roomWatcher)
        room.onFocusChangeListener = eventListener

        //formaty
        regularity_choice.setFormat(format)
        day_choice.setFormat(format)
        lesson_type_choice.setFormat(format)
        subject_choice.setFormat(format)
        sv.setFormat(format)

        // vzhlad a stavy
        IColorGroup.getGroups().forEach { sv.setTypeColor(it.id, it.color.color, it.color.contrast) }
        time_reader.text = lessonTimeFormat.rangeFormat(time_choice.value)
    }

    private fun ifRequirementsNotMet() {
        // neda sa upravovať rozvrh, pretože treba ešte vytvoriť nejaké dáta
        requirements_met.visibility = View.GONE
        requirements_not_met.visibility = View.VISIBLE
        rnm_subjects_required.setOnClickListener(eventListener)
        rnm_lesson_times_required.setOnClickListener(eventListener)
        rnm_lesson_types_required.setOnClickListener(eventListener)

        if (subjects.isNotEmpty()) rnm_subjects_required.visibility = View.GONE
        if (lessonTypes.isNotEmpty()) rnm_lesson_types_required.visibility = View.GONE
        if (lessonTimes.isNotEmpty()) rnm_lesson_times_required.visibility = View.GONE
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.setTitle(R.string.lesson_schedule)
    }

    override fun onCreateView(inf: LayoutInflater, par: ViewGroup?, saved: Bundle?): View = inf
        .inflate(R.layout.schedule_editor, par, false)!!

    override fun onViewCreated(view: View, saved: Bundle?) {
        super.onViewCreated(view, saved)
        if (lessonTimes.count()* lessonTypes.count() * subjects.count() > 0) ifRequirementsMet(saved)
        else ifRequirementsNotMet()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArray(STORED_VALUES, toStorage())
    }

    override fun onStop() {
        super.onStop()
        SQLite.clearUnusedLessons()
    }
}