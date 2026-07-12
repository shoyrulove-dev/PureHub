package com.purehub.app.feature.docpdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CapturedDocPage(
    val uri: Uri,
    val file: File,
)

data class ExportedPdf(
    val uri: Uri,
    val file: File,
)

data class CropAdjustments(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
)

class DocPdfRepository(
    private val context: Context,
) {
    private val timestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    fun createCaptureFile(): File {
        val directory = File(context.cacheDir, "doc_captures").apply { mkdirs() }
        return File(directory, "capture_${timestampFormat.format(Date())}.jpg")
    }

    fun wrapCapturedFile(file: File): CapturedDocPage {
        return CapturedDocPage(
            uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            ),
            file = file,
        )
    }

    fun enhancePage(page: CapturedDocPage, crop: CropAdjustments): CapturedDocPage {
        val original = BitmapFactory.decodeFile(page.file.absolutePath) ?: return page
        val rotated = autoRotateIfLandscape(original)
        val trimmed = cropBitmap(rotated, crop)
        val enhanced = enhanceBitmap(trimmed)

        FileOutputStream(page.file).use { stream ->
            enhanced.compress(Bitmap.CompressFormat.JPEG, 92, stream)
        }

        if (rotated !== original) rotated.recycle()
        if (trimmed !== rotated) trimmed.recycle()
        enhanced.recycle()
        original.recycle()
        return wrapCapturedFile(page.file)
    }

    fun exportPdf(
        pages: List<CapturedDocPage>,
        title: String,
    ): ExportedPdf {
        val outputDirectory = File(
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS),
            "PureHub",
        ).apply { mkdirs() }
        val outputFile = File(
            outputDirectory,
            "${sanitizeFileName(title)}_${timestampFormat.format(Date())}.pdf",
        )

        val document = PdfDocument()
        pages.forEachIndexed { index, page ->
            val sourceBitmap = BitmapFactory.decodeFile(page.file.absolutePath) ?: return@forEachIndexed
            val pageInfo = PdfDocument.PageInfo.Builder(1240, 1754, index + 1).create()
            val pdfPage = document.startPage(pageInfo)
            drawBitmapCentered(
                canvas = pdfPage.canvas,
                bitmap = sourceBitmap,
                width = pageInfo.pageWidth,
                height = pageInfo.pageHeight,
            )
            document.finishPage(pdfPage)
            sourceBitmap.recycle()
        }

        FileOutputStream(outputFile).use { stream ->
            document.writeTo(stream)
        }
        document.close()

        return ExportedPdf(
            uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outputFile,
            ),
            file = outputFile,
        )
    }

    private fun drawBitmapCentered(
        canvas: Canvas,
        bitmap: Bitmap,
        width: Int,
        height: Int,
    ) {
        canvas.drawColor(Color.WHITE)

        val safeBitmap = autoRotateIfLandscape(bitmap)
        val scale = minOf(
            width.toFloat() / safeBitmap.width.toFloat(),
            height.toFloat() / safeBitmap.height.toFloat(),
        ) * 0.92f
        val destinationWidth = safeBitmap.width * scale
        val destinationHeight = safeBitmap.height * scale
        val left = (width - destinationWidth) / 2f
        val top = (height - destinationHeight) / 2f

        canvas.drawBitmap(
            safeBitmap,
            null,
            android.graphics.RectF(left, top, left + destinationWidth, top + destinationHeight),
            Paint(Paint.ANTI_ALIAS_FLAG),
        )

        if (safeBitmap !== bitmap) {
            safeBitmap.recycle()
        }
    }

    private fun autoRotateIfLandscape(bitmap: Bitmap): Bitmap {
        if (bitmap.height >= bitmap.width) return bitmap
        val matrix = Matrix().apply { postRotate(90f) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun sanitizeFileName(input: String): String {
        return input.trim()
            .ifBlank { "purehub_doc" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .lowercase(Locale.US)
    }

    private fun cropBitmap(bitmap: Bitmap, crop: CropAdjustments): Bitmap {
        val safeLeft = crop.left.coerceIn(0f, 0.3f)
        val safeTop = crop.top.coerceIn(0f, 0.3f)
        val safeRight = crop.right.coerceIn(0f, 0.3f)
        val safeBottom = crop.bottom.coerceIn(0f, 0.3f)
        if (safeLeft + safeRight >= 0.9f || safeTop + safeBottom >= 0.9f) return bitmap
        if (safeLeft == 0f && safeTop == 0f && safeRight == 0f && safeBottom == 0f) return bitmap
        val leftPx = (bitmap.width * safeLeft).toInt()
        val topPx = (bitmap.height * safeTop).toInt()
        val rightPx = (bitmap.width * safeRight).toInt()
        val bottomPx = (bitmap.height * safeBottom).toInt()
        val croppedWidth = (bitmap.width - leftPx - rightPx).coerceAtLeast(1)
        val croppedHeight = (bitmap.height - topPx - bottomPx).coerceAtLeast(1)
        return Bitmap.createBitmap(bitmap, leftPx, topPx, croppedWidth, croppedHeight)
    }

    private fun enhanceBitmap(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val gray = ((r + g + b) / 3f)
                val contrasted = if (gray > 150f) 255 else (gray * 0.72f).toInt().coerceIn(0, 255)
                result.setPixel(x, y, Color.rgb(contrasted, contrasted, contrasted))
            }
        }
        return result
    }
}
