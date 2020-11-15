package com.moriak.schednote.activities

import android.Manifest.permission.RECORD_AUDIO
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.speech.SpeechRecognizer
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.moriak.schednote.App
import com.moriak.schednote.R
import com.moriak.schednote.dialogs.VoiceRecognitionDialog
import com.moriak.schednote.events.ShakeSensor
import com.moriak.schednote.settings.Prefs

/**
 * Aktivita s implementovanými senzormi pohybu. Každý potomok tejto aktivity po zatrasení
 * zobrazí dialóg, v ktorom bude možné zadať hlasovú inštrukciu. Dialóg bude poskytovať možné inštrukcie
 * vo vybranom jazyku. Pre zobrazenie tohoto dialógu treba povoliť nahrávanie zvuku a v nastaveniach povoliť
 * hlasové inštrukcie.
 */
abstract class ShakeCompatActivity : AppCompatActivity() {
    companion object {
        private const val GIMME_VOICE = 10
        private const val VOICE_FRAGMENT = "VOICE_FRAGMENT"
    }

    private val shake = ShakeSensor()

    /**
     * Nastavenie citlivosti otrasu a následné vykonanie udalosti
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shake.threshold = 15
        shake.setOnShake { onShake() }
    }

    /**
     * Po zatrasení sa zobrazí rozhranie pre zadávanie hlasových pokynov. Aby to malo význam, musí byť povolené nahrávanie hlasu
     * @param request kód žiadosti
     * @param permissions zoznam potrebných povolení
     * @param granted zoznam povolených poolení
     */
    override fun onRequestPermissionsResult(
        request: Int,
        permissions: Array<out String>,
        granted: IntArray
    ) = when (request) {
        GIMME_VOICE -> if (granted.firstOrNull() != PERMISSION_GRANTED) App.toast(R.string.permission_denied) else onShake()
        else -> super.onRequestPermissionsResult(request, permissions, granted)
    }

    /**
     * Kontrola povolení
     * Zdroj: https://www.youtube.com/watch?v=0bLwXw5aFOs
     */
    protected fun checkPermission(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PERMISSION_GRANTED

    /**
     * Aktivácia senzoru trasenia
     */
    override fun onResume() {
        super.onResume()
        shake.enable()
    }

    /**
     * Aktivácia senzoru trasenia
     */
    override fun onPause() {
        super.onPause()
        shake.disable()
    }

    /**
     * Udalosť sa spustí po zatrasení zariadenia. Pôvodne sa má zobraziť dialóg s inštrukciami,
     * ktoré môže užívateľ vysloviť do mikrofónu
     */
    protected open fun onShake() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) App.toast(R.string.voice_err_unavailable)
        else if (!checkPermission(RECORD_AUDIO)) requestPermissions(
            arrayOf(RECORD_AUDIO),
            GIMME_VOICE
        )
        else if (Prefs.settings.shakeEventEnabled) {
            var dialogShown = false
            for (f in supportFragmentManager.fragments)
                if (dialogShown) break
                else if (f is DialogFragment && f.dialog?.isShowing == true) dialogShown = true
            if (!dialogShown) VoiceRecognitionDialog().show(supportFragmentManager, VOICE_FRAGMENT)
        }
    }

}