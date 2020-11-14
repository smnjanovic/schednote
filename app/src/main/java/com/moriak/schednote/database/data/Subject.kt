package com.moriak.schednote.database.data

import android.os.Parcel
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import com.moriak.schednote.R

data class Subject(val id: Long, var abb: String, var name: String) : Parcelable, NoteCategory {
    companion object CREATOR : Parcelable.Creator<Subject> {
        override fun createFromParcel(parcel: Parcel) =
            Subject(parcel)

        override fun newArray(size: Int): Array<Subject?> = arrayOfNulls(size)

        const val abb_limit = 5
        const val name_limit = 48
        const val l = "a-zA-ZÀ-ž"
        const val d = "0-9"

        fun validAbb(abb: String) = when {
            abb.trim().isEmpty() -> R.string.subject_abb_missing
            abb.trim().length > abb_limit -> R.string.subject_abb_too_long
            !abb.matches("^[$l$d]+$".toRegex()) -> R.string.subject_abb_format_warning
            else -> null
        }

        fun validName(name: String) = when {
            name.trim().isEmpty() -> R.string.subject_abb_missing
            name.length > name_limit -> R.string.subject_abb_too_long
            !name.matches("^[$l][$l$d ]*$".toRegex()) -> R.string.subject_abb_format_warning
            else -> null
        }
    }

    object AbbWatcher : TextWatcher {
        private var st = 0
        private var en = 0
        private val bad = "[^$l]".toRegex()
        override fun afterTextChanged(s: Editable?) {
            if (s != null) {
                if (s.length > abb_limit) s.delete(st, en)
                if (s.contains(bad)) s.delete(st, en)
                if (s.contains(bad)) s.replace(0, s.length, s.replace(bad, ""))
            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            st = start
            en = start + count
        }
    }

    object NameWatcher : TextWatcher {
        private var st = 0
        private var en = 0
        private val illegal = "(^[^a-zA-ZÀ-ž])|([^a-zA-ZÀ-ž0-9 ])|([ ][ ]+)|(^[ ]+)".toRegex()
        override fun afterTextChanged(s: Editable?) {
            if (s != null) {
                if (s.length > name_limit) s.delete(st, en)
                if (s.contains(illegal)) s.delete(st, en)
                if (s.contains(illegal))
                    s.replace(
                        0, s.length, s.replace("[^a-zA-ZÀ-ž0-9 ]+".toRegex(), "")
                            .trimStart().replace("^[0-9]+".toRegex(), "")
                    )
            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            st = start
            en = start + count
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