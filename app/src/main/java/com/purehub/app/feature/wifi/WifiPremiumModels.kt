package com.purehub.app.feature.wifi

enum class WifiSpeedProfile(
    val label: String,
    val targetSeconds: Int,
    val parallelStreams: Int,
    val maxBytes: Int,
) {
    DATA_SAVER("Data saver", 4, 1, 8 * 1_048_576),
    BALANCED("Balanced", 8, 2, 32 * 1_048_576),
    ACCURATE("Accurate", 12, 4, 96 * 1_048_576),
}

data class WifiSpeedEndpoint(
    val id: String,
    val label: String,
    val baseUrl: String,
    val builtIn: Boolean = false,
) {
    fun normalizedBaseUrl(): String = baseUrl.trim().trimEnd('/')
}

enum class WifiSpeedServerMode(val label: String) {
    AUTO("Auto select"),
    CLOUDFLARE("Cloudflare"),
    CUSTOM("Private endpoint"),
}

data class WifiSpeedProgress(
    val phase: String = "Idle",
    val percent: Int = 0,
    val liveMbps: Double? = null,
    val transferredBytes: Long = 0,
)

data class WifiMeshGroup(
    val ssid: String,
    val accessPoints: List<NearbyWifiNetwork>,
    val currentBssid: String?,
    val strongestBssid: String?,
    val currentRssi: Int?,
    val strongestRssi: Int?,
    val stickyClient: Boolean,
    val overlappingChannels: Int,
    val score: Int,
)

data class WifiMonitorSample(
    val timestamp: Long,
    val rssi: Int,
    val bssid: String,
    val gatewayReachable: Boolean,
    val internetReachable: Boolean,
    val latencyMs: Int?,
)

data class WifiMonitorEvent(
    val timestamp: Long,
    val type: String,
    val detail: String,
)

data class WifiMonitorReport(
    val startedAt: Long,
    val endedAt: Long,
    val samples: List<WifiMonitorSample>,
    val events: List<WifiMonitorEvent>,
) {
    val uptimePercent: Int
        get() = if (samples.isEmpty()) 0 else samples.count(WifiMonitorSample::internetReachable) * 100 / samples.size
    val averageLatencyMs: Int?
        get() = samples.mapNotNull(WifiMonitorSample::latencyMs).takeIf(List<Int>::isNotEmpty)?.average()?.toInt()
    val weakestRssi: Int?
        get() = samples.minOfOrNull(WifiMonitorSample::rssi)
}

data class WifiMonitorSession(
    val startedAt: Long,
    val endsAt: Long,
    val intervalSeconds: Int,
    val samples: List<WifiMonitorSample>,
    val events: List<WifiMonitorEvent>,
)

enum class WifiSurveyCaptureMode(val label: String) {
    SIGNAL_ONLY("Signal only"),
    QUICK_HEALTH("Quick health"),
    FULL_TEST("Full test"),
}

enum class RouterControllerPlatform(val label: String) {
    UNIFI("UniFi Network"),
    OMADA("TP-Link Omada"),
    OPENWRT("OpenWrt / LuCI"),
}

data class RouterControllerConfig(
    val platform: RouterControllerPlatform,
    val baseUrl: String,
    val username: String = "",
    val secret: String = "",
)

data class RouterControllerStatus(
    val platform: RouterControllerPlatform,
    val reachable: Boolean,
    val authenticated: Boolean,
    val responseMs: Int?,
    val summary: String,
    val accessPointCount: Int? = null,
    val clientCount: Int? = null,
    val channelUtilizationPercent: Int? = null,
    val wanOnline: Boolean? = null,
)

data class RouterInsight(
    val gateway: String,
    val platform: String,
    val managementUrl: String,
    val responseMs: Int,
    val evidence: String,
    val readOnly: Boolean = true,
)

enum class WifiSurveyMetric(val label: String) {
    SIGNAL("Signal"),
    SIR("Estimated SIR"),
    DOWNLOAD("Download"),
    UPLOAD("Upload"),
    LATENCY("Latency"),
    PACKET_LOSS("Packet loss"),
}

object WifiPremiumInsights {
    fun meshGroups(networks: List<NearbyWifiNetwork>): List<WifiMeshGroup> = networks
        .filter { it.isActive && it.ssid.isNotBlank() && it.ssid != "Hidden network" }
        .groupBy(NearbyWifiNetwork::ssid)
        .map { (ssid, accessPoints) ->
            val current = accessPoints.firstOrNull(NearbyWifiNetwork::isCurrentConnection)
            val strongest = accessPoints.maxByOrNull(NearbyWifiNetwork::rssi)
            val sticky = current != null && strongest != null && strongest.bssid != current.bssid && strongest.rssi - current.rssi >= 12
            val overlap = accessPoints.groupBy { it.channelLabel }.values.sumOf { (it.size - 1).coerceAtLeast(0) }
            val base = ((strongest?.rssi ?: -100) + 100) * 2
            val score = (base - overlap * 8 - if (sticky) 20 else 0).coerceIn(0, 100)
            WifiMeshGroup(ssid, accessPoints, current?.bssid, strongest?.bssid, current?.rssi, strongest?.rssi, sticky, overlap, score)
        }
        .sortedWith(compareByDescending<WifiMeshGroup> { it.currentBssid != null }.thenByDescending(WifiMeshGroup::score))

    fun surveyValue(point: WifiSurveyPoint, metric: WifiSurveyMetric): Double? = when (metric) {
        WifiSurveyMetric.SIGNAL -> point.rssi.toDouble()
        WifiSurveyMetric.SIR -> point.readings.mapNotNull(WifiSurveyReading::estimatedSirDb).takeIf(List<Double>::isNotEmpty)?.average()
        WifiSurveyMetric.DOWNLOAD -> point.downloadMbps
        WifiSurveyMetric.UPLOAD -> point.uploadMbps
        WifiSurveyMetric.LATENCY -> point.latencyMs?.toDouble()
        WifiSurveyMetric.PACKET_LOSS -> point.packetLossPercent?.toDouble()
    }
}
