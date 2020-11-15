package com.moriak.schednote.database.tables

import android.database.sqlite.SQLiteDatabase
import com.moriak.schednote.R
import com.moriak.schednote.database.Trigger
import com.moriak.schednote.database.data.LessonType.Companion.MAX_COUNT
import com.moriak.schednote.database.data.LessonType.Companion.limit
import com.moriak.schednote.database.tables.LessonTypes.TYPE
import com.moriak.schednote.database.tables.LessonTypes.TYPE_NAME

/**
 * Tabuľka typov hodín
 * @property TYPE ID typu hodiny
 * @property TYPE_NAME Názov typu hodiny
 */
object LessonTypes : Table() {
    const val TYPE = "type_id"
    const val TYPE_NAME = "type_name"

    override fun create(db: SQLiteDatabase) = db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS $this (
            $TYPE INTEGER PRIMARY KEY,
            $TYPE_NAME VARCHAR($limit) NOT NULL UNIQUE CHECK(
                LENGTH($TYPE_NAME) BETWEEN 1 AND 24
                AND $TYPE_NAME GLOB '[a-zA-ZÀ-ž]*'
                AND $TYPE_NAME NOT GLOB '*[^a-zA-ZÀ-ž ]*'
            )
        );
    """
    )

    override fun setupTriggers(db: SQLiteDatabase) {
        Trigger.BI.create(
            db,
            this,
            "$MAX_COUNT IN (SELECT COUNT(*) FROM $this)",
            abort(R.string.lesson_type_exceeded)
        )
        Trigger.BIU.create(
            db,
            this,
            "NEW.$TYPE_NAME GLOB '[^a-zA-ZÀ-ž ]'",
            abort(R.string.fill_type_name)
        )
        Trigger.BU.create(db, this, "OLD.$TYPE != NEW.$TYPE", abort(R.string.read_only_id))
        Trigger.BD.create(
            db, this, """
            DELETE FROM $Lessons WHERE $TYPE = OLD.$TYPE;
            DELETE FROM $Colors WHERE $TYPE = OLD.$TYPE;
        """
        )
        Trigger.AIU.create(
            db, this, """
            EXISTS (
                SELECT SUBSTR(n, 1, 1) || LOWER (SUBSTR(n, 2))
                FROM (SELECT UPPER(TRIM(NEW.$TYPE_NAME)) AS n)
                WHERE NEW.$TYPE_NAME != n
            )""", """
                UPDATE $this
                SET $TYPE_NAME = (
                    SELECT SUBSTR(n, 1, 1) || LOWER (SUBSTR(n, 2))
                    FROM (SELECT UPPER(TRIM(NEW.$TYPE_NAME)) AS n)
                )
                WHERE $TYPE = NEW.$TYPE;
            """
        )
    }
}