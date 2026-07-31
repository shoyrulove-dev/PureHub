package com.purehub.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.purehub.app.feature.catalog.MiniAppId
import com.purehub.app.feature.catalog.MiniAppTab
import com.purehub.app.feature.catalog.miniAppsByTab

@Composable
fun ToolsScreen(
    innerPadding: PaddingValues,
    onOpenMiniApp: (MiniAppId) -> Unit,
    showAllTools: Boolean = false,
) {
    val visibleTools = rememberVisibleTools()
    var query by rememberSaveable { mutableStateOf("") }
    val tabs = if (showAllTools) MiniAppTab.entries else listOf(MiniAppTab.MEASURE_TOOLS)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    text = if (showAllTools) "All tools" else MiniAppTab.MEASURE_TOOLS.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Free, private and ad-free utilities. Search by what you need to do.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                placeholder = { Text("What do you want to do?") },
                shape = MaterialTheme.shapes.large,
            )
        }

        tabs.forEach { tab ->
            val tools = miniAppsByTab.getValue(tab).filter { tool ->
                tool in visibleTools && (query.isBlank() || tool.title.contains(query.trim(), ignoreCase = true))
            }
            if (tools.isNotEmpty()) {
                item(key = "${tab.name}-title") {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = tab.accent,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                tools.forEach { tool ->
                    item(key = tool.name) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            onClick = { onOpenMiniApp(tool) },
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = tab.accentContainer,
                                ) {
                                    Icon(
                                        imageVector = tool.icon,
                                        contentDescription = null,
                                        tint = tab.accent,
                                        modifier = Modifier.padding(12.dp).size(22.dp),
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tool.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = toolFriendlySummary(tool),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = "Open ${tool.title}",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun toolFriendlySummary(tool: MiniAppId): String = when (tool) {
    MiniAppId.LUNAR_CALENDAR -> "Vietnamese lunar dates, can-chi and traditional markers."
    MiniAppId.ZEN_HABIT -> "Build simple habits without accounts or pressure."
    MiniAppId.ZEN_POMODORO -> "A calm focus timer with local soundscapes."
    MiniAppId.ZEN_BREATH -> "Guided breathing with gentle motion."
    MiniAppId.COMPASS -> "Direction, bearing and sensor guidance."
    MiniAppId.BUBBLE_LEVEL -> "Quick two-axis leveling and calibration."
    MiniAppId.DECIBEL_METER -> "Private estimated sound-level monitoring."
    MiniAppId.SMART_FLASHLIGHT -> "Torch, screen light and safety patterns."
    MiniAppId.UNIT_CONVERTER -> "Fast offline unit conversion."
    MiniAppId.QR_STUDIO -> "Scan and generate QR codes locally."
    MiniAppId.DOC_TO_PDF -> "Capture, arrange and export private PDFs."
    MiniAppId.OCR_TEXT -> "Extract and copy text from images offline."
    MiniAppId.COLOR_GRABBER -> "Sample and copy colors from the camera."
    MiniAppId.DEEP_CLEANER -> "Review reclaimable files before deleting."
    MiniAppId.SPEAKER_CLEANER -> "Play a controlled tone for residual water."
    MiniAppId.WIFI_ANALYZER -> "Inspect nearby signals and Wi-Fi channels."
    MiniAppId.PASSWORD_VAULT -> "Encrypted local credentials with device protection."
    MiniAppId.WALLPAPER_CHANGER -> "Local wallpaper preview and rotation."
    MiniAppId.BILL_SPLITTER -> "Split items, tax and tips for a group."
    MiniAppId.EXPENSE_TRACKER -> "Private offline expense ledger."
    MiniAppId.DECISION_WHEEL -> "A fair local picker for quick choices."
    MiniAppId.COMMUNITY_UNLOCK -> "Telegram, GitHub and the PureHub roadmap."
}
