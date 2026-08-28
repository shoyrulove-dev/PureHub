package com.purehub.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import com.purehub.app.ui.LocalizedText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Composable
fun FileStudioCard() {
    val context = LocalContext.current
    var files by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var hashes by remember { mutableStateOf<List<String>>(emptyList()) }
    var message by remember { mutableStateOf("Choose local files. PureHub never uploads them.") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { files = it; hashes = emptyList() }
    val zipCreator = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { destination ->
        if (destination != null) runCatching {
            context.contentResolver.openOutputStream(destination)?.use { output ->
                ZipOutputStream(output).use { zip ->
                    files.forEachIndexed { index, uri ->
                        val name = queryDisplayName(context, uri) ?: "file-${index + 1}"
                        zip.putNextEntry(ZipEntry(name))
                        context.contentResolver.openInputStream(uri)?.use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
            message = "ZIP created locally."
        }.onFailure { message = "Could not create ZIP: ${it.message}" }
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FlagshipSuiteHeader("File utility flagship", "File Studio", "Inspect, hash, archive and share files through Android's private document picker.")
            LocalizedText(message)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { picker.launch(arrayOf("*/*")) }) { LocalizedText("Choose") }
                OutlinedButton(onClick = { zipCreator.launch("purehub-files.zip") }, enabled = files.isNotEmpty()) { LocalizedText("Create ZIP") }
                OutlinedButton(onClick = {
                    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply { type = "*/*"; putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(files)); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                    context.startActivity(Intent.createChooser(intent, "Share from PureHub"))
                }, enabled = files.isNotEmpty()) { LocalizedText("Share") }
            }
            OutlinedButton(onClick = {
                hashes = files.map { uri ->
                    val digest = MessageDigest.getInstance("SHA-256")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        val buffer = ByteArray(8192); var count: Int
                        while (input.read(buffer).also { count = it } > 0) digest.update(buffer, 0, count)
                    }
                    digest.digest().joinToString("") { "%02x".format(it) }
                }
                message = "SHA-256 checksums calculated locally."
            }, enabled = files.isNotEmpty()) { LocalizedText("Calculate SHA-256") }
            files.forEachIndexed { index, uri -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(10.dp)) { LocalizedText(queryDisplayName(context, uri) ?: uri.lastPathSegment.orEmpty()); hashes.getOrNull(index)?.let { LocalizedText(it, style = androidx.compose.material3.MaterialTheme.typography.labelSmall) } } } }
        }
    }
}

private fun queryDisplayName(context: android.content.Context, uri: Uri): String? = context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
