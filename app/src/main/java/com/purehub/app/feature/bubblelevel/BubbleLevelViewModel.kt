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
)

class BubbleLevelViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val sensorManager = BubbleLevelSensorManager(application.applicationContext)
    private val _uiState = MutableStateFlow(BubbleLevelUiState())
    val uiState: StateFlow<BubbleLevelUiState> = _uiState.asStateFlow()

    private var sensorJob: Job? = null

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
                            pitch = reading.pitch,
                            roll = reading.roll,
                            tiltMagnitude = reading.tiltMagnitude,
                            errorMessage = null,
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
