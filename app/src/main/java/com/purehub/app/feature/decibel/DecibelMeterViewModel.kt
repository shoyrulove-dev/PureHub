package com.purehub.app.feature.decibel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DecibelMeterUiState(
    val isRunning: Boolean = false,
    val currentDecibel: Float = 0f,
    val peakDecibel: Float = 0f,
    val errorMessage: String? = null,
)

class DecibelMeterViewModel(
    private val meterManager: DecibelMeterManager = DecibelMeterManager(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(DecibelMeterUiState())
    val uiState: StateFlow<DecibelMeterUiState> = _uiState.asStateFlow()

    private var meterJob: Job? = null

    fun start() {
        if (meterJob != null) return
        meterJob = viewModelScope.launch {
            _uiState.update { it.copy(isRunning = true, errorMessage = null) }
            meterManager.levels()
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isRunning = false,
                            errorMessage = throwable.message ?: "Unable to read microphone level.",
                        )
                    }
                }
                .collect { decibel ->
                    _uiState.update {
                        it.copy(
                            currentDecibel = decibel,
                            peakDecibel = maxOf(it.peakDecibel, decibel),
                        )
                    }
                }
        }
        meterJob?.invokeOnCompletion { meterJob = null }
    }

    fun stop() {
        meterJob?.cancel()
        meterJob = null
        _uiState.update { it.copy(isRunning = false) }
    }

    fun resetPeak() {
        _uiState.update { it.copy(peakDecibel = it.currentDecibel) }
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }
}
