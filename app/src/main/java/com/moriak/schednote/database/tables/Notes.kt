package com.moriak.schednote.database.tables

import android.database.sqlite.SQLiteDatabase
import com.moriak.schednote.R.string.read_only_id
import com.moriak.schednote.database.Trigger
import com.moriak.schednote.database.data.Note.Companion.limit
import com.moriak.schednote.database.tables.Notes.DEADLINE
import com.moriak.schednote.database.tables.Notes.NOTE
import com.moriak.schednote.database.tables.Notes.NOTE_ID
import com.moriak.schednote.database.tables.Subjects.SUB_ID

/**
 * Tabuľka úloh
 * @property NOTE_ID ID úlohy
 * @property NOTE úloha
 * @property DEADLINE čas do vypršania úlohy
 */
object Notes : Table() {
    const val NOTE_ID = "note_id"
    const val NOTE = "note"
    const val DEADLINE = "deadline"
    override fun create(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $this (
                $NOTE_ID INTEGER PRIMARY KEY,
                $SUB_ID INTEGER NOT NULL,
                $NOTE VARCHAR($limit) NOT NULL CHECK (LENGTH(TRIM($NOTE)) BETWEEN 1 AND $limit),
                $DEADLINE INTEGER,
                FOREIGN KEY($SUB_ID) REFERENCES $Subjects($SUB_ID)
            );
        """
        )
    }

    override fun setupTriggers(db: SQLiteDatabase) {
        Trigger.BU.create(db, this, "OLD.$NOTE_ID != NEW.$NOTE_ID", abort(read_only_id))
        Trigger.BIU.create(
            db,
            this,
            "NOT EXISTS(SELECT 1 FROM $Subjects WHERE $SUB_ID = NEW.$SUB_ID)",
            abort(FK_ERR)
        )
    }
}