package com.moriak.schednote.fragments

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.moriak.schednote.activities.MainActivity

/**
 * Fragment tohto typu je podobsahom aktivity alebo iného fragmentu
 */
abstract class SubActivity : Fragment() {
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

    override fun onResume() {
        super.onResume()
        if (activity is MainActivity) (activity as MainActivity).introduce()
    }
}