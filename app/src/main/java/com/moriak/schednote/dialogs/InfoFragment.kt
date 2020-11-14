package com.moriak.schednote.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import com.moriak.schednote.App
import com.moriak.schednote.R

class InfoFragment : DialogFragment() {
    companion object {
        const val MSG = "MSG"
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