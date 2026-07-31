package com.purehub.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.app.Activity
import android.os.Build
import android.os.PersistableBundle
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.purehub.app.feature.vault.PasswordVaultRepository
import com.purehub.app.feature.vault.PasswordVaultViewModel
import com.purehub.app.ui.LocalSnackbarHostState
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job

@Composable
fun PasswordVaultCard() {
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    var clipboardClearJob by remember { mutableStateOf<Job?>(null) }
    val activity = context as? Activity
    DisposableEffect(activity) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            clipboardClearJob?.cancel()
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
    val viewModel: PasswordVaultViewModel = viewModel(
        factory = PasswordVaultViewModel.factory(
            repository = PasswordVaultRepository(context.applicationContext),
        ),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Password Vault",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = uiState.note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = uiState.draftTitle,
                onValueChange = viewModel::updateDraftTitle,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Entry title") },
                singleLine = true,
            )
            OutlinedTextField(
                value = uiState.draftUsername,
                onValueChange = viewModel::updateDraftUsername,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Username") },
                singleLine = true,
            )
            OutlinedTextField(
                value = uiState.draftPassword,
                onValueChange = viewModel::updateDraftPassword,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )
            Button(
                onClick = viewModel::saveDraft,
                enabled = uiState.draftTitle.isNotBlank() && uiState.draftPassword.isNotBlank(),
            ) {
                Text("Save Encrypted Entry")
            }
            if (uiState.entries.isEmpty()) {
                Text(
                    text = "No entries saved yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    uiState.entries.forEach { entry ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = entry.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = if (entry.username.isBlank()) "No username" else entry.username,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    text = "•••••••• (${entry.password.length} chars)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = {
                                            val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText(entry.title, entry.password)
                                            clip.description.extras = PersistableBundle().apply {
                                                putBoolean("android.content.extra.IS_SENSITIVE", true)
                                            }
                                            clipboard.setPrimaryClip(clip)
                                            clipboardClearJob?.cancel()
                                            clipboardClearJob = scope.launch {
                                                snackbarHostState.showSnackbar("Password copied. Clipboard clears in 30 seconds.")
                                                delay(30_000)
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                                    clipboard.clearPrimaryClip()
                                                } else {
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                                                }
                                            }
                                        },
                                    ) {
                                        Text("Copy Password")
                                    }
                                    Button(onClick = { viewModel.deleteEntry(entry.id) }) {
                                        Text("Delete")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Text(
                text = "Screenshots and Android cloud backup are blocked while this vault is open. Keep a separate secure backup; this local vault should not be your only copy of critical credentials.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
