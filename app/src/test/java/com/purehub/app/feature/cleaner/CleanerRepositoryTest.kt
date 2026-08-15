package com.purehub.app.feature.cleaner

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CleanerRepositoryTest {
    @Test
    fun groupsDuplicatesBySizeAndHash() {
        val files = listOf(
            CleanerFileItem(1, "a.jpg", 100, "image/jpeg", 3, Uri.parse("content://purehub/a")),
            CleanerFileItem(2, "b.jpg", 100, "image/jpeg", 2, Uri.parse("content://purehub/b")),
            CleanerFileItem(3, "c.jpg", 100, "image/jpeg", 1, Uri.parse("content://purehub/c")),
        )

        val groups = findDuplicateGroups(files) { file ->
            when (file.id) {
                1L, 2L -> "same"
                else -> "other"
            }
        }

        assertEquals(1, groups.size)
        assertEquals(2, groups.first().files.size)
    }

    @Test
    fun reclaimableTotalDoesNotCountLargeDuplicateTwice() {
        val keep = CleanerFileItem(1, "keep.jpg", 200, "image/jpeg", 3, Uri.parse("content://purehub/keep"))
        val copy = CleanerFileItem(2, "copy.jpg", 200, "image/jpeg", 2, Uri.parse("content://purehub/copy"))
        val state = CleanerUiState(
            largeFiles = listOf(copy),
            duplicateGroups = listOf(DuplicateImageGroup("same", listOf(keep, copy))),
        )

        assertEquals(200, state.totalReclaimableBytes)
        assertEquals(200, state.exactDuplicateBytes)
    }
}
