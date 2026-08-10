package com.purehub.app.feature.qr

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QrDecoderTest {
    @Test
    fun decodesQrGeneratedByPureHub() {
        val value = "https://hub.blissbiovn.com/en/qr-studio"
        val bitmap = requireNotNull(QrBitmapGenerator.generate(value, size = 640))

        assertEquals(value, QrDecoder.decode(bitmap))
    }
}
