package com.moriak.schednote.database.tables

import android.database.sqlite.SQLiteDatabase
import com.moriak.schednote.R
import com.moriak.schednote.database.Trigger
import com.moriak.schednote.database.tables.LessonData.LES_ID
import com.moriak.schednote.database.tables.ScheduleRange.BREAK_DUR
import com.moriak.schednote.database.tables.ScheduleRange.LESSON_DUR
import com.moriak.schednote.database.tables.ScheduleRange.LES_NO

/**
 * Tabuľka časového harmonogramu hodín
 * @property LES_NO Poradie hodiny
 * @property LESSON_DUR Trvanie hodiny
 * @property BREAK_DUR Trvanie prestávky po hodine
 */
object ScheduleRange : Table() {
    const val LES_NO = "les_no"
    const val LESSON_DUR = "lesson_minutes"
    const val BREAK_DUR = "break_minutes"

    override fun create(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $this (
                $LES_NO INTEGER PRIMARY KEY CHECK($LES_NO > 0),
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
            "NEW.$LES_NO != (SELECT COALESCE(MAX($LES_NO), 0) + 1 FROM $this WHERE $LES_NO != NEW.$LES_NO)",
            "DELETE FROM $this WHERE $LES_NO != NEW.$LES_NO; ${abort(R.string.schedule_unmodifiable_order)}"
        )

        //poradie hodin nemenne
        Trigger.BU.create(
            db,
            this,
            "OLD.$LES_NO != NEW.$LES_NO",
            abort(R.string.schedule_unmodifiable_order)
        )

        // vymazanie casu hodiny vymaze aj casy nasledujucich hodin
        Trigger.BD.create(
            db, this, """
            DELETE FROM $ScheduleRange WHERE $LES_NO > OLD.$LES_NO;
            DELETE FROM $this WHERE $LES_NO > OLD.$LES_NO;
            DELETE FROM $LessonData WHERE $LES_ID NOT IN (SELECT $LES_ID FROM $Schedule);
        """
        )
    }
}