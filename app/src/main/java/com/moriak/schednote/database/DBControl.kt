package com.moriak.schednote.database

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement
import com.moriak.schednote.database.tables.Table

abstract class DBControl {
    protected class Values {
        private val values = ContentValues()
        fun clear() = values.clear()
        fun del(column: String) = values.remove(column)
        fun put(column: String, value: Boolean?) =
            value?.let { values.put(column, if (it) 1 else 0) } ?: values.putNull(column)

        fun put(column: String, value: Byte?) =
            value?.let { values.put(column, it) } ?: values.putNull(column)

        fun put(column: String, value: Short?) =
            value?.let { values.put(column, it) } ?: values.putNull(column)

        fun put(column: String, value: Float?) =
            value?.let { values.put(column, it) } ?: values.putNull(column)

        fun put(column: String, value: Int?) =
            value?.let { values.put(column, it) } ?: values.putNull(column)

        fun put(column: String, value: Double?) =
            value?.let { values.put(column, it) } ?: values.putNull(column)

        fun put(column: String, value: Long?) =
            value?.let { values.put(column, it) } ?: values.putNull(column)

        fun put(column: String, value: String?) =
            value?.let { values.put(column, it) } ?: values.putNull(column)

        fun getValues() = values
    }

    private val values = Values()
    private val helper = DBHelp()
    private val rd = helper.readableDatabase
    private val wrt = helper.writableDatabase

    protected fun insert(table: Table, values: Values) =
        wrt.insert(table.toString(), null, values.getValues())

    protected fun insert(table: Table, nullColumnHack: String?, values: Values) =
        wrt.insert(table.toString(), nullColumnHack, values.getValues())

    protected fun replace(table: Table, values: Values) =
        wrt.replace(table.toString(), null, values.getValues())

    protected fun replace(table: Table, nullColumnHack: String?, values: Values) =
        wrt.replace(table.toString(), nullColumnHack, values.getValues())

    protected fun update(
        table: Table,
        values: Values,
        whereClause: String = "1=1",
        whereArgs: Array<String> = arrayOf()
    ) =
        wrt.update(table.toString(), values.getValues(), whereClause, whereArgs)

    protected fun delete(
        table: Table,
        whereClause: String = "1=1",
        whereArgs: Array<String> = arrayOf()
    ) =
        wrt.delete(table.toString(), whereClause, whereArgs)

    protected fun <T> all(sql: String, vararg args: String, fn: Cursor.() -> T?): ArrayList<T> {
        val ret = ArrayList<T>()
        val curs = rd.rawQuery(sql, args)
        while (curs.moveToNext()) curs.fn()?.let { ret.add(it) }
        curs.close()
        return ret
    }

    protected fun <T> one(sql: String, vararg args: String, fn: Cursor.() -> T): T? {
        val curs = rd.rawQuery(sql, args)
        if (curs.count == 0) return null
        curs.moveToFirst()
        val ret = curs.fn()
        curs.close()
        return ret
    }

    protected fun execSQL(sql: String, vararg args: String) = wrt.execSQL(sql, args)

    protected fun transaction(fn: SQLiteDatabase.() -> Unit) {
        wrt.beginTransaction()
        wrt.fn()
        wrt.setTransactionSuccessful()
        wrt.endTransaction()
    }

    protected fun transaction(sql: String, fn: SQLiteStatement.() -> Unit) {
        wrt.beginTransaction()
        val statement = wrt.compileStatement(sql)
        statement.fn()
        wrt.setTransactionSuccessful()
        wrt.endTransaction()
    }

    protected fun values(fn: Values.() -> Unit): Values {
        values.clear()
        values.fn()
        return values
    }
}