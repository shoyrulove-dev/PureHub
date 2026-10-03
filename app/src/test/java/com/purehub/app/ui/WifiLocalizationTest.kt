package com.purehub.app.ui

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiLocalizationTest {
    private val catalogSummaries = listOf(
        "Vietnamese lunar dates, can-chi and traditional markers.",
        "Build simple habits without accounts or pressure.",
        "A calm focus timer with local soundscapes.",
        "Guided breathing with gentle motion.",
        "Direction, bearing and sensor guidance.",
        "Quick two-axis leveling and calibration.",
        "Private estimated sound-level monitoring.",
        "Torch, screen light and safety patterns.",
        "Fast offline unit conversion.",
        "Scan QR codes and barcodes, create codes, and keep a private local history.",
        "Capture, arrange and export private PDFs.",
        "Scan, clean, edit and export private documents offline.",
        "Sample and copy colors from the camera.",
        "Create share-ready photos without GPS or EXIF metadata.",
        "Review reclaimable files before deleting.",
        "Play a controlled tone for residual water.",
        "Inspect nearby signals and Wi-Fi channels.",
        "Encrypted local credentials with device protection.",
        "Offline 2FA codes protected by your device lock.",
        "Hash, archive and share local files privately.",
        "Local wallpaper preview and rotation.",
        "Split items, tax and tips for a group.",
        "Private offline expense ledger.",
        "A fair local picker for quick choices.",
        "Telegram, GitHub and the PureHub roadmap.",
        "Record a local MP4 with Android's consent flow.",
    )
    private val criticalWifiCopy = listOf(
        "Nearby Wi-Fi scan is ready on this device.",
        "Without permission, PureHub only shows your current connection.",
        "Notification permission is required for reliable background monitoring.",
        "Nearby Wi-Fi scan is paused. Existing local results remain on this device.",
        "Android deferred the latest scan request. Showing cached results until fresh data arrives.",
        "Live signal history is updating. Nearby Wi-Fi scan refreshes locally when Android allows it.",
        "Run a network test before sampling to attach metrics",
        "30s sample",
        "Router · 92% confidence",
        "3.5% loss",
        "84.2 Mbps down",
        "Signal 72/100",
        "Network health recommendation",
        "Current roaming choice looks healthy.",
        "A stronger access point is available. This device may be sticking to a weaker node.",
        "Filter SSID or BSSID",
        "Secured",
        "Hidden network",
        "Private / randomized MAC",
        "2.4 GHz · Ch 6 · 20 MHz · WPA2",
        "5 GHz · Ch 44 · 866 Mbps",
        "Runs only when you tap. Measures gateway, Internet reachability, DNS, loaded latency and adaptive throughput.",
        "The connected build contacts Cloudflare speed endpoints and public DNS resolvers only during this test. No identifier is added by PureHub.",
        "Auto · fastest configured endpoint",
        "Test mode",
        "Download · Upload · Loaded latency",
        "History",
        "14 runs · stored only on this device",
        "Local history · 3/14",
        "Manage separate projects and floors. Measurements, scale and reports stay on this device.",
        "Home survey",
        "Home survey · Floor 1",
        "Floor 1",
        "Project",
        "Floor",
        "+ Project",
        "Calibrate scale",
        "Cancel calibration",
        "Delete project",
    )

    @Test
    fun `critical wifi copy is translated in every supported non-English language`() {
        listOf(AppLanguage.Vietnamese, AppLanguage.Chinese, AppLanguage.Spanish).forEach { language ->
            criticalWifiCopy.forEach { source ->
                assertNotEquals("Missing ${language.code} translation for: $source", source, translateUiText(source, language))
            }
        }
    }

    @Test
    fun `translated wifi copy uses the expected writing system`() {
        assertTrue(translateUiText("Network health", AppLanguage.Vietnamese).contains("mạng", ignoreCase = true))
        assertTrue(translateUiText("Network health", AppLanguage.Chinese).any { it.code in 0x4E00..0x9FFF })
        assertTrue(translateUiText("Network health", AppLanguage.Spanish).contains("red", ignoreCase = true))
    }

    @Test
    fun `spanish catalog never falls back to english summaries`() {
        catalogSummaries.forEach { source ->
            assertNotEquals("Missing Spanish catalog translation for: $source", source, translateUiText(source, AppLanguage.Spanish))
        }
    }
}
