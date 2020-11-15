package com.moriak.schednote.database.data

import android.os.Parcel
import android.os.Parcelable
import com.moriak.schednote.R

/**
 * Trieda uchováva dáta o jednom predmete
 */
data class Subject(val id: Long, var abb: String, var name: String) : Parcelable, NoteCategory {

    /**
     * Statický objekt poskytuje dáta a pravidlá spoločné pre všetky predmety
     * @property abb_limit Maximálny počet znakov v skratke predmetu
     * @property name_limit Maximálny počet znakov v celom názve predmetu
     * @property l Výber písmen
     * @property d Výber číslic
     */
    companion object CREATOR : Parcelable.Creator<Subject> {
        override fun createFromParcel(parcel: Parcel) = Subject(parcel)
        override fun newArray(size: Int): Array<Subject?> = arrayOfNulls(size)
        const val abb_limit = 5
        const val name_limit = 48
        const val l = "a-zA-ZÀ-ž"
        const val d = "0-9"

        /**
         * Kontrola správnosti formátu skratky
         * @param abb skratka predmetu
         */
        fun validAbb(abb: String) = when {
            abb.trim().isEmpty() -> R.string.subject_abb_missing
            abb.trim().length > abb_limit -> R.string.subject_abb_too_long
            !abb.matches("^[$l$d]+$".toRegex()) -> R.string.subject_abb_format_warning
            else -> null
        }

        /**
         * Kontrola správnosti formátu názvu
         * @param name Celý názov predmetu
         */
        fun validName(name: String) = when {
            name.trim().isEmpty() -> R.string.subject_abb_missing
            name.length > name_limit -> R.string.subject_abb_too_long
            !name.matches("^[$l][$l$d ]*$".toRegex()) -> R.string.subject_abb_format_warning
            else -> null
        }
    }

    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString()!!,
        parcel.readString()!!
    )

    override fun toString() = "$abb — $name"
    override fun equals(other: Any?): Boolean = other is Subject && id == other.id
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(abb)
        parcel.writeString(name)
    }

    override fun describeContents() = 0
    override fun hashCode() = id.hashCode() * 31
}