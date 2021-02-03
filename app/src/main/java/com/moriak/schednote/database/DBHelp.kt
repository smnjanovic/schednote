package com.moriak.schednote.database

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.moriak.schednote.App
import com.moriak.schednote.database.tables.*

/**
 * Trieda slúži na tvorbu a pripojenie sa k offline databáze
 */
class DBHelp : SQLiteOpenHelper(App.ctx, "schednote.db", null, 1) {
    private val tables: Array<Table> =
        arrayOf(Subjects, ScheduleRange, LessonTypes, Notes, Colors, Lessons, LessonsToJoin)

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("PRAGMA foreign_keys = ON;")
        for (t in tables) t.create(db)
        for (t in tables) t.setupTriggers(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) {
        for (t in tables.indices.reversed()) {
            tables[t].also {
                it.createBackup(db)
                it.drop(db)
            }
        }
        for (t in tables) {
            t.recreate(db)
            t.setupTriggers(db)
        }
    }

    override fun onDowngrade(db: SQLiteDatabase, old: Int, new: Int) = onUpgrade(db, new, old)
}