package com.moriak.schednote.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewbinding.ViewBinding
import com.moriak.schednote.activities.MainActivity

/**
 * Fragment tohto typu je podobsahom aktivity alebo iného fragmentu
 */
abstract class SubActivity<B: ViewBinding> : Fragment() {

    private var _binding: B? = null
    protected val binding: B get() = _binding!!
    protected val isBound: Boolean get() = _binding != null

    /**
     * Zobrazenie obsahu dialógoveho fragmentu [dialog], ak ešte zobrazený nie je
     * @param tag Kľúč, pod ktorým fragment možno nájsť.
     * @param dialog ak už je v zozname fragmentov vo [FragmentManager]-i, tak je už zobrazený
     */
    protected fun showDialog(tag: String?, dialog: DialogFragment?) {
        val fm: FragmentManager = requireActivity().supportFragmentManager
        if (dialog != null && fm.findFragmentByTag(tag) != dialog) dialog.show(fm, tag)
    }

    /**
     * Nájdenie fragmentu ak je k dispozícii vo [FragmentManager]-i
     * @param tag Kľúč, pod ktorým fragment možno nájsť.
     */
    protected inline fun <reified T: Fragment> findFragment(tag: String?): T? =
        requireActivity().supportFragmentManager.findFragmentByTag(tag)?.let { if (it is T) it else null }

    /**
     * Odstránenie všetkých vnorených fragmentov v tomto fragmente
     */
    open fun removeAllSubFragments() = Unit

    protected abstract fun makeBinder(inflater: LayoutInflater, container: ViewGroup?): B

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = makeBinder(inflater, container).let {
        _binding = it
        it.root
    }

    @CallSuper
    override fun onResume() {
        super.onResume()
        if (activity is MainActivity) (activity as MainActivity).introduce()
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}