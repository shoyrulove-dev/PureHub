package com.purehub.app.feature.screenrecorder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ScreenRecorderRuntimeTest {
    @Test
    fun `capture size preserves portrait ratio under width cap`() {
        assertEquals(CaptureSize(720, 1560), calculateCaptureSize(1080, 2340, 720))
    }

    @Test
    fun `capture size never upscales and remains encoder safe`() {
        assertEquals(CaptureSize(1080, 2340), calculateCaptureSize(1080, 2340, 2160))
        assertEquals(CaptureSize(1080, 2336), calculateCaptureSize(1081, 2340, 1080))
    }

    @Test
    fun `capture size rejects invalid dimensions`() {
        assertThrows(IllegalArgumentException::class.java) { calculateCaptureSize(0, 2340, 720) }
    }
}
