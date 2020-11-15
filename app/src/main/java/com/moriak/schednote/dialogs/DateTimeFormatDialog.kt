package com.moriak.schednote.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.moriak.schednote.R
import com.moriak.schednote.settings.DateOrder
import com.moriak.schednote.settings.DateSeparator
import com.moriak.schednote.settings.Prefs
import com.moriak.schednote.settings.TimeFormat
import kotlinx.android.synthetic.main.date_time_format.view.*

/**
 * Dialóg nastavenia formátu dátumu a času
 */
class DateTimeFormatDialog : DialogFragment() {
    private lateinit var root: View
    private var confirm = fun(_: DateOrder, _: DateSeparator, _: TimeFormat) = Unit

    override fun onAttach(context: Context) {
        super.onAttach(context)
        retainInstance = true
    }

    @SuppressLint("InflateParams")
    private fun buildView(savedInstanceState: Bundle?): View {
        root = LayoutInflater.from(requireContext()).inflate(R.layout.date_time_format, null, false)
        fun adapter(arr: Array<out Any>) =
            ArrayAdapter(root.context, android.R.layout.simple_list_item_1, arr)

        val orders = DateOrder.values()
        root.date_order_select.adapter = adapter(orders)
        savedInstanceState
            ?: root.date_order_select.setSelection(orders.indexOf(Prefs.settings.dateOrder))

        val separators = DateSeparator.values()
        root.date_separator_select.adapter = adapter(separators)
        savedInstanceState
            ?: root.date_separator_select.setSelection(separators.indexOf(Prefs.settings.dateSeparator))

        val timeFormats = TimeFormat.values()
        root.time_format_select.adapter = adapter(timeFormats)
        savedInstanceState
            ?: root.time_format_select.setSelection(timeFormats.indexOf(Prefs.settings.timeFormat))

        return root
    }

    /**
     * Nastaví, čo sa má stať, keď potvrdím zmeny
     * @param fn metóda, ktorá sa po potvrdení dialógu vykoná. Jej argumenty sú:
     *  [DateOrder] Nastavenie poradia dňa, mesiaca a roka v dátume
     *  [DateSeparator] Nastavenie oddeľovača v dátume
     *  [TimeFormat] Nastavenie formátu času
     */
    fun setOnConfirm(fn: (DateOrder, DateSeparator, TimeFormat) -> Unit) {
        confirm = fn
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog
            .Builder(requireContext())
            .setView(buildView(savedInstanceState))
            .setPositiveButton(R.string.confirm, fun(_, _) {
                confirm(
                    root.date_order_select.selectedItem as DateOrder,
                    root.date_separator_select.selectedItem as DateSeparator,
                    root.time_format_select.selectedItem as TimeFormat
                )
            })
            .setNegativeButton(R.string.abort, fun(_, _) = Unit)
            .create()
    }
}