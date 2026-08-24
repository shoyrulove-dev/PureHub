package com.purehub.app.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

private enum class CleanerView(val label: String) {
    OVERVIEW("Overview"), LARGE_FILES("Large files"), DUPLICATES("Duplicates"),
}

@Composable
fun CleanerScreen(
    innerPadding: PaddingValues,
    embedded: Boolean = false,
    viewModel: CleanerViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    var permissionMessage by remember { mutableStateOf<String?>(null) }
    var activeView by remember { mutableStateOf(CleanerView.OVERVIEW) }
    var confirmDelete by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.all { it }) {
            permissionMessage = null
            viewModel.startScan()
            scope.launch { snackbar.showSnackbar("Media access granted. Scan stays on this device.") }
        } else permissionMessage = "Allow visible media access to review large files and exact duplicate photos or videos."
    }
    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.clearSelection()
            viewModel.startScan()
            scope.launch { snackbar.showSnackbar("Android removed the files you approved.") }
        }
    }

    fun launchScan() {
        val permissions = cleanerPermissions()
        if (permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
            permissionMessage = null
            viewModel.startScan()
        } else permissionLauncher.launch(permissions.toTypedArray())
    }
    fun deleteApprovedFiles() {
        val uris = uiState.selectedFiles.map { it.contentUri }
        if (uris.isEmpty()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val request = MediaStore.createDeleteRequest(context.contentResolver, uris)
            deleteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
        } else {
            viewModel.deleteSelectedFiles()
            scope.launch { snackbar.showSnackbar("Approved files were submitted for deletion.") }
        }
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        icon = { Icon(Icons.Rounded.AutoDelete, null) },
        title = { Text("Remove ${uiState.selectedFiles.size} reviewed files?") },
        text = { Text("This can free ${formatBytes(uiState.selectedBytes)}. Android may show one more confirmation. PureHub never selects personal files silently.") },
        confirmButton = { Button(onClick = { confirmDelete = false; deleteApprovedFiles() }) { Text("Continue") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep files") } },
    )

    Column(Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!embedded) item {
                FlagshipSuiteHeader(
                    eyebrow = "Storage Care flagship",
                    title = "Deep Cleaner",
                    description = "Scan visible media locally, review exact evidence, and approve every deletion yourself.",
                )
            }
            item {
                CleanerDashboard(
                    uiState.isScanning, uiState.statusMessage, uiState.totalReclaimableBytes,
                    uiState.exactDuplicateBytes, uiState.duplicateGroups.size, uiState.largeFiles.size, ::launchScan,
                )
            }
            permissionMessage?.let { item { NoticeCard(it) } }
            uiState.errorMessage?.let { item { NoticeCard(it) } }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CleanerView.entries.forEach { view ->
                        FilterChip(activeView == view, { activeView = view }, { Text(view.label) })
                    }
                }
            }

            if (!uiState.isScanning && uiState.largeFiles.isEmpty() && uiState.duplicateGroups.isEmpty()) item {
                EmptyCleanerCard("Ready for a private scan", "PureHub only reads media Android lets you see. It does not inspect app data, messages, passwords, or system files.")
            }

            if (activeView != CleanerView.LARGE_FILES && uiState.duplicateGroups.isNotEmpty()) {
                item {
                    ReviewSectionHeader(
                        "Exact duplicates",
                        "${uiState.duplicateGroups.size} groups · ${formatBytes(uiState.exactDuplicateBytes)} safely reviewable",
                        "Select copies",
                        viewModel::selectExactDuplicates,
                    )
                }
                items(uiState.duplicateGroups, key = { "duplicate-${it.hash}" }) { group ->
                    DuplicateGroupCard(group, uiState.selectedFileIds, viewModel::toggleSelection)
                }
            }
            if (activeView != CleanerView.DUPLICATES && uiState.largeFiles.isNotEmpty()) {
                item { ReviewSectionHeader("Large media", "${uiState.largeFiles.size} files at least 100 MB", "Select all", viewModel::selectAllLargeFiles) }
                items(uiState.largeFiles, key = { "large-${it.id}" }) { file ->
                    CleanerFileCard(file, file.id in uiState.selectedFileIds) { viewModel.toggleSelection(file) }
                }
            }
            if (!uiState.isScanning && activeView == CleanerView.DUPLICATES && uiState.duplicateGroups.isEmpty()) item {
                EmptyCleanerCard("No exact copies", "No byte-for-byte duplicate photos or videos were found in visible media.")
            }
            if (!uiState.isScanning && activeView == CleanerView.LARGE_FILES && uiState.largeFiles.isEmpty()) item {
                EmptyCleanerCard("No large media", "No visible photo, video, or audio file is currently above 100 MB.")
            }
        }

        if (uiState.selectedFiles.isNotEmpty()) Surface(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            shape = RoundedCornerShape(20.dp), tonalElevation = 4.dp, shadowElevation = 6.dp,
        ) {
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${uiState.selectedFiles.size} reviewed", style = MaterialTheme.typography.labelMedium)
                    Text(formatBytes(uiState.selectedBytes), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                }
                OutlinedButton(onClick = viewModel::clearSelection) { Text("Clear") }
                Button(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.DeleteSweep, null); Text(" Remove") }
            }
        }
    }
}

@Composable
private fun CleanerDashboard(
    scanning: Boolean, status: String, reviewBytes: Long, duplicateBytes: Long,
    duplicateGroups: Int, largeFiles: Int, onScan: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(54.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (scanning) CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                    else Icon(Icons.Rounded.Storage, null, tint = MaterialTheme.colorScheme.primary)
                }
                Column(Modifier.weight(1f)) {
                    Text(if (reviewBytes > 0) formatBytes(reviewBytes) else "Storage review", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text(if (scanning) status else "Potential space · nothing auto-selected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(enabled = !scanning, onClick = onScan) { Icon(Icons.Rounded.Refresh, null); Text(if (scanning) " Scanning" else " Scan") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DashboardMetric(Modifier.weight(1f), Icons.Rounded.FileCopy, "$duplicateGroups", "Duplicate groups", formatBytes(duplicateBytes))
                DashboardMetric(Modifier.weight(1f), Icons.Rounded.Folder, "$largeFiles", "Large files", "100 MB+")
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Security, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Offline SHA-256 matching · Android confirms deletion", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DashboardMetric(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String, detail: String) {
    Surface(modifier, RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReviewSectionHeader(title: String, subtitle: String, action: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable private fun NoticeCard(message: String) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.errorContainer) {
        Text(message, Modifier.padding(14.dp), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable private fun EmptyCleanerCard(title: String, text: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp)); Text(title, fontWeight = FontWeight.Bold)
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CleanerFileCard(file: CleanerFileItem, selected: Boolean, toggle: () -> Unit) {
    val formatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }
    Card(Modifier.fillMaxWidth().clickable(onClick = toggle)) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(selected, { toggle() })
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(file.name.ifBlank { "Unnamed file" }, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${formatBytes(file.sizeBytes)} · ${friendlyType(file.mimeType)}", style = MaterialTheme.typography.bodySmall)
                Text("Updated ${Instant.ofEpochSecond(file.modifiedAtSeconds).atZone(ZoneId.systemDefault()).format(formatter)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DuplicateGroupCard(group: DuplicateImageGroup, selectedIds: Set<Long>, toggle: (CleanerFileItem) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${group.files.size} byte-for-byte matches", fontWeight = FontWeight.Bold)
                    Text("Review ${formatBytes(group.files.drop(1).sumOf { it.sizeBytes })} after keeping the newest", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text("SHA-256", Modifier.padding(horizontal = 8.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                }
            }
            group.files.forEachIndexed { index, file ->
                Row(Modifier.fillMaxWidth().clickable(enabled = index != 0) { toggle(file) }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(file.id in selectedIds, { if (index != 0) toggle(file) }, enabled = index != 0)
                    Column(Modifier.weight(1f)) {
                        Text(file.name.ifBlank { "Unnamed image" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(formatBytes(file.sizeBytes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (index == 0) Text("KEEP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

private fun cleanerPermissions() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO)
} else listOf(Manifest.permission.READ_EXTERNAL_STORAGE)

private fun friendlyType(mime: String) = when {
    mime.startsWith("image/") -> "Image"; mime.startsWith("video/") -> "Video"; mime.startsWith("audio/") -> "Audio"
    mime.isBlank() -> "Unknown type"; else -> mime
}

private fun formatBytes(size: Long): String {
    if (size <= 0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    val group = (kotlin.math.log10(size.toDouble()) / kotlin.math.log10(1024.0)).toInt().coerceIn(units.indices)
    return "${DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, group.toDouble()))} ${units[group]}"
}
