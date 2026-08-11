package com.purehub.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.purehub.app.feature.qr.QrDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.min

private enum class QrStudioTab(val label: String) { Scan("Scan"), Create("Create"), Library("Library") }
private enum class QrTemplate(val label: String) {
    Website("Website"),
    Text("Text"),
    Wifi("Wi-Fi"),
    Email("Email"),
    Phone("Phone"),
    Contact("Contact"),
}

private data class QrHistoryItem(val value: String, val source: String, val savedAt: String)
private data class QrPayloadInfo(val kind: String, val action: String = "", val destination: String = "", val warning: String = "")
private data class QrCreatorFields(val primary: String, val secondary: String = "", val tertiary: String = "")

@Composable
fun QrStudioScreen(
    hasCameraPermission: Boolean,
    onRequestCameraPermission: () -> Unit,
    innerPadding: PaddingValues = PaddingValues(0.dp),
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val preferences = remember { context.getSharedPreferences("purehub.qr-studio.v2", 0) }
    var selectedTab by rememberSaveable { mutableStateOf(QrStudioTab.Scan) }
    var selectedTemplate by rememberSaveable { mutableStateOf(QrTemplate.Website) }
    var creatorPrimary by rememberSaveable { mutableStateOf("https://hub.blissbiovn.com") }
    var creatorSecondary by rememberSaveable { mutableStateOf("") }
    var creatorTertiary by rememberSaveable { mutableStateOf("") }
    var latestScan by rememberSaveable { mutableStateOf("") }
    var scanSource by rememberSaveable { mutableStateOf("Camera") }
    var scanStatus by rememberSaveable { mutableStateOf("Ready. Codes are processed only on this device.") }
    var history by remember { mutableStateOf(loadQrHistory(preferences.getString("history", "[]") ?: "[]")) }
    val qrText = remember(selectedTemplate, creatorPrimary, creatorSecondary, creatorTertiary) {
        buildQrPayload(selectedTemplate, QrCreatorFields(creatorPrimary, creatorSecondary, creatorTertiary))
    }
    val qrBitmap = remember(qrText, creatorPrimary) {
        if (creatorPrimary.isBlank()) null else com.purehub.app.feature.qr.QrBitmapGenerator.generate(qrText)
    }
    val payloadInfo = remember(latestScan) { describeQrPayload(latestScan) }

    fun saveHistory(value: String, source: String) {
        if (value.isBlank()) return
        history = (listOf(QrHistoryItem(value.trim(), source, Instant.now().toString())) + history.filter { it.value != value.trim() }).take(24)
        preferences.edit().putString("history", encodeQrHistory(history)).apply()
    }

    fun acceptScan(value: String, source: String) {
        if (value.isBlank() || value == latestScan) return
        latestScan = value
        scanSource = source
        scanStatus = "$source scan complete. Review the result before taking action."
        saveHistory(value, source)
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scanStatus = "Reading the selected image locally..."
        scope.launch {
            val result = withContext(Dispatchers.Default) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
                        ?.let(QrDecoder::decode)
                }
            }
            result.onSuccess { value ->
                if (value.isNullOrBlank()) scanStatus = "No readable QR or barcode was found in that image."
                else acceptScan(value, "Image")
            }.onFailure { scanStatus = "That image could not be opened." }
        }
    }
    val batchPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scanStatus = "Reading ${uris.take(20).size} images locally..."
        scope.launch {
            var found = 0
            uris.take(20).forEach { uri ->
                val value = withContext(Dispatchers.Default) {
                    runCatching { context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)?.let(QrDecoder::decode) }.getOrNull()
                }
                if (!value.isNullOrBlank()) { acceptScan(value, "Batch"); found += 1 }
            }
            scanStatus = "$found QR or barcode result(s) found in ${uris.take(20).size} images and saved locally."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        QrStudioHeader(selectedTab = selectedTab, onTabSelected = { selectedTab = it })

        when (selectedTab) {
            QrStudioTab.Scan -> QrScannerContent(
                hasCameraPermission = hasCameraPermission,
                onRequestCameraPermission = onRequestCameraPermission,
                onChooseImage = { imagePicker.launch("image/*") },
                onChooseBatch = { batchPicker.launch(arrayOf("image/*")) },
                latestScan = latestScan,
                scanSource = scanSource,
                scanStatus = scanStatus,
                payloadInfo = payloadInfo,
                onCodeDetected = { acceptScan(it, "Camera") },
                onClearResult = {
                    latestScan = ""
                    scanStatus = "Ready for another scan."
                },
            )

            QrStudioTab.Create -> QrCreatorContent(
                selectedTemplate = selectedTemplate,
                primary = creatorPrimary,
                secondary = creatorSecondary,
                tertiary = creatorTertiary,
                qrBitmap = qrBitmap,
                onTemplateSelected = {
                    selectedTemplate = it
                    defaultQrFields(it).also { fields ->
                        creatorPrimary = fields.primary
                        creatorSecondary = fields.secondary
                        creatorTertiary = fields.tertiary
                    }
                },
                onPrimaryChanged = { creatorPrimary = it },
                onSecondaryChanged = { creatorSecondary = it },
                onTertiaryChanged = { creatorTertiary = it },
                onSave = { saveHistory(qrText, "Created") },
            )

            QrStudioTab.Library -> QrLibraryContent(
                history = history,
                onSelect = {
                    latestScan = it.value
                    scanSource = it.source
                    scanStatus = "Saved item opened from your private library."
                    selectedTab = QrStudioTab.Scan
                },
                onClear = {
                    history = emptyList()
                    preferences.edit().remove("history").apply()
                },
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun QrStudioHeader(selectedTab: QrStudioTab, onTabSelected: (QrStudioTab) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.secondaryContainer)))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.onPrimaryContainer) {
                    Icon(Icons.Rounded.QrCode2, null, modifier = Modifier.padding(12.dp), tint = MaterialTheme.colorScheme.primaryContainer)
                }
                Column(Modifier.weight(1f)) {
                    Text("PRIVATE BY DESIGN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text("QR Studio", style = MaterialTheme.typography.headlineSmall)
                    Text("Fast scans, safe previews, zero ads.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                QrStudioTab.entries.forEach { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        label = { Text(tab.label) },
                        leadingIcon = {
                            Icon(
                                when (tab) {
                                    QrStudioTab.Scan -> Icons.Rounded.QrCodeScanner
                                    QrStudioTab.Create -> Icons.Rounded.AutoAwesome
                                    QrStudioTab.Library -> Icons.Rounded.History
                                },
                                null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun QrScannerContent(
    hasCameraPermission: Boolean,
    onRequestCameraPermission: () -> Unit,
    onChooseImage: () -> Unit,
    onChooseBatch: () -> Unit,
    latestScan: String,
    scanSource: String,
    scanStatus: String,
    payloadInfo: QrPayloadInfo,
    onCodeDetected: (String) -> Unit,
    onClearResult: () -> Unit,
) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (hasCameraPermission) {
                QrCameraPreview(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    scanningEnabled = latestScan.isBlank(),
                    onCodeDetected = onCodeDetected,
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp)).background(Color(0xFF07111F)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.QrCodeScanner, null, tint = Color(0xFF6EE7B7), modifier = Modifier.size(48.dp))
                        Text("Camera stays off until you allow it", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Button(onClick = onRequestCameraPermission) { Text("Allow camera") }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onChooseImage, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.AddPhotoAlternate, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(7.dp))
                    Text("Scan image")
                }
                OutlinedButton(onClick = onChooseBatch, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.AddPhotoAlternate, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(7.dp))
                    Text("Batch")
                }
            }
            if (latestScan.isNotBlank()) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(onClick = onClearResult, modifier = Modifier.fillMaxWidth()) { Text("Scan another") }
                }
            }
            Text(scanStatus, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (latestScan.isNotBlank()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.size(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(payloadInfo.kind, style = MaterialTheme.typography.titleMedium)
                        Text("$scanSource · processed locally", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Rounded.Security, "Private", tint = MaterialTheme.colorScheme.primary)
                }
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .78f)) {
                    Text(latestScan, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                }
                if (payloadInfo.warning.isNotBlank()) Text(payloadInfo.warning, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = {
                        context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("QR result", latestScan))
                    }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.size(6.dp)); Text("Copy")
                    }
                    OutlinedButton(onClick = { shareText(context, latestScan) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.IosShare, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.size(6.dp)); Text("Share")
                    }
                }
                if (payloadInfo.destination.isNotBlank()) {
                    Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(payloadInfo.destination))) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.size(7.dp)); Text(payloadInfo.action)
                    }
                }
            }
        }
    }
}

@Composable
private fun QrCreatorContent(
    selectedTemplate: QrTemplate,
    primary: String,
    secondary: String,
    tertiary: String,
    qrBitmap: Bitmap?,
    onTemplateSelected: (QrTemplate) -> Unit,
    onPrimaryChanged: (String) -> Unit,
    onSecondaryChanged: (String) -> Unit,
    onTertiaryChanged: (String) -> Unit,
    onSave: () -> Unit,
) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Create a code", style = MaterialTheme.typography.titleLarge)
            Text("Pick a format and fill in only the information people need.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QrTemplate.entries.forEach { template ->
                    AssistChip(onClick = { onTemplateSelected(template) }, label = { Text(template.label) }, leadingIcon = {
                        if (template == selectedTemplate) Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(17.dp))
                    })
                }
            }
            when (selectedTemplate) {
                QrTemplate.Website -> CreatorField("Website address", primary, onPrimaryChanged, "https://example.com")
                QrTemplate.Text -> CreatorField("Text", primary, onPrimaryChanged, "Write something useful", minLines = 3)
                QrTemplate.Wifi -> {
                    CreatorField("Network name", primary, onPrimaryChanged, "Wi-Fi name")
                    CreatorField("Password", secondary, onSecondaryChanged, "Wi-Fi password")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("WPA", "WEP", "None").forEach { security ->
                            FilterChip(
                                selected = tertiary == security,
                                onClick = { onTertiaryChanged(security) },
                                label = { Text(security) },
                            )
                        }
                    }
                }
                QrTemplate.Email -> {
                    CreatorField("Email address", primary, onPrimaryChanged, "hello@example.com")
                    CreatorField("Subject", secondary, onSecondaryChanged, "Hello")
                }
                QrTemplate.Phone -> CreatorField("Phone number", primary, onPrimaryChanged, "+1 000 000 0000")
                QrTemplate.Contact -> {
                    CreatorField("Name", primary, onPrimaryChanged, "Full name")
                    CreatorField("Phone", secondary, onSecondaryChanged, "+1 000 000 0000")
                    CreatorField("Email", tertiary, onTertiaryChanged, "hello@example.com")
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Security, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(7.dp))
                Text("Created offline with reliable error correction", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            qrBitmap?.let { bitmap ->
                Surface(shape = RoundedCornerShape(24.dp), color = Color.White, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Image(bitmap.asImageBitmap(), "Generated QR code", modifier = Modifier.size(230.dp).padding(12.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { shareQrBitmap(context, bitmap) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.IosShare, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text("Share PNG")
                    }
                    OutlinedButton(onClick = onSave, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.History, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text("Save")
                    }
                }
            }
            if (qrBitmap == null) {
                Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column(
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Rounded.QrCode2, null, modifier = Modifier.size(34.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.size(8.dp))
                        Text("Complete the first field to preview", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatorField(
    label: String,
    value: String,
    onValueChanged: (String) -> Unit,
    placeholder: String,
    minLines: Int = 1,
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = onValueChanged,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        minLines = minLines,
        shape = RoundedCornerShape(16.dp),
        singleLine = minLines == 1,
    )
}

@Composable
private fun QrLibraryContent(history: List<QrHistoryItem>, onSelect: (QrHistoryItem) -> Unit, onClear: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Private library", style = MaterialTheme.typography.titleLarge)
                    Text("Saved only on this phone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (history.isNotEmpty()) IconButton(onClick = onClear) { Icon(Icons.Rounded.DeleteOutline, "Clear library", tint = MaterialTheme.colorScheme.error) }
            }
            if (history.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.History, null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.outline)
                        Text("No saved codes yet", style = MaterialTheme.typography.titleMedium)
                        Text("Your scans and saved creations appear here.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else history.forEachIndexed { index, item ->
                Surface(onClick = { onSelect(item) }, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (item.source == "Created") Icons.Rounded.QrCode2 else Icons.Rounded.QrCodeScanner, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.value, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
                            Text("${item.source} · ${item.savedAt.take(16).replace('T', ' ')}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (index < history.lastIndex) HorizontalDivider(color = Color.Transparent)
            }
        }
    }
}

@Composable
private fun QrCameraPreview(
    modifier: Modifier,
    scanningEnabled: Boolean,
    onCodeDetected: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scannerExecutor = remember { Executors.newSingleThreadExecutor() }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torchEnabled by rememberSaveable { mutableStateOf(false) }
    var zoomRatio by rememberSaveable { mutableStateOf(1f) }
    val currentScanningEnabled by rememberUpdatedState(scanningEnabled)
    val currentOnCodeDetected by rememberUpdatedState(onCodeDetected)
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }

    Box(modifier = modifier.clip(RoundedCornerShape(24.dp)).background(Color(0xFF07111F))) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        QrViewfinderOverlay(Modifier.fillMaxSize())
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color.Black.copy(alpha = .48f),
            modifier = Modifier.align(Alignment.TopStart).padding(14.dp),
        ) {
            Row(modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Security, null, tint = Color(0xFF6EE7B7), modifier = Modifier.size(15.dp))
                Spacer(Modifier.size(6.dp))
                Text("On-device", color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
        }
        if (camera != null) {
            Surface(
                onClick = {
                    val maxZoom = camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 1f
                    zoomRatio = if (zoomRatio > 1f) 1f else min(2f, maxZoom)
                    camera?.cameraControl?.setZoomRatio(zoomRatio)
                },
                shape = RoundedCornerShape(999.dp),
                color = Color.Black.copy(alpha = .48f),
                modifier = Modifier.align(Alignment.TopEnd).padding(14.dp),
            ) {
                Text(if (zoomRatio > 1f) "2×" else "1×", color = Color.White, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            }
        }
        if (camera?.cameraInfo?.hasFlashUnit() == true) {
            FilledTonalButton(
                onClick = {
                    torchEnabled = !torchEnabled
                    camera?.cameraControl?.enableTorch(torchEnabled)
                },
                modifier = Modifier.align(Alignment.BottomCenter).padding(14.dp),
            ) {
                Icon(if (torchEnabled) Icons.Rounded.FlashlightOff else Icons.Rounded.FlashlightOn, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(if (torchEnabled) "Torch off" else "Torch")
            }
        }
    }

    LaunchedEffect(previewView) {
        val provider = ProcessCameraProvider.getInstance(context).get()
        cameraProvider = provider
        camera = bindQrCamera(
            context,
            previewView,
            provider,
            lifecycleOwner,
            { value -> if (currentScanningEnabled) currentOnCodeDetected(value) },
            scannerExecutor,
        )
    }
    DisposableEffect(lifecycleOwner) {
        onDispose {
            cameraProvider?.unbindAll()
            scannerExecutor.shutdownNow()
        }
    }
}

@Composable
private fun QrViewfinderOverlay(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "QR scan line")
    val scanProgress by transition.animateFloat(
        initialValue = .08f,
        targetValue = .92f,
        animationSpec = infiniteRepeatable(animation = tween(1600), repeatMode = RepeatMode.Reverse),
        label = "QR scan progress",
    )
    Canvas(modifier) {
        val side = size.minDimension * .58f
        val left = (size.width - side) / 2
        val top = (size.height - side) / 2
        val length = side * .18f
        val color = Color(0xFF6EE7B7)
        val width = 7.dp.toPx()
        val shade = Color.Black.copy(alpha = .34f)
        drawRect(shade, topLeft = Offset.Zero, size = androidx.compose.ui.geometry.Size(size.width, top))
        drawRect(shade, topLeft = Offset(0f, top + side), size = androidx.compose.ui.geometry.Size(size.width, size.height - top - side))
        drawRect(shade, topLeft = Offset(0f, top), size = androidx.compose.ui.geometry.Size(left, side))
        drawRect(shade, topLeft = Offset(left + side, top), size = androidx.compose.ui.geometry.Size(size.width - left - side, side))
        listOf(
            Offset(left, top) to Offset(left + length, top), Offset(left, top) to Offset(left, top + length),
            Offset(left + side, top) to Offset(left + side - length, top), Offset(left + side, top) to Offset(left + side, top + length),
            Offset(left, top + side) to Offset(left + length, top + side), Offset(left, top + side) to Offset(left, top + side - length),
            Offset(left + side, top + side) to Offset(left + side - length, top + side), Offset(left + side, top + side) to Offset(left + side, top + side - length),
        ).forEach { (start, end) -> drawLine(color, start, end, width, StrokeCap.Round) }
        val scanY = top + side * scanProgress
        drawLine(
            color.copy(alpha = .9f),
            Offset(left + side * .09f, scanY),
            Offset(left + side * .91f, scanY),
            2.dp.toPx(),
            StrokeCap.Round,
        )
    }
}

private fun bindQrCamera(
    context: Context,
    previewView: PreviewView,
    cameraProvider: ProcessCameraProvider,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onCodeDetected: (String) -> Unit,
    scannerExecutor: ExecutorService,
): Camera {
    val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
    val analyzer = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .also { analysis ->
            val mainExecutor = ContextCompat.getMainExecutor(context)
            analysis.setAnalyzer(scannerExecutor) { imageProxy ->
                processQrFrame(imageProxy)?.let { value -> mainExecutor.execute { onCodeDetected(value) } }
            }
        }
    cameraProvider.unbindAll()
    return cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analyzer)
}

private fun processQrFrame(imageProxy: ImageProxy): String? {
    return try {
        QrDecoder.decode(imageProxy)
    } finally {
        imageProxy.close()
    }
}

private fun describeQrPayload(value: String): QrPayloadInfo {
    val trimmed = value.trim()
    if (trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)) {
        return runCatching {
            val uri = Uri.parse(trimmed)
            val host = uri.host.orEmpty()
            val risks = buildList {
                if (uri.scheme != "https") add("The link is not encrypted (HTTP).")
                if (host.startsWith("xn--")) add("The domain uses an internationalized/punycode name.")
                if (host.matches(Regex("\\d{1,3}(\\.\\d{1,3}){3}"))) add("The destination uses a raw IP address.")
                if (!uri.userInfo.isNullOrBlank()) add("The link embeds sign-in information.")
                if (uri.port !in listOf(-1, 80, 443)) add("The link uses unusual port ${uri.port}.")
                if (trimmed.length > 500) add("The destination is unusually long.")
            }
            val warning = risks.joinToString(" ")
            QrPayloadInfo("Website", "Open website", trimmed, warning)
        }.getOrElse { QrPayloadInfo("Invalid link", warning = "This web address is not valid.") }
    }
    return when {
        trimmed.startsWith("WIFI:", true) -> QrPayloadInfo("Wi-Fi network", warning = "The password is visible in the raw QR content.")
        trimmed.startsWith("mailto:", true) -> QrPayloadInfo("Email", "Open email", trimmed)
        trimmed.startsWith("tel:", true) -> QrPayloadInfo("Phone number", "Open dialer", trimmed)
        trimmed.startsWith("sms:", true) -> QrPayloadInfo("Message", "Open messages", trimmed)
        trimmed.startsWith("geo:", true) -> QrPayloadInfo("Location", "Open map", trimmed)
        trimmed.startsWith("MECARD:", true) || trimmed.startsWith("BEGIN:VCARD", true) -> QrPayloadInfo("Contact card")
        else -> QrPayloadInfo("Plain text")
    }
}

private fun defaultQrFields(template: QrTemplate): QrCreatorFields = when (template) {
    QrTemplate.Website -> QrCreatorFields("https://hub.blissbiovn.com")
    QrTemplate.Text -> QrCreatorFields("PureHub — free, private, and ad-free tools")
    QrTemplate.Wifi -> QrCreatorFields("", "", "WPA")
    QrTemplate.Email -> QrCreatorFields("", "")
    QrTemplate.Phone -> QrCreatorFields("")
    QrTemplate.Contact -> QrCreatorFields("", "", "")
}

private fun buildQrPayload(template: QrTemplate, fields: QrCreatorFields): String = when (template) {
    QrTemplate.Website, QrTemplate.Text -> fields.primary.trim()
    QrTemplate.Wifi -> {
        val security = fields.tertiary.takeUnless { it == "None" }.orEmpty()
        "WIFI:T:${escapeQrField(security)};S:${escapeQrField(fields.primary)};P:${escapeQrField(fields.secondary)};H:false;;"
    }
    QrTemplate.Email -> "mailto:${fields.primary.trim()}?subject=${Uri.encode(fields.secondary.trim())}"
    QrTemplate.Phone -> "tel:${fields.primary.filterNot(Char::isWhitespace)}"
    QrTemplate.Contact -> buildString {
        append("MECARD:N:").append(escapeQrField(fields.primary))
        if (fields.secondary.isNotBlank()) append(";TEL:").append(escapeQrField(fields.secondary))
        if (fields.tertiary.isNotBlank()) append(";EMAIL:").append(escapeQrField(fields.tertiary))
        append(";;")
    }
}

private fun escapeQrField(value: String): String = value.trim()
    .replace("\\", "\\\\")
    .replace(";", "\\;")
    .replace(",", "\\,")
    .replace(":", "\\:")

private fun loadQrHistory(raw: String): List<QrHistoryItem> = runCatching {
    val array = JSONArray(raw)
    (0 until array.length()).mapNotNull { index ->
        val item = array.optJSONObject(index) ?: return@mapNotNull null
        item.optString("value").takeIf { it.isNotBlank() }?.let {
            QrHistoryItem(it, item.optString("source", "Camera"), item.optString("savedAt", Instant.now().toString()))
        }
    }
}.getOrDefault(emptyList())

private fun encodeQrHistory(history: List<QrHistoryItem>): String = JSONArray().apply {
    history.forEach { item -> put(JSONObject().put("value", item.value).put("source", item.source).put("savedAt", item.savedAt)) }
}.toString()

private fun shareText(context: Context, text: String) {
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }, "Share QR result"))
}

private fun shareQrBitmap(context: Context, bitmap: Bitmap) {
    val file = File(context.cacheDir, "purehub-qr.png")
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }, "Share PureHub QR"))
}
