package com.purehub.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private data class BreathPhase(val label: String, val seconds: Int, val scale: Float)
private data class BreathPattern(val label: String, val phases: List<BreathPhase>)

private val breathPatterns = linkedMapOf(
    "calm" to BreathPattern("Calm 4-6", listOf(BreathPhase("Inhale", 4, 1f), BreathPhase("Exhale", 6, 0.7f))),
    "box" to BreathPattern("Box 4-4-4-4", listOf(BreathPhase("Inhale", 4, 1f), BreathPhase("Hold", 4, 1f), BreathPhase("Exhale", 4, 0.7f), BreathPhase("Hold", 4, 0.7f))),
    "relax" to BreathPattern("Relax 4-7-8", listOf(BreathPhase("Inhale", 4, 1f), BreathPhase("Hold", 7, 1f), BreathPhase("Exhale", 8, 0.7f))),
)

@Composable
fun ZenBreathCard() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("purehub.zen-breath.v1", 0) }
    var patternId by rememberSaveable { mutableStateOf("calm") }
    var phaseIndex by rememberSaveable { mutableIntStateOf(0) }
    var remaining by rememberSaveable { mutableIntStateOf(4) }
    var running by rememberSaveable { mutableStateOf(false) }
    var cycles by rememberSaveable { mutableIntStateOf(0) }
    var elapsed by rememberSaveable { mutableIntStateOf(0) }
    var totalSessions by rememberSaveable { mutableIntStateOf(preferences.getInt("sessions", 0)) }
    val pattern = breathPatterns.getValue(patternId)
    val phase = pattern.phases[phaseIndex]
    val breathProgress by animateFloatAsState(
        targetValue = phase.scale,
        animationSpec = tween(durationMillis = phase.seconds * 1_000, easing = FastOutSlowInEasing),
        label = "breath_scale",
    )
    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(running, phaseIndex, patternId) {
        if (!running) return@LaunchedEffect
        while (running) {
            delay(1_000)
            elapsed += 1
            if (remaining > 1) {
                remaining -= 1
            } else {
                val next = (phaseIndex + 1) % pattern.phases.size
                if (next == 0) cycles += 1
                phaseIndex = next
                remaining = pattern.phases[next].seconds
                break
            }
        }
    }

    fun reset(nextPattern: String = patternId) {
        patternId = nextPattern
        phaseIndex = 0
        remaining = breathPatterns.getValue(nextPattern).phases.first().seconds
        cycles = 0
        elapsed = 0
        running = false
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Zen Breath", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
            Text("A private, distraction-free breathing guide with selectable rhythms and local session totals.", style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                breathPatterns.forEach { (id, item) ->
                    AssistChip(onClick = { reset(id) }, label = { Text(if (id == patternId) "${item.label} - Active" else item.label) })
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension * 0.31f * breathProgress
                    drawCircle(Brush.radialGradient(listOf(colorScheme.primary.copy(alpha = 0.42f), colorScheme.tertiary.copy(alpha = 0.1f)), center, radius * 1.8f), radius * 1.8f, center)
                    drawCircle(colorScheme.primary.copy(alpha = 0.28f), radius * 1.18f, center)
                    drawCircle(colorScheme.primary, radius, center)
                    drawCircle(colorScheme.onPrimary.copy(alpha = 0.2f), radius, center, style = Stroke(5.dp.toPx()))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (running) phase.label else "Ready", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = colorScheme.onPrimaryContainer)
                    Text("$remaining sec", style = MaterialTheme.typography.bodyLarge, color = colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = {
                    if (running && elapsed >= 30) {
                        totalSessions += 1
                        preferences.edit().putInt("sessions", totalSessions).apply()
                    }
                    running = !running
                }) { Text(if (running) "Pause" else "Start session") }
                Button(onClick = { reset() }) { Text("Reset") }
            }
            Text("$cycles complete cycles · ${elapsed / 60}:${(elapsed % 60).toString().padStart(2, '0')} elapsed · $totalSessions saved sessions", style = MaterialTheme.typography.labelLarge, color = colorScheme.primary)
            Text("Breathe comfortably and stop if you feel dizzy or unwell.", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
        }
    }
}
