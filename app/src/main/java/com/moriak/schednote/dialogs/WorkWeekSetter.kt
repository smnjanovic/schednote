package com.moriak.schednote.dialogs

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.moriak.schednote.R
import com.moriak.schednote.other.Day
import com.moriak.schednote.settings.Prefs
import com.moriak.schednote.settings.WorkWeek
import kotlinx.android.synthetic.main.spinner.view.*

class WorkWeekSetter : DialogFragment() {
    private val workWeeks = WorkWeek.values()
    private var affectedView: TextView? = null

    fun setAffectedView(textView: TextView) {
        affectedView = textView
    }

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = activity?.let { activity ->
        AlertDialog.Builder(activity).apply {
            val v = LayoutInflater.from(context).inflate(R.layout.spinner, null, false)
            v.spinner.adapter =
                ArrayAdapter(context, android.R.layout.simple_list_item_1, workWeeks)
            v.spinner.setSelection(workWeeks.indexOf(Prefs.settings.workWeek))
            setView(v)
            setPositiveButton(R.string.confirm) { _, _ ->
                val pos = v.spinner.selectedItemPosition
                if (pos !in workWeeks.indices) throw RuntimeException("Spinner is empty!")

                val item = workWeeks[pos]
                Prefs.settings.workWeek = item
                affectedView?.text = item.toString()

                val weekend = ArrayList<Day>().also { it.addAll(Day.values()) }
                val workingDays = item.days
                for (day in workingDays) weekend.remove(day)
                //po zmene tyzdna sa vymazu vyucovacie hodiny prebiehajuce cez vikendy
                //App.data.clearLessons(weekend.toTypedArray())
            }
            setNegativeButton(R.string.abort) { _, _ -> Unit }
        }.create()
    } ?: throw (Exception("Activity got destroyed!"))
}
