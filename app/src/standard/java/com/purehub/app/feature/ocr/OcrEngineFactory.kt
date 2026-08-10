package com.purehub.app.feature.ocr

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

object OcrEngineFactory {
    fun create(context: Context, script: OcrScript): OcrEngine = MlKitOcrEngine(script)
}

private class MlKitOcrEngine(script: OcrScript) : OcrEngine {
    private val recognizer: TextRecognizer = when (script) {
        OcrScript.LATIN -> TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        OcrScript.CHINESE -> TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }

    override fun recognize(bitmap: Bitmap, onResult: (Result<String>) -> Unit) {
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { onResult(Result.success(it.text)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    override fun close() = recognizer.close()
}
