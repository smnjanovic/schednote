package com.moriak.schednote.database.tables

import android.database.sqlite.SQLiteDatabase
import com.moriak.schednote.database.Trigger

abstract class View : Table() {
    override fun setupTriggers(db: SQLiteDatabase) =
        Trigger.IIUD.create(db, this, abort("This view is read-only!"))

    override fun createBackup(db: SQLiteDatabase) = Unit
    override fun recreate(db: SQLiteDatabase) = create(db)
    override fun drop(db: SQLiteDatabase) = db.execSQL("DROP VIEW IF EXISTS $this;")
}