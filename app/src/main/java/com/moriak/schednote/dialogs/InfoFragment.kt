package com.moriak.schednote.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import com.moriak.schednote.App
import com.moriak.schednote.R

/**
 * Dialóg slúži na informovanie alebo poučenie užívateľa
 */
class InfoFragment : DialogFragment() {
    companion object {
        private const val MSG = "MSG"

        /**
         * Vytvorenie dialógu so špecifickou správou
         * @param res Zdroj reťazca, ktorý sa má vypísať v dialógovom okne
         * @return Informačný dialóg
         */
        fun create(@StringRes res: Int) = InfoFragment().also { it.msg = App.str(res) }
    }

    private var msg: String = ""

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        savedInstanceState?.let { msg = it.getString(MSG, msg) }
        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.info)
            .setMessage(msg)
            .setNegativeButton(R.string.back, fun(_, _) = Unit)
            .create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(MSG, msg)
    }
}