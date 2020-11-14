package com.moriak.schednote.fragments.of_alarm

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.moriak.schednote.R
import com.moriak.schednote.adapters.AlarmAdapter
import com.moriak.schednote.dialogs.MinuteHourAdvance
import com.moriak.schednote.dialogs.TimeDialog
import com.moriak.schednote.fragments.of_main.SubActivity
import com.moriak.schednote.other.Day
import com.moriak.schednote.settings.Prefs
import com.moriak.schednote.settings.Regularity
import kotlinx.android.synthetic.main.alarm_setter.view.*

class AlarmClockSetter : SubActivity(), AlarmDisplay {
    private companion object {
        const val ADVANCE = "ADVANCE"
        const val DAY = "DAY"
        const val TIME = "TIME"
    }

    private var regularity: Regularity = Regularity.currentWeek
    private var adapter = AlarmAdapter()

    private fun setRegularity(reg: Regularity) {
        when (reg) {
            Regularity.EVERY -> {
                view?.odd_week?.visibility = View.GONE
                view?.even_week?.visibility = View.GONE
            }
            Regularity.EVEN -> {
                view?.odd_week?.visibility = View.VISIBLE
                view?.even_week?.visibility = View.VISIBLE
                view?.odd_week?.alpha = 0.5F
                view?.even_week?.alpha = 1F
            }
            Regularity.ODD -> {
                view?.odd_week?.visibility = View.VISIBLE
                view?.even_week?.visibility = View.VISIBLE
                view?.odd_week?.alpha = 1F
                view?.even_week?.alpha = 0.5F
            }
        }
        regularity = reg
        adapter.loadRegularity(reg)
    }

    private fun setAlarmsBySchedule(minuteAdvance: Int) {
        Prefs.states.lastScheduleStartAdvance = minuteAdvance
        Prefs.notifications.setAlarmsBySchedule()
        adapter.notifyItemRangeChanged(0, adapter.itemCount)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.setTitle(R.string.alarms)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.alarm_setter, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.all_alarms_switch.isChecked = Prefs.notifications.alarmsEnabled
        view.all_alarms_switch.setOnCheckedChangeListener { _, isChecked ->
            if (Prefs.notifications.alarmsEnabled != isChecked) {
                Prefs.notifications.alarmsEnabled = isChecked
                adapter.notifyItemRangeChanged(0, adapter.itemCount)
            }
        }

        view.auto_alarm_set.setOnClickListener {
            val dialog = MinuteHourAdvance()
            dialog.setOnConfirm { setAlarmsBySchedule(it) }
            dialog.show(fragmentManager!!, ADVANCE)
        }

        findFragment(
            ADVANCE,
            MinuteHourAdvance::class.java
        )?.setOnConfirm { setAlarmsBySchedule(it) }

        view.odd_week.tag = Regularity.ODD
        view.even_week.tag = Regularity.EVEN
        val regChoice = View.OnClickListener { setRegularity(it.tag as Regularity) }
        view.odd_week.setOnClickListener(regChoice)
        view.even_week.setOnClickListener(regChoice)
        view.alarms_list.layoutManager = LinearLayoutManager(view.alarms_list.context)
        setRegularity(regularity)
        view.alarms_list.adapter = adapter
        adapter.setOnChangeAttempt { day, dayMinutes: Int ->
            val dialog = TimeDialog()
            dialog.setMinutes(dayMinutes - Prefs.states.lastScheduleStartAdvance)
            dialog.accessBundle().putInt(DAY, day.value)
            dialog.setOnConfirm { h, m ->
                val d = Day[dialog.accessBundle().getInt(DAY)]
                Prefs.notifications.setAlarm(d, regularity, h, m)
                adapter.updateDay(d)
            }
            dialog.show(fragmentManager!!, TIME)
        }
        adapter.setOnAllAlarmsDisabled { view.all_alarms_switch.isChecked = it }
    }
}