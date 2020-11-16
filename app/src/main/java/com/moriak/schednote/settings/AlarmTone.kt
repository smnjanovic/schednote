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

/**
 * Trieda uchováva a poskytuje informácie o skladbe uloženej v zariadení.
 * Existencia tohoto súboru sa overuje pri vzniku inštancie ale už nie počas jej existencie
 *
 * @property id ID skladby uloženej v externej pamäti
 * @property label názov skladby
 * @property uri Prístupové URI k skladbe
 */
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

    /**
     * Tvorba inštancie na uchovanie informácii o skladbe. Vkladané hodnoty nemusia byť použité,
     * ak skladba nie je dostupná. Dostupnosť je kontrolovaná vtedy, keď nie je vyplnená hodnota valid
     *
     * @param audioId ID hľadanej skladby
     * @param title Názov skladby
     * @param valid Pri 100% istote, že je súbor dostupný nastavte hodnotu na true, inak
     * nenastavujte nič. V takom prípade prebehne kontrola dostupnosti sľúbeného súboru
     */
    constructor(audioId: Long, title: String, uri: Uri, valid: Boolean = checkValidId(audioId)) {
        id = if (valid) audioId else -1L
        label =
            if (valid) title.replace("^(.*)\\..*$".toRegex(), "$1") else App.str(R.string.default_)
        this.uri = if (valid) uri else defaultUri
    }

    /**
     * Tvorba inštancie na uchovanie  o skladbe. Ak sa skladba s uvedeným ID [audioId] nájde
     * informácie o nej sa zapíšu do novej inštancie. Inak sa vytvorí inštancia s predvolenými
     * hodnotami.
     *
     * @param audioId ID skladby
     */
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

    /**
     * Tvorba inštancie na uchovanie ktorá uchováva informácie o predvolenej skladbe budenia.
     */
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