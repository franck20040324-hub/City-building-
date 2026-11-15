package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class SoundFxEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @Volatile
    var volume: Float = 0.8f

    fun playBuildChime() = playToneSequence(listOf(523.25 to 60, 659.25 to 80))
    fun playCoinChime() = playToneSequence(listOf(880.0 to 50, 1174.66 to 90))
    fun playUpgradeFanfare() = playToneSequence(listOf(523.25 to 65, 659.25 to 65, 783.99 to 110))
    fun playRefundTone() = playToneSequence(listOf(659.25 to 60, 493.88 to 90))
    fun playWaterSplash() = playToneSequence(listOf(329.63 to 55, 261.63 to 70))

    private fun playToneSequence(notes: List<Pair<Double, Int>>) {
        val vol = volume
        if (vol <= 0.02f) return

        scope.launch {
            try {
                val sampleRate = 22050
                val totalSamples = notes.sumOf { (sampleRate * it.second) / 1000 }
                val buffer = ShortArray(totalSamples)
                var offset = 0
                for ((freq, ms) in notes) {
                    val count = (sampleRate * ms) / 1000
                    for (i in 0 until count) {
                        val env = 1f - (i.toFloat() / count.toFloat())
                        val sample = sin(2.0 * PI * freq * i / sampleRate) * env * vol * 0.25
                        buffer[offset + i] = (sample * Short.MAX_VALUE).toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }
                    offset += count
                }

                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufBytes = maxOf(minBuf, buffer.size * 2)

                @Suppress("DEPRECATION")
                val track = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    AudioTrack.Builder()
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
                        .setBufferSizeInBytes(bufBytes)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build()
                } else {
                    AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufBytes,
                        AudioTrack.MODE_STATIC
                    )
                }

                track.write(buffer, 0, buffer.size)
                track.play()
                Thread.sleep((notes.sumOf { it.second } + 60).toLong())
                track.stop()
                track.release()
            } catch (_: Exception) {
            }
        }
    }
}
