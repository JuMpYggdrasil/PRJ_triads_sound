package com.triadssound.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

class PianoEngine {

    private val sampleRate = 44100
    private val activeNotes = ConcurrentHashMap<Int, ActiveNote>()
    private var audioTrack: AudioTrack? = null
    private var audioJob: Job? = null
    @Volatile
    private var running = false
    private var engineFrame = 0L

    private data class ActiveNote(
        val midiNumber: Int,
        val startFrame: Long,
        val velocity: Double
    )

    fun start() {
        if (audioTrack != null) return

        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize.coerceAtLeast(4096))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()
        running = true

        audioJob = CoroutineScope(Dispatchers.IO + CoroutineName("audio-engine")).launch {
            generateLoop()
        }
    }

    fun noteOn(midiNumber: Int, velocity: Double = 0.8) {
        activeNotes[midiNumber] = ActiveNote(midiNumber, engineFrame, velocity)
    }

    fun noteOff(midiNumber: Int) {
        activeNotes.remove(midiNumber)
    }

    fun releaseAll() {
        activeNotes.clear()
    }

    private fun generateLoop() {
        val buffer = ShortArray(256)

        while (running && audioTrack != null) {
            for (i in buffer.indices) {
                var sample = 0.0
                val absoluteFrame = engineFrame++
                val notes = activeNotes.values.toList()

                for (note in notes) {
                    val noteFrame = absoluteFrame - note.startFrame
                    sample += generatePiano(note, noteFrame)
                }

                sample = sample.coerceIn(-1.0, 1.0)
                buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
            }
            audioTrack?.write(buffer, 0, buffer.size)
        }
    }

    private fun generatePiano(note: ActiveNote, frame: Long): Double {
        val t = frame / sampleRate.toDouble()
        if (t > 5.0 || t < 0.0) return 0.0

        val freq = 440.0 * 2.0.pow((note.midiNumber - 69.0) / 12.0)

        val env = when {
            t < 0.008 -> t / 0.008
            t < 0.06  -> 1.0 - 0.25 * (t - 0.008) / 0.052
            t < 0.5   -> 0.75 - 0.25 * (t - 0.06) / 0.44
            else      -> 0.5 * exp(-1.2 * (t - 0.5))
        }

        if (env <= 0.001) return 0.0

        var sample = 0.0
        for (h in 1..8) {
            val amp = 1.0 / h
            val decay = exp(-2.5 * (h - 1) * t)
            sample += amp * decay * sin(2.0 * PI * freq * h * t)
        }

        return sample * env * note.velocity * 0.4
    }

    fun stop() {
        running = false
        audioJob?.cancel()
        audioJob = null
        try {
            audioTrack?.stop()
        } catch (_: IllegalStateException) {}
        audioTrack?.release()
        audioTrack = null
        activeNotes.clear()
        engineFrame = 0
    }
}
