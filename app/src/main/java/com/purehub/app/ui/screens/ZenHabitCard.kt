package com.purehub.app.ui.screens

import android.graphics.Color.parseColor
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import com.purehub.app.ui.LocalizedText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.purehub.app.data.local.PureHubDatabaseProvider
import com.purehub.app.data.local.entity.HabitEntity
import com.purehub.app.feature.zenhabit.HabitSummary
import com.purehub.app.feature.zenhabit.ZenHabitRepository
import com.purehub.app.feature.zenhabit.ZenHabitSection
import com.purehub.app.feature.zenhabit.ZenHabitViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

private val habitColors = listOf("#10B981", "#0EA5E9", "#8B5CF6", "#F59E0B", "#F43F5E")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZenHabitCard(
    compact: Boolean = false,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val database = PureHubDatabaseProvider.get(context)
    val viewModel: ZenHabitViewModel = viewModel(
        factory = ZenHabitViewModel.factory(
            repository = ZenHabitRepository(database.habitDao(), database.habitCheckInDao()),
        ),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val summaries by viewModel.habitSummaries.collectAsStateWithLifecycle()
    val active = summaries.filterNot { it.habit.isArchived }
    val archived = summaries.filter { it.habit.isArchived }
    val today = LocalDate.now()
    val week = remember(today) { (6 downTo 0).map { offset -> today.minusDays(offset.toLong()) } }
    val completedToday = active.count { it.completedToday }
    val weeklyDone = active.sumOf { it.weeklyCheckIns }
    val weeklyGoal = active.sumOf { it.habit.targetDaysPerWeek.coerceIn(1, 7) }
    val weeklyRate = if (weeklyGoal == 0) 0 else ((weeklyDone * 100f) / weeklyGoal).toInt().coerceIn(0, 100)
    val strongestStreak = active.maxOfOrNull { it.currentStreak } ?: 0
    var deleteCandidate by remember { mutableStateOf<HabitEntity?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        HabitHero(
            completedToday = completedToday,
            total = active.size,
            strongestStreak = strongestStreak,
            weeklyRate = weeklyRate,
        )

        if (!compact) {
            PrimaryTabRow(selectedTabIndex = uiState.section.ordinal) {
                ZenHabitSection.entries.forEach { section ->
                    Tab(
                        selected = uiState.section == section,
                        onClick = { viewModel.selectSection(section) },
                        text = { LocalizedText(section.name.lowercase().replaceFirstChar(Char::uppercase), fontWeight = FontWeight.Bold) },
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (if (compact) ZenHabitSection.TODAY else uiState.section) {
                ZenHabitSection.TODAY -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            LocalizedText("TODAY", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                            LocalizedText("Small actions count", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        }
                        IconButton(
                            onClick = viewModel::toggleComposer,
                            modifier = Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Add habit", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }

                    if (uiState.composerVisible) {
                        HabitComposer(viewModel = viewModel)
                    }

                    if (active.isEmpty()) {
                        EmptyHabitState(onAdd = viewModel::toggleComposer)
                    } else {
                        active.forEach { summary ->
                            HabitTodayCard(
                                summary = summary,
                                week = week,
                                onToggle = { day, completed -> viewModel.toggleDay(summary.habit.id, day, completed) },
                            )
                        }
                    }
                }

                ZenHabitSection.INSIGHTS -> {
                    SectionTitle("WEEKLY INSIGHTS", "Progress, not perfection", Icons.Rounded.Insights)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InsightCard("Check-ins", weeklyDone.toString(), "last 7 days", Modifier.weight(1f))
                        InsightCard("Goal", weeklyGoal.toString(), "planned", Modifier.weight(1f))
                        InsightCard("Complete", "$weeklyRate%", "this week", Modifier.weight(1f))
                    }
                    active.forEach { summary -> HabitInsightRow(summary) }
                }

                ZenHabitSection.MANAGE -> {
                    SectionTitle("PRIVATE DATA", "Manage your habits", Icons.Rounded.CalendarMonth)
                    OutlinedButton(onClick = { shareHabitBackup(context, summaries) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Share, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        LocalizedText("Export private backup")
                    }
                    (active + archived).forEach { summary ->
                        ManageHabitRow(
                            summary = summary,
                            onArchive = { viewModel.setArchived(summary.habit, !summary.habit.isArchived) },
                            onDelete = { deleteCandidate = summary.habit },
                        )
                    }
                    if (active.isEmpty() && archived.isEmpty()) EmptyHabitState(onAdd = { viewModel.selectSection(ZenHabitSection.TODAY); viewModel.toggleComposer() })
                }
            }
        }
    }

    deleteCandidate?.let { habit ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { LocalizedText("Delete ${habit.name}?") },
            text = { LocalizedText("Its private check-in history will also be removed from this device.") },
            confirmButton = { Button(onClick = { viewModel.deleteHabit(habit); deleteCandidate = null }) { LocalizedText("Delete") } },
            dismissButton = { OutlinedButton(onClick = { deleteCandidate = null }) { LocalizedText("Cancel") } },
        )
    }
}

@Composable
private fun HabitHero(completedToday: Int, total: Int, strongestStreak: Int, weeklyRate: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(Color(0xFF064E3B), Color(0xFF0F766E), Color(0xFF075985))))
            .padding(22.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Icon(Icons.Rounded.Spa, contentDescription = null, tint = Color(0xFFA7F3D0), modifier = Modifier.size(18.dp))
                        LocalizedText("PRIVATE DAILY RHYTHM", color = Color(0xFFA7F3D0), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                    }
                    LocalizedText("Zen Habit", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    LocalizedText("Build consistency without accounts, feeds, ads or guilt.", color = Color.White.copy(alpha = .78f), style = MaterialTheme.typography.bodyMedium)
                }
                Box(modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = .1f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Spa, contentDescription = null, tint = Color(0xFFA7F3D0))
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroStat("Today", "$completedToday/$total", Icons.Rounded.Check, Modifier.weight(1f))
                HeroStat("Best active", "${strongestStreak}d", Icons.Rounded.LocalFireDepartment, Modifier.weight(1f))
                HeroStat("This week", "$weeklyRate%", Icons.Rounded.Check, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Column(modifier = modifier.background(Color.White.copy(alpha = .1f), RoundedCornerShape(16.dp)).padding(11.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = Color(0xFFD1FAE5), modifier = Modifier.size(14.dp))
            LocalizedText(label.uppercase(), color = Color.White.copy(alpha = .68f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
        LocalizedText(value, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun HabitComposer(viewModel: ZenHabitViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .35f)), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(value = uiState.draftHabitName, onValueChange = viewModel::updateDraftHabitName, modifier = Modifier.fillMaxWidth(), label = { LocalizedText("Habit name") }, placeholder = { LocalizedText("Read for 10 minutes") }, singleLine = true)
            OutlinedTextField(value = uiState.draftDescription, onValueChange = viewModel::updateDraftDescription, modifier = Modifier.fillMaxWidth(), label = { LocalizedText("Why it matters (optional)") }, singleLine = true)
            LocalizedText("Color", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                habitColors.forEach { hex ->
                    val color = habitColor(hex)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(color)
                            .then(if (uiState.draftColorHex == hex) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(12.dp)) else Modifier)
                            .clickable { viewModel.updateDraftColor(hex) },
                    )
                }
            }
            LocalizedText("Weekly target", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..7).forEach { day -> FilterChip(selected = uiState.draftTargetDays == day, onClick = { viewModel.updateDraftTarget(day) }, label = { LocalizedText(day.toString()) }) }
            }
            Button(onClick = viewModel::saveHabit, enabled = uiState.draftHabitName.isNotBlank() && !uiState.saving, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                LocalizedText("Add habit")
            }
        }
    }
}

@Composable
private fun HabitTodayCard(summary: HabitSummary, week: List<LocalDate>, onToggle: (LocalDate, Boolean) -> Unit) {
    val color = habitColor(summary.habit.colorHex)
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(50.dp).background(if (summary.completedToday) color else Color.Transparent, RoundedCornerShape(16.dp)).border(2.dp, color, RoundedCornerShape(16.dp)).clickable { onToggle(LocalDate.now(), summary.completedToday) },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Check, contentDescription = if (summary.completedToday) "Undo today" else "Complete today", tint = if (summary.completedToday) Color.White else color.copy(alpha = .4f)) }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    LocalizedText(summary.habit.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    LocalizedText(summary.habit.description.ifBlank { "${summary.habit.targetDaysPerWeek} days each week" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.LocalFireDepartment, null, tint = Color(0xFFD97706), modifier = Modifier.size(17.dp)); LocalizedText(summary.currentStreak.toString(), fontWeight = FontWeight.Black, color = Color(0xFFD97706)) }
                    LocalizedText("DAY STREAK", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                week.forEach { day ->
                    val done = summary.completionDates.contains(day)
                    Column(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(11.dp)).background(if (done) color else MaterialTheme.colorScheme.surfaceContainerHigh).clickable { onToggle(day, done) }.padding(vertical = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        LocalizedText(day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), style = MaterialTheme.typography.labelSmall, color = if (done) Color.White.copy(alpha = .75f) else MaterialTheme.colorScheme.onSurfaceVariant)
                        if (done) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(15.dp)) else LocalizedText(day.dayOfMonth.toString(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitInsightRow(summary: HabitSummary) {
    val target = summary.habit.targetDaysPerWeek.coerceIn(1, 7)
    val progress = (summary.weeklyCheckIns / target.toFloat()).coerceIn(0f, 1f)
    Column(modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(17.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(habitColor(summary.habit.colorHex), CircleShape))
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) { LocalizedText(summary.habit.name, fontWeight = FontWeight.Bold); LocalizedText("Best streak ${summary.bestStreak} ${if (summary.bestStreak == 1) "day" else "days"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            LocalizedText("${summary.weeklyCheckIns}/$target", color = habitColor(summary.habit.colorHex), fontWeight = FontWeight.Black)
        }
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().clip(CircleShape), color = habitColor(summary.habit.colorHex))
    }
}

@Composable
private fun ManageHabitRow(summary: HabitSummary, onArchive: () -> Unit, onDelete: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(17.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(11.dp).background(habitColor(summary.habit.colorHex), CircleShape))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) { LocalizedText(summary.habit.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis); LocalizedText(if (summary.habit.isArchived) "Archived" else "${summary.habit.targetDaysPerWeek} days per week", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        IconButton(onClick = onArchive) { Icon(if (summary.habit.isArchived) Icons.Rounded.Restore else Icons.Rounded.Archive, contentDescription = if (summary.habit.isArchived) "Restore" else "Archive") }
        IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun InsightCard(label: String, value: String, detail: String, modifier: Modifier) {
    Column(modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(16.dp)).padding(11.dp)) {
        LocalizedText(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        LocalizedText(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        LocalizedText(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun SectionTitle(eyebrow: String, title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(42.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) }
        Column { LocalizedText(eyebrow, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black); LocalizedText(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) }
    }
}

@Composable
private fun EmptyHabitState(onAdd: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .35f), RoundedCornerShape(22.dp)).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Rounded.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
        LocalizedText("Start with one gentle habit", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
        LocalizedText("Pick something small enough to repeat.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onAdd) { LocalizedText("Create a habit") }
    }
}

private fun habitColor(hex: String): Color = runCatching { Color(parseColor(hex)) }.getOrDefault(Color(0xFF10B981))

private fun shareHabitBackup(context: Context, summaries: List<HabitSummary>) {
    val habits = JSONArray()
    summaries.forEach { summary ->
        habits.put(
            JSONObject()
                .put("name", summary.habit.name)
                .put("description", summary.habit.description)
                .put("color", summary.habit.colorHex)
                .put("targetDaysPerWeek", summary.habit.targetDaysPerWeek)
                .put("archived", summary.habit.isArchived)
                .put("checkIns", JSONArray(summary.completionDates.sorted().map(LocalDate::toString))),
        )
    }
    val payload = JSONObject()
        .put("format", "purehub-zen-habit-v1")
        .put("exportedOn", LocalDate.now().toString())
        .put("habits", habits)
        .toString(2)
    context.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_SUBJECT, "PureHub Zen Habit backup")
                putExtra(Intent.EXTRA_TEXT, payload)
            },
            "Export Zen Habit backup",
        ),
    )
}
