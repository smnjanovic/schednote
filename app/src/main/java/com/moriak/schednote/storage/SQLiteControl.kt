package com.moriak.schednote.storage

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteStatement
import com.moriak.schednote.App

/**
 * Trieda slúži na čítanie a zápis dát. Ak abstraktná trieda poskytuje funkcie, ktoré zjednodušia zápis
 * príkazov v potomkovi ktorý je určený len na výber a zápis konkrétnych dát z konkrétnych tabuliek
 */
abstract class SQLiteControl {
    /**
     * Vylepšený ContentValues, ktorého príkaz put dokáže vložiť aj null.
     */
    protected object Values {
        val cv by lazy { ContentValues() }

        /**
         * Pridanie hodnoty do stĺpca
         * @param column stĺpec
         * @param value hodnota
         */
        fun <T> put(column: String, value: T) = when (value) {
            null -> cv.putNull(column)
            is Boolean -> cv.put(column, if (value) 1 else 0)
            is Byte -> cv.put(column, value)
            is Short -> cv.put(column, value)
            is Float -> cv.put(column, value)
            is Int -> cv.put(column, value)
            is Double -> cv.put(column, value)
            is Long -> cv.put(column, value)
            is String -> cv.put(column, value)
            else -> cv.put(column, value.toString())
        }
    }

    private object Helper: SQLiteOpenHelper(App.ctx, "schednote.db", null, 25) {
        private val tables = arrayOf(Subjects, ScheduleRange, LessonTypes, Notes, LessonData, Schedule)

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("PRAGMA foreign_keys = ON;")
            for (t in tables) t.create(db)
            for (t in tables) t.setupTriggers(db)
        }

        override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) {
            for (t in tables.reversed()) t.drop(db)
            onCreate(db)
        }

        override fun onDowngrade(db: SQLiteDatabase, old: Int, new: Int) = onUpgrade(db, new, old)
    }

    private val rd = Helper.readableDatabase
    private val wrt = Helper.writableDatabase

    /**
     * Pridanie hodnôt do tabuľky
     * @param table Tabuľka
     * @param fn nastavenie hodnôt pre dané stĺpce tabuľky [table].
     */
    protected fun insert(table: Table, fn: Values.()->Unit): Long = wrt.insert(table.toString(), null, values(fn).cv)

    /**
     * Pridanie hodnôt do tabuľky
     * @param table Tabuľka
     * @param nullColumnHack Názov prázdneho riadku tabuľky (samé NULL)
     * @param fn nastavenie hodnôt pre dané stĺpce tabuľky [table].
     */
    protected fun insert(table: Table, nullColumnHack: String?, fn: Values.()->Unit) = wrt.insert(table.toString(), nullColumnHack, values(fn).cv)

    /**
     * Pridanie nahradenie hodnôt v tabuľke
     * @param table Tabuľka
     * @param fn nastavenie hodnôt pre dané stĺpce tabuľky [table].
     */
    protected fun replace(table: Table, fn: Values.()->Unit) = wrt.replace(table.toString(), null, values(fn).cv)

    /**
     * Pridanie nahradenie hodnôt v tabuľke
     * @param table Tabuľka
     * @param nullColumnHack Názov prázdneho riadku tabuľky (samé NULL)
     * @param fn nastavenie hodnôt pre dané stĺpce tabuľky [table].
     */
    protected fun replace(table: Table, nullColumnHack: String?, fn: Values.()->Unit) = wrt.replace(table.toString(), nullColumnHack, values(fn).cv)

    /**
     * Aktualizovať tabuľku
     * @param table tabuľka
     * @param whereClause Podmienka
     * @param whereArgs hodnoty k podmienke
     * @param fn nastavenie hodnôt pre dané stĺpce tabuľky [table].
     */
    protected fun update(
        table: Table,
        whereClause: String = "1=1",
        whereArgs: Array<String> = arrayOf(),
        fn: Values.()->Unit
    ) = wrt.update(table.toString(), values(fn).cv, whereClause, whereArgs)

    /**
     * Vymazať dáta z tabuľky
     * @param table tabuľka
     * @param whereClause Podmienka
     * @param whereArgs hodnoty k podmienke
     */
    protected fun delete(table: Table, whereClause: String = "1=1", whereArgs: Array<String> = arrayOf()) = wrt.delete(table.toString(), whereClause, whereArgs)

    /**
     * Načítanie celého zoznamu výsledkov selectu
     * @param sql SQL dopyt ktorý vracia tabulku
     * @param args Zoznam hodnôt, ktoré nahradia '?' v [sql]
     * @param fn Funkcia ktorá vracia hodnotu, ktorá pribudne do ArrayListu
     * @return Zoznam výsledkov dopytu SQL alebo null ak taký záznam neexistuje
     */
    protected fun <T> all(sql: String, vararg args: String, fn: Cursor.() -> T?): ArrayList<T> {
        val ret = ArrayList<T>()
        val curs = rd.rawQuery(sql, args)
        while (curs.moveToNext()) curs.fn()?.let { ret.add(it) }
        curs.close()
        return ret
    }

    /**
     * Načítanie jedného alebo žiadneho výsledkov selectu
     * @param sql SQL dopyt ktorý vracia tabulku
     * @param args Zoznam hodnôt, ktoré nahradia '?' v [sql]
     * @param fn Funkcia ktorá vracia nejakú hodnotu
     * @return Zoznam výsledkov dopytu SQL alebo null ak taký záznam neexistuje
     */
    protected fun <T> one(sql: String, vararg args: String, fn: Cursor.() -> T): T? {
        val curs = rd.rawQuery(sql, args)
        if (curs.count == 0) return null
        curs.moveToFirst()
        val ret = curs.fn()
        curs.close()
        return ret
    }

    /**
     * Vykonanie transakcie
     * @fn Metóda, čo sas bude diať počas danej transakcie
     */
    protected fun transaction(fn: SQLiteDatabase.() -> Unit) {
        wrt.beginTransaction()
        wrt.fn()
        wrt.setTransactionSuccessful()
        wrt.endTransaction()
    }

    /**
     * Vykonanie transakcie
     * @sql Výraz, ktorý sa skompiluje a bude môcť vykonať niekoľko krát po sebe vykonávať
     * @fn Metóda, čo sas bude diať počas danej transakcie
     */
    protected fun transaction(sql: String, fn: SQLiteStatement.() -> Unit) {
        transaction {
            val statement = wrt.compileStatement(sql)
            statement.fn()
        }
    }

    private fun values(fn: Values.() -> Unit): Values {
        Values.cv.clear()
        Values.fn()
        return Values
    }
}