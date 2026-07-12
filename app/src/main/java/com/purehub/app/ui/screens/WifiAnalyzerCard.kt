package com.purehub.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.purehub.app.feature.wifi.WifiAnalyzerViewModel
import com.purehub.app.ui.LocalSnackbarHostState
import kotlinx.coroutines.launch

@Composable
fun WifiAnalyzerCard(
    viewModel: WifiAnalyzerViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colorScheme = MaterialTheme.colorScheme
    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    var hasPermission by remember { mutableStateOf(checkWifiScanPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        hasPermission = result.values.all { it }
        scope.launch {
            snackbarHostState.showSnackbar(
                if (hasPermission) {
                    "Nearby Wi-Fi permissions granted for local analyzer scan."
                } else {
                    "Wi-Fi analyzer stays limited to current connection details without nearby scan permission."
                },
            )
        }
    }

    DisposableEffect(Unit) {
        viewModel.start()
        onDispose { viewModel.stop() }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "WiFi Analyzer",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = uiState.note,
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
            )
            if (!hasPermission) {
                Button(
                    onClick = { permissionLauncher.launch(wifiScanPermissions()) },
                ) {
                    Text("Enable Nearby Scan")
                }
            }
            Text(
                text = uiState.ssid,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("BSSID ${uiState.bssid}", style = MaterialTheme.typography.bodySmall)
                Text("RSSI ${uiState.rssi} dBm", style = MaterialTheme.typography.bodyMedium)
                Text("Link ${uiState.linkSpeedMbps} Mbps", style = MaterialTheme.typography.bodyMedium)
                Text("Band ${uiState.channelLabel}", style = MaterialTheme.typography.bodyMedium)
                Text("Frequency ${uiState.frequencyMhz} MHz", style = MaterialTheme.typography.bodyMedium)
            }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
            ) {
                drawRoundRect(
                    color = colorScheme.surfaceContainerHighest,
                    cornerRadius = CornerRadius(16.dp.toPx()),
                )
                drawRoundRect(
                    color = if (uiState.level >= 3) colorScheme.primary else colorScheme.tertiary,
                    size = size.copy(width = size.width * ((uiState.level + 1) / 5f)),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                )
            }
            Text(
                text = "Signal history",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
            ) {
                drawRoundRect(
                    color = colorScheme.surfaceContainerLowest,
                    cornerRadius = CornerRadius(20.dp.toPx()),
                )
                val points = uiState.rssiHistory
                if (points.size > 1) {
                    val minRssi = -95f
                    val maxRssi = -35f
                    val stepX = size.width / (points.size - 1).coerceAtLeast(1)
                    val offsets = points.mapIndexed { index, value ->
                        val normalized = ((value - minRssi) / (maxRssi - minRssi)).coerceIn(0f, 1f)
                        Offset(
                            x = stepX * index,
                            y = size.height - (normalized * size.height),
                        )
                    }
                    for (index in 0 until offsets.lastIndex) {
                        drawLine(
                            color = colorScheme.primary,
                            start = offsets[index],
                            end = offsets[index + 1],
                            strokeWidth = 3.dp.toPx(),
                        )
                    }
                }
            }
            Text(
                text = "Nearby networks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            if (uiState.nearbyNetworks.isEmpty()) {
                Text(
                    text = if (hasPermission) "No nearby scan results yet." else "Grant permission to see nearby scan results.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    uiState.nearbyNetworks.forEach { network ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = if (network.isCurrentConnection) "${network.ssid} • current" else network.ssid,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = "${network.rssi} dBm • ${network.channelLabel} • ${network.frequencyMhz} MHz",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = network.bssid,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    text = "${network.level + 1}/5",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun checkWifiScanPermission(context: android.content.Context): Boolean {
    val hasLocation = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    val hasNearbyWifi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.NEARBY_WIFI_DEVICES,
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
    return hasLocation && hasNearbyWifi
}

private fun wifiScanPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.NEARBY_WIFI_DEVICES,
        )
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}
