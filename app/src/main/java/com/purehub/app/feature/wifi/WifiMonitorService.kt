package com.purehub.app.feature.wifi

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.purehub.app.MainActivity
import com.purehub.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * User-started, battery-aware connection monitor. The persistent notification is
 * intentional: Android may otherwise stop sampling as soon as PureHub leaves the screen.
 */
class WifiMonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null
    private lateinit var wifiManager: WifiManager
    private val preferences by lazy { getSharedPreferences(PREFERENCES, MODE_PRIVATE) }

    override fun onCreate() {
        super.onCreate()
        wifiManager = applicationContext.getSystemService(WifiManager::class.java)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSession()
            else -> startSession(
                durationMinutes = intent?.getIntExtra(EXTRA_DURATION_MINUTES, 30) ?: 30,
                intervalSeconds = intent?.getIntExtra(EXTRA_INTERVAL_SECONDS, 10) ?: 10,
            )
        }
        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startSession(durationMinutes: Int, intervalSeconds: Int) {
        if (monitorJob?.isActive == true) return
        val now = System.currentTimeMillis()
        val stored = WifiMonitorStore.loadActive(preferences)
        if (stored != null && stored.endsAt <= now) {
            WifiMonitorStore.finishActive(preferences, stored.endsAt)
            stopSelf()
            return
        }
        val existing = stored?.takeIf { it.endsAt > now }
        val session = existing ?: WifiMonitorSession(
            startedAt = now,
            endsAt = now + durationMinutes.coerceIn(1, 8 * 60) * 60_000L,
            intervalSeconds = intervalSeconds.coerceIn(5, 300),
            samples = emptyList(),
            events = emptyList(),
        )
        WifiMonitorStore.saveActive(preferences, session)
        val foregroundType = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(session), foregroundType)
        monitorJob = scope.launch {
            var current = session
            var lastInternet = current.samples.lastOrNull()?.internetReachable
            var lastBssid = current.samples.lastOrNull()?.bssid
            while (isActive && System.currentTimeMillis() < current.endsAt) {
                val sample = collectSample()
                val events = current.events.toMutableList()
                if (lastInternet == true && !sample.internetReachable) events += WifiMonitorEvent(sample.timestamp, "Outage", "Internet became unreachable")
                if (lastInternet == false && sample.internetReachable) events += WifiMonitorEvent(sample.timestamp, "Recovery", "Internet connection recovered")
                if (!lastBssid.isNullOrBlank() && lastBssid != "--" && sample.bssid != "--" && lastBssid != sample.bssid) {
                    events += WifiMonitorEvent(sample.timestamp, "Roaming", "${lastBssid.takeLast(5)} -> ${sample.bssid.takeLast(5)}")
                }
                if ((sample.latencyMs ?: 0) >= 150) events += WifiMonitorEvent(sample.timestamp, "Latency spike", "${sample.latencyMs} ms")
                current = current.copy(samples = (current.samples + sample).takeLast(4_320), events = events.takeLast(500))
                WifiMonitorStore.saveActive(preferences, current)
                getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(current))
                sendBroadcast(Intent(ACTION_UPDATED).setPackage(packageName))
                lastInternet = sample.internetReachable
                lastBssid = sample.bssid
                delay(current.intervalSeconds * 1_000L)
            }
            finishSession()
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun collectSample(): WifiMonitorSample = withContext(Dispatchers.IO) {
        @Suppress("DEPRECATION") val info = runCatching { wifiManager.connectionInfo }.getOrNull()
        @Suppress("DEPRECATION") val gateway = runCatching { WifiDiagnostics.gatewayHost(wifiManager.dhcpInfo) }.getOrNull()
        val health = WifiDiagnostics.quickHealth(gateway)
        WifiMonitorSample(
            timestamp = System.currentTimeMillis(),
            rssi = info?.rssi ?: -100,
            bssid = info?.bssid?.takeUnless { it == "02:00:00:00:00:00" }.orEmpty().ifBlank { "--" },
            gatewayReachable = health.first,
            internetReachable = health.second != null,
            latencyMs = health.second,
        )
    }

    private fun stopSession() {
        monitorJob?.cancel()
        monitorJob = null
        finishSession()
    }

    private fun finishSession() {
        WifiMonitorStore.finishActive(preferences)
        sendBroadcast(Intent(ACTION_UPDATED).setPackage(packageName))
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun notification(session: WifiMonitorSession): android.app.Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this, 1, Intent(this, WifiMonitorService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val uptime = if (session.samples.isEmpty()) 100 else session.samples.count(WifiMonitorSample::internetReachable) * 100 / session.samples.size
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.app_name) + " network monitor")
            .setContentText("${session.samples.size} samples · $uptime% uptime · ${session.events.size} events")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openIntent)
            .addAction(0, "Stop and save", stopIntent)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Network monitoring", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Visible while a user-started Wi-Fi reliability session is running"
                },
            )
        }
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val PREFERENCES = "purehub.wifi-monitor.v1"
        const val ACTION_UPDATED = "com.purehub.app.WIFI_MONITOR_UPDATED"
        const val ACTION_STOP = "com.purehub.app.WIFI_MONITOR_STOP"
        const val EXTRA_DURATION_MINUTES = "duration_minutes"
        const val EXTRA_INTERVAL_SECONDS = "interval_seconds"
        private const val CHANNEL_ID = "wifi_monitor"
        private const val NOTIFICATION_ID = 4107
    }
}
