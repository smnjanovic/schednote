package com.moriak.schednote.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.moriak.schednote.storage.Prefs.Settings.dateFormat
import com.moriak.schednote.storage.Prefs.Settings.timeFormat
import com.moriak.schednote.R
import com.moriak.schednote.enums.DateFormat
import com.moriak.schednote.enums.TimeFormat
import com.moriak.schednote.views.OptionStepper
import kotlinx.android.synthetic.main.date_time_format.view.*

/**
 * Dialógové okno umožňuje nastaviť formát dátumu a času
 */
class DateTimeFormatDialog : DialogFragment() {
    private companion object { private const val STORED = "OPTIONS_INDEXES" }
    private class Format(private val context: Context): OptionStepper.Format {
        override fun getItemDescription(item: Any?): String = when (item) {
            is DateFormat.Order -> item.name
            is DateFormat.Split -> context.getString(item.res)
            is TimeFormat -> context.getString(item.res)
            else -> ""
        }
    }
    private lateinit var root: View
    private lateinit var format: Format
    private var confirm = fun(_: DateFormat, _: TimeFormat) = Unit

    private fun inflate(layout: Int, root: ViewGroup? = null) = LayoutInflater
        .from(requireContext()).inflate(layout, root, false)

    private fun buildView(savedInstanceState: Bundle?): View {
        root = inflate(R.layout.date_time_format)
        root.date_order_select.setOptions(DateFormat.Order.values())
        root.date_separator_select.setOptions(DateFormat.Split.values())
        root.time_format_select.setOptions(TimeFormat.values())
        savedInstanceState?.getIntArray(STORED)?.let {
            root.date_order_select.index = it[0]
            root.date_separator_select.index = it[1]
            root.time_format_select.index = it[2]
        } ?: let {
            root.date_order_select.index = dateFormat.order.ordinal
            root.date_separator_select.index = dateFormat.split.ordinal
            root.time_format_select.index = timeFormat.ordinal
        }
        format = Format(root.context)
        root.date_order_select.setFormat(format)
        root.date_separator_select.setFormat(format)
        root.time_format_select.setFormat(format)
        return root
    }

    /**
     * Nastaví, čo sa má stať, keď potvrdím zmeny
     * @param fn metóda, ktorá sa po potvrdení dialógu vykoná. Jej argumenty sú:
     *  [DateFormat] Nastavenie formátu dátumu
     *  [TimeFormat] Nastavenie formátu času
     */
    fun setOnConfirm(fn: (DateFormat, TimeFormat) -> Unit) { confirm = fn }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog
            .Builder(requireContext())
            .setView(buildView(savedInstanceState))
            .setPositiveButton(R.string.confirm, fun(_, _) {
                val d = DateFormat.values().find {
                    it.order == root.date_order_select.option &&
                            it.split == root.date_separator_select.option
                }!!
                val t = root.time_format_select.option as TimeFormat
                confirm(d, t)
            })
            .setNegativeButton(R.string.abort, fun(_, _) = Unit)
            .create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val intArr = intArrayOf(
            root.date_order_select.index,
            root.date_separator_select.index,
            root.time_format_select.index
        )
        outState.putIntArray(STORED, intArr)
    }
}