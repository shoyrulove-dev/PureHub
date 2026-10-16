package com.purehub.app.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import com.purehub.app.ui.LocalizedText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.purehub.app.feature.flashlight.SmartFlashlightViewModel
import com.purehub.app.ui.LocalSnackbarHostState
import kotlinx.coroutines.launch

@Composable
fun SmartFlashlightCard(
    viewModel: SmartFlashlightViewModel = viewModel(),
) {
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var morseText by remember { mutableStateOf("PURE HUB") }
    var screenLightOn by remember { mutableStateOf(false) }
    var screenLightColor by remember { mutableStateOf(Color.White) }
    var screenLightBrightness by remember { mutableStateOf(100f) }
    val hasCameraPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA,
    ) == PackageManager.PERMISSION_GRANTED
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        scope.launch {
            snackbarHostState.showSnackbar(
                if (granted) "Camera permission granted for local torch control." else "Torch control needs camera permission on Android.",
            )
        }
    }

    fun withCameraPermission(action: () -> Unit) {
        if (hasCameraPermission) {
            action()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(screenLightOn, screenLightBrightness) {
        val activity = context as? Activity
        val previous = activity?.window?.attributes?.screenBrightness
        if (screenLightOn && activity != null) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity.window.attributes = activity.window.attributes.apply {
                screenBrightness = (screenLightBrightness / 100f).coerceIn(.05f, 1f)
            }
        }
        onDispose {
            if (activity != null && previous != null) {
                activity.window.attributes = activity.window.attributes.apply { screenBrightness = previous }
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FlagshipSuiteHeader(
                eyebrow = "Light Suite flagship",
                title = "Smart Flashlight",
                description = "Reliable torch control, emergency SOS and custom Morse patterns, processed entirely on your device.",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { withCameraPermission { viewModel.toggleTorch() } }) {
                    LocalizedText(if (uiState.isTorchOn) "Turn Off" else "Turn On")
                }
                Button(onClick = viewModel::stopPattern) {
                    LocalizedText("Stop Pattern")
                }
            }
            if (uiState.maxTorchStrength > 1) {
                Text("Torch brightness ${uiState.torchStrength}/${uiState.maxTorchStrength}", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = uiState.torchStrength.toFloat(),
                    onValueChange = { viewModel.setTorchStrength(it.toInt()) },
                    valueRange = 1f..uiState.maxTorchStrength.toFloat(),
                    steps = (uiState.maxTorchStrength - 2).coerceAtLeast(0),
                )
            }
            Text("Strobe speed ${uiState.strobeHz} Hz", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = uiState.strobeHz.toFloat(),
                onValueChange = { viewModel.setStrobeHz(it.toInt()) },
                valueRange = 1f..30f,
                steps = 28,
            )
            Button(onClick = { withCameraPermission(viewModel::playStrobe) }, modifier = Modifier.fillMaxWidth()) {
                LocalizedText("Start Strobe")
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(
                    onClick = { withCameraPermission { viewModel.playSosPattern() } },
                    label = { LocalizedText("SOS") },
                )
                AssistChip(
                    onClick = { withCameraPermission { viewModel.playMorsePattern("HELP") } },
                    label = { LocalizedText("HELP") },
                )
                AssistChip(
                    onClick = { withCameraPermission { viewModel.playMorsePattern("PURE HUB") } },
                    label = { LocalizedText("PURE HUB") },
                )
            }
            OutlinedTextField(
                value = morseText,
                onValueChange = { morseText = it.uppercase() },
                modifier = Modifier.fillMaxWidth(),
                label = { LocalizedText("Custom Morse text") },
                supportingText = { LocalizedText("A-Z and spaces only.") },
                singleLine = true,
            )
            Button(
                onClick = { withCameraPermission { viewModel.playMorsePattern(morseText) } },
                enabled = morseText.isNotBlank(),
            ) {
                LocalizedText("Play Morse")
            }
            Text("Auto-off", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 1, 5, 15, 30).forEach { minutes ->
                    AssistChip(
                        onClick = { viewModel.setAutoOffMinutes(minutes) },
                        label = { Text(if (minutes == 0) "Off" else "$minutes min") },
                    )
                }
            }
            Text("Screen light", style = MaterialTheme.typography.labelLarge)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (screenLightOn) screenLightColor else MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp),
            ) {
                Button(onClick = { screenLightOn = !screenLightOn }) {
                    Text(if (screenLightOn) "Turn screen light off" else "Turn screen light on")
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("White" to Color.White, "Red" to Color(0xFFFF4D5E), "Amber" to Color(0xFFFFB300), "Blue" to Color(0xFF4DA3FF)).forEach { (name, color) ->
                    AssistChip(onClick = { screenLightColor = color }, label = { Text(name) })
                }
            }
            Text("Screen brightness ${screenLightBrightness.toInt()}%", style = MaterialTheme.typography.labelLarge)
            Slider(value = screenLightBrightness, onValueChange = { screenLightBrightness = it }, valueRange = 15f..100f, steps = 16)
            LocalizedText(
                text = uiState.statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
