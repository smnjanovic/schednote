package com.moriak.schednote.contracts

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract

/**
 * Kontrakt na získanie mediálneho súboru (obrázok, video, audio, ...)
 */
object PickContract: ActivityResultContract<Uri, Uri?>() {
    override fun createIntent(context: Context, input: Uri): Intent = Intent(Intent.ACTION_PICK, input)
    override fun parseResult(resultCode: Int, intent: Intent?): Uri? = intent?.data
}
