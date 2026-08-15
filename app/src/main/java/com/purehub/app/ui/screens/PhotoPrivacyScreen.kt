package com.purehub.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PhotoPrivacyScreen(
    innerPadding: PaddingValues,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf<Uri?>(null) }
    var isExporting by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Choose a photo. PureHub creates a separate clean copy and never changes the original.") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        selected = uri
        status = if (uri == null) "No photo selected." else "Photo ready. Review the privacy boundary, then create a clean JPEG."
    }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { destination ->
        val source = selected
        if (destination != null && source != null) scope.launch {
            isExporting = true
            status = "Creating privacy-clean copy on this device..."
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
                "Clean JPEG saved. GPS, camera, author, and original EXIF blocks were not copied."
            } else {
                "Could not create the clean copy. Try a standard JPEG, PNG, or WebP image."
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(innerPadding).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FlagshipSuiteHeader(
            eyebrow = "Private media utility",
            title = "Photo Privacy",
            description = "Remove location and camera metadata by re-encoding a new share-ready photo locally.",
            accent = MaterialTheme.colorScheme.tertiary,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = .45f),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Rounded.Image, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Text(if (selected == null) "No photo selected" else "Photo selected", fontWeight = FontWeight.Bold)
                        Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { picker.launch(arrayOf("image/*")) },
                    ) { Text("Choose photo") }
                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = selected != null && !isExporting,
                        onClick = { exporter.launch("purehub-privacy-clean.jpg") },
                    ) {
                        Icon(Icons.Rounded.SaveAlt, contentDescription = null)
                        Text(if (isExporting) " Creating..." else " Create copy")
                    }
                }
            }
        }

        PrivacyBoundary(
            icon = { Icon(Icons.Rounded.LocationOff, contentDescription = null) },
            title = "Metadata removed",
            body = "The exported JPEG does not copy GPS, device model, capture time, author, or original EXIF fields.",
        )
        PrivacyBoundary(
            icon = { Icon(Icons.Rounded.CheckCircle, contentDescription = null) },
            title = "Original protected",
            body = "The source remains untouched. You choose the destination and decide when to share the clean copy.",
        )
    }
}

@Composable
private fun PrivacyBoundary(
    icon: @Composable () -> Unit,
    title: String,
    body: String,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                Column(modifier = Modifier.padding(10.dp)) { icon() }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
