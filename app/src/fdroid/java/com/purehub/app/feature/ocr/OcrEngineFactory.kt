package com.purehub.app.feature.ocr

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

object OcrEngineFactory {
    fun create(context: Context, script: OcrScript): OcrEngine = TesseractOcrEngine(context.applicationContext, script)
}

private class TesseractOcrEngine(
    private val context: Context,
    private val script: OcrScript,
) : OcrEngine {
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val closed = AtomicBoolean(false)

    override fun recognize(bitmap: Bitmap, onResult: (Result<String>) -> Unit) {
        executor.execute {
            val result = runCatching {
                check(!closed.get()) { "OCR engine is closed." }
                val language = when (script) {
                    OcrScript.LATIN -> "eng+vie"
                    OcrScript.CHINESE -> "chi_sim"
                }
                val dataRoot = prepareLanguageData(language)
                val api = TessBaseAPI()
                try {
                    check(api.init(dataRoot.absolutePath, language)) { "Tesseract language data could not be initialized." }
                    api.setImage(bitmap)
                    api.utF8Text.orEmpty()
                } finally {
                    api.recycle()
                }
            }
            if (!closed.get()) mainHandler.post { onResult(result) }
        }
    }

    private fun prepareLanguageData(language: String): File {
        val root = File(context.filesDir, "tesseract")
        val tessdata = File(root, "tessdata").apply { mkdirs() }
        language.split('+').forEach { code ->
            val target = File(tessdata, "$code.traineddata")
            if (!target.exists() || target.length() == 0L) {
                context.assets.open("tessdata/$code.traineddata").use { input ->
                    target.outputStream().use(input::copyTo)
                }
            }
        }
        return root
    }

    override fun close() {
        closed.set(true)
        executor.shutdownNow()
    }
}
