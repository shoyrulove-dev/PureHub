package com.purehub.app.feature.docpdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

data class NormalizedPoint(
    val x: Float,
    val y: Float,
)

data class DocumentCorners(
    val topLeft: NormalizedPoint,
    val topRight: NormalizedPoint,
    val bottomRight: NormalizedPoint,
    val bottomLeft: NormalizedPoint,
) {
    companion object {
        fun fullFrame(inset: Float = 0f): DocumentCorners {
            val safe = inset.coerceIn(0f, 0.25f)
            return DocumentCorners(
                topLeft = NormalizedPoint(safe, safe),
                topRight = NormalizedPoint(1f - safe, safe),
                bottomRight = NormalizedPoint(1f - safe, 1f - safe),
                bottomLeft = NormalizedPoint(safe, 1f - safe),
            )
        }

        fun fromCrop(crop: CropAdjustments): DocumentCorners = DocumentCorners(
            topLeft = NormalizedPoint(crop.left, crop.top),
            topRight = NormalizedPoint(1f - crop.right, crop.top),
            bottomRight = NormalizedPoint(1f - crop.right, 1f - crop.bottom),
            bottomLeft = NormalizedPoint(crop.left, 1f - crop.bottom),
        ).sanitized()
    }

    fun sanitized(): DocumentCorners = DocumentCorners(
        topLeft = NormalizedPoint(topLeft.x.coerceIn(0f, 0.48f), topLeft.y.coerceIn(0f, 0.48f)),
        topRight = NormalizedPoint(topRight.x.coerceIn(0.52f, 1f), topRight.y.coerceIn(0f, 0.48f)),
        bottomRight = NormalizedPoint(bottomRight.x.coerceIn(0.52f, 1f), bottomRight.y.coerceIn(0.52f, 1f)),
        bottomLeft = NormalizedPoint(bottomLeft.x.coerceIn(0f, 0.48f), bottomLeft.y.coerceIn(0.52f, 1f)),
    )
}

/** Corrects a photographed page without uploading it or requiring a native image library. */
object DocumentPerspectiveCorrector {
    fun correct(bitmap: Bitmap, corners: DocumentCorners): Bitmap {
        val safe = corners.sanitized()
        val points = listOf(safe.topLeft, safe.topRight, safe.bottomRight, safe.bottomLeft)
            .map { point -> NormalizedPoint(point.x * bitmap.width, point.y * bitmap.height) }
        val (outputWidth, outputHeight) = outputSize(bitmap.width, bitmap.height, safe)
        val source = floatArrayOf(
            points[0].x, points[0].y,
            points[1].x, points[1].y,
            points[2].x, points[2].y,
            points[3].x, points[3].y,
        )
        val destination = floatArrayOf(
            0f, 0f,
            outputWidth.toFloat(), 0f,
            outputWidth.toFloat(), outputHeight.toFloat(),
            0f, outputHeight.toFloat(),
        )
        val transform = Matrix()
        if (!transform.setPolyToPoly(source, 0, destination, 0, 4)) return bitmap
        return Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888).also { output ->
            Canvas(output).drawBitmap(bitmap, transform, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        }
    }

    internal fun outputSize(width: Int, height: Int, corners: DocumentCorners): Pair<Int, Int> {
        val safe = corners.sanitized()
        val points = listOf(safe.topLeft, safe.topRight, safe.bottomRight, safe.bottomLeft)
            .map { point -> NormalizedPoint(point.x * width, point.y * height) }
        val outputWidth = max(distance(points[0], points[1]), distance(points[3], points[2]))
            .roundToInt().coerceIn(1, width * 2)
        val outputHeight = max(distance(points[0], points[3]), distance(points[1], points[2]))
            .roundToInt().coerceIn(1, height * 2)
        return outputWidth to outputHeight
    }

    private fun distance(first: NormalizedPoint, second: NormalizedPoint): Float =
        hypot(second.x - first.x, second.y - first.y)
}
