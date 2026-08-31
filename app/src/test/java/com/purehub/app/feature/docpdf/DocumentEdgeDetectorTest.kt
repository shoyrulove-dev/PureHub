package com.purehub.app.feature.docpdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentEdgeDetectorTest {
    @Test
    fun detectsLightDocumentAgainstDarkBorder() {
        val width = 200
        val height = 300
        val dark = 0xFF202020.toInt()
        val light = 0xFFF4F4F4.toInt()
        val pixels = IntArray(width * height) { dark }
        for (y in 35 until 270) for (x in 24 until 178) pixels[y * width + x] = light

        val result = DocumentEdgeDetector.detect(pixels, width, height)

        assertTrue(result.confidence >= 0.42f)
        assertEquals(0.11f, result.crop.left, 0.04f)
        assertEquals(0.10f, result.crop.top, 0.04f)
        assertEquals(0.10f, result.crop.right, 0.04f)
        assertEquals(0.09f, result.crop.bottom, 0.04f)
    }

    @Test
    fun returnsNoCropForUniformImage() {
        val result = DocumentEdgeDetector.detect(IntArray(120 * 160) { 0xFFFFFFFF.toInt() }, 120, 160)

        assertEquals(0f, result.confidence)
        assertEquals(CropAdjustments(), result.crop)
    }
}
