package com.purehub.app.feature.zenhabit

import java.time.LocalDate

internal object HabitStreakCalculator {
    fun calculateCurrentStreak(
        completedDates: Set<LocalDate>,
        today: LocalDate = LocalDate.now(),
    ): Int {
        var streak = 0
        var cursor = today

        while (completedDates.contains(cursor)) {
            streak += 1
            cursor = cursor.minusDays(1)
        }

        return streak
    }
}
