package com.moriak.schednote.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.recyclerview.widget.RecyclerView
import com.moriak.schednote.R
import com.moriak.schednote.other.Day
import com.moriak.schednote.settings.Prefs
import com.moriak.schednote.settings.Regularity
import com.moriak.schednote.settings.WorkWeek
import kotlinx.android.synthetic.main.day_alarm.view.*

class AlarmAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val workWeek: WorkWeek = Prefs.settings.workWeek
    private var regularity: Regularity = Regularity.currentWeek
    private var onAllAlarmsEnableChange: (Boolean) -> Unit = fun(_) {}
    private val onEnableChange = CompoundButton.OnCheckedChangeListener { switch, enable ->
        val before = Prefs.notifications.alarmsEnabled
        Prefs.notifications.enableAlarm((switch.tag as AlarmHolder).day, regularity, enable)
        val now = Prefs.notifications.alarmsEnabled
        if (before != now) onAllAlarmsEnableChange(now)
    }
    private var onChangeAttempt = fun(_: Day, _: Int) = Unit
    private val onSetTime = View.OnClickListener {
        val holder = it.tag as AlarmHolder
        onChangeAttempt(holder.day, Prefs.notifications.getAlarm(holder.day, regularity))
    }

    fun setOnChangeAttempt(fn: (Day, Int) -> Unit) {
        onChangeAttempt = fn
    }

    fun setOnAllAlarmsDisabled(fn: (Boolean) -> Unit) {
        onAllAlarmsEnableChange = fn
    }

    fun loadRegularity(reg: Regularity) {
        if (regularity != reg) {
            regularity = reg
            notifyItemRangeChanged(0, workWeek.days.size)
        }
    }

    fun updateDay(day: Day) {
        val pos = workWeek.days.indexOf(day)
        if (pos > -1) notifyItemChanged(pos)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        AlarmHolder(LayoutInflater.from(parent.context).inflate(R.layout.day_alarm, parent, false))

    override fun getItemCount(): Int = workWeek.days.size
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
        (holder as AlarmHolder).bind()

    private inner class AlarmHolder(view: View) : RecyclerView.ViewHolder(view) {
        lateinit var day: Day
        fun bind() {
            day = workWeek.days[adapterPosition]
            itemView.on_off.tag = this
            itemView.change_time.tag = this
            itemView.alarm_time.tag = this

            itemView.alarm_day.text = day.toString()
            itemView.alarm_time.text =
                Prefs.settings.getTimeString(Prefs.notifications.getAlarm(day, regularity))
            itemView.on_off.isChecked = Prefs.notifications.isAlarmEnabled(day, regularity)

            itemView.alarm_time.setOnClickListener(onSetTime)
            itemView.change_time.setOnClickListener(onSetTime)
            itemView.on_off.setOnCheckedChangeListener(onEnableChange)
        }
    }
}