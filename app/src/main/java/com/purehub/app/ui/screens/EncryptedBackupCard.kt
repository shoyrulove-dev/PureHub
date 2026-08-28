package com.purehub.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import com.purehub.app.ui.LocalizedText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.purehub.app.feature.backup.EncryptedBackupManager
import java.text.DateFormat
import java.util.Date
import com.purehub.app.ui.LocalSnackbarHostState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun EncryptedBackupCard() {
    val context = LocalContext.current
    val manager = remember { EncryptedBackupManager(context.applicationContext) }
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    var passphrase by rememberSaveable { mutableStateOf("") }
    var pendingExport by remember { mutableStateOf<String?>(null) }
    var backupStatus by remember { mutableStateOf(manager.backupStatus()) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val content = pendingExport
        if (uri != null && content != null) scope.launch(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(content) }
            manager.recordSuccessfulExport(content)
            withContext(Dispatchers.Main) {
                backupStatus = manager.backupStatus()
                snackbar.showSnackbar("Encrypted PureHub backup saved and fingerprint recorded.")
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val raw = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("File unavailable") }
                withContext(Dispatchers.IO) { manager.import(raw, passphrase.toCharArray()) }
            }.onSuccess { snackbar.showSnackbar("Backup restored. Reopen tools to refresh their data.") }
                .onFailure { snackbar.showSnackbar(it.message ?: "Backup import failed.") }
        }
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary)
                LocalizedText("Encrypted private backup", style = MaterialTheme.typography.titleMedium)
            }
            LocalizedText("Habit, check-ins, expenses, passwords, 2FA accounts and OCR/QR history. AES-256; your passphrase never leaves this device.", style = MaterialTheme.typography.bodySmall)
            LocalizedText(
                text = if (backupStatus.exportedAtMillis > 0) {
                    "Last saved ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(backupStatus.exportedAtMillis))} · fingerprint ${backupStatus.fingerprint}"
                } else {
                    "No verified export recorded on this device yet."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = passphrase, onValueChange = { passphrase = it }, modifier = Modifier.fillMaxWidth(),
                label = { LocalizedText("Backup passphrase (8+ characters)") },
                visualTransformation = PasswordVisualTransformation(), singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(enabled = passphrase.length >= 8, onClick = {
                    scope.launch {
                        runCatching { withContext(Dispatchers.IO) { manager.export(passphrase.toCharArray()) } }
                            .onSuccess { pendingExport = it; exportLauncher.launch("purehub-backup.purehub") }
                            .onFailure { snackbar.showSnackbar(it.message ?: "Backup export failed.") }
                    }
                }) { LocalizedText("Export") }
                Button(enabled = passphrase.length >= 8, onClick = { importLauncher.launch(arrayOf("*/*")) }) { LocalizedText("Import") }
            }
        }
    }
}
