package com.purehub.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.purehub.app.data.local.PureHubDatabaseProvider
import com.purehub.app.feature.zenhabit.ZenHabitRepository
import com.purehub.app.feature.zenhabit.ZenHabitViewModel

@Composable
fun ZenHabitCard(
    compact: Boolean = false,
) {
    val context = LocalContext.current
    val database = PureHubDatabaseProvider.get(context)
    val viewModel: ZenHabitViewModel = viewModel(
        factory = ZenHabitViewModel.factory(
            repository = ZenHabitRepository(
                habitDao = database.habitDao(),
                checkInDao = database.habitCheckInDao(),
            ),
        ),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val habits by viewModel.habitSummaries.collectAsStateWithLifecycle()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (!compact) {
                Text(
                    text = "Zen Habit",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "Track quiet daily consistency with local Room storage only. No account, no sync, no cloud dependency.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = uiState.draftHabitName,
                    onValueChange = viewModel::updateDraftHabitName,
                    modifier = Modifier.weight(1f),
                    label = { Text("New habit") },
                    singleLine = true,
                )
                Button(
                    onClick = viewModel::saveHabit,
                    enabled = uiState.draftHabitName.isNotBlank() && !uiState.saving,
                ) {
                    Text("Add")
                }
            }

            if (habits.isEmpty()) {
                Text(
                    text = "No habits yet. Start with one simple streak you can keep daily.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    habits.forEach { summary ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    text = summary.habit.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "Current streak ${summary.currentStreak} day(s) • ${summary.totalCheckIns} total check-ins",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                FilterChip(
                                    selected = summary.completedToday,
                                    onClick = {
                                        viewModel.toggleToday(
                                            habitId = summary.habit.id,
                                            isCompletedToday = summary.completedToday,
                                        )
                                    },
                                    label = {
                                        Text(if (summary.completedToday) "Completed today" else "Mark today done")
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
