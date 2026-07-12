package com.purehub.app.feature.cleaner

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import java.io.InputStream
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val LARGE_FILE_THRESHOLD_BYTES = 100L * 1024L * 1024L

data class CleanerFileItem(
    val id: Long,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val modifiedAtSeconds: Long,
    val contentUri: Uri,
)

data class DuplicateImageGroup(
    val hash: String,
    val files: List<CleanerFileItem>,
)

data class CleanerScanResult(
    val largeFiles: List<CleanerFileItem>,
    val duplicateGroups: List<DuplicateImageGroup>,
)

class CleanerRepository(
    private val contentResolver: ContentResolver,
) {
    suspend fun scan(
        onProgress: suspend (String) -> Unit,
    ): CleanerScanResult = withContext(Dispatchers.IO) {
        onProgress("Scanning large media files")
        val largeFiles = queryLargeFiles()

        onProgress("Analyzing duplicate images")
        val duplicateGroups = queryDuplicateImages()

        CleanerScanResult(
            largeFiles = largeFiles.sortedByDescending { it.sizeBytes },
            duplicateGroups = duplicateGroups.sortedByDescending { group ->
                group.files.sumOf { it.sizeBytes }
            },
        )
    }

    suspend fun deleteFiles(files: List<CleanerFileItem>) = withContext(Dispatchers.IO) {
        files.forEach { file ->
            runCatching {
                contentResolver.delete(file.contentUri, null, null)
            }
        }
    }

    private fun queryLargeFiles(): List<CleanerFileItem> {
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
        )
        val selection = buildString {
            append("${MediaStore.Files.FileColumns.SIZE} >= ? AND ")
            append("${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?, ?)")
        }
        val args = arrayOf(
            LARGE_FILE_THRESHOLD_BYTES.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO.toString(),
        )

        return buildList {
            contentResolver.query(
                collection,
                projection,
                selection,
                args,
                "${MediaStore.Files.FileColumns.SIZE} DESC",
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val modifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    add(
                        CleanerFileItem(
                            id = id,
                            name = cursor.getString(nameColumn).orEmpty(),
                            sizeBytes = cursor.getLong(sizeColumn),
                            mimeType = cursor.getString(mimeColumn).orEmpty(),
                            modifiedAtSeconds = cursor.getLong(modifiedColumn),
                            contentUri = ContentUris.withAppendedId(collection, id),
                        ),
                    )
                }
            }
        }
    }

    private fun queryDuplicateImages(): List<DuplicateImageGroup> {
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_MODIFIED,
        )

        val images = buildList {
            contentResolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.SIZE} DESC",
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val modifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    add(
                        CleanerFileItem(
                            id = id,
                            name = cursor.getString(nameColumn).orEmpty(),
                            sizeBytes = cursor.getLong(sizeColumn),
                            mimeType = cursor.getString(mimeColumn).orEmpty(),
                            modifiedAtSeconds = cursor.getLong(modifiedColumn),
                            contentUri = ContentUris.withAppendedId(collection, id),
                        ),
                    )
                }
            }
        }

        return findDuplicateGroups(images) { file ->
            hashImage(contentResolver.openInputStream(file.contentUri))
        }
    }

    private fun hashImage(inputStream: InputStream?): String? {
        inputStream ?: return null
        return runCatching {
            inputStream.use { stream ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                val digest = MessageDigest.getInstance("SHA-256")
                var read = stream.read(buffer)
                while (read >= 0) {
                    digest.update(buffer, 0, read)
                    read = stream.read(buffer)
                }
                digest.digest().joinToString(separator = "") { "%02x".format(it) }
            }
        }.getOrNull()
    }
}

fun findDuplicateGroups(
    images: List<CleanerFileItem>,
    hashProvider: (CleanerFileItem) -> String?,
): List<DuplicateImageGroup> {
    return images
        .groupBy { it.sizeBytes }
        .values
        .asSequence()
        .filter { it.size > 1 }
        .flatMap { sameSizeFiles ->
            sameSizeFiles
                .groupBy(hashProvider)
                .entries
                .asSequence()
                .filter { (hash, files) -> hash != null && files.size > 1 }
                .map { entry ->
                    DuplicateImageGroup(
                        hash = entry.key.orEmpty(),
                        files = entry.value.sortedByDescending { it.modifiedAtSeconds },
                    )
                }
        }
        .toList()
}
