package com.moriak.schednote.database

import android.database.sqlite.SQLiteDatabase
import com.moriak.schednote.database.Trigger.*
import com.moriak.schednote.database.tables.Table
import java.util.*

/**
 * Trieda umožňuje vytvoriť trigger s unikátnym názvom pre udalosti reprezentované hodnotami
 * [Trigger].
 * @property BI BEFORE INSERT
 * @property AI AFTER INSERT
 * @property II INSTEAD OF INSERT
 * @property BU BEFORE UPDATE
 * @property AU AFTER UPDATE
 * @property IU INSTEAD OF UPDATE
 * @property BD BEFORE DELETE
 * @property AD AFTER DELETE
 * @property ID INSTEAD OF DELETE
 * @property BIU BEFORE INSERT OR UPDATE
 * @property AIU AFTER INSERT OR UPDATE
 * @property IIU INSTEAD OF INSERT OR UPDATE
 * @property BID BEFORE INSERT OR DELETE
 * @property AID AFTER INSERT OR DELETE
 * @property IID INSTEAD OF INSERT OR DELETE
 * @property BUD BEFORE UPDATE OR DELETE
 * @property AUD AFTER UPDATE OR DELETE
 * @property IUD INSTEAD OF UPDATE OR DELETE
 * @property BIUD BEFORE INSERT OR UPDATE OR DELETE
 * @property AIUD AFTER INSERT OR UPDATE OR DELETE
 * @property IIUD INSTEAD OF INSERT OR UPDATE OR DELETE
 */
enum class Trigger {
    BI, AI, II, BU, AU, IU, BD, AD, ID,
    BIU, AIU, IIU, BID, AID, IID, BUD, AUD, IUD,
    BIUD, AIUD, IIUD;

    private companion object {
        //scitavat o 1 po kazdom novom triggri, aby sa nestalo, ze vytvorim 2 triggre s rovnakym nazvom
        private var order = 0
    }

    /**
     * Vytvorenie triggra s daným obsahom
     * @param db Databása ku ktorej mám prístup
     * @param table Tabuľka pre ktorú nastavujem trigger
     * @param body SQL algoritmus ktorý sa vykoná pri špecifickej udalosti
     */
    fun create(db: SQLiteDatabase, table: Table, body: String) = create(db, table, null, body)

    /**
     * Vytvorenie triggra s daným obsahom
     * @param db Databása ku ktorej mám prístup
     * @param table Tabuľka pre ktorú nastavujem trigger
     * @param cond Podmienka, za akej sa trigger vykoná. Môže byť null, v takom prípade sa vykoná vždy
     * @param body SQL algoritmus ktorý sa vykoná pri špecifickej udalosti
     */
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
                BI.create(db, table, cond, body)
                BU.create(db, table, cond, body)
            }
            AIU -> {
                AI.create(db, table, cond, body)
                AU.create(db, table, cond, body)
            }
            IIU -> {
                II.create(db, table, cond, body)
                IU.create(db, table, cond, body)
            }

            BID -> {
                BI.create(db, table, cond, body)
                BD.create(db, table, cond, body)
            }
            AID -> {
                AI.create(db, table, cond, body)
                AD.create(db, table, cond, body)
            }
            IID -> {
                II.create(db, table, cond, body)
                ID.create(db, table, cond, body)
            }

            BUD -> {
                BU.create(db, table, cond, body)
                BD.create(db, table, cond, body)
            }
            AUD -> {
                AU.create(db, table, cond, body)
                AD.create(db, table, cond, body)
            }
            IUD -> {
                IU.create(db, table, cond, body)
                ID.create(db, table, cond, body)
            }

            BIUD -> {
                BI.create(db, table, cond, body)
                BU.create(db, table, cond, body)
                BD.create(db, table, cond, body)
            }
            AIUD -> {
                AI.create(db, table, cond, body)
                AU.create(db, table, cond, body)
                AD.create(db, table, cond, body)
            }
            IIUD -> {
                II.create(db, table, cond, body)
                IU.create(db, table, cond, body)
                ID.create(db, table, cond, body)
            }
        }
    }
}
