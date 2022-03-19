package com.moriak.schednote.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding

abstract class CustomBoundDialog<T: ViewBinding>: DialogFragment() {
    protected data class ActionButton(@StringRes val textId: Int, val action: ()->Unit = fun() = Unit)

    private var _binding: T? = null
    protected val binding: T get() = _binding!!
    @StringRes protected open val title: Int = 0
    @StringRes protected open val message: Int = 0
    protected open val positiveButton: ActionButton? = null
    protected open val neutralButton: ActionButton? = null
    protected open val negativeButton: ActionButton? = null
    protected val isBound get() = _binding != null

    protected abstract fun setupBinding(inflater: LayoutInflater): T
    protected open fun setupContent(saved: Bundle?) = Unit
    final override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = setupBinding(LayoutInflater.from(requireContext()))
        val builder = AlertDialog.Builder(requireContext()).setView(binding.root)
        if (title != 0) builder.setTitle(title)
        if (message != 0) builder.setMessage(message)
        positiveButton?.let { builder.setPositiveButton(it.textId) { _, _ -> it.action() } }
        neutralButton?.let { builder.setNeutralButton(it.textId) { _, _ -> it.action() } }
        negativeButton?.let { builder.setNegativeButton(it.textId) { _, _ -> it.action() } }
        setupContent(savedInstanceState)
        return builder.create()
    }
}