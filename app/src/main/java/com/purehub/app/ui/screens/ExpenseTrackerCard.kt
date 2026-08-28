package com.purehub.app.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.purehub.app.data.local.PureHubDatabaseProvider
import com.purehub.app.feature.expense.ExpenseInsights
import com.purehub.app.feature.expense.ExpenseTrackerRepository
import com.purehub.app.feature.expense.ExpenseTrackerViewModel
import com.purehub.app.feature.receipt.recognizeReceipt
import com.purehub.app.ui.LocalSnackbarHostState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@Composable
fun ExpenseTrackerCard() {
    val context = LocalContext.current
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val preferences = remember { context.getSharedPreferences("purehub_money", 0) }
    val database = PureHubDatabaseProvider.get(context)
    val viewModel: ExpenseTrackerViewModel = viewModel(
        factory = ExpenseTrackerViewModel.factory(ExpenseTrackerRepository(database.expenseDao())),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    var mode by rememberSaveable { mutableStateOf(SuiteMode.QUICK) }
    var query by rememberSaveable { mutableStateOf("") }
    var budgetText by rememberSaveable { mutableStateOf(preferences.getString("monthly_budget", "").orEmpty()) }
    val insights = remember(expenses) { ExpenseInsights.calculate(expenses) }
    val filtered = remember(expenses, query) {
        expenses.filter { query.isBlank() || it.entry.title.contains(query, true) || it.entry.category.contains(query, true) }
    }
    val budgetMinor = ((budgetText.toDoubleOrNull() ?: 0.0) * 100).toLong()
    val budgetProgress = if (budgetMinor > 0L) (insights.currentMonthMinor.toFloat() / budgetMinor).coerceIn(0f, 1f) else 0f
    val receiptPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) recognizeReceipt(context, uri) { result ->
            result.onSuccess {
                viewModel.applyReceipt(it)
                scope.launch { snackbar.showSnackbar("Receipt recognized locally. Review the fields before saving.") }
            }.onFailure { scope.launch { snackbar.showSnackbar("Receipt OCR could not read this image.") } }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            FlagshipSuiteHeader(
                eyebrow = "Private Finance flagship",
                title = "Money Studio",
                description = "Receipt capture, monthly budget, category insights and a portable local ledger without accounts or ads.",
            )
            SuiteModeSwitch(mode, { mode = it }, "Budget intelligence, category totals, search, and CSV export.")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MoneyMetric("This month", formatMoney(insights.currentMonthMinor), Modifier.weight(1f))
                MoneyMetric("All time", formatMoney(insights.allTimeMinor), Modifier.weight(1f))
            }

            if (mode == SuiteMode.PRO) {
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = {
                        budgetText = it.filter { char -> char.isDigit() || char == '.' }.take(12)
                        preferences.edit().putString("monthly_budget", budgetText).apply()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { LocalizedText("Monthly budget") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    supportingText = { LocalizedText(if (budgetMinor > 0) "${(budgetProgress * 100).toInt()}% used" else "Optional and stored only on this device") },
                )
                if (budgetMinor > 0) LinearProgressIndicator(progress = { budgetProgress }, modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    insights.categoryTotals.take(6).forEach {
                        AssistChip(onClick = { query = it.category }, label = { LocalizedText("${it.category} ${formatMoney(it.amountMinor)}") })
                    }
                }
            }

            Button(onClick = { receiptPicker.launch("image/*") }) { LocalizedText("Scan receipt") }
            OutlinedTextField(uiState.draftTitle, viewModel::updateDraftTitle, Modifier.fillMaxWidth(), label = { LocalizedText("Title") }, singleLine = true)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    uiState.draftAmount,
                    viewModel::updateDraftAmount,
                    Modifier.weight(1f),
                    label = { LocalizedText("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                OutlinedTextField(uiState.draftCategory, viewModel::updateDraftCategory, Modifier.weight(1f), label = { LocalizedText("Category") }, singleLine = true)
            }
            OutlinedTextField(uiState.draftNote, viewModel::updateDraftNote, Modifier.fillMaxWidth(), label = { LocalizedText("Note") }, minLines = 2)
            Button(
                onClick = viewModel::saveExpense,
                enabled = uiState.draftTitle.isNotBlank() && uiState.draftAmount.isNotBlank(),
            ) { LocalizedText("Save expense") }
            PrivacyReceipt("Ledger protected locally", "Receipt recognition and insights run on-device. No bank connection or analytics SDK sees these entries.")

            if (mode == SuiteMode.PRO && expenses.isNotEmpty()) {
                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { LocalizedText("Search title or category") }, singleLine = true)
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_SUBJECT, "PureHub expense export")
                            putExtra(Intent.EXTRA_TEXT, ExpenseInsights.toCsv(expenses))
                        }, "Export expense CSV"))
                    },
                ) { LocalizedText("Export CSV") }
            }

            if (filtered.isEmpty()) {
                LocalizedText(if (expenses.isEmpty()) "No expenses logged yet." else "No matching expense.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    filtered.forEach { summary ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                LocalizedText(summary.entry.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                LocalizedText("${summary.amountDisplay} · ${summary.entry.category}")
                                LocalizedText(
                                    Instant.ofEpochMilli(summary.entry.happenedAtEpochMillis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (summary.entry.note.isNotBlank()) LocalizedText(summary.entry.note, style = MaterialTheme.typography.bodySmall)
                                OutlinedButton(onClick = { viewModel.deleteExpense(summary.entry) }) { LocalizedText("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoneyMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            LocalizedText(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LocalizedText(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatMoney(amountMinor: Long): String = "%.2f".format(amountMinor / 100.0)
