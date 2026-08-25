package com.purehub.app.feature.screenrecorder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import java.io.File
import java.io.FileDescriptor

class ScreenRecorderService : Service() {
    private var recorder: MediaRecorder? = null
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var descriptor: android.os.ParcelFileDescriptor? = null
    private var outputUri: Uri? = null
    private var legacyOutputFile: File? = null
    private var recordingStarted = false
    private var stopping = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopRecording()
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
            ACTION_START -> startRecording(intent)
        }
        return START_NOT_STICKY
    }

    private fun startRecording(intent: Intent) {
        if (ScreenRecorderRuntime.status.value.phase != ScreenRecordingPhase.IDLE) return
        ScreenRecorderRuntime.update(ScreenRecordingPhase.PREPARING, "Preparing a private local recording…")
        createNotificationChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(ScreenRecordingPhase.PREPARING),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
        )

        runCatching {
            val data = if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(EXTRA_DATA)
            }
            val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
            require(data != null && resultCode != 0) { "Screen capture permission was not granted." }

            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val activeProjection = requireNotNull(manager.getMediaProjection(resultCode, data)) {
                "Android could not start screen capture."
            }
            projection = activeProjection
            activeProjection.registerCallback(
                object : MediaProjection.Callback() {
                    override fun onStop() {
                        if (!stopping) stopRecording(stopProjection = false)
                    }
                },
                Handler(Looper.getMainLooper()),
            )

            val metrics = resources.displayMetrics
            val size = calculateCaptureSize(
                displayWidth = metrics.widthPixels,
                displayHeight = metrics.heightPixels,
                widthCap = intent.getIntExtra(EXTRA_WIDTH_CAP, DEFAULT_WIDTH_CAP).coerceIn(480, 2160),
            )
            val frameRate = intent.getIntExtra(EXTRA_FRAME_RATE, DEFAULT_FRAME_RATE).coerceIn(24, 60)
            val bitRate = intent.getIntExtra(EXTRA_BIT_RATE, DEFAULT_BIT_RATE).coerceIn(2_000_000, 20_000_000)
            createOutput("PureHub-${System.currentTimeMillis()}.mp4")
            val fileDescriptor: FileDescriptor? = descriptor?.fileDescriptor
            val outputFile = legacyOutputFile
            require(fileDescriptor != null || outputFile != null) { "Android could not create the recording file." }

            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this) else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder?.apply {
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoEncodingBitRate(bitRate)
                setVideoFrameRate(frameRate)
                setVideoSize(size.width, size.height)
                if (fileDescriptor != null) setOutputFile(fileDescriptor) else setOutputFile(outputFile!!.absolutePath)
                prepare()
            }
            virtualDisplay = activeProjection.createVirtualDisplay(
                "PureHubRecorder",
                size.width,
                size.height,
                metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                recorder?.surface,
                null,
                null,
            )
            recorder?.start()
            recordingStarted = true
            ScreenRecorderRuntime.update(
                ScreenRecordingPhase.RECORDING,
                "Recording ${size.width}×${size.height} at $frameRate fps. Stop from PureHub or the notification.",
            )
            updateNotification(ScreenRecordingPhase.RECORDING)
        }.onFailure { failure ->
            discardOutput()
            releaseResources(stopProjection = true)
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            ScreenRecorderRuntime.update(
                ScreenRecordingPhase.IDLE,
                failure.message ?: "Screen recording could not start.",
            )
            stopSelf()
        }
    }

    private fun pauseRecording() {
        if (ScreenRecorderRuntime.status.value.phase != ScreenRecordingPhase.RECORDING) return
        runCatching { recorder?.pause() }
            .onSuccess {
                ScreenRecorderRuntime.update(ScreenRecordingPhase.PAUSED, "Recording paused. Resume or save the recording.")
                updateNotification(ScreenRecordingPhase.PAUSED)
            }
            .onFailure { ScreenRecorderRuntime.update(ScreenRecordingPhase.RECORDING, "Pause is not supported by this device.") }
    }

    private fun resumeRecording() {
        if (ScreenRecorderRuntime.status.value.phase != ScreenRecordingPhase.PAUSED) return
        runCatching { recorder?.resume() }
            .onSuccess {
                ScreenRecorderRuntime.update(ScreenRecordingPhase.RECORDING, "Recording resumed. Stop from PureHub or the notification.")
                updateNotification(ScreenRecordingPhase.RECORDING)
            }
            .onFailure { ScreenRecorderRuntime.update(ScreenRecordingPhase.PAUSED, "Resume failed. Save the current recording and try again.") }
    }

    private fun stopRecording(stopProjection: Boolean = true, stopService: Boolean = true) {
        if (stopping) return
        stopping = true
        val saved = recordingStarted && runCatching { recorder?.stop() }.isSuccess
        if (saved) finalizeOutput() else discardOutput()
        releaseResources(stopProjection)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        ScreenRecorderRuntime.update(
            ScreenRecordingPhase.IDLE,
            if (saved) "Saved locally to Movies/PureHub." else "No usable recording was created.",
        )
        stopping = false
        if (stopService) stopSelf()
    }

    private fun createOutput(displayName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/PureHub")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            outputUri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            descriptor = outputUri?.let { contentResolver.openFileDescriptor(it, "w") }
        } else {
            val directory = File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "PureHub").apply { mkdirs() }
            legacyOutputFile = File(directory, displayName)
        }
    }

    private fun finalizeOutput() {
        descriptor?.close()
        descriptor = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            outputUri?.let { uri ->
                contentResolver.update(uri, ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }, null, null)
            }
        } else {
            legacyOutputFile?.let { file ->
                MediaScannerConnection.scanFile(this, arrayOf(file.absolutePath), arrayOf("video/mp4"), null)
            }
        }
    }

    private fun discardOutput() {
        runCatching { descriptor?.close() }
        descriptor = null
        outputUri?.let { uri -> runCatching { contentResolver.delete(uri, null, null) } }
        legacyOutputFile?.let { file -> runCatching { file.delete() } }
    }

    private fun releaseResources(stopProjection: Boolean) {
        recordingStarted = false
        runCatching { recorder?.release() }
        recorder = null
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        val activeProjection = projection
        projection = null
        if (stopProjection) runCatching { activeProjection?.stop() }
        runCatching { descriptor?.close() }
        descriptor = null
        outputUri = null
        legacyOutputFile = null
    }

    override fun onDestroy() {
        if (recordingStarted && !stopping) stopRecording(stopProjection = false, stopService = false)
        else releaseResources(stopProjection = false)
        super.onDestroy()
    }

    private fun buildNotification(phase: ScreenRecordingPhase): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(com.purehub.app.R.mipmap.ic_launcher)
            .setContentTitle("PureHub screen recording")
            .setContentText(if (phase == ScreenRecordingPhase.PAUSED) "Paused — saved only when you stop" else "Recording locally on this device")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
        if (phase == ScreenRecordingPhase.RECORDING) builder.addAction(0, "Pause", serviceAction(ACTION_PAUSE, 8))
        if (phase == ScreenRecordingPhase.PAUSED) builder.addAction(0, "Resume", serviceAction(ACTION_RESUME, 9))
        return builder.addAction(0, "Stop & save", serviceAction(ACTION_STOP, 7)).build()
    }

    private fun serviceAction(action: String, requestCode: Int): PendingIntent = PendingIntent.getService(
        this,
        requestCode,
        Intent(this, ScreenRecorderService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun updateNotification(phase: ScreenRecordingPhase) {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, buildNotification(phase))
    }

    private fun createNotificationChannel() {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
            NotificationChannel(CHANNEL, "Screen recorder", NotificationManager.IMPORTANCE_LOW),
        )
    }

    companion object {
        const val ACTION_START = "purehub.screen.START"
        const val ACTION_STOP = "purehub.screen.STOP"
        const val ACTION_PAUSE = "purehub.screen.PAUSE"
        const val ACTION_RESUME = "purehub.screen.RESUME"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "result_data"
        const val EXTRA_WIDTH_CAP = "width_cap"
        const val EXTRA_FRAME_RATE = "frame_rate"
        const val EXTRA_BIT_RATE = "bit_rate"
        private const val DEFAULT_WIDTH_CAP = 1080
        private const val DEFAULT_FRAME_RATE = 30
        private const val DEFAULT_BIT_RATE = 8_000_000
        private const val NOTIFICATION_ID = 702
        private const val CHANNEL = "purehub_screen_recorder"
    }
}
