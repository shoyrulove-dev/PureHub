package com.purehub.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Smart Flashlight",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "Torch control, SOS, and Morse blinking all run directly on-device through Camera APIs.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { withCameraPermission { viewModel.toggleTorch() } }) {
                    Text(if (uiState.isTorchOn) "Turn Off" else "Turn On")
                }
                Button(onClick = viewModel::stopPattern) {
                    Text("Stop Pattern")
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(
                    onClick = { withCameraPermission { viewModel.playSosPattern() } },
                    label = { Text("SOS") },
                )
                AssistChip(
                    onClick = { withCameraPermission { viewModel.playMorsePattern("HELP") } },
                    label = { Text("HELP") },
                )
                AssistChip(
                    onClick = { withCameraPermission { viewModel.playMorsePattern("PURE HUB") } },
                    label = { Text("PURE HUB") },
                )
            }
            OutlinedTextField(
                value = morseText,
                onValueChange = { morseText = it.uppercase() },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Custom Morse text") },
                supportingText = { Text("A-Z and spaces only.") },
                singleLine = true,
            )
            Button(
                onClick = { withCameraPermission { viewModel.playMorsePattern(morseText) } },
                enabled = morseText.isNotBlank(),
            ) {
                Text("Play Morse")
            }
            Text(
                text = uiState.statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            uiState.errorMessage?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
