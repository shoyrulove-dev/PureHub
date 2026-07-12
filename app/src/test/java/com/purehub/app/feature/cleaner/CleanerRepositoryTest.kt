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
}
