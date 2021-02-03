package com.moriak.schednote.database.tables

import android.database.sqlite.SQLiteDatabase
import com.moriak.schednote.App

/**
 * Reprezentácia SQLite TABLE. Potomkovia tejto triedy budú mať na starosti vytvorenie tabuľky
 * a triggrov a odstránenie tabuľky.
 */
abstract class Table {
    /**
     * @property FK_ERR Chybové hlásenie o neplatnosti cudzieho kľúča
     */
    companion object {
        const val FK_ERR = "Invalid foreign key!"
    }

    private val table: String

    init {
        var first = true
        val sb = StringBuilder()
        javaClass.simpleName.forEach {
            if (it.isUpperCase()) {
                if (first) first = false else sb.append('_')
                sb.append(it.toLowerCase())
            } else sb.append(it)
        }
        table = sb.toString()
    }

    /**
     * @return Názov tabuľky
     */
    final override fun toString() = table

    /**
     * Vytvorenie tabuľky
     * @param db Databáza, ku ktorej pristupujem, keď nastavujem triggre
     */
    abstract fun create(db: SQLiteDatabase)

    /**
     * Zálohovanie dát z tabuľky do dočasnej tabuľky
     * @param db Databáza, ku ktorej pristupujem, keď nastavujem triggre
     */
    open fun createBackup(db: SQLiteDatabase) =
        db.execSQL("CREATE TEMP TABLE IF NOT EXISTS ${this}_backup AS SELECT * FROM $this;")

    /**
     * Vytvorenie tabuľky zo zálohy
     * @param db Databáza, ku ktorej pristupujem, keď nastavujem triggre
     */
    open fun recreate(db: SQLiteDatabase) {
        create(db)
        db.execSQL("CREATE TABLE IF NOT EXISTS $this AS SELECT * FROM ${this}_backup;")
    }

    /**
     * Nastavenie triggrov tabuľke
     * @param db Databáza, ku ktorej pristupujem, keď nastavujem triggre
     */
    open fun setupTriggers(db: SQLiteDatabase) = Unit

    /**
     * Odstránenie tabuľky
     * @param db Databáza, ku ktorej pristupujem, keď nastavujem triggre
     */
    open fun drop(db: SQLiteDatabase) = db.execSQL("DROP TABLE IF EXISTS $this;")

    /**
     * Vyhodenie výnimky s hlásenim z textového zdroja
     * @param resId id textového zdroja
     * @return SQL výraz, ktorý ukončí chod sql s chybovým hlásením
     */
    fun abort(resId: Int): String = "SELECT RAISE(ABORT, '${App.str(resId)}');"

    /**
     * Vyhodenie výnimky s hlásenim z textového zdroja
     * @param msg chybové hlásenie
     * @return SQL výraz, ktorý ukončí chod sql s chybovým hlásením
     */
    fun abort(msg: String): String = "SELECT RAISE(ABORT, '$msg');"

    /**
     * Vyhodenie výnimky s hlásenim z textového zdroja
     * @param resId id textového zdroja
     * @param sqlCond podmienka, za akej vznikne chyba
     * @return SQL výraz, ktorý ukončí chod sql s chybovým hlásením
     */
    fun abort(resId: Int, sqlCond: String) =
        "SELECT RAISE(ABORT, '${App.str(resId)}') WHERE $sqlCond;"

    /**
     * Vyhodenie výnimky s hlásenim z textového zdroja
     * @param msg chybové hlásenie
     * @param sqlCond podmienka, za akej vznikne chyba
     * @return SQL výraz, ktorý ukončí chod sql s chybovým hlásením
     */
    fun abort(msg: String, sqlCond: String) = "SELECT RAISE(ABORT, '$msg') WHERE $sqlCond;"
}