package com.purehub.app.feature.colorgrabber

import kotlin.math.roundToInt

data class GrabbedColor(
    val red: Int,
    val green: Int,
    val blue: Int,
) {
    val hex: String
        get() = "#%02X%02X%02X".format(red, green, blue)
}

object ColorGrabberUtils {
    fun sampleCenterRgba(
        rgbaBytes: ByteArray,
        width: Int,
        height: Int,
    ): GrabbedColor {
        if (width <= 0 || height <= 0 || rgbaBytes.size < width * height * 4) {
            return GrabbedColor(0, 0, 0)
        }

        val centerX = width / 2
        val centerY = height / 2
        val index = (centerY * width + centerX) * 4

        return GrabbedColor(
            red = rgbaBytes[index].toInt() and 0xFF,
            green = rgbaBytes[index + 1].toInt() and 0xFF,
            blue = rgbaBytes[index + 2].toInt() and 0xFF,
        )
    }

    fun describeBrightness(color: GrabbedColor): String {
        val luminance = (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue) / 255.0
        return when {
            luminance >= 0.75 -> "Bright"
            luminance >= 0.4 -> "Balanced"
            else -> "Deep"
        }
    }

    fun toPercent(value: Int): String {
        return "${((value / 255f) * 100f).roundToInt()}%"
    }
}
