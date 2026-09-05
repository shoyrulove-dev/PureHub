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
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import com.purehub.app.ui.LocalizedText
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.purehub.app.feature.ocr.OcrEngineFactory
import com.purehub.app.feature.ocr.OcrScript
import com.purehub.app.feature.ocr.OcrDocumentStore
import com.purehub.app.feature.ocr.OcrStoredPage
import com.purehub.app.data.local.PureHubDatabaseProvider
import com.purehub.app.feature.expense.ExpenseTrackerRepository
import com.purehub.app.feature.receipt.ReceiptParser
import com.purehub.app.feature.receipt.ReceiptResult
import com.purehub.app.ui.LocalSnackbarHostState
import com.purehub.app.feature.docpdf.DocPdfRepository
import com.purehub.app.feature.docpdf.DocumentCorners
import com.purehub.app.feature.docpdf.DocumentEdgeDetector
import com.purehub.app.feature.docpdf.DocumentPerspectiveCorrector
import com.purehub.app.feature.docpdf.NormalizedPoint
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
    val documentId: String = "",
    val pageCount: Int = 1,
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
    val documentStore = remember { OcrDocumentStore(context.applicationContext) }
    val expenseRepository = remember {
        ExpenseTrackerRepository(PureHubDatabaseProvider.get(context.applicationContext).expenseDao())
    }
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
    var selectedPageIndex by remember { mutableIntStateOf(-1) }
    var selectedTab by rememberSaveable { mutableStateOf(OcrStudioTab.Scan) }
    var selectedMode by rememberSaveable { mutableStateOf(OcrMode.Document) }
    var selectedFilter by rememberSaveable { mutableStateOf(OcrFilter.Clean) }
    var rotation by rememberSaveable { mutableIntStateOf(0) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pendingBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pendingSource by rememberSaveable { mutableStateOf("") }
    var pendingCorners by remember { mutableStateOf(DocumentCorners.fullFrame(0.02f)) }
    var frameConfidence by rememberSaveable { mutableFloatStateOf(0f) }
    var activeDocumentId by rememberSaveable { mutableStateOf("") }
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
                    selectedPageIndex = pages.lastIndex
                    status = "${pages.size} page(s) captured privately. Review the text before export."
                    selectedTab = OcrStudioTab.Text
                }
            }.onFailure { status = "OCR could not process this image." }
            processing = false
        }
    }

    fun reviewBeforeRecognizing(bitmap: Bitmap, source: String) {
        if (pages.size >= 20) {
            bitmap.recycle()
            status = "A scan can contain up to 20 pages. Export this document before starting another."
            return
        }
        pendingBitmap?.takeIf { !it.isRecycled }?.recycle()
        val prepared = rotateAndLimitOcrBitmap(bitmap, rotation)
        val frame = DocumentEdgeDetector.detect(prepared)
        pendingBitmap = prepared
        pendingSource = source
        pendingCorners = if (frame.confidence > 0f) DocumentCorners.fromCrop(frame.crop) else DocumentCorners.fullFrame(0.02f)
        frameConfidence = frame.confidence
        status = if (frame.confidence > 0f) {
            "Page frame found (${(frame.confidence * 100).toInt()}%). Adjust the four corners, then recognize."
        } else {
            "Check the four corners, then recognize. The full page is selected because no reliable frame was found."
        }
    }

    fun acceptReviewedPage() {
        val source = pendingBitmap ?: return
        pendingBitmap = null
        val edited = prepareOcrBitmap(source, 0, selectedFilter, pendingCorners)
        recognize(edited, pendingSource.ifBlank { "Scanned page" })
        pendingSource = ""
        frameConfidence = 0f
    }

    fun rotateReviewOrNextCapture() {
        rotation = (rotation + 90) % 360
        val source = pendingBitmap ?: return
        val rotated = rotateAndLimitOcrBitmap(source, 90)
        val frame = DocumentEdgeDetector.detect(rotated)
        pendingBitmap = rotated
        frameConfidence = frame.confidence
        pendingCorners = if (frame.confidence > 0f) {
            DocumentCorners.fromCrop(frame.crop)
        } else {
            DocumentCorners.fullFrame(0.02f)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        val selected = uris.take((20 - pages.size).coerceAtLeast(0))
        if (selected.isEmpty()) {
            status = "A scan can contain up to 20 pages. Export this document before starting another."
            return@rememberLauncherForActivityResult
        }
        if (selected.size == 1) {
            val bitmap = runCatching {
                context.contentResolver.openInputStream(selected.first())?.use(BitmapFactory::decodeStream)
            }.getOrNull()
            if (bitmap == null) status = "The selected image could not be opened."
            else reviewBeforeRecognizing(bitmap, "Imported image")
            return@rememberLauncherForActivityResult
        }
        processing = true
        status = "Preparing ${selected.size} image(s) for private batch OCR..."
        fun recognizeNext(index: Int) {
            if (index >= selected.size) {
                processing = false
                if (pages.isNotEmpty()) {
                    selectedPageIndex = pages.lastIndex
                    currentBitmap = pages.last().bitmap
                    extractedText = pages.last().text
                    selectedTab = OcrStudioTab.Text
                    status = "Batch OCR finished: ${selected.size} image(s), ${pages.size} page(s) in this document."
                }
                return
            }
            val bitmap = runCatching {
                context.contentResolver.openInputStream(selected[index])?.use(BitmapFactory::decodeStream)
            }.getOrNull()
            if (bitmap == null) {
                recognizeNext(index + 1)
                return
            }
            val edited = prepareOcrBitmap(bitmap, rotation, selectedFilter)
            status = "Recognizing image ${index + 1}/${selected.size} on this device..."
            recognizer.recognize(edited) { result ->
                result.getOrNull()?.let { raw ->
                    val text = cleanOcrText(raw, selectedMode)
                    if (text.isNotBlank()) pages += OcrPage(edited, text, "Batch image ${index + 1}")
                }
                recognizeNext(index + 1)
            }
        }
        recognizeNext(0)
    }

    val parsedReceipt = remember(selectedMode, extractedText) {
        if (selectedMode == OcrMode.Receipt && extractedText.isNotBlank()) ReceiptParser.parse(extractedText) else null
    }

    fun saveCurrent() {
        val combined = combinedOcrText(pages, extractedText)
        if (combined.isBlank()) return
        val storedPages = pages.map { OcrStoredPage(it.bitmap, it.text, it.source) }
            .ifEmpty { currentBitmap?.let { listOf(OcrStoredPage(it, extractedText, "OCR")) }.orEmpty() }
        if (storedPages.isEmpty()) return
        runCatching {
            documentStore.save(activeDocumentId.ifBlank { null }, documentTitle, storedPages)
        }.onSuccess { document ->
            activeDocumentId = document.id
            val item = OcrHistoryItem(
                title = document.title,
                text = combined,
                source = "${document.pageCount} saved page${if (document.pageCount == 1) "" else "s"}",
                savedAt = document.savedAt,
                documentId = document.id,
                pageCount = document.pageCount,
            )
            persistHistory(listOf(item) + history.filterNot {
                it.documentId == item.documentId || (it.documentId.isBlank() && it.text == item.text)
            })
            scope.launch { snackbarHostState.showSnackbar("Complete document saved privately on this device.") }
        }.onFailure {
            status = "The complete document could not be saved. Your current scan remains open."
        }
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
                rotation = rotation,
                onRotate = ::rotateReviewOrNextCapture,
                pendingBitmap = pendingBitmap,
                pendingCorners = pendingCorners,
                frameConfidence = frameConfidence,
                onCornersChanged = { pendingCorners = it },
                onAutoFrame = {
                    pendingBitmap?.let { bitmap ->
                        val frame = DocumentEdgeDetector.detect(bitmap)
                        frameConfidence = frame.confidence
                        pendingCorners = if (frame.confidence > 0f) {
                            DocumentCorners.fromCrop(frame.crop)
                        } else {
                            DocumentCorners.fullFrame(0.02f)
                        }
                    }
                },
                onAcceptReview = ::acceptReviewedPage,
                onCancelReview = {
                    pendingBitmap?.takeIf { !it.isRecycled }?.recycle()
                    pendingBitmap = null
                    pendingSource = ""
                    frameConfidence = 0f
                    status = "Ready. Capture a page or choose an image."
                },
                processing = processing,
                status = status,
                pageCount = pages.size,
                onChooseImage = { imagePicker.launch(arrayOf("image/*")) },
                onCapture = {
                    captureOcrPage(
                        context = context,
                        imageCapture = imageCapture,
                        executor = cameraExecutor,
                        onCaptured = { reviewBeforeRecognizing(it, "Camera") },
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
                selectedPageIndex = selectedPageIndex,
                receipt = parsedReceipt,
                onTextChanged = { value ->
                    extractedText = value
                    if (selectedPageIndex in pages.indices) {
                        pages[selectedPageIndex] = pages[selectedPageIndex].copy(text = value)
                    }
                },
                onTitleChanged = { documentTitle = it },
                onCopy = { copyOcrText(context, extractedText) },
                onShare = { shareOcrText(context, documentTitle, combinedOcrText(pages, extractedText)) },
                onExportText = {
                    shareFile(context, exportOcrText(context, documentTitle, combinedOcrText(pages, extractedText)), "text/plain")
                },
                onExportPdf = {
                    val pagePairs = pages.map { page -> page.bitmap to page.text }
                        .ifEmpty { currentBitmap?.let { listOf(it to extractedText) }.orEmpty() }
                    if (pagePairs.isEmpty()) {
                        shareFile(context, exportOcrPdf(context, documentTitle, extractedText), "application/pdf")
                    } else {
                        val staged = documentRepository.stageOcrPages(pagePairs)
                        shareFile(context, documentRepository.exportPdf(staged, documentTitle).file, "application/pdf")
                    }
                },
                onSendToDocumentSuite = {
                    val pagePairs = pages.map { page -> page.bitmap to page.text }
                        .ifEmpty { currentBitmap?.let { listOf(it to extractedText) }.orEmpty() }
                    if (pagePairs.isEmpty()) {
                        status = "This library item has text only. Add its original image before sending to Doc to PDF."
                    } else {
                        val staged = documentRepository.stageOcrPages(pagePairs)
                        status = "${staged.size} searchable page(s) sent to Doc to PDF."
                        scope.launch { snackbarHostState.showSnackbar("Document Suite is ready with ${staged.size} OCR page(s).") }
                    }
                },
                onSave = ::saveCurrent,
                onSaveReceipt = { receipt ->
                    val total = receipt.total
                    if (total == null) {
                        status = "No reliable receipt total found. Edit the OCR text or add the expense manually."
                    } else scope.launch {
                        expenseRepository.addExpense(
                            title = receipt.merchant.ifBlank { "Scanned receipt" },
                            amountText = total.toString(),
                            category = "Shopping",
                            transactionType = "expense",
                            wallet = "Cash",
                            note = listOfNotNull(
                                receipt.date?.let { "Receipt date: $it" },
                                receipt.tax?.let { "Tax: $it" },
                                "Imported from OCR Studio",
                            ).joinToString(" - "),
                        )
                        status = "Receipt saved to Money Studio. Review the category and wallet when convenient."
                        snackbarHostState.showSnackbar("Receipt saved to Money Studio.")
                    }
                },
                onSelectPage = { index ->
                    pages.getOrNull(index)?.let { page ->
                        selectedPageIndex = index
                        currentBitmap = page.bitmap
                        extractedText = page.text
                    }
                },
                onDeletePage = {
                    val index = selectedPageIndex
                    if (index in pages.indices) pages.removeAt(index)
                    if (pages.isEmpty()) {
                        selectedPageIndex = -1
                        currentBitmap = null
                        extractedText = ""
                        selectedTab = OcrStudioTab.Scan
                    } else {
                        selectedPageIndex = index.coerceAtMost(pages.lastIndex)
                        currentBitmap = pages[selectedPageIndex].bitmap
                        extractedText = pages[selectedPageIndex].text
                    }
                },
                onAddPage = { selectedTab = OcrStudioTab.Scan },
                onClear = {
                    activeDocumentId = ""
                    pages.clear()
                    selectedPageIndex = -1
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
                    activeDocumentId = it.documentId
                    val stored = it.documentId.takeIf(String::isNotBlank)?.let(documentStore::load)
                    if (stored != null) {
                        stored.pages.forEach { page -> pages += OcrPage(page.bitmap, page.text, page.source) }
                        selectedPageIndex = pages.lastIndex
                        currentBitmap = pages.lastOrNull()?.bitmap
                        extractedText = pages.lastOrNull()?.text.orEmpty()
                        status = "Reopened ${pages.size} complete page(s). You can edit or export the document again."
                    } else {
                        selectedPageIndex = -1
                        extractedText = it.text
                        currentBitmap = null
                        status = "Opened legacy text-only history. New saves keep the complete document."
                    }
                    selectedTab = OcrStudioTab.Text
                },
                onDelete = { target ->
                    target.documentId.takeIf(String::isNotBlank)?.let(documentStore::delete)
                    persistHistory(history.filterNot { it == target })
                },
                onClear = {
                    documentStore.clear()
                    persistHistory(emptyList())
                },
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
                    LocalizedText("PRIVATE BY DESIGN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    LocalizedText("OCR Studio", style = MaterialTheme.typography.headlineSmall)
                    LocalizedText("Scan, clean and export text without uploading your documents.", style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OcrStudioTab.entries.forEach { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        label = { LocalizedText(tab.label) },
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
    rotation: Int,
    onRotate: () -> Unit,
    pendingBitmap: Bitmap?,
    pendingCorners: DocumentCorners,
    frameConfidence: Float,
    onCornersChanged: (DocumentCorners) -> Unit,
    onAutoFrame: () -> Unit,
    onAcceptReview: () -> Unit,
    onCancelReview: () -> Unit,
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
                FilterChip(selectedMode == mode, onClick = { onModeSelected(mode) }, label = { LocalizedText(mode.label) })
            }
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
            if (pendingBitmap != null) {
                OcrPageReview(
                    bitmap = pendingBitmap,
                    corners = pendingCorners,
                    frameConfidence = frameConfidence,
                    onCornersChanged = onCornersChanged,
                    onAutoFrame = onAutoFrame,
                    onAccept = onAcceptReview,
                    onCancel = onCancelReview,
                )
            } else if (hasCameraPermission) {
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
                            LocalizedText(" On-device", color = Color.White, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    if (pageCount > 0) {
                        AssistChip(
                            onClick = {},
                            label = { LocalizedText("$pageCount page${if (pageCount == 1) "" else "s"}") },
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
                        LocalizedText(if (processing) "  Reading..." else "  Capture page")
                    }
                }
            } else {
                Column(
                    Modifier.fillMaxWidth().padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Rounded.Security, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
                    LocalizedText("Camera stays off until you allow it", style = MaterialTheme.typography.titleMedium)
                    LocalizedText("OCR runs locally after each capture.", style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = onRequestCameraPermission) { LocalizedText("Allow camera") }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onChooseImage, enabled = !processing, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.AddPhotoAlternate, null)
                LocalizedText("  Scan image")
            }
            OutlinedButton(onClick = onRotate, enabled = !processing, modifier = Modifier.weight(1f)) {
                Icon(Icons.AutoMirrored.Rounded.RotateRight, null)
                LocalizedText("  Rotate $rotation°")
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LocalizedText("Document cleanup", style = MaterialTheme.typography.titleSmall)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OcrFilter.entries.forEach { filter ->
                        FilterChip(selectedFilter == filter, onClick = { onFilterSelected(filter) }, label = { LocalizedText(filter.label) })
                    }
                }
                LocalizedText("Auto-frame runs locally. Review and drag all four corners before recognition.", style = MaterialTheme.typography.bodySmall)
            }
        }
        LocalizedText("Recognition language", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OcrLanguage.entries.forEach { language ->
                FilterChip(
                    selected = selectedLanguage == language,
                    onClick = { onLanguageSelected(language) },
                    label = { LocalizedText(language.label) },
                )
            }
        }
        LocalizedText(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun OcrPageReview(
    bitmap: Bitmap,
    corners: DocumentCorners,
    frameConfidence: Float,
    onCornersChanged: (DocumentCorners) -> Unit,
    onAutoFrame: () -> Unit,
    onAccept: () -> Unit,
    onCancel: () -> Unit,
) {
    val currentCorners = rememberUpdatedState(corners)
    val currentOnCornersChanged = rememberUpdatedState(onCornersChanged)
    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                LocalizedText("Review page", style = MaterialTheme.typography.titleMedium)
                LocalizedText(
                    if (frameConfidence > 0f) "Auto-frame confidence ${(frameConfidence * 100).toInt()}%" else "Manual frame",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AssistChip(onClick = onAutoFrame, label = { LocalizedText("Auto-frame") }, leadingIcon = { Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(16.dp)) })
        }
        BoxWithConstraints(
            Modifier.fillMaxWidth().aspectRatio(bitmap.width.toFloat() / bitmap.height.coerceAtLeast(1)).clip(RoundedCornerShape(16.dp)),
        ) {
            val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
            Image(bitmap.asImageBitmap(), "Page awaiting corner review", Modifier.fillMaxSize())
            ComposeCanvas(
                Modifier.fillMaxSize().pointerInput(Unit) {
                    var activeCorner = -1
                    detectDragGestures(
                        onDragStart = { touch ->
                            val values = currentCorners.value.points()
                            activeCorner = values.indices.minByOrNull { index ->
                                val dx = touch.x - values[index].x * widthPx
                                val dy = touch.y - values[index].y * heightPx
                                dx * dx + dy * dy
                            } ?: -1
                        },
                        onDragEnd = { activeCorner = -1 },
                        onDragCancel = { activeCorner = -1 },
                    ) { change, _ ->
                        if (activeCorner < 0) return@detectDragGestures
                        change.consume()
                        val point = NormalizedPoint(
                            (change.position.x / widthPx).coerceIn(0f, 1f),
                            (change.position.y / heightPx).coerceIn(0f, 1f),
                        )
                        currentOnCornersChanged.value(currentCorners.value.withPoint(activeCorner, point).sanitized())
                    }
                },
            ) {
                val values = corners.points().map { point -> Offset(point.x * size.width, point.y * size.height) }
                val outline = Path().apply {
                    moveTo(values[0].x, values[0].y)
                    lineTo(values[1].x, values[1].y)
                    lineTo(values[2].x, values[2].y)
                    lineTo(values[3].x, values[3].y)
                    close()
                }
                drawPath(outline, Color(0xFF34D399), style = Stroke(width = 5f))
                values.forEach { point ->
                    drawCircle(Color.White, radius = 13f, center = point)
                    drawCircle(Color(0xFF059669), radius = 9f, center = point)
                }
            }
        }
        LocalizedText("Drag each green corner to the page edge. PureHub corrects perspective before OCR.", style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { LocalizedText("Retake") }
            Button(onClick = onAccept, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.DocumentScanner, null)
                LocalizedText(" Recognize")
            }
        }
    }
}

private fun DocumentCorners.points(): List<NormalizedPoint> = listOf(topLeft, topRight, bottomRight, bottomLeft)

private fun DocumentCorners.withPoint(index: Int, point: NormalizedPoint): DocumentCorners = when (index) {
    0 -> copy(topLeft = point)
    1 -> copy(topRight = point)
    2 -> copy(bottomRight = point)
    3 -> copy(bottomLeft = point)
    else -> this
}

@Composable
private fun OcrTextContent(
    bitmap: Bitmap?,
    text: String,
    title: String,
    status: String,
    pages: List<OcrPage>,
    selectedPageIndex: Int,
    receipt: ReceiptResult?,
    onTextChanged: (String) -> Unit,
    onTitleChanged: (String) -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onExportText: () -> Unit,
    onExportPdf: () -> Unit,
    onSendToDocumentSuite: () -> Unit,
    onSave: () -> Unit,
    onSaveReceipt: (ReceiptResult) -> Unit,
    onSelectPage: (Int) -> Unit,
    onDeletePage: () -> Unit,
    onAddPage: () -> Unit,
    onClear: () -> Unit,
) {
    val displayedPageCount = pages.size.coerceAtLeast(if (text.isBlank()) 0 else 1)
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (text.isBlank()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Description, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
                    LocalizedText("No text yet", style = MaterialTheme.typography.titleLarge)
                    LocalizedText("Capture a page or choose an image to begin.")
                    FilledTonalButton(onClick = onAddPage, modifier = Modifier.padding(top = 12.dp)) { LocalizedText("Start scanning") }
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
            AssistChip(onClick = {}, label = { LocalizedText("$displayedPageCount page${if (displayedPageCount == 1) "" else "s"}") })
            AssistChip(onClick = {}, label = { LocalizedText("${text.split(Regex("\\s+")).count { it.isNotBlank() }} words") })
            AssistChip(onClick = {}, label = { LocalizedText("On-device") }, leadingIcon = { Icon(Icons.Rounded.Security, null, Modifier.size(16.dp)) })
        }
        if (pages.size > 1 && selectedPageIndex in pages.indices) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    enabled = selectedPageIndex > 0,
                    onClick = { onSelectPage(selectedPageIndex - 1) },
                    modifier = Modifier.weight(1f),
                ) { LocalizedText("Previous") }
                AssistChip(onClick = {}, label = { LocalizedText("Page ${selectedPageIndex + 1}/${pages.size}") })
                OutlinedButton(
                    enabled = selectedPageIndex < pages.lastIndex,
                    onClick = { onSelectPage(selectedPageIndex + 1) },
                    modifier = Modifier.weight(1f),
                ) { LocalizedText("Next") }
            }
        }
        OutlinedTextField(value = title, onValueChange = onTitleChanged, label = { LocalizedText("Document title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            value = text,
            onValueChange = onTextChanged,
            label = { LocalizedText("Recognized text") },
            minLines = 7,
            maxLines = 12,
            modifier = Modifier.fillMaxWidth(),
        )
        DetectedActions(text)
        receipt?.let { item ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.62f))) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LocalizedText("Receipt detected", style = MaterialTheme.typography.titleSmall)
                    LocalizedText(item.merchant, style = MaterialTheme.typography.bodyMedium)
                    LocalizedText(
                        item.total?.let { "Total %.2f".format(it) } ?: "Total needs review",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    item.date?.let { LocalizedText("Date $it", style = MaterialTheme.typography.bodySmall) }
                    Button(enabled = item.total != null, onClick = { onSaveReceipt(item) }, modifier = Modifier.fillMaxWidth()) {
                        LocalizedText("Save to Money Studio")
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onCopy, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.ContentCopy, null); LocalizedText(" Copy") }
            FilledTonalButton(onClick = onShare, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.IosShare, null); LocalizedText(" Share") }
        }
        FilledTonalButton(onClick = onSendToDocumentSuite, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.DocumentScanner, null)
            LocalizedText(" Continue in Doc to PDF")
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onExportText, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.Description, null); LocalizedText(" TXT") }
            OutlinedButton(onClick = onExportPdf, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.PictureAsPdf, null); LocalizedText(" PDF") }
            OutlinedButton(onClick = onSave, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.History, null); LocalizedText(" Save") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onAddPage, modifier = Modifier.weight(1f)) { LocalizedText("Add page") }
            if (pages.isNotEmpty()) OutlinedButton(onClick = onDeletePage, modifier = Modifier.weight(1f)) { LocalizedText("Delete page") }
            OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) { LocalizedText("New document") }
        }
        LocalizedText(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            LocalizedText("Quick actions", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                url?.let { AssistChip(onClick = { openSafeIntent(context, Intent(Intent.ACTION_VIEW, Uri.parse(it))) }, label = { LocalizedText("Open link") }) }
                email?.let { AssistChip(onClick = { openSafeIntent(context, Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$it"))) }, label = { LocalizedText("Email") }) }
                phone?.let { AssistChip(onClick = { openSafeIntent(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:${it.filter { c -> c.isDigit() || c == '+' }}"))) }, label = { LocalizedText("Call") }) }
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
            Column(Modifier.weight(1f)) { LocalizedText("Private library", style = MaterialTheme.typography.titleLarge); LocalizedText("Searchable and stored only on this device.") }
            if (history.isNotEmpty()) IconButton(onClick = onClear) { Icon(Icons.Rounded.DeleteOutline, "Clear library") }
        }
        OutlinedTextField(value = query, onValueChange = { query = it }, label = { LocalizedText("Search scans") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        if (filtered.isEmpty()) {
            Card(Modifier.fillMaxWidth()) { LocalizedText(if (history.isEmpty()) "Saved OCR documents will appear here." else "No matching document.", Modifier.padding(24.dp)) }
        }
        filtered.forEach { item ->
            Card(onClick = { onOpen(item) }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) { Icon(Icons.Rounded.Description, null, Modifier.padding(10.dp)) }
                    Column(Modifier.weight(1f)) {
                        LocalizedText(item.title, style = MaterialTheme.typography.titleSmall)
                        LocalizedText(item.text.replace('\n', ' ').take(90), maxLines = 2, style = MaterialTheme.typography.bodySmall)
                        LocalizedText(item.source, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
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

private fun applyOcrFilter(source: Bitmap, filter: OcrFilter): Bitmap {
    if (filter == OcrFilter.Original) return source
    val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    val matrix = ColorMatrix().apply {
        setSaturation(if (filter == OcrFilter.Mono) 0f else 0.25f)
        if (filter == OcrFilter.Clean) {
            val contrast = 1.18f
            val translate = (-0.5f * contrast + 0.5f) * 255f
            postConcat(ColorMatrix(floatArrayOf(contrast,0f,0f,0f,translate, 0f,contrast,0f,0f,translate, 0f,0f,contrast,0f,translate, 0f,0f,0f,1f,0f)))
        }
    }
    Canvas(output).drawBitmap(source, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = ColorMatrixColorFilter(matrix) })
    return output
}

private fun rotateAndLimitOcrBitmap(source: Bitmap, rotation: Int): Bitmap {
    val limited = limitOcrBitmap(source)
    if (limited !== source) source.recycle()
    if (rotation == 0) return limited
    return Bitmap.createBitmap(
        limited,
        0,
        0,
        limited.width,
        limited.height,
        Matrix().apply { postRotate(rotation.toFloat()) },
        true,
    ).also { if (it !== limited) limited.recycle() }
}

private fun prepareOcrBitmap(
    source: Bitmap,
    rotation: Int,
    filter: OcrFilter,
    reviewedCorners: DocumentCorners? = null,
): Bitmap {
    val prepared = rotateAndLimitOcrBitmap(source, rotation)
    val corners = reviewedCorners ?: DocumentEdgeDetector.detect(prepared).let { frame ->
        if (frame.confidence > 0f) DocumentCorners.fromCrop(frame.crop) else DocumentCorners.fullFrame(0.01f)
    }
    val corrected = DocumentPerspectiveCorrector.correct(prepared, corners)
    if (corrected !== prepared) prepared.recycle()
    val filtered = applyOcrFilter(corrected, filter)
    if (filtered !== corrected) corrected.recycle()
    return filtered
}

private fun limitOcrBitmap(source: Bitmap, maxDimension: Int = 1800): Bitmap {
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
    val values = pages.map { it.text.trim() }
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
        OcrHistoryItem(
            item.optString("title"),
            item.optString("text"),
            item.optString("source"),
            item.optString("savedAt"),
            item.optString("documentId"),
            item.optInt("pageCount", 1).coerceAtLeast(1),
        )
    }.filter { it.text.isNotBlank() }
}.getOrDefault(emptyList())

private fun encodeOcrHistory(items: List<OcrHistoryItem>): String = JSONArray().apply {
    items.forEach { item ->
        put(
            JSONObject()
                .put("title", item.title)
                .put("text", item.text)
                .put("source", item.source)
                .put("savedAt", item.savedAt)
                .put("documentId", item.documentId)
                .put("pageCount", item.pageCount),
        )
    }
}.toString()
