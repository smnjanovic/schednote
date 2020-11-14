package com.moriak.schednote.database.tables

import android.database.sqlite.SQLiteDatabase
import com.moriak.schednote.R
import com.moriak.schednote.database.Trigger
import com.moriak.schednote.database.data.Lesson.Companion.room_limit
import com.moriak.schednote.database.tables.LessonTypes.TYPE
import com.moriak.schednote.database.tables.LessonsToJoin.LEFT
import com.moriak.schednote.database.tables.LessonsToJoin.MID
import com.moriak.schednote.database.tables.LessonsToJoin.RIGHT
import com.moriak.schednote.database.tables.Schedule.ORDER
import com.moriak.schednote.database.tables.Subjects.SUB_ID

object Lessons : Table() {
    const val LES_ID = "les_id"
    const val WEEK_ODDITY = "week_oddity"
    const val DAY = "day"
    const val START = "start"
    const val DUR = "dur"
    const val ROOM = "room"

    override fun create(db: SQLiteDatabase) = db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS $this (
            $LES_ID INTEGER PRIMARY KEY,
            $WEEK_ODDITY INTEGER CHECK ($WEEK_ODDITY BETWEEN 0 AND 1),
            $DAY INTEGER NOT NULL CHECK($DAY BETWEEN 1 AND 7),
            $START INTEGER NOT NULL,
            $DUR INTEGER NOT NULL CHECK($DUR > 0),
            $TYPE INTEGER NOT NULL,
            $SUB_ID INTEGER NOT NULL,
            $ROOM VARCHAR($room_limit) CHECK(LENGTH($ROOM) <= $room_limit),
            FOREIGN KEY ($START) REFERENCES $Schedule($START),
            FOREIGN KEY ($TYPE) REFERENCES $LessonTypes($TYPE),
            FOREIGN KEY ($SUB_ID) REFERENCES $Subjects($SUB_ID)
        );
    """
    )

    override fun setupTriggers(db: SQLiteDatabase) {
        Trigger.BU.create(db, this, "OLD.$LES_ID != NEW.$LES_ID", abort(R.string.read_only_id))

        val invalidValues = abort(
            R.string.schedule_out_of_range, """
            NOT EXISTS (SELECT 1 FROM $Schedule) OR EXISTS (
                SELECT 1 FROM (SELECT MIN($ORDER) AS mi, MAX($ORDER) AS ma FROM $Schedule)
                WHERE NEW.$START NOT BETWEEN mi AND ma OR NEW.$START + NEW.$DUR - 1 NOT BETWEEN mi AND ma
            )"""
        )
        val invalidKeys = abort(
            FK_ERR, """
            NOT EXISTS (SELECT 1 FROM $LessonTypes WHERE $TYPE = NEW.$TYPE)
            OR NOT EXISTS (SELECT 1 FROM $Subjects WHERE $SUB_ID = NEW.$SUB_ID)
        """
        )

        Trigger.BIU.create(
            db, this, """
            -- overenie spravnosti hodnot
            $invalidValues
            $invalidKeys
            
            -- kontrola obsadenosti casoveho useku
            -- ak nova hodina prebieha uprostred starej, tu staru hodinu rozdeli na 2 kusy
            INSERT INTO $Lessons($WEEK_ODDITY, $DAY, $START, $DUR, $TYPE, $SUB_ID, $ROOM)
                SELECT $WEEK_ODDITY, $DAY, $START AS st, NEW.$START - $START AS dur, $TYPE, $SUB_ID, $ROOM
                FROM $Lessons
                WHERE $DAY = NEW.$DAY
                AND $START < NEW.$START
                AND $START + $DUR > NEW.$START + NEW.$DUR
                AND ($WEEK_ODDITY = NEW.$WEEK_ODDITY OR $WEEK_ODDITY IS NULL OR NEW.$WEEK_ODDITY IS NULL)
                AND $LES_ID != NEW.$LES_ID
            UNION ALL
                SELECT $WEEK_ODDITY, $DAY, NEW.$START + NEW.$DUR, $START + $DUR - (NEW.$START + NEW.$DUR), $TYPE, $SUB_ID, $ROOM
                FROM $Lessons
                WHERE $DAY = NEW.$DAY
                AND $START < NEW.$START
                AND $START + $DUR > NEW.$START + NEW.$DUR
                AND ($WEEK_ODDITY = NEW.$WEEK_ODDITY OR $WEEK_ODDITY IS NULL OR NEW.$WEEK_ODDITY IS NULL)
                AND $LES_ID != NEW.$LES_ID;
                
            -- v pripade, ze predosly prikaz nesposobil rekurzivne volanie tohto triggra, 
            -- ktory by po vlozeni kazdej z 2 hodin aktualizoval hodinu ktorá s nimi 
            -- koliduje, je nutne tuto hodinu odstranit rucne
            DELETE FROM $Lessons WHERE $LES_ID IN (
                SELECT $LES_ID
                FROM $Lessons
                WHERE $DAY = NEW.$DAY
                AND $START < NEW.$START
                AND $START + $DUR > NEW.$START + NEW.$DUR
                AND ($WEEK_ODDITY = NEW.$WEEK_ODDITY OR $WEEK_ODDITY IS NULL OR NEW.$WEEK_ODDITY IS NULL)
                AND $LES_ID != NEW.$LES_ID
            );

            -- zmiznu hodiny, ktore prebiehaju uprostred pridanej hodiny
            DELETE FROM $Lessons WHERE $LES_ID IN (
                SELECT $LES_ID
                FROM $Lessons
                WHERE $DAY = NEW.$DAY
                AND $START >= NEW.$START
                AND $START + $DUR <= NEW.$START + NEW.$DUR
                AND ($WEEK_ODDITY = NEW.$WEEK_ODDITY OR $WEEK_ODDITY IS NULL OR NEW.$WEEK_ODDITY IS NULL)
                AND $LES_ID != NEW.$LES_ID
            );

            -- upravia sa hodiny ktore zacnu skor, ale prebiehaju a koncia pocas pridanej hodiny
            UPDATE $Lessons
            SET $DUR = NEW.$START - $START
            WHERE $LES_ID IN (
                SELECT $LES_ID
                FROM $Lessons
                WHERE $DAY = NEW.$DAY
                AND $START < NEW.$START
                AND $START + $DUR BETWEEN NEW.$START + 1 AND NEW.$START + NEW.$DUR
                AND ($WEEK_ODDITY = NEW.$WEEK_ODDITY OR $WEEK_ODDITY IS NULL OR NEW.$WEEK_ODDITY IS NULL)
                AND $LES_ID != NEW.$LES_ID
            );

            -- upravia sa hodiny ktore zacnu a prebiehaju pocas pridanej hodiny, ale koncia neskor
            UPDATE $Lessons
            SET $DUR = $START + $DUR - (NEW.$START + NEW.$DUR),
                $START = NEW.$START + NEW.$DUR
            WHERE $LES_ID IN (
                SELECT $LES_ID
                FROM $Lessons
                WHERE $DAY = NEW.$DAY
                AND $START BETWEEN NEW.$START AND NEW.$START + NEW.$DUR - 1
                AND $START + $DUR > NEW.$START + NEW.$DUR
                AND ($WEEK_ODDITY = NEW.$WEEK_ODDITY OR $WEEK_ODDITY IS NULL OR NEW.$WEEK_ODDITY IS NULL)
                AND $LES_ID != NEW.$LES_ID
            );
        """
        )

        val where = "$LEFT = NEW.$LES_ID OR $MID = NEW.$LES_ID OR $RIGHT = NEW.$LES_ID"
        val cond = "EXISTS(SELECT 1 FROM $LessonsToJoin WHERE $where)"
        val joinViaUpdate = """
            UPDATE $Lessons
            SET $START = (SELECT MIN($START) FROM $LessonsToJoin WHERE $where),
                $DUR = (SELECT MAX($DUR) FROM $LessonsToJoin WHERE $where)
            WHERE $LES_ID = NEW.$LES_ID;
        """

        val joinViaInsert = """
            INSERT INTO $Lessons ($DAY, $WEEK_ODDITY, $START, $DUR, $TYPE, $SUB_ID, $ROOM)
            SELECT $DAY, $WEEK_ODDITY, MIN($START), MAX($DUR), $TYPE, $SUB_ID, $ROOM
            FROM $LessonsToJoin
            WHERE $where
            GROUP BY $DAY, $TYPE, $SUB_ID, $ROOM;
        """

        Trigger.AI.create(db, this, cond, joinViaUpdate)
        Trigger.AU.create(db, this, cond, joinViaInsert)
    }
}