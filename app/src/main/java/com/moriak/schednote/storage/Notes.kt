package com.moriak.schednote.storage

import android.database.sqlite.SQLiteDatabase
import com.moriak.schednote.storage.Notes.DEADLINE
import com.moriak.schednote.storage.Notes.NOTE
import com.moriak.schednote.storage.Notes.NOTE_ID
import com.moriak.schednote.storage.Subjects.SUB_ID
import com.moriak.schednote.enums.Trigger

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
                $NOTE VARCHAR(256) NOT NULL CHECK (LENGTH(TRIM($NOTE)) BETWEEN 1 AND 256),
                $DEADLINE INTEGER,
                FOREIGN KEY($SUB_ID) REFERENCES $Subjects($SUB_ID)
            );
        """
        )
    }

    override fun setupTriggers(db: SQLiteDatabase) {
        Trigger.BU.create(db, this, "OLD.$NOTE_ID != NEW.$NOTE_ID", abort("Table ID is immutable"))
        Trigger.BIU.create(
            db,
            this,
            "NOT EXISTS(SELECT 1 FROM $Subjects WHERE $SUB_ID = NEW.$SUB_ID)",
            abort("Invalid foreign key!")
        )
    }
}