package com.purehub.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.purehub.app.feature.screenrecorder.ScreenRecorderService

@Composable
fun ScreenRecorderCard() {
    val context = LocalContext.current
    var recording by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Recordings are saved locally to Movies/PureHub.") }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val service = Intent(context, ScreenRecorderService::class.java).apply {
                action = ScreenRecorderService.ACTION_START
                putExtra(ScreenRecorderService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenRecorderService.EXTRA_DATA, result.data)
            }
            ContextCompat.startForegroundService(context, service)
            recording = true; status = "Recording. Use Stop here or in the notification."
        } else status = "Screen capture permission was cancelled."
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FlagshipSuiteHeader("Creator flagship", "Screen Recorder", "Capture your screen to a local MP4 with Android's visible system consent flow.")
            Text(status)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    permission.launch(manager.createScreenCaptureIntent())
                }, enabled = !recording) { Text("Start recording") }
                OutlinedButton(onClick = {
                    context.startService(Intent(context, ScreenRecorderService::class.java).setAction(ScreenRecorderService.ACTION_STOP))
                    recording = false; status = "Saved to Movies/PureHub."
                }, enabled = recording) { Text("Stop & save") }
            }
            Text("Android always shows a permission dialog and foreground notification. PureHub cannot record secretly.", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        }
    }
}
