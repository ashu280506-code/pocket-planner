package com.ashu.pocketplanner.fx

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** Tiny synthesized sounds, so the app ships no audio files. */
object Sound {
    private const val SR = 44100
    private var popTrack: AudioTrack? = null
    private var chimeTrack: AudioTrack? = null

    private fun make(pcm: ShortArray): AudioTrack {
        val t = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SR)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(pcm.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        t.write(pcm, 0, pcm.size)
        return t
    }

    /** A short bubbly "pop": a fast downward pitch sweep with a quick decay. */
    private fun popPcm(): ShortArray {
        val n = (SR * 0.16).toInt()
        val out = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            val f = 1250 * exp(-t * 22) + 240
            phase += 2 * PI * f / SR
            val env = exp(-t * 30) * (1 - exp(-t * 1200))
            out[i] = (sin(phase) * env * 0.55 * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    /** A rising four-note sparkle for finishing the whole day. */
    private fun chimePcm(): ShortArray {
        val notes = doubleArrayOf(784.0, 988.0, 1175.0, 1568.0)
        val step = 0.09
        val n = (SR * (step * notes.size + 0.5)).toInt()
        val mix = DoubleArray(n)
        notes.forEachIndexed { k, f ->
            val start = (SR * step * k).toInt()
            for (i in start until n) {
                val t = (i - start).toDouble() / SR
                val env = exp(-t * 7) * (1 - exp(-t * 900))
                mix[i] += sin(2 * PI * f * t) * env * 0.22 + sin(2 * PI * f * 2 * t) * env * 0.06
            }
        }
        return ShortArray(n) { (mix[it].coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort() }
    }

    private fun replay(t: AudioTrack) {
        runCatching { t.stop() }
        runCatching {
            t.reloadStaticData()
            t.play()
        }
    }

    @Synchronized
    fun pop() {
        runCatching { replay(popTrack ?: make(popPcm()).also { popTrack = it }) }
    }

    @Synchronized
    fun chime() {
        runCatching { replay(chimeTrack ?: make(chimePcm()).also { chimeTrack = it }) }
    }
}
