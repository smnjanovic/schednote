package com.moriak.schednote.database.tables

import android.database.sqlite.SQLiteDatabase
import com.moriak.schednote.database.Trigger
import com.moriak.schednote.database.tables.Colors.A
import com.moriak.schednote.database.tables.Colors.H
import com.moriak.schednote.database.tables.Colors.L
import com.moriak.schednote.database.tables.Colors.S
import com.moriak.schednote.database.tables.LessonTypes.TYPE

/**
 * Tabuľka farieb k jednotlivým typom hodín
 * @property H Odtieň
 * @property S Sýtosť
 * @property L Svetlosť
 * @property A Priehľadnosť
 */
object Colors : Table() {
    const val H = "h"
    const val S = "s"
    const val L = "l"
    const val A = "a"
    override fun create(db: SQLiteDatabase) = db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS $this (
            $TYPE INTEGER NOT NULL UNIQUE,
            $H INTEGER NOT NULL CHECK ($H BETWEEN 0 AND 359),
            $S INTEGER NOT NULL CHECK ($S BETWEEN 0 AND 100),
            $L INTEGER NOT NULL CHECK ($L BETWEEN 0 AND 100),
            $A INTEGER NOT NULL CHECK ($A BETWEEN 0 AND 100),
            FOREIGN KEY($TYPE) REFERENCES $LessonTypes($TYPE)
        );
    """
    )

    override fun setupTriggers(db: SQLiteDatabase) {
        super.setupTriggers(db)
        Trigger.BIU.create(
            db,
            this,
            "NEW.$TYPE NOT IN (SELECT $TYPE FROM $LessonTypes)",
            abort(FK_ERR)
        )
    }
}