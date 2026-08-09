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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.purehub.app.feature.compass.CompassViewModel
import kotlin.math.cos
import kotlin.math.sin

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
    val northColor = MaterialTheme.colorScheme.error
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
            FlagshipSuiteHeader(
                eyebrow = "Sensor Suite",
                title = "Compass",
                description = "A spring-smoothed heading from locally filtered motion and magnetic sensors.",
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
                        .size(280.dp),
                ) {
                    val strokeWidth = 12.dp.toPx()
                    val radius = size.minDimension / 2f
                    rotate(degrees = -smoothRotation, pivot = Offset(radius, radius)) {
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
                    }
                    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        textAlign = Paint.Align.CENTER
                        textSize = 18.dp.toPx()
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                    val labelRadius = radius * 0.70f
                    val labelAngles = listOf(
                        "N" to -90f,
                        "E" to 0f,
                        "S" to 90f,
                        "W" to 180f,
                    )
                    drawContext.canvas.nativeCanvas.apply {
                        labelAngles.forEach { (label, baseAngle) ->
                            val radians = Math.toRadians((baseAngle - smoothRotation).toDouble())
                            val x = radius + labelRadius * cos(radians).toFloat()
                            val y = radius + labelRadius * sin(radians).toFloat() + labelPaint.textSize * 0.35f
                            labelPaint.color = if (label == "N") northColor.toArgb() else outlineColor.toArgb()
                            drawText(label, x, y, labelPaint)
                        }
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
        Text("Accuracy: ${uiState.accuracyLabel}", color = MaterialTheme.colorScheme.primary)
        uiState.accuracyWarning?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        if (!uiState.isSensorAvailable && uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
