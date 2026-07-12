package com.purehub.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.purehub.app.data.CommunityPreferencesRepository
import com.purehub.app.feature.community.CommunityViewModel
import com.purehub.app.ui.LocalSnackbarHostState
import kotlinx.coroutines.launch

private const val TELEGRAM_DEEP_LINK = "tg://resolve?domain=aaa_letan_vip_bot&start=getcode"
private const val TELEGRAM_WEB_LINK = "https://t.me/aaa_letan_vip_bot?start=getcode"

@Composable
fun CommunityScreen(
    innerPadding: PaddingValues,
    embedded: Boolean = false,
) {
    val context = LocalContext.current
    val viewModel: CommunityViewModel = viewModel(
        factory = CommunityViewModel.factory(
            repository = CommunityPreferencesRepository(context.applicationContext),
        ),
    )
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var localCode by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(uiState.proCode) {
        if (localCode.isBlank()) {
            localCode = uiState.proCode
        }
    }

    Column(
        modifier = Modifier
            .then(if (embedded) Modifier.fillMaxSize() else Modifier.fillMaxSize())
            .padding(innerPadding)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (!embedded) {
            Text(
                text = "Pro Features for Free",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (!embedded) {
                    Text(
                        text = "Community Access",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "Join the Telegram bot to receive your personal Pro Code.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val telegramIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(TELEGRAM_DEEP_LINK),
                        )
                        val browserFallbackIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(TELEGRAM_WEB_LINK),
                        )

                        try {
                            context.startActivity(telegramIntent)
                            scope.launch { snackbarHostState.showSnackbar("Opening Telegram community link.") }
                        } catch (_: ActivityNotFoundException) {
                            context.startActivity(browserFallbackIntent)
                            scope.launch { snackbarHostState.showSnackbar("Telegram not found. Opened browser fallback.") }
                        }
                    },
                ) {
                    Text("Join Community to get Pro Code")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (!embedded) {
                    Text(
                        text = "Unlock Pro",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = localCode,
                    onValueChange = { localCode = it },
                    label = { Text("Enter received Pro Code") },
                    supportingText = {
                        Text("Stored locally only. No cloud verification, no analytics, no account required.")
                    },
                    singleLine = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.saveCode(localCode.trim())
                            scope.launch { snackbarHostState.showSnackbar("Pro code saved locally.") }
                        },
                    ) {
                        Text("Save Code")
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            localCode = ""
                            viewModel.saveCode("")
                            scope.launch { snackbarHostState.showSnackbar("Pro code reset on this device.") }
                        },
                    ) {
                        Text("Reset")
                    }
                }
                Text(
                    text = if (uiState.isUnlocked) {
                        "Status: Pro unlocked on this device."
                    } else {
                        "Status: Free mode active."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (uiState.isUnlocked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}
