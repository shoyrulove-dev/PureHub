package com.purehub.app.feature.lunar

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.purehub.app.MainActivity
import com.purehub.app.R
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class LunarReminderWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val events = LunarEventRepository(context).load()
        val delivered = context.getSharedPreferences("lunar_reminder_delivery", Context.MODE_PRIVATE)
        val now = LocalTime.now()
        val region = LunarPreferences.region(context)
        events.filter { it.enabled && it.deletedAtEpochMs == null }.forEach { event ->
          event.reminderOffsets.forEach { offset ->
            if (now.isBefore(LocalTime.of(event.reminderHour, event.reminderMinute))) return@forEach
            val target = LocalDate.now().plusDays(offset.toLong())
            val lunar = LunarCalendarConverter.convertSolarToLunar(target, region)
            val leapMatches = when (event.leapRule) {
                LunarLeapRule.MainMonth -> !lunar.isLeapMonth
                LunarLeapRule.LeapMonth -> lunar.isLeapMonth
                LunarLeapRule.Both -> true
            }
            val exactDay = lunar.day == event.day
            val lastDayFallback = event.day == 30 && lunar.day == 29 &&
                LunarCalendarConverter.convertSolarToLunar(target.plusDays(1), region).let {
                    it.month != lunar.month || it.isLeapMonth != lunar.isLeapMonth
                }
            val monthMatches = event.recurrence == LunarEventRecurrence.Monthly || lunar.month == event.month
            val deliveryKey = "${event.id}:${target}:$offset"
            if ((exactDay || lastDayFallback) && monthMatches && leapMatches && !delivered.getBoolean(deliveryKey, false)) {
                if (notify(event, target, offset)) delivered.edit().putBoolean(deliveryKey, true).apply()
            }
          }
        }
        return Result.success()
    }

    private fun notify(event: LunarEvent, target: LocalDate, offset: Int): Boolean {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Lunar reminders", NotificationManager.IMPORTANCE_DEFAULT))
        val intent = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val lead = if (offset == 0) "Today" else "In $offset day(s)"
        manager.notify(event.id.hashCode(), NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(event.title)
            .setContentText("$lead · $target · lunar ${event.day}/${event.month}")
            .setContentIntent(intent).setAutoCancel(true).build())
        return true
    }

    companion object {
        private const val CHANNEL = "lunar_reminders"
        private const val WORK = "purehub_lunar_reminder_check"
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<LunarReminderWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }
}
