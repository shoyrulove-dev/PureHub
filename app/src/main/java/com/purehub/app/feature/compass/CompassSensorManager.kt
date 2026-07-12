package com.purehub.app.feature.compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlin.math.abs

class CompassSensorManager(
    context: Context,
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    fun azimuthFlow(): Flow<Float> = callbackFlow {
        if (accelerometer == null || magnetometer == null) {
            close(IllegalStateException("Required compass sensors are unavailable on this device."))
            return@callbackFlow
        }

        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)
        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)
        var lastHeading = 0f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> lowPass(event.values, gravity)
                    Sensor.TYPE_MAGNETIC_FIELD -> lowPass(event.values, geomagnetic)
                }

                val success = SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)
                if (!success) return

                SensorManager.getOrientation(rotationMatrix, orientation)
                val rawHeading = Math.toDegrees(orientation[0].toDouble()).toFloat()
                val normalizedHeading = ((rawHeading + 360f) % 360f)
                val stabilizedHeading = normalizeForShortestRotation(
                    current = normalizedHeading,
                    previous = lastHeading,
                )

                if (abs(stabilizedHeading - lastHeading) >= 0.5f) {
                    lastHeading = stabilizedHeading
                    trySend(stabilizedHeading)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(
            listener,
            accelerometer,
            SensorManager.SENSOR_DELAY_GAME,
        )
        sensorManager.registerListener(
            listener,
            magnetometer,
            SensorManager.SENSOR_DELAY_GAME,
        )

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }.conflate()

    private fun lowPass(input: FloatArray, output: FloatArray, alpha: Float = 0.15f) {
        for (index in input.indices) {
            output[index] += alpha * (input[index] - output[index])
        }
    }

    private fun normalizeForShortestRotation(current: Float, previous: Float): Float {
        var delta = current - previous
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        return previous + delta
    }
}
