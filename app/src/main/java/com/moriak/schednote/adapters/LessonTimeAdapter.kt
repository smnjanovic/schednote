package com.moriak.schednote.adapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.moriak.schednote.R
import com.moriak.schednote.data.LessonTime
import com.moriak.schednote.databinding.LessonDataBinding
import com.moriak.schednote.storage.Prefs.Settings.earliestMinute
import com.moriak.schednote.storage.Prefs.Settings.timeFormat

/**
 * Tento adaptér spravuje zoznam časových blokov rozvrhu.
 */
class LessonTimeAdapter : CustomAdapter<LessonTime, LessonDataBinding>() {
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
        triggerItemAction(h.adapterPosition, extras, when (it.id) {
            R.id.edit -> ACTION_EDIT
            R.id.delete -> ACTION_DELETE
            else -> 0
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        LessonTimeHolder(LessonDataBinding.inflate(LayoutInflater.from(parent.context), parent, false))

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
    inner class LessonTimeHolder(b: LessonDataBinding): CustomViewHolder(b) {
        override fun bind(pos: Int) {
            val st = computeStart(pos)
            binding.lesTimeOrder.text = item!!.order.toString()
            binding.lesTimeStart.text = timeFormat.getFormat(st)
            binding.lesTimeEnd.text = timeFormat.getFormat(st + item!!.lessonDuration)
            binding.lesTools.save.visibility = View.GONE
            binding.lesTools.cancel.visibility = View.GONE
            binding.lesTools.edit.tag = this
            binding.lesTools.delete.tag = this
            binding.lesTools.edit.setOnClickListener(clickAction)
            binding.lesTools.delete.setOnClickListener(clickAction)
        }
    }
}