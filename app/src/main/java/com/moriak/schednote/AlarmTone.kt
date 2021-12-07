package com.moriak.schednote

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore.Audio.Media.IS_RINGTONE
import com.moriak.schednote.enums.PermissionHandler
import java.io.FileNotFoundException
import android.provider.MediaStore.Audio.Media.DISPLAY_NAME as LABEL
import android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI as EXT_URI
import android.provider.MediaStore.Audio.Media.INTERNAL_CONTENT_URI as INT_URI
import android.provider.MediaStore.Audio.Media._ID as ID

/**
 * Inštancia tejto triedy uchováva ID, názov a URI, k zvukovému súboru.
 *
 * @property id ID skladby uloženej v externej pamäti
 * @property label názov skladby
 * @property uri Prístupové URI k skladbe
 */
class AlarmTone private constructor (val id: Long, val label: String, val uri: Uri) {
    companion object {
        private val default: AlarmTone by lazy {
            var tone: AlarmTone? = null
            App.ctx.contentResolver.query(INT_URI, arrayOf(ID, LABEL), "$IS_RINGTONE = 1", null, null)?.use {
                if (it.count > 0) {
                    it.moveToFirst()
                    val vId = it.getLong(0)
                    val vLabel = it.getString(1)
                    tone = AlarmTone(vId, vLabel, ContentUris.withAppendedId(INT_URI, vId))
                }
            }
            tone ?: throw FileNotFoundException ("No sound files in this device!")
        }

        fun seek(pUri: Uri?): AlarmTone {
            pUri ?: return default
            var tone: AlarmTone? = null

            if (pUri.toString().substring(0, EXT_URI.toString().length) == EXT_URI.toString()) {
                PermissionHandler.AUDIO_ACCESS.ifAllowed(App.ctx) {
                    App.ctx.contentResolver.query(pUri, arrayOf(ID, LABEL), null, null, null)?.use {
                        if (it.moveToFirst()) {
                            val vId = it.getLong(0)
                            val vLabel = it.getString(1)
                            tone = AlarmTone(vId, vLabel, pUri)
                        }
                    }
                }
                return tone ?: default
            }

            App.ctx.contentResolver.query(pUri, arrayOf(ID, LABEL), "$IS_RINGTONE = 1", null, null)?.use {
                if (it.moveToFirst()) {
                    val vId = it.getLong(0)
                    val vLabel = it.getString(1)
                    tone = AlarmTone(vId, vLabel, pUri)
                }
            }
            return tone ?: default
        }

        /**
         * Načíta zoznam skladieb, ktoré možno nastaviť ako tón budenia
         * @param list zoznam do ktorého budú tieto tóny budenia vložené
         * @return vráti ten istý zoznam, ktorý vstúpil ako parameter, s novými položkami
         */
        fun <T: MutableList<AlarmTone>>seek(list: T): T {
            list.clear()
            App.ctx.contentResolver.query(INT_URI, arrayOf(ID, LABEL), "$IS_RINGTONE = 1", null, null)?.use {
                while (it.moveToNext()) {
                    val vId = it.getLong(0)
                    val vLabel = it.getString(1)
                    list.add(AlarmTone(vId, vLabel, ContentUris.withAppendedId(INT_URI, vId)))
                }
            }
            return list
        }
    }
}