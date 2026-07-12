package com.purehub.app.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.purehub.app.feature.cleaner.CleanerFileItem
import com.purehub.app.feature.cleaner.CleanerViewModel
import com.purehub.app.feature.cleaner.DuplicateImageGroup
import com.purehub.app.ui.LocalSnackbarHostState
import java.text.DecimalFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@Composable
fun CleanerScreen(
    innerPadding: PaddingValues,
    embedded: Boolean = false,
    viewModel: CleanerViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    var permissionMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (result.values.all { it }) {
            permissionMessage = null
            viewModel.startScan()
            scope.launch { snackbarHostState.showSnackbar("Media permissions granted for local cleaner scan.") }
        } else {
            permissionMessage = "Media permissions are needed to scan large files and detect duplicate images locally."
            scope.launch { snackbarHostState.showSnackbar("Cleaner scan skipped because media permission was declined.") }
        }
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.clearSelection()
            viewModel.startScan()
            scope.launch { snackbarHostState.showSnackbar("Selected files deleted.") }
        }
    }

    fun launchScan() {
        val permissions = cleanerPermissions()
        val allGranted = permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            permissionMessage = null
            viewModel.startScan()
        } else {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    fun requestDelete() {
        val selectedUris = uiState.selectedFiles.map { it.contentUri }
        if (selectedUris.isEmpty()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, selectedUris)
            deleteLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
        } else {
            viewModel.deleteSelectedFiles()
        }
    }

    Column(
        modifier = Modifier
            .then(if (embedded) Modifier.fillMaxSize() else Modifier.fillMaxSize())
            .padding(innerPadding)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!embedded) {
                    Text(
                        text = "Deep Cleaner",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "PureHub scans visible media only, on-device and offline, without MANAGE_EXTERNAL_STORAGE.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = ::launchScan,
                ) {
                    Text(if (uiState.isScanning) "Scanning..." else "Start Offline Scan")
                }
                if (uiState.isScanning) {
                    CircularProgressIndicator()
                }
                Text(
                    text = "Status: ${uiState.statusMessage}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                permissionMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                uiState.errorMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (!uiState.isScanning) {
                    Text(
                        text = "Potential review size: ${formatBytes(uiState.totalReclaimableBytes)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    if (uiState.selectedFiles.isNotEmpty()) {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = ::requestDelete,
                        ) {
                            Text("Delete Selected (${uiState.selectedFiles.size})")
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionHeader(
                    title = "Large Files",
                    subtitle = "${uiState.largeFiles.size} items",
                )
            }
            if (uiState.largeFiles.isEmpty()) {
                item {
                    EmptyCleanerCard("No large media files found. Try another scan after adding more photos, videos, or audio.")
                }
            } else {
                items(uiState.largeFiles, key = { it.id }) { file ->
                    CleanerFileCard(
                        file = file,
                        isSelected = file.id in uiState.selectedFileIds,
                        onToggleSelection = { viewModel.toggleSelection(file) },
                    )
                }
            }

            item {
                SectionHeader(
                    title = "Duplicate Images",
                    subtitle = "${uiState.duplicateGroups.size} groups",
                )
            }
            if (uiState.duplicateGroups.isEmpty()) {
                item {
                    EmptyCleanerCard("No duplicate image groups found. Your photo library already looks pretty lean.")
                }
            } else {
                items(uiState.duplicateGroups, key = { it.hash }) { group ->
                    DuplicateGroupCard(
                        group = group,
                        selectedIds = uiState.selectedFileIds,
                        onToggleSelection = { viewModel.toggleSelection(it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyCleanerCard(
    text: String,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.padding(16.dp),
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CleanerFileCard(
    file: CleanerFileItem,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
) {
    val formatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() },
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = file.name.ifBlank { "Unnamed file" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "${formatBytes(file.sizeBytes)} • ${file.mimeType.ifBlank { "Unknown type" }}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Updated ${Instant.ofEpochSecond(file.modifiedAtSeconds).atZone(ZoneId.systemDefault()).format(formatter)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DuplicateGroupCard(
    group: DuplicateImageGroup,
    selectedIds: Set<Long>,
    onToggleSelection: (CleanerFileItem) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "${group.files.size} matching images",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "Keep one, review ${formatBytes(group.files.drop(1).sumOf { it.sizeBytes })} reclaimable",
                style = MaterialTheme.typography.bodyMedium,
            )
            group.files.forEachIndexed { index, file ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Checkbox(
                        checked = file.id in selectedIds,
                        onCheckedChange = { if (index != 0) onToggleSelection(file) },
                        enabled = index != 0,
                    )
                    Text(
                        text = buildString {
                            append(file.name.ifBlank { "Unnamed" })
                            if (index == 0) append(" • suggested keep")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun cleanerPermissions(): List<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
        )
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

private fun formatBytes(size: Long): String {
    if (size <= 0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (kotlin.math.log10(size.toDouble()) / kotlin.math.log10(1024.0)).toInt()
    val value = size / Math.pow(1024.0, digitGroups.toDouble())
    return "${DecimalFormat("#,##0.#").format(value)} ${units[digitGroups]}"
}
