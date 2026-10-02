package com.purehub.app.feature.wifi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiPremiumInsightsTest {
    @Test
    fun `mesh groups flag a sticky client when another node is much stronger`() {
        val current = network("Home", "AA:00:00:00:00:01", -78, true, "5 GHz · Ch 36")
        val stronger = network("Home", "AA:00:00:00:00:02", -55, false, "5 GHz · Ch 44")

        val mesh = WifiPremiumInsights.meshGroups(listOf(current, stronger)).single()

        assertTrue(mesh.stickyClient)
        assertEquals(stronger.bssid, mesh.strongestBssid)
        assertEquals(2, mesh.accessPoints.size)
    }

    @Test
    fun `survey exposes attached diagnostic metrics`() {
        val point = WifiSurveyPoint(.5f, .5f, -62, "Home", latencyMs = 24, packetLossPercent = 1, downloadMbps = 83.5, uploadMbps = 21.0)

        assertEquals(83.5, WifiPremiumInsights.surveyValue(point, WifiSurveyMetric.DOWNLOAD)!!, .001)
        assertEquals(24.0, WifiPremiumInsights.surveyValue(point, WifiSurveyMetric.LATENCY)!!, .001)
    }

    @Test
    fun `private speed endpoint normalizes trailing slashes`() {
        val endpoint = WifiSpeedEndpoint("lab", "Lab", "https://speed.example.test///")

        assertEquals("https://speed.example.test", endpoint.normalizedBaseUrl())
    }

    @Test
    fun `device identity combines services and observed ports`() {
        val device = LanDevice("192.168.1.20", "office-printer.local", 4, false, services = listOf("_ipp._tcp.local"))

        val identified = LanDiscovery.enrichIdentity(device, listOf(OpenLanPort(631, "IPP")))

        assertEquals("Printer", identified.identityLabel)
        assertTrue(identified.identityConfidence >= 70)
    }

    private fun network(ssid: String, bssid: String, rssi: Int, current: Boolean, channel: String) = NearbyWifiNetwork(
        ssid, bssid, rssi, 3, 5_180, channel, "WPA2", current,
    )
}
