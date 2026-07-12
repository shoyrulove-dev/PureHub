package com.purehub.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
    val soundscapes = listOf("Rain", "Cafe", "Brown Noise")

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
                Text(
                    text = "Zen Pomodoro",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "A calm offline focus loop with local timer state and real on-device white-noise playback.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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

            Text(
                text = formatSeconds(uiState.secondsRemaining),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
            )
            LinearProgressIndicator(
                progress = { uiState.progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { viewModel.toggleTimer() }) {
                    Text(if (uiState.isRunning) "Pause" else "Start")
                }
                Button(onClick = { viewModel.reset() }) {
                    Text("Reset")
                }
            }

            Text(
                text = "Soundscape",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                soundscapes.forEach { label ->
                    AssistChip(
                        onClick = { viewModel.selectSoundscape(label) },
                        label = { Text(label) },
                    )
                }
            }
            Text(
                text = "Volume ${(uiState.volume * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = uiState.volume,
                onValueChange = { viewModel.updateVolume(it) },
            )
            Text(
                text = uiState.note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    AssistChip(
        onClick = onClick,
        label = { Text(if (selected) "${preset.label} • Active" else preset.label) },
    )
}

private fun formatSeconds(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
