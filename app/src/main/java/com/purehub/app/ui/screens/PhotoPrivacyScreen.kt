package com.purehub.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.exifinterface.media.ExifInterface
import com.purehub.app.ui.LocalizedText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PhotoPrivacyMenuAction { ChoosePhoto }

@Composable
fun PhotoPrivacyScreen(
    innerPadding: PaddingValues,
    menuAction: PhotoPrivacyMenuAction? = null,
    onMenuActionHandled: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf<Uri?>(null) }
    var isExporting by remember { mutableStateOf(false) }
    var metadataSummary by remember { mutableStateOf<PhotoMetadataSummary?>(null) }
    var status by remember { mutableStateOf("Choose a photo to remove its shareable metadata locally.") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        selected = uri
        metadataSummary = null
        status = if (uri == null) "No photo selected." else "Reading metadata on this device..."
        if (uri != null) scope.launch {
            metadataSummary = withContext(Dispatchers.IO) { inspectPhotoMetadata(context, uri) }
            status = "Ready. Create a new JPEG without the original metadata."
        }
    }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { destination ->
        val source = selected
        if (destination != null && source != null) scope.launch {
            isExporting = true
            status = "Creating your clean copy locally..."
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val bitmap = context.contentResolver.openInputStream(source)?.use(BitmapFactory::decodeStream)
                        ?: error("The selected image could not be decoded")
                    try {
                        context.contentResolver.openOutputStream(destination)?.use { output ->
                            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)) { "JPEG export failed" }
                        } ?: error("The destination is unavailable")
                    } finally {
                        bitmap.recycle()
                    }
                }
            }
            isExporting = false
            status = if (result.isSuccess) {
                "Clean JPEG saved. The original photo was not changed."
            } else {
                "Could not create the clean copy. Try a JPEG, PNG, or WebP image."
            }
        }
    }

    LaunchedEffect(menuAction) {
        if (menuAction == PhotoPrivacyMenuAction.ChoosePhoto) {
            picker.launch(arrayOf("image/*"))
            onMenuActionHandled()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = .52f),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                    Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.LocationOff, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    LocalizedText("Photo Privacy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    LocalizedText(
                        "Create a clean copy without location or camera metadata.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(188.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .58f),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(42.dp),
                        )
                        LocalizedText(
                            if (selected == null) "Choose a photo" else "Photo ready",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        LocalizedText(
                            status,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (selected == null) {
                    Button(
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        onClick = { picker.launch(arrayOf("image/*")) },
                    ) {
                        Icon(Icons.Rounded.Image, contentDescription = null)
                        LocalizedText(" Choose photo")
                    }
                } else {
                    metadataSummary?.let { summary ->
                        MetadataSummary(summary)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f).height(48.dp),
                            onClick = { picker.launch(arrayOf("image/*")) },
                        ) {
                            LocalizedText("Change")
                        }
                        Button(
                            modifier = Modifier.weight(1.35f).height(48.dp),
                            enabled = !isExporting,
                            onClick = { exporter.launch("purehub-privacy-clean.jpg") },
                        ) {
                            Icon(Icons.Rounded.SaveAlt, contentDescription = null)
                            LocalizedText(if (isExporting) " Creating..." else " Create copy")
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .42f),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    LocalizedText("Original stays protected", fontWeight = FontWeight.Bold)
                    LocalizedText(
                        "Only the new JPEG is saved. GPS, device and other EXIF fields are left out.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetadataSummary(summary: PhotoMetadataSummary) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            LocalizedText("Metadata found", fontWeight = FontWeight.Bold)
            LocalizedText(
                listOfNotNull(
                    "${summary.width} x ${summary.height}",
                    "GPS ${if (summary.hasLocation) "present" else "not found"}",
                    summary.camera.takeIf(String::isNotBlank)?.let { "Camera $it" },
                    summary.captureTime.takeIf(String::isNotBlank)?.let { "Captured $it" },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = if (summary.hasLocation) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private data class PhotoMetadataSummary(
    val width: Int,
    val height: Int,
    val hasLocation: Boolean,
    val camera: String,
    val captureTime: String,
)

private fun inspectPhotoMetadata(context: android.content.Context, uri: Uri): PhotoMetadataSummary {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    val exif = runCatching {
        context.contentResolver.openInputStream(uri)?.use(::ExifInterface)
    }.getOrNull()
    return PhotoMetadataSummary(
        width = bounds.outWidth.coerceAtLeast(0),
        height = bounds.outHeight.coerceAtLeast(0),
        hasLocation = exif?.latLong != null,
        camera = listOfNotNull(
            exif?.getAttribute(ExifInterface.TAG_MAKE)?.trim()?.takeIf(String::isNotBlank),
            exif?.getAttribute(ExifInterface.TAG_MODEL)?.trim()?.takeIf(String::isNotBlank),
        ).distinct().joinToString(" "),
        captureTime = exif?.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL).orEmpty(),
    )
}
