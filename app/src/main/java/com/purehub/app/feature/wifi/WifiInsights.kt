package com.purehub.app.feature.wifi

data class WifiChannelInsight(
    val band: String,
    val channel: Int,
    val nearbyCount: Int,
    val strongestRssi: Int,
)

data class WifiChannelRating(
    val band: String,
    val channel: Int,
    val pressure: Double,
    val qualityPercent: Int,
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
        val quietest = rateChannels(networks).minByOrNull { it.pressure }
            ?: return "No channel recommendation is available yet."
        return "Best observed choice: ${quietest.band} channel ${quietest.channel} (${quietest.qualityPercent}% quality). Width, overlap and signal strength are included."
    }

    fun rateChannels(networks: List<NearbyWifiNetwork>, band: String? = null): List<WifiChannelRating> {
        val selected = networks.filter { band == null || bandForFrequency(it.frequencyMhz) == band }
        if (selected.isEmpty()) return emptyList()
        val bands = selected.map { bandForFrequency(it.frequencyMhz) }.distinct()
        return bands.flatMap { currentBand ->
            val bandNetworks = selected.filter { bandForFrequency(it.frequencyMhz) == currentBand }
            val candidates = when (currentBand) {
                "2.4 GHz" -> listOf(1, 6, 11)
                else -> bandNetworks.mapNotNull { channelForFrequency(it.frequencyMhz) }.distinct().sorted()
            }
            val raw = candidates.map { channel ->
                channel to bandNetworks.sumOf { network -> interferenceAt(channel, network) }
            }
            val maxPressure = raw.maxOfOrNull { it.second }?.coerceAtLeast(0.0001) ?: 1.0
            raw.map { (channel, pressure) ->
                WifiChannelRating(
                    band = currentBand,
                    channel = channel,
                    pressure = pressure,
                    qualityPercent = (100.0 - (pressure / maxPressure * 85.0)).toInt().coerceIn(5, 100),
                )
            }
        }.sortedWith(compareBy<WifiChannelRating> { it.band }.thenBy { it.channel })
    }

    private fun interferenceAt(candidateChannel: Int, network: NearbyWifiNetwork): Double {
        val networkChannel = channelForFrequency(network.frequencyMhz) ?: return 0.0
        val channelSpacingMhz = 5.0
        val separationMhz = kotlin.math.abs(candidateChannel - networkChannel) * channelSpacingMhz
        val overlapRange = (network.channelWidthMhz / 2.0 + 10.0).coerceAtLeast(20.0)
        val overlap = (1.0 - separationMhz / overlapRange).coerceIn(0.0, 1.0)
        val signalWeight = Math.pow(10.0, (network.rssi + 100.0) / 20.0)
        return overlap * signalWeight
    }
}
