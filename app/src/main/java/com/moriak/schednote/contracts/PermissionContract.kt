package com.moriak.schednote.contracts

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import com.moriak.schednote.enums.PermissionHandler

/**
 * Kontrakt na získanie množiny povolení od používateľa.
 */
object PermissionContract: ActivityResultContract<PermissionHandler, Boolean>() {
    private val rmp = RequestMultiplePermissions()

    override fun createIntent(context: Context, input: PermissionHandler): Intent =
        rmp.createIntent(context, input.permissions)

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean =
        rmp.parseResult(resultCode, intent).all { it.value }
}