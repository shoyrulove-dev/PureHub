package com.purehub.app.feature

import android.Manifest
import android.app.NotificationManager
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.testing.TestListenableWorkerBuilder
import com.purehub.app.feature.lunar.LunarCalendarConverter
import com.purehub.app.feature.lunar.LunarEvent
import com.purehub.app.feature.lunar.LunarEventRepository
import com.purehub.app.feature.lunar.LunarLeapRule
import com.purehub.app.feature.lunar.LunarPreferences
import com.purehub.app.feature.lunar.LunarReminderWorker
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LunarReminderInstrumentedTest {
    @Test
    fun workerPostsTodayAndSkipsTomorrowAndWrongLeapRule() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        if (Build.VERSION.SDK_INT >= 33) {
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        val notifications = context.getSystemService(NotificationManager::class.java)
        val repository = LunarEventRepository(context)
        val savedEvents = repository.load()
        val today = LocalDate.now()
        val region = LunarPreferences.region(context)
        val lunarToday = LunarCalendarConverter.convertSolarToLunar(today, region)
        val lunarTomorrow = LunarCalendarConverter.convertSolarToLunar(today.plusDays(1), region)
        val matchingRule = if (lunarToday.isLeapMonth) LunarLeapRule.LeapMonth else LunarLeapRule.MainMonth
        val oppositeRule = if (lunarToday.isLeapMonth) LunarLeapRule.MainMonth else LunarLeapRule.LeapMonth
        val due = LunarEvent(title = "Beta 64 lunar reminder", day = lunarToday.day, month = lunarToday.month,
            leapRule = matchingRule, reminderHour = 0)
        val future = LunarEvent(title = "Tomorrow", day = lunarTomorrow.day, month = lunarTomorrow.month,
            reminderHour = 0)
        val wrongLeapRule = LunarEvent(title = "Wrong leap month", day = lunarToday.day, month = lunarToday.month,
            leapRule = oppositeRule, reminderHour = 0)

        try {
            assertTrue("Enable notifications for PureHub debug before running this test", notifications.areNotificationsEnabled())
            repository.save(listOf(due, future, wrongLeapRule))
            TestListenableWorkerBuilder<LunarReminderWorker>(context).build().doWork()
            val delivered = notifications.activeNotifications.map { it.id }
            assertTrue(due.id.hashCode() in delivered)
            assertFalse(future.id.hashCode() in delivered)
            assertFalse(wrongLeapRule.id.hashCode() in delivered)
        } finally {
            repository.save(savedEvents)
            listOf(due, future, wrongLeapRule).forEach { notifications.cancel(it.id.hashCode()) }
            context.getSharedPreferences("lunar_reminder_delivery", 0).edit()
                .remove("${due.id}:${today}:0").apply()
        }
    }
}
