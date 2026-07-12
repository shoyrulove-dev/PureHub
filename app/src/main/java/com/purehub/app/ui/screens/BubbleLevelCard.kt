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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.purehub.app.feature.bubblelevel.BubbleLevelViewModel

@Composable
fun BubbleLevelCard(
    viewModel: BubbleLevelViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val metrics = LocalContext.current.resources.displayMetrics
    var rulerCentimeters by remember { mutableFloatStateOf(8f) }
    val colorScheme = MaterialTheme.colorScheme

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
                text = "Bubble Level & Ruler",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "Accelerometer readings stay on-device and drive both a calm level bubble and a quick screen ruler.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

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
                    val bubbleOffsetX = (uiState.roll / 45f).coerceIn(-1f, 1f) * radius * 0.6f
                    val bubbleOffsetY = (uiState.pitch / 45f).coerceIn(-1f, 1f) * radius * 0.6f

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
                        color = colorScheme.primary,
                        radius = 18.dp.toPx(),
                        center = Offset(center.x + bubbleOffsetX, center.y + bubbleOffsetY),
                    )
                }
            }

            Text(
                text = if (uiState.tiltMagnitude < 0.55f) "Surface is close to level." else "Adjust device until bubble reaches center.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val pxPerCm = metrics.xdpi / 2.54f
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
