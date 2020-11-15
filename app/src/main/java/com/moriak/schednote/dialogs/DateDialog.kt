package com.moriak.schednote.dialogs

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import com.moriak.schednote.App
import java.util.Calendar.*

/**
 * Trieda zobrazuje dialog na nastavenie dátumu. Pred zobrazením do nej možno vložiť predpripravený dátum na úpravu
 */
class DateDialog : DialogFragment(), DatePickerDialog.OnDateSetListener {
    companion object {
        private const val CAL = "CAL"

        /**
         * Vytvorenie nového dialógu
         * @param ms počiatočný dátum
         * @param fn algoritmus, ktorý sa má vykonať po potvrdení dátumu
         * @return [DateDialog]
         */
        fun newInstance(ms: Long?, fn: (Long) -> Unit): DateDialog = DateDialog().apply {
            setMillis(ms)
            setOnConfirm(fn)
        }
    }

    private var onConfirm: (Long) -> Unit = fun(_) {}
    private var millis = System.currentTimeMillis()

    /**
     * Nastavenie dátumu v milisekundách
     * @param ms milisekundy. Ak je hodnota null, nastaví sa súčasný čas
     */
    fun setMillis(ms: Long?) {
        millis = ms ?: System.currentTimeMillis()
        if (dialog is DatePickerDialog) {
            App.cal.timeInMillis = millis
            (dialog as DatePickerDialog).updateDate(
                App.cal.get(YEAR),
                App.cal.get(MONTH),
                App.cal.get(DAY_OF_MONTH)
            )
        }
    }

    /**
     * Udalost nastavitelna zvonka
     * @param fn Metóda, ktorá sa má vykonať po tom ako bol dátum nastavený
     */
    fun setOnConfirm(fn: (Long) -> Unit) {
        onConfirm = fn
    }

    /**
     * Vytvorenie dialogoveho okna, aplikovanie menitelnej udalosti
     * @param saved dáta, ktoré sa zachovali pred ukončením fragmentu systémom
     * @return [Dialog] Vráti dialóg na zobrazenie
     */
    override fun onCreateDialog(saved: Bundle?): Dialog {
        activity ?: throw(NullPointerException("Activity must have been destroyed!"))
        millis = saved?.getLong(CAL) ?: System.currentTimeMillis()
        App.cal.timeInMillis = millis
        return DatePickerDialog(
            activity!!,
            this,
            App.cal.get(YEAR),
            App.cal.get(MONTH),
            App.cal.get(DAY_OF_MONTH)
        )
    }

    /**
     * pokus o nastavenie datumu
     * @param view Dialóg na výber dátumu
     * @param year Nastavenie roku
     * @param month Nastavenie mesiaca
     * @param day Nastavenie casu
     */
    override fun onDateSet(view: DatePicker?, year: Int, month: Int, day: Int) {
        App.cal.timeInMillis = 0
        App.cal.set(year, month, day)
        onConfirm(App.cal.timeInMillis)
    }

    /**
     * po otoceni displeja dialog ostane viditelny
     * @param outState balik zalohovanych dat
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(CAL, millis)
    }
}