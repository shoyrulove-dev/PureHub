package com.purehub.app.feature.wifi

import java.util.Locale
import kotlin.math.exp

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
    fun estimatedSirDb(target: NearbyWifiNetwork, networks: List<NearbyWifiNetwork>): Double? {
        val targetChannel = channelForFrequency(target.frequencyMhz) ?: return null
        val interferencePower = networks.asSequence().filter { it.bssid != target.bssid }.sumOf { other ->
            val otherChannel = channelForFrequency(other.frequencyMhz) ?: return@sumOf 0.0
            if (bandForFrequency(other.frequencyMhz) != bandForFrequency(target.frequencyMhz)) return@sumOf 0.0
            val separationMhz = kotlin.math.abs(targetChannel - otherChannel) * 5.0
            val overlapRange = (target.channelWidthMhz / 2.0 + other.channelWidthMhz / 2.0).coerceAtLeast(20.0)
            val overlap = (1.0 - separationMhz / overlapRange).coerceIn(0.0, 1.0)
            Math.pow(10.0, other.rssi / 10.0) * overlap
        }
        if (interferencePower <= 0.0) return null
        return 10.0 * kotlin.math.log10(Math.pow(10.0, target.rssi / 10.0) / interferencePower)
    }

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

    fun recommendation(
        networks: List<NearbyWifiNetwork>,
        preferredBand: String? = null,
        countryCode: String = Locale.getDefault().country,
    ): String {
        if (networks.isEmpty()) return "Scan nearby networks to compare channel pressure."
        val rated = rateChannels(networks, preferredBand, countryCode)
            .ifEmpty { rateChannels(networks, countryCode = countryCode) }
        val quietest = rated.minByOrNull { it.pressure }
            ?: return "No channel recommendation is available yet."
        val scope = if (preferredBand == null) "across nearby bands" else "on $preferredBand"
        return "Best observed choice $scope: channel ${quietest.channel} (${quietest.qualityPercent}% quality). Width, overlap and signal strength are included."
    }

    fun rateChannels(
        networks: List<NearbyWifiNetwork>,
        band: String? = null,
        countryCode: String = Locale.getDefault().country,
    ): List<WifiChannelRating> {
        val selected = networks.filter { band == null || bandForFrequency(it.frequencyMhz) == band }
        if (selected.isEmpty()) return emptyList()
        val bands = selected.map { bandForFrequency(it.frequencyMhz) }.distinct()
        return bands.flatMap { currentBand ->
            val bandNetworks = selected.filter { bandForFrequency(it.frequencyMhz) == currentBand }
            val candidates = candidateChannels(currentBand, countryCode)
                .ifEmpty { bandNetworks.mapNotNull { channelForFrequency(it.frequencyMhz) }.distinct().sorted() }
            val raw = candidates.map { channel ->
                channel to bandNetworks.sumOf { network -> interferenceAt(channel, network) }
            }
            raw.map { (channel, pressure) ->
                WifiChannelRating(
                    band = currentBand,
                    channel = channel,
                    pressure = pressure,
                    qualityPercent = (100.0 * exp(-pressure / 180.0)).toInt().coerceIn(5, 100),
                )
            }
        }.sortedWith(compareBy<WifiChannelRating> { it.band }.thenBy { it.channel })
    }

    internal fun candidateChannels(band: String, countryCode: String): List<Int> = when (band) {
        "2.4 GHz" -> when (countryCode.uppercase(Locale.US)) {
            "JP" -> listOf(1, 6, 11, 14)
            "US", "CA", "MX" -> listOf(1, 6, 11)
            else -> listOf(1, 5, 9, 13)
        }
        // Conservative non-DFS set. Upper UNII channels are included only in regions where they are commonly permitted.
        "5 GHz" -> if (countryCode.uppercase(Locale.US) in setOf("US", "CA", "MX", "BR", "AU", "NZ")) {
            listOf(36, 40, 44, 48, 149, 153, 157, 161)
        } else {
            listOf(36, 40, 44, 48)
        }
        // Preferred Scanning Channels keep the recommendation useful without rendering every 20 MHz channel.
        "6 GHz" -> (5..229 step 16).toList()
        else -> emptyList()
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
