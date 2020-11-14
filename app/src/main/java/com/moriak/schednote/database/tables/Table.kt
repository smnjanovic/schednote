package com.moriak.schednote.database.tables

import android.database.sqlite.SQLiteDatabase
import com.moriak.schednote.App

abstract class Table {
    companion object {
        const val FK_ERR = "Invalid foreign key!"
    }

    private val table = javaClass.simpleName.let { old ->
        StringBuilder().apply {
            var first = true
            old.forEach {
                if (it.isUpperCase()) {
                    if (first) first = false else append('_')
                    append(it.toLowerCase())
                } else append(it)
            }
        }.toString()
    }

    final override fun toString() = table
    abstract fun create(db: SQLiteDatabase)

    open fun createBackup(db: SQLiteDatabase) =
        db.execSQL("CREATE TEMP TABLE IF NOT EXISTS ${this}_backup AS SELECT * FROM $this;")

    open fun recreate(db: SQLiteDatabase) {
        create(db)
        db.execSQL("CREATE TABLE IF NOT EXISTS $this AS SELECT * FROM ${this}_backup;")
    }

    open fun setupTriggers(db: SQLiteDatabase) = Unit
    open fun drop(db: SQLiteDatabase) = db.execSQL("DROP TABLE IF EXISTS $this;")

    fun abort(resId: Int): String = "SELECT RAISE(ABORT, '${App.str(resId)}');"
    fun abort(msg: String): String = "SELECT RAISE(ABORT, '$msg');"
    fun abort(resId: Int, sqlCond: String) =
        "SELECT RAISE(ABORT, '${App.str(resId)}') WHERE $sqlCond;"

    fun abort(msg: String, sqlCond: String) = "SELECT RAISE(ABORT, '$msg') WHERE $sqlCond;"
}