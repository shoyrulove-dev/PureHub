package com.purehub.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.purehub.app.feature.bubblelevel.BubbleLevelViewModel
import kotlinx.coroutines.delay

private enum class LevelMode(val label: String) {
    Surface("Surface"),
    EdgeX("Edge X"),
    EdgeY("Edge Y"),
}

@Composable
fun BubbleLevelCard(
    viewModel: BubbleLevelViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val metrics = LocalResources.current.displayMetrics
    var rulerCentimeters by remember { mutableFloatStateOf(8f) }
    var rulerScale by rememberSaveable { mutableFloatStateOf(1f) }
    var sensorActive by rememberSaveable { mutableStateOf(false) }
    var levelMode by rememberSaveable { mutableStateOf(LevelMode.Surface) }
    var tolerance by rememberSaveable { mutableFloatStateOf(0.5f) }
    var settled by remember { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme
    val isLevel = when (levelMode) {
        LevelMode.Surface -> uiState.tiltMagnitude <= tolerance
        LevelMode.EdgeX -> kotlin.math.abs(uiState.roll) <= tolerance
        LevelMode.EdgeY -> kotlin.math.abs(uiState.pitch) <= tolerance
    }
    val levelColor = Color(0xFF10B981)
    val haptics = LocalHapticFeedback.current

    DisposableEffect(sensorActive) {
        if (sensorActive) viewModel.start() else viewModel.stop()
        onDispose { viewModel.stop() }
    }

    LaunchedEffect(sensorActive, isLevel, levelMode) {
        settled = false
        if (sensorActive && isLevel) {
            delay(1_500)
            settled = true
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FlagshipSuiteHeader(
                eyebrow = "Sensor Suite",
                title = "Bubble Level & Ruler",
                description = "A calm two-axis level and quick ruler powered by private on-device readings.",
            )
            Button(onClick = { sensorActive = !sensorActive }) {
                Text(if (sensorActive) "Pause level sensor" else "Enable level sensor")
            }
            Button(onClick = viewModel::calibrateZero, enabled = sensorActive) { Text("Calibrate zero") }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LevelMode.entries.forEach { mode ->
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { levelMode = mode },
                        enabled = levelMode != mode,
                    ) { Text(mode.label) }
                }
            }
            uiState.accuracyWarning?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Pitch ${uiState.pitch.toInt()} deg", style = MaterialTheme.typography.bodyMedium)
                Text("Roll ${uiState.roll.toInt()} deg", style = MaterialTheme.typography.bodyMedium)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension * 0.36f
                    val bubbleOffsetX = if (levelMode == LevelMode.EdgeY) 0f else (uiState.roll / 45f).coerceIn(-1f, 1f) * radius * 0.6f
                    val bubbleOffsetY = if (levelMode == LevelMode.EdgeX) 0f else (uiState.pitch / 45f).coerceIn(-1f, 1f) * radius * 0.6f
                    val guideInset = 12.dp.toPx()

                    drawCircle(
                        color = colorScheme.secondaryContainer,
                        radius = radius,
                        center = center,
                    )
                    drawCircle(
                        color = colorScheme.outline,
                        radius = radius,
                        center = center,
                        style = Stroke(width = 4.dp.toPx()),
                    )
                    listOf(
                        Offset(guideInset, guideInset),
                        Offset(size.width - guideInset, guideInset),
                        Offset(guideInset, size.height - guideInset),
                        Offset(size.width - guideInset, size.height - guideInset),
                    ).forEach { corner ->
                        drawLine(
                            color = colorScheme.outlineVariant,
                            start = corner,
                            end = center,
                            strokeWidth = 1.5.dp.toPx(),
                        )
                    }
                    drawLine(
                        color = colorScheme.outlineVariant,
                        start = Offset(center.x - radius, center.y),
                        end = Offset(center.x + radius, center.y),
                        strokeWidth = 2.dp.toPx(),
                    )
                    drawLine(
                        color = colorScheme.outlineVariant,
                        start = Offset(center.x, center.y - radius),
                        end = Offset(center.x, center.y + radius),
                        strokeWidth = 2.dp.toPx(),
                    )
                    drawCircle(
                        color = if (settled) levelColor else colorScheme.outline,
                        radius = 34.dp.toPx(),
                        center = center,
                        style = Stroke(width = 3.dp.toPx()),
                    )
                    drawCircle(
                        color = if (settled) levelColor else colorScheme.primary,
                        radius = 18.dp.toPx(),
                        center = Offset(center.x + bubbleOffsetX, center.y + bubbleOffsetY),
                    )
                }
            }

            Text(
                text = when {
                    settled -> "Level confirmed · reading held steady."
                    isLevel -> "Inside tolerance · keep the phone still."
                    else -> "Move the bubble into the center target."
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (settled) levelColor else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (settled) FontWeight.SemiBold else FontWeight.Normal,
            )

            Text("Tolerance ±${"%.1f".format(tolerance)}°", style = MaterialTheme.typography.titleSmall)
            Slider(value = tolerance, onValueChange = { tolerance = it }, valueRange = 0.1f..1.5f, steps = 13)

            val pxPerCm = (metrics.xdpi / 2.54f) * rulerScale
            val rulerWidth = ((pxPerCm * rulerCentimeters) / metrics.density).dp
            Text(
                text = "Ruler ${"%.1f".format(rulerCentimeters)} cm",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Slider(
                value = rulerCentimeters,
                onValueChange = { rulerCentimeters = it },
                valueRange = 2f..15f,
            )
            Text("Ruler calibration ${"%.0f".format(rulerScale * 100)}%", style = MaterialTheme.typography.bodySmall)
            Slider(value = rulerScale, onValueChange = { rulerScale = it }, valueRange = 0.85f..1.15f)
            Canvas(
                modifier = Modifier
                    .width(rulerWidth)
                    .height(56.dp),
            ) {
                drawLine(
                    color = colorScheme.primary,
                    start = Offset(0f, size.height * 0.8f),
                    end = Offset(size.width, size.height * 0.8f),
                    strokeWidth = 3.dp.toPx(),
                )
                val totalMarks = rulerCentimeters.toInt().coerceAtLeast(1) * 10
                for (index in 0..totalMarks) {
                    val x = size.width * index / totalMarks
                    val major = index % 10 == 0
                    val medium = index % 5 == 0
                    val markHeight = when {
                        major -> size.height * 0.7f
                        medium -> size.height * 0.52f
                        else -> size.height * 0.4f
                    }
                    drawLine(
                        color = colorScheme.onSurface,
                        start = Offset(x, size.height * 0.8f),
                        end = Offset(x, size.height * 0.8f - markHeight),
                        strokeWidth = if (major) 3.dp.toPx() else 2.dp.toPx(),
                    )
                }
            }
            Text(
                "Phone DPI can be approximate. Compare the ruler with a known reference and adjust calibration before measuring.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            uiState.errorMessage?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
