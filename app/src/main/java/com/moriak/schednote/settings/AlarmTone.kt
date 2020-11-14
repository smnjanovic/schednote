package com.moriak.schednote.settings

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.content.ContentUris
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.media.RingtoneManager.*
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import com.moriak.schednote.App
import com.moriak.schednote.R
import android.provider.MediaStore.Audio.Media as Tune

class AlarmTone : Parcelable {
    companion object CREATOR : Parcelable.Creator<AlarmTone> {
        override fun createFromParcel(parcel: Parcel): AlarmTone = AlarmTone(parcel)
        override fun newArray(size: Int): Array<AlarmTone?> = arrayOfNulls(size)
        private val defaultUri = getDefaultUri(TYPE_ALARM) ?: getDefaultUri(TYPE_NOTIFICATION)!!
        private fun checkValidId(id: Long): Boolean {
            val ext = Tune.EXTERNAL_CONTENT_URI
            var valid = false
            if (id > -1L && App.ctx.checkSelfPermission(READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED)
                App.ctx.contentResolver.query(
                    ext, arrayOf("1"), "${Tune._ID} = $id",
                    null, null
                )?.use { valid = it.count > 0 }
            return valid
        }
    }

    val id: Long
    val label: String
    val uri: Uri

    constructor(audioId: Long, title: String, uri: Uri, valid: Boolean = checkValidId(audioId)) {
        id = if (valid) audioId else -1L
        label = if (valid) title else App.str(R.string.default_)
        this.uri = if (valid) uri else defaultUri
    }

    constructor(audioId: Long) {
        var vId = -1L
        var vLabel = App.str(R.string.default_)
        var vUri = defaultUri
        if (audioId > -1L && App.ctx.checkSelfPermission(READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED) {
            val ext = Tune.EXTERNAL_CONTENT_URI
            App.ctx.contentResolver.query(
                ext, arrayOf(Tune._ID, Tune.DISPLAY_NAME), "${Tune._ID} = ?",
                arrayOf(audioId.toString()), null
            )?.use {
                if (it.moveToFirst()) {
                    vId = it.getLong(0)
                    vLabel = it.getString(1)
                    vUri = ContentUris.withAppendedId(ext, vId)
                }
            }
        }
        id = vId
        label = vLabel
        uri = vUri
    }

    constructor() : this(-1L)
    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString()!!,
        Uri.parse(parcel.readString())
    )

    override fun describeContents(): Int = 0
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(label)
        parcel.writeString(uri.toString())
    }
}