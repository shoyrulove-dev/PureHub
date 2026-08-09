package com.purehub.app.feature.zenhabit

import com.purehub.app.data.local.dao.HabitCheckInDao
import com.purehub.app.data.local.dao.HabitDao
import com.purehub.app.data.local.entity.HabitCheckInEntity
import com.purehub.app.data.local.entity.HabitEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class HabitSummary(
    val habit: HabitEntity,
    val currentStreak: Int,
    val bestStreak: Int,
    val completedToday: Boolean,
    val totalCheckIns: Int,
    val weeklyCheckIns: Int,
    val completionDates: Set<LocalDate>,
)

class ZenHabitRepository(
    private val habitDao: HabitDao,
    private val checkInDao: HabitCheckInDao,
) {
    fun observeHabitSummaries(today: LocalDate = LocalDate.now()): Flow<List<HabitSummary>> {
        return combine(
            habitDao.observeHabits(),
            checkInDao.observeAllCheckIns(),
        ) { habits, checkIns ->
            habits.map { habit ->
                val dates = checkIns
                    .asSequence()
                    .filter { it.habitId == habit.id }
                    .map { LocalDate.parse(it.completedOn) }
                    .toSet()

                HabitSummary(
                    habit = habit,
                    currentStreak = HabitStreakCalculator.calculateCurrentStreak(dates, today),
                    bestStreak = calculateBestStreak(dates),
                    completedToday = dates.contains(today),
                    totalCheckIns = dates.size,
                    weeklyCheckIns = dates.count { !it.isBefore(today.minusDays(6)) && !it.isAfter(today) },
                    completionDates = dates,
                )
            }
        }
    }

    suspend fun addHabit(
        name: String,
        description: String = "",
        colorHex: String = "#10B981",
        targetDaysPerWeek: Int = 7,
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return

        habitDao.upsertHabit(
            HabitEntity(
                name = trimmedName,
                description = description.trim(),
                colorHex = colorHex,
                targetDaysPerWeek = targetDaysPerWeek.coerceIn(1, 7),
                createdAtEpochMillis = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun toggleToday(habitId: Long, isCompletedToday: Boolean, today: LocalDate = LocalDate.now()) {
        val dateKey = today.toString()
        if (isCompletedToday) {
            checkInDao.deleteCheckIn(habitId, dateKey)
            return
        }

        checkInDao.upsertCheckIn(
            HabitCheckInEntity(
                habitId = habitId,
                completedOn = dateKey,
                createdAtEpochMillis = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun toggleDay(habitId: Long, day: LocalDate, completed: Boolean) {
        toggleToday(habitId = habitId, isCompletedToday = completed, today = day)
    }

    suspend fun setArchived(habit: HabitEntity, archived: Boolean) {
        habitDao.updateHabit(habit.copy(isArchived = archived))
    }

    suspend fun deleteHabit(habit: HabitEntity) {
        habitDao.deleteHabit(habit)
    }

    private fun calculateBestStreak(dates: Set<LocalDate>): Int {
        if (dates.isEmpty()) return 0
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        dates.sorted().forEach { date ->
            run = if (previous?.plusDays(1) == date) run + 1 else 1
            best = maxOf(best, run)
            previous = date
        }
        return best
    }
}
