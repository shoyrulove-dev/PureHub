package com.purehub.app.feature.pomodoro

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.Manifest
import android.content.pm.PackageManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.purehub.app.MainActivity
import com.purehub.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PomodoroTimerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null
    private var targetElapsed = 0L
    private var label = "Focus session"

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTimer(intent.getIntExtra(EXTRA_SECONDS, 25 * 60), intent.getStringExtra(EXTRA_LABEL).orEmpty())
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESET -> stopTimer()
        }
        return START_NOT_STICKY
    }

    private fun startTimer(seconds: Int, requestedLabel: String) {
        label = requestedLabel.ifBlank { "Focus session" }
        targetElapsed = SystemClock.elapsedRealtime() + seconds.coerceAtLeast(1) * 1_000L
        startForeground(NOTIFICATION_ID, notification(seconds, running = true))
        timerJob?.cancel()
        timerJob = scope.launch {
            while (true) {
                val remaining = ((targetElapsed - SystemClock.elapsedRealtime() + 999L) / 1_000L).toInt()
                if (remaining <= 0) break
                notifyIfAllowed(notification(remaining, running = true))
                delay(1_000L)
            }
            notifyIfAllowed(notification(0, running = false, complete = true))
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopTimer() {
        timerJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun notification(seconds: Int, running: Boolean, complete: Boolean = false) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_purehub_monochrome)
            .setContentTitle(if (complete) "Focus session complete" else label)
            .setContentText(if (complete) "Nice work. Take a mindful break." else "%02d:%02d remaining".format(seconds / 60, seconds % 60))
            .setOngoing(running)
            .setOnlyAlertOnce(!complete)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 1, Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .addAction(0, "Pause", serviceIntent(ACTION_PAUSE, 2))
            .addAction(0, "Reset", serviceIntent(ACTION_RESET, 3))
            .build()

    private fun serviceIntent(action: String, requestCode: Int) = PendingIntent.getService(
        this, requestCode, Intent(this, PomodoroTimerService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun notifyIfAllowed(notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Pomodoro timer", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Active PureHub focus timer and completion alert"
                },
            )
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { timerJob?.cancel(); super.onDestroy() }

    companion object {
        private const val CHANNEL_ID = "purehub_pomodoro"
        private const val NOTIFICATION_ID = 2501
        const val ACTION_START = "com.purehub.app.pomodoro.START"
        const val ACTION_PAUSE = "com.purehub.app.pomodoro.PAUSE"
        const val ACTION_RESET = "com.purehub.app.pomodoro.RESET"
        const val EXTRA_SECONDS = "seconds"
        const val EXTRA_LABEL = "label"

        fun command(context: Context, action: String, seconds: Int = 0, label: String = "") {
            val intent = Intent(context, PomodoroTimerService::class.java).setAction(action)
                .putExtra(EXTRA_SECONDS, seconds).putExtra(EXTRA_LABEL, label)
            if (action == ACTION_START) context.startForegroundService(intent) else context.startService(intent)
        }
    }
}
