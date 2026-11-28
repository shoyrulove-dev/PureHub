package com.purehub.app.feature

import android.content.ContextWrapper
import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.purehub.app.feature.wifi.WifiSurveyExporter
import com.purehub.app.feature.wifi.WifiSurveyPoint
import com.purehub.app.feature.wifi.WifiSurveyProject
import com.purehub.app.ui.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WifiReportInstrumentedTest {
    @Suppress("DEPRECATION")
    @Test
    fun sampleSurveyExportsReadablePdfThroughShareUri() {
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        lateinit var share: Intent
        val context = object : ContextWrapper(target) {
            override fun startActivity(intent: Intent) {
                share = requireNotNull(intent.getParcelableExtra(Intent.EXTRA_INTENT))
            }
        }
        val project = WifiSurveyProject(
            id = "acceptance",
            name = "Sample survey",
            floor = "Floor 1",
            points = listOf(WifiSurveyPoint(.5f, .5f, -55, "Sample Wi-Fi")),
        )

        WifiSurveyExporter.exportAndShare(context, project, WifiSurveyExporter.Format.PDF, AppLanguage.English)

        assertEquals("application/pdf", share.type)
        val uri = requireNotNull(share.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
        val descriptor = requireNotNull(target.contentResolver.openFileDescriptor(uri, "r"))
        PdfRenderer(descriptor).use { renderer ->
            assertEquals(1, renderer.pageCount)
        }
    }
}
