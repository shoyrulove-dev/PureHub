package com.purehub.app.feature.flashlight

import android.app.Application
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SmartFlashlightUiState(
    val isTorchOn: Boolean = false,
    val isPatternRunning: Boolean = false,
    val statusMessage: String = "Torch ready for local control.",
    val errorMessage: String? = null,
)

class SmartFlashlightViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val cameraManager = application.getSystemService(CameraManager::class.java)
    private val torchCameraId = cameraManager.cameraIdList.firstOrNull { id ->
        val characteristics = cameraManager.getCameraCharacteristics(id)
        characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
            characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
    }

    private val _uiState = MutableStateFlow(SmartFlashlightUiState())
    val uiState: StateFlow<SmartFlashlightUiState> = _uiState.asStateFlow()

    private var patternJob: Job? = null

    fun toggleTorch() {
        val next = !_uiState.value.isTorchOn
        setTorch(next, if (next) "Torch enabled." else "Torch disabled.")
    }

    fun playSosPattern() {
        playPattern(
            sequence = listOf(200L, 200L, 200L, 200L, 200L, 500L, 500L, 200L, 500L, 200L, 500L, 500L, 200L, 200L, 200L, 200L, 200L),
            label = "SOS pattern running locally.",
        )
    }

    fun playMorsePattern(message: String) {
        val clean = message.uppercase().filter { it in 'A'..'Z' || it == ' ' }
        if (clean.isBlank()) return
        val pattern = buildPattern(clean)
        playPattern(pattern, "Morse pattern: $clean")
    }

    fun stopPattern() {
        patternJob?.cancel()
        patternJob = null
        setTorch(false, "Pattern stopped.")
        _uiState.update { it.copy(isPatternRunning = false) }
    }

    private fun playPattern(sequence: List<Long>, label: String) {
        val cameraId = torchCameraId ?: run {
            _uiState.update { it.copy(errorMessage = "No flash unit is available on this device.") }
            return
        }
        patternJob?.cancel()
        patternJob = viewModelScope.launch {
            _uiState.update { it.copy(isPatternRunning = true, statusMessage = label, errorMessage = null) }
            try {
                sequence.forEachIndexed { index, duration ->
                    val torchOn = index % 2 == 0
                    cameraManager.setTorchMode(cameraId, torchOn)
                    _uiState.update { it.copy(isTorchOn = torchOn) }
                    delay(duration)
                }
            } catch (throwable: Throwable) {
                _uiState.update { it.copy(errorMessage = throwable.message ?: "Unable to control torch.") }
            } finally {
                runCatching { cameraManager.setTorchMode(cameraId, false) }
                _uiState.update { it.copy(isTorchOn = false, isPatternRunning = false) }
                patternJob = null
            }
        }
    }

    private fun setTorch(enabled: Boolean, message: String) {
        val cameraId = torchCameraId ?: run {
            _uiState.update { it.copy(errorMessage = "No flash unit is available on this device.") }
            return
        }
        runCatching {
            cameraManager.setTorchMode(cameraId, enabled)
        }.onSuccess {
            _uiState.update {
                it.copy(
                    isTorchOn = enabled,
                    statusMessage = message,
                    errorMessage = null,
                )
            }
        }.onFailure { throwable ->
            _uiState.update { it.copy(errorMessage = throwable.message ?: "Unable to control torch.") }
        }
    }

    private fun buildPattern(message: String): List<Long> {
        val map = mapOf(
            'A' to ".-",
            'B' to "-...",
            'C' to "-.-.",
            'D' to "-..",
            'E' to ".",
            'F' to "..-.",
            'G' to "--.",
            'H' to "....",
            'I' to "..",
            'J' to ".---",
            'K' to "-.-",
            'L' to ".-..",
            'M' to "--",
            'N' to "-.",
            'O' to "---",
            'P' to ".--.",
            'Q' to "--.-",
            'R' to ".-.",
            'S' to "...",
            'T' to "-",
            'U' to "..-",
            'V' to "...-",
            'W' to ".--",
            'X' to "-..-",
            'Y' to "-.--",
            'Z' to "--..",
        )
        val dot = 160L
        val dash = 420L
        val gap = 120L
        val letterGap = 320L
        val wordGap = 700L
        val pattern = mutableListOf<Long>()

        message.forEachIndexed { index, char ->
            if (char == ' ') {
                pattern += wordGap
                return@forEachIndexed
            }
            val symbols = map[char].orEmpty()
            symbols.forEachIndexed { symbolIndex, symbol ->
                pattern += if (symbol == '.') dot else dash
                pattern += if (symbolIndex == symbols.lastIndex) letterGap else gap
            }
            if (index == message.lastIndex && pattern.isNotEmpty()) {
                pattern.removeLast()
            }
        }
        return pattern
    }

    override fun onCleared() {
        stopPattern()
        super.onCleared()
    }
}
