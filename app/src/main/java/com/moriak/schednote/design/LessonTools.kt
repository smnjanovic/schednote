package com.moriak.schednote.design

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.annotation.StringRes
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.activities.MainActivity
import com.moriak.schednote.database.data.*
import com.moriak.schednote.database.data.Lesson.RoomWatcher
import com.moriak.schednote.menu.ScheduleDisplay
import com.moriak.schednote.menu.SubContent
import com.moriak.schednote.other.Day
import com.moriak.schednote.settings.Prefs
import com.moriak.schednote.settings.Regularity
import kotlinx.android.synthetic.main.lesson_tools.view.*

class LessonTools private constructor() {
    companion object {
        private val handler = Handler(Looper.myLooper()!!)
        fun makeTools(tools: LessonTools?) = tools ?: LessonTools()
    }

    private abstract class LessonTime(protected val lesson: LessonData) {
        val order get() = lesson.order
    }

    private class LessonStart(lesson: LessonData) : LessonTime(lesson) {
        override fun toString() = lesson.startFormat
    }

    private class LessonEnd(lesson: LessonData) : LessonTime(lesson) {
        override fun toString() = lesson.endFormat
    }

    private var editingLesson: Lesson? = null
    private var autoAdaptEventEnd = true

    private val parent get() = mainView.parent as ViewGroup?

    @SuppressLint("InflateParams")
    private val mainView = LayoutInflater.from(App.ctx)
        .inflate(R.layout.lesson_tools, null, false)
        ?.lesson_editor_tools ?: throw NullPointerException("No such layout is available!")

    // vzdy budu vkladane spravne hodnoty
    private var day: Day
        get() {
            if (days.isEmpty()) throw IndexOutOfBoundsException("List of days is Empty!")
            return days[mainView.day_setter.selectedItemPosition]
        }
        set(value) {
            val pos = days.indexOf(value)
            if (pos == -1)
                throw IndexOutOfBoundsException("This day isn't part of currently set WorkWeek!")
            mainView.day_setter.setSelection(pos)
        }
    private var time: IntRange
        get() {
            if (scheduleTimes.isEmpty() || lesStarts.isEmpty() || lesEnds.isEmpty())
                throw IndexOutOfBoundsException("Schedule consists of no Lessons!")
            val result = lesStarts[mainView.start_setter.selectedItemPosition].order..
                    lesEnds[mainView.duration_setter.selectedItemPosition].order
            ScheduleEvent.rangeCheck(result)
            return result
        }
        set(value) {
            ScheduleEvent.rangeCheck(value)
            if (scheduleTimes.firstOrNull()?.let { value.first < it.order } != false ||
                scheduleTimes.lastOrNull()?.let { value.last > it.order } != false)
                throw IndexOutOfBoundsException("This time period is out of schedule time period!")

            autoAdaptEventEnd = false
            mainView.start_setter.setSelection(value.first - 1)
            adaptEventEnd()
            mainView.duration_setter.setSelection(value.last - lesEnds.first().order)
            autoAdaptEventEnd = true
        }
    private var regularity: Regularity
        get() {
            if (regularities.isEmpty()) throw Exception("List of regularities is empty!")
            return regularities[mainView.regularity_setter.selectedItemPosition]
        }
        set(value) {
            val i = regularities.indexOf(value)
            if (i == -1) throw IndexOutOfBoundsException("This regularity isn't an option!")
            mainView.regularity_setter.setSelection(i)
        }

    private var subject: Subject
        get() {
            if (subjects.isEmpty()) throw IllegalArgumentException("List of subjects is Empty!")
            return subjects[mainView.subject_setter.selectedItemPosition]
        }
        set(value) {
            val pos = subjects.indexOf(value)
            if (pos == -1) throw IllegalArgumentException("This subject isn't an option!")
            mainView.subject_setter.setSelection(pos)
        }

    private var type: Int
        get() {
            if (types.isEmpty()) throw Exception("List of lesson types is empty!")
            return types[mainView.type_setter.selectedItemPosition].id
        }
        set(value) {
            for (t in types.indices)
                if (types[t].id == value)
                    return mainView.type_setter.setSelection(t)
            throw Exception("This type of lesson isn't an option!")
        }

    private var location
        get() = Lesson.roomValid(mainView.room.text)
        set(value) {
            mainView.room.text?.let { s -> s.replace(0, s.length, value) }
        }

    private var confirmBtn: View? = null
    private var abortBtn: View? = null
    private var clearBtn: View? = null

    private var days = ArrayList<Day>()
    private var scheduleTimes = ArrayList<LessonData>()
    private var lesStarts = ArrayList<LessonStart>()
    private var lesEnds = ArrayList<LessonEnd>()
    private var regularities = ArrayList<Regularity>()
    private var types = ArrayList<LessonType>()
    private var subjects = ArrayList<Subject>()

    private val isModifiable get() = scheduleTimes.size * types.size * subjects.size > 0
    private var onInput: (ScheduleEvent) -> Boolean = fun(_) = true
    private var onClear: () -> Unit = fun() = Unit
    private var onUpdateEnd: () -> Unit = fun() = Unit

    init {
        days.addAll(Prefs.settings.workWeek.days)
        scheduleTimes.addAll(App.data.timetable())
        for (lesson in scheduleTimes) lesStarts.add(LessonStart(lesson))
        val range = (mainView.start_setter?.selectedItemPosition?.coerceAtLeast(0)
            ?: 0) until scheduleTimes.size
        for (l in range) lesEnds.add(LessonEnd(scheduleTimes[l]))
        regularities.addAll(Regularity.values)
        types.addAll(App.data.lessonTypes())
        subjects.addAll(App.data.subjects())

        val ctx = App.ctx
        val lay = android.R.layout.simple_list_item_1
        mainView.subject_setter.adapter = ArrayAdapter(ctx, lay, subjects)
        mainView.regularity_setter.adapter = ArrayAdapter(ctx, lay, regularities)
        mainView.day_setter.adapter = ArrayAdapter(ctx, lay, days)
        mainView.start_setter.adapter = ArrayAdapter(ctx, lay, lesStarts)
        mainView.start_setter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (autoAdaptEventEnd) adaptEventEnd()
            }
        }

        mainView.duration_setter.adapter = ArrayAdapter(ctx, lay, lesEnds)
        mainView.type_setter.adapter = ArrayAdapter(ctx, lay, types)
        mainView.room.addTextChangedListener(RoomWatcher)

        customizeViewVisibility()
    }

    private fun adaptEventEnd() {
        val start = lesStarts[mainView.start_setter.selectedItemPosition].order
        val end = lesEnds[mainView.duration_setter.selectedItemPosition].order

        lesEnds.clear()
        for (l in mainView.start_setter.selectedItemPosition until scheduleTimes.size) lesEnds.add(
            LessonEnd(scheduleTimes[l])
        )
        mainView.duration_setter.setSelection((end - start).coerceAtLeast(0))
        (mainView.duration_setter.adapter as ArrayAdapter<*>).notifyDataSetChanged()
    }

    fun setEvents(activity: MainActivity) {
        mainView.subjects_restriction.setOnClickListener {
            activity.menuChoice(SubContent.SUBJECTS)
        }
        mainView.lesson_type_restriction.setOnClickListener {
            Prefs.states.lastScheduleDisplay = ScheduleDisplay.LESSON_TYPES
            activity.forceMenuChoice(SubContent.SCHEDULE)
        }
        mainView.schedule_restriction.setOnClickListener {
            Prefs.states.lastScheduleDisplay = ScheduleDisplay.TIME_SCHEDULE
            activity.forceMenuChoice(SubContent.SCHEDULE)
        }
    }

    fun abortMessages() = handler.removeMessages(0)
    private fun parseLesson(id: Long) = Lesson(id, regularity, day, time, type, subject, location)
    private fun parseFree() = Free(regularity, day, time)
    private fun input(scheduleEvent: ScheduleEvent) {
        // ked pri podozreni ze nedoslo k zmene, skusat to znova a po 3 sekundach to vzdat.
        if (!onInput(scheduleEvent)) handler.postDelayed({ input(scheduleEvent) }, 200)
        handler.postDelayed({ handler.removeMessages(0) }, 2500)
    }

    fun attachTo(newParent: ViewGroup?) = also {
        parent?.removeView(mainView)
        newParent?.addView(mainView)
    }

    fun involveButtons(confirmButton: View?, abortButton: View?, clearButton: View?) = also {
        confirmBtn = confirmButton
        abortBtn = abortButton
        clearBtn = clearButton
        updateButtons()
        confirmBtn?.setOnClickListener { confirm() }
        abortBtn?.setOnClickListener { abort() }
        clearBtn?.setOnClickListener { removeOrEmpty() }
        if (!isModifiable) {
            confirmBtn?.visibility = GONE
            abortBtn?.visibility = GONE
            clearBtn?.visibility = GONE
        }
    }

    fun setOnUpdateEnd(fn: () -> Unit) {
        onUpdateEnd = fn
    }

    fun setOnInput(fn: (ScheduleEvent) -> Boolean) {
        onInput = fn
    }

    fun setOnClear(fn: () -> Unit) {
        onClear = fn
    }

    private fun customizeViewVisibility() {
        mainView.apply {
            val modifiable = isModifiable
            error_zone.visibility = if (modifiable) GONE else VISIBLE
            edit_zone.visibility = if (modifiable) VISIBLE else GONE
            confirmBtn?.visibility = if (modifiable) VISIBLE else GONE
            abortBtn?.visibility = if (modifiable) VISIBLE else GONE
            clearBtn?.visibility = if (modifiable) VISIBLE else GONE

            if (!modifiable) {
                lesson_type_restriction.visibility =
                    if (App.data.hasLessonTypes()) GONE else VISIBLE
                schedule_restriction.visibility = if (App.data.isScheduleSet()) GONE else VISIBLE
                subjects_restriction.visibility = if (App.data.hasSubjects()) GONE else VISIBLE
            }
        }
    }

    fun setLesson(lesson: Lesson) {
        editingLesson = lesson
        updateButtons()
        subject = lesson.sub
        day = lesson.day
        time = lesson.time
        regularity = lesson.regularity
        type = lesson.type
        location = lesson.room
    }

    private fun unsetLesson() {
        if (editingLesson != null) {
            editingLesson = null
            updateButtons()
            onUpdateEnd()
        }
    }

    fun getLessonInfo(): String? = editingLesson?.toString()

    private fun updateButtons() {
        fun txt(view: View?, @StringRes res: Int) {
            view?.let { if (it is TextView) it.setText(res) }
        }
        txt(confirmBtn, editingLesson?.let { R.string.edit } ?: R.string.insert)
        txt(abortBtn, R.string.abort)
        abortBtn?.visibility = editingLesson?.let { VISIBLE } ?: GONE
        txt(clearBtn, editingLesson?.let { R.string.delete } ?: R.string.to_empty)
    }

    private fun confirm() {
        if (!isModifiable) return
        val id = App.data.setLesson(
            editingLesson?.id ?: -1L,
            regularity,
            day,
            time,
            type,
            subject.id,
            location
        )
        if (id < 1) throw RuntimeException("Lesson could not be set! Check the inputs on \"LessonTools.kt\"!")
        val resId = editingLesson?.let { R.string.lesson_updated } ?: R.string.lesson_inserted
        unsetLesson()
        input(parseLesson(id))
        App.toast(resId, Gravity.CENTER)
    }

    private fun abort() {
        if (isModifiable) {
            unsetLesson()
            App.toast(R.string.lesson_edit_aborted, Gravity.CENTER)
        }
    }

    private fun removeOrEmpty() {
        if (!isModifiable) return
        when {
            editingLesson == null -> {
                App.data.clearSchedule()
                onClear()
                App.toast(R.string.schedule_emptied, Gravity.CENTER)
            }
            editingLesson!!.id < 1 -> {
                App.data.clearSchedule(day, time, regularity)
                input(parseFree())
                App.toast(R.string.time_cleared, Gravity.CENTER)
            }
            else -> {
                App.data.deleteLesson(editingLesson!!.id)
                input(parseFree())
                App.toast(R.string.lesson_removed, Gravity.CENTER)
            }
        }
        unsetLesson()
    }
}