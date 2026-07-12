package com.purehub.app.feature.speakercleaner

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SpeakerCleanerUiState(
    val isPlaying: Boolean = false,
    val frequencyHz: Float = 165f,
    val note: String = "A centered 165 Hz loop can help shake out light moisture and dust from speaker grills.",
)

class SpeakerCleanerViewModel(
    private val audioManager: SpeakerCleanerAudioManager = SpeakerCleanerAudioManager(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(SpeakerCleanerUiState())
    val uiState: StateFlow<SpeakerCleanerUiState> = _uiState.asStateFlow()

    fun updateFrequency(value: Float) {
        _uiState.update { it.copy(frequencyHz = value.coerceIn(120f, 220f)) }
        if (_uiState.value.isPlaying) {
            audioManager.stop()
            audioManager.play(_uiState.value.frequencyHz.toDouble())
        }
    }

    fun togglePlayback() {
        if (_uiState.value.isPlaying) {
            audioManager.stop()
            _uiState.update { it.copy(isPlaying = false, note = "Tone stopped.") }
        } else {
            audioManager.play(_uiState.value.frequencyHz.toDouble())
            _uiState.update { it.copy(isPlaying = true, note = "Tone is playing locally through AudioTrack.") }
        }
    }

    override fun onCleared() {
        audioManager.stop()
        super.onCleared()
    }
}
