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
     * @param p_cond Podmienka, za akej sa trigger vykoná. Môže byť null, v takom prípade sa vykoná vždy
     * @param p_body SQL algoritmus ktorý sa vykoná pri špecifickej udalosti
     */
    fun create(db: SQLiteDatabase, table: Table, p_cond: String?, p_body: String) {
        fun newestOldest(str: String): String {
            return str.replace(
                "NEWEST.",
                if (this == BD || this == AD || this == ID) "OLD." else "NEW."
            ).replace(
                "OLDEST.",
                if (this == BI || this == AI || this == II) "NEW." else "OLD."
            )
        }

        val cond = p_cond?.let { newestOldest(p_cond) }
        val body = newestOldest(p_body)
        when (this) {
            BI, AI, II, BU, AU, IU, BD, AD, ID -> {
                val bai = when (this) {
                    BI, BU, BD -> "BEFORE"
                    AI, AU, AD -> "AFTER"
                    else -> "INSTEAD OF"
                }
                val iud = when (this) {
                    BI, AI, II -> "INSERT"
                    BU, AU, IU -> "UPDATE"
                    else -> "DELETE"
                }
                val trigger = "${name.toLowerCase(Locale.ROOT)}_$table${++order}"
                db.execSQL("CREATE TRIGGER $trigger $bai $iud ON $table${cond?.let { " WHEN $it" } ?: ""} BEGIN $body END;")
            }
            BIU -> {
                BI.create(db, table, p_cond, p_body)
                BU.create(db, table, p_cond, p_body)
            }
            AIU -> {
                AI.create(db, table, p_cond, p_body)
                AU.create(db, table, p_cond, p_body)
            }
            IIU -> {
                II.create(db, table, p_cond, p_body)
                IU.create(db, table, p_cond, p_body)
            }

            BID -> {
                BI.create(db, table, p_cond, p_body)
                BD.create(db, table, p_cond, p_body)
            }
            AID -> {
                AI.create(db, table, p_cond, p_body)
                AD.create(db, table, p_cond, p_body)
            }
            IID -> {
                II.create(db, table, p_cond, p_body)
                ID.create(db, table, p_cond, p_body)
            }

            BUD -> {
                BU.create(db, table, p_cond, p_body)
                BD.create(db, table, p_cond, p_body)
            }
            AUD -> {
                AU.create(db, table, p_cond, p_body)
                AD.create(db, table, p_cond, p_body)
            }
            IUD -> {
                IU.create(db, table, p_cond, p_body)
                ID.create(db, table, p_cond, p_body)
            }

            BIUD -> {
                BI.create(db, table, p_cond, p_body)
                BU.create(db, table, p_cond, p_body)
                BD.create(db, table, p_cond, p_body)
            }
            AIUD -> {
                AI.create(db, table, p_cond, p_body)
                AU.create(db, table, p_cond, p_body)
                AD.create(db, table, p_cond, p_body)
            }
            IIUD -> {
                II.create(db, table, p_cond, p_body)
                IU.create(db, table, p_cond, p_body)
                ID.create(db, table, p_cond, p_body)
            }
        }
    }
}
