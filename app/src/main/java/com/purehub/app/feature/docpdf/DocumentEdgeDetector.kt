package com.purehub.app.feature.docpdf

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class DocumentFrame(
    val crop: CropAdjustments,
    val confidence: Float,
)

/**
 * Lightweight, offline document-frame detector. It looks for sustained luminance changes
 * relative to the image border and returns conservative insets. A low-confidence result never
 * crops the page, so callers can safely fall back to manual controls.
 */
object DocumentEdgeDetector {
    fun detect(bitmap: Bitmap): DocumentFrame {
        val sampleWidth = min(bitmap.width, 420)
        val sampleHeight = max(1, (bitmap.height * (sampleWidth.toFloat() / bitmap.width)).toInt())
            .coerceAtMost(560)
        val sample = if (sampleWidth == bitmap.width && sampleHeight == bitmap.height) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, sampleWidth, sampleHeight, true)
        }
        return try {
            val pixels = IntArray(sample.width * sample.height)
            sample.getPixels(pixels, 0, sample.width, 0, 0, sample.width, sample.height)
            detect(pixels, sample.width, sample.height)
        } finally {
            if (sample !== bitmap) sample.recycle()
        }
    }

    internal fun detect(pixels: IntArray, width: Int, height: Int): DocumentFrame {
        if (width < 24 || height < 24 || pixels.size < width * height) return emptyFrame()
        val border = max(2, min(width, height) / 40)
        var borderTotal = 0f
        var borderCount = 0
        fun addPixel(x: Int, y: Int) {
            borderTotal += luminance(pixels[y * width + x])
            borderCount += 1
        }
        for (x in 0 until width) {
            for (y in 0 until border) addPixel(x, y)
            for (y in height - border until height) addPixel(x, y)
        }
        for (y in border until height - border) {
            for (x in 0 until border) addPixel(x, y)
            for (x in width - border until width) addPixel(x, y)
        }
        val background = borderTotal.toFloat() / borderCount.coerceAtLeast(1)
        val threshold = 30f
        val rowScores = FloatArray(height)
        val columnScores = FloatArray(width)
        val step = max(1, min(width, height) / 220)
        for (y in 0 until height step step) {
            var changed = 0
            var samples = 0
            for (x in 0 until width step step) {
                if (abs(luminance(pixels[y * width + x]) - background) >= threshold) changed += 1
                samples += 1
            }
            rowScores[y] = changed.toFloat() / samples.coerceAtLeast(1)
        }
        for (x in 0 until width step step) {
            var changed = 0
            var samples = 0
            for (y in 0 until height step step) {
                if (abs(luminance(pixels[y * width + x]) - background) >= threshold) changed += 1
                samples += 1
            }
            columnScores[x] = changed.toFloat() / samples.coerceAtLeast(1)
        }
        val minCoverage = 0.18f
        val top = firstSustained(rowScores, minCoverage, step)
        val bottom = lastSustained(rowScores, minCoverage, step)
        val left = firstSustained(columnScores, minCoverage, step)
        val right = lastSustained(columnScores, minCoverage, step)
        if (left < 0 || top < 0 || right <= left || bottom <= top) return emptyFrame()

        val contentWidth = (right - left + 1).toFloat() / width
        val contentHeight = (bottom - top + 1).toFloat() / height
        if (contentWidth < 0.35f || contentHeight < 0.35f) return emptyFrame()
        val paddingX = width * 0.012f
        val paddingY = height * 0.012f
        val crop = CropAdjustments(
            left = ((left - paddingX) / width).coerceIn(0f, 0.28f),
            top = ((top - paddingY) / height).coerceIn(0f, 0.28f),
            right = ((width - 1 - right - paddingX) / width).coerceIn(0f, 0.28f),
            bottom = ((height - 1 - bottom - paddingY) / height).coerceIn(0f, 0.28f),
        )
        val edgeInset = listOf(crop.left, crop.top, crop.right, crop.bottom).count { it > 0.015f }
        val confidence = ((contentWidth * contentHeight) * 0.7f + edgeInset / 4f * 0.3f).coerceIn(0f, 1f)
        return if (confidence >= 0.42f) DocumentFrame(crop, confidence) else emptyFrame()
    }

    private fun firstSustained(scores: FloatArray, threshold: Float, step: Int): Int {
        var hits = 0
        for (index in scores.indices step step) {
            hits = if (scores[index] >= threshold) hits + 1 else 0
            if (hits >= 2) return (index - step).coerceAtLeast(0)
        }
        return -1
    }

    private fun lastSustained(scores: FloatArray, threshold: Float, step: Int): Int {
        var hits = 0
        var index = scores.lastIndex
        while (index >= 0) {
            hits = if (scores[index] >= threshold) hits + 1 else 0
            if (hits >= 2) return (index + step).coerceAtMost(scores.lastIndex)
            index -= step
        }
        return -1
    }

    private fun luminance(color: Int): Float {
        val red = color ushr 16 and 0xFF
        val green = color ushr 8 and 0xFF
        val blue = color and 0xFF
        return red * 0.2126f + green * 0.7152f + blue * 0.0722f
    }

    private fun emptyFrame() = DocumentFrame(CropAdjustments(), 0f)
}
