package com.moriak.schednote.storage

import android.database.sqlite.SQLiteDatabase
import java.util.*

/**
 * Trieda tohto typu je reprezentantom tabuľky v SQLite. má za úlohu poznať názvy stĺpcov,
 * vytvoriť danú tabuľku, pokiaľ neexistuje, príp. k nej pridať triggre.
 */
abstract class Table {
    private val table = javaClass.simpleName.replace("(.)([A-Z])".toRegex(), "$1_$2").lowercase()

    /**
     * @return Názov tabuľky
     */
    final override fun toString() = table

    /**
     * Vytvorenie tabuľky
     * @param db Databáza, v ktorej tabuľku vytvorím
     */
    abstract fun create(db: SQLiteDatabase)

    /**
     * Pridanie triggrov do tabuľky
     * @param db Databáza, v ktorej triggre vytvorím
     */
    open fun setupTriggers(db: SQLiteDatabase) = Unit

    /**
     * Odstránenie tabuľky
     * @param db Databáza, z ktorej tabuľku odstránim
     */
    fun drop(db: SQLiteDatabase) = db.execSQL("DROP TABLE IF EXISTS $this;")

    /**
     * Vyhodenie výnimky s hlásenim [msg].
     * @param msg chybové hlásenie
     * @return SQL výraz, ktorý ukončí chod sql s chybovým hlásením
     */
    protected fun abort(msg: String): String = "SELECT RAISE(ABORT, '$msg');"
}