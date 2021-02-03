package com.moriak.schednote.fragments.of_main

import android.content.Context
import androidx.annotation.IdRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.moriak.schednote.activities.MainActivity

/**
 * Fragment predstavuje vnorený obsah v aktivite alebo vo fragmente
 */
abstract class SubActivity : Fragment() {

    /**
     * Zobrazenie dialógoveho fragmentu pokiaľ existuje, je dialógom a líši sa od parametra [dialog]
     * @param tag Označenie fragmentu
     * @param dialog ak je tento dialóg na zozname fragmentov v Fragment Manageri, tak je už zobrazený, inak zobraziť!
     */
    protected fun showDialog(tag: String?, dialog: DialogFragment?) {
        if (dialog != null && findFragment(tag) != dialog)
            fragmentManager?.let { dialog.show(it, tag) }
    }

    /**
     * Ak fragment [fragment] nie je medzi fragmentami pod značkou [tag] dôjde k výmene fragmentov.
     * @param resId odkaz na kontajner
     * @param fragment Fragment
     * @param tag označenie fragmentu
     */
    protected fun attachFragment(@IdRes resId: Int, fragment: Fragment?, tag: String?) {
        if (fragment != null && findFragment(tag) != fragment)
            fragmentManager?.beginTransaction()?.replace(resId, fragment, tag)?.commit()
    }

    /**
     * Dôjde k výmene fragmentov, aj keď sú rovnaké
     * @param resId odkaz na kontajner
     * @param fragment Fragment
     * @param tag označenie fragmentu
     */
    protected fun forceAttachFragment(resId: Int, fragment: Fragment?, tag: String?) {
        if (fragment != null)
            fragmentManager?.beginTransaction()?.replace(resId, fragment, tag)?.commit()
    }

    /**
     * Odstránenie fragmentu
     * @param tag Značka
     * @param metaClass Trieda
     */
    protected fun removeFragment(tag: String?, metaClass: Class<out Fragment>) {
        findFragment(tag, metaClass)?.let {
            activity?.supportFragmentManager?.beginTransaction()?.remove(it)?.commit()
        }
    }

    /**
     * Nájdenie fragmentu ak je k dispozícii vo FragmentManageri
     * @param tag
     * @param metaClass
     */
    @Suppress("UNCHECKED_CAST")
    protected fun <T> findFragment(tag: String?, metaClass: Class<out T>): T? {
        val t = findFragment(tag) as T?
        return if (metaClass.isInstance(t)) t else null
    }

    private fun findFragment(tag: String?) =
        activity?.supportFragmentManager?.findFragmentByTag(tag)

    /**
     * Nájdenie fragmentu v FragmentManageri alebo vytvoriť nový
     * @param tag
     * @param metaClass
     */
    protected fun <T> requireFragment(tag: String?, metaClass: Class<out T>): T =
        findFragment(tag, metaClass) ?: metaClass.newInstance()

    /**
     * Všetky inštancie su retainable - nebudú zanikať pri rotácii
     */
    override fun onAttach(context: Context) {
        super.onAttach(context)
        retainInstance = true
    }

    /**
     * Odstránenie všetkých vnorených fragmentov v tomto fragmente
     */
    open fun removeAllSubFragments() = Unit

    override fun onResume() {
        super.onResume()
        if (activity is MainActivity) (activity as MainActivity).introduce()
    }
}