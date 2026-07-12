package com.purehub.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.purehub.app.feature.converter.ConverterCategory
import com.purehub.app.feature.converter.UnitConverterEngine

@Composable
fun UnitConverterCard() {
    var category by remember { mutableStateOf(ConverterCategory.LENGTH) }
    var fromIndex by remember { mutableIntStateOf(0) }
    var toIndex by remember { mutableIntStateOf(1) }
    var inputValue by remember { mutableStateOf("1") }
    val units = UnitConverterEngine.categories.getValue(category)
    val result = UnitConverterEngine.convert(
        value = inputValue,
        category = category,
        fromIndex = fromIndex.coerceIn(units.indices),
        toIndex = toIndex.coerceIn(units.indices),
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Unit Converter",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "Static zero-latency conversion math keeps daily measurements instant and fully offline.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ConverterCategory.entries.forEach { entry ->
                    AssistChip(
                        onClick = {
                            category = entry
                            fromIndex = 0
                            toIndex = 1.coerceAtMost(UnitConverterEngine.categories.getValue(entry).lastIndex)
                        },
                        label = { Text(entry.title) },
                    )
                }
            }
            OutlinedTextField(
                value = inputValue,
                onValueChange = { inputValue = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Value") },
                singleLine = true,
            )
            Text(
                text = "From",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                units.forEachIndexed { index, unit ->
                    AssistChip(
                        onClick = { fromIndex = index },
                        label = { Text(if (fromIndex == index) "${unit.label} • From" else unit.label) },
                    )
                }
            }
            Text(
                text = "To",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                units.forEachIndexed { index, unit ->
                    AssistChip(
                        onClick = { toIndex = index },
                        label = { Text(if (toIndex == index) "${unit.label} • To" else unit.label) },
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (result.isBlank()) "Enter a number to convert." else "$inputValue ${units[fromIndex].label} = $result ${units[toIndex].label}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
