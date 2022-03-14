package com.moriak.schednote.adapters

import android.os.Bundle
import android.view.View
import com.moriak.schednote.storage.Prefs.Settings.earliestMinute
import com.moriak.schednote.storage.Prefs.Settings.timeFormat
import com.moriak.schednote.R
import com.moriak.schednote.data.LessonTime
import kotlinx.android.synthetic.main.edit_tools.view.*
import kotlinx.android.synthetic.main.lesson_data.view.*

/**
 * Tento adaptér spravuje zoznam časových blokov rozvrhu.
 */
class LessonTimeAdapter : CustomAdapter<LessonTime>(R.layout.lesson_data) {
    /**
     * @property ACTION_EDIT Označenie pre pokus o zmenu trvania hodiny a prestávky
     * @property ACTION_DELETE Označenie pre pokus o odstránenie časového bloku
     */
    companion object {
        const val ACTION_EDIT = 1993
        const val ACTION_DELETE = 1994
        private const val ORDER = "LESSON_ORDER"
        private const val L_DUR = "LESSON_DURATION"
        private const val B_DUR = "BREAK_AFTER_LESSON"
    }

    private val clickAction = View.OnClickListener {
        val h = it.tag as LessonTimeHolder
        triggerItemAction(h.adapterPosition, extras, when (it) {
            h.itemView.edit -> ACTION_EDIT
            h.itemView.delete -> ACTION_DELETE
            else -> 0
        })
    }

    override fun instantiateViewHolder(v: View): CustomViewHolder = LessonTimeHolder(v)

    override fun bundleToItem(bundle: Bundle): LessonTime =
        bundle.let { LessonTime(it.getInt(ORDER), it.getInt(L_DUR), it.getInt(B_DUR)) }

    override fun itemToBundle(item: LessonTime, bundle: Bundle) {
        bundle.putInt(ORDER, item.order)
        bundle.putInt(L_DUR, item.lessonDuration)
        bundle.putInt(B_DUR, item.breakDuration)
    }

    /**
     * Vypočet začiatku časového bloku.
     * @param pos pozícia časového bloku
     * @return začiatok časového bloku v dňových minútach
     */
    fun computeStart(pos: Int): Int {
        var min = earliestMinute
        for (i in 0 until pos) min += items[i].let { it.lessonDuration + it.breakDuration }
        return min
    }

    /**
     * Objekt vizualizuje položku zoznamu (Časový blok rozvrhu).
     */
    inner class LessonTimeHolder(v: View): CustomViewHolder(v) {
        override fun bind(pos: Int) {
            val st = computeStart(pos)
            itemView.les_time_order.text = item!!.order.toString()
            itemView.les_time_start.text = timeFormat.getFormat(st)
            itemView.les_time_end.text = timeFormat.getFormat(st + item!!.lessonDuration)
            itemView.save.visibility = View.GONE
            itemView.cancel.visibility = View.GONE
            itemView.edit.tag = this
            itemView.delete.tag = this
            itemView.edit.setOnClickListener(clickAction)
            itemView.delete.setOnClickListener(clickAction)
        }
    }
}