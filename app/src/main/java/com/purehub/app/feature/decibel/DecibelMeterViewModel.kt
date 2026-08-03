package com.purehub.app.feature.decibel

import android.os.SystemClock
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
    val averageDecibel: Float = 0f,
    val averageWindowSeconds: Int = 5,
    val errorMessage: String? = null,
)

private data class DecibelSample(
    val capturedAt: Long,
    val value: Float,
)

class DecibelMeterViewModel(
    private val meterManager: DecibelMeterManager = DecibelMeterManager(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(DecibelMeterUiState())
    val uiState: StateFlow<DecibelMeterUiState> = _uiState.asStateFlow()

    private var meterJob: Job? = null
    private val samples = ArrayDeque<DecibelSample>()

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
                    val now = SystemClock.elapsedRealtime()
                    samples.addLast(DecibelSample(now, decibel))
                    while (samples.firstOrNull()?.capturedAt?.let { it < now - 60_000L } == true) {
                        samples.removeFirst()
                    }
                    _uiState.update {
                        it.copy(
                            currentDecibel = decibel,
                            peakDecibel = maxOf(it.peakDecibel, decibel),
                            averageDecibel = averageForWindow(it.averageWindowSeconds, now),
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

    fun selectAverageWindow(seconds: Int) {
        if (seconds !in setOf(5, 10, 30, 60)) return
        val now = SystemClock.elapsedRealtime()
        _uiState.update {
            it.copy(
                averageWindowSeconds = seconds,
                averageDecibel = averageForWindow(seconds, now),
            )
        }
    }

    private fun averageForWindow(seconds: Int, now: Long): Float {
        val cutoff = now - seconds * 1_000L
        val window = samples.filter { it.capturedAt >= cutoff }
        return if (window.isEmpty()) 0f else window.map { it.value }.average().toFloat()
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }
}
