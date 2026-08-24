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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.purehub.app.feature.vault.PasswordVaultRepository
import com.purehub.app.feature.vault.PasswordVaultViewModel
import com.purehub.app.feature.vault.VaultSecurity
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
    var mode by rememberSaveable { mutableStateOf(SuiteMode.QUICK) }
    var query by rememberSaveable { mutableStateOf("") }
    var showDraft by rememberSaveable { mutableStateOf(false) }
    var revealedEntryId by rememberSaveable { mutableStateOf<String?>(null) }
    val health = remember(uiState.entries) { VaultSecurity.health(uiState.entries) }
    val filteredEntries = remember(uiState.entries, query) {
        uiState.entries.filter { query.isBlank() || it.title.contains(query, true) || it.username.contains(query, true) }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FlagshipSuiteHeader(
                eyebrow = "Security Suite flagship",
                title = "Password Vault",
                description = "Keep credentials protected with Android-backed encryption, guarded screenshots and sensitive clipboard cleanup.",
            )
            SuiteModeSwitch(
                mode = mode,
                onModeChanged = { mode = it },
                proHint = "Local health audit, strong generator, search, and deliberate reveal controls.",
            )
            if (mode == SuiteMode.PRO) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Vault health ${health.score}/100", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        LinearProgressIndicator(progress = { health.score / 100f }, modifier = Modifier.fillMaxWidth())
                        Text(
                            "${health.total} entries · ${health.weak} weak · ${health.reused} reused",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Text(text = uiState.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                visualTransformation = if (showDraft) VisualTransformation.None else PasswordVisualTransformation(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = viewModel::generatePassword) { Text("Generate strong") }
                OutlinedButton(onClick = { showDraft = !showDraft }) { Text(if (showDraft) "Hide" else "Reveal") }
            }
            Button(
                onClick = viewModel::saveDraft,
                enabled = uiState.draftTitle.isNotBlank() && uiState.draftPassword.isNotBlank(),
            ) {
                Text("Save Encrypted Entry")
            }
            PrivacyReceipt(
                action = "Android-backed encrypted storage",
                detail = "Screenshots are blocked here; copied secrets are marked sensitive and cleared after 30 seconds.",
            )
            if (mode == SuiteMode.PRO && uiState.entries.isNotEmpty()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search entries") },
                    singleLine = true,
                )
            }
            if (filteredEntries.isEmpty()) {
                Text(
                    text = if (uiState.entries.isEmpty()) "No entries saved yet." else "No matching entry.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    filteredEntries.forEach { entry ->
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
                                    text = if (revealedEntryId == entry.id) entry.password else "Password hidden (${entry.password.length} chars)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    OutlinedButton(onClick = {
                                        revealedEntryId = if (revealedEntryId == entry.id) null else entry.id
                                    }) { Text(if (revealedEntryId == entry.id) "Hide" else "Reveal") }
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
