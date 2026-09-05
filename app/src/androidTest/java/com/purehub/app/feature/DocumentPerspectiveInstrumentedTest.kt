package com.purehub.app.feature

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.purehub.app.feature.docpdf.DocumentCorners
import com.purehub.app.feature.docpdf.DocumentPerspectiveCorrector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DocumentPerspectiveInstrumentedTest {
    @Test
    fun fourPointTransformCreatesReviewedPageOnAndroid() {
        val source = Bitmap.createBitmap(200, 300, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }

        val corrected = DocumentPerspectiveCorrector.correct(source, DocumentCorners.fullFrame(0.1f))

        assertNotSame(source, corrected)
        assertEquals(160, corrected.width)
        assertEquals(240, corrected.height)
        source.recycle()
        corrected.recycle()
    }
}
