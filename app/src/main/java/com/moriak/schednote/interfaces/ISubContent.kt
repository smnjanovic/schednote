package com.moriak.schednote.interfaces

import com.moriak.schednote.fragments.SubActivity

/**
 * Objekty tohto typu reprezentujú vnorené obsahy k nadradenému obsahu. Musia byť typu [Enum].
 * @property button ID tlačidla, ktoré je určené k tomu, aby sa po kliknutí naň zobrazil daný obsah
 * @property fragmentClass Trieda fragmentu, ktorý má daný obsah zobraziť
 */
interface ISubContent {
    val parent: ISubContent?
    val button: Int
    val fragmentClass: Class<out SubActivity<*>>

    fun remember()

    /**
     * Objekty tohto typu by mali byť spoločníkmi (Companion) tried typov [ISubContent].
     * @property container ID bloku, v ktorom bude obsah [ISubContent] zobrazený.
     * @property values Inštancie triedy [ISubContent], ktorej je táto trieda spoločníkom (Companion).
     * @property layout ID layout-u, v ktorom sa blok [container] má nachádzať.
     */
    interface ISubContentCompanion {
        val container: Int
        val values: Array<out ISubContent>
        val lastSet: ISubContent
    }
}
