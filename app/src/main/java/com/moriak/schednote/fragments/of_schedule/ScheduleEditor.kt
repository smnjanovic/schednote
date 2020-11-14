package com.moriak.schednote.fragments.of_schedule

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.activities.MainActivity
import com.moriak.schednote.design.LessonTools
import com.moriak.schednote.design.ScheduleIllustrator
import com.moriak.schednote.fragments.of_main.SubActivity
import kotlinx.android.synthetic.main.schedule_editor.view.*

class ScheduleEditor : SubActivity(), SchedulePart {
    private var ms = System.currentTimeMillis()
    private var illustrator: ScheduleIllustrator? = null
    private var editor: LessonTools? = null

    fun notify(msg: String) {
        val old = ms
        ms = System.currentTimeMillis()
        App.log("$msg: ${ms - old}")
    }

    override fun onAttach(context: Context) {
        notify("before attachment")
        super.onAttach(context)
        activity?.setTitle(R.string.lesson_schedule)
        notify("after attachment")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        notify("before view creation")
        return inflater.inflate(R.layout.schedule_editor, container, false)!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.post {
            illustrator = ScheduleIllustrator.schedule(illustrator).attachTo(view.schedule_frame)
                .involveButtons(view.odd, view.even)
        }

        view.post {
            editor = LessonTools.makeTools(editor).attachTo(view.editor_frame)
                .involveButtons(view.confirm, view.abort_or_clear, view.delete_or_wipe_out)
        }

        view.post {
            illustrator?.customizeColumnWidth(view.schedule_frame.measuredWidth)
            activity?.let { if (it is MainActivity) editor!!.setEvents(it) }

            editor!!.setOnInput { added -> illustrator!!.put(added) }
            editor!!.setOnClear { illustrator!!.empty() }
            editor!!.setOnUpdateEnd {
                view.default_subject.visibility = View.GONE
                view.default_subject_value.text = null
            }

            illustrator!!.setOnLessonClick {
                editor!!.setLesson(it)
                view.default_subject.visibility = View.VISIBLE
                view.default_subject_value.text = editor!!.getLessonInfo()
            }

            editor!!.getLessonInfo()?.let {
                view.default_subject.visibility = View.VISIBLE
                view.default_subject_value.text = it
            } ?: let { view.default_subject.visibility = View.GONE }
        }
    }

    override fun onPause() {
        super.onPause()
        editor?.abortMessages()
    }
}