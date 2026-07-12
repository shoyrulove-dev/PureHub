package com.purehub.app.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.ClipboardManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.purehub.app.ui.LocalSnackbarHostState
import kotlinx.coroutines.launch

@Composable
fun OcrTextExtractorCard(
    hasCameraPermission: Boolean,
    onRequestCameraPermission: () -> Unit,
) {
    var extractedText by remember {
        mutableStateOf("Point the camera at printed text to run offline OCR.")
    }
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "OCR Text Extractor",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Bundled ML Kit text recognition runs fully on-device, so receipts, notes, and printed pages never leave the phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (hasCameraPermission) {
                OcrCameraPreview(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    onTextDetected = { text ->
                        if (text.isNotBlank()) {
                            extractedText = text
                        }
                    },
                )
            } else {
                Button(onClick = onRequestCameraPermission) {
                    Text("Allow Camera for Offline OCR")
                }
            }

            SelectionContainer {
                Text(
                    text = extractedText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .verticalScroll(rememberScrollState()),
                )
            }
            Button(
                onClick = {
                    val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(
                        android.content.ClipData.newPlainText("PureHub OCR", extractedText),
                    )
                    scope.launch { snackbarHostState.showSnackbar("OCR text copied locally.") }
                },
                enabled = extractedText.isNotBlank(),
            ) {
                Text("Copy Text")
            }
        }
    }
}

@Composable
private fun OcrCameraPreview(
    modifier: Modifier,
    onTextDetected: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier,
    )

    LaunchedEffect(previewView) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        bindOcrCamera(
            context = context,
            previewView = previewView,
            cameraProvider = cameraProvider,
            lifecycleOwner = lifecycleOwner,
            onTextDetected = onTextDetected,
        )
    }
}

@SuppressLint("UnsafeOptInUsageError")
private fun bindOcrCamera(
    context: Context,
    previewView: PreviewView,
    cameraProvider: ProcessCameraProvider,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onTextDetected: (String) -> Unit,
) {
    val preview = Preview.Builder().build().also {
        it.surfaceProvider = previewView.surfaceProvider
    }
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val analyzer = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .also { analysis ->
            analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                processOcrFrame(
                    imageProxy = imageProxy,
                    onTextDetected = onTextDetected,
                    recognizer = recognizer,
                )
            }
        }

    cameraProvider.unbindAll()
    cameraProvider.bindToLifecycle(
        lifecycleOwner,
        CameraSelector.DEFAULT_BACK_CAMERA,
        preview,
        analyzer,
    )
}

private fun processOcrFrame(
    imageProxy: ImageProxy,
    onTextDetected: (String) -> Unit,
    recognizer: com.google.mlkit.vision.text.TextRecognizer,
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }

    val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    recognizer.process(inputImage)
        .addOnSuccessListener { visionText ->
            val text = visionText.text.trim()
            if (text.isNotBlank()) {
                onTextDetected(text)
            }
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}
