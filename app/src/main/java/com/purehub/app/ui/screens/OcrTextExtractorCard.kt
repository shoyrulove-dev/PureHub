package com.purehub.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.purehub.app.feature.ocr.OcrEngineFactory
import com.purehub.app.feature.ocr.OcrScript
import com.purehub.app.ui.LocalSnackbarHostState
import com.purehub.app.feature.docpdf.DocPdfRepository
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.util.concurrent.Executor
import kotlin.math.max

private enum class OcrStudioTab(val label: String) { Scan("Scan"), Text("Text"), Library("Library") }
private enum class OcrMode(val label: String) { Document("Document"), Receipt("Receipt"), Note("Note") }
private enum class OcrFilter(val label: String) { Original("Original"), Clean("Clean"), Mono("B&W") }
private enum class OcrLanguage(val label: String) { Latin("English + Vietnamese"), Chinese("简体中文") }

private data class OcrPage(
    val bitmap: Bitmap,
    val text: String,
    val source: String,
)

private data class OcrHistoryItem(
    val title: String,
    val text: String,
    val source: String,
    val savedAt: String,
)

@Composable
fun OcrTextExtractorCard(
    hasCameraPermission: Boolean,
    onRequestCameraPermission: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val preferences = remember { context.getSharedPreferences("purehub.ocr-studio.v2", 0) }
    val documentRepository = remember { DocPdfRepository(context.applicationContext) }
    var selectedLanguage by rememberSaveable { mutableStateOf(OcrLanguage.Latin) }
    val recognizer = remember(selectedLanguage) {
        OcrEngineFactory.create(
            context.applicationContext,
            if (selectedLanguage == OcrLanguage.Chinese) OcrScript.CHINESE else OcrScript.LATIN,
        )
    }
    val cameraExecutor = remember { ContextCompat.getMainExecutor(context) }
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val imageCapture = remember {
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
    }
    val pages = remember { mutableStateListOf<OcrPage>() }
    var selectedTab by rememberSaveable { mutableStateOf(OcrStudioTab.Scan) }
    var selectedMode by rememberSaveable { mutableStateOf(OcrMode.Document) }
    var selectedFilter by rememberSaveable { mutableStateOf(OcrFilter.Clean) }
    var rotation by rememberSaveable { mutableIntStateOf(0) }
    var cropPercent by rememberSaveable { mutableFloatStateOf(0.02f) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var extractedText by rememberSaveable { mutableStateOf("") }
    var documentTitle by rememberSaveable { mutableStateOf("My scan") }
    var status by rememberSaveable { mutableStateOf("Ready. Capture a page or choose an image.") }
    var processing by rememberSaveable { mutableStateOf(false) }
    var history by remember { mutableStateOf(loadOcrHistory(preferences.getString("history", "[]") ?: "[]")) }

    fun persistHistory(next: List<OcrHistoryItem>) {
        history = next.take(40)
        preferences.edit().putString("history", encodeOcrHistory(history)).apply()
    }

    fun recognize(bitmap: Bitmap, source: String) {
        processing = true
        status = "Recognizing text on this device..."
        recognizer.recognize(bitmap) { result ->
            result.onSuccess { rawText ->
                val text = cleanOcrText(rawText, selectedMode)
                currentBitmap = bitmap
                extractedText = text
                if (text.isBlank()) {
                    status = "No readable text found. Try better light or a tighter crop."
                } else {
                    pages += OcrPage(bitmap = bitmap, text = text, source = source)
                    status = "${pages.size} page(s) captured privately. Review the text before export."
                    selectedTab = OcrStudioTab.Text
                }
            }.onFailure { status = "OCR could not process this image." }
            processing = false
        }
    }

    fun prepareAndRecognize(bitmap: Bitmap, source: String) {
        if (pages.size >= 20) {
            status = "A scan can contain up to 20 pages. Export this document before starting another."
            return
        }
        val edited = transformOcrBitmap(limitOcrBitmap(bitmap), rotation, cropPercent, selectedFilter)
        recognize(edited, source)
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        status = "Opening the selected image locally..."
        val bitmap = runCatching {
            context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
        }.getOrNull()
        if (bitmap == null) status = "That image could not be opened."
        else prepareAndRecognize(bitmap, "Image")
    }

    fun saveCurrent() {
        val combined = combinedOcrText(pages, extractedText)
        if (combined.isBlank()) return
        val item = OcrHistoryItem(
            title = documentTitle.ifBlank { "Untitled scan" },
            text = combined,
            source = if (pages.size > 1) "${pages.size} pages" else pages.lastOrNull()?.source ?: "OCR",
            savedAt = Instant.now().toString(),
        )
        persistHistory(listOf(item) + history.filterNot { it.text == item.text })
        scope.launch { snackbarHostState.showSnackbar("Saved to your private OCR library.") }
    }

    DisposableEffect(recognizer) { onDispose { recognizer.close() } }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        OcrStudioHeader(selectedTab, onTabSelected = { selectedTab = it })
        when (selectedTab) {
            OcrStudioTab.Scan -> OcrScanContent(
                hasCameraPermission = hasCameraPermission,
                onRequestCameraPermission = onRequestCameraPermission,
                previewView = previewView,
                imageCapture = imageCapture,
                lifecycleOwner = lifecycleOwner,
                executor = cameraExecutor,
                selectedMode = selectedMode,
                onModeSelected = { selectedMode = it },
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it },
                selectedLanguage = selectedLanguage,
                onLanguageSelected = { selectedLanguage = it },
                cropPercent = cropPercent,
                onCropChanged = { cropPercent = it },
                rotation = rotation,
                onRotate = { rotation = (rotation + 90) % 360 },
                processing = processing,
                status = status,
                pageCount = pages.size,
                onChooseImage = { imagePicker.launch("image/*") },
                onCapture = {
                    captureOcrPage(
                        context = context,
                        imageCapture = imageCapture,
                        executor = cameraExecutor,
                        onCaptured = { prepareAndRecognize(it, "Camera") },
                        onError = { status = it },
                    )
                },
            )

            OcrStudioTab.Text -> OcrTextContent(
                bitmap = currentBitmap,
                text = extractedText,
                title = documentTitle,
                status = status,
                pages = pages,
                onTextChanged = { extractedText = it },
                onTitleChanged = { documentTitle = it },
                onCopy = { copyOcrText(context, extractedText) },
                onShare = { shareOcrText(context, documentTitle, combinedOcrText(pages, extractedText)) },
                onExportText = {
                    shareFile(context, exportOcrText(context, documentTitle, combinedOcrText(pages, extractedText)), "text/plain")
                },
                onExportPdf = {
                    val pagePairs = pages.mapIndexed { index, page ->
                        page.bitmap to if (index == pages.lastIndex) extractedText else page.text
                    }.ifEmpty { currentBitmap?.let { listOf(it to extractedText) }.orEmpty() }
                    if (pagePairs.isEmpty()) {
                        shareFile(context, exportOcrPdf(context, documentTitle, extractedText), "application/pdf")
                    } else {
                        val staged = documentRepository.stageOcrPages(pagePairs)
                        shareFile(context, documentRepository.exportPdf(staged, documentTitle).file, "application/pdf")
                    }
                },
                onSendToDocumentSuite = {
                    val pagePairs = pages.mapIndexed { index, page ->
                        page.bitmap to if (index == pages.lastIndex) extractedText else page.text
                    }.ifEmpty { currentBitmap?.let { listOf(it to extractedText) }.orEmpty() }
                    if (pagePairs.isEmpty()) {
                        status = "This library item has text only. Add its original image before sending to Doc to PDF."
                    } else {
                        val staged = documentRepository.stageOcrPages(pagePairs)
                        status = "${staged.size} searchable page(s) sent to Doc to PDF."
                        scope.launch { snackbarHostState.showSnackbar("Document Suite is ready with ${staged.size} OCR page(s).") }
                    }
                },
                onSave = ::saveCurrent,
                onAddPage = { selectedTab = OcrStudioTab.Scan },
                onClear = {
                    pages.clear()
                    currentBitmap = null
                    extractedText = ""
                    status = "Ready for a new document."
                    selectedTab = OcrStudioTab.Scan
                },
            )

            OcrStudioTab.Library -> OcrLibraryContent(
                history = history,
                onOpen = {
                    pages.clear()
                    documentTitle = it.title
                    extractedText = it.text
                    currentBitmap = null
                    status = "Opened from your private OCR library."
                    selectedTab = OcrStudioTab.Text
                },
                onDelete = { target -> persistHistory(history.filterNot { it == target }) },
                onClear = { persistHistory(emptyList()) },
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun OcrStudioHeader(selectedTab: OcrStudioTab, onTabSelected: (OcrStudioTab) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.Transparent), modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.background(
                Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.secondaryContainer)),
            ).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.onPrimaryContainer) {
                    Icon(Icons.Rounded.DocumentScanner, null, modifier = Modifier.padding(12.dp), tint = MaterialTheme.colorScheme.primaryContainer)
                }
                Column(Modifier.weight(1f)) {
                    Text("PRIVATE BY DESIGN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text("OCR Studio", style = MaterialTheme.typography.headlineSmall)
                    Text("Scan, clean and export text without uploading your documents.", style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OcrStudioTab.entries.forEach { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        label = { Text(tab.label) },
                        leadingIcon = {
                            Icon(
                                when (tab) {
                                    OcrStudioTab.Scan -> Icons.Rounded.DocumentScanner
                                    OcrStudioTab.Text -> Icons.Rounded.Description
                                    OcrStudioTab.Library -> Icons.Rounded.History
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
private fun OcrScanContent(
    hasCameraPermission: Boolean,
    onRequestCameraPermission: () -> Unit,
    previewView: PreviewView,
    imageCapture: ImageCapture,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    executor: Executor,
    selectedMode: OcrMode,
    onModeSelected: (OcrMode) -> Unit,
    selectedFilter: OcrFilter,
    onFilterSelected: (OcrFilter) -> Unit,
    selectedLanguage: OcrLanguage,
    onLanguageSelected: (OcrLanguage) -> Unit,
    cropPercent: Float,
    onCropChanged: (Float) -> Unit,
    rotation: Int,
    onRotate: () -> Unit,
    processing: Boolean,
    status: String,
    pageCount: Int,
    onChooseImage: () -> Unit,
    onCapture: () -> Unit,
) {
    val context = LocalContext.current
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OcrMode.entries.forEach { mode ->
                FilterChip(selectedMode == mode, onClick = { onModeSelected(mode) }, label = { Text(mode.label) })
            }
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
            if (hasCameraPermission) {
                Box(Modifier.fillMaxWidth().aspectRatio(1f).background(Color(0xFF07111E))) {
                    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
                    LaunchedEffect(previewView) {
                        bindOcrCaptureCamera(context, previewView, imageCapture, lifecycleOwner)
                    }
                    Box(
                        Modifier.align(Alignment.Center).fillMaxWidth(0.82f).aspectRatio(0.72f)
                            .clip(RoundedCornerShape(18.dp))
                            .border(2.dp, Color(0xFF6EE7B7), RoundedCornerShape(18.dp)),
                    )
                    Surface(
                        modifier = Modifier.align(Alignment.TopStart).padding(14.dp),
                        shape = RoundedCornerShape(50),
                        color = Color(0xCC0F172A),
                    ) {
                        Row(Modifier.padding(horizontal = 11.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Security, null, tint = Color(0xFF6EE7B7), modifier = Modifier.size(16.dp))
                            Text(" On-device", color = Color.White, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    if (pageCount > 0) {
                        AssistChip(
                            onClick = {},
                            label = { Text("$pageCount page${if (pageCount == 1) "" else "s"}") },
                            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                        )
                    }
                    Button(
                        onClick = onCapture,
                        enabled = !processing,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(18.dp).height(54.dp),
                    ) {
                        if (processing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Rounded.DocumentScanner, null)
                        Text(if (processing) "  Reading..." else "  Capture page")
                    }
                }
            } else {
                Column(
                    Modifier.fillMaxWidth().padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Rounded.Security, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("Camera stays off until you allow it", style = MaterialTheme.typography.titleMedium)
                    Text("OCR runs locally after each capture.", style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = onRequestCameraPermission) { Text("Allow camera") }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onChooseImage, enabled = !processing, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.AddPhotoAlternate, null)
                Text("  Scan image")
            }
            OutlinedButton(onClick = onRotate, enabled = !processing, modifier = Modifier.weight(1f)) {
                Icon(Icons.AutoMirrored.Rounded.RotateRight, null)
                Text("  Rotate $rotation°")
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Document cleanup", style = MaterialTheme.typography.titleSmall)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OcrFilter.entries.forEach { filter ->
                        FilterChip(selectedFilter == filter, onClick = { onFilterSelected(filter) }, label = { Text(filter.label) })
                    }
                }
                Text("Edge crop ${Math.round(cropPercent * 100)}%", style = MaterialTheme.typography.labelMedium)
                Slider(value = cropPercent, onValueChange = onCropChanged, valueRange = 0f..0.16f)
            }
        }
        Text("Recognition language", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OcrLanguage.entries.forEach { language ->
                FilterChip(
                    selected = selectedLanguage == language,
                    onClick = { onLanguageSelected(language) },
                    label = { Text(language.label) },
                )
            }
        }
        Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun OcrTextContent(
    bitmap: Bitmap?,
    text: String,
    title: String,
    status: String,
    pages: List<OcrPage>,
    onTextChanged: (String) -> Unit,
    onTitleChanged: (String) -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onExportText: () -> Unit,
    onExportPdf: () -> Unit,
    onSendToDocumentSuite: () -> Unit,
    onSave: () -> Unit,
    onAddPage: () -> Unit,
    onClear: () -> Unit,
) {
    val displayedPageCount = pages.size.coerceAtLeast(if (text.isBlank()) 0 else 1)
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (text.isBlank()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Description, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("No text yet", style = MaterialTheme.typography.titleLarge)
                    Text("Capture a page or choose an image to begin.")
                    FilledTonalButton(onClick = onAddPage, modifier = Modifier.padding(top = 12.dp)) { Text("Start scanning") }
                }
            }
            return
        }
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Scanned document preview",
                modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(18.dp)),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = {}, label = { Text("$displayedPageCount page${if (displayedPageCount == 1) "" else "s"}") })
            AssistChip(onClick = {}, label = { Text("${text.split(Regex("\\s+")).count { it.isNotBlank() }} words") })
            AssistChip(onClick = {}, label = { Text("On-device") }, leadingIcon = { Icon(Icons.Rounded.Security, null, Modifier.size(16.dp)) })
        }
        OutlinedTextField(value = title, onValueChange = onTitleChanged, label = { Text("Document title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            value = text,
            onValueChange = onTextChanged,
            label = { Text("Recognized text") },
            minLines = 7,
            maxLines = 12,
            modifier = Modifier.fillMaxWidth(),
        )
        DetectedActions(text)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onCopy, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.ContentCopy, null); Text(" Copy") }
            FilledTonalButton(onClick = onShare, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.IosShare, null); Text(" Share") }
        }
        FilledTonalButton(onClick = onSendToDocumentSuite, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.DocumentScanner, null)
            Text(" Continue in Doc to PDF")
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onExportText, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.Description, null); Text(" TXT") }
            OutlinedButton(onClick = onExportPdf, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.PictureAsPdf, null); Text(" PDF") }
            OutlinedButton(onClick = onSave, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.History, null); Text(" Save") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onAddPage, modifier = Modifier.weight(1f)) { Text("Add page") }
            OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) { Text("New document") }
        }
        Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DetectedActions(text: String) {
    val context = LocalContext.current
    val url = Regex("https?://\\S+", RegexOption.IGNORE_CASE).find(text)?.value?.trimEnd('.', ',', ')')
    val email = Regex("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", RegexOption.IGNORE_CASE).find(text)?.value
    val phone = Regex("(?:\\+?\\d[\\d .-]{7,}\\d)").find(text)?.value
    if (url == null && email == null && phone == null) return
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Quick actions", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                url?.let { AssistChip(onClick = { openSafeIntent(context, Intent(Intent.ACTION_VIEW, Uri.parse(it))) }, label = { Text("Open link") }) }
                email?.let { AssistChip(onClick = { openSafeIntent(context, Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$it"))) }, label = { Text("Email") }) }
                phone?.let { AssistChip(onClick = { openSafeIntent(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:${it.filter { c -> c.isDigit() || c == '+' }}"))) }, label = { Text("Call") }) }
            }
        }
    }
}

@Composable
private fun OcrLibraryContent(
    history: List<OcrHistoryItem>,
    onOpen: (OcrHistoryItem) -> Unit,
    onDelete: (OcrHistoryItem) -> Unit,
    onClear: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(history, query) { history.filter { query.isBlank() || it.title.contains(query, true) || it.text.contains(query, true) } }
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Private library", style = MaterialTheme.typography.titleLarge); Text("Searchable and stored only on this device.") }
            if (history.isNotEmpty()) IconButton(onClick = onClear) { Icon(Icons.Rounded.DeleteOutline, "Clear library") }
        }
        OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Search scans") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        if (filtered.isEmpty()) {
            Card(Modifier.fillMaxWidth()) { Text(if (history.isEmpty()) "Saved OCR documents will appear here." else "No matching document.", Modifier.padding(24.dp)) }
        }
        filtered.forEach { item ->
            Card(onClick = { onOpen(item) }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) { Icon(Icons.Rounded.Description, null, Modifier.padding(10.dp)) }
                    Column(Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.titleSmall)
                        Text(item.text.replace('\n', ' ').take(90), maxLines = 2, style = MaterialTheme.typography.bodySmall)
                        Text(item.source, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { onDelete(item) }) { Icon(Icons.Rounded.DeleteOutline, "Delete") }
                }
            }
        }
    }
}

private fun bindOcrCaptureCamera(
    context: Context,
    previewView: PreviewView,
    imageCapture: ImageCapture,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
) {
    val provider = ProcessCameraProvider.getInstance(context).get()
    val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
    provider.unbindAll()
    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
}

private fun captureOcrPage(
    context: Context,
    imageCapture: ImageCapture,
    executor: Executor,
    onCaptured: (Bitmap) -> Unit,
    onError: (String) -> Unit,
) {
    val file = File(context.cacheDir, "ocr-${System.currentTimeMillis()}.jpg")
    imageCapture.takePicture(
        ImageCapture.OutputFileOptions.Builder(file).build(),
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap == null) onError("The captured page could not be opened.") else onCaptured(bitmap)
            }
            override fun onError(exception: ImageCaptureException) { onError("Capture failed. Please try again.") }
        },
    )
}

private fun transformOcrBitmap(source: Bitmap, rotation: Int, crop: Float, filter: OcrFilter): Bitmap {
    val rotated = if (rotation == 0) source else Bitmap.createBitmap(source, 0, 0, source.width, source.height, Matrix().apply { postRotate(rotation.toFloat()) }, true)
    val insetX = (rotated.width * crop).toInt().coerceAtMost(max(0, rotated.width / 3))
    val insetY = (rotated.height * crop).toInt().coerceAtMost(max(0, rotated.height / 3))
    val cropped = Bitmap.createBitmap(rotated, insetX, insetY, max(1, rotated.width - insetX * 2), max(1, rotated.height - insetY * 2))
    if (filter == OcrFilter.Original) return cropped
    val output = Bitmap.createBitmap(cropped.width, cropped.height, Bitmap.Config.ARGB_8888)
    val matrix = ColorMatrix().apply {
        setSaturation(if (filter == OcrFilter.Mono) 0f else 0.25f)
        if (filter == OcrFilter.Clean) {
            val contrast = 1.18f
            val translate = (-0.5f * contrast + 0.5f) * 255f
            postConcat(ColorMatrix(floatArrayOf(contrast,0f,0f,0f,translate, 0f,contrast,0f,0f,translate, 0f,0f,contrast,0f,translate, 0f,0f,0f,1f,0f)))
        }
    }
    Canvas(output).drawBitmap(cropped, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = ColorMatrixColorFilter(matrix) })
    return output
}

private fun limitOcrBitmap(source: Bitmap, maxDimension: Int = 2200): Bitmap {
    val largest = max(source.width, source.height)
    if (largest <= maxDimension) return source
    val scale = maxDimension.toFloat() / largest.toFloat()
    return Bitmap.createScaledBitmap(
        source,
        max(1, (source.width * scale).toInt()),
        max(1, (source.height * scale).toInt()),
        true,
    )
}

private fun cleanOcrText(raw: String, mode: OcrMode): String {
    val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
    return when (mode) {
        OcrMode.Receipt -> lines.joinToString("\n")
        OcrMode.Document -> lines.joinToString("\n").replace(Regex("[ \\t]+"), " ")
        OcrMode.Note -> lines.joinToString(" ").replace(Regex("\\s+"), " ")
    }.trim()
}

private fun combinedOcrText(pages: List<OcrPage>, current: String): String {
    if (pages.isEmpty()) return current.trim()
    val values = pages.map { it.text }.toMutableList()
    if (current.isNotBlank()) values[values.lastIndex] = current.trim()
    return values.mapIndexed { index, value -> if (values.size > 1) "Page ${index + 1}\n$value" else value }.joinToString("\n\n")
}

private fun copyOcrText(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("PureHub OCR", text))
}

private fun shareOcrText(context: Context, title: String, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, title); putExtra(Intent.EXTRA_TEXT, text) }
    context.startActivity(Intent.createChooser(intent, "Share recognized text"))
}

private fun safeFileName(title: String): String = title.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "purehub-ocr" }

private fun exportOcrText(context: Context, title: String, text: String): File {
    val directory = File(context.cacheDir, "shared").apply { mkdirs() }
    return File(directory, "${safeFileName(title)}.txt").apply { writeText(text, Charsets.UTF_8) }
}

private fun exportOcrPdf(context: Context, title: String, text: String): File {
    val directory = File(context.cacheDir, "shared").apply { mkdirs() }
    val file = File(directory, "${safeFileName(title)}.pdf")
    val document = PdfDocument()
    val pageWidth = 595
    val pageHeight = 842
    val lines = text.lines().flatMap { line -> if (line.length <= 82) listOf(line) else line.chunked(82) }
    val chunks = lines.chunked(46).ifEmpty { listOf(listOf("")) }
    chunks.forEachIndexed { index, pageLines ->
        val page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(15, 23, 42); textSize = 12f }
        val heading = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(5, 150, 105); textSize = 18f; isFakeBoldText = true }
        page.canvas.drawText(if (index == 0) title.take(52) else "$title - ${index + 1}", 42f, 52f, heading)
        pageLines.forEachIndexed { lineIndex, line -> page.canvas.drawText(line, 42f, 86f + lineIndex * 15f, paint) }
        document.finishPage(page)
    }
    FileOutputStream(file).use(document::writeTo)
    document.close()
    return file
}

private fun shareFile(context: Context, file: File, mime: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply { type = mime; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    context.startActivity(Intent.createChooser(intent, "Share ${file.name}"))
}

private fun openSafeIntent(context: Context, intent: Intent) {
    runCatching { context.startActivity(intent) }
}

private fun loadOcrHistory(raw: String): List<OcrHistoryItem> = runCatching {
    val array = JSONArray(raw)
    (0 until array.length()).map { index ->
        val item = array.getJSONObject(index)
        OcrHistoryItem(item.optString("title"), item.optString("text"), item.optString("source"), item.optString("savedAt"))
    }.filter { it.text.isNotBlank() }
}.getOrDefault(emptyList())

private fun encodeOcrHistory(items: List<OcrHistoryItem>): String = JSONArray().apply {
    items.forEach { item -> put(JSONObject().put("title", item.title).put("text", item.text).put("source", item.source).put("savedAt", item.savedAt)) }
}.toString()
