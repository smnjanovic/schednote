package com.moriak.schednote.storage

import android.database.sqlite.SQLiteDatabase
import com.moriak.schednote.storage.Subjects.ABB
import com.moriak.schednote.storage.Subjects.NAME
import com.moriak.schednote.storage.Subjects.SUB_ID
import com.moriak.schednote.enums.Trigger

/**
 * Tabuľka predmetov
 * @property SUB_ID ID predmetu
 * @property ABB Skratka predmetu
 * @property NAME Názov predmetu
 */
object Subjects : Table() {
    const val SUB_ID = "sub_id"
    const val ABB = "abb"
    const val NAME = "name"

    override fun create(db: SQLiteDatabase) = db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS $this (
            $SUB_ID INTEGER PRIMARY KEY,
            $ABB VARCHAR(5) NOT NULL UNIQUE CHECK(LENGTH(TRIM($ABB)) BETWEEN 1 AND 5 AND $ABB NOT GLOB '*[^a-zA-ZÀ-ž0-9]*'),
            $NAME VARCHAR(48) NOT NULL CHECK(LENGTH(TRIM($NAME)) BETWEEN 1 AND 48 AND $NAME NOT GLOB '*[^a-zA-ZÀ-ž0-9 ]*')
        );
    """
    )

    override fun setupTriggers(db: SQLiteDatabase) {
        Trigger.BU.create(
            db,
            this,
            "OLD.$SUB_ID != NEW.$SUB_ID",
            abort("Table ID is immutable")
        )

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

        // vymazanim predmetu sa zmazu aj vsetky suvisiace data z inych tabuliek
        Trigger.BD.create(
            db, this, """
            DELETE FROM $Notes WHERE $SUB_ID = OLD.$SUB_ID;
            DELETE FROM $LessonData WHERE $SUB_ID = OLD.$SUB_ID;
        """
        )
    }
}