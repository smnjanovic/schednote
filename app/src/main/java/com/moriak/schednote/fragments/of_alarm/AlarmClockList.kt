package com.moriak.schednote.fragments.of_alarm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.CompoundButton
import androidx.appcompat.app.AlertDialog
import com.moriak.schednote.R
import com.moriak.schednote.adapters.AlarmAdapter
import com.moriak.schednote.dayMinutes
import com.moriak.schednote.dialogs.AlarmClockAdvance
import com.moriak.schednote.dialogs.DateTimeDialog
import com.moriak.schednote.dialogs.DateTimeDialog.Companion.FLAG_TIME
import com.moriak.schednote.enums.Day
import com.moriak.schednote.enums.Regularity
import com.moriak.schednote.fragments.ListSubActivity
import com.moriak.schednote.getRegularity
import com.moriak.schednote.notifications.AlarmClockSetter
import com.moriak.schednote.now
import com.moriak.schednote.storage.Prefs.Settings.dualWeekSchedule
import com.moriak.schednote.storage.Prefs.Settings.workWeek
import kotlinx.android.synthetic.main.alarm_setter.*
import kotlinx.android.synthetic.main.alarm_setter.view.*
import java.util.*

/**
 * V tomto fragmente sa nastavujú budíky. V adaptéri sa zobrazia budíky pre nastavený pracovný deň
 * a ak je povolený 2-týždenný rozvrh dá sa prepínať medzi budíkmi párneho a nepárneho týždňa
 */
class AlarmClockList : ListSubActivity<Day>(R.layout.alarm_setter, R.id.alarms_list, 0, 0, false) {
    private companion object {
        const val ADVANCE = "ADVANCE"
        const val DAY = "DAY"
        const val TIME = "TIME"
    }

    private val cal = Calendar.getInstance()

    private var regularity: Regularity = cal.now.getRegularity(workWeek, dualWeekSchedule)

    private var editingDay: Day? = null

    private val regChoice = View.OnClickListener { setRegularity(it.tag as Regularity) }

    private val alarmOnOff = CompoundButton.OnCheckedChangeListener { v, b ->
        if (AlarmClockSetter.isEnabled(regularity) xor b) {
            val success = AlarmClockSetter.enableAlarms(v.context, regularity, b)
            adapter.notifyItemRangeChanged(0, adapter.itemCount)
            if (!success) cantSetAlarm()
        }
    }

    override val adapter: AlarmAdapter = AlarmAdapter(regularity)

    private fun cantSetAlarm() {
        AlertDialog.Builder(requireContext())
            .setMessage(R.string.cant_set_exact_alarm)
            .setPositiveButton(R.string.permission_grant) { _, _ ->
                if (android.os.Build.VERSION.SDK_INT >= 31) {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                }
            }
            .setNegativeButton(R.string.abort) { _, _ -> }
            .show()
    }

    private fun setRegularity(reg: Regularity) {
        view?.odd_week?.visibility = if (reg == Regularity.EVERY) View.GONE else View.VISIBLE
        view?.even_week?.visibility = if (reg == Regularity.EVERY) View.GONE else View.VISIBLE
        view?.odd_week?.alpha = if (reg == Regularity.ODD) 1F else 0.5F
        view?.even_week?.alpha = if (reg == Regularity.EVEN) 1F else 0.5F
        if (regularity != reg) {
            regularity = reg
            adapter.setRegularity(reg)
            view?.all_alarms_switch?.isChecked = AlarmClockSetter.isEnabled(reg)
        }
    }

    private fun setAlarm(ms: Long?) {
        val success = ms?.let {
            cal.timeInMillis = ms
            AlarmClockSetter.setAlarm(requireContext(), editingDay!!, regularity, cal.dayMinutes)
        } ?: AlarmClockSetter.enableAlarm(requireContext(), editingDay!!, regularity, false)
        val index = adapter.findIndexOf(fun(item, day) = item == day, editingDay)
        adapter.notifyItemChanged(index)
        if (!success) cantSetAlarm()
    }

    private fun onAdvanceSet() {
        val success = AlarmClockSetter.setAlarmsBySchedule(requireContext())
        adapter.notifyItemRangeChanged(0, adapter.itemCount)
        all_alarms_switch.isChecked = AlarmClockSetter.isEnabled(regularity)
        if (!success) cantSetAlarm()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.setTitle(R.string.alarms)
    }

    override fun onItemAction(pos: Int, data: Bundle, code: Int) {
        if (code == AlarmAdapter.ACTION_ALARM_SET) {
            editingDay = adapter.getItemAt(pos)
            cal.now.dayMinutes = AlarmClockSetter.getAlarm(adapter.getItemAt(pos), regularity)
            showDialog(TIME, DateTimeDialog(cal.timeInMillis, FLAG_TIME).setOnConfirm(this::setAlarm))
        }
        else {
            val onOff = code == AlarmAdapter.ACTION_ALARM_ON
            val success = AlarmClockSetter.enableAlarm(requireContext(), adapter.getItemAt(pos), regularity, onOff)
            requireView().all_alarms_switch.isChecked = AlarmClockSetter.isEnabled(regularity)
            if (!success) cantSetAlarm()
        }
    }

    override fun firstLoad(): List<Day> = workWeek.workDays.toList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        editingDay = savedInstanceState?.getInt(DAY)?.let { if (it == 0) null else Day[it] }

        findFragment<AlarmClockAdvance>(ADVANCE)?.setOnConfirm(this::onAdvanceSet)
        findFragment<DateTimeDialog>(TIME)?.setOnConfirm(this::setAlarm)

        view.odd_week.tag = Regularity.ODD
        view.even_week.tag = Regularity.EVEN
        setRegularity(regularity)
        view.all_alarms_switch.isChecked = AlarmClockSetter.isEnabled(regularity)

        view.all_alarms_switch.setOnCheckedChangeListener(alarmOnOff)
        view.auto_alarm_set.setOnClickListener {
            val dialog = AlarmClockAdvance()
            dialog.setOnConfirm(this::onAdvanceSet)
            showDialog(ADVANCE, dialog)
        }
        view.odd_week.setOnClickListener(regChoice)
        view.even_week.setOnClickListener(regChoice)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        editingDay?.let { outState.putInt(DAY, it.value) }
    }
}