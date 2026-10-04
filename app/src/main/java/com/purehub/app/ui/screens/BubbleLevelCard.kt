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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Slider
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import com.purehub.app.ui.LocalizedText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlinx.coroutines.launch

private enum class LevelMode(val label: String) {
    Surface("Surface"),
    Edge("Edge"),
    Camera("Camera"),
}

private enum class LevelUnit { Degrees, Percent }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun BubbleLevelCard(
    modifier: Modifier = Modifier,
    viewModel: BubbleLevelViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("purehub.level.pro.v2", 0) }
    var rulerCentimeters by remember { mutableFloatStateOf(8f) }
    var rulerScale by rememberSaveable { mutableFloatStateOf(1f) }
    var sensorActive by rememberSaveable { mutableStateOf(true) }
    var levelMode by rememberSaveable { mutableStateOf(LevelMode.Surface) }
    var tolerance by rememberSaveable { mutableFloatStateOf(preferences.getFloat("tolerance", 0.5f)) }
    var targetSlope by rememberSaveable { mutableFloatStateOf(preferences.getFloat("target_slope", 0f)) }
    var levelUnit by rememberSaveable { mutableStateOf(if (preferences.getString("unit", "degrees") == "percent") LevelUnit.Percent else LevelUnit.Degrees) }
    var settled by remember { mutableStateOf(false) }
    var heldMeasurement by rememberSaveable { mutableStateOf("") }
    var soundCueEnabled by rememberSaveable { mutableStateOf(preferences.getBoolean("sound", false)) }
    var hasCameraPermission by rememberSaveable { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var measurementHistory by rememberSaveable { mutableStateOf(preferences.getString("history", "").orEmpty().split("||").filter(String::isNotBlank)) }
    var showCalibrationDialog by rememberSaveable { mutableStateOf(false) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasCameraPermission = it }
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 55) }
    val colorScheme = MaterialTheme.colorScheme
    val slopePercent = kotlin.math.tan(Math.toRadians(uiState.tiltMagnitude.toDouble())) * 100.0
    val targetDegrees = Math.toDegrees(kotlin.math.atan((targetSlope / 100f).toDouble())).toFloat()
    val isLevel = when (levelMode) {
        LevelMode.Surface, LevelMode.Camera -> kotlin.math.abs(uiState.tiltMagnitude - targetDegrees) <= tolerance
        LevelMode.Edge -> kotlin.math.abs(kotlin.math.abs(uiState.roll) - targetDegrees) <= tolerance
    }
    val levelColor = Color(0xFF67E8F9)
    val haptics = LocalHapticFeedback.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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

    ModalNavigationDrawer(
        modifier = modifier,
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.52f)) {
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LocalizedText("Options", style = MaterialTheme.typography.titleSmall)
                    LocalizedText("Mode", style = MaterialTheme.typography.labelMedium)
                    LevelMode.entries.forEach { item ->
                        NavigationDrawerItem(
                            label = { LocalizedText(item.label, maxLines = 1, softWrap = false) },
                            selected = levelMode == item,
                            onClick = {
                                levelMode = item
                                if (item == LevelMode.Camera && !hasCameraPermission) cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                scope.launch { drawerState.close() }
                            },
                        )
                    }
                    HorizontalDivider()
                    Button(modifier = Modifier.fillMaxWidth(), onClick = { showCalibrationDialog = true }, enabled = sensorActive) {
                        LocalizedText("Calibrate", maxLines = 1, softWrap = false)
                    }
                    TextButton(modifier = Modifier.fillMaxWidth(), onClick = viewModel::resetCalibration) {
                        LocalizedText("Reset zero", maxLines = 1, softWrap = false)
                    }
                    LocalizedText("Accuracy", style = MaterialTheme.typography.labelMedium)
                    LocalizedText("±${"%.1f".format(tolerance)}°", style = MaterialTheme.typography.bodySmall)
                    Slider(value = tolerance, onValueChange = { tolerance = it; preferences.edit().putFloat("tolerance", it).apply() }, valueRange = 0.1f..1.5f, steps = 13)
                    LocalizedText("Target ${targetSlope.toInt()}%", style = MaterialTheme.typography.bodySmall)
                    listOf(0f, 1f, 2f, 5f).chunked(2).forEach { targets ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            targets.forEach { target ->
                                FilterChip(
                                    modifier = Modifier.weight(1f),
                                    selected = targetSlope == target,
                                    onClick = { targetSlope = target; preferences.edit().putFloat("target_slope", target).apply() },
                                    label = { LocalizedText("${target.toInt()}%") },
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = levelUnit == LevelUnit.Degrees, onClick = { levelUnit = LevelUnit.Degrees; preferences.edit().putString("unit", "degrees").apply() }, label = { LocalizedText("°") })
                        FilterChip(selected = levelUnit == LevelUnit.Percent, onClick = { levelUnit = LevelUnit.Percent; preferences.edit().putString("unit", "percent").apply() }, label = { LocalizedText("%") })
                    }
                    FilterChip(
                        selected = soundCueEnabled,
                        onClick = { soundCueEnabled = !soundCueEnabled; preferences.edit().putBoolean("sound", soundCueEnabled).apply() },
                        label = { LocalizedText("Sound", maxLines = 1) },
                    )
                    HorizontalDivider()
                    LocalizedText("History", style = MaterialTheme.typography.labelMedium)
                    if (measurementHistory.isEmpty()) LocalizedText("No measurements", style = MaterialTheme.typography.bodySmall)
                    measurementHistory.take(4).forEach { LocalizedText(it, style = MaterialTheme.typography.bodySmall) }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(onClick = {
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "PureHub level\n$heldMeasurement")
                            }, "Share"))
                        }, enabled = heldMeasurement.isNotBlank()) { LocalizedText("Share") }
                        TextButton(onClick = { measurementHistory = emptyList(); preferences.edit().remove("history").apply() }, enabled = measurementHistory.isNotEmpty()) { LocalizedText("Clear") }
                    }
                    HorizontalDivider()
                    LocalizedText("Ruler", style = MaterialTheme.typography.labelMedium)
                    LocalizedText("${"%.1f".format(rulerCentimeters)} cm", style = MaterialTheme.typography.bodySmall)
                    Slider(value = rulerCentimeters, onValueChange = { rulerCentimeters = it }, valueRange = 2f..15f)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        LocalizedText("Scale", style = MaterialTheme.typography.bodySmall)
                        LocalizedText("${"%.0f".format(rulerScale * 100)}%", style = MaterialTheme.typography.bodySmall)
                    }
                    Slider(value = rulerScale, onValueChange = { rulerScale = it }, valueRange = 0.85f..1.15f)
                }
            }
        },
    ) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(Icons.Rounded.Menu, contentDescription = null)
                }
                LocalizedText(levelMode.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
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
                    settled -> "Level"
                    isLevel -> "Hold still"
                    else -> "Center bubble"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (settled) levelColor else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (settled) FontWeight.SemiBold else FontWeight.Normal,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LocalizedText("X ${formatLevelValue(uiState.pitch, levelUnit)}", style = MaterialTheme.typography.bodySmall)
                LocalizedText("Y ${formatLevelValue(uiState.roll, levelUnit)}", style = MaterialTheme.typography.bodySmall)
                LocalizedText("${"%.1f".format(slopePercent)}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
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

    if (showCalibrationDialog) {
        AlertDialog(
            onDismissRequest = { showCalibrationDialog = false },
            title = { LocalizedText("Calibrate level") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LocalizedText("1. Remove a thick phone case if it rocks.")
                    LocalizedText("2. Place the phone on a known-flat reference.")
                    LocalizedText("3. Keep it still, then save this position as zero.")
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.calibrateZero(); showCalibrationDialog = false }) { LocalizedText("Save zero") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { viewModel.resetCalibration(); showCalibrationDialog = false }) { LocalizedText("Reset zero") }
                    TextButton(onClick = { showCalibrationDialog = false }) { LocalizedText("Cancel") }
                }
            },
        )
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
