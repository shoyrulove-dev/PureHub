package com.purehub.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import com.purehub.app.ui.LocalizedText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.purehub.app.feature.speakercleaner.SpeakerCleanerViewModel

@Composable
fun SpeakerCleanerCard(
    viewModel: SpeakerCleanerViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FlagshipSuiteHeader(
                eyebrow = "Audio Care flagship",
                title = "Speaker Cleaner",
                description = "Timed low-frequency presets designed to help move light moisture from a phone speaker.",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Gentle" to 150f, "Balanced" to 165f, "Deep" to 185f).forEach { (label, value) ->
                    FilterChip(selected = uiState.frequencyHz == value, onClick = { viewModel.selectPreset(value) }, label = { LocalizedText(label) })
                }
            }
            LocalizedText(
                text = "Frequency ${uiState.frequencyHz.toInt()} Hz",
                style = MaterialTheme.typography.titleMedium,
            )
            Slider(
                value = uiState.frequencyHz,
                onValueChange = viewModel::updateFrequency,
                valueRange = 120f..220f,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 30, 60).forEach { seconds ->
                    FilterChip(selected = uiState.durationSeconds == seconds, onClick = { viewModel.setDuration(seconds) }, label = { LocalizedText("${seconds}s") })
                }
            }
            LinearProgressIndicator(
                progress = { if (uiState.durationSeconds == 0) 0f else 1f - uiState.remainingSeconds.toFloat() / uiState.durationSeconds },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = viewModel::togglePlayback) {
                    LocalizedText(if (uiState.isPlaying) "Stop • ${uiState.remainingSeconds}s" else "Start cleaning cycle")
                }
            }
            LocalizedText(
                text = uiState.note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (uiState.completed) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        LocalizedText("Cycle complete", fontWeight = FontWeight.Bold)
                        LocalizedText(
                            text = "Play the same familiar audio at a comfortable volume and compare clarity before running another cycle.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            LocalizedText(
                text = "Start at a comfortable volume, keep the speaker facing down, and stop if the sound distorts.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
