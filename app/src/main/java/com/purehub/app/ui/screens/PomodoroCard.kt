package com.purehub.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import com.purehub.app.ui.LocalizedText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.purehub.app.feature.pomodoro.PomodoroPreset
import com.purehub.app.feature.pomodoro.PomodoroViewModel

@Composable
fun PomodoroCard(
    innerPadding: PaddingValues = PaddingValues(0.dp),
    compact: Boolean = false,
    viewModel: PomodoroViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val soundscapes = listOf("White Noise", "Brown Noise", "Soft Rain")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(innerPadding),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (!compact) {
                FlagshipSuiteHeader(
                    eyebrow = "Zen Suite flagship",
                    title = "Zen Pomodoro",
                    description = "Accurate focus timing, quick presets, private weekly progress, and on-device soundscapes.",
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                viewModel.presets().forEach { preset ->
                    PresetChip(
                        preset = preset,
                        selected = preset == uiState.selectedPreset,
                        onClick = { viewModel.selectPreset(preset) },
                    )
                }
            }

            LocalizedText(
                text = formatSeconds(uiState.secondsRemaining),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
            )
            CircularProgressIndicator(
                progress = { 1f - uiState.progress.coerceIn(0f, 1f) },
                modifier = Modifier.padding(vertical = 4.dp),
                strokeWidth = 8.dp,
            )
            LinearProgressIndicator(
                progress = { uiState.progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    if (!uiState.isRunning && Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    viewModel.toggleTimer()
                }) {
                    LocalizedText(if (uiState.isRunning) "Pause" else "Start")
                }
                Button(onClick = { viewModel.reset() }) {
                    LocalizedText("Reset")
                }
            }

            LocalizedText(
                text = "Soundscape",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                soundscapes.forEach { label ->
                    FilterChip(
                        selected = label == uiState.selectedSoundscape,
                        onClick = { viewModel.selectSoundscape(label) },
                        label = { LocalizedText(label) },
                    )
                }
            }
            LocalizedText(
                text = "Volume ${(uiState.volume * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = uiState.volume,
                onValueChange = { viewModel.updateVolume(it) },
            )
            LocalizedText(
                text = uiState.note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LocalizedText(
                text = "This week: ${uiState.weeklySessions} sessions · ${uiState.weeklyMinutes} focused minutes",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun PresetChip(
    preset: PomodoroPreset,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { LocalizedText(if (selected) "${preset.label} • Active" else preset.label) },
    )
}

private fun formatSeconds(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
