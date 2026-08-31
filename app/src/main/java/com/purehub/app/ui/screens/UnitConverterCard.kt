package com.purehub.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.purehub.app.feature.converter.ConverterCategory
import com.purehub.app.feature.converter.ConverterUnit
import com.purehub.app.feature.converter.UnitConverterEngine
import com.purehub.app.ui.LocalizedText

@Composable
fun UnitConverterCard() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("purehub.converter.v2", 0) }
    var category by remember { mutableStateOf(ConverterCategory.LENGTH) }
    var fromIndex by remember { mutableIntStateOf(0) }
    var toIndex by remember { mutableIntStateOf(1) }
    var inputValue by remember { mutableStateOf("1") }
    var unitSearch by remember { mutableStateOf("") }
    var favorites by remember { mutableStateOf(preferences.getStringSet("favorites", emptySet()).orEmpty().toSet()) }
    var history by remember {
        mutableStateOf(preferences.getString("history", "").orEmpty().split('\u001e').filter(String::isNotBlank))
    }
    val units = UnitConverterEngine.categories.getValue(category)
    val safeFrom = fromIndex.coerceIn(units.indices)
    val safeTo = toIndex.coerceIn(units.indices)
    val result = UnitConverterEngine.convert(inputValue, category, safeFrom, safeTo)
    val pairKey = "${category.name}:$safeFrom:$safeTo"
    val resultLine = if (result.isBlank()) "" else "$inputValue ${units[safeFrom].label} = $result ${units[safeTo].label}"
    val visibleUnits = units.withIndex().filter {
        unitSearch.isBlank() || it.value.label.contains(unitSearch, ignoreCase = true)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            FlagshipSuiteHeader(
                eyebrow = "Everyday Tools flagship",
                title = "Unit Converter",
                description = "Fast offline conversion with practical everyday, travel, cooking and engineering units.",
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ConverterCategory.entries.forEach { entry ->
                    FilterChip(
                        selected = category == entry,
                        onClick = {
                            category = entry
                            fromIndex = 0
                            toIndex = 1.coerceAtMost(UnitConverterEngine.categories.getValue(entry).lastIndex)
                            unitSearch = ""
                        },
                        label = { LocalizedText(entry.title) },
                    )
                }
            }
            OutlinedTextField(
                value = inputValue,
                onValueChange = { inputValue = it.take(40) },
                modifier = Modifier.fillMaxWidth(),
                label = { LocalizedText("Value") },
                singleLine = true,
            )
            OutlinedTextField(
                value = unitSearch,
                onValueChange = { unitSearch = it.take(40) },
                modifier = Modifier.fillMaxWidth(),
                label = { LocalizedText("Search units") },
                singleLine = true,
            )
            LocalizedText("From", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            UnitChoices(visibleUnits, safeFrom, "From") { fromIndex = it }
            LocalizedText("To", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            UnitChoices(visibleUnits, safeTo, "To") { toIndex = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val previous = fromIndex
                    fromIndex = toIndex
                    toIndex = previous
                }) { LocalizedText("Swap") }
                OutlinedButton(onClick = {
                    favorites = if (pairKey in favorites) favorites - pairKey else favorites + pairKey
                    preferences.edit().putStringSet("favorites", favorites).apply()
                }) { LocalizedText(if (pairKey in favorites) "Unfavorite" else "Favorite") }
            }
            LocalizedText(
                text = resultLine.ifBlank { "Enter a number to convert." },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = resultLine.isNotBlank(),
                    onClick = {
                        history = (listOf(resultLine) + history.filterNot { it == resultLine }).take(8)
                        preferences.edit().putString("history", history.joinToString("\u001e")).apply()
                    },
                ) { LocalizedText("Save to history") }
                OutlinedButton(
                    enabled = resultLine.isNotBlank(),
                    onClick = {
                        context.getSystemService(ClipboardManager::class.java)
                            .setPrimaryClip(ClipData.newPlainText("PureHub conversion", resultLine))
                    },
                ) { LocalizedText("Copy result") }
            }
            FavoriteConversions(favorites) { savedCategory, savedFrom, savedTo ->
                category = savedCategory
                fromIndex = savedFrom
                toIndex = savedTo
                unitSearch = ""
            }
            if (history.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    LocalizedText("Recent conversions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    OutlinedButton(onClick = {
                        history = emptyList()
                        preferences.edit().remove("history").apply()
                    }) { LocalizedText("Clear") }
                }
                history.take(5).forEach { LocalizedText(it, style = MaterialTheme.typography.bodySmall) }
            }
            PrivacyReceipt(
                action = "Conversion history stays local",
                detail = "Favorites and recent results remain on this phone and can be cleared at any time.",
            )
        }
    }
}

@Composable
private fun UnitChoices(
    units: List<IndexedValue<ConverterUnit>>,
    selectedIndex: Int,
    role: String,
    onSelect: (Int) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        units.forEach { indexed ->
            FilterChip(
                selected = selectedIndex == indexed.index,
                onClick = { onSelect(indexed.index) },
                label = { LocalizedText(if (selectedIndex == indexed.index) "${indexed.value.label} • $role" else indexed.value.label) },
            )
        }
    }
}

@Composable
private fun FavoriteConversions(
    favorites: Set<String>,
    onSelect: (ConverterCategory, Int, Int) -> Unit,
) {
    if (favorites.isEmpty()) return
    LocalizedText("Favorite conversions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        favorites.sorted().forEach { saved ->
            val parts = saved.split(':')
            val category = parts.getOrNull(0)?.let { runCatching { ConverterCategory.valueOf(it) }.getOrNull() }
            val from = parts.getOrNull(1)?.toIntOrNull()
            val to = parts.getOrNull(2)?.toIntOrNull()
            val units = category?.let(UnitConverterEngine.categories::get)
            if (category != null && from != null && to != null && units != null && from in units.indices && to in units.indices) {
                AssistChip(
                    onClick = { onSelect(category, from, to) },
                    label = { LocalizedText("${units[from].label} → ${units[to].label}") },
                )
            }
        }
    }
}
