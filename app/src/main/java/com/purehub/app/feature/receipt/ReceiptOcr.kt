package com.purehub.app.feature.receipt

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

fun recognizeReceipt(
    context: Context,
    uri: Uri,
    onResult: (Result<ReceiptResult>) -> Unit,
) {
    val bitmap = runCatching {
        context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
            ?: error("Receipt image could not be opened.")
    }.getOrElse { error -> onResult(Result.failure(error)); return }
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    recognizer.process(InputImage.fromBitmap(bitmap, 0))
        .addOnSuccessListener { text -> onResult(Result.success(ReceiptParser.parse(text.text))) }
        .addOnFailureListener { onResult(Result.failure(it)) }
        .addOnCompleteListener { recognizer.close(); bitmap.recycle() }
}
