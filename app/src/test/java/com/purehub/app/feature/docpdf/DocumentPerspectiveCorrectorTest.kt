package com.purehub.app.feature.docpdf

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DocumentPerspectiveCorrectorTest {
    @Test
    fun correctsReviewedCornersIntoAFlatPage() {
        val output = DocumentPerspectiveCorrector.outputSize(200, 300, DocumentCorners.fullFrame(0.1f))

        assertEquals(160, output.first)
        assertEquals(240, output.second)
    }

    @Test
    fun sanitizesCornersSoHandlesCannotCross() {
        val unsafe = DocumentCorners(
            NormalizedPoint(0.9f, 0.9f),
            NormalizedPoint(0.1f, 0.9f),
            NormalizedPoint(0.1f, 0.1f),
            NormalizedPoint(0.9f, 0.1f),
        ).sanitized()

        assertEquals(0.48f, unsafe.topLeft.x)
        assertEquals(0.52f, unsafe.topRight.x)
        assertEquals(0.52f, unsafe.bottomRight.y)
        assertEquals(0.52f, unsafe.bottomLeft.y)
    }
}
