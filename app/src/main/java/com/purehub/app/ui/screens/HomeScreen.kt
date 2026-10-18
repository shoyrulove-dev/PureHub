package com.purehub.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.purehub.app.ui.LocalizedText
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material.icons.rounded.Search
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.purehub.app.feature.catalog.MiniAppId
import com.purehub.app.feature.lunar.LunarCalendarConverter
import com.purehub.app.ui.LocalAppLanguage
import com.purehub.app.ui.locale
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    innerPadding: PaddingValues,
    onOpenMiniApp: (MiniAppId) -> Unit,
) {
    val today = LocalDate.now()
    val lunarInfo = LunarCalendarConverter.describeDate(today)
    val language = LocalAppLanguage.current
    val largeText = LocalDensity.current.fontScale >= 1.5f
    val formatter = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", language.locale())
    val visibleTools = rememberVisibleTools()
    val favoriteTools = rememberFavoriteTools()
    val recentTools = rememberRecentTools()
    var query by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(Brush.verticalGradient(listOf(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f),
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            ))),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { LocalizedText("Search") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                shape = MaterialTheme.shapes.large,
            )
        }
        item {
            LocalizedText(if (query.isNotBlank()) "Search" else if (favoriteTools.isNotEmpty()) "Favorites" else if (recentTools.isNotEmpty()) "Recent" else "Quick access",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            HomeQuickShortcuts(
                visibleTools = visibleTools.filter { toolMatchesSearch(it, query) }.toSet(),
                favoriteTools = favoriteTools, recentTools = recentTools, onOpenTool = onOpenMiniApp,
                limit = if (query.isBlank()) 4 else Int.MAX_VALUE,
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onOpenMiniApp(MiniAppId.LUNAR_CALENDAR) },
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(Color(0xFF4C1D95), Color(0xFF7C3AED), Color(0xFF0E7490))))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(Icons.Rounded.CalendarMonth, null, tint = Color(0xFFE9D5FF))
                    LocalizedText(
                        text = "TODAY",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFE9D5FF),
                    )
                    LocalizedText(
                        text = today.format(formatter),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, color = Color.White,
                    )
                    LocalizedText(
                        text = "Lunar ${lunarInfo.lunarDate.displayText}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFD8B4FE),
                    )
                    lunarInfo.holidayLabel?.let { LocalizedText(it, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE9D5FF)) }
                }
            }
        }
        item {
            LocalizedText("Recommended locally", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            val recommended = listOf(MiniAppId.WIFI_ANALYZER, MiniAppId.OCR_TEXT, MiniAppId.SMART_FLASHLIGHT)
                .filter { it in visibleTools }
            if (largeText) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    recommended.forEach { tool ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
                            onClick = { onOpenMiniApp(tool) },
                            shape = MaterialTheme.shapes.large,
                            color = tool.tab.uiAccentContainer(),
                        ) {
                            Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(tool.icon, null, tint = tool.tab.uiAccent())
                                LocalizedText(compactToolTitle(tool, language), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    recommended.forEach { tool ->
                        Surface(
                            modifier = Modifier.weight(1f).heightIn(min = 92.dp),
                            onClick = { onOpenMiniApp(tool) },
                            shape = MaterialTheme.shapes.large,
                            color = tool.tab.uiAccentContainer(),
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(tool.icon, null, tint = tool.tab.uiAccent())
                                LocalizedText(compactToolTitle(tool, language), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 2)
                            }
                        }
                    }
                }
            }
        }
    }
}
