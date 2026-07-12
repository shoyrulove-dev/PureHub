package com.purehub.app.feature.wifi

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NearbyWifiNetwork(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val level: Int,
    val frequencyMhz: Int,
    val channelLabel: String,
    val isCurrentConnection: Boolean,
)

data class WifiAnalyzerUiState(
    val ssid: String = "Unavailable",
    val bssid: String = "--",
    val rssi: Int = -100,
    val level: Int = 0,
    val linkSpeedMbps: Int = 0,
    val frequencyMhz: Int = 0,
    val channelLabel: String = "--",
    val nearbyNetworks: List<NearbyWifiNetwork> = emptyList(),
    val rssiHistory: List<Int> = emptyList(),
    val hasScanPermission: Boolean = false,
    val note: String = "Wi-Fi details stay on-device. Grant Nearby Wi-Fi and Location to unlock nearby scan results.",
)

class WifiAnalyzerViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val wifiManager = appContext.getSystemService(WifiManager::class.java)
    private val _uiState = MutableStateFlow(WifiAnalyzerUiState())
    val uiState: StateFlow<WifiAnalyzerUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null
    private var lastNearbyScanAtMillis: Long = 0L

    fun start() {
        if (pollJob != null) return
        pollJob = viewModelScope.launch {
            while (true) {
                pollWifiState()
                delay(2_500)
            }
        }
    }

    fun stop() {
        pollJob?.cancel()
        pollJob = null
    }

    private fun pollWifiState() {
        val hasScanPermission = hasWifiScanPermission()
        @Suppress("DEPRECATION")
        val info: WifiInfo? = runCatching { wifiManager.connectionInfo }.getOrNull()
        val rssi = info?.rssi ?: -100
        val frequency = info?.frequency ?: 0
        @Suppress("DEPRECATION")
        val signalLevel = WifiManager.calculateSignalLevel(rssi, 5)

        val shouldRefreshNearbyScan = System.currentTimeMillis() - lastNearbyScanAtMillis >= 15_000
        val nearbyNetworks = if (hasScanPermission && wifiManager.isWifiEnabled) {
            runCatching {
                if (shouldRefreshNearbyScan) {
                    @Suppress("DEPRECATION")
                    wifiManager.startScan()
                    lastNearbyScanAtMillis = System.currentTimeMillis()
                }
                @Suppress("DEPRECATION")
                wifiManager.scanResults
                    .orEmpty()
                    .sortedByDescending { it.level }
                    .distinctBy { it.BSSID }
                    .take(12)
                    .map { result ->
                        result.toNearbyNetwork(
                            currentBssid = info?.bssid,
                        )
                    }
            }.getOrDefault(emptyList())
        } else {
            emptyList()
        }

        val history = (_uiState.value.rssiHistory + rssi).takeLast(30)
        _uiState.value = WifiAnalyzerUiState(
            ssid = info?.ssid?.trim('"').orEmpty().ifBlank { "Not connected" },
            bssid = info?.bssid.orEmpty().ifBlank { "--" },
            rssi = rssi,
            level = signalLevel,
            linkSpeedMbps = info?.linkSpeed ?: 0,
            frequencyMhz = frequency,
            channelLabel = deriveChannel(frequency),
            nearbyNetworks = nearbyNetworks,
            rssiHistory = history,
            hasScanPermission = hasScanPermission,
            note = when {
                !wifiManager.isWifiEnabled -> "Wi-Fi is currently turned off."
                !hasScanPermission -> "Grant Nearby Wi-Fi and Location to scan nearby networks and show signal ranking."
                shouldRefreshNearbyScan -> "Live signal history is updating. Nearby Wi-Fi scan refreshes locally every few seconds."
                else -> "Live signal history is updating. Nearby Wi-Fi results are cached between local refreshes."
            },
        )
    }

    private fun hasWifiScanPermission(): Boolean {
        val hasLocation = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val hasNearbyWifi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.NEARBY_WIFI_DEVICES,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return hasLocation && hasNearbyWifi
    }

    private fun ScanResult.toNearbyNetwork(currentBssid: String?): NearbyWifiNetwork {
        @Suppress("DEPRECATION")
        val level = WifiManager.calculateSignalLevel(this.level, 5)
        return NearbyWifiNetwork(
            ssid = SSID.ifBlank { "Hidden network" },
            bssid = BSSID.orEmpty().ifBlank { "--" },
            rssi = this.level,
            level = level,
            frequencyMhz = frequency,
            channelLabel = deriveChannel(frequency),
            isCurrentConnection = BSSID == currentBssid,
        )
    }

    private fun deriveChannel(frequencyMhz: Int): String {
        if (frequencyMhz in 2412..2484) {
            val channel = ((frequencyMhz - 2407) / 5)
            return "2.4 GHz • Ch $channel"
        }
        if (frequencyMhz in 5000..5900) {
            val channel = (frequencyMhz - 5000) / 5
            return "5 GHz • Ch $channel"
        }
        if (frequencyMhz in 5925..7125) {
            val channel = (frequencyMhz - 5950) / 5
            return "6 GHz • Ch $channel"
        }
        return "--"
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }
}
