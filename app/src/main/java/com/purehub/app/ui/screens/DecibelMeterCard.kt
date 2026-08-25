package com.purehub.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.purehub.app.feature.decibel.DecibelMeterViewModel
import com.purehub.app.feature.decibel.SoundSessionCodec
import com.purehub.app.ui.LocalSnackbarHostState
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun DecibelMeterCard(
    viewModel: DecibelMeterViewModel = viewModel(),
) {
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var mode by rememberSaveable { mutableStateOf(SuiteMode.QUICK) }
    val hasRecordAudioPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        scope.launch {
            snackbarHostState.showSnackbar(
                if (granted) "Microphone access granted for local decibel sampling." else "Microphone access is optional. Decibel meter stays off without it.",
            )
        }
        if (granted) viewModel.start() else viewModel.stop()
    }
    val colorScheme = MaterialTheme.colorScheme

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FlagshipSuiteHeader(
                eyebrow = "Sensor Suite",
                title = "Sound Meter",
                description = "Current, peak, and rolling sound estimates with no streaming or upload path.",
            )
            SuiteModeSwitch(mode, { mode = it }, "Calibrated sessions, min/average/max history, and one-second CSV samples.")
            Text(
                text = "~${uiState.currentDecibel.toInt()} dB",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Estimated peak ~${uiState.peakDecibel.toInt()} dB",
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Rolling average ~${uiState.averageDecibel.toInt()} dB · last ${uiState.averageWindowSeconds}s",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(5, 10, 30, 60).forEach { seconds ->
                    FilterChip(
                        selected = uiState.averageWindowSeconds == seconds,
                        onClick = { viewModel.selectAverageWindow(seconds) },
                        label = { Text("${seconds}s") },
                    )
                }
            }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
            ) {
                val progress = (uiState.currentDecibel / 120f).coerceIn(0f, 1f)
                drawRoundRect(
                    color = colorScheme.surfaceContainerHighest,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                )
                drawRoundRect(
                    color = when {
                        uiState.currentDecibel < 40f -> colorScheme.primary
                        uiState.currentDecibel < 75f -> colorScheme.tertiary
                        else -> colorScheme.error
                    },
                    size = size.copy(width = size.width * progress),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        if (hasRecordAudioPermission) viewModel.start() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                ) {
                    Text(if (uiState.isRunning) "Monitoring" else "Start")
                }
                Button(onClick = viewModel::stop) {
                    Text("Stop")
                }
                Button(onClick = viewModel::resetPeak) {
                    Text("Reset Peak")
                }
            }
            uiState.errorMessage?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.error,
                )
            }
            Text(
                text = "Estimate only. Android microphones vary by device; do not use this result for legal or workplace safety decisions.",
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
            )
            Text("Calibration offset ${if (uiState.calibrationOffset >= 0) "+" else ""}${uiState.calibrationOffset.toInt()} dB")
            Slider(value = uiState.calibrationOffset, onValueChange = viewModel::setCalibrationOffset, valueRange = -20f..20f, steps = 39)
            Text(uiState.accuracyWarning, style = MaterialTheme.typography.bodySmall, color = colorScheme.tertiary)
            PrivacyReceipt(
                action = "Microphone samples stay on-device",
                detail = "PureHub stores only estimated dB points for saved sessions, never microphone audio.",
            )
            if (mode == SuiteMode.PRO && uiState.sessionHistory.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Recent sessions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedButton(onClick = viewModel::clearHistory) { Text("Clear") }
                }
                uiState.sessionHistory.take(5).forEach { session ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(session.startedAtEpochMillis)),
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text("Min ${session.minimumDecibel.toInt()} · Avg ${session.averageDecibel.toInt()} · Max ${session.maximumDecibel.toInt()} dB")
                            Text("${session.points.size} one-second samples", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                            OutlinedButton(onClick = {
                                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_SUBJECT, "PureHub sound session")
                                    putExtra(Intent.EXTRA_TEXT, SoundSessionCodec.csv(session))
                                }, "Export sound CSV"))
                            }) { Text("Export CSV") }
                        }
                    }
                }
            }
        }
    }
}
