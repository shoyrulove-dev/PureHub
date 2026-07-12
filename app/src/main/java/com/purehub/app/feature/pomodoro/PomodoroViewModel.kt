package com.purehub.app.feature.pomodoro

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PomodoroPreset(
    val label: String,
    val minutes: Int,
)

data class PomodoroUiState(
    val selectedPreset: PomodoroPreset = PomodoroPreset("Focus 25", 25),
    val secondsRemaining: Int = 25 * 60,
    val isRunning: Boolean = false,
    val selectedSoundscape: String = "Rain",
    val volume: Float = 0.35f,
    val note: String = "Select a soundscape and press Start to play a local loop.",
) {
    val progress: Float
        get() {
            val total = selectedPreset.minutes * 60f
            if (total <= 0f) return 0f
            return secondsRemaining / total
        }
}

class PomodoroViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val presets = listOf(
        PomodoroPreset("Focus 25", 25),
        PomodoroPreset("Deep 50", 50),
        PomodoroPreset("Reset 10", 10),
    )
    private val audioManager = PomodoroAudioManager(application.applicationContext)

    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun presets(): List<PomodoroPreset> = presets

    fun selectPreset(preset: PomodoroPreset) {
        timerJob?.cancel()
        timerJob = null
        audioManager.stop()
        _uiState.update {
            it.copy(
                selectedPreset = preset,
                secondsRemaining = preset.minutes * 60,
                isRunning = false,
            )
        }
    }

    fun selectSoundscape(label: String) {
        _uiState.update {
            it.copy(
                selectedSoundscape = label,
                note = "Selected $label. Local loop is ready for playback.",
            )
        }
        if (_uiState.value.isRunning) {
            audioManager.play(label, _uiState.value.volume)
        }
    }

    fun updateVolume(volume: Float) {
        _uiState.update { it.copy(volume = volume.coerceIn(0f, 1f)) }
        audioManager.setVolume(_uiState.value.volume)
    }

    fun toggleTimer() {
        if (_uiState.value.isRunning) {
            pause()
        } else {
            startTimer()
        }
    }

    fun reset() {
        timerJob?.cancel()
        timerJob = null
        audioManager.fadeOutAndStop()
        _uiState.update {
            it.copy(
                secondsRemaining = it.selectedPreset.minutes * 60,
                isRunning = false,
                note = "Timer reset. Local playback stopped.",
            )
        }
    }

    private fun pause() {
        timerJob?.cancel()
        timerJob = null
        audioManager.fadeOutAndStop()
        _uiState.update {
            it.copy(
                isRunning = false,
                note = "Paused. Resume when ready.",
            )
        }
    }

    private fun startTimer() {
        if (_uiState.value.secondsRemaining <= 0) {
            reset()
        }
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRunning = true,
                    note = "Focus mode active with ${it.selectedSoundscape}.",
                )
            }
            audioManager.play(_uiState.value.selectedSoundscape, _uiState.value.volume)

            while (_uiState.value.secondsRemaining > 0) {
                delay(1_000)
                _uiState.update { state ->
                    state.copy(secondsRemaining = (state.secondsRemaining - 1).coerceAtLeast(0))
                }
            }

            audioManager.fadeOutAndStop()
            _uiState.update {
                it.copy(
                    isRunning = false,
                    note = "Session complete. Local loop stopped automatically.",
                )
            }
            timerJob = null
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        audioManager.stop()
        super.onCleared()
    }
}
