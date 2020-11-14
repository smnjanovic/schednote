package com.moriak.schednote.database.tables

import android.database.sqlite.SQLiteDatabase
import com.moriak.schednote.database.tables.LessonTypes.TYPE
import com.moriak.schednote.database.tables.Lessons.DAY
import com.moriak.schednote.database.tables.Lessons.DUR
import com.moriak.schednote.database.tables.Lessons.LES_ID
import com.moriak.schednote.database.tables.Lessons.ROOM
import com.moriak.schednote.database.tables.Lessons.START
import com.moriak.schednote.database.tables.Lessons.WEEK_ODDITY
import com.moriak.schednote.database.tables.Subjects.SUB_ID

/**
 * Pohlad na tabulku vyucovacich hodin sleduje ci nejdu 2 alebo 3 rovnake hodiny za sebou
 * To, že sú rovnaké znamená, že sú v rovnakej miestnosti, z rovnakého predmetu, rovnakého typu výuky)
 */
object LessonsToJoin : View() {
    const val LEFT = "left_id"
    const val MID = "mid_id"
    const val RIGHT = "right_id"
    override fun create(db: SQLiteDatabase) = db.execSQL(
        """
        CREATE VIEW $this AS
        SELECT a.$LES_ID AS $LEFT, b.$LES_ID AS $MID, c.$LES_ID AS $RIGHT, a.$WEEK_ODDITY,
            a.$DAY, a.$START, c.$START + c.$DUR - a.$START AS $DUR, a.$TYPE, a.$SUB_ID, a.$ROOM
        FROM $Lessons a JOIN $Lessons b
        ON a.$START + a.$DUR = b.$START AND a.$DAY = b.$DAY
        AND a.$TYPE = b.$TYPE AND a.$SUB_ID = b.$SUB_ID
        AND a.$ROOM LIKE b.$ROOM
        AND (a.$WEEK_ODDITY = b.$WEEK_ODDITY OR a.$WEEK_ODDITY IS NULL AND b.$WEEK_ODDITY IS NULL)
        
        JOIN $Lessons c
        ON b.$START + b.$DUR = c.$START AND b.$DAY = c.$DAY
        AND b.$TYPE = c.$TYPE AND b.$SUB_ID = c.$SUB_ID
        AND b.$ROOM LIKE c.$ROOM
        AND (b.$WEEK_ODDITY = c.$WEEK_ODDITY OR b.$WEEK_ODDITY IS NULL AND c.$WEEK_ODDITY IS NULL)
        
        UNION ALL
        
        SELECT a.$LES_ID, NULL, b.$LES_ID, a.$WEEK_ODDITY, a.$DAY, a.$START, b.$START + b.$DUR - a.$START,
            a.$TYPE, a.$SUB_ID, a.$ROOM
        FROM $Lessons a JOIN $Lessons b
        ON a.$START + a.$DUR = b.$START AND a.$DAY = b.$DAY
        AND a.$TYPE = b.$TYPE AND a.$SUB_ID = b.$SUB_ID
        AND a.$ROOM LIKE b.$ROOM
        AND (a.$WEEK_ODDITY = b.$WEEK_ODDITY OR a.$WEEK_ODDITY IS NULL AND b.$WEEK_ODDITY IS NULL);
    """
    )
}