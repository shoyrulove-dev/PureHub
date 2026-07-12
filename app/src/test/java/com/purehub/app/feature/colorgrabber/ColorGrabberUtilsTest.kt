package com.purehub.app.feature.colorgrabber

import org.junit.Assert.assertEquals
import org.junit.Test

class ColorGrabberUtilsTest {
    @Test
    fun samplesCenterPixelFromRgbaBuffer() {
        val width = 3
        val height = 3
        val rgba = ByteArray(width * height * 4)
        val centerIndex = ((height / 2) * width + (width / 2)) * 4
        rgba[centerIndex] = 0x11
        rgba[centerIndex + 1] = 0x22
        rgba[centerIndex + 2] = 0x33
        rgba[centerIndex + 3] = 0x7F

        val sampled = ColorGrabberUtils.sampleCenterRgba(rgba, width, height)

        assertEquals("#112233", sampled.hex)
    }
}
