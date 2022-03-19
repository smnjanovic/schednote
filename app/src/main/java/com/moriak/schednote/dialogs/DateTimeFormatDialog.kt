package com.moriak.schednote.dialogs

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import com.moriak.schednote.R
import com.moriak.schednote.databinding.DateTimeFormatBinding
import com.moriak.schednote.enums.DateFormat
import com.moriak.schednote.enums.TimeFormat
import com.moriak.schednote.storage.Prefs.Settings.dateFormat
import com.moriak.schednote.storage.Prefs.Settings.timeFormat
import com.moriak.schednote.views.OptionStepper

/**
 * Dialógové okno umožňuje nastaviť formát dátumu a času
 */
class DateTimeFormatDialog : CustomBoundDialog<DateTimeFormatBinding>() {
    private companion object { private const val STORED = "OPTIONS_INDEXES" }
    private class Format(private val context: Context): OptionStepper.Format {
        override fun getItemDescription(item: Any?): String = when (item) {
            is DateFormat.Order -> item.name
            is DateFormat.Split -> context.getString(item.res)
            is TimeFormat -> context.getString(item.res)
            else -> ""
        }
    }

    override val negativeButton = ActionButton(R.string.abort) {}
    override val positiveButton = ActionButton(R.string.confirm) {
        val d = DateFormat.values().find {
            it.order == binding.dateOrderSelect.option &&
                    it.split == binding.dateSeparatorSelect.option
        }!!
        val t = binding.timeFormatSelect.option as TimeFormat
        confirm(d, t)
    }
    private lateinit var format: Format
    private var confirm = fun(_: DateFormat, _: TimeFormat) = Unit

    /**
     * Nastaví, čo sa má stať, keď potvrdím zmeny
     * @param fn metóda, ktorá sa po potvrdení dialógu vykoná. Jej argumenty sú:
     *  [DateFormat] Nastavenie formátu dátumu
     *  [TimeFormat] Nastavenie formátu času
     */
    fun setOnConfirm(fn: (DateFormat, TimeFormat) -> Unit) { confirm = fn }

    override fun setupBinding(inflater: LayoutInflater) = DateTimeFormatBinding.inflate(inflater)

    override fun setupContent(saved: Bundle?) {
        binding.dateOrderSelect.setOptions(DateFormat.Order.values())
        binding.dateSeparatorSelect.setOptions(DateFormat.Split.values())
        binding.timeFormatSelect.setOptions(TimeFormat.values())
        saved?.getIntArray(STORED)?.let {
            binding.dateOrderSelect.index = it[0]
            binding.dateSeparatorSelect.index = it[1]
            binding.timeFormatSelect.index = it[2]
        } ?: let {
            binding.dateOrderSelect.index = dateFormat.order.ordinal
            binding.dateSeparatorSelect.index = dateFormat.split.ordinal
            binding.timeFormatSelect.index = timeFormat.ordinal
        }
        format = Format(binding.root.context)
        binding.dateOrderSelect.setFormat(format)
        binding.dateSeparatorSelect.setFormat(format)
        binding.timeFormatSelect.setFormat(format)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val intArr = intArrayOf(
            binding.dateOrderSelect.index,
            binding.dateSeparatorSelect.index,
            binding.timeFormatSelect.index
        )
        outState.putIntArray(STORED, intArr)
    }
}