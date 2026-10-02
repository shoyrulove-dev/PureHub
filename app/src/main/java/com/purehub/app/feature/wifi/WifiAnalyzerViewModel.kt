package com.purehub.app.feature.wifi

import android.annotation.SuppressLint
import android.Manifest
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.log10
import kotlin.math.pow
import java.util.UUID

data class NearbyWifiNetwork(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val level: Int,
    val frequencyMhz: Int,
    val channelLabel: String,
    val securityLabel: String,
    val isCurrentConnection: Boolean,
    val channelWidthMhz: Int = 20,
    val wifiStandard: String = "--",
    val estimatedDistanceMeters: Double? = null,
    val vendor: String = "Vendor unavailable",
    val estimatedSirDb: Double? = null,
    val isActive: Boolean = true,
    val lastSeenAtMillis: Long = System.currentTimeMillis(),
)

data class WifiSurveyPoint(
    val x: Float,
    val y: Float,
    val rssi: Int,
    val ssid: String,
    val timestamp: Long = System.currentTimeMillis(),
    val observedAccessPoints: Int = 0,
    val readings: List<WifiSurveyReading> = emptyList(),
    val latencyMs: Int? = null,
    val packetLossPercent: Int? = null,
    val downloadMbps: Double? = null,
    val uploadMbps: Double? = null,
)

data class WifiSurveyReading(
    val bssid: String,
    val ssid: String,
    val rssi: Int,
    val estimatedSirDb: Double?,
)

data class WifiRoamingEvent(val timestamp: Long, val fromBssid: String, val toBssid: String, val ssid: String)

data class WifiAnalyzerUiState(
    val ssid: String = "Unavailable",
    val bssid: String = "--",
    val rssi: Int = -100,
    val level: Int = 0,
    val linkSpeedMbps: Int = 0,
    val frequencyMhz: Int = 0,
    val channelLabel: String = "--",
    val nearbyNetworks: List<NearbyWifiNetwork> = emptyList(),
    val channelInsights: List<WifiChannelInsight> = emptyList(),
    val recommendation: String = "Scan nearby networks to compare channel pressure.",
    val rssiHistory: List<Int> = emptyList(),
    /** Per-AP traces are kept only in memory for the current private session. */
    val networkSignalHistories: Map<String, List<Int>> = emptyMap(),
    val lastNearbyScanAtMillis: Long? = null,
    val nearbyScanCount: Int = 0,
    val lastScanRequestAccepted: Boolean? = null,
    val isNearbyScanPaused: Boolean = false,
    val isRunningDiagnostics: Boolean = false,
    val latestDiagnostics: WifiDiagnosticResult? = null,
    val diagnosticHistory: List<WifiDiagnosticResult> = emptyList(),
    val speedProfile: WifiSpeedProfile = WifiSpeedProfile.BALANCED,
    val speedServerMode: WifiSpeedServerMode = WifiSpeedServerMode.AUTO,
    val customSpeedBaseUrl: String = "",
    val speedProgress: WifiSpeedProgress = WifiSpeedProgress(),
    val meshGroups: List<WifiMeshGroup> = emptyList(),
    val isMonitoring: Boolean = false,
    val monitorStartedAt: Long? = null,
    val monitorEndsAt: Long? = null,
    val monitorSamples: List<WifiMonitorSample> = emptyList(),
    val monitorEvents: List<WifiMonitorEvent> = emptyList(),
    val monitorReports: List<WifiMonitorReport> = emptyList(),
    val monitorIntervalSeconds: Int = 10,
    val isInspectingRouter: Boolean = false,
    val routerInsight: RouterInsight? = null,
    val controllerConfig: RouterControllerConfig? = null,
    val controllerStatus: RouterControllerStatus? = null,
    val isConnectingController: Boolean = false,
    val isDiscoveringDevices: Boolean = false,
    val lanDevices: List<LanDevice> = emptyList(),
    val scanningPortsFor: String? = null,
    val openPortsByHost: Map<String, List<OpenLanPort>> = emptyMap(),
    val lanActionMessage: String? = null,
    val trustedLanIps: Set<String> = emptySet(),
    val localIpAddress: String? = null,
    val gatewayAddress: String? = null,
    val accessPointVendor: String = "--",
    val isWalkTestActive: Boolean = false,
    val walkSamples: List<Int> = emptyList(),
    val isCapturingSurveyPoint: Boolean = false,
    val surveyPoints: List<WifiSurveyPoint> = emptyList(),
    val surveyProjects: List<WifiSurveyProject> = emptyList(),
    val activeSurveyProjectId: String = "",
    val surveyCaptureMode: WifiSurveyCaptureMode = WifiSurveyCaptureMode.QUICK_HEALTH,
    val scanIntervalSeconds: Int = 35,
    val roamingEvents: List<WifiRoamingEvent> = emptyList(),
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
    private var diagnosticsJob: Job? = null
    private var lastScanRequestAtMillis: Long = 0L
    @Volatile private var latestScanBroadcastAtMillis: Long = 0L
    private var consumedScanBroadcastAtMillis: Long = -1L
    private var scanReceiverRegistered = false
    private val diagnosticsPreferences = appContext.getSharedPreferences("purehub.wifi-diagnostics.v1", 0)
    private val discoveryPreferences = appContext.getSharedPreferences("purehub.wifi-discovery.v1", 0)
    private val surveyPreferences = appContext.getSharedPreferences("purehub.wifi-survey.v1", 0)
    private val monitorPreferences = appContext.getSharedPreferences(WifiMonitorService.PREFERENCES, 0)
    private val settingsPreferences = appContext.getSharedPreferences("purehub.wifi-pro-settings.v1", 0)
    private var lastConnectedBssid: String? = null
    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                latestScanBroadcastAtMillis = System.currentTimeMillis()
                val updated = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                _uiState.value = _uiState.value.copy(lastScanRequestAccepted = updated)
            }
        }
    }
    private var monitorReceiverRegistered = false
    private val monitorReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == WifiMonitorService.ACTION_UPDATED) refreshMonitorState()
        }
    }

    init {
        val legacyPoints = loadSurveyPoints()
        val (projects, activeProjectId) = WifiSurveyProjectStore.load(surveyPreferences, legacyPoints)
        _uiState.value = _uiState.value.copy(
            surveyPoints = projects.first { it.id == activeProjectId }.points,
            surveyProjects = projects,
            activeSurveyProjectId = activeProjectId,
            trustedLanIps = discoveryPreferences.getStringSet("trusted_ips", emptySet()).orEmpty(),
            monitorReports = WifiMonitorStore.load(monitorPreferences),
            controllerConfig = RouterControllerStore.load(appContext),
            speedServerMode = runCatching { WifiSpeedServerMode.valueOf(settingsPreferences.getString("speed_mode", WifiSpeedServerMode.AUTO.name).orEmpty()) }.getOrDefault(WifiSpeedServerMode.AUTO),
            customSpeedBaseUrl = settingsPreferences.getString("speed_custom_url", "").orEmpty(),
            monitorIntervalSeconds = settingsPreferences.getInt("monitor_interval", 10).coerceIn(5, 300),
            surveyCaptureMode = runCatching { WifiSurveyCaptureMode.valueOf(settingsPreferences.getString("survey_mode", WifiSurveyCaptureMode.QUICK_HEALTH.name).orEmpty()) }.getOrDefault(WifiSurveyCaptureMode.QUICK_HEALTH),
            scanIntervalSeconds = settingsPreferences.getInt("scan_interval", 35).coerceIn(10, 120),
        )
        refreshMonitorState()
    }

    fun start() {
        if (pollJob != null) return
        if (!scanReceiverRegistered) {
            ContextCompat.registerReceiver(
                appContext,
                scanReceiver,
                IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION),
                ContextCompat.RECEIVER_EXPORTED,
            )
            scanReceiverRegistered = true
        }
        if (!monitorReceiverRegistered) {
            ContextCompat.registerReceiver(appContext, monitorReceiver, IntentFilter(WifiMonitorService.ACTION_UPDATED), ContextCompat.RECEIVER_NOT_EXPORTED)
            monitorReceiverRegistered = true
        }
        pollJob = viewModelScope.launch {
            while (true) {
                pollWifiState()
                refreshMonitorState()
                delay(2_500)
            }
        }
    }

    fun stop() {
        pollJob?.cancel()
        pollJob = null
        if (scanReceiverRegistered) {
            runCatching { appContext.unregisterReceiver(scanReceiver) }
            scanReceiverRegistered = false
        }
        if (monitorReceiverRegistered) {
            runCatching { appContext.unregisterReceiver(monitorReceiver) }
            monitorReceiverRegistered = false
        }
    }

    fun setNearbyScanPaused(paused: Boolean) {
        _uiState.value = _uiState.value.copy(isNearbyScanPaused = paused)
    }

    fun setSpeedProfile(profile: WifiSpeedProfile) {
        if (!_uiState.value.isRunningDiagnostics) _uiState.value = _uiState.value.copy(speedProfile = profile)
    }

    fun setSpeedServer(mode: WifiSpeedServerMode, customBaseUrl: String = _uiState.value.customSpeedBaseUrl) {
        if (_uiState.value.isRunningDiagnostics) return
        val normalized = customBaseUrl.trim()
        settingsPreferences.edit().putString("speed_mode", mode.name).putString("speed_custom_url", normalized).apply()
        _uiState.value = _uiState.value.copy(speedServerMode = mode, customSpeedBaseUrl = normalized)
    }

    fun setMonitorInterval(seconds: Int) {
        if (_uiState.value.isMonitoring) return
        val value = seconds.coerceIn(5, 300)
        settingsPreferences.edit().putInt("monitor_interval", value).apply()
        _uiState.value = _uiState.value.copy(monitorIntervalSeconds = value)
    }

    fun setSurveyCaptureMode(mode: WifiSurveyCaptureMode) {
        settingsPreferences.edit().putString("survey_mode", mode.name).apply()
        _uiState.value = _uiState.value.copy(surveyCaptureMode = mode)
    }

    fun setScanInterval(seconds: Int) {
        val value = seconds.coerceIn(10, 120)
        settingsPreferences.edit().putInt("scan_interval", value).apply()
        _uiState.value = _uiState.value.copy(scanIntervalSeconds = value)
    }

    fun runDiagnostics() {
        if (_uiState.value.isRunningDiagnostics) return
        _uiState.value = _uiState.value.copy(isRunningDiagnostics = true)
        diagnosticsJob = viewModelScope.launch {
            runCatching {
                val profile = _uiState.value.speedProfile
                val endpoints = configuredSpeedEndpoints(_uiState.value)
                withContext(Dispatchers.IO) {
                    WifiDiagnostics.run(WifiDiagnostics.gatewayHost(wifiManager.dhcpInfo), profile, endpoints) { progress ->
                        _uiState.value = _uiState.value.copy(speedProgress = progress)
                    }
                }
            }.onSuccess { result ->
                val history = (listOf(result) + _uiState.value.diagnosticHistory).take(14)
                diagnosticsPreferences.edit().putString("history", history.joinToString(";") { row -> listOf(row.timestamp, row.gatewayMs ?: -1, row.internetMs ?: -1, row.jitterMs ?: -1, row.packetLossPercent ?: -1, row.dnsMs ?: -1, row.downloadMbps ?: -1.0, row.uploadMbps ?: -1.0, row.loadedLatencyMs ?: -1, row.dnsProvider.orEmpty()).joinToString("|") }).apply()
                _uiState.value = _uiState.value.copy(latestDiagnostics = result, diagnosticHistory = history)
            }
            _uiState.value = _uiState.value.copy(isRunningDiagnostics = false, speedProgress = WifiSpeedProgress())
            diagnosticsJob = null
        }
    }

    fun cancelDiagnostics() {
        diagnosticsJob?.cancel()
        diagnosticsJob = null
        _uiState.value = _uiState.value.copy(isRunningDiagnostics = false, speedProgress = WifiSpeedProgress())
    }

    fun startMonitoring(minutes: Int) {
        if (_uiState.value.isMonitoring) return
        val duration = minutes.coerceIn(1, 8 * 60)
        val serviceStartedAt = System.currentTimeMillis()
        ContextCompat.startForegroundService(
            appContext,
            Intent(appContext, WifiMonitorService::class.java)
                .putExtra(WifiMonitorService.EXTRA_DURATION_MINUTES, duration)
                .putExtra(WifiMonitorService.EXTRA_INTERVAL_SECONDS, _uiState.value.monitorIntervalSeconds),
        )
        _uiState.value = _uiState.value.copy(
            isMonitoring = true,
            monitorStartedAt = serviceStartedAt,
            monitorEndsAt = serviceStartedAt + duration * 60_000L,
            monitorSamples = emptyList(),
            monitorEvents = emptyList(),
        )
    }

    fun stopMonitoring() {
        val current = _uiState.value
        if (!current.isMonitoring) return
        appContext.startService(Intent(appContext, WifiMonitorService::class.java).setAction(WifiMonitorService.ACTION_STOP))
        viewModelScope.launch { delay(350); refreshMonitorState() }
    }

    private fun refreshMonitorState() {
        val active = WifiMonitorStore.loadActive(monitorPreferences)
        val previous = _uiState.value
        _uiState.value = previous.copy(
            isMonitoring = active != null,
            monitorStartedAt = active?.startedAt,
            monitorEndsAt = active?.endsAt,
            monitorSamples = active?.samples.orEmpty(),
            monitorEvents = active?.events.orEmpty(),
            monitorReports = WifiMonitorStore.load(monitorPreferences),
        )
    }

    fun inspectRouter() {
        if (_uiState.value.isInspectingRouter) return
        _uiState.value = _uiState.value.copy(isInspectingRouter = true)
        viewModelScope.launch {
            val insight = withContext(Dispatchers.IO) { RouterInspector.inspect(_uiState.value.gatewayAddress) }
            _uiState.value = _uiState.value.copy(isInspectingRouter = false, routerInsight = insight)
        }
    }

    fun saveControllerConfig(config: RouterControllerConfig?) {
        RouterControllerStore.save(appContext, config)
        _uiState.value = _uiState.value.copy(controllerConfig = config, controllerStatus = null)
    }

    fun connectController() {
        val config = _uiState.value.controllerConfig ?: return
        if (_uiState.value.isConnectingController) return
        _uiState.value = _uiState.value.copy(isConnectingController = true)
        viewModelScope.launch {
            val status = withContext(Dispatchers.IO) { RouterControllerClient.inspect(config) }
            _uiState.value = _uiState.value.copy(isConnectingController = false, controllerStatus = status)
        }
    }

    fun discoverLanDevices() {
        if (_uiState.value.isDiscoveringDevices) return
        _uiState.value = _uiState.value.copy(isDiscoveringDevices = true)
        viewModelScope.launch {
            val localIp = _uiState.value.localIpAddress
            val known = discoveryPreferences.getStringSet("known_ips", emptySet()).orEmpty()
            val multicastLock = runCatching { wifiManager.createMulticastLock("purehub-lan-discovery").apply { setReferenceCounted(false); acquire() } }.getOrNull()
            val devices = try {
                LanDiscovery.scan(localIp, known).map { device ->
                    device.copy(vendor = device.macAddress?.let { OuiDatabase.lookup(appContext, it) })
                }
            } finally { runCatching { multicastLock?.release() } }
            discoveryPreferences.edit().putStringSet("known_ips", devices.map { it.ipAddress }.toSet()).apply()
            _uiState.value = _uiState.value.copy(isDiscoveringDevices = false, lanDevices = devices)
        }
    }

    fun scanPorts(host: String, profile: PortScanProfile) {
        if (_uiState.value.scanningPortsFor != null) return
        _uiState.value = _uiState.value.copy(scanningPortsFor = host)
        viewModelScope.launch {
            val result = LanDiscovery.scanPorts(host, profile)
            val previous = _uiState.value
            _uiState.value = previous.copy(
                scanningPortsFor = null,
                openPortsByHost = previous.openPortsByHost + (host to result),
                lanDevices = previous.lanDevices.map { if (it.ipAddress == host) LanDiscovery.enrichIdentity(it, result) else it },
            )
        }
    }

    fun scanCustomPorts(host: String, specification: String) {
        if (_uiState.value.scanningPortsFor != null) return
        val count = LanDiscovery.parsePortSpecification(specification).size
        if (count == 0) {
            _uiState.value = _uiState.value.copy(lanActionMessage = "Enter valid ports such as 22,80,443 or 8000-8010.")
            return
        }
        _uiState.value = _uiState.value.copy(scanningPortsFor = host, lanActionMessage = "Scanning $count selected TCP ports…")
        viewModelScope.launch {
            val result = LanDiscovery.scanCustomPorts(host, specification)
            val previous = _uiState.value
            _uiState.value = previous.copy(
                scanningPortsFor = null,
                openPortsByHost = previous.openPortsByHost + (host to result),
                lanDevices = previous.lanDevices.map { if (it.ipAddress == host) LanDiscovery.enrichIdentity(it, result) else it },
                lanActionMessage = if (result.isEmpty()) "No selected TCP ports are open on $host." else "Found ${result.size} open TCP port(s) on $host.",
            )
        }
    }

    fun wakeDevice(macAddress: String) {
        val localIp = _uiState.value.localIpAddress
        viewModelScope.launch {
            val sent = LanDiscovery.wakeOnLan(macAddress, localIp)
            _uiState.value = _uiState.value.copy(
                lanActionMessage = if (sent) "Wake-on-LAN packet sent locally." else "Enter a valid device MAC address.",
            )
        }
    }

    fun clearLanActionMessage() {
        _uiState.value = _uiState.value.copy(lanActionMessage = null)
    }

    fun toggleTrustedDevice(host: String) {
        val previous = _uiState.value
        val next = if (host in previous.trustedLanIps) previous.trustedLanIps - host else previous.trustedLanIps + host
        discoveryPreferences.edit().putStringSet("trusted_ips", next).apply()
        _uiState.value = previous.copy(trustedLanIps = next)
    }

    fun createSurveyProject(name: String, floor: String) {
        val previous = _uiState.value
        val project = WifiSurveyProject(UUID.randomUUID().toString(), name.trim().ifBlank { "Wi-Fi survey" }, floor.trim().ifBlank { "Floor 1" })
        val projects = previous.surveyProjects + project
        saveSurveyProjects(projects, project.id)
        _uiState.value = previous.copy(surveyProjects = projects, activeSurveyProjectId = project.id, surveyPoints = emptyList())
    }

    fun selectSurveyProject(id: String) {
        val previous = _uiState.value
        val project = previous.surveyProjects.firstOrNull { it.id == id } ?: return
        saveSurveyProjects(previous.surveyProjects, id)
        _uiState.value = previous.copy(activeSurveyProjectId = id, surveyPoints = project.points)
    }

    fun deleteActiveSurveyProject() {
        val previous = _uiState.value
        if (previous.surveyProjects.size <= 1) return
        val projects = previous.surveyProjects.filterNot { it.id == previous.activeSurveyProjectId }
        val active = projects.first()
        saveSurveyProjects(projects, active.id)
        _uiState.value = previous.copy(surveyProjects = projects, activeSurveyProjectId = active.id, surveyPoints = active.points)
    }

    fun setActiveSurveyPlan(uri: String?) = updateActiveSurveyProject { it.copy(planUri = uri) }

    fun calibrateActiveSurvey(x1: Float, y1: Float, x2: Float, y2: Float, distanceMeters: Float) {
        if (distanceMeters <= 0f) return
        updateActiveSurveyProject { it.copy(calibration = WifiSurveyCalibration(x1, y1, x2, y2, distanceMeters)) }
    }

    fun addSurveyPoint(x: Float, y: Float) {
        if (_uiState.value.isCapturingSurveyPoint) return
        _uiState.value = _uiState.value.copy(isCapturingSurveyPoint = true)
        viewModelScope.launch {
            val samples = mutableListOf<Int>()
            val accessPointSamples = linkedMapOf<String, MutableList<NearbyWifiNetwork>>()
            repeat(7) {
                val snapshot = _uiState.value
                samples += snapshot.rssi
                snapshot.nearbyNetworks.filter(NearbyWifiNetwork::isActive).forEach { network ->
                    accessPointSamples.getOrPut(network.bssid) { mutableListOf() } += network
                }
                delay(700)
            }
            val previous = _uiState.value
            val median = samples.sorted()[samples.size / 2]
            val readings = accessPointSamples.mapNotNull { (bssid, observations) ->
                val representative = observations.maxByOrNull(NearbyWifiNetwork::lastSeenAtMillis) ?: return@mapNotNull null
                WifiSurveyReading(
                    bssid = bssid,
                    ssid = representative.ssid,
                    rssi = observations.map(NearbyWifiNetwork::rssi).sorted()[observations.size / 2],
                    estimatedSirDb = observations.mapNotNull(NearbyWifiNetwork::estimatedSirDb).takeIf(List<Double>::isNotEmpty)?.average(),
                )
            }
            val surveyDiagnostics = when (previous.surveyCaptureMode) {
                WifiSurveyCaptureMode.SIGNAL_ONLY -> null
                WifiSurveyCaptureMode.QUICK_HEALTH -> withContext(Dispatchers.IO) {
                    val health = WifiDiagnostics.quickHealth(previous.gatewayAddress)
                    WifiDiagnosticResult(
                        timestamp = System.currentTimeMillis(), gatewayMs = null, internetMs = health.second,
                        jitterMs = null, packetLossPercent = if (health.second == null) 100 else 0,
                        dnsMs = null, dnsProvider = null, loadedLatencyMs = null,
                        downloadMbps = null, uploadMbps = null, speedServer = "Quick health",
                    )
                }
                WifiSurveyCaptureMode.FULL_TEST -> if (com.purehub.app.BuildConfig.INTERNET_ENABLED) withContext(Dispatchers.IO) {
                    WifiDiagnostics.run(previous.gatewayAddress, WifiSpeedProfile.DATA_SAVER, configuredSpeedEndpoints(previous))
                } else null
            }
            val point = WifiSurveyPoint(
                x.coerceIn(0f, 1f),
                y.coerceIn(0f, 1f),
                median,
                previous.ssid,
                observedAccessPoints = readings.size,
                readings = readings,
                latencyMs = surveyDiagnostics?.internetMs ?: previous.latestDiagnostics?.internetMs,
                packetLossPercent = surveyDiagnostics?.packetLossPercent ?: previous.latestDiagnostics?.packetLossPercent,
                downloadMbps = surveyDiagnostics?.downloadMbps ?: previous.latestDiagnostics?.downloadMbps,
                uploadMbps = surveyDiagnostics?.uploadMbps ?: previous.latestDiagnostics?.uploadMbps,
            )
            val points = (previous.surveyPoints + point).takeLast(150)
            updateActiveSurveyProject(previous.copy(isCapturingSurveyPoint = false), points)
        }
    }

    fun undoSurveyPoint() {
        val previous = _uiState.value
        val points = previous.surveyPoints.dropLast(1)
        updateActiveSurveyProject(previous, points)
    }

    fun clearSurvey() {
        updateActiveSurveyProject(_uiState.value, emptyList())
    }

    fun setWalkTestActive(active: Boolean) {
        val previous = _uiState.value
        _uiState.value = previous.copy(isWalkTestActive = active, walkSamples = if (active) emptyList() else previous.walkSamples)
    }

    // The permission state is checked below and platform calls are guarded for permission races.
    @SuppressLint("MissingPermission")
    private fun pollWifiState() {
        val hasScanPermission = hasWifiScanPermission()
        @Suppress("DEPRECATION")
        val info: WifiInfo? = runCatching { wifiManager.connectionInfo }.getOrNull()
        val rssi = info?.rssi ?: -100
        val frequency = info?.frequency ?: 0
        @Suppress("DEPRECATION")
        val signalLevel = WifiManager.calculateSignalLevel(rssi, 5)

        val previous = _uiState.value
        val currentBssid = info?.bssid.orEmpty().takeUnless { it.isBlank() || it == "02:00:00:00:00:00" }
        val roamingEvents = if (currentBssid != null && lastConnectedBssid != null && currentBssid != lastConnectedBssid) {
            (listOf(WifiRoamingEvent(System.currentTimeMillis(), lastConnectedBssid!!, currentBssid, info?.ssid?.trim('"').orEmpty())) + previous.roamingEvents).take(12)
        } else previous.roamingEvents
        if (currentBssid != null) lastConnectedBssid = currentBssid
        val now = System.currentTimeMillis()
        val shouldRefreshNearbyScan = !previous.isNearbyScanPaused && now - lastScanRequestAtMillis >= previous.scanIntervalSeconds * 1_000L
        var latestNearbyScanAtMillis = _uiState.value.lastNearbyScanAtMillis
        var nearbyScanCount = _uiState.value.nearbyScanCount
        var lastScanRequestAccepted = previous.lastScanRequestAccepted
        val nearbyNetworks = if (previous.isNearbyScanPaused) {
            previous.nearbyNetworks
        } else if (hasScanPermission && wifiManager.isWifiEnabled) {
            runCatching {
                if (shouldRefreshNearbyScan) {
                    @Suppress("DEPRECATION")
                    lastScanRequestAccepted = wifiManager.startScan()
                    lastScanRequestAtMillis = now
                }
                val hasFreshBroadcast = latestScanBroadcastAtMillis > 0L && latestScanBroadcastAtMillis > consumedScanBroadcastAtMillis
                if (hasFreshBroadcast || previous.nearbyNetworks.isEmpty()) {
                    @Suppress("DEPRECATION")
                    val detected = wifiManager.scanResults.orEmpty()
                        .sortedByDescending { it.level }
                        .distinctBy { it.BSSID }
                        .map { result -> result.toNearbyNetwork(currentBssid = info?.bssid, observedAtMillis = now) }
                    val withSir = detected.map { network -> network.copy(estimatedSirDb = WifiInsights.estimatedSirDb(network, detected)) }
                    val detectedIds = detected.map(NearbyWifiNetwork::bssid).toSet()
                    val recentlyLost = previous.nearbyNetworks.filter { it.bssid !in detectedIds && now - it.lastSeenAtMillis <= 300_000 }
                        .map { it.copy(isActive = false, isCurrentConnection = false) }
                    if (hasFreshBroadcast) {
                        consumedScanBroadcastAtMillis = latestScanBroadcastAtMillis
                        latestNearbyScanAtMillis = latestScanBroadcastAtMillis
                        nearbyScanCount += 1
                    }
                    (withSir + recentlyLost).sortedWith(compareByDescending<NearbyWifiNetwork> { it.isActive }.thenByDescending { it.rssi })
                } else {
                    previous.nearbyNetworks.map { network ->
                        network.copy(isActive = now - network.lastSeenAtMillis <= 90_000)
                    }
                }
            }.getOrDefault(emptyList())
        } else {
            emptyList()
        }

        val history = (previous.rssiHistory + rssi).takeLast(30)
        val walkSamples = if (previous.isWalkTestActive) (previous.walkSamples + rssi).takeLast(720) else previous.walkSamples
        val signalHistories = nearbyNetworks.associate { network ->
            network.bssid to ((previous.networkSignalHistories[network.bssid].orEmpty() + network.rssi).takeLast(60))
        }
        _uiState.value = WifiAnalyzerUiState(
            ssid = info?.ssid?.trim('"').orEmpty().ifBlank { "Not connected" },
            bssid = info?.bssid.orEmpty().ifBlank { "--" },
            rssi = rssi,
            level = signalLevel,
            linkSpeedMbps = info?.linkSpeed ?: 0,
            frequencyMhz = frequency,
            channelLabel = deriveChannel(frequency),
            nearbyNetworks = nearbyNetworks,
            channelInsights = WifiInsights.analyze(nearbyNetworks),
            recommendation = WifiInsights.recommendation(
                nearbyNetworks,
                WifiInsights.bandForFrequency(frequency).takeUnless { it == "Unknown" },
            ),
            rssiHistory = history,
            networkSignalHistories = signalHistories,
            lastNearbyScanAtMillis = latestNearbyScanAtMillis,
            nearbyScanCount = nearbyScanCount,
            lastScanRequestAccepted = lastScanRequestAccepted,
            isNearbyScanPaused = previous.isNearbyScanPaused,
            isRunningDiagnostics = previous.isRunningDiagnostics,
            latestDiagnostics = previous.latestDiagnostics,
            diagnosticHistory = previous.diagnosticHistory.ifEmpty { loadDiagnosticHistory() },
            speedProfile = previous.speedProfile,
            speedProgress = previous.speedProgress,
            meshGroups = WifiPremiumInsights.meshGroups(nearbyNetworks),
            isMonitoring = previous.isMonitoring,
            monitorStartedAt = previous.monitorStartedAt,
            monitorEndsAt = previous.monitorEndsAt,
            monitorSamples = previous.monitorSamples,
            monitorEvents = previous.monitorEvents,
            monitorReports = previous.monitorReports,
            isInspectingRouter = previous.isInspectingRouter,
            routerInsight = previous.routerInsight,
            isDiscoveringDevices = previous.isDiscoveringDevices,
            lanDevices = previous.lanDevices,
            scanningPortsFor = previous.scanningPortsFor,
            openPortsByHost = previous.openPortsByHost,
            lanActionMessage = previous.lanActionMessage,
            trustedLanIps = previous.trustedLanIps,
            localIpAddress = WifiDiagnostics.ipv4(wifiManager.dhcpInfo?.ipAddress ?: 0),
            gatewayAddress = WifiDiagnostics.gatewayHost(wifiManager.dhcpInfo),
            accessPointVendor = OuiDatabase.lookup(appContext, info?.bssid.orEmpty()),
            isWalkTestActive = previous.isWalkTestActive,
            walkSamples = walkSamples,
            isCapturingSurveyPoint = previous.isCapturingSurveyPoint,
            surveyPoints = previous.surveyPoints,
            surveyProjects = previous.surveyProjects,
            activeSurveyProjectId = previous.activeSurveyProjectId,
            roamingEvents = roamingEvents,
            hasScanPermission = hasScanPermission,
            note = when {
                !wifiManager.isWifiEnabled -> "Wi-Fi is currently turned off."
                !hasScanPermission -> "Grant Nearby Wi-Fi and Location to scan nearby networks and show signal ranking."
                previous.isNearbyScanPaused -> "Nearby Wi-Fi scan is paused. Existing local results remain on this device."
                lastScanRequestAccepted == false -> "Android deferred the latest scan request. Showing cached results until fresh data arrives."
                shouldRefreshNearbyScan -> "Live signal history is updating. Nearby Wi-Fi scan refreshes locally when Android allows it."
                else -> "Live signal history is updating. Nearby Wi-Fi results are cached between local refreshes."
            },
        )
    }

    private fun loadDiagnosticHistory(): List<WifiDiagnosticResult> = diagnosticsPreferences.getString("history", "").orEmpty()
        .split(';').mapNotNull { line ->
            val values = line.split('|')
            if (values.size < 8) null else runCatching {
                WifiDiagnosticResult(values[0].toLong(), values[1].toInt().nullIfNegative(), values[2].toInt().nullIfNegative(), values[3].toInt().nullIfNegative(), values[4].toInt().nullIfNegative(), values[5].toInt().nullIfNegative(), values.getOrNull(9)?.ifBlank { null }, values.getOrNull(8)?.toIntOrNull()?.nullIfNegative(), values[6].toDouble().nullIfNegative(), values[7].toDouble().nullIfNegative())
            }.getOrNull()
        }

    private fun saveSurveyPoints(points: List<WifiSurveyPoint>) {
        surveyPreferences.edit().putString("points", points.joinToString(";") { point ->
            val readings = point.readings.joinToString("~") { reading ->
                listOf(
                    android.net.Uri.encode(reading.bssid),
                    android.net.Uri.encode(reading.ssid),
                    reading.rssi,
                    reading.estimatedSirDb ?: "",
                ).joinToString(",")
            }
            listOf(point.x, point.y, point.rssi, point.timestamp, point.observedAccessPoints, android.net.Uri.encode(point.ssid), readings).joinToString("|")
        }).apply()
    }

    private fun updateActiveSurveyProject(transform: (WifiSurveyProject) -> WifiSurveyProject) {
        val previous = _uiState.value
        val projects = previous.surveyProjects.map { if (it.id == previous.activeSurveyProjectId) transform(it) else it }
        val active = projects.firstOrNull { it.id == previous.activeSurveyProjectId } ?: return
        saveSurveyProjects(projects, active.id)
        _uiState.value = previous.copy(surveyProjects = projects, surveyPoints = active.points)
    }

    private fun updateActiveSurveyProject(baseState: WifiAnalyzerUiState, points: List<WifiSurveyPoint>) {
        val projects = baseState.surveyProjects.map { if (it.id == baseState.activeSurveyProjectId) it.copy(points = points) else it }
        saveSurveyProjects(projects, baseState.activeSurveyProjectId)
        saveSurveyPoints(points)
        _uiState.value = baseState.copy(surveyProjects = projects, surveyPoints = points)
    }

    private fun saveSurveyProjects(projects: List<WifiSurveyProject>, activeId: String) {
        WifiSurveyProjectStore.save(surveyPreferences, projects, activeId)
    }

    private fun configuredSpeedEndpoints(state: WifiAnalyzerUiState): List<WifiSpeedEndpoint> {
        val custom = state.customSpeedBaseUrl.takeIf { it.startsWith("https://") || it.startsWith("http://") }
            ?.let { WifiSpeedEndpoint("custom", "Private endpoint", it) }
        return when (state.speedServerMode) {
            WifiSpeedServerMode.CLOUDFLARE -> listOf(WifiDiagnostics.cloudflareEndpoint)
            WifiSpeedServerMode.CUSTOM -> listOfNotNull(custom).ifEmpty { listOf(WifiDiagnostics.cloudflareEndpoint) }
            WifiSpeedServerMode.AUTO -> listOfNotNull(WifiDiagnostics.cloudflareEndpoint, custom)
        }
    }

    private fun loadSurveyPoints(): List<WifiSurveyPoint> = surveyPreferences.getString("points", "").orEmpty().split(';').mapNotNull { row ->
        val values = row.split('|'); if (values.size < 6) null else runCatching {
            val readings = values.getOrNull(6).orEmpty().split('~').mapNotNull { encoded ->
                val fields = encoded.split(',')
                if (fields.size != 4) null else WifiSurveyReading(
                    android.net.Uri.decode(fields[0]),
                    android.net.Uri.decode(fields[1]),
                    fields[2].toInt(),
                    fields[3].toDoubleOrNull(),
                )
            }
            WifiSurveyPoint(values[0].toFloat(), values[1].toFloat(), values[2].toInt(), android.net.Uri.decode(values[5]), values[3].toLong(), values[4].toInt(), readings)
        }.getOrNull()
    }

    private fun Int.nullIfNegative(): Int? = takeIf { it >= 0 }
    private fun Double.nullIfNegative(): Double? = takeIf { it >= 0 }

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

    private fun ScanResult.toNearbyNetwork(currentBssid: String?, observedAtMillis: Long): NearbyWifiNetwork {
        @Suppress("DEPRECATION")
        val level = WifiManager.calculateSignalLevel(this.level, 5)
        return NearbyWifiNetwork(
            ssid = SSID.ifBlank { "Hidden network" },
            bssid = BSSID.orEmpty().ifBlank { "--" },
            rssi = this.level,
            level = level,
            frequencyMhz = frequency,
            channelLabel = deriveChannel(frequency),
            securityLabel = WifiInsights.securityLabel(capabilities),
            isCurrentConnection = BSSID == currentBssid,
            channelWidthMhz = when (channelWidth) {
                ScanResult.CHANNEL_WIDTH_40MHZ -> 40
                ScanResult.CHANNEL_WIDTH_80MHZ -> 80
                ScanResult.CHANNEL_WIDTH_160MHZ -> 160
                ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ -> 160
                5 -> 320
                else -> 20
            },
            wifiStandard = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) wifiStandardLabel(wifiStandard) else "--",
            estimatedDistanceMeters = estimateDistance(this.level, frequency),
            vendor = OuiDatabase.lookup(appContext, BSSID.orEmpty()),
            lastSeenAtMillis = observedAtMillis,
        )
    }

    private fun wifiStandardLabel(value: Int): String = when (value) {
        ScanResult.WIFI_STANDARD_11N -> "Wi-Fi 4"
        ScanResult.WIFI_STANDARD_11AC -> "Wi-Fi 5"
        ScanResult.WIFI_STANDARD_11AX -> "Wi-Fi 6/6E"
        7 -> "Wi-Fi 7"
        else -> "Legacy"
    }

    private fun estimateDistance(rssi: Int, frequencyMhz: Int): Double? {
        if (frequencyMhz <= 0 || rssi >= 0) return null
        return 10.0.pow((27.55 - 20.0 * log10(frequencyMhz.toDouble()) + kotlin.math.abs(rssi)) / 20.0)
    }

    private fun deriveChannelLegacy(frequencyMhz: Int): String {
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

    private fun deriveChannel(frequencyMhz: Int): String = WifiInsights.channelLabel(frequencyMhz)

    override fun onCleared() {
        stop()
        super.onCleared()
    }
}
