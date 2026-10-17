package com.purehub.app.feature.flashlight

import android.app.Application
import android.os.Build
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SmartFlashlightUiState(
    val isTorchOn: Boolean = false,
    val isPatternRunning: Boolean = false,
    val torchStrength: Int = 1,
    val maxTorchStrength: Int = 1,
    val strobeHz: Int = 8,
    val autoOffMinutes: Int = 0,
    val statusMessage: String = "Torch ready for local control.",
    val errorMessage: String? = null,
)

class SmartFlashlightViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val cameraManager = application.getSystemService(CameraManager::class.java)
    private val torchCameraId = runCatching { cameraManager.cameraIdList.firstOrNull { id ->
        val characteristics = cameraManager.getCameraCharacteristics(id)
        characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
            characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
    } }.getOrNull()
    private val maxTorchStrength = runCatching { torchCameraId?.let { id ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
        } else 1
    } ?: 1 }.getOrDefault(1).coerceAtLeast(1)

    private val _uiState = MutableStateFlow(SmartFlashlightUiState(maxTorchStrength = maxTorchStrength))
    val uiState: StateFlow<SmartFlashlightUiState> = _uiState.asStateFlow()

    private var patternJob: Job? = null
    private var autoOffJob: Job? = null
    private var patternGeneration = 0L

    fun toggleTorch() {
        if (_uiState.value.isPatternRunning) { stopPattern(); return }
        val next = !_uiState.value.isTorchOn
        setTorch(next, if (next) "Torch enabled." else "Torch disabled.", _uiState.value.torchStrength)
    }

    fun setTorchStrength(level: Int) {
        val safeLevel = level.coerceIn(1, maxTorchStrength)
        _uiState.update { it.copy(torchStrength = safeLevel) }
        if (_uiState.value.isTorchOn) setTorch(true, "Torch brightness set to $safeLevel of $maxTorchStrength.", safeLevel)
    }

    fun setStrobeHz(hz: Int) = _uiState.update { it.copy(strobeHz = hz.coerceIn(1, 30)) }

    fun setAutoOffMinutes(minutes: Int) {
        val safeMinutes = minutes.coerceIn(0, 30)
        _uiState.update { it.copy(autoOffMinutes = safeMinutes) }
        scheduleAutoOff()
    }

    fun playSosPattern() {
        playPattern(
            sequence = buildMorsePattern("SOS"),
            label = "SOS pattern running locally.",
        )
    }

    fun playMorsePattern(message: String) {
        val clean = message.uppercase().filter { it in 'A'..'Z' || it == ' ' }
        if (clean.isBlank()) return
        val pattern = buildMorsePattern(clean)
        playPattern(pattern, "Morse pattern: $clean")
    }

    fun stopPattern() {
        patternGeneration++
        patternJob?.cancel()
        patternJob = null
        autoOffJob?.cancel()
        setTorch(false, "Pattern stopped.")
        _uiState.update { it.copy(isPatternRunning = false) }
    }

    private fun playPattern(sequence: List<Long>, label: String) {
        val cameraId = torchCameraId ?: run {
            _uiState.update { it.copy(errorMessage = "No flash unit is available on this device.") }
            return
        }
        val generation = ++patternGeneration
        patternJob?.cancel()
        autoOffJob?.cancel()
        patternJob = viewModelScope.launch {
            _uiState.update { it.copy(isPatternRunning = true, statusMessage = label, errorMessage = null) }
            scheduleAutoOff()
            try {
                sequence.forEachIndexed { index, duration ->
                    val torchOn = index % 2 == 0
                    setTorchMode(cameraId, torchOn, _uiState.value.torchStrength)
                    _uiState.update { it.copy(isTorchOn = torchOn) }
                    delay(duration)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (throwable: Throwable) {
                _uiState.update { it.copy(errorMessage = throwable.message ?: "Unable to control torch.") }
            } finally {
                if (generation == patternGeneration) {
                    runCatching { cameraManager.setTorchMode(cameraId, false) }
                    _uiState.update { it.copy(isTorchOn = false, isPatternRunning = false) }
                    autoOffJob?.cancel()
                    patternJob = null
                }
            }
        }
    }

    fun playStrobe() {
        val halfCycle = (500L / _uiState.value.strobeHz.coerceAtLeast(1)).coerceAtLeast(16L)
        playPattern(List(240) { halfCycle }, "${_uiState.value.strobeHz} Hz strobe running locally.")
    }

    private fun setTorch(enabled: Boolean, message: String, strength: Int = _uiState.value.torchStrength) {
        val cameraId = torchCameraId ?: run {
            _uiState.update { it.copy(errorMessage = "No flash unit is available on this device.") }
            return
        }
        runCatching {
            setTorchMode(cameraId, enabled, strength)
        }.onSuccess {
            _uiState.update {
                it.copy(
                    isTorchOn = enabled,
                    statusMessage = message,
                    errorMessage = null,
                )
            }
            if (enabled) scheduleAutoOff() else autoOffJob?.cancel()
        }.onFailure { throwable ->
            _uiState.update { it.copy(errorMessage = throwable.message ?: "Unable to control torch.") }
        }
    }

    private fun setTorchMode(cameraId: String, enabled: Boolean, strength: Int) {
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && maxTorchStrength > 1) {
            cameraManager.turnOnTorchWithStrengthLevel(cameraId, strength.coerceIn(1, maxTorchStrength))
        } else {
            cameraManager.setTorchMode(cameraId, enabled)
        }
    }

    private fun scheduleAutoOff() {
        autoOffJob?.cancel()
        val minutes = _uiState.value.autoOffMinutes
        if ((!_uiState.value.isTorchOn && !_uiState.value.isPatternRunning) || minutes == 0) return
        autoOffJob = viewModelScope.launch {
            delay(minutes * 60_000L)
            stopPattern()
        }
    }

    override fun onCleared() {
        stopPattern()
        autoOffJob?.cancel()
        super.onCleared()
    }
}

internal fun buildMorsePattern(message: String): List<Long> {
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
        val dash = dot * 3
        val gap = dot
        val letterGap = dot * 3
        val wordGap = dot * 7
        val pattern = mutableListOf<Long>()

        val clean = message.uppercase(java.util.Locale.ROOT).filter { it in 'A'..'Z' || it == ' ' }.trim()
        clean.forEach { char ->
            if (char == ' ') {
                if (pattern.isNotEmpty()) pattern[pattern.lastIndex] = wordGap
                return@forEach
            }
            val symbols = map[char].orEmpty()
            symbols.forEachIndexed { symbolIndex, symbol ->
                pattern += if (symbol == '.') dot else dash
                pattern += if (symbolIndex == symbols.lastIndex) letterGap else gap
            }
        }
        if (pattern.isNotEmpty()) pattern.removeAt(pattern.lastIndex)
        return pattern
    }
