package com.purehub.app.ui.screens

import android.graphics.Paint
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.purehub.app.feature.compass.CompassViewModel

@Composable
fun CompassScreen(
    innerPadding: PaddingValues,
    embedded: Boolean = false,
    viewModel: CompassViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var sensorActive by rememberSaveable { mutableStateOf(false) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val outlineColor = MaterialTheme.colorScheme.outline
    val smoothRotation by animateFloatAsState(
        targetValue = uiState.heading,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "compass_rotation",
    )

    DisposableEffect(sensorActive) {
        if (sensorActive) viewModel.startCompass() else viewModel.stopCompass()
        onDispose {
            viewModel.stopCompass()
        }
    }

    Column(
        modifier = Modifier
            .then(if (embedded) Modifier.fillMaxSize() else Modifier.fillMaxSize())
            .padding(innerPadding)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (!embedded) {
            Text(
                text = "Compass",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Accelerometer + magnetic field sensors are filtered locally and rendered with spring-smoothed motion for a calmer, non-jittery heading.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(
                    modifier = Modifier
                        .size(280.dp)
                        .rotate(-smoothRotation),
                ) {
                    val strokeWidth = 12.dp.toPx()
                    val radius = size.minDimension / 2f
                    drawCircle(
                        color = primaryColor,
                        style = Stroke(width = strokeWidth),
                    )
                    drawLine(
                        color = tertiaryColor,
                        start = Offset(x = radius, y = radius * 0.22f),
                        end = Offset(x = radius, y = radius * 1.05f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = outlineColor,
                        start = Offset(x = radius, y = radius * 0.95f),
                        end = Offset(x = radius, y = radius * 1.68f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        textAlign = Paint.Align.CENTER
                        textSize = 18.dp.toPx()
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                    val labelRadius = radius * 0.78f
                    drawContext.canvas.nativeCanvas.apply {
                        labelPaint.color = tertiaryColor.toArgb()
                        drawText("N", radius, radius - labelRadius + labelPaint.textSize * 0.35f, labelPaint)
                        labelPaint.color = outlineColor.toArgb()
                        drawText("S", radius, radius + labelRadius + labelPaint.textSize * 0.35f, labelPaint)
                        drawText("E", radius + labelRadius, radius + labelPaint.textSize * 0.35f, labelPaint)
                        drawText("W", radius - labelRadius, radius + labelPaint.textSize * 0.35f, labelPaint)
                    }
                }
            }
        }
        Button(onClick = { sensorActive = !sensorActive }) {
            Text(if (sensorActive) "Pause compass" else "Enable compass")
        }
        Text(
            text = "Heading: ${((smoothRotation % 360f) + 360f).toInt() % 360} deg ${uiState.cardinalDirection}",
            style = MaterialTheme.typography.titleMedium,
        )
        if (!uiState.isSensorAvailable && uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
