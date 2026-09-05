package com.purehub.app.feature.ocr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

/** Keeps retained OCR previews compact while recognition can still use full ARGB input. */
object OcrBitmapMemory {
    fun compactForRetention(source: Bitmap): Bitmap {
        if (source.config == Bitmap.Config.RGB_565) return source
        return Bitmap.createBitmap(source.width, source.height, Bitmap.Config.RGB_565).also { compact ->
            Canvas(compact).drawBitmap(
                source,
                0f,
                0f,
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
            )
        }
    }

    fun recycle(bitmap: Bitmap?) {
        bitmap?.takeIf { !it.isRecycled }?.recycle()
    }
}
