package com.purehub.app.feature.ocr

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OcrBitmapMemoryInstrumentedTest {
    @Test
    fun twentyRetainedPagesStayWithinThePreviewMemoryBudget() {
        val retained = (1..20).map {
            val recognitionInput = Bitmap.createBitmap(1273, 1800, Bitmap.Config.ARGB_8888)
            OcrBitmapMemory.compactForRetention(recognitionInput).also { compact ->
                recognitionInput.recycle()
                assertEquals(Bitmap.Config.RGB_565, compact.config)
            }
        }

        val allocatedBytes = retained.sumOf { it.allocationByteCount.toLong() }
        assertTrue("20 retained OCR pages use $allocatedBytes bytes", allocatedBytes <= 96L * 1024 * 1024)
        retained.forEach(Bitmap::recycle)
    }
}
