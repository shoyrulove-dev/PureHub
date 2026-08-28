package com.purehub.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import com.purehub.app.ui.LocalizedText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.purehub.app.feature.screenrecorder.ScreenRecorderRuntime
import com.purehub.app.feature.screenrecorder.ScreenRecorderService
import com.purehub.app.feature.screenrecorder.ScreenRecordingPhase

private enum class RecorderPreset(
    val label: String,
    val detail: String,
    val widthCap: Int,
    val frameRate: Int,
    val bitRate: Int,
) {
    EFFICIENT("Efficient", "720-wide · 30 fps · 4 Mbps", 720, 30, 4_000_000),
    BALANCED("Balanced", "1080-wide · 30 fps · 8 Mbps", 1080, 30, 8_000_000),
    SMOOTH("Smooth", "720-wide · 60 fps · 8 Mbps", 720, 60, 8_000_000),
}

@Composable
fun ScreenRecorderCard() {
    val context = LocalContext.current
    val runtime by ScreenRecorderRuntime.status.collectAsStateWithLifecycle()
    var mode by rememberSaveable { mutableStateOf(SuiteMode.QUICK) }
    var preset by rememberSaveable { mutableStateOf(RecorderPreset.BALANCED) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val service = Intent(context, ScreenRecorderService::class.java).apply {
                action = ScreenRecorderService.ACTION_START
                putExtra(ScreenRecorderService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenRecorderService.EXTRA_DATA, result.data)
                putExtra(ScreenRecorderService.EXTRA_WIDTH_CAP, preset.widthCap)
                putExtra(ScreenRecorderService.EXTRA_FRAME_RATE, preset.frameRate)
                putExtra(ScreenRecorderService.EXTRA_BIT_RATE, preset.bitRate)
            }
            ContextCompat.startForegroundService(context, service)
        } else {
            ScreenRecorderRuntime.update(ScreenRecordingPhase.IDLE, "Screen capture permission was cancelled.")
        }
    }
    val isIdle = runtime.phase == ScreenRecordingPhase.IDLE
    val isRecording = runtime.phase == ScreenRecordingPhase.RECORDING
    val isPaused = runtime.phase == ScreenRecordingPhase.PAUSED

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            FlagshipSuiteHeader(
                eyebrow = "Creator flagship",
                title = "Screen Recorder",
                description = "Capture local MP4 video with quality controls and Android's visible consent flow.",
            )
            SuiteModeSwitch(
                mode = mode,
                onModeChanged = { mode = it },
                proHint = "Choose recording quality, pause and resume without losing the active session.",
            )
            if (mode == SuiteMode.PRO && isIdle) {
                LocalizedText("Quality preset")
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
                LocalizedText(preset.detail, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            }
            LocalizedText(runtime.message)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                        permission.launch(manager.createScreenCaptureIntent())
                    },
                    enabled = isIdle,
                ) { LocalizedText(if (runtime.phase == ScreenRecordingPhase.PREPARING) "Preparing…" else "Start recording") }
                if (isRecording || isPaused) {
                    OutlinedButton(
                        onClick = {
                            context.startService(
                                Intent(context, ScreenRecorderService::class.java).setAction(
                                    if (isPaused) ScreenRecorderService.ACTION_RESUME else ScreenRecorderService.ACTION_PAUSE,
                                ),
                            )
                        },
                    ) { LocalizedText(if (isPaused) "Resume" else "Pause") }
                    OutlinedButton(
                        onClick = {
                            context.startService(Intent(context, ScreenRecorderService::class.java).setAction(ScreenRecorderService.ACTION_STOP))
                        },
                    ) { LocalizedText("Stop & save") }
                }
            }
            PrivacyReceipt(
                action = "Visible, local recording",
                detail = "Android always asks for consent and shows a foreground notification. PureHub has no INTERNET permission.",
            )
        }
    }
}
