package com.purehub.app.feature.bubblelevel

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlin.math.sqrt

data class BubbleLevelReading(
    val pitch: Float,
    val roll: Float,
    val tiltMagnitude: Float,
)

class BubbleLevelSensorManager(
    context: Context,
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    fun readings(): Flow<BubbleLevelReading> = callbackFlow {
        if (accelerometer == null) {
            close(IllegalStateException("Accelerometer is unavailable on this device."))
            return@callbackFlow
        }

        val gravity = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                lowPass(event.values, gravity)

                val x = gravity[0]
                val y = gravity[1]
                val z = gravity[2].coerceAtLeast(0.001f)
                val pitch = Math.toDegrees(kotlin.math.atan2(x.toDouble(), z.toDouble())).toFloat()
                val roll = Math.toDegrees(kotlin.math.atan2(y.toDouble(), z.toDouble())).toFloat()
                val tiltMagnitude = sqrt(x * x + y * y)

                trySend(
                    BubbleLevelReading(
                        pitch = pitch,
                        roll = roll,
                        tiltMagnitude = tiltMagnitude,
                    ),
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(
            listener,
            accelerometer,
            SensorManager.SENSOR_DELAY_GAME,
        )

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }.conflate()

    private fun lowPass(input: FloatArray, output: FloatArray, alpha: Float = 0.18f) {
        for (index in input.indices) {
            output[index] += alpha * (input[index] - output[index])
        }
    }
}
