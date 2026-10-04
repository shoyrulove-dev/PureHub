package com.purehub.app.ui

import org.junit.Assert.assertNotEquals
import org.junit.Test

class CoreExperienceLocalizationTest {
    private val criticalCopy = listOf(
        "Home",
        "Settings",
        "Help",
        "Back",
        "Ready. Capture a page or choose an image.",
        "Saved OCR documents will appear here.",
        "No matching document.",
        "Complete document saved privately on this device.",
        "Scanned document preview",
        "Scan settings",
        "Doc to PDF",
        "PDF ready locally.",
        "Selected captured page",
        "Color Grabber",
        "Color HEX copied locally.",
        "Photo Privacy",
        "Screen Recorder",
        "WiFi Analyzer",
        "Network health recommendation",
        "Network tests",
        "Coverage survey",
        "Channel graph",
        "LAN devices",
        "Private local Wi-Fi analysis",
        "Run diagnostics",
        "Speed history",
        "Survey projects",
        "Bubble Level",
        "Accuracy",
        "Ruler",
        "Sound",
        "Calibrate zero",
        "Move the bubble into the center target.",
        "A calm two-axis level and quick ruler powered by private on-device readings.",
        "Enable level sensor",
        "Pause level sensor",
        "Surface",
        "Edge X",
        "Edge Y",
        "Inside tolerance · keep the phone still.",
        "Measurements remain local",
        "Motion sensors stay on this device. Camera mode only opens after permission and frames are never saved.",
    )

    @Test
    fun `home settings vision wifi and bubble have all supported translations`() {
        listOf(AppLanguage.Vietnamese, AppLanguage.Chinese, AppLanguage.Spanish).forEach { language ->
            criticalCopy.forEach { source ->
                assertNotEquals(
                    "Missing ${language.code} translation for: $source",
                    source,
                    translateUiText(source, language),
                )
            }
        }
    }
}
