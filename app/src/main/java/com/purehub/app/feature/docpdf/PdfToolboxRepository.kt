package com.purehub.app.feature.docpdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ImportedPdf(
    val name: String,
    val file: File,
    val pageCount: Int,
    val bytes: Long,
)

data class VisualSignature(
    val signer: String,
    val pageNumber: Int,
)

/**
 * Private PDF operations built on Android's local renderer. Imported pages are deliberately
 * flattened when exported. This avoids cloud processing and preserves the visible page while
 * removing interactive forms, scripts and hidden attachments from the output.
 */
class PdfToolboxRepository(private val context: Context) {
    private val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    fun importPdf(uri: Uri, index: Int): ImportedPdf {
        val directory = File(context.cacheDir, "pdf_toolbox_imports").apply { mkdirs() }
        val file = File(directory, "pdf_${timestamp.format(Date())}_$index.pdf")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use(input::copyTo)
        } ?: error("The selected PDF could not be opened")
        val count = openRenderer(file) { it.pageCount }
        return ImportedPdf(
            name = queryDisplayName(uri) ?: "Document ${index + 1}",
            file = file,
            pageCount = count,
            bytes = file.length(),
        )
    }

    fun merge(inputs: List<ImportedPdf>, title: String): ExportedPdf {
        require(inputs.isNotEmpty()) { "Add at least one PDF" }
        return renderPdf(inputs.flatMap { item -> (0 until item.pageCount).map { item.file to it } }, title)
    }

    fun split(input: ImportedPdf, firstPage: Int, lastPage: Int, title: String): ExportedPdf {
        val first = firstPage.coerceIn(1, input.pageCount)
        val last = lastPage.coerceIn(first, input.pageCount)
        return renderPdf((first..last).map { input.file to (it - 1) }, title)
    }

    fun compress(input: ImportedPdf, title: String, quality: Float): ExportedPdf {
        val normalized = quality.coerceIn(0.45f, 0.9f)
        val width = (1240 * normalized).toInt().coerceAtLeast(560)
        return renderPdf(
            pages = (0 until input.pageCount).map { input.file to it },
            title = title,
            renderWidth = width,
        )
    }

    fun sign(input: ImportedPdf, title: String, signature: VisualSignature): ExportedPdf {
        require(signature.signer.isNotBlank()) { "Enter a signer name" }
        return renderPdf(
            pages = (0 until input.pageCount).map { input.file to it },
            title = title,
            signature = signature.copy(pageNumber = signature.pageNumber.coerceIn(1, input.pageCount)),
        )
    }

    private fun renderPdf(
        pages: List<Pair<File, Int>>,
        title: String,
        renderWidth: Int = 1240,
        signature: VisualSignature? = null,
    ): ExportedPdf {
        val output = outputFile(title)
        val document = PdfDocument()
        var outputIndex = 0
        var activeFile: File? = null
        var descriptor: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pages.forEach { (file, pageIndex) ->
                if (activeFile != file) {
                    renderer?.close()
                    descriptor?.close()
                    descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    renderer = PdfRenderer(descriptor!!)
                    activeFile = file
                }
                val source = renderer!!.openPage(pageIndex)
                try {
                    val ratio = source.height.toFloat() / source.width.coerceAtLeast(1)
                    val bitmapWidth = renderWidth
                    val bitmapHeight = (bitmapWidth * ratio).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    source.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    outputIndex += 1
                    val pageInfo = PdfDocument.PageInfo.Builder(bitmapWidth, bitmapHeight, outputIndex).create()
                    val target = document.startPage(pageInfo)
                    target.canvas.drawBitmap(bitmap, null, RectF(0f, 0f, bitmapWidth.toFloat(), bitmapHeight.toFloat()), null)
                    if (signature != null && outputIndex == signature.pageNumber) {
                        drawSignature(target.canvas, bitmapWidth, bitmapHeight, signature.signer)
                    }
                    document.finishPage(target)
                    bitmap.recycle()
                } finally {
                    source.close()
                }
            }
            FileOutputStream(output).use(document::writeTo)
        } finally {
            renderer?.close()
            descriptor?.close()
            document.close()
        }
        return ExportedPdf(
            uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", output),
            file = output,
        )
    }

    private fun drawSignature(canvas: android.graphics.Canvas, width: Int, height: Int, signer: String) {
        val boxWidth = (width * 0.38f).coerceAtMost(460f)
        val boxHeight = (height * 0.11f).coerceAtMost(165f)
        val left = width - boxWidth - width * 0.045f
        val top = height - boxHeight - height * 0.045f
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 118, 110)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(RectF(left, top, left + boxWidth, top + boxHeight), 14f, 14f, border)
        val signaturePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = (boxHeight * 0.32f).coerceIn(25f, 48f)
            typeface = Typeface.create("cursive", Typeface.ITALIC)
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 118, 110)
            textSize = (boxHeight * 0.14f).coerceIn(13f, 20f)
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("SIGNED LOCALLY BY PUREHUB", left + 18f, top + 28f, labelPaint)
        canvas.drawText(signer.take(42), left + 18f, top + boxHeight * 0.7f, signaturePaint)
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        canvas.drawText(date, left + 18f, top + boxHeight - 12f, labelPaint)
    }

    private fun outputFile(title: String): File {
        val directory = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS), "PureHub").apply { mkdirs() }
        val safe = title.trim().ifBlank { "purehub_pdf" }.replace(Regex("[^a-zA-Z0-9_-]"), "_").lowercase(Locale.US)
        return File(directory, "${safe}_${timestamp.format(Date())}.pdf")
    }

    private inline fun <T> openRenderer(file: File, block: (PdfRenderer) -> T): T {
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use(block)
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }
}
