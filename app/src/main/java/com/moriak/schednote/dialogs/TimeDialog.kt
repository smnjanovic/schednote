package com.moriak.schednote.dialogs

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import com.moriak.schednote.settings.Prefs
import com.moriak.schednote.settings.TimeFormat

/**
 * Trieda zobrazuje dialog na nastavenie dátumu. Pred zobrazením do nej možno vložiť predpripravený dátum na úpravu
 * @property minutes Nastavenie datumu v milisekundach
 */
class TimeDialog : DialogFragment(), TimePickerDialog.OnTimeSetListener {
    companion object {
        const val MINUTES = "MINUTES"
    }

    private var onConfirm: (Int, Int) -> Unit = fun(_, _) {}
    private var minutes = 360
    private val bundle = Bundle()

    /**
     * Prístup k doplnkovým dátam
     * @return [Bundle]
     */
    fun accessBundle() = bundle

    /**
     * Nastavenie dátumu v milisekundách
     * @param min nastavenie času v minútach meraných od polnoci
     */
    fun setMinutes(min: Int) {
        minutes = min
    }

    /**
     * Udalost nastavitelna zvonka
     * @param fn Metóda, ktorá sa má vykonať po tom ako bol dátum nastavený
     */
    fun setOnConfirm(fn: (Int, Int) -> Unit) {
        onConfirm = fn
    }

    /**
     * Vytvorenie dialogoveho okna, aplikovanie menitelnej udalosti
     * @param saved dáta, ktoré sa zachovali pred ukončením fragmentu systémom
     * @return [Dialog] Vráti dialóg na zobrazenie
     */
    override fun onCreateDialog(saved: Bundle?): Dialog {
        activity ?: throw(NullPointerException("Activity must have been destroyed!"))
        saved?.let {
            minutes = it.getInt(MINUTES, minutes)
            bundle.putAll(it)
        }
        return TimePickerDialog(
            activity!!,
            this,
            minutes / 60,
            minutes % 60,
            Prefs.settings.timeFormat == TimeFormat.H24
        )
    }

    /**
     * pokus o nastavenie datumu
     * @param view Dialóg na výber dátumu
     * @param hourOfDay Nastavenie hodiny
     * @param minute Nastavenie minuty
     */
    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) =
        onConfirm(hourOfDay, minute)

    /**
     * po otoceni displeja dialog ostane viditelny
     * @param outState balik zalohovanych dat
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(MINUTES, minutes)
        outState.putAll(bundle)
    }
}