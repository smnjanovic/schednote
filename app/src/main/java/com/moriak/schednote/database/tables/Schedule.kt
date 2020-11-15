package com.moriak.schednote.database.tables

import android.database.sqlite.SQLiteDatabase
import com.moriak.schednote.R
import com.moriak.schednote.database.Trigger
import com.moriak.schednote.database.tables.Lessons.DUR
import com.moriak.schednote.database.tables.Lessons.START
import com.moriak.schednote.database.tables.Schedule.BREAK_DUR
import com.moriak.schednote.database.tables.Schedule.LESSON_DUR
import com.moriak.schednote.database.tables.Schedule.ORDER

/**
 * Tabuľka časového harmonogramu hodín
 * @property ORDER Poradie hodiny
 * @property LESSON_DUR Trvanie hodiny
 * @property BREAK_DUR Trvanie prestávky po hodine
 */
object Schedule : Table() {
    const val ORDER = "les_order"
    const val LESSON_DUR = "lesson_minutes"
    const val BREAK_DUR = "break_minutes"

    override fun create(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $this (
                $ORDER INTEGER PRIMARY KEY CHECK($ORDER > 0),
                $LESSON_DUR INTEGER NOT NULL CHECK($LESSON_DUR > 0),
                $BREAK_DUR INTEGER NOT NULL CHECK($BREAK_DUR >= 0)
            );
        """
        )
    }

    override fun setupTriggers(db: SQLiteDatabase) {
        //zabezpecit aby bola kazda hodina pridana ako posledná
        Trigger.AI.create(
            db, this,
            "NEW.$ORDER != (SELECT COALESCE(MAX($ORDER), 0) + 1 FROM $this WHERE $ORDER != NEW.$ORDER)",
            "DELETE FROM $this WHERE $ORDER != NEW.$ORDER; ${abort(R.string.schedule_unmodifiable_order)}"
        )

        //poradie hodin nemenne
        Trigger.BU.create(
            db,
            this,
            "OLD.$ORDER != NEW.$ORDER",
            abort(R.string.schedule_unmodifiable_order)
        )

        // vymazanie hodiny vymaze vsetky nasledujuce hodiny, vratane naplanovanych predmetov
        Trigger.BD.create(
            db, this, """
            DELETE FROM $Lessons WHERE OLD.$ORDER BETWEEN $START AND $START + $DUR - 1;
            DELETE FROM $this WHERE $ORDER > OLD.$ORDER;
        """
        )
    }
}