package com.moriak.schednote.enums

import android.Manifest.permission.*
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.text.Html.fromHtml
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import com.moriak.schednote.App
import com.moriak.schednote.R.string.*
import com.moriak.schednote.enums.PermissionHandler.*

/**
 * Táto trieda poskytuje množinu povolení a informáciu pre používateľa o tom, prečo toto povolenie aplikácia potrebuje
 * [IMAGE_ACCESS] - žiada o prístup k fotkám.
 * [AUDIO_ACCESS] - žiada o prístup k hudbe.
 * [VOICE_RECORD] - žiada o povolenie nahrávania hlasu
 * [CALENDAR] - žiada o povolenie čítania a zápisu do kalendára
 * @property permissions Zoznam povolení
 * @property rationale Dôvod prečo aplikácia potrebuje povolenia [permissions].
 */
enum class PermissionHandler(val permissions: Array<String>, @StringRes val rationale: Int) {
    IMAGE_ACCESS(arrayOf(READ_EXTERNAL_STORAGE), rationale_access_images),
    AUDIO_ACCESS(arrayOf(READ_EXTERNAL_STORAGE), rationale_access_music),
    VOICE_RECORD(arrayOf(RECORD_AUDIO), rationale_record_audio),
    CALENDAR(arrayOf(READ_CALENDAR, WRITE_CALENDAR), rationale_calendar);

    private fun spanned(ctx: Context) = fromHtml(ctx.getString(rationale), FROM_HTML_MODE_LEGACY)!!

    /**
     * Skontroluje, či má aplikácia všetky potrebné povolenia od užívateľa
     * @param context
     * @return true, ak aplikácia má všetky povolenia
     */
    fun isAllowed(context: Context) = permissions.all { context.checkSelfPermission(it) == PERMISSION_GRANTED }

    /**
     * Ak boli všetky povolenia už užívateľom schválené vykoná sa obsah funkcie [grantedCallback].
     * @param context
     * @param grantedCallback funkcia, ktorá sa vykoná, len ak sú všetky súvisiace povolenia pridelené
     * @return true, ak sa vykonal [grantedCallback], teda aplikácia všetky má všetky povolenia
     */
    fun ifAllowed(context: Context, grantedCallback: () -> Unit): Boolean {
        val allowed = isAllowed(context)
        if (allowed) grantedCallback() else App.toast(spanned(context), true)
        return allowed
    }

    /**
     * Ak boli všetky potrebné povolenia už schválené vykoná sa funkcia [grantedCallback].
     * Inak sa vyžiadajú potrebné povolenia od užívateľa využitím [launcher].
     * Užívateľ sa medzitým dozvie, k čomu tie povolenia aplikácia potrebuje.
     * @param context kontext
     * @param launcher Objekt v ktorom prebehne žiadosť o zoznam povolení
     * @param grantedCallback funkcia, ktorá sa vykoná, keď má aplikácia pridelené všetky potrebné povolenia
     */
    fun allowMe(context: Context, launcher: ActivityResultLauncher<PermissionHandler>, grantedCallback: () -> Unit) {
        when {
            isAllowed(context) -> grantedCallback()
            else -> AlertDialog.Builder(context)
                .setMessage(spanned(context))
                .setNegativeButton(permission_deny) { _, _ -> App.toast(permission_denied) }
                .setPositiveButton(permission_grant) { _, _ -> launcher.launch(this) }
                .show()
        }
    }
}