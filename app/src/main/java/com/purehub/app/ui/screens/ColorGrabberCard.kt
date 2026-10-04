package com.purehub.app.ui.screens

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.purehub.app.ui.LocalAppLanguage
import com.purehub.app.ui.LocalSnackbarHostState
import com.purehub.app.ui.LocalizedText
import com.purehub.app.ui.translateUiText
import kotlinx.coroutines.launch

enum class ColorGrabberMenuAction { Camera, CopyHex }

@Composable
fun ColorGrabberCard(
    hasCameraPermission: Boolean,
    onRequestCameraPermission: () -> Unit,
    menuAction: ColorGrabberMenuAction? = null,
    onMenuActionHandled: () -> Unit = {},
    innerPadding: PaddingValues = PaddingValues(0.dp),
) {
    var color by remember { mutableStateOf(GrabbedColor(64, 112, 176)) }
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val appLanguage = LocalAppLanguage.current

    fun copyHex() {
        val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("PureHub Color", color.hex))
        scope.launch { snackbarHostState.showSnackbar(translateUiText("Color HEX copied locally.", appLanguage)) }
    }

    LaunchedEffect(menuAction) {
        when (menuAction) {
            ColorGrabberMenuAction.Camera -> if (!hasCameraPermission) onRequestCameraPermission()
            ColorGrabberMenuAction.CopyHex -> copyHex()
            null -> return@LaunchedEffect
        }
        onMenuActionHandled()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF07111E))) {
        if (hasCameraPermission) {
            ColorGrabberPreview(
                modifier = Modifier.fillMaxSize(),
                onColorSampled = { sampled -> color = sampled },
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 60.dp)
                    .fillMaxWidth(0.9f)
                    .aspectRatio(0.59f)
                    .border(3.dp, Color(0xFF67E8F9), RoundedCornerShape(24.dp)),
            )
        } else {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LocalizedText(
                    "Camera stays off until you allow it",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                )
                Button(onClick = onRequestCameraPermission) {
                    LocalizedText("Allow Camera for Color Grabber")
                }
            }
        }

        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(Color(color.red, color.green, color.blue), CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    LocalizedText(
                        text = color.hex,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    LocalizedText(
                        text = "RGB ${color.red}, ${color.green}, ${color.blue}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(onClick = ::copyHex, modifier = Modifier.height(42.dp)) {
                    Icon(
                        Icons.Rounded.ContentCopy,
                        contentDescription = translateUiText("Copy HEX", appLanguage),
                        modifier = Modifier.size(18.dp),
                    )
                    LocalizedText(" Copy")
                }
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

    AndroidView(factory = { previewView }, modifier = modifier)

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
    val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
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
    cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analyzer)
}

private fun processColorFrame(imageProxy: ImageProxy, onColorSampled: (GrabbedColor) -> Unit) {
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
