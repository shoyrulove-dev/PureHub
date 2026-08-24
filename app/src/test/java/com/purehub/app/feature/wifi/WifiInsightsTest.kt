package com.purehub.app.feature.wifi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiInsightsTest {
    @Test
    fun mapsCommonWifiChannelsAcrossBands() {
        assertEquals(1, WifiInsights.channelForFrequency(2412))
        assertEquals(14, WifiInsights.channelForFrequency(2484))
        assertEquals(36, WifiInsights.channelForFrequency(5180))
        assertEquals(5, WifiInsights.channelForFrequency(5975))
    }

    @Test
    fun reportsSecurityAndQuietestObservedChannel() {
        assertEquals("WPA3", WifiInsights.securityLabel("[WPA3-SAE-CCMP][ESS]"))
        assertEquals("Open", WifiInsights.securityLabel("[ESS]"))
        val networks = listOf(network(2412, -45), network(2412, -70), network(2437, -65))
        assertTrue(WifiInsights.recommendation(networks).contains("channel 6"))
    }

    private fun network(frequency: Int, rssi: Int) = NearbyWifiNetwork(
        ssid = "Test",
        bssid = frequency.toString(),
        rssi = rssi,
        level = 3,
        frequencyMhz = frequency,
        channelLabel = WifiInsights.channelLabel(frequency),
        securityLabel = "WPA2",
        isCurrentConnection = false,
    )
}
