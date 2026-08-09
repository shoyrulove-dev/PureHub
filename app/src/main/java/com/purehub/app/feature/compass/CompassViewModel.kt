package com.purehub.app.feature.compass

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

data class CompassUiState(
    val heading: Float = 0f,
    val cardinalDirection: String = "N",
    val isSensorAvailable: Boolean = true,
    val errorMessage: String? = null,
    val accuracyLabel: String = "Calibrating",
    val accuracyWarning: String? = "Move the phone in a figure-eight pattern before relying on the heading.",
)

class CompassViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val sensorManager = CompassSensorManager(application.applicationContext)
    private val _uiState = MutableStateFlow(CompassUiState())
    val uiState: StateFlow<CompassUiState> = _uiState.asStateFlow()

    private var compassJob: Job? = null

    fun startCompass() {
        if (compassJob != null) return

        compassJob = viewModelScope.launch {
            sensorManager.azimuthFlow()
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isSensorAvailable = false,
                            errorMessage = throwable.message,
                        )
                    }
                }
                .collect { reading ->
                    val warning = when {
                        reading.accuracy <= android.hardware.SensorManager.SENSOR_STATUS_UNRELIABLE -> "Compass accuracy is unreliable. Move the phone in a figure-eight pattern."
                        reading.magneticFieldMicroTesla !in 25f..65f -> "Possible magnetic interference (${reading.magneticFieldMicroTesla.toInt()} µT). Move away from metal or magnets."
                        else -> null
                    }
                    _uiState.update {
                        it.copy(
                            heading = reading.heading,
                            cardinalDirection = cardinalFromHeading(reading.heading),
                            isSensorAvailable = true,
                            errorMessage = null,
                            accuracyLabel = when (reading.accuracy) { 3 -> "High"; 2 -> "Medium"; 1 -> "Low"; else -> "Unreliable" },
                            accuracyWarning = warning,
                        )
                    }
                }
        }
        compassJob?.invokeOnCompletion {
            compassJob = null
        }
    }

    fun stopCompass() {
        compassJob?.cancel()
        compassJob = null
    }

    private fun cardinalFromHeading(heading: Float): String {
        val normalized = ((heading % 360f) + 360f) % 360f
        val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val index = (((normalized + 22.5f) % 360f) / 45f).toInt()
        return directions[index]
    }

    override fun onCleared() {
        stopCompass()
        super.onCleared()
    }
}
