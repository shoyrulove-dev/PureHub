package com.purehub.app.feature.qr

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.BarcodeFormat

object QrDecoder {
    fun decode(bitmap: Bitmap): String? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        return decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(width, height, pixels))))
    }

    fun decode(imageProxy: ImageProxy): String? {
        val plane = imageProxy.planes.firstOrNull() ?: return null
        val width = imageProxy.width
        val height = imageProxy.height
        val luminance = ByteArray(width * height)
        val buffer = plane.buffer.duplicate()
        for (row in 0 until height) {
            val rowOffset = row * plane.rowStride
            for (column in 0 until width) {
                luminance[row * width + column] = buffer.get(rowOffset + column * plane.pixelStride)
            }
        }
        val source = PlanarYUVLuminanceSource(luminance, width, height, 0, 0, width, height, false)
        return decode(BinaryBitmap(HybridBinarizer(source)))
    }

    private fun decode(bitmap: BinaryBitmap): String? = try {
        MultiFormatReader().apply {
            setHints(
                mapOf(
                    DecodeHintType.TRY_HARDER to true,
                    DecodeHintType.POSSIBLE_FORMATS to BarcodeFormat.values().toList(),
                ),
            )
        }.decodeWithState(bitmap).text?.takeIf(String::isNotBlank)
    } catch (_: NotFoundException) {
        null
    }
}
