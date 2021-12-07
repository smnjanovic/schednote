package com.moriak.schednote.activities

import android.hardware.Sensor
import android.hardware.Sensor.TYPE_ACCELEROMETER
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.SensorManager.SENSOR_DELAY_NORMAL
import android.speech.SpeechRecognizer.isRecognitionAvailable
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.moriak.schednote.App
import com.moriak.schednote.storage.Prefs.Settings.shakeEventEnabled
import com.moriak.schednote.R
import com.moriak.schednote.contracts.PermissionContract
import com.moriak.schednote.dialogs.VoiceRecognitionDialog
import com.moriak.schednote.enums.PermissionHandler

/**
 * Aktivita tohto typu po zatrasení zobrazí dialógové okno, v ktorom bude ponúkaná možnosť
 * zadať hlasovú inštrukciu. Užívateľ si môže v ňom zobraziť, aké inštrukcie môže aplikácii zadávať.
 */
abstract class ShakeCompatActivity : AppCompatActivity() {
    private companion object { private const val VOICE_FRAGMENT = "VOICE_FRAGMENT" }

    private inner class ShakeSensor(private val sensorManager: SensorManager) : SensorEventListener {
        private val threshold = 15F
        private var waitForIt: Long = 0L
        private var lastX = 0F
        private var lastY = 0F
        private var lastZ = 0F

        fun activate(active: Boolean) {
            if (!active) sensorManager.unregisterListener(this)
            else sensorManager.getDefaultSensor(TYPE_ACCELEROMETER)?.let {
                sensorManager.registerListener(this, it, SENSOR_DELAY_NORMAL)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        override fun onSensorChanged(event: SensorEvent) {
            val curX = event.values[0]
            val curY = event.values[1]
            val curZ = event.values[2]
            val bounds = -threshold..threshold
            val dx = curX - lastX !in bounds
            val dy = curY - lastY !in bounds
            val dz = curZ - lastZ !in bounds
            lastX = curX
            lastY = curY
            lastZ = curZ

            if (dx || dy || dz) {
                val now = System.currentTimeMillis()
                if (now >= waitForIt) {
                    waitForIt = now + 1500
                    onShake()
                }
            }
        }
    }

    private val launcher = registerForActivityResult(PermissionContract) {}
    private val shake by lazy { ShakeSensor(getSystemService(SensorManager::class.java)) }

    override fun onResume() {
        super.onResume()
        shake.activate(true)
    }

    override fun onPause() {
        super.onPause()
        shake.activate(false)
    }

    /**
     * Udalosť sa spustí po každom zatrasení zariadenia.
     */
    protected open fun onShake() {
        if (shakeEventEnabled) {
            PermissionHandler.VOICE_RECORD.allowMe(this, launcher) {
                val noDialog = !supportFragmentManager.fragments.any { f ->
                    f is DialogFragment && f.dialog?.isShowing == true
                }
                if (noDialog) {
                    if (!isRecognitionAvailable(this)) App.toast(R.string.voice_err_unavailable)
                    else VoiceRecognitionDialog().show(supportFragmentManager, VOICE_FRAGMENT)
                }
            }
        }
    }
}