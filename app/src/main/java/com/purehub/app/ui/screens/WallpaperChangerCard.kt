package com.purehub.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.purehub.app.feature.wallpaper.WallpaperChangerViewModel
import com.purehub.app.feature.wallpaper.WallpaperRotationRepository
import com.purehub.app.ui.LocalSnackbarHostState
import kotlinx.coroutines.launch

@Composable
fun WallpaperChangerCard() {
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val viewModel: WallpaperChangerViewModel = viewModel(
        factory = WallpaperChangerViewModel.factory(
            repository = WallpaperRotationRepository(context.applicationContext),
        ),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            viewModel.setSelectedUris(uris.map(Uri::toString))
            scope.launch { snackbarHostState.showSnackbar("Wallpaper sources saved locally.") }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Wallpaper Changer",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = uiState.note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = { pickerLauncher.launch(arrayOf("image/*")) }) {
                Text("Choose Local Images")
            }
            Text(
                text = "${uiState.selectedUris.size} image(s) selected",
                style = MaterialTheme.typography.bodyMedium,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(12, 24, 48).forEach { hours ->
                    AssistChip(
                        onClick = { viewModel.updateRotationHours(hours) },
                        label = {
                            Text(if (uiState.rotationHours == hours) "$hours h • Selected" else "$hours h")
                        },
                    )
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = {
                        viewModel.applyNow()
                        scope.launch { snackbarHostState.showSnackbar("Applied next wallpaper from local set.") }
                    },
                    enabled = uiState.selectedUris.isNotEmpty(),
                ) {
                    Text("Apply Now")
                }
                Button(
                    onClick = {
                        viewModel.scheduleRotation()
                        scope.launch { snackbarHostState.showSnackbar("Wallpaper rotation scheduled.") }
                    },
                    enabled = uiState.selectedUris.isNotEmpty(),
                ) {
                    Text(if (uiState.rotationEnabled) "Reschedule" else "Start Rotation")
                }
                Button(
                    onClick = {
                        viewModel.cancelRotation()
                        scope.launch { snackbarHostState.showSnackbar("Wallpaper rotation stopped.") }
                    },
                    enabled = uiState.rotationEnabled,
                ) {
                    Text("Stop Rotation")
                }
            }
        }
    }
}
