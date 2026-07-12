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
    val completedToday: Boolean,
    val totalCheckIns: Int,
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
                    completedToday = dates.contains(today),
                    totalCheckIns = dates.size,
                )
            }
        }
    }

    suspend fun addHabit(name: String) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return

        habitDao.upsertHabit(
            HabitEntity(
                name = trimmedName,
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
}
