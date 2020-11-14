package com.moriak.schednote.fragments.of_schedule

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.adapters.ScheduleAdapter
import com.moriak.schednote.database.data.LessonData.Companion.MAX_LESSON_COUNT
import com.moriak.schednote.design.ItemTopSpacing
import com.moriak.schednote.dialogs.LessonDurationSetter
import com.moriak.schednote.dialogs.ScheduleStartSetter
import com.moriak.schednote.fragments.of_main.SubActivity
import com.moriak.schednote.settings.Prefs
import kotlinx.android.synthetic.main.lesson_time_scheduler.*
import kotlinx.android.synthetic.main.lesson_time_scheduler.view.*

class LessonTimesList : SubActivity(), SchedulePart {
    companion object {
        const val START_SCHEDULE = "START_SCHEDULE"
        const val LESSON_SETTER = "LESSON_SETTER"
    }

    private lateinit var startChanger: ScheduleStartSetter
    private lateinit var lessonEditor: LessonDurationSetter

    private lateinit var adapter: ScheduleAdapter

    private val scheduleStartChangeEvent =
        View.OnClickListener { showDialog(START_SCHEDULE, startChanger) }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.setTitle(R.string.time_schedule)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) =
        inflater.inflate(R.layout.lesson_time_scheduler, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = ScheduleAdapter()

        // prvotne nastavenie hodnot
        view.schedule_start.text = Prefs.settings.getTimeString(Prefs.settings.earliestMinute)
        view.schedule_start.setOnClickListener(scheduleStartChangeEvent)
        view.edit_schedule_start.setOnClickListener(scheduleStartChangeEvent)

        view.lesson_list.layoutManager = LinearLayoutManager(context)
        view.lesson_list.adapter = adapter
        view.lesson_list.addItemDecoration(ItemTopSpacing(5))
        view.lesson_list.setEmptyView(view.sched_empty)

        startChanger = requireFragment(START_SCHEDULE, ScheduleStartSetter::class.java)
        lessonEditor = requireFragment(LESSON_SETTER, LessonDurationSetter::class.java)

        startChanger.setAffectedView(view.schedule_start)
        startChanger.setAffectedAdapter(adapter)
        lessonEditor.setAdapter(adapter)

        adapter.setShowDialog {
            lessonEditor.setValues(it.order, it.lessonDuration, it.breakDuration)
            showDialog(LESSON_SETTER, lessonEditor)
        }

        addLesson.setOnClickListener {
            if (adapter.itemCount < MAX_LESSON_COUNT) {
                lessonEditor.setDefault()
                showDialog(LESSON_SETTER, lessonEditor)
            } else App.toast(R.string.too_many_lessons)
        }
    }
}