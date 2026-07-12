package com.purehub.app.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import androidx.camera.core.ImageProxy

@Composable
fun QrStudioScreen(
    hasCameraPermission: Boolean,
    onRequestCameraPermission: () -> Unit,
    innerPadding: PaddingValues = PaddingValues(0.dp),
) {
    var qrText by rememberSaveable { mutableStateOf("https://example.com/purehub") }
    var latestScan by rememberSaveable { mutableStateOf("No code scanned yet") }
    val qrBitmap = remember(qrText) { com.purehub.app.feature.qr.QrBitmapGenerator.generate(qrText) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "QR Generator",
                    style = MaterialTheme.typography.titleLarge,
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = qrText,
                    onValueChange = { qrText = it },
                    label = { Text("Text or URL") },
                    supportingText = { Text("Generated offline with ZXing core.") },
                )
                qrBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Generated QR code",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "QR Scanner",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = latestScan,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (hasCameraPermission) {
                    QrCameraPreview(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        onCodeDetected = { latestScan = it },
                    )
                } else {
                    Button(onClick = onRequestCameraPermission) {
                        Text("Allow Camera for Offline Scanning")
                    }
                }
            }
        }
    }
}

@Composable
private fun QrCameraPreview(
    modifier: Modifier,
    onCodeDetected: (String) -> Unit,
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
        bindQrCamera(
            context = context,
            previewView = previewView,
            cameraProvider = cameraProvider,
            lifecycleOwner = lifecycleOwner,
            onCodeDetected = onCodeDetected,
        )
    }
}

@SuppressLint("UnsafeOptInUsageError")
private fun bindQrCamera(
    context: Context,
    previewView: PreviewView,
    cameraProvider: ProcessCameraProvider,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onCodeDetected: (String) -> Unit,
) {
    val preview = Preview.Builder().build().also {
        it.surfaceProvider = previewView.surfaceProvider
    }

    val barcodeScanner = BarcodeScanning.getClient()
    val analyzer = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .also { analysis ->
            analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                processQrFrame(
                    imageProxy = imageProxy,
                    onCodeDetected = onCodeDetected,
                    scanner = barcodeScanner,
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

private fun processQrFrame(
    imageProxy: ImageProxy,
    onCodeDetected: (String) -> Unit,
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }

    val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    scanner.process(inputImage)
        .addOnSuccessListener { barcodes ->
            val detected = barcodes.firstOrNull()?.rawValue
            if (!detected.isNullOrBlank()) {
                onCodeDetected(detected)
            }
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}
