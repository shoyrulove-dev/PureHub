package com.purehub.app.feature.zenhabit

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class HabitStreakCalculatorTest {
    @Test
    fun countsConsecutiveDaysBackFromToday() {
        val today = LocalDate.of(2026, 6, 15)
        val dates = setOf(
            today,
            today.minusDays(1),
            today.minusDays(2),
            today.minusDays(4),
        )

        val streak = HabitStreakCalculator.calculateCurrentStreak(
            completedDates = dates,
            today = today,
        )

        assertEquals(3, streak)
    }

    @Test
    fun returnsZeroWhenTodayIsMissing() {
        val today = LocalDate.of(2026, 6, 15)
        val dates = setOf(today.minusDays(1))

        val streak = HabitStreakCalculator.calculateCurrentStreak(
            completedDates = dates,
            today = today,
        )

        assertEquals(0, streak)
    }
}
