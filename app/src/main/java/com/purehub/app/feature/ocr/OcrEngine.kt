package com.purehub.app.feature.ocr

import android.graphics.Bitmap
import java.io.Closeable

enum class OcrScript { LATIN, CHINESE }

interface OcrEngine : Closeable {
    fun recognize(bitmap: Bitmap, onResult: (Result<String>) -> Unit)
}
