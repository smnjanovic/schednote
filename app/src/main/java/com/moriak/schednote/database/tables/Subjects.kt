package com.moriak.schednote.database.tables

import android.database.sqlite.SQLiteDatabase
import com.moriak.schednote.R
import com.moriak.schednote.database.Trigger
import com.moriak.schednote.database.data.Subject.CREATOR.abb_limit
import com.moriak.schednote.database.data.Subject.CREATOR.d
import com.moriak.schednote.database.data.Subject.CREATOR.l
import com.moriak.schednote.database.data.Subject.CREATOR.name_limit

object Subjects : Table() {
    const val SUB_ID = "sub_id"
    const val ABB = "abb"
    const val NAME = "name"

    override fun create(db: SQLiteDatabase) = db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS $this (
            $SUB_ID INTEGER PRIMARY KEY,
            $ABB VARCHAR($abb_limit) NOT NULL UNIQUE CHECK(LENGTH(TRIM($ABB)) BETWEEN 1 AND $abb_limit AND $ABB NOT GLOB '*[^$l$d]*'),
            $NAME VARCHAR($name_limit) NOT NULL CHECK(LENGTH(TRIM($NAME)) BETWEEN 1 AND $name_limit AND $NAME NOT GLOB '*[^$l$d ]*')
        );
    """
    )

    override fun setupTriggers(db: SQLiteDatabase) {
        Trigger.BU.create(db, this, "OLD.$SUB_ID != NEW.$SUB_ID", abort(R.string.read_only_id))

        Trigger.AIU.create(
            db,
            this,
            "NEW.$ABB != UPPER(TRIM(NEW.$ABB))",
            "UPDATE $Subjects SET $ABB = UPPER(TRIM($ABB)) WHERE $SUB_ID = NEW.$SUB_ID;"
        )

        // 1-znakovy nazov zacne velkym pismenom
        Trigger.AIU.create(
            db,
            this,
            "LENGTH(TRIM(NEW.$NAME)) = 1 AND UPPER(TRIM(NEW.$NAME)) != NEW.$NAME",
            "UPDATE $Subjects SET $NAME = UPPER(TRIM($NAME)) WHERE $SUB_ID = NEW.$SUB_ID;"
        )

        // viacznakovy nazov zacne velkym pismenom, pokracuje malymi
        Trigger.AIU.create(
            db,
            this,
            """
                LENGTH(TRIM(NEW.$NAME)) > 1 AND UPPER(SUBSTR(TRIM(NEW.$NAME), 1, 1))
                || LOWER(SUBSTR(TRIM(NEW.$NAME), 2)) != NEW.$NAME
            """,
            """
                UPDATE $Subjects
                SET $NAME = UPPER(SUBSTR(TRIM($NAME), 1, 1)) || LOWER(SUBSTR(TRIM($NAME), 2))
                WHERE $SUB_ID = NEW.$SUB_ID;
            """
        )

        // vymazanim predmetu sa zmazu aj vsetky
        Trigger.BD.create(
            db, this, """
            DELETE FROM $Notes WHERE $SUB_ID = OLD.$SUB_ID;
            DELETE FROM $Lessons WHERE $SUB_ID = OLD.$SUB_ID;
        """
        )
    }
}