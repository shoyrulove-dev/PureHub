package com.purehub.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import com.purehub.app.ui.LocalizedText
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import com.purehub.app.feature.bubblelevel.BubbleLevelViewModel
import kotlinx.coroutines.delay

private enum class LevelMode(val label: String) {
    Surface("Surface"),
    Edge("Edge"),
    Camera("Camera"),
}

private enum class LevelUnit { Degrees, Percent }

@Composable
fun BubbleLevelCard(
    viewModel: BubbleLevelViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("purehub.level.pro.v2", 0) }
    val metrics = LocalResources.current.displayMetrics
    var rulerCentimeters by remember { mutableFloatStateOf(8f) }
    var rulerScale by rememberSaveable { mutableFloatStateOf(1f) }
    var sensorActive by rememberSaveable { mutableStateOf(false) }
    var levelMode by rememberSaveable { mutableStateOf(LevelMode.Surface) }
    var tolerance by rememberSaveable { mutableFloatStateOf(preferences.getFloat("tolerance", 0.5f)) }
    var targetSlope by rememberSaveable { mutableFloatStateOf(preferences.getFloat("target_slope", 0f)) }
    var levelUnit by rememberSaveable { mutableStateOf(if (preferences.getString("unit", "degrees") == "percent") LevelUnit.Percent else LevelUnit.Degrees) }
    var settled by remember { mutableStateOf(false) }
    var mode by rememberSaveable { mutableStateOf(SuiteMode.QUICK) }
    var heldMeasurement by rememberSaveable { mutableStateOf("") }
    var soundCueEnabled by rememberSaveable { mutableStateOf(preferences.getBoolean("sound", false)) }
    var hasCameraPermission by rememberSaveable { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var measurementHistory by rememberSaveable { mutableStateOf(preferences.getString("history", "").orEmpty().split("||").filter(String::isNotBlank)) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasCameraPermission = it }
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 55) }
    val colorScheme = MaterialTheme.colorScheme
    val slopePercent = kotlin.math.tan(Math.toRadians(uiState.tiltMagnitude.toDouble())) * 100.0
    val targetDegrees = Math.toDegrees(kotlin.math.atan((targetSlope / 100f).toDouble())).toFloat()
    val isLevel = when (levelMode) {
        LevelMode.Surface, LevelMode.Camera -> kotlin.math.abs(uiState.tiltMagnitude - targetDegrees) <= tolerance
        LevelMode.Edge -> kotlin.math.abs(kotlin.math.abs(uiState.roll) - targetDegrees) <= tolerance
    }
    val levelColor = Color(0xFF10B981)
    val haptics = LocalHapticFeedback.current

    DisposableEffect(sensorActive) {
        if (sensorActive) viewModel.start() else viewModel.stop()
        onDispose { viewModel.stop() }
    }

    DisposableEffect(Unit) {
        onDispose { toneGenerator.release() }
    }

    LaunchedEffect(sensorActive, isLevel, levelMode) {
        settled = false
        if (sensorActive && isLevel) {
            delay(1_500)
            settled = true
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            if (soundCueEnabled) toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 140)
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    LocalizedText("Live level", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    LocalizedText("Private on-device sensor", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilterChip(
                    selected = mode == SuiteMode.PRO,
                    onClick = { mode = if (mode == SuiteMode.PRO) SuiteMode.QUICK else SuiteMode.PRO },
                    label = { LocalizedText("Options", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelMedium) },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LevelMode.entries.forEach { mode ->
                    Button(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        onClick = {
                            levelMode = mode
                            if (mode == LevelMode.Camera && !hasCameraPermission) cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        enabled = levelMode != mode,
                    ) { LocalizedText(mode.label, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelMedium) }
                }
            }
            uiState.accuracyWarning?.let { LocalizedText(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (levelMode == LevelMode.Camera && hasCameraPermission) {
                    BubbleCameraPreview(Modifier.matchParentSize())
                }
                Canvas(modifier = Modifier.matchParentSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension * 0.36f
                    val bubbleOffsetX = (uiState.roll / 45f).coerceIn(-1f, 1f) * radius * 0.72f
                    val bubbleOffsetY = if (levelMode == LevelMode.Edge) 0f else (uiState.pitch / 45f).coerceIn(-1f, 1f) * radius * 0.72f
                    val guideInset = 12.dp.toPx()

                    drawCircle(
                        color = if (levelMode == LevelMode.Camera) Color.Black.copy(alpha = 0.18f) else colorScheme.secondaryContainer,
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

            LocalizedText(
                text = when {
                    settled -> "Level confirmed · reading held steady."
                    isLevel -> "Inside tolerance · keep the phone still."
                    else -> "Move the bubble into the center target."
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (settled) levelColor else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (settled) FontWeight.SemiBold else FontWeight.Normal,
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LocalizedText("Pitch ${formatLevelValue(uiState.pitch, levelUnit)}", style = MaterialTheme.typography.bodySmall)
                LocalizedText("Roll ${formatLevelValue(uiState.roll, levelUnit)}", style = MaterialTheme.typography.bodySmall)
                LocalizedText("Slope ${"%.1f".format(slopePercent)}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp), onClick = { sensorActive = !sensorActive }) {
                    LocalizedText(if (sensorActive) "Pause" else "Start", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelMedium)
                }
                Button(modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp), onClick = {
                    heldMeasurement = if (heldMeasurement.isBlank()) "${levelMode.label}: ${"%.1f".format(uiState.pitch)}° · ${"%.1f".format(uiState.roll)}°" else ""
                }, enabled = sensorActive) { LocalizedText(if (heldMeasurement.isBlank()) "Hold" else "Resume", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelMedium) }
                Button(modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp), onClick = {
                    val entry = "${levelMode.label}: ${"%.1f".format(uiState.pitch)}° · ${"%.1f".format(uiState.roll)}° · ${"%.1f".format(slopePercent)}%"
                    measurementHistory = (listOf(entry) + measurementHistory).take(12)
                    preferences.edit().putString("history", measurementHistory.joinToString("||")).apply()
                }, enabled = sensorActive) { LocalizedText("Save", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelMedium) }
            }
            Button(modifier = Modifier.fillMaxWidth(), onClick = viewModel::calibrateZero, enabled = sensorActive) { LocalizedText("Calibrate zero") }

            if (mode == SuiteMode.PRO) {
                FilterChip(
                    selected = soundCueEnabled,
                    onClick = {
                        soundCueEnabled = !soundCueEnabled
                        preferences.edit().putBoolean("sound", soundCueEnabled).apply()
                    },
                    label = { LocalizedText(if (soundCueEnabled) "Level sound on" else "Level sound off") },
                )
                LocalizedText("Tolerance ±${"%.1f".format(tolerance)}°", style = MaterialTheme.typography.titleSmall)
                Slider(value = tolerance, onValueChange = { tolerance = it; preferences.edit().putFloat("tolerance", it).apply() }, valueRange = 0.1f..1.5f, steps = 13)
                LocalizedText("Target slope ${targetSlope.toInt()}%", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0f, 1f, 2f, 5f).forEach { target ->
                        FilterChip(selected = targetSlope == target, onClick = { targetSlope = target; preferences.edit().putFloat("target_slope", target).apply() }, label = { LocalizedText("${target.toInt()}%") })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = levelUnit == LevelUnit.Degrees, onClick = { levelUnit = LevelUnit.Degrees; preferences.edit().putString("unit", "degrees").apply() }, label = { LocalizedText("Degrees") })
                    FilterChip(selected = levelUnit == LevelUnit.Percent, onClick = { levelUnit = LevelUnit.Percent; preferences.edit().putString("unit", "percent").apply() }, label = { LocalizedText("Percent") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = {
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "PureHub level measurement\n$heldMeasurement")
                        }, "Share level measurement"))
                    }, enabled = heldMeasurement.isNotBlank()) { LocalizedText("Share") }
                }
                if (heldMeasurement.isNotBlank()) {
                    LocalizedText(heldMeasurement, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                LocalizedText("History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (measurementHistory.isEmpty()) LocalizedText("No saved measurements yet.", style = MaterialTheme.typography.bodySmall)
                measurementHistory.forEach { LocalizedText(it, style = MaterialTheme.typography.bodySmall) }
                if (measurementHistory.isNotEmpty()) Button(onClick = { measurementHistory = emptyList(); preferences.edit().remove("history").apply() }) { LocalizedText("Clear history") }
            }

            if (mode == SuiteMode.PRO) {
            val pxPerCm = (metrics.xdpi / 2.54f) * rulerScale
            val rulerWidth = ((pxPerCm * rulerCentimeters) / metrics.density).dp
            LocalizedText(
                text = "Ruler ${"%.1f".format(rulerCentimeters)} cm",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Slider(
                value = rulerCentimeters,
                onValueChange = { rulerCentimeters = it },
                valueRange = 2f..15f,
            )
            LocalizedText("Ruler calibration ${"%.0f".format(rulerScale * 100)}%", style = MaterialTheme.typography.bodySmall)
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
            LocalizedText(
                "Phone DPI can be approximate. Compare the ruler with a known reference and adjust calibration before measuring.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            }
            PrivacyReceipt(
                action = "Measurements remain local",
                detail = "PureHub reads motion sensors only while enabled and shares a held value only when you choose Share.",
            )
            uiState.errorMessage?.let { error ->
                LocalizedText(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private fun formatLevelValue(value: Float, unit: LevelUnit): String = when (unit) {
    LevelUnit.Degrees -> "${"%.1f".format(value)}°"
    LevelUnit.Percent -> "${"%.1f".format(kotlin.math.tan(Math.toRadians(value.toDouble())) * 100.0)}%"
}

@Composable
private fun BubbleCameraPreview(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    AndroidView(factory = { previewView }, modifier = modifier)
    DisposableEffect(previewView, lifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            runCatching {
                val provider = future.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
            }
        }
        future.addListener(listener, ContextCompat.getMainExecutor(context))
        onDispose { runCatching { if (future.isDone) future.get().unbindAll() } }
    }
}
