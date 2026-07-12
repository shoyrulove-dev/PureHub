package com.purehub.app.feature.speakercleaner

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

class SpeakerCleanerAudioManager {
    private var audioTrack: AudioTrack? = null

    fun play(
        frequencyHz: Double = 165.0,
        volume: Float = 0.7f,
    ) {
        if (audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) return

        val sampleRate = 44_100
        val durationSeconds = 2
        val sampleCount = sampleRate * durationSeconds
        val samples = ShortArray(sampleCount)

        for (index in 0 until sampleCount) {
            val angle = 2.0 * PI * index * frequencyHz / sampleRate
            samples[index] = (sin(angle) * Short.MAX_VALUE * volume).toInt().toShort()
        }

        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val bufferSize = maxOf(minBuffer, samples.size * 2)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
            .apply {
                write(samples, 0, samples.size)
                setLoopPoints(0, samples.size, -1)
                play()
            }
    }

    fun stop() {
        audioTrack?.run {
            pause()
            flush()
            release()
        }
        audioTrack = null
    }
}
