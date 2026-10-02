package com.purehub.app.feature.wifi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    @Test
    fun estimatesSirOnlyWhenAnOverlappingInterfererExists() {
        val target = network(2412, -45)
        val sameChannel = network(2412, -65).copy(bssid = "interferer")
        val distantBand = network(5180, -30).copy(bssid = "five-gigahertz")

        val sir = WifiInsights.estimatedSirDb(target, listOf(target, sameChannel, distantBand))

        assertTrue(sir != null && sir in 19.9..20.1)
        assertNull(WifiInsights.estimatedSirDb(target, listOf(target, distantBand)))
    }

    @Test
    fun includesQuietUnobservedChannelsInRecommendations() {
        val ratings = WifiInsights.rateChannels(listOf(network(5180, -40)), "5 GHz")

        assertTrue(ratings.any { it.channel == 48 && it.qualityPercent == 100 })
        assertTrue(ratings.first { it.channel == 36 }.qualityPercent < 100)
        assertEquals(listOf(5, 21, 37), WifiInsights.candidateChannels("6 GHz", "VN").take(3))
    }

    @Test
    fun parsesReadOnlyControllerTelemetryWithoutInventingMissingValues() {
        val devices = """{"data":[{"type":"uap","channelUtilization":42},{"type":"switch"},{"deviceType":"access point","channel_utilization":58}]}"""
        val clients = """{"result":{"totalRows":17,"data":[{"name":"phone"}]}}"""

        val telemetry = parseControllerTelemetry(devices, clients)

        assertEquals(2, telemetry.accessPointCount)
        assertEquals(17, telemetry.clientCount)
        assertEquals(50, telemetry.channelUtilizationPercent)
        assertNull(telemetry.wanOnline)
    }

    @Test
    fun normalizesFractionalControllerChannelUtilization() {
        val telemetry = parseControllerTelemetry("""[{"type":"uap","utilization":0.42}]""", "")

        assertEquals(42, telemetry.channelUtilizationPercent)
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
