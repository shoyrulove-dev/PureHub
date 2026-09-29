package com.purehub.app.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.purehub.app.feature.screenrecorder.ScreenRecorderRuntime
import com.purehub.app.feature.screenrecorder.ScreenRecorderService
import com.purehub.app.feature.screenrecorder.ScreenRecordingPhase
import com.purehub.app.ui.LocalAppLanguage
import com.purehub.app.ui.LocalizedText
import com.purehub.app.ui.translateUiText
import kotlinx.coroutines.delay

private enum class RecorderPreset(
    val label: String,
    val detail: String,
    val widthCap: Int,
    val frameRate: Int,
    val bitRate: Int,
) {
    EFFICIENT("Efficient", "720p · 30 fps", 720, 30, 4_000_000),
    BALANCED("Balanced", "1080p · 30 fps", 1080, 30, 8_000_000),
    SMOOTH("Smooth", "720p · 60 fps", 720, 60, 8_000_000),
}

enum class ScreenRecorderMenuAction { Start, Quality }

@Composable
fun ScreenRecorderCard(
    innerPadding: PaddingValues = PaddingValues(0.dp),
    menuAction: ScreenRecorderMenuAction? = null,
    onMenuActionHandled: () -> Unit = {},
) {
    val context = LocalContext.current
    val runtime by ScreenRecorderRuntime.status.collectAsStateWithLifecycle()
    var showQuality by rememberSaveable { mutableStateOf(false) }
    var preset by rememberSaveable { mutableStateOf(RecorderPreset.BALANCED) }
    var includeMicrophone by rememberSaveable { mutableStateOf(false) }
    var countdown by rememberSaveable { mutableStateOf(0) }
    val microphonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        includeMicrophone = granted
        if (!granted) ScreenRecorderRuntime.update(ScreenRecordingPhase.IDLE, "Microphone permission was not granted. Video-only recording remains available.")
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val service = Intent(context, ScreenRecorderService::class.java).apply {
                action = ScreenRecorderService.ACTION_START
                putExtra(ScreenRecorderService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenRecorderService.EXTRA_DATA, result.data)
                putExtra(ScreenRecorderService.EXTRA_WIDTH_CAP, preset.widthCap)
                putExtra(ScreenRecorderService.EXTRA_FRAME_RATE, preset.frameRate)
                putExtra(ScreenRecorderService.EXTRA_BIT_RATE, preset.bitRate)
                putExtra(ScreenRecorderService.EXTRA_INCLUDE_MICROPHONE, includeMicrophone)
            }
            ContextCompat.startForegroundService(context, service)
        } else {
            ScreenRecorderRuntime.update(ScreenRecordingPhase.IDLE, "Screen capture permission was cancelled.")
        }
    }
    val isIdle = runtime.phase == ScreenRecordingPhase.IDLE
    val isRecording = runtime.phase == ScreenRecordingPhase.RECORDING
    val isPaused = runtime.phase == ScreenRecordingPhase.PAUSED

    LaunchedEffect(menuAction) {
        when (menuAction) {
            ScreenRecorderMenuAction.Start -> if (isIdle) countdown = 3
            ScreenRecorderMenuAction.Quality -> showQuality = true
            null -> return@LaunchedEffect
        }
        onMenuActionHandled()
    }

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1_000)
            if (countdown > 1) {
                countdown -= 1
            } else {
                countdown = 0
                val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                permission.launch(manager.createScreenCaptureIntent())
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .58f),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    LocalizedText("Screen Recorder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    LocalizedText(
                        "Record MP4 locally with Android's permission prompt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(170.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .58f),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Videocam,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(42.dp),
                        )
                        LocalizedText(
                            when {
                                isRecording -> "Recording"
                                isPaused -> "Recording paused"
                                countdown > 0 -> "Starting in $countdown"
                                else -> "Ready to start"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        LocalizedText(
                            if (isIdle) "${translateUiText(preset.label, LocalAppLanguage.current)} · ${preset.detail}" else runtime.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (isIdle) {
                    FilterChip(
                        selected = showQuality,
                        onClick = { showQuality = !showQuality },
                        label = { LocalizedText(if (showQuality) "Hide quality options" else "Quality: ${preset.label}") },
                    )
                    if (showQuality) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            RecorderPreset.entries.forEach { value ->
                                FilterChip(
                                    selected = preset == value,
                                    onClick = { preset = value },
                                    label = { LocalizedText(value.label) },
                                )
                            }
                        }
                        FilterChip(
                            selected = includeMicrophone,
                            onClick = {
                                if (includeMicrophone) {
                                    includeMicrophone = false
                                } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    includeMicrophone = true
                                } else {
                                    microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            leadingIcon = { Icon(Icons.Rounded.Mic, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            label = { LocalizedText("Include microphone") },
                        )
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        onClick = { countdown = 3 },
                        enabled = countdown == 0,
                    ) {
                        Icon(Icons.Rounded.Videocam, contentDescription = null)
                        LocalizedText(if (countdown > 0) " Starting in $countdown" else " Start recording")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f).height(48.dp),
                            onClick = {
                                context.startService(
                                    Intent(context, ScreenRecorderService::class.java).setAction(
                                        if (isPaused) ScreenRecorderService.ACTION_RESUME else ScreenRecorderService.ACTION_PAUSE,
                                    ),
                                )
                            },
                        ) { LocalizedText(if (isPaused) "Resume" else "Pause") }
                        Button(
                            modifier = Modifier.weight(1f).height(48.dp),
                            onClick = {
                                context.startService(Intent(context, ScreenRecorderService::class.java).setAction(ScreenRecorderService.ACTION_STOP))
                            },
                        ) {
                            Icon(Icons.Rounded.SaveAlt, contentDescription = null)
                            LocalizedText(" Stop & save")
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .42f),
        ) {
            LocalizedText(
                "Android asks for consent before recording. Videos are saved locally to Movies/PureHub.",
                modifier = Modifier.padding(14.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
