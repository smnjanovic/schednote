package com.moriak.schednote.adapters

import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import com.moriak.schednote.storage.Prefs
import com.moriak.schednote.R
import com.moriak.schednote.enums.Day
import com.moriak.schednote.enums.Regularity
import com.moriak.schednote.notifications.AlarmClockSetter
import kotlinx.android.synthetic.main.day_alarm.view.*

/**
 * Adapter zobrazuje budíky pracovné dni v týždni. Na každý deň možno nastaviť max. 1 budík.
 */
class AlarmAdapter(private var regularity: Regularity): CustomAdapter<Day>(R.layout.day_alarm) {
    /**
     * @property ACTION_ALARM_ON Označenie pre pokus o zapnutie budíka
     * @property ACTION_ALARM_OFF Označenie pre pokus o vypnutie budíka
     * @property ACTION_ALARM_SET Označenie pre pokus o nastavenie času budenia
     */
    companion object {
        const val ACTION_ALARM_ON = 1990
        const val ACTION_ALARM_OFF = 1991
        const val ACTION_ALARM_SET = 1992
    }
    private var clickAction = View.OnClickListener {
        val h = it.tag as AlarmHolder
        triggerItemAction(h.adapterPosition, extras, ACTION_ALARM_SET)
    }
    private var switchAction = CompoundButton.OnCheckedChangeListener { btn, b ->
        val h = btn.tag as AlarmHolder
        triggerItemAction(h.adapterPosition, extras, if (b) ACTION_ALARM_ON else ACTION_ALARM_OFF)
    }

    /**
     * Nastaviť týždeň pre ktorý sa budú zobrazovať budíky
     * @param reg týždeň (párny / nepárny / každý)
     */
    fun setRegularity(reg: Regularity) {
        regularity = reg
        if (itemCount > 0) notifyItemRangeChanged(0, itemCount)
    }

    override fun instantiateViewHolder(v: View): CustomViewHolder = AlarmHolder(v)
    override fun bundleToItem(bundle: Bundle): Day = throw Exception("Unimplemented!!!")
    override fun itemToBundle(item: Day, bundle: Bundle) { throw Exception("Unimplemented!!!") }

    /**
     * Objekt vizualizuje položku zoznamu (deň budenia)
     */
    inner class AlarmHolder(v: View): CustomViewHolder(v) {
        override fun bind(pos: Int) {
            itemView.alarm_day.text = itemView.context.getString(item!!.res)
            val minutes = AlarmClockSetter.getAlarm(item!!, regularity)
            itemView.alarm_time.text = Prefs.Settings.timeFormat.getFormat(minutes)
            itemView.alarm_edit.tag = this
            itemView.alarm_on_off.tag = this
            itemView.alarm_on_off.isChecked = AlarmClockSetter.isEnabled(item!!, regularity)
            itemView.alarm_edit.setOnClickListener(clickAction)
            itemView.alarm_on_off.setOnCheckedChangeListener(switchAction)
        }
    }
}