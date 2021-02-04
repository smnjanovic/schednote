package com.moriak.schednote.database

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement
import com.moriak.schednote.database.tables.Table

/**
 * Trieda slúži na čítanie a zápis dát. Ak abstraktná trieda poskytuje funkcie, ktoré zjednodušia zápis
 * príkazov v potomkovi ktorý je určený len na výber a zápis konkrétnych dát z konkrétnych tabuliek
 */
abstract class DBControl {
    /**
     * Vďaka tejto triede v potomkovi netreba kontrolovať či je hodnota vkladanej hodnoty null alebo nie.
     */
    protected class Values {
        private val values = ContentValues()

        /**
         * Vyprázdnenie vkladacích hodnôt
         */
        fun clear() = values.clear()

        /**
         * Odstránenie konkrétnej vkladacej hodnoty
         * @param column Názov stĺpca ktorému hodnota patrila
         */
        fun del(column: String) = values.remove(column)

        /**
         * Pridanie hodnoty do stĺpca
         * @param column stĺpec
         * @param value vkladaná hodnota
         */
        fun put(column: String, value: Boolean?) =
            value?.let { values.put(column, if (it) 1 else 0) } ?: values.putNull(column)

        /**
         * Pridanie hodnoty do stĺpca
         * @param column stĺpec
         * @param value vkladaná hodnota
         */
        fun put(column: String, value: Byte?) =
            value?.let { values.put(column, it) } ?: values.putNull(column)

        /**
         * Pridanie hodnoty do stĺpca
         * @param column stĺpec
         * @param value vkladaná hodnota
         */
        fun put(column: String, value: Short?) =
            value?.let { values.put(column, it) } ?: values.putNull(column)

        /**
         * Pridanie hodnoty do stĺpca
         * @param column stĺpec
         * @param value vkladaná hodnota
         */
        fun put(column: String, value: Float?) =
            value?.let { values.put(column, it) } ?: values.putNull(column)

        /**
         * Pridanie hodnoty do stĺpca
         * @param column stĺpec
         * @param value vkladaná hodnota
         */
        fun put(column: String, value: Int?) =
            value?.let { values.put(column, it) } ?: values.putNull(column)

        /**
         * Pridanie hodnoty do stĺpca
         * @param column stĺpec
         * @param value vkladaná hodnota
         */
        fun put(column: String, value: Double?) =
            value?.let { values.put(column, it) } ?: values.putNull(column)

        /**
         * Pridanie hodnoty do stĺpca
         * @param column stĺpec
         * @param value vkladaná hodnota
         */
        fun put(column: String, value: Long?) =
            value?.let { values.put(column, it) } ?: values.putNull(column)

        /**
         * Pridanie hodnoty do stĺpca
         * @param column stĺpec
         * @param value vkladaná hodnota
         */
        fun put(column: String, value: String?) =
            value?.let { values.put(column, it) } ?: values.putNull(column)

        /**
         * Získanie inštancie s hodnotami
         */
        fun getValues() = values
    }

    private val values = Values()
    private val helper = DBHelp()
    private val rd = helper.readableDatabase
    private val wrt = helper.writableDatabase

    /**
     * Pridanie hodnôt do tabuľky
     * @param table Tabuľka
     * @param values Vkladané hodnoty
     */
    protected fun insert(table: Table, values: Values) =
        wrt.insert(table.toString(), null, values.getValues())

    /**
     * Pridanie hodnôt do tabuľky
     * @param table Tabuľka
     * @param nullColumnHack Názov prázdneho riadku tabuľky (samé NULL)
     * @param values Vkladané hodnoty
     */
    protected fun insert(table: Table, nullColumnHack: String?, values: Values) =
        wrt.insert(table.toString(), nullColumnHack, values.getValues())

    /**
     * Pridanie nahradenie hodnôt v tabuľke
     * @param table Tabuľka
     * @param values Vkladané hodnoty
     */
    protected fun replace(table: Table, values: Values) =
        wrt.replace(table.toString(), null, values.getValues())

    /**
     * Pridanie nahradenie hodnôt v tabuľke
     * @param table Tabuľka
     * @param nullColumnHack Názov prázdneho riadku tabuľky (samé NULL)
     * @param values Vkladané hodnoty
     */
    protected fun replace(table: Table, nullColumnHack: String?, values: Values) =
        wrt.replace(table.toString(), nullColumnHack, values.getValues())

    /**
     * Aktualizovať tabuľku
     * @param table tabuľka
     * @param values vkladané hodnoty
     * @param whereClause Podmienka
     * @param whereArgs hodnoty k podmienke
     */
    protected fun update(
        table: Table,
        values: Values,
        whereClause: String = "1=1",
        whereArgs: Array<String> = arrayOf()
    ) = wrt.update(table.toString(), values.getValues(), whereClause, whereArgs)

    /**
     * Vymazať dáta z tabuľky
     * @param table tabuľka
     * @param whereClause Podmienka
     * @param whereArgs hodnoty k podmienke
     */
    protected fun delete(
        table: Table,
        whereClause: String = "1=1",
        whereArgs: Array<String> = arrayOf()
    ) = wrt.delete(table.toString(), whereClause, whereArgs)

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

    /**
     * Nastavia sa nové hodnoty pripravene na odoslanie do databázy
     * @param fn metóda určená na pridávanie hodnôt do [values]
     * @return množina hodnôt, ktoré sa nakoniec vložia do tabuľky
     */
    protected fun values(fn: Values.() -> Unit): Values {
        values.clear()
        values.fn()
        return values
    }
}