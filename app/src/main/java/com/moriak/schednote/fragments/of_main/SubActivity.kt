package com.moriak.schednote.fragments.of_main

import android.content.Context
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.moriak.schednote.activities.MainActivity

abstract class SubActivity : Fragment() {
    protected fun showDialog(tag: String?, dialog: DialogFragment?) {
        if (dialog != null && findFragment(tag) != dialog) fragmentManager?.let {
            dialog.show(
                it,
                tag
            )
        }
    }

    protected fun attachFragment(resId: Int, fragment: Fragment?, tag: String?) {
        if (fragment != null && findFragment(tag) != fragment)
            fragmentManager?.beginTransaction()?.replace(resId, fragment, tag)?.commit()
    }

    protected fun forceAttachFragment(resId: Int, fragment: Fragment?, tag: String?) {
        if (fragment != null) fragmentManager?.beginTransaction()?.replace(resId, fragment, tag)
            ?.commit()
    }

    protected fun removeFragment(tag: String?, metaClass: Class<out Fragment>) {
        findFragment(tag, metaClass)?.let {
            activity?.supportFragmentManager?.beginTransaction()?.remove(it)?.commit()
        }
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <T> findFragment(tag: String?, metaClass: Class<out T>): T? {
        val t = findFragment(tag) as T?
        return if (metaClass.isInstance(t)) t else null
    }

    protected fun findFragment(tag: String?) =
        activity?.supportFragmentManager?.findFragmentByTag(tag)

    protected fun <T> requireFragment(tag: String?, metaClass: Class<out T>): T =
        findFragment(tag, metaClass) ?: metaClass.newInstance()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        retainInstance = true
    }

    override fun onResume() {
        super.onResume()
        if (activity is MainActivity) (activity as MainActivity).introduce()
    }
}