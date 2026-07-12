package com.purehub.app.feature.pomodoro

import android.animation.ValueAnimator
import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.RawRes
import androidx.core.animation.doOnEnd
import com.purehub.app.R

class PomodoroAudioManager(
    private val context: Context,
) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentVolume = 0.35f
    private var volumeAnimator: ValueAnimator? = null

    fun play(soundscape: String, targetVolume: Float = currentVolume) {
        val resourceId = soundscapeResource(soundscape) ?: return
        if (mediaPlayer != null && mediaPlayer?.isPlaying == true) {
            setVolume(targetVolume)
            return
        }

        stop()
        mediaPlayer = MediaPlayer.create(context, resourceId)?.apply {
            isLooping = true
            setVolume(0f, 0f)
            start()
        }
        fadeTo(targetVolume, 700L)
    }

    fun setVolume(volume: Float) {
        currentVolume = volume.coerceIn(0f, 1f)
        mediaPlayer?.setVolume(currentVolume, currentVolume)
    }

    fun fadeTo(targetVolume: Float, durationMs: Long) {
        val player = mediaPlayer ?: return
        volumeAnimator?.cancel()
        val start = currentVolume
        val end = targetVolume.coerceIn(0f, 1f)
        volumeAnimator = ValueAnimator.ofFloat(start, end).apply {
            duration = durationMs
            addUpdateListener { animator ->
                val value = animator.animatedValue as Float
                currentVolume = value
                player.setVolume(value, value)
            }
            start()
        }
    }

    fun fadeOutAndStop(durationMs: Long = 500L) {
        val player = mediaPlayer ?: return
        volumeAnimator?.cancel()
        val start = currentVolume
        volumeAnimator = ValueAnimator.ofFloat(start, 0f).apply {
            duration = durationMs
            addUpdateListener { animator ->
                val value = animator.animatedValue as Float
                currentVolume = value
                player.setVolume(value, value)
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
    }

    @RawRes
    private fun soundscapeResource(label: String): Int? {
        return when (label) {
            "Rain" -> R.raw.rain_loop
            "Cafe" -> R.raw.cafe_loop
            "Brown Noise" -> R.raw.brown_noise_loop
            else -> null
        }
    }
}
