package com.moriak.schednote.fragments.of_schedule

import android.annotation.SuppressLint
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
import com.moriak.schednote.databinding.ScheduleEditorBinding
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
import java.util.*

/**
 * Fragment umožňuje modifikovať rozvrh hodín. Musia k tomu však nejaké dáta už existovať.
 * Inak sa zobrazia odkazy na fragmenty, v ktorých tie dáta možno vytvoriť.
 */
class ScheduleEditor : SubActivity<ScheduleEditorBinding>() {
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
            binding.scheduleEditor.timeReader.text = lessonTimeFormat.rangeFormat(range)
            describeLesson()
        }

        override fun onSet(range: IntRange) {
            binding.scheduleEditor.timeReader.text = lessonTimeFormat.rangeFormat(range)
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
                    binding.scheduleEditor.regularityChoice.option as Regularity,
                    binding.scheduleEditor.dayChoice.option as Day,
                    binding.scheduleEditor.timeChoice.value,
                    (binding.scheduleEditor.lessonTypeChoice.option as LessonType).id,
                    binding.scheduleEditor.subjectChoice.option as Subject,
                    binding.scheduleEditor.room.text?.toString()
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
                    val reg = binding.scheduleEditor.regularityChoice.option as Regularity
                    val day = binding.scheduleEditor.dayChoice.option as Day
                    val time = binding.scheduleEditor.timeChoice.value

                    if (regularityBelongs(reg)) free(day, time)
                    SQLite.clearSchedule(day, time, reg)
                    ScheduleWidget.update(v.context)
                    App.toast(R.string.schedule_freed)
                }
                else editing = null
            }
            R.id.delete_or_empty_out -> {
                if (editing != null) {
                    if (regularityBelongs(editing!!.reg)) free(editing!!.day, editing!!.time)
                    SQLite.clearSchedule(editing!!.day, editing!!.time, editing!!.reg)
                    App.toast(R.string.lesson_removed)
                    ScheduleWidget.update(v.context)
                    editing = null
                }
                else {
                    if (regularityBelongs(binding.scheduleEditor.regularityChoice.option as Regularity)) {
                        binding.schedulePreview.sv.clear()
                        binding.schedulePreview.sv.clearTags()
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
            if (value != EditTools.WHAT) binding.scheduleEditor.room.clearFocus()
            binding.scheduleEditor.schWhen.visibility = if (value == EditTools.WHEN) View.VISIBLE else View.GONE
            binding.scheduleEditor.schWhat.visibility = if (value == EditTools.WHAT) View.VISIBLE else View.GONE
            binding.scheduleEditor.schWhenBtn.alpha = if (value != EditTools.WHAT) 1F else 0.55F
            binding.scheduleEditor.schWhatBtn.alpha = if (value != EditTools.WHEN) 1F else 0.55F
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
            if (isBound) {
                binding.schedulePreview.odd.visibility = if (value == EVERY) View.GONE else View.VISIBLE
                binding.schedulePreview.even.visibility = binding.schedulePreview.odd.visibility
                binding.schedulePreview.regularityDescriptor.visibility = binding.schedulePreview.even.visibility
                binding.schedulePreview.odd.alpha = if (value == ODD) 1F else 0.55F
                binding.schedulePreview.even.alpha = if (value == EVEN) 1F else 0.55F
                loadSchedule()
                binding.schedulePreview.regularityDescriptor.setText(value.res)
            }
        }
    private var editing: Lesson? = null; set(value) {
        field = value
        if (isBound) {
            if (value != null) {
                binding.scheduleEditor.regularityChoice.index = regularities.indexOf(value.reg)
                binding.scheduleEditor.dayChoice.index = days.indexOf(value.day)
                binding.scheduleEditor.timeChoice.value = value.time
                binding.scheduleEditor.lessonTypeChoice.index = lessonTypes.indexOfFirst { it.id == value.type }
                binding.scheduleEditor.subjectChoice.index = subjects.indexOfFirst { it.id == value.sub.id }
                binding.scheduleEditor.room.setText(value.room)
            }
            binding.scheduleEditor.confirm.setText(value?.let { R.string.edit } ?: R.string.insert)
            binding.scheduleEditor.abortOrFree.setText(value?.let { R.string.abort } ?: R.string.free)
            binding.scheduleEditor.deleteOrEmptyOut.setText(value?.let { R.string.delete } ?: R.string.to_empty)
            describeLesson()
        }
    }

    private fun describeLesson() {
        binding.scheduleEditor.lessonDescription.text = editing?.let {
            StringBuilder().append(getString(R.string.lesson))
                .append(": ").append(it.sub.abb)
                .append(" — ").append(getString(it.day.res))
                .append(" ").append(it.reg.toString())
                .append(" ").append(lessonTimeFormat.rangeFormat(it.time))
                .append("\n").append(getString(R.string.edits))
                .append(": ").append((binding.scheduleEditor.subjectChoice.option as Subject).abb)
                .append(" — ").append(getString((binding.scheduleEditor.dayChoice.option as Day).res))
                .append(" ").append(binding.scheduleEditor.regularityChoice.option as Regularity)
                .append(" ").append(lessonTimeFormat.rangeFormat(binding.scheduleEditor.timeChoice.value))
                .toString()
        } ?: StringBuilder().append(getString(R.string.lesson))
            .append(": ").append((binding.scheduleEditor.subjectChoice.option as Subject).abb)
            .append(" — ").append(getString((binding.scheduleEditor.dayChoice.option as Day).res))
            .append(" ").append(binding.scheduleEditor.regularityChoice.option as Regularity)
            .append(" ").append(lessonTimeFormat.rangeFormat(binding.scheduleEditor.timeChoice.value))
            .toString()
    }

    private fun regularityBelongs(reg: Regularity?): Boolean = when (regularity) {
        EVEN -> reg == EVERY || reg == EVEN
        ODD -> reg == EVERY || reg == ODD
        EVERY -> reg == EVERY
    }

    private fun addLesson(les: Lesson) {
        binding.schedulePreview.sv.addLesson(les.day, les.time, les.type, les.sub.abb, les.room)
        binding.schedulePreview.sv.setTag(les.day, les.time, les)
    }

    private fun free(day: Day, time: IntRange) {
        binding.schedulePreview.sv.free(day, time)
        binding.schedulePreview.sv.setTag(day, time, null)
    }

    private fun loadSchedule() {
        binding.schedulePreview.sv.clear()
        binding.schedulePreview.sv.clearTags()
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
        "${binding.scheduleEditor.regularityChoice.index}",
        "${binding.scheduleEditor.dayChoice.index}",
        "${binding.scheduleEditor.timeChoice.value.first}",
        "${binding.scheduleEditor.timeChoice.value.last}",
        "${binding.scheduleEditor.lessonTypeChoice.index}",
        "${binding.scheduleEditor.subjectChoice.index}",
        binding.scheduleEditor.room.text?.toString()
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
        binding.scheduleEditor.regularityChoice.index = int(9)
        binding.scheduleEditor.dayChoice.index = int(10)
        binding.scheduleEditor.timeChoice.value = int(11)..int(12)
        binding.scheduleEditor.lessonTypeChoice.index = int(13)
        binding.scheduleEditor.subjectChoice.index = int(14)
        binding.scheduleEditor.room.setText(arr[15])
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun ifRequirementsMet(saved: Bundle?) {
        binding.requirementsMet.visibility = View.VISIBLE
        binding.requirementsNotMet.root.visibility = View.GONE

        // data pred obnovou alebo inicializaciou
        binding.scheduleEditor.regularityChoice.setOptions(regularities)
        binding.scheduleEditor.dayChoice.setOptions(days)
        binding.scheduleEditor.timeChoice.extreme = lessonTimes.count().let { if (it > 0) 1..it else 0..0 }
        binding.scheduleEditor.lessonTypeChoice.setOptions(lessonTypes)
        binding.scheduleEditor.subjectChoice.setOptions(subjects)
        binding.schedulePreview.sv.setWorkWeek(workWeek)

        if (saved != null) saved.getStringArray(STORED_VALUES)?.let(this::fromStorage)
        else regularity = Calendar.getInstance().getRegularity(workWeek, dualWeekSchedule)
        loadSchedule()
        describeLesson()

        // udalosti
        binding.schedulePreview.sv.setOnTouchListener(eventListener)
        binding.scheduleEditor.schWhenBtn.setOnClickListener(eventListener)
        binding.scheduleEditor.schWhatBtn.setOnClickListener(eventListener)
        binding.scheduleEditor.confirm.setOnClickListener(eventListener)
        binding.scheduleEditor.abortOrFree.setOnClickListener(eventListener)
        binding.scheduleEditor.deleteOrEmptyOut.setOnClickListener(eventListener)
        binding.schedulePreview.odd.setOnClickListener(eventListener)
        binding.schedulePreview.even.setOnClickListener(eventListener)
        binding.scheduleEditor.regularityChoice.setOnChange(eventListener)
        binding.scheduleEditor.dayChoice.setOnChange(eventListener)
        binding.scheduleEditor.lessonTypeChoice.setOnChange(eventListener)
        binding.scheduleEditor.subjectChoice.setOnChange(eventListener)
        binding.scheduleEditor.timeChoice.setRangeChangeListener(eventListener)
        binding.scheduleEditor.room.addTextChangedListener(roomWatcher)
        binding.scheduleEditor.room.onFocusChangeListener = eventListener

        //formaty
        binding.scheduleEditor.regularityChoice.setFormat(format)
        binding.scheduleEditor.dayChoice.setFormat(format)
        binding.scheduleEditor.lessonTypeChoice.setFormat(format)
        binding.scheduleEditor.subjectChoice.setFormat(format)
        binding.schedulePreview.sv.setFormat(format)

        // vzhlad a stavy
        IColorGroup.getGroups().forEach {
            binding.schedulePreview.sv
                .setTypeColor(it.id, it.color.color, it.color.contrast)
        }
        binding.scheduleEditor.timeReader.text = lessonTimeFormat
            .rangeFormat(binding.scheduleEditor.timeChoice.value)
    }

    private fun ifRequirementsNotMet() {
        // neda sa upravovať rozvrh, pretože treba ešte vytvoriť nejaké dáta
        binding.requirementsMet.visibility = View.GONE
        binding.requirementsNotMet.root.visibility = View.VISIBLE
        binding.requirementsNotMet.rnmSubjectsRequired.setOnClickListener(eventListener)
        binding.requirementsNotMet.rnmLessonTimesRequired.setOnClickListener(eventListener)
        binding.requirementsNotMet.rnmLessonTypesRequired.setOnClickListener(eventListener)

        if (subjects.isNotEmpty()) binding.requirementsNotMet.rnmSubjectsRequired.visibility = View.GONE
        if (lessonTypes.isNotEmpty()) binding.requirementsNotMet.rnmLessonTypesRequired.visibility = View.GONE
        if (lessonTimes.isNotEmpty()) binding.requirementsNotMet.rnmLessonTimesRequired.visibility = View.GONE
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.setTitle(R.string.lesson_schedule)
    }

    override fun makeBinder(inflater: LayoutInflater, container: ViewGroup?) =
        ScheduleEditorBinding.inflate(inflater, container, false)

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