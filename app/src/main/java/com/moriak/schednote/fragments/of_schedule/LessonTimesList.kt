package com.moriak.schednote.fragments.of_schedule

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.adapters.LessonTimeAdapter
import com.moriak.schednote.database.data.LessonTime
import com.moriak.schednote.database.data.LessonTime.Companion.MAX_LESSON_COUNT
import com.moriak.schednote.design.ItemTopSpacing
import com.moriak.schednote.dialogs.LessonDurationSetter
import com.moriak.schednote.dialogs.ScheduleStartSetter
import com.moriak.schednote.fragments.of_main.SubActivity
import com.moriak.schednote.settings.Prefs
import kotlinx.android.synthetic.main.lesson_time_scheduler.*
import kotlinx.android.synthetic.main.lesson_time_scheduler.view.*

/**
 * V tomto fragmente sa nastavujú otváracie hodiny.
 * Nastaví sa tu, od kedy rozvrh začína a pridávajú sa hodiny ako časové jednotky rozvrhu [LessonTime].
 * Upravovanej hodina sa nastaví koľko trvá, a aká dlhá prestávka má byť po nej
 */
class LessonTimesList : SubActivity(), SchedulePart {
    private companion object {
        private const val START_SCHEDULE = "START_SCHEDULE"
        private const val LESSON_SETTER = "LESSON_SETTER"
    }

    private lateinit var startChanger: ScheduleStartSetter
    private lateinit var lessonEditor: LessonDurationSetter

    private lateinit var adapter: LessonTimeAdapter

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
    ) = inflater.inflate(R.layout.lesson_time_scheduler, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = LessonTimeAdapter()

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

        startChanger.setOnConfirm { scheduleStart ->
            Prefs.settings.earliestMinute = scheduleStart
            view.schedule_start?.text = Prefs.settings.getTimeString(scheduleStart)
            adapter.notifyItemRangeChanged(0, adapter.itemCount)
        }
        lessonEditor.setOnConfirm { order, lesDur, breakDur ->
            if (order > -1L) adapter.update(order, lesDur, breakDur)
            else adapter.insert(lesDur, breakDur)
            Prefs.states.lastSetLessonDuration = lesDur
            Prefs.states.lastSetBreakDuration = breakDur
        }

        adapter.setShowDialog {
            lessonEditor.setValues(it.order, it.lessonDuration, it.breakDuration)
            showDialog(LESSON_SETTER, lessonEditor)
        }

        addLessonUnit.setOnClickListener {
            if (adapter.itemCount < MAX_LESSON_COUNT) {
                lessonEditor.setDefault()
                showDialog(LESSON_SETTER, lessonEditor)
            } else App.toast(R.string.too_many_lessons)
        }
    }
}