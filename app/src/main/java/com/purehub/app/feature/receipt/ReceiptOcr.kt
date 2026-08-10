package com.purehub.app.feature.receipt

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.purehub.app.feature.ocr.OcrEngineFactory
import com.purehub.app.feature.ocr.OcrScript

fun recognizeReceipt(
    context: Context,
    uri: Uri,
    onResult: (Result<ReceiptResult>) -> Unit,
) {
    val bitmap = runCatching {
        context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
            ?: error("Receipt image could not be opened.")
    }.getOrElse { error -> onResult(Result.failure(error)); return }
    val recognizer = OcrEngineFactory.create(context.applicationContext, OcrScript.LATIN)
    recognizer.recognize(bitmap) { result ->
        onResult(result.map(ReceiptParser::parse))
        recognizer.close()
        bitmap.recycle()
    }
}
