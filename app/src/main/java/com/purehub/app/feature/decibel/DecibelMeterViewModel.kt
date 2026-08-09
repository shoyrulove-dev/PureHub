package com.purehub.app.feature.decibel

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
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
    val calibrationOffset: Float = 0f,
    val accuracyWarning: String = "Relative estimate only. Calibrate against a trusted sound meter for improved readings.",
)

private data class DecibelSample(
    val capturedAt: Long,
    val value: Float,
)

class DecibelMeterViewModel @JvmOverloads constructor(
    application: Application,
    private val meterManager: DecibelMeterManager = DecibelMeterManager(),
) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("purehub.sensor-calibration.v1", 0)
    private val _uiState = MutableStateFlow(DecibelMeterUiState(calibrationOffset = preferences.getFloat("decibel_offset", 0f)))
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
                .collect { rawDecibel ->
                    val decibel = (rawDecibel + _uiState.value.calibrationOffset).coerceIn(0f, 120f)
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

    fun setCalibrationOffset(value: Float) {
        val safe = value.coerceIn(-20f, 20f)
        preferences.edit().putFloat("decibel_offset", safe).apply()
        _uiState.update { it.copy(calibrationOffset = safe, accuracyWarning = if (safe == 0f) "Relative estimate only. Calibrate against a trusted sound meter for improved readings." else "Calibration offset ${if (safe >= 0) "+" else ""}${safe.toInt()} dB is active.") }
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
