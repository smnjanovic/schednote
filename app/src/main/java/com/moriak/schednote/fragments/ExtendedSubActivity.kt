package com.moriak.schednote.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.viewbinding.ViewBinding
import com.moriak.schednote.R
import com.moriak.schednote.interfaces.ISubContent
import com.moriak.schednote.interfaces.ISubContent.ISubContentCompanion

/**
 * Fragmenty tohto typu majú vnorené obsahy v podobe ďaľších fragmentov,
 * z ktorých môže byť súčasne zobrazený iba jeden.
 */
abstract class ExtendedSubActivity<B: ViewBinding>(private val contents: ISubContentCompanion) : SubActivity<B>() {
    private companion object { private const val CONTENT = "CONTENT" }
    private val transparent = Color.TRANSPARENT
    private val inactiveText by lazy { resources.getColor(R.color.textColor, null) }
    private val activeButton by lazy { resources.getColor(R.color.colorPrimary, null) }
    private val fragmentTag = "${javaClass.canonicalName}.$CONTENT"
    private val choice = View.OnClickListener { v ->
        contents.values.find { it.button == v.id }?.let(this::setActiveTab)
    }

    private fun getButton(tab: ISubContent): TextView = requireView().findViewById(tab.button)

    private fun modifyButton (tab: ISubContent, active: Boolean) = getButton(tab).let {
        it.foreground?.setTint(if (active) activeButton else transparent)
        it.setTextColor(if (active) activeButton else inactiveText)
    }

    private fun setActiveTab(tab: ISubContent, reload: Boolean = true) {
        modifyButton(contents.lastSet, false)
        modifyButton(tab, true)
        if (reload) requireActivity().supportFragmentManager.beginTransaction()
            .replace(contents.container, tab.fragmentClass.newInstance(), fragmentTag)
            .commit()
        tab.remember()
    }

    final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setActiveTab(contents.lastSet, savedInstanceState == null)
        contents.values.forEach { getButton(it).setOnClickListener(choice) }
    }

    final override fun removeAllSubFragments() {
        findFragment<SubActivity<*>>(fragmentTag)?.let {
            requireActivity().supportFragmentManager.beginTransaction().remove(it).commit()
        }
    }
}