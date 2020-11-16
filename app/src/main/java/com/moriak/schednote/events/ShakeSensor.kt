package com.moriak.schednote.events

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.moriak.schednote.App
import kotlin.math.abs

/**
 * Trieda na zjednodušenie implementácie senzora na trasenie displeja
 * @property threshold nastavenie citlivosti trasenia. Hodnotu možno voľne meniť
 */

class ShakeSensor : SensorEventListener {
    private val sensorManager = App.ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    var threshold = 5

    private var lastX = 0F
    private var lastY = 0F
    private var lastZ = 0F

    private var onShake: () -> Unit = fun() {}

    /**
     * nastaviteľná udalosť ak dôjde k dostatočne silnému otrasu zariadenia
     * @param fn metóda, ktorá sa vykoná ak k takejto udalosť dôjde
     */
    fun setOnShake(fn: () -> Unit) {
        onShake = fn
    }

    /**
     * Aktivuje poslúchač senzoru
     */
    fun enable(): Boolean = accelerometer?.let {
        sensorManager.registerListener(
            this,
            it,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    } ?: false

    /**
     * Deaktivuje poslúchač senzoru
     */
    fun disable(): Unit = accelerometer?.let { sensorManager.unregisterListener(this) } ?: Unit

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onSensorChanged(event: SensorEvent) {
        val curX = event.values[0]
        val curY = event.values[1]
        val curZ = event.values[2]
        if (abs(curX - lastX) > threshold || abs(curY - lastY) > threshold || abs(curZ - lastZ) > threshold) onShake()

        lastX = curX
        lastY = curY
        lastZ = curZ
    }
}