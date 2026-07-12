package com.purehub.app.ui.screens

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.purehub.app.data.local.PureHubDatabaseProvider
import com.purehub.app.feature.expense.ExpenseTrackerRepository
import com.purehub.app.feature.expense.ExpenseTrackerViewModel

@Composable
fun ExpenseTrackerCard() {
    val context = LocalContext.current
    val database = PureHubDatabaseProvider.get(context)
    val viewModel: ExpenseTrackerViewModel = viewModel(
        factory = ExpenseTrackerViewModel.factory(
            repository = ExpenseTrackerRepository(database.expenseDao()),
        ),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Expense Tracker",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "A simple local ledger stored in Room. Categories, amounts, and notes stay only on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = uiState.draftTitle,
                onValueChange = viewModel::updateDraftTitle,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title") },
                singleLine = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = uiState.draftAmount,
                    onValueChange = viewModel::updateDraftAmount,
                    modifier = Modifier.weight(1f),
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = uiState.draftCategory,
                    onValueChange = viewModel::updateDraftCategory,
                    modifier = Modifier.weight(1f),
                    label = { Text("Category") },
                    singleLine = true,
                )
            }
            OutlinedTextField(
                value = uiState.draftNote,
                onValueChange = viewModel::updateDraftNote,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Note") },
                minLines = 2,
            )
            Button(
                onClick = viewModel::saveExpense,
                enabled = uiState.draftTitle.isNotBlank() && uiState.draftAmount.isNotBlank(),
            ) {
                Text("Save Expense")
            }
            if (expenses.isEmpty()) {
                Text(
                    text = "No expenses logged yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    expenses.forEach { summary ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = summary.entry.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "${summary.amountDisplay} • ${summary.entry.category}",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                if (summary.entry.note.isNotBlank()) {
                                    Text(
                                        text = summary.entry.note,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Button(onClick = { viewModel.deleteExpense(summary.entry) }) {
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
