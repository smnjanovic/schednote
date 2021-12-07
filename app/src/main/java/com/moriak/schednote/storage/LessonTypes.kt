package com.moriak.schednote.storage

import android.database.sqlite.SQLiteDatabase
import com.moriak.schednote.storage.LessonTypes.TYPE
import com.moriak.schednote.storage.LessonTypes.TYPE_NAME
import com.moriak.schednote.enums.Trigger

/**
 * Tabuľka typov hodín
 * @property TYPE ID typu hodiny
 * @property TYPE_NAME Názov typu hodiny
 */
object LessonTypes : Table() {
    const val TYPE = "type_id"
    const val TYPE_NAME = "type_name"
    const val COLOR = "color"

    override fun create(db: SQLiteDatabase) = db.execSQL(
        """CREATE TABLE IF NOT EXISTS $this (
    $TYPE INTEGER PRIMARY KEY,
    $TYPE_NAME VARCHAR(24) NOT NULL UNIQUE CHECK(
        LENGTH($TYPE_NAME) BETWEEN 1 AND 24
        AND $TYPE_NAME GLOB '[a-zA-ZÀ-ž]*'
        AND $TYPE_NAME NOT GLOB '*[^a-zA-ZÀ-ž ]*'
    ),
    $COLOR INTEGER NOT NULL DEFAULT(random() & 0xFFFFFFFF | 0xB2000000)
);"""
    )

    override fun setupTriggers(db: SQLiteDatabase) {
        // dodrzanie maximalneho poctu typov hodin
        var cond = "(SELECT COUNT(*) FROM $this) >= 5"
        var body = abort("Lesson type count reached the maximum!")
        Trigger.BI.create(db, this, cond, body)

        // nazov typu hodiny ma obmedzenu mnozinu povolenych znakov
        cond = "NEW.$TYPE_NAME GLOB '[^a-zA-ZÀ-ž ]'"
        body = abort("Each lesson type must be unique!")
        Trigger.BIU.create(db, this, cond, body)

        // nesmiem prepisat primarny kluc!
        cond = "OLD.$TYPE != NEW.$TYPE"
        body = abort("Table ID is immutable")
        Trigger.BU.create(db, this, cond, body)

        // Vymazanim typu hodiny vymazem aj udaj o farbe
        // k danemu typu hodiny aj vsetky hodiny tohto typu
        body = "DELETE FROM $LessonData WHERE $TYPE = OLD.$TYPE; "
        Trigger.BD.create(db, this, body)

        // upraviť názov tak aby začínal veľkým písmenom
        val up = "SELECT SUBSTR(n, 1, 1) || LOWER(SUBSTR(n, 2)) FROM (SELECT UPPER(TRIM(NEW.$TYPE_NAME)) AS n)"
        cond = "EXISTS ($up WHERE NEW.$TYPE_NAME != n)"
        body = "UPDATE $this SET $TYPE_NAME = ($up) WHERE $TYPE = NEW.$TYPE;"
        Trigger.AIU.create(db, this, cond, body)
    }
}