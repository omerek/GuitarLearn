package com.omerek.guitarlearn.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors

/** Akor, nota ve tab seslerini çalar. Tüm sentez arka planda yapılır. */
object SoundPlayer {
    private val executor = Executors.newSingleThreadExecutor { r -> Thread(r, "sound").apply { isDaemon = true } }
    @Volatile private var generation = 0

    fun playChord(midiNotes: List<Int>) = submit { Synth.strum(midiNotes) }

    fun playNote(frequency: Double, seconds: Double = 2.5) = submit { Synth.pluck(frequency, seconds) }

    /** Tab olaylarını sırayla çalar. Her olay bir vuruş (dörtlük) sürer. */
    fun playSequence(events: List<List<Int>>, bpm: Int) = submit {
        val beat = (Synth.SAMPLE_RATE * 60.0 / bpm).toInt()
        val total = beat * events.size + Synth.SAMPLE_RATE
        val mix = FloatArray(total)
        events.forEachIndexed { i, notes ->
            notes.forEach { midi ->
                val tone = Synth.pluck(Tuning.midiToFrequency(midi), 1.5)
                val start = i * beat
                for (n in tone.indices) {
                    if (start + n >= total) break
                    mix[start + n] += tone[n] * 0.6f
                }
            }
        }
        Synth.normalize(mix, 0.8f)
        mix
    }

    /** Çalan sesi keser. */
    fun stop() {
        generation++
    }

    private fun submit(render: () -> FloatArray) {
        val myGen = ++generation
        executor.execute {
            val pcm = Synth.toPcm16(render())
            if (myGen != generation) return@execute
            play(pcm, myGen)
        }
    }

    private fun play(pcm: ShortArray, myGen: Int) {
        val minBuf = AudioTrack.getMinBufferSize(
            Synth.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
        )
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(Synth.SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(maxOf(minBuf, 8192))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        try {
            track.play()
            var offset = 0
            val chunk = 2048
            // Yeni bir ses istenirse (generation değişirse) bu ses kesilir
            while (offset < pcm.size && myGen == generation) {
                val n = minOf(chunk, pcm.size - offset)
                val written = track.write(pcm, offset, n)
                if (written <= 0) break
                offset += written
            }
            if (myGen == generation) {
                // Tamponda kalan sesin bitmesini bekle
                Thread.sleep((minBuf / 2 * 1000L / Synth.SAMPLE_RATE) + 50)
            }
        } finally {
            try { track.stop() } catch (_: Exception) {}
            track.release()
        }
    }
}
