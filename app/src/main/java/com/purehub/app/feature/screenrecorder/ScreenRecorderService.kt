package com.purehub.app.feature.screenrecorder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import java.io.FileDescriptor

class ScreenRecorderService : Service() {
    private var recorder: MediaRecorder? = null
    private var projection: MediaProjection? = null
    private var descriptor: android.os.ParcelFileDescriptor? = null
    private var outputUri: android.net.Uri? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopRecording()
            ACTION_START -> startRecording(intent)
        }
        return START_NOT_STICKY
    }

    private fun startRecording(intent: Intent) {
        createNotificationChannel()
        val stopIntent = Intent(this, ScreenRecorderService::class.java).setAction(ACTION_STOP)
        val stopPending = android.app.PendingIntent.getService(this, 7, stopIntent, android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(com.purehub.app.R.mipmap.ic_launcher)
            .setContentTitle("PureHub screen recording")
            .setContentText("Recording locally on this device")
            .setOngoing(true)
            .addAction(0, "Stop", stopPending)
            .build()
        ServiceCompat.startForeground(this, 702, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)

        val data = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(EXTRA_DATA, Intent::class.java) else @Suppress("DEPRECATION") intent.getParcelableExtra(EXTRA_DATA)
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        if (data == null || resultCode == 0) return stopSelf()
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val activeProjection = manager.getMediaProjection(resultCode, data) ?: return stopSelf()
        projection = activeProjection
        activeProjection.registerCallback(object : MediaProjection.Callback() { override fun onStop() { release() } }, Handler(Looper.getMainLooper()))
        val metrics = resources.displayMetrics
        val width = (metrics.widthPixels / 2) * 2
        val height = (metrics.heightPixels / 2) * 2
        val values = android.content.ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "PureHub-${System.currentTimeMillis()}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/PureHub")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        outputUri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        descriptor = outputUri?.let { contentResolver.openFileDescriptor(it, "w") }
        val fileDescriptor: FileDescriptor = descriptor?.fileDescriptor ?: return stopSelf()
        recorder = MediaRecorder(this).apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setVideoEncodingBitRate(6_000_000)
            setVideoFrameRate(30)
            setVideoSize(width, height)
            setOutputFile(fileDescriptor)
            prepare()
        }
        projection?.createVirtualDisplay("PureHubRecorder", width, height, metrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, recorder?.surface, null, null)
        recorder?.start()
    }

    private fun stopRecording() {
        runCatching { recorder?.stop() }
        outputUri?.let { uri -> contentResolver.update(uri, android.content.ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }, null, null) }
        release()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun release() {
        runCatching { recorder?.release() }; recorder = null
        runCatching { projection?.stop() }; projection = null
        runCatching { descriptor?.close() }; descriptor = null
    }

    override fun onDestroy() { release(); super.onDestroy() }

    private fun createNotificationChannel() {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(NotificationChannel(CHANNEL, "Screen recorder", NotificationManager.IMPORTANCE_LOW))
    }

    companion object {
        const val ACTION_START = "purehub.screen.START"
        const val ACTION_STOP = "purehub.screen.STOP"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "result_data"
        private const val CHANNEL = "purehub_screen_recorder"
    }
}
