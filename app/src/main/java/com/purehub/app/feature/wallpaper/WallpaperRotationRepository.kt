package com.purehub.app.feature.wallpaper

import android.app.WallpaperManager
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class WallpaperRotationRepository(
    private val context: Context,
) {
    private val preferences = context.getSharedPreferences("purehub_wallpaper", Context.MODE_PRIVATE)
    private val workManager = WorkManager.getInstance(context)

    fun saveSelectedUris(uris: List<Uri>) {
        preferences.edit()
            .putStringSet(KEY_URIS, uris.map { it.toString() }.toSet())
            .apply()
    }

    fun loadSelectedUris(): List<Uri> {
        return preferences.getStringSet(KEY_URIS, emptySet()).orEmpty()
            .map(Uri::parse)
            .sortedBy { it.toString() }
    }

    fun saveRotationHours(hours: Int) {
        preferences.edit().putInt(KEY_HOURS, hours).apply()
    }

    fun loadRotationHours(): Int = preferences.getInt(KEY_HOURS, 24)

    fun setRotationEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun isRotationEnabled(): Boolean = preferences.getBoolean(KEY_ENABLED, false)

    fun scheduleRotation(hours: Int) {
        saveRotationHours(hours)
        setRotationEnabled(true)
        val workRequest = PeriodicWorkRequestBuilder<WallpaperRotationWorker>(
            repeatInterval = hours.toLong().coerceAtLeast(12L),
            repeatIntervalTimeUnit = TimeUnit.HOURS,
        ).build()
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest,
        )
    }

    fun cancelRotation() {
        setRotationEnabled(false)
        workManager.cancelUniqueWork(WORK_NAME)
    }

    fun applyNextWallpaperNow() {
        val uris = loadSelectedUris()
        if (uris.isEmpty()) return
        val currentIndex = preferences.getInt(KEY_INDEX, 0)
        val nextUri = uris[currentIndex % uris.size]

        context.contentResolver.openInputStream(nextUri)?.use { inputStream ->
            WallpaperManager.getInstance(context).setStream(inputStream)
        }

        preferences.edit().putInt(KEY_INDEX, (currentIndex + 1) % uris.size).apply()
    }

    companion object {
        private const val KEY_URIS = "selected_uris"
        private const val KEY_HOURS = "rotation_hours"
        private const val KEY_ENABLED = "rotation_enabled"
        private const val KEY_INDEX = "rotation_index"
        const val WORK_NAME = "purehub_wallpaper_rotation"
    }
}
