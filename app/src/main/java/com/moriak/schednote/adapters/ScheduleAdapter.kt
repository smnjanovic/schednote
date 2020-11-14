package com.moriak.schednote.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.database.data.LessonData
import com.moriak.schednote.settings.LessonTimeFormat
import kotlinx.android.synthetic.main.lesson_data.view.*

class ScheduleAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = App.data.timetable()
    private lateinit var showDialog: (LessonData) -> Unit

    private var onUpdate = View.OnClickListener {
        showDialog(items[(it.tag as ScheduleHolder).adapterPosition])
    }
    private var onDelete = View.OnClickListener {
        val pos = (it.tag as ScheduleHolder).adapterPosition
        if (App.data.removeFromSchedule(items[pos].order) > 0) {
            val oldSize = items.size
            items.clear()
            items.addAll(App.data.timetable())
            notifyItemRangeRemoved(pos, oldSize - items.size)
        }
    }

    fun setShowDialog(fn: (LessonData) -> Unit) {
        showDialog = fn
    }

    fun insert(lesDur: Int, breakDur: Int) {
        val id = App.data.insertIntoSchedule(lesDur, breakDur)
        if (id > -1) {
            items.add(LessonData(id, lesDur, breakDur))
            notifyItemInserted(items.lastIndex)
        } else App.toast(R.string.busy_day_error)
    }

    fun update(order: Int, lesDur: Int, breakDur: Int) {
        val upd = App.data.updateSchedule(order, lesDur, breakDur)
        if (upd > 0) {
            items[order - 1].lessonDuration = lesDur
            items[order - 1].breakDuration = breakDur
            notifyItemRangeChanged(order - 1, items.size - order + 1)
        } else App.toast(R.string.busy_day_error)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        ScheduleHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.lesson_data, parent, false)
        )

    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
        (holder as ScheduleHolder).bind()

    inner class ScheduleHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind() {
            val item = items[adapterPosition]
            itemView.order.text = item.order.toString()

            val range = App.data.scheduleRangeToMinuteRange(item.order..item.order)!!
            itemView.start.text = LessonTimeFormat.timeFormat(range.first)
            itemView.end.text = LessonTimeFormat.timeFormat(range.last)

            itemView.les_time_edit.tag = this
            itemView.les_time_delete.tag = this

            itemView.les_time_edit.setOnClickListener(onUpdate)
            itemView.les_time_delete.setOnClickListener(onDelete)
        }
    }
}