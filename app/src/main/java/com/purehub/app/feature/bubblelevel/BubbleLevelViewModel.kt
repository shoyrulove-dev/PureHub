package com.purehub.app.feature.bubblelevel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BubbleLevelUiState(
    val pitch: Float = 0f,
    val roll: Float = 0f,
    val tiltMagnitude: Float = 0f,
    val errorMessage: String? = null,
    val accuracyWarning: String? = "Place the phone on a known-flat surface, then tap Calibrate zero.",
    val isCalibrated: Boolean = false,
)

class BubbleLevelViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val sensorManager = BubbleLevelSensorManager(application.applicationContext)
    private val _uiState = MutableStateFlow(BubbleLevelUiState())
    val uiState: StateFlow<BubbleLevelUiState> = _uiState.asStateFlow()

    private var sensorJob: Job? = null
    private val preferences = application.getSharedPreferences("purehub.sensor-calibration.v1", 0)
    private var pitchOffset = preferences.getFloat("bubble_pitch", 0f)
    private var rollOffset = preferences.getFloat("bubble_roll", 0f)

    fun calibrateZero() {
        pitchOffset += _uiState.value.pitch
        rollOffset += _uiState.value.roll
        preferences.edit().putFloat("bubble_pitch", pitchOffset).putFloat("bubble_roll", rollOffset).apply()
        _uiState.update { it.copy(pitch = 0f, roll = 0f, tiltMagnitude = 0f, isCalibrated = true, accuracyWarning = null) }
    }

    fun start() {
        if (sensorJob != null) return
        sensorJob = viewModelScope.launch {
            sensorManager.readings()
                .catch { throwable ->
                    _uiState.update { it.copy(errorMessage = throwable.message) }
                }
                .collect { reading ->
                    _uiState.update {
                        it.copy(
                            pitch = reading.pitch - pitchOffset,
                            roll = reading.roll - rollOffset,
                            tiltMagnitude = kotlin.math.sqrt((reading.pitch - pitchOffset) * (reading.pitch - pitchOffset) + (reading.roll - rollOffset) * (reading.roll - rollOffset)),
                            errorMessage = null,
                            isCalibrated = pitchOffset != 0f || rollOffset != 0f,
                            accuracyWarning = when {
                                reading.accuracy <= android.hardware.SensorManager.SENSOR_STATUS_UNRELIABLE -> "Accelerometer accuracy is unreliable. Keep the device still."
                                reading.gravityMagnitude !in 8.8f..10.8f -> "Device is moving; wait for the reading to settle before calibrating."
                                pitchOffset == 0f && rollOffset == 0f -> "Calibrate zero on a known-flat surface for best results."
                                else -> null
                            },
                        )
                    }
                }
        }
        sensorJob?.invokeOnCompletion { sensorJob = null }
    }

    fun stop() {
        sensorJob?.cancel()
        sensorJob = null
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }
}
