package com.purehub.app.feature.decibel

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.math.log10
import kotlin.math.sqrt

class DecibelMeterManager {
    fun levels(): Flow<Float> = flow {
        val sampleRate = 44_100
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val bufferSize = (minBuffer * 2).coerceAtLeast(4_096)
        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
        )
        val buffer = ShortArray(bufferSize / 2)

        try {
            record.startRecording()
            while (true) {
                val read = record.read(buffer, 0, buffer.size)
                if (read <= 0) {
                    delay(80)
                    continue
                }
                var sumSquares = 0.0
                for (index in 0 until read) {
                    val sample = buffer[index].toDouble()
                    sumSquares += sample * sample
                }
                val rms = sqrt(sumSquares / read).coerceAtLeast(1.0)
                val decibel = (20 * log10(rms / 32767.0) + 90).toFloat().coerceIn(0f, 120f)
                emit(decibel)
                delay(80)
            }
        } finally {
            runCatching { record.stop() }
            record.release()
        }
    }.flowOn(Dispatchers.IO)
}
