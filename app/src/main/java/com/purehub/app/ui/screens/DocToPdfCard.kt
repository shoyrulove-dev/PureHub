package com.purehub.app.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.purehub.app.feature.docpdf.CapturedDocPage
import com.purehub.app.feature.docpdf.CropAdjustments
import com.purehub.app.feature.docpdf.DocPdfRepository
import com.purehub.app.feature.docpdf.ExportedPdf
import com.purehub.app.ui.LocalSnackbarHostState
import java.util.concurrent.Executor
import kotlinx.coroutines.launch

@Composable
fun DocToPdfCard(
    hasCameraPermission: Boolean,
    onRequestCameraPermission: () -> Unit,
    innerPadding: PaddingValues = PaddingValues(0.dp),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val repository = remember { DocPdfRepository(context.applicationContext) }
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val cameraExecutor = remember { ContextCompat.getMainExecutor(context) }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    val pages = remember { mutableStateListOf<CapturedDocPage>() }
    var selectedPageIndex by remember { mutableIntStateOf(-1) }
    var crop by remember { mutableStateOf(CropAdjustments(left = 0.04f, top = 0.04f, right = 0.04f, bottom = 0.04f)) }
    var documentTitle by rememberSaveable { mutableStateOf("purehub_doc") }
    var exportMessage by rememberSaveable { mutableStateOf("Capture pages, then export a local PDF.") }
    var exportedPdf by remember { mutableStateOf<ExportedPdf?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(innerPadding),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Doc to PDF",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = exportMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = documentTitle,
                onValueChange = { documentTitle = it },
                label = { Text("Document title") },
                singleLine = true,
            )

            if (hasCameraPermission) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                )
                LaunchedEffect(previewView) {
                    bindDocumentCamera(
                        context = context,
                        previewView = previewView,
                        lifecycleOwner = lifecycleOwner,
                        imageCapture = imageCapture,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            capturePage(
                                context = context,
                                repository = repository,
                                imageCapture = imageCapture,
                                executor = cameraExecutor,
                                onPageCaptured = {
                                    pages += it
                                    selectedPageIndex = pages.lastIndex
                                    exportMessage = "${pages.size} page(s) staged locally."
                                },
                                onError = { message -> exportMessage = message },
                            )
                        },
                    ) {
                        Text("Capture Page")
                    }
                    Button(
                        onClick = {
                            if (pages.isEmpty()) {
                                exportMessage = "Capture at least one page before export."
                            } else {
                                exportedPdf = repository.exportPdf(pages, documentTitle)
                                exportMessage = "Saved PDF to ${exportedPdf?.file?.absolutePath}"
                                scope.launch { snackbarHostState.showSnackbar("PDF exported locally.") }
                            }
                        },
                    ) {
                        Text("Export PDF")
                    }
                }

                exportedPdf?.let { pdf ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                val openIntent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(pdf.uri, "application/pdf")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(openIntent, "Open PDF"))
                                scope.launch { snackbarHostState.showSnackbar("Opening exported PDF.") }
                            },
                        ) {
                            Text("Open PDF")
                        }
                        Button(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, pdf.uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share PDF"))
                                scope.launch { snackbarHostState.showSnackbar("PDF ready to share.") }
                            },
                        ) {
                            Text("Share PDF")
                        }
                    }
                }

                if (pages.isNotEmpty()) {
                    Text(
                        text = "Crop selected page directly",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    CropSlider("Left", crop.left) { crop = crop.copy(left = it) }
                    CropSlider("Top", crop.top) { crop = crop.copy(top = it) }
                    CropSlider("Right", crop.right) { crop = crop.copy(right = it) }
                    CropSlider("Bottom", crop.bottom) { crop = crop.copy(bottom = it) }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                val index = selectedPageIndex
                                if (index in pages.indices) {
                                    pages[index] = repository.enhancePage(pages[index], crop)
                                    exportMessage = "Applied crop and enhancement to page ${index + 1}."
                                }
                            },
                        ) {
                            Text("Apply Crop")
                        }
                        Button(
                            onClick = {
                                if (selectedPageIndex > 0) {
                                    val current = pages[selectedPageIndex]
                                    pages[selectedPageIndex] = pages[selectedPageIndex - 1]
                                    pages[selectedPageIndex - 1] = current
                                    selectedPageIndex -= 1
                                }
                            },
                        ) {
                            Text("Move Left")
                        }
                        Button(
                            onClick = {
                                if (selectedPageIndex in 0 until pages.lastIndex) {
                                    val current = pages[selectedPageIndex]
                                    pages[selectedPageIndex] = pages[selectedPageIndex + 1]
                                    pages[selectedPageIndex + 1] = current
                                    selectedPageIndex += 1
                                }
                            },
                        ) {
                            Text("Move Right")
                        }
                        Button(
                            onClick = {
                                if (selectedPageIndex in pages.indices) {
                                    pages.removeAt(selectedPageIndex)
                                    selectedPageIndex = if (pages.isEmpty()) -1 else (selectedPageIndex - 1).coerceAtLeast(0)
                                    exportMessage = "Removed selected page."
                                }
                            },
                        ) {
                            Text("Remove")
                        }
                    }

                    val selectedPage = pages.getOrNull(selectedPageIndex)
                    selectedPage?.let { page ->
                        val largeBitmap = remember(page.file.absolutePath, page.file.lastModified()) {
                            BitmapFactory.decodeFile(page.file.absolutePath)
                        }
                        largeBitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Selected captured page",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        pages.forEachIndexed { index, page ->
                            val bitmap = remember(page.file.absolutePath, page.file.lastModified()) {
                                BitmapFactory.decodeFile(page.file.absolutePath)
                            }
                            Card(
                                modifier = Modifier.padding(vertical = 4.dp),
                                onClick = { selectedPageIndex = index },
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    bitmap?.let {
                                        Image(
                                            bitmap = it.asImageBitmap(),
                                            contentDescription = "Captured page",
                                            modifier = Modifier.size(100.dp),
                                        )
                                    }
                                    Text(
                                        text = if (selectedPageIndex == index) "Page ${index + 1} selected" else "Page ${index + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Button(onClick = onRequestCameraPermission) {
                    Text("Allow Camera for Doc Capture")
                }
            }
        }
    }
}

@Composable
private fun CropSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "$label ${(value * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..0.18f,
        )
    }
}

private fun bindDocumentCamera(
    context: Context,
    previewView: PreviewView,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    imageCapture: ImageCapture,
) {
    val provider = ProcessCameraProvider.getInstance(context).get()
    val preview = Preview.Builder().build().also {
        it.surfaceProvider = previewView.surfaceProvider
    }
    provider.unbindAll()
    provider.bindToLifecycle(
        lifecycleOwner,
        CameraSelector.DEFAULT_BACK_CAMERA,
        preview,
        imageCapture,
    )
}

private fun capturePage(
    context: Context,
    repository: DocPdfRepository,
    imageCapture: ImageCapture,
    executor: Executor,
    onPageCaptured: (CapturedDocPage) -> Unit,
    onError: (String) -> Unit,
) {
    val outputFile = repository.createCaptureFile()
    val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                onPageCaptured(repository.wrapCapturedFile(outputFile))
            }

            override fun onError(exception: ImageCaptureException) {
                val message = exception.message ?: "Capture failed"
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                onError(message)
            }
        },
    )
}
