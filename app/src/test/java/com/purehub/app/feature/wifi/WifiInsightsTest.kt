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
        assertTrue(WifiInsights.recommendation(networks).contains("channel 11"))
    }

    @Test
    fun ratesOverlapUsingSignalAndChannelWidth() {
        val networks = listOf(
            network(2412, -35, width = 40),
            network(2417, -50, width = 20),
            network(2437, -80, width = 20),
        )

        val ratings = WifiInsights.rateChannels(networks, "2.4 GHz")
        assertEquals(listOf(1, 6, 11), ratings.map { it.channel })
        assertEquals(11, ratings.maxBy { it.qualityPercent }.channel)
    }

    private fun network(frequency: Int, rssi: Int, width: Int = 20) = NearbyWifiNetwork(
        ssid = "Test",
        bssid = frequency.toString(),
        rssi = rssi,
        level = 3,
        frequencyMhz = frequency,
        channelLabel = WifiInsights.channelLabel(frequency),
        securityLabel = "WPA2",
        isCurrentConnection = false,
        channelWidthMhz = width,
    )
}
