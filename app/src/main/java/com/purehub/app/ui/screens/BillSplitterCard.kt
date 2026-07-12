package com.purehub.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.purehub.app.feature.billsplitter.BillLineItem
import com.purehub.app.feature.billsplitter.BillPreset
import com.purehub.app.feature.billsplitter.BillPresetItem
import com.purehub.app.feature.billsplitter.BillPresetRepository
import com.purehub.app.feature.billsplitter.BillSplitSummary
import com.purehub.app.feature.billsplitter.BillSplitterCalculator
import com.purehub.app.ui.LocalSnackbarHostState
import java.text.DecimalFormat
import kotlinx.coroutines.launch

@Composable
fun BillSplitterCard(
    innerPadding: PaddingValues = PaddingValues(0.dp),
) {
    val context = LocalContext.current
    val repository = remember { BillPresetRepository(context.applicationContext) }
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    var presetName by rememberSaveable { mutableStateOf("") }
    var selectedPresetName by rememberSaveable { mutableStateOf("") }
    var savedPresets by remember { mutableStateOf(emptyList<BillPreset>()) }

    var totalBill by rememberSaveable { mutableStateOf("0") }
    var taxAmount by rememberSaveable { mutableStateOf("0") }
    var tipAmount by rememberSaveable { mutableStateOf("0") }
    var peopleCount by rememberSaveable { mutableIntStateOf(3) }
    val items = remember {
        mutableStateListOf(
            EditableBillItem(1, "Pizza", "18.0", mutableStateListOf(0, 1)),
            EditableBillItem(2, "Drinks", "9.0", mutableStateListOf(1, 2)),
        )
    }

    LaunchedEffect(Unit) {
        repository.presets.collect { savedPresets = it }
    }

    val summary = BillSplitterCalculator.calculate(
        totalBill = totalBill.toDoubleOrNull() ?: 0.0,
        taxAmount = taxAmount.toDoubleOrNull() ?: 0.0,
        tipAmount = tipAmount.toDoubleOrNull() ?: 0.0,
        peopleCount = peopleCount,
        items = items.map {
            BillLineItem(
                id = it.id,
                name = it.name,
                amount = it.amount.toDoubleOrNull() ?: 0.0,
                assignedPeople = it.assignedPeople.toSet(),
            )
        },
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(innerPadding),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Bill Splitter",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "Mix shared charges with item assignments, manage reusable presets locally, and share the final split as plain text.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (savedPresets.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    savedPresets.forEach { preset ->
                        AssistChip(
                            onClick = {
                                selectedPresetName = preset.name
                                presetName = preset.name
                                applyPreset(
                                    preset = preset,
                                    onApplyHeader = { total, tax, tip, people ->
                                        totalBill = total
                                        taxAmount = tax
                                        tipAmount = tip
                                        peopleCount = people
                                    },
                                    items = items,
                                )
                            },
                            label = { Text(preset.name) },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MoneyField(
                    modifier = Modifier.weight(1f),
                    label = "Total bill",
                    value = totalBill,
                    onValueChange = { totalBill = it },
                )
                MoneyField(
                    modifier = Modifier.weight(1f),
                    label = "Tax",
                    value = taxAmount,
                    onValueChange = { taxAmount = it },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MoneyField(
                    modifier = Modifier.weight(1f),
                    label = "Tip",
                    value = tipAmount,
                    onValueChange = { tipAmount = it },
                )
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = peopleCount.toString(),
                    onValueChange = {
                        peopleCount = (it.toIntOrNull() ?: peopleCount).coerceIn(1, 12)
                        items.forEach { item ->
                            item.assignedPeople.removeAll { index -> index >= peopleCount }
                        }
                    },
                    label = { Text("People") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = presetName,
                onValueChange = { presetName = it },
                label = { Text("Preset name") },
                supportingText = {
                    Text(
                        if (savedPresets.isEmpty()) {
                            "No local presets saved yet."
                        } else if (selectedPresetName.isBlank()) {
                            "Tap a preset chip above to restore and manage it."
                        } else {
                            "Managing preset: $selectedPresetName"
                        },
                    )
                },
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        val finalName = presetName.ifBlank { "Preset ${savedPresets.size + 1}" }
                        scope.launch {
                            repository.savePreset(
                                buildPreset(
                                    name = finalName,
                                    totalBill = totalBill,
                                    taxAmount = taxAmount,
                                    tipAmount = tipAmount,
                                    peopleCount = peopleCount,
                                    items = items,
                                ),
                            )
                            selectedPresetName = finalName
                            presetName = finalName
                            snackbarHostState.showSnackbar("Preset \"$finalName\" saved locally.")
                        }
                    },
                ) {
                    Text("Save Preset")
                }
                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, buildShareSummary(summary))
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share split summary"))
                        scope.launch { snackbarHostState.showSnackbar("Split summary ready to share.") }
                    },
                ) {
                    Text("Share Summary")
                }
            }
            if (selectedPresetName.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            if (presetName.isNotBlank() && presetName != selectedPresetName) {
                                scope.launch {
                                    repository.renamePreset(selectedPresetName, presetName)
                                    selectedPresetName = presetName
                                    snackbarHostState.showSnackbar("Preset renamed to \"$presetName\".")
                                }
                            }
                        },
                    ) {
                        Text("Rename Preset")
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                repository.deletePreset(selectedPresetName)
                                snackbarHostState.showSnackbar("Preset \"$selectedPresetName\" deleted.")
                                selectedPresetName = ""
                                presetName = ""
                            }
                        },
                    ) {
                        Text("Delete Preset")
                    }
                }
            }

            items.forEach { item ->
                EditableItemCard(
                    item = item,
                    peopleCount = peopleCount,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        items += EditableBillItem(
                            id = (items.maxOfOrNull { it.id } ?: 0) + 1,
                            name = "Item ${items.size + 1}",
                            amount = "0",
                            assignedPeople = mutableStateListOf(),
                        )
                    },
                ) {
                    Text("Add Item")
                }
                Button(
                    onClick = {
                        if (items.isNotEmpty()) {
                            items.removeLast()
                        }
                    },
                ) {
                    Text("Remove Last")
                }
            }

            Text(
                text = "Per person",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            summary.people.forEach { person ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Person ${person.personIndex + 1}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text("Assigned: ${money(person.assignedTotal)}")
                        Text("Shared: ${money(person.sharedTotal)}")
                        Text(
                            text = "Total: ${money(person.grandTotal)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            Text(
                text = "Shared remainder: ${money(summary.remainingSharedSubtotal)}  |  Extras: ${money(summary.extrasTotal)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private class EditableBillItem(
    val id: Int,
    name: String,
    amount: String,
    assignedPeople: MutableList<Int>,
) {
    var name by mutableStateOf(name)
    var amount by mutableStateOf(amount)
    val assignedPeople = mutableStateListOf<Int>().apply { addAll(assignedPeople) }
}

@Composable
private fun EditableItemCard(
    item: EditableBillItem,
    peopleCount: Int,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = item.name,
                onValueChange = { item.name = it },
                label = { Text("Item name") },
                singleLine = true,
            )
            MoneyField(
                modifier = Modifier.fillMaxWidth(),
                label = "Amount",
                value = item.amount,
                onValueChange = { item.amount = it },
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                (0 until peopleCount).forEach { personIndex ->
                    val selected = personIndex in item.assignedPeople
                    AssistChip(
                        onClick = {
                            if (selected) item.assignedPeople.remove(personIndex)
                            else item.assignedPeople.add(personIndex)
                        },
                        label = { Text("P${personIndex + 1}") },
                    )
                }
            }
        }
    }
}

@Composable
private fun MoneyField(
    modifier: Modifier,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        modifier = modifier.widthIn(min = 120.dp),
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
    )
}

private fun applyPreset(
    preset: BillPreset,
    onApplyHeader: (String, String, String, Int) -> Unit,
    items: MutableList<EditableBillItem>,
) {
    onApplyHeader(
        preset.totalBill,
        preset.taxAmount,
        preset.tipAmount,
        preset.peopleCount,
    )
    items.clear()
    preset.items.forEachIndexed { index, item ->
        items += EditableBillItem(
            id = index + 1,
            name = item.name,
            amount = item.amount,
            assignedPeople = item.assignedPeople.toMutableList(),
        )
    }
}

private fun buildPreset(
    name: String,
    totalBill: String,
    taxAmount: String,
    tipAmount: String,
    peopleCount: Int,
    items: List<EditableBillItem>,
): BillPreset {
    return BillPreset(
        name = name,
        totalBill = totalBill,
        taxAmount = taxAmount,
        tipAmount = tipAmount,
        peopleCount = peopleCount,
        items = items.map { item ->
            BillPresetItem(
                name = item.name,
                amount = item.amount,
                assignedPeople = item.assignedPeople.toList(),
            )
        },
    )
}

private fun money(value: Double): String = DecimalFormat("#,##0.00").format(value)

private fun buildShareSummary(summary: BillSplitSummary): String {
    return buildString {
        appendLine("PureHub Bill Splitter")
        appendLine("Overall total: ${money(summary.overallTotal)}")
        summary.people.forEach { person ->
            appendLine("Person ${person.personIndex + 1}: ${money(person.grandTotal)}")
        }
        appendLine("Extras: ${money(summary.extrasTotal)}")
        append("Shared remainder: ${money(summary.remainingSharedSubtotal)}")
    }
}
