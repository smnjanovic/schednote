package com.moriak.schednote.storage

import android.database.sqlite.SQLiteDatabase
import com.moriak.schednote.enums.Trigger
import com.moriak.schednote.storage.LessonData.LES_ID
import com.moriak.schednote.storage.Schedule.DAY
import com.moriak.schednote.storage.Schedule.REG
import com.moriak.schednote.storage.ScheduleRange.LES_NO

/**
 * Tabuľka vyučovacích hodín
 * @property REG pravidelnost hodiny: 0 = parny tyzden, 1 = neparny tyzden, 2 = kazdy tyzden
 * párny týždeň a NULL, ak hodina prebieha každý týždeň
 * @property DAY deň vyučovania
 */
object Schedule : Table() {
    const val REG = "week_oddity"
    const val DAY = "day"

    override fun create(db: SQLiteDatabase) = db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS $this (
            $REG INTEGER NOT NULL CHECK ($REG BETWEEN 0 AND 2),
            $DAY INTEGER NOT NULL CHECK ($DAY BETWEEN 1 AND 7),
            $LES_NO INTEGER NOT NULL,
            $LES_ID NOT NULL,
            FOREIGN KEY ($LES_NO) REFERENCES $ScheduleRange($LES_NO),
            FOREIGN KEY ($LES_ID) REFERENCES $LessonData($LES_ID),
            PRIMARY KEY ($REG, $DAY, $LES_NO)
        );
    """
    )

    override fun setupTriggers(db: SQLiteDatabase) {
        //prepisanie casti rozvrhu
        val clear = "DELETE FROM $this WHERE $DAY = NEW.$DAY AND $LES_NO = NEW.$LES_NO"
        Trigger.BIU.create(db, this, "NEW.$REG = 0", "$clear AND $REG != 1;")
        Trigger.BIU.create(db, this, "NEW.$REG = 1", "$clear AND $REG != 0;")
        Trigger.BIU.create(db, this, "NEW.$REG = 2", "$clear;")
    }
}