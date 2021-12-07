package com.moriak.schednote.enums

import android.database.sqlite.SQLiteDatabase
import com.moriak.schednote.storage.Table
import com.moriak.schednote.enums.Trigger.*

/**
 * Trieda umožňuje vytvoriť 1 - 3 triggre s unikátnym názvom a spoločným algoritmom.
 *
 * Každá z následovných inštancií vytvára triggre ktoré sa spustia
 * po rozdielných udalostiach SQL transakcie.
 *
 * Udalosti:
 * [BI] BEFORE INSERT
 * [AI] AFTER INSERT
 * [II] INSTEAD OF INSERT
 * [BU] BEFORE UPDATE
 * [AU] AFTER UPDATE
 * [IU] INSTEAD OF UPDATE
 * [BD] BEFORE DELETE
 * [AD] AFTER DELETE
 * [ID] INSTEAD OF DELETE
 * [BIU] BEFORE INSERT OR UPDATE
 * [AIU] AFTER INSERT OR UPDATE
 * [IIU] INSTEAD OF INSERT OR UPDATE
 * [BID] BEFORE INSERT OR DELETE
 * [AID] AFTER INSERT OR DELETE
 * [IID] INSTEAD OF INSERT OR DELETE
 * [BUD] BEFORE UPDATE OR DELETE
 * [AUD] AFTER UPDATE OR DELETE
 * [IUD] INSTEAD OF UPDATE OR DELETE
 * [BIUD] BEFORE INSERT OR UPDATE OR DELETE
 * [AIUD] AFTER INSERT OR UPDATE OR DELETE
 * [IIUD] INSTEAD OF INSERT OR UPDATE OR DELETE
 */
enum class Trigger {
    BI, BU, BD, AI, AU, AD, II, IU, ID,
    BIU, BID, BUD, AIU, AID, AUD, IIU, IID, IUD,
    BIUD, AIUD, IIUD;
    private companion object { private var order = 0 }

    private val `when` = when (name.first()) {
        'A' -> "AFTER"
        'B' -> "BEFORE"
        else -> "INSTEAD OF"
    }

    private val transactions = Array(name.length - 1) {
        when (name[it + 1]) {
            'I' -> "INSERT"
            'U' -> "UPDATE"
            else -> "DELETE"
        }
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
        fun newestOldest(str: String): String = str
            .replace("NEWEST.", if (this == BD || this == AD || this == ID) "OLD." else "NEW.")
            .replace("OLDEST.", if (this == BI || this == AI || this == II) "NEW." else "OLD.")
        val cond = p_cond?.let { "WHEN ${newestOldest(p_cond)}" } ?: ""
        val body = newestOldest(p_body)
        transactions.forEach {
            db.execSQL("CREATE TRIGGER ${name}_$table${++order} $`when` $it ON $table $cond BEGIN $body END;")
        }
    }
}
