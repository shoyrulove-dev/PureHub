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
                .collect { heading ->
                    _uiState.update {
                        it.copy(
                            heading = heading,
                            cardinalDirection = cardinalFromHeading(heading),
                            isSensorAvailable = true,
                            errorMessage = null,
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
