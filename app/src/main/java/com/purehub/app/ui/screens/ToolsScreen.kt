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
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import com.purehub.app.ui.LocalizedText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.purehub.app.feature.catalog.MiniAppId
import com.purehub.app.feature.catalog.MiniAppTab
import com.purehub.app.feature.catalog.miniAppsByTab
import com.purehub.app.data.ToolVisibilityRepository
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ToolsScreen(
    innerPadding: PaddingValues,
    onOpenMiniApp: (MiniAppId) -> Unit,
    showAllTools: Boolean = false,
) {
    val visibleTools = rememberVisibleTools()
    val favoriteTools = rememberFavoriteTools()
    val context = LocalContext.current
    val preferences = remember { ToolVisibilityRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    val tabs = if (showAllTools) MiniAppTab.entries else listOf(MiniAppTab.MEASURE_TOOLS)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                LocalizedText(
                    text = if (showAllTools) "All tools" else MiniAppTab.MEASURE_TOOLS.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                LocalizedText(
                    text = "Free, private and ad-free utilities. Search by what you need to do.",
                    style = MaterialTheme.typography.bodyMedium,
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
                placeholder = { LocalizedText("What do you want to do?") },
                shape = MaterialTheme.shapes.large,
            )
        }

        tabs.forEach { tab ->
            val tools = miniAppsByTab.getValue(tab).filter { tool ->
                tool in visibleTools && toolMatchesSearch(tool, query)
            }
            if (tools.isNotEmpty()) {
                item(key = "${tab.name}-title") {
                    LocalizedText(
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
                                modifier = Modifier.padding(11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = tab.accentContainer,
                                ) {
                                    Icon(
                                        imageVector = tool.icon,
                                        contentDescription = null,
                                        tint = tab.accent,
                                        modifier = Modifier.padding(9.dp).size(21.dp),
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    LocalizedText(
                                        text = tool.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    LocalizedText(
                                        text = toolFriendlySummary(tool),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                IconButton(onClick = {
                                    scope.launch { preferences.setToolFavorite(tool.name, tool !in favoriteTools) }
                                }) {
                                    Icon(
                                        imageVector = if (tool in favoriteTools) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                        contentDescription = if (tool in favoriteTools) "Remove ${tool.title} from favorites" else "Add ${tool.title} to favorites",
                                        tint = if (tool in favoriteTools) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
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

private fun toolMatchesSearch(tool: MiniAppId, query: String): Boolean {
    val terms = query.trim().lowercase().split(Regex("\\s+")).filter(String::isNotBlank)
    if (terms.isEmpty()) return true
    val searchable = "${tool.title} ${toolFriendlySummary(tool)} ${toolSearchIntents(tool)}".lowercase()
    return terms.all(searchable::contains)
}

private fun toolSearchIntents(tool: MiniAppId): String = when (tool) {
    MiniAppId.LUNAR_CALENDAR -> "moon date am lich calendar can chi"
    MiniAppId.ZEN_HABIT -> "habit streak routine goal thoi quen"
    MiniAppId.ZEN_POMODORO -> "focus timer study work tap trung"
    MiniAppId.ZEN_BREATH -> "breathing calm relax stress meditation"
    MiniAppId.COMPASS -> "direction bearing north navigation la ban"
    MiniAppId.BUBBLE_LEVEL -> "level shelf angle tilt can bang ke"
    MiniAppId.DECIBEL_METER -> "sound noise microphone db loudness tieng on"
    MiniAppId.SMART_FLASHLIGHT -> "torch light sos emergency den pin"
    MiniAppId.UNIT_CONVERTER -> "convert length weight temperature doi don vi"
    MiniAppId.QR_STUDIO -> "scan barcode link url wifi contact create quet ma vach"
    MiniAppId.DOC_TO_PDF -> "document image photo merge reorder export tai lieu"
    MiniAppId.OCR_TEXT -> "photo image text receipt extract recognize scan chu"
    MiniAppId.COLOR_GRABBER -> "color picker camera hex rgb palette mau"
    MiniAppId.PHOTO_PRIVACY -> "remove exif gps metadata location xoa vi tri anh"
    MiniAppId.DEEP_CLEANER -> "storage large duplicate junk cleanup bo nho don dep"
    MiniAppId.SPEAKER_CLEANER -> "water eject tone speaker loa nuoc"
    MiniAppId.WIFI_ANALYZER -> "network signal channel router internet mang song"
    MiniAppId.PASSWORD_VAULT -> "password credential secret encrypted mat khau"
    MiniAppId.AUTHENTICATOR_VAULT -> "2fa otp totp authenticator code xac thuc"
    MiniAppId.FILE_STUDIO -> "zip archive hash checksum compress nen tep"
    MiniAppId.WALLPAPER_CHANGER -> "background image home lock screen hinh nen"
    MiniAppId.BILL_SPLITTER -> "split tax tip restaurant friends chia hoa don"
    MiniAppId.EXPENSE_TRACKER -> "money budget spending ledger chi tieu ngan sach"
    MiniAppId.DECISION_WHEEL -> "random picker choice roulette boc tham"
    MiniAppId.COMMUNITY_UNLOCK -> "support feedback roadmap github telegram cong dong"
    MiniAppId.SCREEN_RECORDER -> "record screen video capture quay man hinh"
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
    MiniAppId.QR_STUDIO -> "Scan QR codes and barcodes, create codes, and keep a private local history."
    MiniAppId.DOC_TO_PDF -> "Capture, arrange and export private PDFs."
    MiniAppId.OCR_TEXT -> "Scan, clean, edit and export private documents offline."
    MiniAppId.COLOR_GRABBER -> "Sample and copy colors from the camera."
    MiniAppId.PHOTO_PRIVACY -> "Create share-ready photos without GPS or EXIF metadata."
    MiniAppId.DEEP_CLEANER -> "Review reclaimable files before deleting."
    MiniAppId.SPEAKER_CLEANER -> "Play a controlled tone for residual water."
    MiniAppId.WIFI_ANALYZER -> "Inspect nearby signals and Wi-Fi channels."
    MiniAppId.PASSWORD_VAULT -> "Encrypted local credentials with device protection."
    MiniAppId.AUTHENTICATOR_VAULT -> "Offline 2FA codes protected by your device lock."
    MiniAppId.FILE_STUDIO -> "Hash, archive and share local files privately."
    MiniAppId.WALLPAPER_CHANGER -> "Local wallpaper preview and rotation."
    MiniAppId.BILL_SPLITTER -> "Split items, tax and tips for a group."
    MiniAppId.EXPENSE_TRACKER -> "Private offline expense ledger."
    MiniAppId.DECISION_WHEEL -> "A fair local picker for quick choices."
    MiniAppId.COMMUNITY_UNLOCK -> "Telegram, GitHub and the PureHub roadmap."
    MiniAppId.SCREEN_RECORDER -> "Record a local MP4 with Android's consent flow."
}
