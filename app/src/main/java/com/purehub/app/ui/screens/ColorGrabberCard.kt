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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.purehub.app.feature.colorgrabber.ColorGrabberUtils
import com.purehub.app.feature.colorgrabber.GrabbedColor
import com.purehub.app.ui.LocalSnackbarHostState
import kotlinx.coroutines.launch

@Composable
fun ColorGrabberCard(
    hasCameraPermission: Boolean,
    onRequestCameraPermission: () -> Unit,
) {
    var color by remember { mutableStateOf(GrabbedColor(64, 112, 176)) }
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
                text = "Color Grabber",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "CameraX samples the center pixel in real time so you can pick HEX and RGB values without sending frames anywhere.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (hasCameraPermission) {
                ColorGrabberPreview(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    onColorSampled = { sampled ->
                        color = sampled
                    },
                )
            } else {
                Button(onClick = onRequestCameraPermission) {
                    Text("Allow Camera for Color Grabber")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            color = Color(color.red, color.green, color.blue),
                            shape = CircleShape,
                        ),
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = color.hex,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "RGB ${color.red}, ${color.green}, ${color.blue}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "${ColorGrabberUtils.describeBrightness(color)} tone • R ${ColorGrabberUtils.toPercent(color.red)} • G ${ColorGrabberUtils.toPercent(color.green)} • B ${ColorGrabberUtils.toPercent(color.blue)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Button(
                onClick = {
                    val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(
                        android.content.ClipData.newPlainText("PureHub Color", color.hex),
                    )
                    scope.launch { snackbarHostState.showSnackbar("Color HEX copied locally.") }
                },
            ) {
                Text("Copy HEX")
            }
        }
    }
}

@Composable
private fun ColorGrabberPreview(
    modifier: Modifier,
    onColorSampled: (GrabbedColor) -> Unit,
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
        bindColorGrabberCamera(
            context = context,
            previewView = previewView,
            cameraProvider = cameraProvider,
            lifecycleOwner = lifecycleOwner,
            onColorSampled = onColorSampled,
        )
    }
}

@SuppressLint("UnsafeOptInUsageError")
private fun bindColorGrabberCamera(
    context: Context,
    previewView: PreviewView,
    cameraProvider: ProcessCameraProvider,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onColorSampled: (GrabbedColor) -> Unit,
) {
    val preview = Preview.Builder().build().also {
        it.surfaceProvider = previewView.surfaceProvider
    }
    val analyzer = ImageAnalysis.Builder()
        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .also { analysis ->
            analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                processColorFrame(imageProxy, onColorSampled)
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

private fun processColorFrame(
    imageProxy: ImageProxy,
    onColorSampled: (GrabbedColor) -> Unit,
) {
    val plane = imageProxy.planes.firstOrNull()
    if (plane == null) {
        imageProxy.close()
        return
    }

    val buffer = plane.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    onColorSampled(ColorGrabberUtils.sampleCenterRgba(bytes, imageProxy.width, imageProxy.height))
    imageProxy.close()
}
