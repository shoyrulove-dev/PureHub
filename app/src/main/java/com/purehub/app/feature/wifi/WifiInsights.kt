package com.purehub.app.feature.wifi

data class WifiChannelInsight(
    val band: String,
    val channel: Int,
    val nearbyCount: Int,
    val strongestRssi: Int,
)

object WifiInsights {
    fun channelForFrequency(frequencyMhz: Int): Int? = when (frequencyMhz) {
        2484 -> 14
        in 2412..2472 -> (frequencyMhz - 2407) / 5
        in 5000..5900 -> (frequencyMhz - 5000) / 5
        in 5955..7115 -> (frequencyMhz - 5950) / 5
        else -> null
    }

    fun bandForFrequency(frequencyMhz: Int): String = when (frequencyMhz) {
        in 2412..2484 -> "2.4 GHz"
        in 5000..5900 -> "5 GHz"
        in 5925..7125 -> "6 GHz"
        else -> "Unknown"
    }

    fun channelLabel(frequencyMhz: Int): String {
        val channel = channelForFrequency(frequencyMhz) ?: return "--"
        return "${bandForFrequency(frequencyMhz)} · Ch $channel"
    }

    fun securityLabel(capabilities: String): String = when {
        capabilities.contains("WPA3", ignoreCase = true) || capabilities.contains("SAE", ignoreCase = true) -> "WPA3"
        capabilities.contains("WPA2", ignoreCase = true) -> "WPA2"
        capabilities.contains("WPA", ignoreCase = true) -> "WPA"
        capabilities.contains("WEP", ignoreCase = true) -> "WEP"
        capabilities.contains("OWE", ignoreCase = true) -> "Enhanced open"
        else -> "Open"
    }

    fun analyze(networks: List<NearbyWifiNetwork>): List<WifiChannelInsight> = networks
        .mapNotNull { network ->
            channelForFrequency(network.frequencyMhz)?.let { channel ->
                Triple(bandForFrequency(network.frequencyMhz), channel, network)
            }
        }
        .groupBy { it.first to it.second }
        .map { (key, values) ->
            WifiChannelInsight(key.first, key.second, values.size, values.maxOf { it.third.rssi })
        }
        .sortedWith(compareBy<WifiChannelInsight> { it.band }.thenBy { it.nearbyCount }.thenBy { it.channel })

    fun recommendation(networks: List<NearbyWifiNetwork>): String {
        if (networks.isEmpty()) return "Scan nearby networks to compare channel pressure."
        val quietest = analyze(networks).minWithOrNull(
            compareBy<WifiChannelInsight> { it.nearbyCount }.thenBy { it.strongestRssi },
        ) ?: return "No channel recommendation is available yet."
        return "Lowest observed pressure: ${quietest.band} channel ${quietest.channel} (${quietest.nearbyCount} nearby). Router conditions can still vary."
    }
}
