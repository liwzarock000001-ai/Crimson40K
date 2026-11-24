package com.example.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class SoundType {
    CLICK,
    BOLTER,
    PLASMA,
    MELEE,
    SURGE,
    TURN_HORN,
    VICTORY,
    WARP_BEACON,
    WARP_STRIKE
}

class SoundEffects {
    var enabled: Boolean = true
    var volume: Float = 0.8f
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun play(type: SoundType) {
        if (!enabled || volume <= 0.01f) return
        val vol = volume.coerceIn(0f, 1f)
        scope.launch {
            runCatching {
                val sampleRate = 22050
                val samples = when (type) {
                    SoundType.CLICK -> synthTone(sampleRate, 0.04f, 880f, 620f, 0.25f * vol)
                    SoundType.BOLTER -> synthBurst(sampleRate, 0.16f, 220f, vol)
                    SoundType.PLASMA -> synthTone(sampleRate, 0.22f, 920f, 240f, 0.45f * vol)
                    SoundType.MELEE -> synthTone(sampleRate, 0.14f, 440f, 110f, 0.45f * vol)
                    SoundType.SURGE -> synthTone(sampleRate, 0.30f, 320f, 980f, 0.42f * vol)
                    SoundType.TURN_HORN -> synthTone(sampleRate, 0.26f, 220f, 330f, 0.35f * vol)
                    SoundType.VICTORY -> synthTone(sampleRate, 0.40f, 440f, 880f, 0.40f * vol)
                    SoundType.WARP_BEACON -> synthTone(sampleRate, 0.45f, 180f, 650f, 0.4f * vol)
                    SoundType.WARP_STRIKE -> synthBurst(sampleRate, 0.35f, 140f, vol)
                }
                playPcm(sampleRate, samples)
            }
        }
    }

    private fun synthTone(
        sampleRate: Int,
        durationSec: Float,
        fStart: Float,
        fEnd: Float,
        gain: Float
    ): ShortArray {
        val count = (sampleRate * durationSec).toInt().coerceAtLeast(1)
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toFloat() / count
            val freq = fStart + (fEnd - fStart) * t
            phase += 2.0 * PI * freq / sampleRate
            val env = exp(-3.5f * t)
            val sample = (sin(phase) * gain * env * Short.MAX_VALUE).toInt()
            out[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun synthBurst(sampleRate: Int, durationSec: Float, baseFreq: Float, vol: Float): ShortArray {
        val count = (sampleRate * durationSec).toInt().coerceAtLeast(1)
        val out = ShortArray(count)
        val rng = Random(42)
        for (i in 0 until count) {
            val t = i.toFloat() / count
            val pulse = ((t * 3f) % 1f)
            val env = exp(-5f * pulse)
            val tone = sin(2.0 * PI * baseFreq * (1.0 - 0.3 * pulse) * t).toFloat()
            val noise = (rng.nextFloat() * 2f - 1f) * 0.45f
            val sample = ((tone * 0.65f + noise) * 0.4f * vol * env * Short.MAX_VALUE).toInt()
            out[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun playPcm(sampleRate: Int, pcm: ShortArray) {
        val bufferBytes = pcm.size * 2
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferBytes)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(pcm, 0, pcm.size)
        track.play()
        Thread.sleep((pcm.size * 1000L / sampleRate) + 40L)
        track.stop()
        track.release()
    }
}
