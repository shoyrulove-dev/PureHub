package com.purehub.app.feature.speakercleaner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SpeakerCleanerUiState(
    val isPlaying: Boolean = false,
    val frequencyHz: Float = 165f,
    val durationSeconds: Int = 30,
    val remainingSeconds: Int = 30,
    val completed: Boolean = false,
    val note: String = "A centered 165 Hz loop can help shake out light moisture and dust from speaker grills.",
)

class SpeakerCleanerViewModel(
    private val audioManager: SpeakerCleanerAudioManager = SpeakerCleanerAudioManager(),
) : ViewModel() {
    private var timerJob: Job? = null
    private val _uiState = MutableStateFlow(SpeakerCleanerUiState())
    val uiState: StateFlow<SpeakerCleanerUiState> = _uiState.asStateFlow()

    fun updateFrequency(value: Float) {
        _uiState.update { it.copy(frequencyHz = value.coerceIn(120f, 220f)) }
        if (_uiState.value.isPlaying) {
            audioManager.stop()
            audioManager.play(_uiState.value.frequencyHz.toDouble())
        }
    }

    fun selectPreset(frequency: Float) = updateFrequency(frequency)

    fun setDuration(seconds: Int) {
        if (_uiState.value.isPlaying) return
        _uiState.update { it.copy(durationSeconds = seconds, remainingSeconds = seconds) }
    }

    fun togglePlayback() {
        if (_uiState.value.isPlaying) {
            stop("Cycle stopped.")
        } else {
            audioManager.play(_uiState.value.frequencyHz.toDouble())
            _uiState.update { it.copy(isPlaying = true, completed = false, remainingSeconds = it.durationSeconds, note = "Cleaning cycle is running locally.") }
            timerJob = viewModelScope.launch {
                while (_uiState.value.remainingSeconds > 0) {
                    delay(1_000)
                    _uiState.update { it.copy(remainingSeconds = (it.remainingSeconds - 1).coerceAtLeast(0)) }
                }
                stop("Cycle complete. Compare the same familiar audio before repeating.", completed = true)
            }
        }
    }

    private fun stop(note: String, completed: Boolean = false) {
        timerJob?.cancel()
        timerJob = null
        audioManager.stop()
        _uiState.update { it.copy(isPlaying = false, completed = completed, remainingSeconds = it.durationSeconds, note = note) }
    }

    override fun onCleared() {
        stop("Tone stopped.")
        super.onCleared()
    }
}
