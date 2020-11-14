package com.moriak.schednote.database

import android.database.sqlite.SQLiteDatabase
import com.moriak.schednote.database.tables.Table
import java.util.*

enum class Trigger {
    BI,     // BEFORE INSERT
    AI,     // AFTER INSERT
    II,     // INSTEAD OF INSERT
    BU,     // BEFORE UPDATE
    AU,     // AFTER UPDATE
    IU,     // INSTEAD OF UPDATE
    BD,     // BEFORE DELETE
    AD,     // AFTER DELETE
    ID,     // INSTEAD OF DELETE
    BIU,    // BEFORE INSERT OR UPDATE
    AIU,    // AFTER INSERT OR UPDATE
    IIU,    // INSTEAD OF INSERT OR UPDATE
    BID,    // BEFORE INSERT OR DELETE
    AID,    // AFTER INSERT OR DELETE
    IID,    // INSTEAD OF INSERT OR DELETE
    BUD,    // BEFORE UPDATE OR DELETE
    AUD,    // AFTER UPDATE OR DELETE
    IUD,    // INSTEAD OF UPDATE OR DELETE
    BIUD,   // BEFORE INSERT OR UPDATE OR DELETE
    AIUD,   // AFTER INSERT OR UPDATE OR DELETE
    IIUD;   // INSTEAD OF INSERT OR UPDATE OR DELETE

    companion object {
        private var order = 0 //teraz sa nikdy nestane, ze vytvorim 2 triggre s rovnakym nazvom
    }

    fun create(db: SQLiteDatabase, table: Table, body: String) = create(db, table, null, body)
    fun create(db: SQLiteDatabase, table: Table, cond: String?, body: String) {
        when (this) {
            BI, AI, II, BU, AU, IU, BD, AD, ID -> {
                fun newestOldest(str: String) = str
                    .replace(
                        "NEWEST.",
                        if (this == BD || this == AD || this == ID) "OLD." else "NEW."
                    )
                    .replace(
                        "OLDEST.",
                        if (this == BI || this == AI || this == II) "NEW." else "OLD."
                    )
                // before or after or instead of
                val bai = when (this) {
                    BI, BU, BD -> "BEFORE"
                    AI, AU, AD -> "AFTER"
                    else -> "INSTEAD OF"
                }
                // insert or update or delete
                val iud = when (this) {
                    BI, AI, II -> "INSERT"
                    BU, AU, IU -> "UPDATE"
                    else -> "DELETE"
                }

                val triggerName = "${name.toLowerCase(Locale.ROOT)}_$table${++order}"
                val condition = cond?.let { "WHEN ${newestOldest(it)}" } ?: ""

                db.execSQL(
                    "CREATE TRIGGER $triggerName $bai $iud ON $table\n$condition BEGIN ${
                        newestOldest(
                            body
                        )
                    } END;"
                )
            }
            BIU -> {
                BI.create(db, table, cond, body); BU.create(db, table, cond, body)
            }
            AIU -> {
                AI.create(db, table, cond, body); AU.create(db, table, cond, body)
            }
            IIU -> {
                II.create(db, table, cond, body); IU.create(db, table, cond, body)
            }

            BID -> {
                BI.create(db, table, cond, body); BD.create(db, table, cond, body)
            }
            AID -> {
                AI.create(db, table, cond, body); AD.create(db, table, cond, body)
            }
            IID -> {
                II.create(db, table, cond, body); ID.create(db, table, cond, body)
            }

            BUD -> {
                BU.create(db, table, cond, body); BD.create(db, table, cond, body)
            }
            AUD -> {
                AU.create(db, table, cond, body); AD.create(db, table, cond, body)
            }
            IUD -> {
                IU.create(db, table, cond, body); ID.create(db, table, cond, body)
            }

            BIUD -> {
                BI.create(db, table, cond, body); BU.create(db, table, cond, body); BD.create(
                    db,
                    table,
                    cond,
                    body
                )
            }
            AIUD -> {
                AI.create(db, table, cond, body); AU.create(db, table, cond, body); AD.create(
                    db,
                    table,
                    cond,
                    body
                )
            }
            IIUD -> {
                II.create(db, table, cond, body); IU.create(db, table, cond, body); ID.create(
                    db,
                    table,
                    cond,
                    body
                )
            }
        }
    }
}
