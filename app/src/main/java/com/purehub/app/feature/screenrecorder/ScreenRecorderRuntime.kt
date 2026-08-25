package com.purehub.app.feature.screenrecorder

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ScreenRecordingPhase {
    IDLE,
    PREPARING,
    RECORDING,
    PAUSED,
}

data class ScreenRecordingStatus(
    val phase: ScreenRecordingPhase = ScreenRecordingPhase.IDLE,
    val message: String = "Recordings are saved locally to Movies/PureHub.",
)

object ScreenRecorderRuntime {
    private val mutableStatus = MutableStateFlow(ScreenRecordingStatus())
    val status = mutableStatus.asStateFlow()

    fun update(phase: ScreenRecordingPhase, message: String) {
        mutableStatus.value = ScreenRecordingStatus(phase, message)
    }
}

data class CaptureSize(val width: Int, val height: Int)

fun calculateCaptureSize(displayWidth: Int, displayHeight: Int, widthCap: Int): CaptureSize {
    require(displayWidth > 0 && displayHeight > 0 && widthCap > 0)
    val scale = minOf(1f, widthCap.toFloat() / displayWidth.toFloat())
    fun even(value: Int) = value.coerceAtLeast(2).let { if (it % 2 == 0) it else it - 1 }
    return CaptureSize(
        width = even((displayWidth * scale).toInt()),
        height = even((displayHeight * scale).toInt()),
    )
}
