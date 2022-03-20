package com.moriak.schednote.fragments.of_schedule

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.adapters.LessonTimeAdapter
import com.moriak.schednote.data.LessonTime
import com.moriak.schednote.databinding.LessonTimeSchedulerBinding
import com.moriak.schednote.dayMinutes
import com.moriak.schednote.dialogs.DateTimeDialog
import com.moriak.schednote.dialogs.DateTimeDialog.Companion.FLAG_TIME
import com.moriak.schednote.dialogs.LessonDurationSetter
import com.moriak.schednote.enums.LessonTimeFormat
import com.moriak.schednote.fragments.ListSubActivity
import com.moriak.schednote.fragments.of_schedule.LessonTimeList.EditState.*
import com.moriak.schednote.storage.Prefs.Settings.earliestMinute
import com.moriak.schednote.storage.Prefs.Settings.lessonTimeFormat
import com.moriak.schednote.storage.Prefs.Settings.timeFormat
import com.moriak.schednote.storage.Prefs.States.lastSetBreakDuration
import com.moriak.schednote.storage.Prefs.States.lastSetLessonDuration
import com.moriak.schednote.storage.SQLite
import com.moriak.schednote.widgets.ScheduleWidget
import java.util.*

/**
 * Fragment obsahuje zoznam a správu časových blokov vyučovacieho času.
 * Tu používateľ nastaví začiatok rozvrhu a následne trvanie jednotlivých
 * hodín a prestávok po nich v minútach
 */
class LessonTimeList : ListSubActivity<LessonTime, LessonTimeAdapter, LessonTimeSchedulerBinding>(5) {
    private companion object {
        private const val START = "START_SCHEDULE"
        private const val SET = "LESSON_SETTER"
        private const val MAX = 18
        private val cal by lazy { Calendar.getInstance() }
    }

    override val adapter = LessonTimeAdapter()
    override val adapterView: RecyclerView by lazy { binding.lessonList }
    override val emptyView: View? by lazy { binding.schedEmpty }

    private val click = View.OnClickListener {
        when (it) {
            binding.scheduleStart, binding.editScheduleStart -> {
                val ms = cal.apply { dayMinutes = earliestMinute }.timeInMillis
                showDialog(START, DateTimeDialog(ms, FLAG_TIME).setOnConfirm(this::changeScheduleStart))
            }
            binding.addLessonUnit -> {
                if (adapter.itemCount < MAX) changeStart(null)
                else App.toast(R.string.too_many_lessons)
            }
        }
    }

    private fun changeScheduleStart(ms: Long?) {
        ms ?: return
        val dayMinutes = cal.apply { timeInMillis = ms }.dayMinutes
        if (adapter.computeStart(adapter.itemCount) - earliestMinute + dayMinutes < 1440) {
            earliestMinute = dayMinutes
            if (lessonTimeFormat == LessonTimeFormat.START_TIME)
                ScheduleWidget.update(requireContext())
            binding.scheduleStart.text = timeFormat.getFormat(earliestMinute)
            adapter.notifyItemRangeChanged(0, adapter.itemCount)
        }
        else App.toast(R.string.lesson_time_limit_exceeded)
    }

    private fun changeStart(item: LessonTime?) = showDialog(SET, LessonDurationSetter(item).also {
        it.setOnConfirm(this::changeLessonTime)
    })

    private fun saveChanges(order: Int, lDur: Int, bDur: Int): SaveResult {
        val isNew = order == -1
        val totalDur = adapter.computeStart(adapter.itemCount)
        val currentDur = if (order == -1) 0
        else adapter.getItemAt(order - 1).let { it.breakDuration + it.lessonDuration }
        val state = when {
            totalDur - currentDur + lDur + bDur >= 24 * 60 -> MINUTES_EXCEEDED
            isNew && adapter.itemCount >= MAX -> LESSONS_EXCEEDED
            else -> SUCCESS
        }
        var id = if (state == SUCCESS) order else -1
        if (state == SUCCESS) {
            if (isNew) id = SQLite.insertIntoSchedule(lDur, bDur)
            else SQLite.updateLessonTime(order, lDur, bDur)
            ScheduleWidget.update(requireContext())
            if (id == -1) throw Exception ("Failed to change lesson time!")
        }
        return SaveResult(state, id, isNew)
    }

    private fun changeLessonTime(order: Int, lDur: Int, bDur: Int) {
        val saveResult = saveChanges(order, lDur, bDur)
        if (saveResult.state == SUCCESS) {
            lastSetLessonDuration = lDur
            lastSetBreakDuration = bDur
            if (saveResult.isNew) adapter.insertItem(LessonTime(saveResult.resultOrder, lDur, bDur))
            else {
                adapter.updateItem(LessonTime(order, lDur, bDur), order - 1)
                if (order < adapter.itemCount)
                    adapter.notifyItemRangeChanged(order, adapter.itemCount - order)
            }
        }
        else App.toast(saveResult.state.res!!)
    }

    override fun onItemAction(pos: Int, data: Bundle, code: Int) {
        when (code) {
            LessonTimeAdapter.ACTION_EDIT -> changeStart(adapter.getItemAt(pos))
            LessonTimeAdapter.ACTION_DELETE -> {
                SQLite.removeFromSchedule(adapter.getItemAt(pos).order)
                adapter.deleteRange(pos until adapter.itemCount)
                ScheduleWidget.update(requireContext())
            }
        }
    }

    override fun firstLoad(): List<LessonTime> = SQLite.lessonTimes()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.setTitle(R.string.time_schedule)
    }

    override fun makeBinder(inflater: LayoutInflater, container: ViewGroup?) =
        LessonTimeSchedulerBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // prvotne nastavenie hodnot
        binding.scheduleStart.text = timeFormat.getFormat(earliestMinute)
        binding.scheduleStart.setOnClickListener(click)
        binding.editScheduleStart.setOnClickListener(click)
        binding.addLessonUnit.setOnClickListener(click)
        findFragment<DateTimeDialog>(START)?.setOnConfirm(this::changeScheduleStart)
        findFragment<LessonDurationSetter>(SET)?.setOnConfirm(this::changeLessonTime)
    }

    private enum class EditState(@StringRes val res: Int?) {
        SUCCESS(null),
        MINUTES_EXCEEDED(R.string.lesson_time_limit_exceeded),
        LESSONS_EXCEEDED(R.string.too_many_lessons)
    }

    private data class SaveResult(val state: EditState, val resultOrder: Int, val isNew: Boolean)
}