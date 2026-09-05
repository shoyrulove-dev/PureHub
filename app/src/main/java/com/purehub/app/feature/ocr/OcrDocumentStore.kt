package com.purehub.app.feature.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.util.UUID

data class OcrStoredPage(
    val bitmap: Bitmap,
    val text: String,
    val source: String,
)

data class OcrStoredDocument(
    val id: String,
    val title: String,
    val savedAt: String,
    val pages: List<OcrStoredPage>,
)

data class OcrSavedDocument(
    val id: String,
    val title: String,
    val savedAt: String,
    val pageCount: Int,
)

/** Keeps complete OCR documents private in app-internal storage. */
class OcrDocumentStore(context: Context) {
    private val root = File(context.filesDir, "ocr_documents").apply { mkdirs() }

    fun save(id: String?, title: String, pages: List<OcrStoredPage>): OcrSavedDocument {
        require(pages.isNotEmpty()) { "At least one OCR page is required" }
        val safeId = id?.takeIf(::isSafeId) ?: UUID.randomUUID().toString()
        val directory = File(root, safeId).apply { mkdirs() }
        val pageJson = JSONArray()
        pages.forEachIndexed { index, page ->
            val imageName = "page_${index + 1}.jpg"
            val image = File(directory, imageName)
            val pendingImage = File(directory, "$imageName.pending")
            FileOutputStream(pendingImage).use { output ->
                check(page.bitmap.compress(Bitmap.CompressFormat.JPEG, 94, output)) { "OCR page could not be saved" }
            }
            Files.move(pendingImage.toPath(), image.toPath(), StandardCopyOption.REPLACE_EXISTING)
            pageJson.put(
                JSONObject()
                    .put("image", imageName)
                    .put("text", page.text)
                    .put("source", page.source),
            )
        }
        directory.listFiles { file -> file.name.startsWith("page_") && file.extension == "jpg" }
            ?.filter { file -> pageJson.none { item -> (item as JSONObject).optString("image") == file.name } }
            ?.forEach(File::delete)
        val savedAt = Instant.now().toString()
        val safeTitle = title.trim().ifBlank { "Untitled scan" }
        val manifest = JSONObject()
            .put("id", safeId)
            .put("title", safeTitle)
            .put("savedAt", savedAt)
            .put("pages", pageJson)
        val pendingManifest = File(directory, "manifest.json.pending")
        pendingManifest.writeText(manifest.toString(), Charsets.UTF_8)
        Files.move(
            pendingManifest.toPath(),
            File(directory, "manifest.json").toPath(),
            StandardCopyOption.REPLACE_EXISTING,
        )
        return OcrSavedDocument(safeId, safeTitle, savedAt, pages.size)
    }

    fun load(id: String): OcrStoredDocument? {
        if (!isSafeId(id)) return null
        val directory = File(root, id)
        val manifestFile = File(directory, "manifest.json")
        if (!manifestFile.isFile) return null
        return runCatching {
            val manifest = JSONObject(manifestFile.readText(Charsets.UTF_8))
            val pageArray = manifest.getJSONArray("pages")
            val pages = (0 until pageArray.length()).mapNotNull { index ->
                val item = pageArray.getJSONObject(index)
                val image = File(directory, item.getString("image"))
                val bitmap = BitmapFactory.decodeFile(
                    image.absolutePath,
                    BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 },
                ) ?: return@mapNotNull null
                OcrStoredPage(bitmap, item.optString("text"), item.optString("source", "Saved page"))
            }
            if (pages.isEmpty()) return null
            OcrStoredDocument(
                id = id,
                title = manifest.optString("title", "Untitled scan"),
                savedAt = manifest.optString("savedAt"),
                pages = pages,
            )
        }.getOrNull()
    }

    fun delete(id: String): Boolean {
        if (!isSafeId(id)) return false
        return File(root, id).takeIf(File::exists)?.deleteRecursively() ?: true
    }

    fun clear() {
        root.listFiles()?.forEach(File::deleteRecursively)
    }

    /** Removes document directories no longer referenced by the bounded history index. */
    fun prune(retainedIds: Set<String>) {
        val safeIds = retainedIds.filterTo(mutableSetOf(), ::isSafeId)
        root.listFiles()
            ?.filter(File::isDirectory)
            ?.filterNot { it.name in safeIds }
            ?.forEach(File::deleteRecursively)
    }

    private fun isSafeId(id: String): Boolean = id.matches(Regex("[a-zA-Z0-9-]{8,64}"))
}

private fun JSONArray.none(predicate: (Any) -> Boolean): Boolean {
    for (index in 0 until length()) if (predicate(get(index))) return false
    return true
}
