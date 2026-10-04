package com.purehub.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NetworkWifi
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.purehub.app.BuildConfig
import com.purehub.app.feature.wifi.*
import com.purehub.app.ui.LocalAppLanguage
import com.purehub.app.ui.LocalSnackbarHostState
import com.purehub.app.ui.LocalizedText
import com.purehub.app.ui.locale
import com.purehub.app.ui.translateUiText
import kotlinx.coroutines.launch

/** Scanner-first surface: live connection first, technical discovery in compact tabs. */
private enum class WifiTab(val label: String) { SIGNAL("Overview"), CHANNELS("Channels"), NETWORKS("Scan"), TESTS("Diagnose"), DEVICES("Devices"), WALK("Walk"), SURVEY("Map") }
private enum class WifiNetworkSort { SIGNAL, NAME, CHANNEL }
private val WifiInk = Color(0xFF07131E)
private val WifiPanel = Color(0xFF0D2230)
private val WifiMint = Color(0xFFA970FF)
private val WifiBlue = Color(0xFF71B8FF)

@Composable
fun WifiAnalyzerCard(modifier: Modifier = Modifier, viewModel: WifiAnalyzerViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbar = LocalSnackbarHostState.current
    val language = LocalAppLanguage.current
    val scope = rememberCoroutineScope()
    var hasPermission by remember { mutableStateOf(checkWifiScanPermission(context)) }
    var tab by rememberSaveable { mutableStateOf(WifiTab.SIGNAL) }
    var selectedBand by rememberSaveable { mutableStateOf("2.4 GHz") }
    var menuOpen by remember { mutableStateOf(false) }
    var filterText by rememberSaveable { mutableStateOf("") }
    var minimumSignal by rememberSaveable { mutableStateOf(-100) }
    var securityOnly by rememberSaveable { mutableStateOf(false) }
    var showLostNetworks by rememberSaveable { mutableStateOf(true) }
    var networkSort by rememberSaveable { mutableStateOf(WifiNetworkSort.SIGNAL) }
    var comparedBssids by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingMonitorMinutes by remember { mutableStateOf<Int?>(null) }
    val activeSurveyProject = state.surveyProjects.firstOrNull { it.id == state.activeSurveyProjectId }
    val bands = state.nearbyNetworks.map { WifiInsights.bandForFrequency(it.frequencyMhz) }.filterNot { it == "Unknown" }.distinct()
    val activeBand = selectedBand.takeIf { it in bands } ?: bands.firstOrNull().orEmpty()
    val ratings = WifiInsights.rateChannels(state.nearbyNetworks, activeBand.takeIf(String::isNotBlank))
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        hasPermission = result.values.all { it }
        scope.launch { snackbar.showSnackbar(translateUiText(if (hasPermission) "Nearby Wi-Fi scan is ready on this device." else "Without permission, PureHub only shows your current connection.", language)) }
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val minutes = pendingMonitorMinutes
        pendingMonitorMinutes = null
        if (granted && minutes != null) viewModel.startMonitoring(minutes)
        else if (!granted) scope.launch { snackbar.showSnackbar(translateUiText("Notification permission is required for reliable background monitoring.", language)) }
    }
    fun startForegroundMonitor(minutes: Int) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            pendingMonitorMinutes = minutes
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else viewModel.startMonitoring(minutes)
    }
    val surveyPlanLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            viewModel.setActiveSurveyPlan(uri.toString())
        }
    }
    DisposableEffect(Unit) { viewModel.start(); onDispose { viewModel.stop() } }

    val filteredNetworks = state.nearbyNetworks.filter { network ->
        network.rssi >= minimumSignal &&
            (showLostNetworks || network.isActive) &&
            (!securityOnly || network.securityLabel !in setOf("Open", "Enhanced open")) &&
            (filterText.isBlank() || network.ssid.contains(filterText, ignoreCase = true) || network.bssid.contains(filterText, ignoreCase = true))
    }.let { filtered ->
        when (networkSort) {
            WifiNetworkSort.SIGNAL -> filtered.sortedWith(compareByDescending<NearbyWifiNetwork> { it.isActive }.thenByDescending { it.rssi })
            WifiNetworkSort.NAME -> filtered.sortedWith(compareByDescending<NearbyWifiNetwork> { it.isActive }.thenBy { it.ssid.lowercase() })
            WifiNetworkSort.CHANNEL -> filtered.sortedWith(compareByDescending<NearbyWifiNetwork> { it.isActive }.thenBy { WifiInsights.channelForFrequency(it.frequencyMhz) ?: Int.MAX_VALUE })
        }
    }
    Surface(color = WifiInk, shape = RoundedCornerShape(28.dp), modifier = modifier.fillMaxSize()) {
        Column {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                WifiConnectionCanvas(state, tab != WifiTab.SIGNAL, menuOpen, { menuOpen = it }, viewModel::setNearbyScanPaused) { tab = it }
                Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (tab) {
                    WifiTab.SIGNAL -> SignalPane(state, comparedBssids)
                    WifiTab.CHANNELS -> ChannelsPane(hasPermission, bands, activeBand, { selectedBand = it }, state.recommendation, ratings, state.nearbyNetworks) { launcher.launch(wifiScanPermissions()) }
                    WifiTab.NETWORKS -> NetworksPane(
                        hasPermission, filteredNetworks, state.nearbyScanCount, filterText, { filterText = it }, minimumSignal,
                        { minimumSignal = it }, securityOnly, { securityOnly = it }, comparedBssids,
                        { bssid -> comparedBssids = if (bssid in comparedBssids) comparedBssids - bssid else (comparedBssids + bssid).take(4).toSet() },
                        state.isNearbyScanPaused, viewModel::setNearbyScanPaused, context,
                        showLostNetworks, { showLostNetworks = it }, networkSort, { networkSort = it },
                    ) { launcher.launch(wifiScanPermissions()) }
                    WifiTab.TESTS -> DiagnosticsPane(
                        state, viewModel::runDiagnostics, viewModel::cancelDiagnostics,
                        viewModel::setSpeedProfile, ::startForegroundMonitor, viewModel::stopMonitoring,
                        viewModel::inspectRouter, viewModel::setSpeedServer, viewModel::setMonitorInterval,
                        viewModel::saveControllerConfig, viewModel::connectController, viewModel::setScanInterval,
                    )
                    WifiTab.DEVICES -> DevicesPane(
                        state,
                        viewModel::discoverLanDevices,
                        viewModel::scanPorts,
                        viewModel::scanCustomPorts,
                        viewModel::wakeDevice,
                        viewModel::toggleTrustedDevice,
                        viewModel::clearLanActionMessage,
                    )
                    WifiTab.WALK -> WalkTestPane(state, viewModel::setWalkTestActive)
                    WifiTab.SURVEY -> SurveyPane(
                        state = state,
                        activeProject = activeSurveyProject,
                        choosePlan = { surveyPlanLauncher.launch(arrayOf("image/*")) },
                        createProject = viewModel::createSurveyProject,
                        selectProject = viewModel::selectSurveyProject,
                        deleteProject = viewModel::deleteActiveSurveyProject,
                        calibrate = viewModel::calibrateActiveSurvey,
                        setCaptureMode = viewModel::setSurveyCaptureMode,
                        addPoint = viewModel::addSurveyPoint,
                        undo = viewModel::undoSurveyPoint,
                        clear = viewModel::clearSurvey,
                        context = context,
                    )
                }
            }
            }
            WifiTabs(tab) { tab = it }
        }
    }
}

@Composable
private fun WifiConnectionCanvas(state: WifiAnalyzerUiState, compact: Boolean, menuOpen: Boolean, onMenu: (Boolean) -> Unit, onPauseChanged: (Boolean) -> Unit, onNavigate: (WifiTab) -> Unit) {
    val qualityColor = signalColor(state.rssi)
    if (compact) {
        Column(
            Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF123747), WifiPanel))).padding(horizontal = 14.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WifiMenuButton(state, menuOpen, onMenu, onPauseChanged, onNavigate)
                Column(Modifier.padding(start = 10.dp).weight(1f)) {
                    LocalizedText(state.ssid, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    LocalizedText("${WifiInsights.bandForFrequency(state.frequencyMhz)} · ${state.channelLabel.substringAfter("· ", state.channelLabel)} · ${state.linkSpeedMbps} Mbps", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = .58f), maxLines = 1)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${state.rssi} dBm", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    LocalizedText(signalQuality(state.rssi), color = qualityColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
            LinearProgressIndicator(progress = { ((state.level + 1) * .2f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(3.dp), color = qualityColor, trackColor = Color.White.copy(alpha = .10f))
        }
        return
    }
    Column(
        Modifier.fillMaxWidth().background(
            Brush.verticalGradient(listOf(Color(0xFF102D3C), WifiPanel, WifiInk)),
        ).padding(horizontal = 18.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            WifiMenuButton(state, menuOpen, onMenu, onPauseChanged, onNavigate)
            Column(Modifier.padding(start = 10.dp).weight(1f)) {
                LocalizedText("PUREHUB WI-FI", style = MaterialTheme.typography.labelSmall, color = WifiMint, fontWeight = FontWeight.Black)
                LocalizedText("Network health", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = .58f))
            }
            LocalizedText(signalQuality(state.rssi), style = MaterialTheme.typography.labelLarge, color = qualityColor, fontWeight = FontWeight.Black)
        }
        LocalizedText(state.ssid, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.Bottom) {
            LocalizedText("${state.rssi}", style = MaterialTheme.typography.displayMedium, color = Color.White, fontWeight = FontWeight.Black)
            LocalizedText(" dBm", style = MaterialTheme.typography.titleSmall, color = Color.White.copy(alpha = .6f), modifier = Modifier.padding(bottom = 7.dp))
            Spacer(Modifier.weight(1f)); SignalBars(state.level, qualityColor)
        }
        SignalTrace(state.rssiHistory, qualityColor)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WifiPill("${state.linkSpeedMbps} Mbps", "Link")
            WifiPill(WifiInsights.bandForFrequency(state.frequencyMhz).replace("Unknown", "--"), "Band")
            WifiPill(state.channelLabel.substringAfter("Ch ", "--"), "Channel")
        }
        val health = ((state.level + 1) * 20).coerceIn(0, 100)
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(progress = { health / 100f }, modifier = Modifier.weight(1f).height(5.dp), color = qualityColor, trackColor = Color.White.copy(alpha = .10f))
            LocalizedText("Signal $health/100", modifier = Modifier.padding(start = 10.dp), color = qualityColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun WifiMenuButton(state: WifiAnalyzerUiState, menuOpen: Boolean, onMenu: (Boolean) -> Unit, onPauseChanged: (Boolean) -> Unit, onNavigate: (WifiTab) -> Unit) {
    Box {
        Surface(color = WifiMint.copy(alpha = .14f), shape = RoundedCornerShape(14.dp)) {
            IconButton(onClick = { onMenu(true) }, modifier = Modifier.size(40.dp)) { Icon(Icons.Rounded.Menu, "Wi-Fi tools", tint = WifiMint) }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { onMenu(false) }) {
            DropdownMenuItem(text = { LocalizedText("CONNECTION", color = WifiMint, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black) }, onClick = {}, enabled = false)
            DropdownMenuItem(text = { LocalizedText("Overview") }, onClick = { onNavigate(WifiTab.SIGNAL); onMenu(false) })
            DropdownMenuItem(text = { LocalizedText("Scan networks") }, onClick = { onNavigate(WifiTab.NETWORKS); onMenu(false) })
            DropdownMenuItem(text = { LocalizedText("Channel graph") }, onClick = { onNavigate(WifiTab.CHANNELS); onMenu(false) })
            HorizontalDivider()
            DropdownMenuItem(text = { LocalizedText("TOOLS", color = WifiMint, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black) }, onClick = {}, enabled = false)
            DropdownMenuItem(text = { LocalizedText("Network tests") }, onClick = { onNavigate(WifiTab.TESTS); onMenu(false) })
            DropdownMenuItem(text = { LocalizedText("LAN devices") }, onClick = { onNavigate(WifiTab.DEVICES); onMenu(false) })
            DropdownMenuItem(text = { LocalizedText("Walk test") }, onClick = { onNavigate(WifiTab.WALK); onMenu(false) })
            DropdownMenuItem(text = { LocalizedText("Coverage survey") }, onClick = { onNavigate(WifiTab.SURVEY); onMenu(false) })
            HorizontalDivider()
            DropdownMenuItem(text = { LocalizedText(if (state.isNearbyScanPaused) "Resume scan" else "Pause scan") }, onClick = { onPauseChanged(!state.isNearbyScanPaused); onMenu(false) })
        }
    }
}

@Composable private fun SignalBars(level: Int, color: Color) = Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
    (1..4).forEach { bar -> Box(Modifier.size(width = 6.dp, height = (bar * 7).dp).background(if (bar <= level + 1) color else Color.White.copy(alpha = .16f), RoundedCornerShape(8.dp))) }
}

@Composable
private fun SignalTrace(history: List<Int>, color: Color) {
    Canvas(Modifier.fillMaxWidth().height(54.dp)) {
        val grid = Color.White.copy(alpha = .10f)
        repeat(3) { index -> val y = size.height * (index + 1) / 4f; drawLine(grid, Offset(0f, y), Offset(size.width, y), 1.dp.toPx()) }
        if (history.size > 1) {
            val step = size.width / history.lastIndex.coerceAtLeast(1)
            val points = history.mapIndexed { index, rssi -> Offset(index * step, size.height - ((rssi + 95f) / 60f).coerceIn(0f, 1f) * (size.height - 12.dp.toPx()) - 6.dp.toPx()) }
            points.zipWithNext().forEach { (a, b) -> drawLine(color, a, b, 3.dp.toPx(), cap = StrokeCap.Round) }
            points.lastOrNull()?.let { drawCircle(color, 5.dp.toPx(), it) }
        }
    }
}

@Composable private fun WifiPill(value: String, label: String) = Surface(color = Color.White.copy(alpha = .08f), shape = RoundedCornerShape(12.dp)) {
    Column(Modifier.padding(horizontal = 9.dp, vertical = 5.dp)) { LocalizedText(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = .55f)); Text(value, style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold) }
}

@Composable
private fun WifiTabs(active: WifiTab, onSelected: (WifiTab) -> Unit) {
    val primaryTabs = listOf(WifiTab.SIGNAL, WifiTab.NETWORKS, WifiTab.TESTS, WifiTab.SURVEY)
    val selectedPrimary = when (active) {
        WifiTab.CHANNELS -> WifiTab.NETWORKS
        WifiTab.DEVICES -> WifiTab.TESTS
        WifiTab.WALK -> WifiTab.SURVEY
        else -> active
    }
    val icons = mapOf(
        WifiTab.SIGNAL to Icons.Rounded.Wifi,
        WifiTab.NETWORKS to Icons.Rounded.Refresh,
        WifiTab.TESTS to Icons.Rounded.PlayArrow,
        WifiTab.SURVEY to Icons.Rounded.Map,
    )
    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF091923)).padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        primaryTabs.forEach { tab ->
            Surface(
                modifier = Modifier.weight(1f),
                color = if (selectedPrimary == tab) WifiMint else Color.White.copy(alpha = .055f),
                contentColor = if (selectedPrimary == tab) WifiInk else Color.White.copy(alpha = .64f),
                shape = RoundedCornerShape(14.dp),
                onClick = { onSelected(tab) },
            ) {
                Column(Modifier.padding(horizontal = 3.dp, vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Icon(icons.getValue(tab), null, modifier = Modifier.size(18.dp))
                    LocalizedText(tab.label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, maxLines = 1, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun SignalPane(state: WifiAnalyzerUiState, comparedBssids: Set<String>) = Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    LocalizedText("At a glance", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Black)
    val healthAdvice = networkHealthRecommendation(state)
    Surface(color = healthAdvice.second.copy(alpha = .12f), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            LocalizedText("Network health recommendation", style = MaterialTheme.typography.labelMedium, color = healthAdvice.second, fontWeight = FontWeight.Black)
            LocalizedText(healthAdvice.first, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .78f))
        }
    }
    state.meshGroups.firstOrNull { it.currentBssid != null }?.let { mesh ->
        Surface(color = WifiBlue.copy(alpha = .09f), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    LocalizedText("Mesh & roaming", color = WifiBlue, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelLarge)
                    Text("${mesh.score}/100", color = Color.White, fontWeight = FontWeight.Black)
                }
                DetailLine("Access points", mesh.accessPoints.size.toString())
                DetailLine("Channel overlap", mesh.overlappingChannels.toString())
                if (mesh.stickyClient) LocalizedText("A stronger access point is available. This device may be sticking to a weaker node.", color = Color(0xFFFFB86B), style = MaterialTheme.typography.bodySmall)
                else LocalizedText("Current roaming choice looks healthy.", color = Color.White.copy(alpha = .7f), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        CompactMetric("Security", state.nearbyNetworks.firstOrNull { it.isCurrentConnection }?.securityLabel ?: "--", Modifier.weight(1f))
        CompactMetric("Frequency", "${state.frequencyMhz} MHz", Modifier.weight(1f))
        CompactMetric("Samples", "${state.rssiHistory.size}/30", Modifier.weight(1f))
    }
    Surface(color = Color.White.copy(alpha = .045f), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailLine("Access point", state.bssid)
            DetailLine("Channel", state.channelLabel)
            DetailLine("Link speed", "${state.linkSpeedMbps} Mbps")
        }
    }
    if (comparedBssids.isNotEmpty()) {
        LocalizedText("AP comparison", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Black)
        MultiApTrace(state.nearbyNetworks.filter { it.bssid in comparedBssids }, state.networkSignalHistories)
        LocalizedText("Tracking ${comparedBssids.size}/4 selected access points in this private session.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .62f))
    }
    LocalizedText(state.note, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .62f))
    if (state.roamingEvents.isNotEmpty()) {
        LocalizedText("Roaming events", style = MaterialTheme.typography.titleSmall, color = WifiMint, fontWeight = FontWeight.Black)
        state.roamingEvents.take(3).forEach { event -> DetailLine(java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(event.timestamp)), "${event.fromBssid.takeLast(5)} → ${event.toBssid.takeLast(5)}") }
    }
    LocalizedText("Private local Wi-Fi analysis", style = MaterialTheme.typography.labelSmall, color = WifiMint.copy(alpha = .85f))
}

private fun networkHealthRecommendation(state: WifiAnalyzerUiState): Pair<String, Color> {
    val result = state.latestDiagnostics
    return when {
        state.rssi < -75 -> "Signal is weak. Move closer to the access point, then compare the recommended channel before adding an extender." to Color(0xFFFFB86B)
        (result?.packetLossPercent ?: 0) >= 3 -> "Packet loss is affecting reliability. Re-run near the router to separate Wi-Fi interference from ISP trouble." to Color(0xFFFFB86B)
        (result?.loadedLatencyMs ?: 0) >= 180 -> "Latency rises under load. Pause heavy transfers or enable router traffic management if available." to WifiBlue
        state.nearbyNetworks.firstOrNull { it.isCurrentConnection }?.securityLabel in setOf("Open", "WEP") -> "This network uses weak or open security. Prefer WPA2/WPA3 before sending sensitive data." to Color(0xFFFFB86B)
        else -> state.recommendation to WifiMint
    }
}

@Composable
private fun MultiApTrace(networks: List<NearbyWifiNetwork>, histories: Map<String, List<Int>>) {
    val colors = listOf(WifiMint, WifiBlue, Color(0xFFFFB86B), Color(0xFFC4A7FF))
    Canvas(Modifier.fillMaxWidth().height(112.dp)) {
        drawRoundRect(Color.White.copy(alpha = .05f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()))
        networks.forEachIndexed { index, network ->
            val history = histories[network.bssid].orEmpty()
            if (history.size > 1) {
                val step = size.width / history.lastIndex.coerceAtLeast(1)
                val points = history.mapIndexed { point, rssi -> Offset(point * step, size.height - ((rssi + 95f) / 60f).coerceIn(0f, 1f) * (size.height - 14.dp.toPx()) - 7.dp.toPx()) }
                points.zipWithNext().forEach { (a, b) -> drawLine(colors[index % colors.size], a, b, 2.5.dp.toPx(), cap = StrokeCap.Round) }
            }
        }
    }
    networks.forEachIndexed { index, network -> Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(colors[index % colors.size], RoundedCornerShape(8.dp)))
        Text(network.ssid, modifier = Modifier.padding(start = 7.dp), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = .82f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    } }
}

@Composable private fun DetailLine(label: String, value: String) = Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    LocalizedText(label, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = .58f)); Spacer(Modifier.weight(1f)); LocalizedText(value, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun ChannelsPane(hasPermission: Boolean, bands: List<String>, activeBand: String, onBand: (String) -> Unit, recommendation: String, ratings: List<WifiChannelRating>, networks: List<NearbyWifiNetwork>, enable: () -> Unit) {
    if (!hasPermission) return NearbyPermissionPane(enable)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LocalizedText("Channel guide", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Black)
        LocalizedText(recommendation, style = MaterialTheme.typography.bodySmall, color = WifiMint)
        if (bands.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { bands.forEach { band -> Button(onClick = { onBand(band) }, colors = ButtonDefaults.buttonColors(containerColor = if (activeBand == band) WifiMint else Color.White.copy(alpha = .08f), contentColor = if (activeBand == band) WifiInk else Color.White)) { Text(band, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) } } }
        ChannelOverlapGraph(networks.filter { WifiInsights.bandForFrequency(it.frequencyMhz) == activeBand }, activeBand)
        ChannelGraph(ratings)
        LocalizedText("Quality uses observed signal, overlap and advertised channel width; it is guidance, not a guarantee.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .55f))
    }
}

@Composable
private fun ChannelOverlapGraph(networks: List<NearbyWifiNetwork>, band: String) {
    if (networks.isEmpty()) return
    var zoom by rememberSaveable(band) { mutableStateOf(1f) }
    var panX by remember(band) { mutableStateOf(0f) }
    val channels = networks.mapNotNull { WifiInsights.channelForFrequency(it.frequencyMhz) }
    val minChannel = if (band == "2.4 GHz") 1 else (channels.minOrNull() ?: 1)
    val maxChannel = if (band == "2.4 GHz") 14 else (channels.maxOrNull() ?: 1)
    val colors = listOf(WifiMint, WifiBlue, Color(0xFFFFB86B), Color(0xFFC4A7FF), Color(0xFFFF87B7))
    LocalizedText("Channel overlap", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Black)
    Canvas(
        Modifier.fillMaxWidth().height(176.dp)
            .pointerInput(band) {
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    zoom = (zoom * gestureZoom).coerceIn(1f, 3.5f)
                    panX = if (zoom <= 1.01f) 0f else (panX + pan.x).coerceIn(-size.width.toFloat(), size.width.toFloat())
                }
            }
            .graphicsLayer(scaleX = zoom, transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center, translationX = panX),
    ) {
        drawRoundRect(Color.White.copy(alpha = .05f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()))
        val span = (maxChannel - minChannel).coerceAtLeast(1).toFloat()
        networks.forEachIndexed { index, network ->
            val channel = WifiInsights.channelForFrequency(network.frequencyMhz) ?: return@forEachIndexed
            val centerX = ((channel - minChannel) / span) * size.width
            val radius = (network.channelWidthMhz.coerceAtLeast(20) / 20f * size.width / span * 1.6f).coerceAtLeast(24.dp.toPx())
            val peakY = size.height - ((network.rssi + 95f) / 60f).coerceIn(0f, 1f) * (size.height - 20.dp.toPx()) - 10.dp.toPx()
            val points = (0..30).map { step ->
                val x = (centerX - radius + radius * 2 * step / 30f).coerceIn(0f, size.width)
                val normalized = (x - centerX) / radius
                Offset(x, size.height - (size.height - peakY) * (1 - normalized * normalized).coerceAtLeast(0f))
            }
            points.zipWithNext().forEach { (a, b) -> drawLine(colors[index % colors.size], a, b, 2.dp.toPx(), cap = StrokeCap.Round) }
        }
    }
}

@Composable
private fun ChannelGraph(ratings: List<WifiChannelRating>) {
    if (ratings.isEmpty()) { LocalizedText("No channel ratings yet. Wait for the first local scan.", color = Color.White.copy(alpha = .64f)); return }
    Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .06f)), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { ratings.forEach { rating ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${rating.channel}", color = Color.White.copy(alpha = .75f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(28.dp))
                Box(Modifier.weight(1f).height(10.dp).background(Color.White.copy(alpha = .10f), RoundedCornerShape(12.dp))) { Box(Modifier.fillMaxWidth(rating.qualityPercent / 100f).height(10.dp).background(if (rating.qualityPercent >= 70) WifiMint else WifiBlue, RoundedCornerShape(12.dp))) }
                Text("${rating.qualityPercent}%", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp))
            }
        } }
    }
}

@Composable
private fun NetworksPane(
    hasPermission: Boolean, networks: List<NearbyWifiNetwork>, scanCount: Int, filterText: String, onFilterText: (String) -> Unit,
    minimumSignal: Int, onMinimumSignal: (Int) -> Unit, securityOnly: Boolean, onSecurityOnly: (Boolean) -> Unit,
    comparedBssids: Set<String>, onCompare: (String) -> Unit, paused: Boolean, onPauseChanged: (Boolean) -> Unit,
    context: android.content.Context, showLost: Boolean, onShowLost: (Boolean) -> Unit,
    sort: WifiNetworkSort, onSort: (WifiNetworkSort) -> Unit, enable: () -> Unit,
) {
    if (!hasPermission) return NearbyPermissionPane(enable)
    val language = LocalAppLanguage.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LocalizedText("Nearby networks", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Black); Spacer(Modifier.weight(1f)); LocalizedText("Scan $scanCount", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = .55f))
        }
        OutlinedTextField(value = filterText, onValueChange = onFilterText, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { LocalizedText("Filter SSID or BSSID") }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = WifiMint, focusedLabelColor = WifiMint, unfocusedBorderColor = Color.White.copy(alpha = .3f), unfocusedLabelColor = Color.White.copy(alpha = .55f)))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = minimumSignal == -100, onClick = { onMinimumSignal(-100) }, label = { LocalizedText("All") })
            FilterChip(selected = minimumSignal == -75, onClick = { onMinimumSignal(-75) }, label = { LocalizedText("-75 dBm+") })
            FilterChip(selected = minimumSignal == -67, onClick = { onMinimumSignal(-67) }, label = { LocalizedText("-67 dBm+") })
            FilterChip(selected = securityOnly, onClick = { onSecurityOnly(!securityOnly) }, label = { LocalizedText("Secured") })
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            item { FilterChip(selected = sort == WifiNetworkSort.SIGNAL, onClick = { onSort(WifiNetworkSort.SIGNAL) }, label = { LocalizedText("Signal") }) }
            item { FilterChip(selected = sort == WifiNetworkSort.NAME, onClick = { onSort(WifiNetworkSort.NAME) }, label = { LocalizedText("Name") }) }
            item { FilterChip(selected = sort == WifiNetworkSort.CHANNEL, onClick = { onSort(WifiNetworkSort.CHANNEL) }, label = { LocalizedText("Channel") }) }
            item { FilterChip(selected = showLost, onClick = { onShowLost(!showLost) }, label = { LocalizedText("Show lost") }) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { onPauseChanged(!paused) }) { Icon(if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); LocalizedText(if (paused) "Resume scan" else "Pause scan") }
            TextButton(onClick = { exportNetworks(context, networks, language) }) { Icon(Icons.Rounded.FileDownload, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); LocalizedText("Export CSV") }
        }
        if (networks.isEmpty()) LocalizedText("No nearby scan results yet. Keep Wi-Fi on and wait a few seconds.", color = Color.White.copy(alpha = .62f)) else networks.forEach { NearbyNetworkRow(it, it.bssid in comparedBssids, onCompare) }
    }
}

@Composable private fun NearbyNetworkRow(network: NearbyWifiNetwork, compared: Boolean, onCompare: (String) -> Unit) = Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
    Icon(Icons.Rounded.Wifi, null, tint = if (network.isActive) signalColor(network.rssi) else Color.White.copy(alpha = .28f), modifier = Modifier.size(24.dp))
    Column(Modifier.padding(start = 12.dp).weight(1f)) { if (network.ssid == "Hidden network") LocalizedText(network.ssid, color = Color.White, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) else Text(network.ssid, color = Color.White, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis); LocalizedText("${network.channelLabel} · ${network.channelWidthMhz} MHz · ${network.securityLabel}", color = Color.White.copy(alpha = .55f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis); LocalizedText("${network.wifiStandard} · ${network.estimatedDistanceMeters?.let { "~%.1f m".format(java.util.Locale.US, it) } ?: "distance unavailable"}", color = Color.White.copy(alpha = .42f), style = MaterialTheme.typography.labelSmall, maxLines = 1); LocalizedText(network.vendor, color = WifiBlue.copy(alpha = .72f), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    Column(horizontalAlignment = Alignment.End) { Text("${network.rssi} dBm", color = if (network.isActive) Color.White else Color.White.copy(alpha = .4f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold); network.estimatedSirDb?.let { Text("SIR ~%.1f dB".format(java.util.Locale.US, it), color = WifiBlue, style = MaterialTheme.typography.labelSmall) }; if (network.isCurrentConnection) LocalizedText("Current", style = MaterialTheme.typography.labelSmall, color = WifiMint) else if (!network.isActive) LocalizedText("Lost", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = .42f)); Checkbox(checked = compared, onCheckedChange = { onCompare(network.bssid) }, colors = CheckboxDefaults.colors(checkedColor = WifiMint)) }
}

private fun exportNetworks(context: android.content.Context, networks: List<NearbyWifiNetwork>, language: com.purehub.app.ui.AppLanguage) {
    val csv = buildString {
        appendLine("ssid,bssid,vendor,status,last_seen_epoch_ms,rssi_dbm,estimated_sir_db,band,channel,channel_width_mhz,wifi_standard,security")
        networks.forEach { network ->
            appendLine(
                listOf(
                    network.ssid,
                    network.bssid,
                    network.vendor,
                    if (network.isActive) "active" else "lost",
                    network.lastSeenAtMillis,
                    network.rssi,
                    network.estimatedSirDb?.let { "%.1f".format(java.util.Locale.US, it) } ?: "",
                    WifiInsights.bandForFrequency(network.frequencyMhz),
                    WifiInsights.channelForFrequency(network.frequencyMhz) ?: "",
                    network.channelWidthMhz,
                    network.wifiStandard,
                    network.securityLabel,
                ).joinToString(",") { "\"${it.toString().replace("\"", "\"\"")}\"" },
            )
        }
    }
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/csv"; putExtra(Intent.EXTRA_SUBJECT, translateUiText("PureHub Wi-Fi scan", language)); putExtra(Intent.EXTRA_TEXT, csv) }, translateUiText("Export Wi-Fi CSV", language)))
}

@Composable
private fun DiagnosticsPane(
    state: WifiAnalyzerUiState,
    onRun: () -> Unit,
    onCancel: () -> Unit,
    onProfile: (WifiSpeedProfile) -> Unit,
    onStartMonitor: (Int) -> Unit,
    onStopMonitor: () -> Unit,
    onInspectRouter: () -> Unit,
    onSpeedServer: (WifiSpeedServerMode, String) -> Unit,
    onMonitorInterval: (Int) -> Unit,
    onSaveController: (RouterControllerConfig?) -> Unit,
    onConnectController: () -> Unit,
    onScanInterval: (Int) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val language = LocalAppLanguage.current
    val result = state.latestDiagnostics
    var showSpeedServerDialog by rememberSaveable { mutableStateOf(false) }
    var customSpeedUrl by rememberSaveable(state.customSpeedBaseUrl) { mutableStateOf(state.customSpeedBaseUrl) }
    var showControllerDialog by rememberSaveable { mutableStateOf(false) }
    var controllerPlatform by rememberSaveable(state.controllerConfig?.platform) { mutableStateOf(state.controllerConfig?.platform ?: RouterControllerPlatform.UNIFI) }
    var controllerUrl by rememberSaveable(state.controllerConfig?.baseUrl) { mutableStateOf(state.controllerConfig?.baseUrl.orEmpty()) }
    var controllerAccount by rememberSaveable(state.controllerConfig?.username) { mutableStateOf(state.controllerConfig?.username.orEmpty()) }
    var controllerSecret by rememberSaveable { mutableStateOf("") }
    if (showSpeedServerDialog) AlertDialog(
        onDismissRequest = { showSpeedServerDialog = false },
        title = { LocalizedText("Private speed endpoint") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LocalizedText("Enter a self-hosted endpoint compatible with /__down?bytes= and /__up. Auto selects the lowest preflight latency.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(customSpeedUrl, { customSpeedUrl = it }, singleLine = true, label = { LocalizedText("Base URL") }, placeholder = { Text("https://speed.example.com") })
        } },
        confirmButton = { TextButton(onClick = { onSpeedServer(WifiSpeedServerMode.CUSTOM, customSpeedUrl); showSpeedServerDialog = false }) { LocalizedText("Save") } },
        dismissButton = { TextButton(onClick = { showSpeedServerDialog = false }) { LocalizedText("Cancel") } },
    )
    if (showControllerDialog) AlertDialog(
        onDismissRequest = { showControllerDialog = false },
        title = { LocalizedText("Read-only controller") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(RouterControllerPlatform.entries) { platform -> FilterChip(selected = controllerPlatform == platform, onClick = { controllerPlatform = platform }, label = { Text(platform.label) }) } }
            OutlinedTextField(controllerUrl, { controllerUrl = it }, singleLine = true, label = { LocalizedText("Local controller URL") }, placeholder = { Text("https://192.168.1.1") })
            OutlinedTextField(controllerAccount, { controllerAccount = it }, singleLine = true, label = { LocalizedText(if (controllerPlatform == RouterControllerPlatform.OMADA) "Controller ID" else "Username / account") })
            OutlinedTextField(controllerSecret, { controllerSecret = it }, singleLine = true, visualTransformation = PasswordVisualTransformation(), label = { LocalizedText(if (controllerPlatform == RouterControllerPlatform.OPENWRT) "Password" else "Local API token") })
            LocalizedText("Credentials are encrypted on this device and sent only to this controller. PureHub never changes router settings.", style = MaterialTheme.typography.labelSmall)
        } },
        confirmButton = { TextButton(onClick = {
            onSaveController(RouterControllerConfig(controllerPlatform, controllerUrl, controllerAccount, controllerSecret.ifBlank { state.controllerConfig?.secret.orEmpty() }))
            controllerSecret = ""; showControllerDialog = false
        }) { LocalizedText("Save") } },
        dismissButton = { TextButton(onClick = { showControllerDialog = false }) { LocalizedText("Cancel") } },
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LocalizedText("Network tests", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Black)
        LocalizedText("Runs only when you tap. Measures gateway, Internet reachability, DNS, loaded latency and adaptive throughput.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .62f))
        LocalizedText("The connected build contacts Cloudflare speed endpoints and public DNS resolvers only during this test. No identifier is added by PureHub.", style = MaterialTheme.typography.labelSmall, color = WifiBlue.copy(alpha = .78f))
        LocalizedText("Accuracy profile", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = .62f))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(WifiSpeedProfile.entries) { profile ->
                FilterChip(selected = state.speedProfile == profile, onClick = { onProfile(profile) }, enabled = !state.isRunningDiagnostics, label = { LocalizedText(profile.label) })
            }
        }
        LocalizedText("Speed server", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = .62f))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(WifiSpeedServerMode.entries) { mode -> FilterChip(
                selected = state.speedServerMode == mode,
                onClick = { if (mode == WifiSpeedServerMode.CUSTOM) showSpeedServerDialog = true else onSpeedServer(mode, state.customSpeedBaseUrl) },
                enabled = !state.isRunningDiagnostics,
                label = { LocalizedText(mode.label) },
            ) }
        }
        Surface(color = Color.White.copy(alpha = .045f), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                DetailLine("Speed server", when (state.speedServerMode) {
                    WifiSpeedServerMode.AUTO -> "Auto · fastest configured endpoint"
                    WifiSpeedServerMode.CLOUDFLARE -> "Cloudflare · speed.cloudflare.com"
                    WifiSpeedServerMode.CUSTOM -> state.customSpeedBaseUrl.ifBlank { "Private endpoint not configured" }
                })
                DetailLine("Test mode", "Download · Upload · Loaded latency")
                DetailLine("History", "14 runs · stored only on this device")
            }
        }
        Button(onClick = { if (state.isRunningDiagnostics) onCancel() else onRun() }, enabled = BuildConfig.INTERNET_ENABLED, colors = ButtonDefaults.buttonColors(containerColor = if (state.isRunningDiagnostics) Color(0xFFFFB86B) else WifiMint, contentColor = WifiInk), modifier = Modifier.fillMaxWidth()) {
            if (state.isRunningDiagnostics) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = WifiInk) else Icon(Icons.Rounded.PlayArrow, null)
            Spacer(Modifier.width(6.dp)); LocalizedText(if (state.isRunningDiagnostics) "Cancel test" else "Run network test", fontWeight = FontWeight.Black)
        }
        if (!BuildConfig.INTERNET_ENABLED) LocalizedText("Connected speed tests are unavailable in the offline F-Droid build.", color = WifiBlue, style = MaterialTheme.typography.bodySmall)
        if (state.isRunningDiagnostics) {
            LinearProgressIndicator(progress = { state.speedProgress.percent / 100f }, modifier = Modifier.fillMaxWidth(), color = WifiMint)
            DetailLine(state.speedProgress.phase, state.speedProgress.liveMbps?.let { "%.1f Mbps".format(java.util.Locale.US, it) } ?: "Preparing")
        }
        if (result != null) {
            val diagnosis = when {
                (result.packetLossPercent ?: 0) > 0 -> "Connection loss was observed. Check router distance or ISP stability."
                (result.dnsMs ?: 0) > 180 -> "DNS response is slow. Try another resolver in Phase 2 settings."
                (result.gatewayMs ?: 0) > 80 -> "Local gateway latency is high. Check Wi-Fi interference or router load."
                else -> "Connection path looks healthy from this device."
            }
            LocalizedText("Latest result", style = MaterialTheme.typography.titleSmall, color = WifiMint, fontWeight = FontWeight.Black)
            DetailLine("Speed server", result.speedServer)
            DetailLine("Profile / data", "${result.testProfile} · ${"%.1f".format(java.util.Locale.US, result.transferredBytes / 1_048_576.0)} MB · ${result.durationMs / 1000}s")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                DiagnosticMetric("Gateway", result.gatewayMs?.let { "$it ms" } ?: "--", Modifier.weight(1f))
                DiagnosticMetric("Internet", result.internetMs?.let { "$it ms" } ?: "--", Modifier.weight(1f))
                DiagnosticMetric("Jitter", result.jitterMs?.let { "$it ms" } ?: "--", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                DiagnosticMetric("Loss", result.packetLossPercent?.let { "$it%" } ?: "--", Modifier.weight(1f))
                DiagnosticMetric("DNS", result.dnsMs?.let { "${result.dnsProvider ?: ""} $it ms" } ?: "--", Modifier.weight(1f))
                DiagnosticMetric("Down", result.downloadMbps?.let { "%.1f Mbps".format(java.util.Locale.US, it) } ?: "--", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                DiagnosticMetric("Upload", result.uploadMbps?.let { "%.1f Mbps".format(java.util.Locale.US, it) } ?: "--", Modifier.weight(1f))
                DiagnosticMetric("Loaded latency", result.loadedLatencyMs?.let { "$it ms" } ?: "--", Modifier.weight(1f))
            }
            if (result.downloadSamplesMbps.size > 1) {
                LocalizedText("Download speed curve", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = .55f))
                SpeedTrace(result.downloadSamplesMbps)
            }
            LocalizedText(diagnosis, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .72f))
        }
        if (state.diagnosticHistory.isNotEmpty()) {
            LocalizedText("Local history · ${state.diagnosticHistory.size}/14", style = MaterialTheme.typography.labelMedium, color = WifiMint)
            state.diagnosticHistory.take(8).forEach { item -> DetailLine(
                java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT, language.locale()).format(java.util.Date(item.timestamp)),
                "↓ ${item.downloadMbps?.let { "%.1f".format(java.util.Locale.US, it) } ?: "--"} · ↑ ${item.uploadMbps?.let { "%.1f".format(java.util.Locale.US, it) } ?: "--"} Mbps",
            ) }
        }
        HorizontalDivider(color = Color.White.copy(alpha = .10f))
        LocalizedText("Connection monitor", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Black)
        LocalizedText("Foreground session survives screen-off and watches outages, roaming and latency spikes with a visible notification.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .62f))
        if (!state.isMonitoring) LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(listOf(10, 30, 60)) { seconds -> FilterChip(selected = state.monitorIntervalSeconds == seconds, onClick = { onMonitorInterval(seconds) }, label = { LocalizedText("${seconds}s sample") }) }
        }
        if (state.isMonitoring) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CompactMetric("Samples", state.monitorSamples.size.toString(), Modifier.weight(1f))
                CompactMetric("Events", state.monitorEvents.size.toString(), Modifier.weight(1f))
                CompactMetric("Uptime", if (state.monitorSamples.isEmpty()) "--" else "${state.monitorSamples.count(WifiMonitorSample::internetReachable) * 100 / state.monitorSamples.size}%", Modifier.weight(1f))
            }
            Button(onClick = onStopMonitor, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB86B), contentColor = WifiInk), modifier = Modifier.fillMaxWidth()) { LocalizedText("Stop and save report", fontWeight = FontWeight.Black) }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(15, 30, 60).forEach { minutes -> OutlinedButton(onClick = { onStartMonitor(minutes) }, enabled = BuildConfig.INTERNET_ENABLED, modifier = Modifier.weight(1f)) { Text("$minutes min") } }
            }
        }
        state.monitorReports.firstOrNull()?.let { report ->
            DetailLine("Last monitor", "${report.uptimePercent}% uptime · ${report.averageLatencyMs ?: 0} ms · ${report.events.size} events")
            TextButton(onClick = { exportMonitorReport(context, report) }) { LocalizedText("Export incident timeline") }
        }
        HorizontalDivider(color = Color.White.copy(alpha = .10f))
        LocalizedText("Local router", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Black)
        LocalizedText("Read-only gateway fingerprint. PureHub does not log in, change settings or upload router details.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .62f))
        OutlinedButton(onClick = onInspectRouter, enabled = !state.isInspectingRouter && state.gatewayAddress != null, modifier = Modifier.fillMaxWidth()) {
            if (state.isInspectingRouter) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Rounded.Refresh, null)
            Spacer(Modifier.width(6.dp)); LocalizedText(if (state.isInspectingRouter) "Inspecting gateway" else "Detect local router")
        }
        state.routerInsight?.let { insight ->
            Surface(color = WifiMint.copy(alpha = .09f), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DetailLine("Platform", insight.platform); DetailLine("Gateway", insight.gateway); DetailLine("Response", "${insight.responseMs} ms · read-only")
                    TextButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(insight.managementUrl))) } }) { LocalizedText("Open router console") }
                }
            }
        }
        HorizontalDivider(color = Color.White.copy(alpha = .10f))
        LocalizedText("Controller connector", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Black)
        LocalizedText("Optional local, read-only connector for UniFi Network, Omada Open API or OpenWrt ubus.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .62f))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { showControllerDialog = true }, enabled = BuildConfig.INTERNET_ENABLED, modifier = Modifier.weight(1f)) { LocalizedText(if (state.controllerConfig == null) "Configure" else "Edit") }
            Button(onClick = onConnectController, enabled = BuildConfig.INTERNET_ENABLED && state.controllerConfig != null && !state.isConnectingController, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = WifiMint, contentColor = WifiInk)) {
                LocalizedText(if (state.isConnectingController) "Connecting" else "Test read-only")
            }
        }
        state.controllerStatus?.let { status ->
            DetailLine(status.platform.label, status.summary)
            status.accessPointCount?.let { DetailLine("Access points", it.toString()) }
            status.clientCount?.let { DetailLine("Clients", it.toString()) }
            status.channelUtilizationPercent?.let { DetailLine("Channel utilization", "$it%") }
            status.wanOnline?.let { DetailLine("WAN status", if (it) "Online" else "Offline") }
        }
        LocalizedText("Scan refresh", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = .62f))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(listOf(15, 35, 60)) { seconds -> FilterChip(selected = state.scanIntervalSeconds == seconds, onClick = { onScanInterval(seconds) }, label = { Text("${seconds}s") }) }
        }
    }
}

@Composable
private fun DevicesPane(
    state: WifiAnalyzerUiState,
    onDiscover: () -> Unit,
    onScanPorts: (String, PortScanProfile) -> Unit,
    onCustomScan: (String, String) -> Unit,
    onWake: (String) -> Unit,
    onToggleTrusted: (String) -> Unit,
    onDismissMessage: () -> Unit,
) {
    var customHost by rememberSaveable { mutableStateOf<String?>(null) }
    var customPorts by rememberSaveable { mutableStateOf("22,53,80,443,445,554,8080,8443") }
    var wakeHost by rememberSaveable { mutableStateOf<String?>(null) }
    var wakeMac by rememberSaveable { mutableStateOf("") }

    customHost?.let { host ->
        AlertDialog(
            onDismissRequest = { customHost = null },
            title = { LocalizedText("Custom TCP scan") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LocalizedText("Scan $host. Use commas and bounded ranges; up to 256 ports per run.")
                OutlinedTextField(customPorts, { customPorts = it }, singleLine = true, label = { LocalizedText("Ports") }, placeholder = { Text("22,80,443,8000-8010") })
            } },
            confirmButton = { TextButton(onClick = { onCustomScan(host, customPorts); customHost = null }) { LocalizedText("Scan") } },
            dismissButton = { TextButton(onClick = { customHost = null }) { LocalizedText("Cancel") } },
        )
    }
    wakeHost?.let { host ->
        AlertDialog(
            onDismissRequest = { wakeHost = null },
            title = { LocalizedText("Wake device") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LocalizedText("Send one local Wake-on-LAN magic packet for $host. The target must support WoL.")
                OutlinedTextField(wakeMac, { wakeMac = it }, singleLine = true, label = { LocalizedText("MAC address") }, placeholder = { Text("AA:BB:CC:DD:EE:FF") })
            } },
            confirmButton = { TextButton(onClick = { onWake(wakeMac); wakeHost = null }) { LocalizedText("Wake") } },
            dismissButton = { TextButton(onClick = { wakeHost = null }) { LocalizedText("Cancel") } },
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LocalizedText("LAN devices", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Black)
        LocalizedText("Scans only the current private /24 network after you tap the button. No cloud inventory or background monitoring.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .62f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            CompactMetric("Devices", state.lanDevices.size.toString(), Modifier.weight(1f))
            CompactMetric("New", state.lanDevices.count(LanDevice::isNew).toString(), Modifier.weight(1f))
            CompactMetric("Trusted", state.trustedLanIps.size.toString(), Modifier.weight(1f))
        }
        DetailLine("Local IP", state.localIpAddress ?: "--"); DetailLine("Gateway", state.gatewayAddress ?: "--"); DetailLine("Access point", state.accessPointVendor)
        Button(onClick = onDiscover, enabled = !state.isDiscoveringDevices && state.localIpAddress != null, colors = ButtonDefaults.buttonColors(containerColor = WifiMint, contentColor = WifiInk), modifier = Modifier.fillMaxWidth()) {
            if (state.isDiscoveringDevices) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = WifiInk) else Icon(Icons.Rounded.Refresh, null)
            Spacer(Modifier.width(6.dp)); LocalizedText(if (state.isDiscoveringDevices) "Discovering" else "Discover devices", fontWeight = FontWeight.Black)
        }
        state.lanActionMessage?.let { message ->
            Surface(color = WifiMint.copy(alpha = .10f), shape = RoundedCornerShape(12.dp), onClick = onDismissMessage) {
                LocalizedText(message, modifier = Modifier.padding(10.dp), color = WifiMint, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (state.lanDevices.isEmpty()) LocalizedText("No local scan results yet.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .60f))
        state.lanDevices.forEach { device ->
            Surface(color = Color.White.copy(alpha = .05f), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.NetworkWifi, null, tint = if (device.isNew) WifiMint else WifiBlue, modifier = Modifier.size(20.dp))
                        Column(Modifier.padding(start = 10.dp).weight(1f)) {
                            Text(device.hostName, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            LocalizedText("${device.identityLabel} · ${device.identityConfidence}% confidence", color = WifiMint, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            Text(listOfNotNull(device.ipAddress, device.vendor?.takeUnless { it.startsWith("Vendor") }).joinToString(" · "), color = Color.White.copy(alpha = .55f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            device.macAddress?.let { Text(it, color = Color.White.copy(alpha = .38f), style = MaterialTheme.typography.labelSmall) }
                            Text(device.discoverySources.joinToString(" · "), color = WifiMint.copy(alpha = .72f), style = MaterialTheme.typography.labelSmall)
                            if (device.services.isNotEmpty()) Text(device.services.take(3).joinToString(" · "), color = Color.White.copy(alpha = .42f), style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (device.ipv6Addresses.isNotEmpty()) Text(device.ipv6Addresses.first(), color = Color.White.copy(alpha = .38f), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Column(horizontalAlignment = Alignment.End) { Text(device.latencyMs?.let { "$it ms" } ?: "--", color = Color.White, style = MaterialTheme.typography.labelLarge); if (device.isNew) LocalizedText("New", style = MaterialTheme.typography.labelSmall, color = WifiMint) }
                    }
                    val ports = state.openPortsByHost[device.ipAddress]
                    if (ports != null) DetailLine("Identity evidence", device.identityEvidence.take(3).joinToString(" · ").ifBlank { inferLanDeviceType(ports) })
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        MiniAction(if (state.scanningPortsFor == device.ipAddress) "Scanning…" else "Quick", Modifier.weight(1f), state.scanningPortsFor == null) { onScanPorts(device.ipAddress, PortScanProfile.QUICK) }
                        MiniAction("Extended ports", Modifier.weight(1f), state.scanningPortsFor == null) { onScanPorts(device.ipAddress, PortScanProfile.EXTENDED) }
                        MiniAction("Custom", Modifier.weight(1f), state.scanningPortsFor == null) { customHost = device.ipAddress }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        MiniAction("Wake", Modifier.weight(1f), true) { wakeHost = device.ipAddress; wakeMac = device.macAddress.orEmpty() }
                        MiniAction(if (device.ipAddress in state.trustedLanIps) "Trusted" else "Trust", Modifier.weight(1f), true) { onToggleTrusted(device.ipAddress) }
                    }
                    if (ports != null) LocalizedText(
                        if (ports.isEmpty()) "No common open ports found" else ports.joinToString(" · ") { "${it.port} ${it.service}${it.banner?.let { banner -> " ($banner)" } ?: ""}" },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (ports.isEmpty()) Color.White.copy(alpha = .55f) else WifiMint,
                    )
                }
            }
        }
    }
}

private fun inferLanDeviceType(ports: List<OpenLanPort>): String = when {
    ports.any { it.port in setOf(515, 631, 9100) } -> "Printer"
    ports.any { it.port == 554 } -> "Camera / media device"
    ports.any { it.port in setOf(445, 139) } -> "Computer / NAS"
    ports.any { it.port in setOf(53, 67) } -> "Router / network service"
    ports.any { it.port in setOf(1883, 8883) } -> "IoT / MQTT device"
    ports.any { it.port in setOf(80, 443, 8000, 8008, 8080, 8443) } -> "Web-managed device"
    ports.isEmpty() -> "Unknown"
    else -> "Network device"
}

@Composable
private fun SurveyPane(
    state: WifiAnalyzerUiState,
    activeProject: WifiSurveyProject?,
    choosePlan: () -> Unit,
    createProject: (String, String) -> Unit,
    selectProject: (String) -> Unit,
    deleteProject: () -> Unit,
    calibrate: (Float, Float, Float, Float, Float) -> Unit,
    setCaptureMode: (WifiSurveyCaptureMode) -> Unit,
    addPoint: (Float, Float) -> Unit,
    undo: () -> Unit,
    clear: () -> Unit,
    context: android.content.Context,
) {
    val language = LocalAppLanguage.current
    var selectedSurveyBssid by rememberSaveable { mutableStateOf<String?>(null) }
    var surveyMetric by rememberSaveable { mutableStateOf(WifiSurveyMetric.SIGNAL) }
    var baselineProjectId by rememberSaveable { mutableStateOf<String?>(null) }
    var showNewProject by rememberSaveable { mutableStateOf(false) }
    var projectName by rememberSaveable { mutableStateOf("") }
    var floorName by rememberSaveable { mutableStateOf("") }
    var calibrationMode by rememberSaveable { mutableStateOf(false) }
    var calibrationPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var calibrationDistance by rememberSaveable { mutableStateOf("") }
    var askCalibrationDistance by rememberSaveable { mutableStateOf(false) }

    if (showNewProject) AlertDialog(
        onDismissRequest = { showNewProject = false },
        title = { LocalizedText("New survey project") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(projectName, { projectName = it }, label = { LocalizedText("Project name") }, singleLine = true)
            OutlinedTextField(floorName, { floorName = it }, label = { LocalizedText("Floor / area") }, singleLine = true)
        } },
        confirmButton = { TextButton(onClick = { createProject(projectName, floorName); projectName = ""; floorName = ""; showNewProject = false }) { LocalizedText("Create") } },
        dismissButton = { TextButton(onClick = { showNewProject = false }) { LocalizedText("Cancel") } },
    )
    if (askCalibrationDistance && calibrationPoints.size == 2) AlertDialog(
        onDismissRequest = { askCalibrationDistance = false; calibrationMode = false; calibrationPoints = emptyList() },
        title = { LocalizedText("Calibrate real distance") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LocalizedText("Enter the measured distance between the two selected points.")
            OutlinedTextField(calibrationDistance, { calibrationDistance = it.filter { char -> char.isDigit() || char == '.' || char == ',' } }, label = { LocalizedText("Distance (meters)") }, singleLine = true)
        } },
        confirmButton = { TextButton(onClick = {
            calibrationDistance.replace(',', '.').toFloatOrNull()?.takeIf { it > 0f }?.let { meters ->
                calibrate(calibrationPoints[0].x, calibrationPoints[0].y, calibrationPoints[1].x, calibrationPoints[1].y, meters)
            }
            askCalibrationDistance = false; calibrationMode = false; calibrationPoints = emptyList(); calibrationDistance = ""
        }) { LocalizedText("Save scale") } },
        dismissButton = { TextButton(onClick = { askCalibrationDistance = false; calibrationMode = false; calibrationPoints = emptyList() }) { LocalizedText("Cancel") } },
    )
    val surveyedAccessPoints = state.surveyPoints.flatMap(WifiSurveyPoint::readings)
        .distinctBy(WifiSurveyReading::bssid)
        .sortedBy(WifiSurveyReading::ssid)
    val displayPoints = state.surveyPoints.mapNotNull { point ->
        val accessPointPoint = if (selectedSurveyBssid == null) point else point.readings.firstOrNull { it.bssid == selectedSurveyBssid }?.let { point.copy(rssi = it.rssi, ssid = it.ssid) }
        accessPointPoint?.let { measured -> WifiPremiumInsights.surveyValue(measured, surveyMetric)?.let { value -> measured.copy(rssi = surveyMetricAsSignal(value, surveyMetric)) } }
    }
    val image = remember(activeProject?.planUri) {
        activeProject?.planUri?.let { value ->
            runCatching {
                context.contentResolver.openInputStream(android.net.Uri.parse(value))?.use(BitmapFactory::decodeStream)?.asImageBitmap()
            }.getOrNull()
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LocalizedText("Coverage survey", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Black)
        LocalizedText("Manage separate projects and floors. Measurements, scale and reports stay on this device.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .62f))
        LocalizedText("Point capture", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = .62f))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(WifiSurveyCaptureMode.entries) { mode -> FilterChip(
                selected = state.surveyCaptureMode == mode,
                onClick = { setCaptureMode(mode) },
                enabled = !state.isCapturingSurveyPoint && (mode != WifiSurveyCaptureMode.FULL_TEST || BuildConfig.INTERNET_ENABLED),
                label = { LocalizedText(mode.label) },
            ) }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(state.surveyProjects) { project ->
                FilterChip(selected = project.id == state.activeSurveyProjectId, onClick = { selectProject(project.id) }, label = { LocalizedText("${project.name} · ${project.floor}", maxLines = 1) })
            }
            item { AssistChip(onClick = { showNewProject = true }, label = { LocalizedText("+ Project") }) }
        }
        activeProject?.let { project ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CompactMetric("Project", project.name, Modifier.weight(1f)); CompactMetric("Floor", project.floor, Modifier.weight(1f))
            }
            project.calibration?.metersPerNormalizedUnit?.let { DetailLine("Map calibration", "%.2f m / plan unit".format(java.util.Locale.US, it)) }
        }
        LocalizedText("Measured layer", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = .62f))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(WifiSurveyMetric.entries) { metric -> FilterChip(selected = surveyMetric == metric, onClick = { surveyMetric = metric }, label = { LocalizedText(metric.label) }) }
        }
        if (state.surveyProjects.size > 1) {
            LocalizedText("Compare with baseline", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = .62f))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                item { FilterChip(selected = baselineProjectId == null, onClick = { baselineProjectId = null }, label = { LocalizedText("None") }) }
                items(state.surveyProjects.filterNot { it.id == state.activeSurveyProjectId }) { project -> FilterChip(
                    selected = baselineProjectId == project.id,
                    onClick = { baselineProjectId = project.id },
                    label = { LocalizedText("${project.name} · ${project.floor}", maxLines = 1) },
                ) }
            }
            val baseline = state.surveyProjects.firstOrNull { it.id == baselineProjectId }
            val currentAverage = activeProject?.points?.mapNotNull { WifiPremiumInsights.surveyValue(it, surveyMetric) }?.takeIf(List<Double>::isNotEmpty)?.average()
            val baselineAverage = baseline?.points?.mapNotNull { WifiPremiumInsights.surveyValue(it, surveyMetric) }?.takeIf(List<Double>::isNotEmpty)?.average()
            if (currentAverage != null && baselineAverage != null) DetailLine("Average change", "%+.1f".format(java.util.Locale.US, currentAverage - baselineAverage))
        }
        if (surveyedAccessPoints.isNotEmpty()) {
            LocalizedText("Heatmap layer", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = .62f))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                item { FilterChip(selected = selectedSurveyBssid == null, onClick = { selectedSurveyBssid = null }, label = { LocalizedText("Connected") }) }
                items(surveyedAccessPoints) { accessPoint ->
                    FilterChip(
                        selected = selectedSurveyBssid == accessPoint.bssid,
                        onClick = { selectedSurveyBssid = accessPoint.bssid },
                        label = { Text(accessPoint.ssid, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = choosePlan, colors = ButtonDefaults.buttonColors(containerColor = WifiMint, contentColor = WifiInk)) { LocalizedText(if (image == null) "Choose floor plan" else "Change floor plan", fontWeight = FontWeight.Black) }
            TextButton(onClick = undo, enabled = state.surveyPoints.isNotEmpty()) { LocalizedText("Undo point") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { calibrationMode = !calibrationMode; calibrationPoints = emptyList() }, enabled = image != null) { LocalizedText(if (calibrationMode) "Cancel calibration" else "Calibrate scale") }
            TextButton(onClick = deleteProject, enabled = state.surveyProjects.size > 1) { LocalizedText("Delete project") }
        }
        if (calibrationMode) LocalizedText("Tap two known points on the plan, then enter their real distance.", color = WifiBlue, style = MaterialTheme.typography.bodySmall)
        Surface(color = Color.White.copy(alpha = .05f), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().height(420.dp)) {
                Canvas(
                Modifier.fillMaxSize().pointerInput(image, state.rssi, state.isCapturingSurveyPoint, calibrationMode) {
                    detectTapGestures { offset ->
                        val normalized = Offset(offset.x / size.width, offset.y / size.height)
                        if (calibrationMode) {
                            calibrationPoints = (calibrationPoints + normalized).takeLast(2)
                            if (calibrationPoints.size == 2) askCalibrationDistance = true
                        } else if (!state.isCapturingSurveyPoint) addPoint(normalized.x, normalized.y)
                    }
                },
            ) {
                if (image != null) drawImage(image, dstSize = IntSize(size.width.toInt(), size.height.toInt()), alpha = .76f)
                else {
                    drawRoundRect(Color.White.copy(alpha = .03f))
                    repeat(8) { row -> drawLine(Color.White.copy(alpha = .07f), Offset(0f, size.height * row / 8f), Offset(size.width, size.height * row / 8f), 1f) }
                    repeat(6) { col -> drawLine(Color.White.copy(alpha = .07f), Offset(size.width * col / 6f, 0f), Offset(size.width * col / 6f, size.height), 1f) }
                }
                if (displayPoints.size >= 3) {
                    val cols = 24; val rows = 32; val cellW = size.width / cols; val cellH = size.height / rows
                    repeat(rows) { row -> repeat(cols) { col ->
                        val x = (col + .5f) / cols; val y = (row + .5f) / rows
                        var weighted = 0.0; var weights = 0.0
                        displayPoints.forEach { point ->
                            val distanceSquared = ((x - point.x) * (x - point.x) + (y - point.y) * (y - point.y)).toDouble().coerceAtLeast(.0008)
                            val weight = 1.0 / distanceSquared; weighted += point.rssi * weight; weights += weight
                        }
                        drawRect(signalColor((weighted / weights).toInt()).copy(alpha = .20f), Offset(col * cellW, row * cellH), Size(cellW + 1f, cellH + 1f))
                    } }
                }
                displayPoints.forEach { point ->
                    val center = Offset(point.x * size.width, point.y * size.height)
                    val color = signalColor(point.rssi)
                    drawCircle(color.copy(alpha = .22f), 34.dp.toPx(), center)
                    drawCircle(color, 9.dp.toPx(), center)
                    drawCircle(WifiInk, 4.dp.toPx(), center)
                }
                calibrationPoints.forEach { point -> drawCircle(WifiBlue, 11.dp.toPx(), Offset(point.x * size.width, point.y * size.height)) }
                if (calibrationPoints.size == 2) drawLine(WifiBlue, Offset(calibrationPoints[0].x * size.width, calibrationPoints[0].y * size.height), Offset(calibrationPoints[1].x * size.width, calibrationPoints[1].y * size.height), 3.dp.toPx())
                }
                if (image == null && state.surveyPoints.isEmpty()) {
                    Column(Modifier.align(Alignment.Center).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Map, null, tint = WifiMint, modifier = Modifier.size(38.dp))
                        LocalizedText("Add a floor plan to start mapping", color = Color.White, fontWeight = FontWeight.Black, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        LocalizedText("Then tap your real position to capture a stable signal sample.", color = Color.White.copy(alpha = .55f), style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        }
        DetailLine("Current signal", "${state.rssi} dBm")
        DetailLine("Survey points", state.surveyPoints.size.toString())
        state.surveyPoints.lastOrNull()?.let { point ->
            DetailLine("Last point diagnostics", listOfNotNull(point.latencyMs?.let { "$it ms" }, point.packetLossPercent?.let { "$it% loss" }, point.downloadMbps?.let { "%.1f Mbps down".format(java.util.Locale.US, it) }).joinToString(" · ").ifBlank { "Run a network test before sampling to attach metrics" })
        }
        if (state.isCapturingSurveyPoint) LinearProgressIndicator(Modifier.fillMaxWidth(), color = WifiMint)
        if (state.isCapturingSurveyPoint) LocalizedText("Sampling signal… stay at this position.", color = WifiMint, style = MaterialTheme.typography.bodySmall)
        activeProject?.let { project ->
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { exportSurvey(context, state.surveyPoints) }, enabled = state.surveyPoints.isNotEmpty(), modifier = Modifier.weight(1f)) { LocalizedText("CSV") }
                OutlinedButton(onClick = { WifiSurveyExporter.exportAndShare(context, project, WifiSurveyExporter.Format.PNG, language) }, enabled = state.surveyPoints.isNotEmpty(), modifier = Modifier.weight(1f)) { LocalizedText("PNG") }
                Button(onClick = { WifiSurveyExporter.exportAndShare(context, project, WifiSurveyExporter.Format.PDF, language) }, enabled = state.surveyPoints.isNotEmpty(), modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = WifiMint, contentColor = WifiInk)) { LocalizedText("PDF") }
            }
        }
        TextButton(onClick = clear, enabled = state.surveyPoints.isNotEmpty()) { LocalizedText("Clear active floor") }
        LocalizedText("The background is an IDW estimate between measured points. It is coverage guidance, not an RF propagation prediction.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .55f))
    }
}

private fun exportMonitorReport(context: android.content.Context, report: WifiMonitorReport) {
    val csv = buildString {
        appendLine("record,timestamp,rssi_dbm,bssid,gateway_reachable,internet_reachable,latency_ms,event_type,event_detail")
        report.samples.forEach { sample -> appendLine("sample,${sample.timestamp},${sample.rssi},\"${sample.bssid}\",${sample.gatewayReachable},${sample.internetReachable},${sample.latencyMs ?: ""},,") }
        report.events.forEach { event -> appendLine("event,${event.timestamp},,,,,,\"${event.type.replace("\"", "\"\"")}\",\"${event.detail.replace("\"", "\"\"")}\"") }
    }
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"; putExtra(Intent.EXTRA_SUBJECT, "PureHub network incident timeline"); putExtra(Intent.EXTRA_TEXT, csv)
    }, "Export incident timeline"))
}

private fun exportSurvey(context: android.content.Context, points: List<WifiSurveyPoint>) {
    val csv = buildString {
        appendLine("timestamp,x_percent,y_percent,connected_ssid,connected_rssi_dbm,latency_ms,packet_loss_percent,download_mbps,upload_mbps,observed_access_points,ap_bssid,ap_ssid,ap_rssi_dbm,estimated_sir_db")
        points.forEach { point ->
            val rows = point.readings.ifEmpty { listOf(WifiSurveyReading("", "", point.rssi, null)) }
            rows.forEach { reading -> appendLine(
                listOf(
                    point.timestamp,
                    point.x * 100,
                    point.y * 100,
                    point.ssid,
                    point.rssi,
                    point.latencyMs ?: "",
                    point.packetLossPercent ?: "",
                    point.downloadMbps ?: "",
                    point.uploadMbps ?: "",
                    point.observedAccessPoints,
                    reading.bssid,
                    reading.ssid,
                    reading.rssi,
                    reading.estimatedSirDb?.let { "%.1f".format(java.util.Locale.US, it) } ?: "",
                ).joinToString(",") { "\"${it.toString().replace("\"", "\"\"")}\"" },
            ) }
        }
    }
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"; putExtra(Intent.EXTRA_SUBJECT, "PureHub Wi-Fi coverage survey"); putExtra(Intent.EXTRA_TEXT, csv)
    }, "Export coverage survey"))
}

@Composable
private fun CompactMetric(label: String, value: String, modifier: Modifier = Modifier) = Surface(
    modifier = modifier,
    color = Color.White.copy(alpha = .055f),
    shape = RoundedCornerShape(14.dp),
) {
    Column(Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
        LocalizedText(value, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        LocalizedText(label, color = Color.White.copy(alpha = .5f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
private fun MiniAction(label: String, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) = OutlinedButton(
    onClick = onClick,
    modifier = modifier.height(38.dp),
    enabled = enabled,
    contentPadding = PaddingValues(horizontal = 5.dp),
) { LocalizedText(label, style = MaterialTheme.typography.labelSmall, maxLines = 1) }

@Composable
private fun WalkTestPane(state: WifiAnalyzerUiState, onActive: (Boolean) -> Unit) {
    val samples = state.walkSamples
    val average = samples.takeIf { it.isNotEmpty() }?.average()?.toInt()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LocalizedText("Walk test", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Black)
        LocalizedText("Walk through your home for 1–3 minutes. PureHub records signal samples locally so you can find weak zones and roaming changes.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .62f))
        Button(onClick = { onActive(!state.isWalkTestActive) }, colors = ButtonDefaults.buttonColors(containerColor = if (state.isWalkTestActive) Color(0xFFFFB86B) else WifiMint, contentColor = WifiInk), modifier = Modifier.fillMaxWidth()) {
            Icon(if (state.isWalkTestActive) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(6.dp)); LocalizedText(if (state.isWalkTestActive) "Stop walk test" else "Start walk test", fontWeight = FontWeight.Black)
        }
        if (samples.isNotEmpty()) {
            DetailLine("Samples", samples.size.toString()); DetailLine("Average", "$average dBm"); DetailLine("Weakest", "${samples.minOrNull()} dBm"); DetailLine("Strongest", "${samples.maxOrNull()} dBm")
            SignalTrace(samples, if ((average ?: -100) >= -67) WifiMint else Color(0xFFFFB86B))
            LocalizedText(if ((samples.minOrNull() ?: -100) < -75) "Weak coverage was observed. Consider moving closer to an access point or checking channel congestion." else "No weak zone was observed in this walk so far.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .72f))
        }
    }
}

@Composable
private fun DiagnosticMetric(label: String, value: String, modifier: Modifier) = Surface(modifier = modifier, color = Color.White.copy(alpha = .06f), shape = RoundedCornerShape(12.dp)) {
    Column(Modifier.padding(10.dp)) { LocalizedText(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = .55f)); Text(value, color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
}

@Composable
private fun SpeedTrace(samples: List<Double>) = Canvas(Modifier.fillMaxWidth().height(58.dp)) {
    if (samples.size < 2) return@Canvas
    val maximum = samples.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    val step = size.width / samples.lastIndex
    val points = samples.mapIndexed { index, value -> Offset(index * step, size.height - (value / maximum).toFloat() * (size.height - 8.dp.toPx()) - 4.dp.toPx()) }
    points.zipWithNext().forEach { (from, to) -> drawLine(WifiMint, from, to, 3.dp.toPx(), cap = StrokeCap.Round) }
}

@Composable private fun NearbyPermissionPane(enable: () -> Unit) = Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Icon(Icons.Rounded.Refresh, null, tint = WifiMint, modifier = Modifier.size(28.dp)); LocalizedText("Unlock nearby Wi-Fi", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Black); LocalizedText("Android requires Nearby Wi-Fi and Location permission to show local channel and network information.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .65f)); Button(onClick = enable, colors = ButtonDefaults.buttonColors(containerColor = WifiMint, contentColor = WifiInk)) { LocalizedText("Enable Nearby Scan", fontWeight = FontWeight.Black) }
}

private fun signalQuality(rssi: Int): String = when { rssi >= -55 -> "Excellent"; rssi >= -67 -> "Good"; rssi >= -75 -> "Fair"; else -> "Weak" }
private fun surveyMetricAsSignal(value: Double, metric: WifiSurveyMetric): Int = when (metric) {
    WifiSurveyMetric.SIGNAL -> value.toInt()
    WifiSurveyMetric.SIR -> (-85 + value.coerceIn(0.0, 35.0)).toInt()
    WifiSurveyMetric.DOWNLOAD, WifiSurveyMetric.UPLOAD -> (-90 + value.coerceIn(0.0, 200.0) / 5.0).toInt()
    WifiSurveyMetric.LATENCY -> (-45 - value.coerceIn(0.0, 250.0) / 5.0).toInt()
    WifiSurveyMetric.PACKET_LOSS -> (-50 - value.coerceIn(0.0, 50.0)).toInt()
}
private fun signalColor(rssi: Int): Color = when { rssi >= -67 -> WifiMint; rssi >= -75 -> WifiBlue; else -> Color(0xFFFFB86B) }
private fun checkWifiScanPermission(context: android.content.Context): Boolean { val location = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED; val nearby = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED else true; return location && nearby }
private fun wifiScanPermissions(): Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES) else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
