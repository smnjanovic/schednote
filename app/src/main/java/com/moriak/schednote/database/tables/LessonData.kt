package com.moriak.schednote.database.tables

import android.database.sqlite.SQLiteDatabase
import com.moriak.schednote.R
import com.moriak.schednote.database.Trigger
import com.moriak.schednote.database.data.Lesson.Companion.room_limit
import com.moriak.schednote.database.tables.LessonData.LES_ID
import com.moriak.schednote.database.tables.LessonData.ROOM
import com.moriak.schednote.database.tables.LessonTypes.TYPE
import com.moriak.schednote.database.tables.Subjects.SUB_ID

/**
 * Tabuľka vyučovacích hodín
 * @property LES_ID ID hodiny
 * párny týždeň a NULL, ak hodina prebieha každý týždeň
 * @property ROOM miestnosť vyučovania
 */
object LessonData : Table() {
    const val LES_ID = "les_id"
    const val ROOM = "room"

    override fun create(db: SQLiteDatabase) = db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS $this (
            $LES_ID INTEGER NOT NULL PRIMARY KEY,
            $TYPE INTEGER NOT NULL,
            $SUB_ID INTEGER NOT NULL,
            $ROOM VARCHAR($room_limit) NOT NULL CHECK(LENGTH($ROOM) <= $room_limit),
            FOREIGN KEY ($TYPE) REFERENCES $LessonTypes($TYPE),
            FOREIGN KEY ($SUB_ID) REFERENCES $Subjects($SUB_ID)
        );
    """
    )

    override fun setupTriggers(db: SQLiteDatabase) {
        fun find(upd: Boolean): String {
            val select = "SELECT $LES_ID FROM $this WHERE"
            val cond = "$TYPE = NEW.$TYPE AND $SUB_ID = NEW.$SUB_ID AND $ROOM LIKE NEW.$ROOM"
            val exc = if (upd) " AND $LES_ID != NEW.$LES_ID" else ""
            return "$select $cond$exc"
        }

        fun del(upd: Boolean): String = "DELETE FROM $this WHERE $LES_ID IN (${find(upd)})"

        Trigger.BU.create(db, this, "OLD.$LES_ID != NEW.$LES_ID", abort(R.string.read_only_id))
        // overenie FK
        Trigger.BIU.create(
            db, this, """
            NOT EXISTS(SELECT 1 FROM $LessonTypes WHERE $TYPE = NEW.$TYPE)
            OR NOT EXISTS(SELECT 1 FROM $Subjects WHERE $SUB_ID = NEW.$SUB_ID)
        """, "SELECT RAISE(ABORT, '$FK_ERR');"
        )
        // zmazanie duplicit
        val cond = "$TYPE = NEW.$TYPE AND $SUB_ID = NEW.$SUB_ID AND $ROOM LIKE NEW.$ROOM"
        val exc = "$LES_ID != NEW.$LES_ID"
        val sel = "SELECT 1 FROM $this WHERE $cond"
        val del = "DELETE FROM $this WHERE $cond"
        Trigger.BI.create(db, this, "EXISTS($sel)", "$del;")
        Trigger.BU.create(db, this, "EXISTS($sel$exc)", "$sel$exc;")

        // vymazat hodiny aj z rozvrhu
        Trigger.BD.create(db, this, "DELETE FROM $Schedule WHERE $LES_ID = OLD.$LES_ID;")
    }
}