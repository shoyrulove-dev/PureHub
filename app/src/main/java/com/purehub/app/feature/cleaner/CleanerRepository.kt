package com.purehub.app.feature.cleaner

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
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
        selectedUris: List<Uri>,
        onProgress: suspend (String) -> Unit,
    ): CleanerScanResult = withContext(Dispatchers.IO) {
        onProgress("Reading files selected in Android")
        val selectedFiles = selectedUris.distinct().mapNotNull(::readSelectedFile)
        val largeFiles = selectedFiles.filter { it.sizeBytes >= LARGE_FILE_THRESHOLD_BYTES }

        onProgress("Analyzing byte-for-byte duplicates")
        val duplicateGroups = findDuplicateGroups(selectedFiles) { file ->
            hashFile(contentResolver.openInputStream(file.contentUri))
        }

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

    private fun readSelectedFile(uri: Uri): CleanerFileItem? {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE, DocumentsContract.Document.COLUMN_LAST_MODIFIED)
        return runCatching {
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                val modifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                CleanerFileItem(
                    id = uri.toString().hashCode().toLong(),
                    name = if (nameIndex >= 0) cursor.getString(nameIndex).orEmpty() else uri.lastPathSegment.orEmpty(),
                    sizeBytes = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else 0L,
                    mimeType = contentResolver.getType(uri).orEmpty(),
                    modifiedAtSeconds = if (modifiedIndex >= 0 && !cursor.isNull(modifiedIndex)) cursor.getLong(modifiedIndex) / 1000L else 0L,
                    contentUri = uri,
                )
            }
        }.getOrNull()
    }

    private fun hashFile(inputStream: InputStream?): String? {
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
