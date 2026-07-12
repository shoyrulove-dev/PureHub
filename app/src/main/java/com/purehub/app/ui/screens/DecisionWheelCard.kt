package com.purehub.app.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun DecisionWheelCard() {
    var optionsText by remember { mutableStateOf("Coffee, Tea, Juice, Water") }
    var targetRotation by remember { mutableFloatStateOf(0f) }
    var result by remember { mutableStateOf("Spin the wheel to decide.") }
    val options = optionsText.split(",").map { it.trim() }.filter { it.isNotBlank() }.ifEmpty { listOf("Option") }
    val animatedRotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessVeryLow,
        ),
        label = "decision_wheel_rotation",
    )
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.error,
    )
    val centerColor = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outline
    val pointerColor = MaterialTheme.colorScheme.onSurface
    val density = LocalDensity.current

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Decision Wheel",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "A playful offline roulette with smooth springy motion. Separate choices with commas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = optionsText,
                onValueChange = { optionsText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Options") },
                minLines = 2,
            )
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension * 0.38f
                val sliceAngle = 360f / options.size
                val rect = Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)

                rotate(animatedRotation, center) {
                    options.forEachIndexed { index, option ->
                        drawArc(
                            color = colors[index % colors.size],
                            startAngle = index * sliceAngle - 90f,
                            sweepAngle = sliceAngle,
                            useCenter = true,
                            topLeft = rect.topLeft,
                            size = rect.size,
                        )
                        val midAngle = Math.toRadians((index * sliceAngle + sliceAngle / 2f - 90f).toDouble())
                        val textX = center.x + cos(midAngle).toFloat() * radius * 0.58f
                        val textY = center.y + sin(midAngle).toFloat() * radius * 0.58f
                        drawContext.canvas.nativeCanvas.drawText(
                            option.take(10),
                            textX,
                            textY,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                textAlign = android.graphics.Paint.Align.CENTER
                                textSize = with(density) { 13.dp.toPx() }
                                isFakeBoldText = true
                            },
                        )
                    }
                }

                drawCircle(
                    color = centerColor,
                    radius = radius * 0.18f,
                    center = center,
                )
                drawCircle(
                    color = outlineColor,
                    radius = radius,
                    center = center,
                    style = Stroke(width = 4.dp.toPx()),
                )
                drawLine(
                    color = pointerColor,
                    start = Offset(center.x, center.y - radius - 14.dp.toPx()),
                    end = Offset(center.x, center.y - radius + 12.dp.toPx()),
                    strokeWidth = 6.dp.toPx(),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        val winnerIndex = Random.nextInt(options.size)
                        val sliceAngle = 360f / options.size
                        val winnerCenter = winnerIndex * sliceAngle + sliceAngle / 2f
                        val currentNormalized = ((targetRotation % 360f) + 360f) % 360f
                        val desiredNormalized = (360f - winnerCenter + 360f) % 360f
                        val deltaToWinner = (desiredNormalized - currentNormalized + 360f) % 360f
                        targetRotation += 1440f + deltaToWinner
                        result = "Result: ${options[winnerIndex]}"
                    },
                ) {
                    Text("Spin")
                }
            }
            Text(
                text = result,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
