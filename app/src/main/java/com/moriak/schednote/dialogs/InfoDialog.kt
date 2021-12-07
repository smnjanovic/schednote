package com.moriak.schednote.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import com.moriak.schednote.R

/**
 * Dialóg slúži na informovanie alebo poučenie užívateľa
 */
class InfoDialog : DialogFragment {
    private companion object { private const val MSG = "MSG" }

    @StringRes private var msg: Int?

    constructor() { msg = null }

    constructor(@StringRes message: Int) { msg = message }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        savedInstanceState?.let { if (it.containsKey(MSG)) msg = it.getInt(MSG) }
        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.info)
            .also { msg?.let(it::setMessage) }
            .setNegativeButton(R.string.back, fun(_, _) = Unit)
            .create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        msg?.let { outState.putInt(MSG, it) }
    }
}