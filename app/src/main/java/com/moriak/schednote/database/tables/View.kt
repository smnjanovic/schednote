package com.moriak.schednote.database.tables

import android.database.sqlite.SQLiteDatabase
import com.moriak.schednote.database.Trigger

/**
 * Trieda reprezentuje SQLite VIEW. Potomkovia tejto triedy budú mať na starosti
 * tvorbu pohľadov, trigrov a ich odstráňovanie
 */
abstract class View : Table() {
    /**
     * Nastavenie triggrov pohľadu
     * @param db Databáza, ku ktorej pristupujem, keď nastavujem triggre
     */
    override fun setupTriggers(db: SQLiteDatabase) =
        Trigger.IIUD.create(db, this, abort("This view is read-only!"))

    /**
     * Vytvorenie zálohy dát pri pohľade nie je potrebné
     * @param db Databáza, ku ktorej pristupujem, keď nastavujem triggre
     */
    override fun createBackup(db: SQLiteDatabase) = Unit

    /**
     * Vytvorenie pohľadu so zachovaním starých výsledkov je možné obyčajným vytvorením nového pohľadu
     * @param db Databáza, ku ktorej pristupujem, keď nastavujem triggre
     */
    override fun recreate(db: SQLiteDatabase) = create(db)

    /**
     * Odstránenie pohľadu
     * @param db Databáza, ku ktorej pristupujem, keď nastavujem triggre
     */
    override fun drop(db: SQLiteDatabase) = db.execSQL("DROP VIEW IF EXISTS $this;")
}