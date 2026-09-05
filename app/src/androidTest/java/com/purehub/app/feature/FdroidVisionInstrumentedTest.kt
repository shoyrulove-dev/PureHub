package com.purehub.app.feature

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.purehub.app.feature.ocr.OcrEngineFactory
import com.purehub.app.feature.ocr.OcrScript
import com.purehub.app.feature.qr.QrBitmapGenerator
import com.purehub.app.feature.qr.QrDecoder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FdroidVisionInstrumentedTest {
    @Test
    fun qrRoundTripWorksOnDevice() {
        val value = "https://hub.blissbiovn.com/en/qr-studio"
        val bitmap = requireNotNull(QrBitmapGenerator.generate(value, 720))

        assertEquals(value, QrDecoder.decode(bitmap))
    }

    @Test
    fun bundledTesseractRecognizesPrintedVietnameseSampleOffline() {
        assertPrintedSampleRecognized(OcrScript.LATIN, "PUREHUB TIẾNG VIỆT 67890", "67890")
    }

    @Test
    fun bundledTesseractLoadsChinesePackAndRecognizesPrintedSampleOffline() {
        assertPrintedSampleRecognized(OcrScript.CHINESE, "PUREHUB 中文 OCR 24680", "24680")
    }

    private fun assertPrintedSampleRecognized(script: OcrScript, sample: String, expected: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bitmap = Bitmap.createBitmap(1400, 420, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(Color.WHITE)
            drawText(
                sample,
                50f,
                240f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 110f },
            )
        }
        val latch = CountDownLatch(1)
        var recognized = ""
        var failure: Throwable? = null
        val engine = OcrEngineFactory.create(context, script)
        engine.recognize(bitmap) { result ->
            result.onSuccess { recognized = it }.onFailure { failure = it }
            latch.countDown()
        }

        assertTrue("OCR timed out", latch.await(40, TimeUnit.SECONDS))
        engine.close()
        failure?.let { throw AssertionError("OCR failed", it) }
        assertTrue("Unexpected OCR output: $recognized", recognized.contains(expected))
    }
}
