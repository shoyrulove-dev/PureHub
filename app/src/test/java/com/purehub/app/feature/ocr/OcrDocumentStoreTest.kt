package com.purehub.app.feature.ocr

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OcrDocumentStoreTest {
    private lateinit var store: OcrDocumentStore

    @Before
    fun setUp() {
        store = OcrDocumentStore(RuntimeEnvironment.getApplication())
        store.clear()
    }

    @After
    fun tearDown() {
        store.clear()
    }

    @Test
    fun savesReopensAndUpdatesCompleteDocuments() {
        val first = bitmap(Color.WHITE)
        val second = bitmap(Color.YELLOW)
        val saved = store.save(
            null,
            "Private notes",
            listOf(OcrStoredPage(first, "Page one", "Camera"), OcrStoredPage(second, "Page two", "Gallery")),
        )

        assertEquals(2, saved.pageCount)
        val loaded = requireNotNull(store.load(saved.id))
        assertEquals("Private notes", loaded.title)
        assertEquals(listOf("Page one", "Page two"), loaded.pages.map(OcrStoredPage::text))
        loaded.pages.forEach { it.bitmap.recycle() }

        val updated = store.save(saved.id, "Updated", listOf(OcrStoredPage(first, "Edited", "Camera")))
        val reopened = requireNotNull(store.load(updated.id))
        assertEquals(1, reopened.pages.size)
        assertEquals("Edited", reopened.pages.single().text)
        reopened.pages.forEach { it.bitmap.recycle() }
        first.recycle()
        second.recycle()
    }

    @Test
    fun rejectsUnsafeDocumentIdentifiers() {
        assertNull(store.load("../outside"))
        assertFalse(store.delete("../outside"))
    }

    @Test
    fun deletesOnlyTheSelectedDocument() {
        val page = bitmap(Color.WHITE)
        val first = store.save(null, "First", listOf(OcrStoredPage(page, "one", "Camera")))
        val second = store.save(null, "Second", listOf(OcrStoredPage(page, "two", "Camera")))

        assertTrue(store.delete(first.id))
        assertNull(store.load(first.id))
        assertEquals("Second", store.load(second.id)?.title)
        store.load(second.id)?.pages?.forEach { it.bitmap.recycle() }
        page.recycle()
    }

    private fun bitmap(color: Int): Bitmap = Bitmap.createBitmap(80, 120, Bitmap.Config.ARGB_8888).apply {
        eraseColor(color)
    }
}
