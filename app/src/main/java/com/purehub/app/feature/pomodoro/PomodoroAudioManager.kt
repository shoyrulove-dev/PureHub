package com.purehub.app.feature.pomodoro

import android.animation.ValueAnimator
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.MediaPlayer
import android.media.AudioTrack
import androidx.annotation.RawRes
import androidx.core.animation.doOnEnd
import com.purehub.app.R
import kotlin.random.Random

class PomodoroAudioManager(
    private val context: Context,
) {
    private var mediaPlayer: MediaPlayer? = null
    private var audioTrack: AudioTrack? = null
    private var currentSoundscape: String? = null
    private var currentVolume = 0.35f
    private var volumeAnimator: ValueAnimator? = null

    fun play(soundscape: String, targetVolume: Float = currentVolume) {
        val alreadyPlaying = mediaPlayer?.isPlaying == true || audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING
        if (currentSoundscape == soundscape && alreadyPlaying) {
            setVolume(targetVolume)
            return
        }

        stop()
        currentSoundscape = soundscape
        currentVolume = 0f
        if (soundscape == "White Noise") {
            audioTrack = createWhiteNoiseTrack()?.apply {
                setVolume(0f)
                play()
            }
        } else {
            val resourceId = soundscapeResource(soundscape) ?: return
            mediaPlayer = MediaPlayer.create(context, resourceId)?.apply {
                isLooping = true
                setVolume(0f, 0f)
                start()
            }
        }
        fadeTo(targetVolume, 700L)
    }

    fun setVolume(volume: Float) {
        currentVolume = volume.coerceIn(0f, 1f)
        mediaPlayer?.setVolume(currentVolume, currentVolume)
        audioTrack?.setVolume(currentVolume)
    }

    fun fadeTo(targetVolume: Float, durationMs: Long) {
        if (mediaPlayer == null && audioTrack == null) return
        volumeAnimator?.cancel()
        val start = currentVolume
        val end = targetVolume.coerceIn(0f, 1f)
        volumeAnimator = ValueAnimator.ofFloat(start, end).apply {
            duration = durationMs
            addUpdateListener { animator ->
                val value = animator.animatedValue as Float
                currentVolume = value
                mediaPlayer?.setVolume(value, value)
                audioTrack?.setVolume(value)
            }
            start()
        }
    }

    fun fadeOutAndStop(durationMs: Long = 500L) {
        if (mediaPlayer == null && audioTrack == null) return
        volumeAnimator?.cancel()
        val start = currentVolume
        volumeAnimator = ValueAnimator.ofFloat(start, 0f).apply {
            duration = durationMs
            addUpdateListener { animator ->
                val value = animator.animatedValue as Float
                currentVolume = value
                mediaPlayer?.setVolume(value, value)
                audioTrack?.setVolume(value)
            }
            doOnEnd {
                stop()
                currentVolume = start.coerceAtLeast(0.2f)
            }
            start()
        }
    }

    fun stop() {
        volumeAnimator?.cancel()
        volumeAnimator = null
        mediaPlayer?.run {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        audioTrack?.run {
            runCatching { stop() }
            release()
        }
        audioTrack = null
        currentSoundscape = null
    }

    @RawRes
    private fun soundscapeResource(label: String): Int? {
        return when (label) {
            "Soft Rain" -> R.raw.rain_loop
            "Brown Noise" -> R.raw.brown_noise_loop
            else -> null
        }
    }

    private fun createWhiteNoiseTrack(): AudioTrack? {
        val sampleRate = 22_050
        val frameCount = sampleRate * 2
        val samples = ShortArray(frameCount) {
            Random.nextInt(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(samples.size * Short.SIZE_BYTES)
                .build()
                .also { track ->
                    track.write(samples, 0, samples.size)
                    track.setLoopPoints(0, frameCount, -1)
                }
        }.getOrNull()
    }
}
