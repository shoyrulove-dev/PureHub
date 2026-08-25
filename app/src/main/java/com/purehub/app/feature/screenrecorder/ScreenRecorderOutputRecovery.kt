package com.purehub.app.feature.screenrecorder

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import java.util.concurrent.Executors

private const val RECORDING_PREFIX = "PureHub-"
private const val RECORDING_SUFFIX = ".mp4"
private const val RECORDING_DIRECTORY = "Movies/PureHub"

internal fun isPureHubPendingRecording(
    displayName: String,
    relativePath: String,
    ownerPackageName: String,
    appPackageName: String,
): Boolean =
    ownerPackageName == appPackageName &&
        relativePath.trimEnd('/') == RECORDING_DIRECTORY &&
        displayName.startsWith(RECORDING_PREFIX) &&
        displayName.endsWith(RECORDING_SUFFIX, ignoreCase = true)

object ScreenRecorderOutputRecovery {
    fun schedule(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val appContext = context.applicationContext
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            try {
                recoverPendingOutputs(appContext)
            } finally {
                executor.shutdown()
            }
        }
    }

    internal fun recoverPendingOutputs(context: Context): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return 0
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.RELATIVE_PATH,
            MediaStore.Video.Media.OWNER_PACKAGE_NAME,
        )
        val selection = buildString {
            append("${MediaStore.Video.Media.IS_PENDING} = ?")
            append(" AND ${MediaStore.Video.Media.OWNER_PACKAGE_NAME} = ?")
            append(" AND ${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?")
            append(" AND ${MediaStore.Video.Media.DISPLAY_NAME} LIKE ?")
        }
        val args = arrayOf(
            "1",
            context.packageName,
            "$RECORDING_DIRECTORY%",
            "$RECORDING_PREFIX%$RECORDING_SUFFIX",
        )
        return runCatching {
            val staleUris = buildList {
                context.contentResolver.query(collection, projection, selection, args, null)?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                    val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.RELATIVE_PATH)
                    val ownerColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.OWNER_PACKAGE_NAME)
                    while (cursor.moveToNext()) {
                        if (
                            isPureHubPendingRecording(
                                displayName = cursor.getString(nameColumn).orEmpty(),
                                relativePath = cursor.getString(pathColumn).orEmpty(),
                                ownerPackageName = cursor.getString(ownerColumn).orEmpty(),
                                appPackageName = context.packageName,
                            )
                        ) {
                            add(ContentUris.withAppendedId(collection, cursor.getLong(idColumn)))
                        }
                    }
                }
            }
            staleUris.count { uri -> runCatching { context.contentResolver.delete(uri, null, null) > 0 }.getOrDefault(false) }
        }.getOrDefault(0)
    }
}
