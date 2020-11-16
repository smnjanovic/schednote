package com.moriak.schednote.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.database.data.LessonTime
import com.moriak.schednote.settings.Prefs
import kotlinx.android.synthetic.main.lesson_data.view.*

/**
 * Adapter zobrazuje určitý počet hodín z ktorých je rozvrh zložený a
 * udáva od kedy do kedy prebieha daná hodina
 * Hodinu možno pridávať len na koniec, a to uvedením trvania samotnej hodiny a
 * trvania prestávky po nej
 */
class LessonTimeAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = App.data.lessonTimes()
    private lateinit var showDialog: (LessonTime) -> Unit

    private var onUpdate = View.OnClickListener {
        showDialog(items[(it.tag as LessonTimeHolder).adapterPosition])
    }
    private var onDelete = View.OnClickListener {
        val pos = (it.tag as LessonTimeHolder).adapterPosition
        if (App.data.removeFromSchedule(items[pos].order) > 0) {
            val oldSize = items.size
            items.clear()
            items.addAll(App.data.lessonTimes())
            notifyItemRangeRemoved(pos, oldSize - items.size)
        }
    }

    /**
     * Touto funkciou nastavím ako sa má vytvoriť, zobraziť a správať nový dialóg
     * @param fn Vstupom sú dáta o hodine, ktoré chcem upraviť
     */
    fun setShowDialog(fn: (LessonTime) -> Unit) {
        showDialog = fn
    }

    /**
     * Vložiť hodinu
     * @param lesDur trvanie hodiny
     * @param breakDur trvanie prestávky
     */
    fun insert(lesDur: Int, breakDur: Int) {
        val id = App.data.insertIntoSchedule(lesDur, breakDur)
        if (id > -1) {
            items.add(LessonTime(id, lesDur, breakDur))
            notifyItemInserted(items.lastIndex)
        } else App.toast(R.string.busy_day_error)
    }

    /**
     * Upraviť hodinu
     * @param lesDur zmena trvania hodiny
     * @param breakDur zmena trvania prestávky
     */
    fun update(order: Int, lesDur: Int, breakDur: Int) {
        val upd = App.data.updateLessonTime(order, lesDur, breakDur)
        if (upd > 0) {
            items[order - 1].lessonDuration = lesDur
            items[order - 1].breakDuration = breakDur
            notifyItemRangeChanged(order - 1, items.size - order + 1)
        } else App.toast(R.string.busy_day_error)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        LessonTimeHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.lesson_data, parent, false)
        )

    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
        (holder as LessonTimeHolder).bind()

    inner class LessonTimeHolder(view: View) : RecyclerView.ViewHolder(view) {

        /**
         * Vyskladanie viewHoldera
         */
        fun bind() {
            val item = items[adapterPosition]
            itemView.order.text = item.order.toString()

            itemView.start.text = Prefs.settings.lessonTimeFormat.startFormat(item.order)
            itemView.end.text = Prefs.settings.lessonTimeFormat.endFormat(item.order)

            itemView.les_time_edit.tag = this
            itemView.les_time_delete.tag = this

            itemView.les_time_edit.setOnClickListener(onUpdate)
            itemView.les_time_delete.setOnClickListener(onDelete)
        }
    }
}