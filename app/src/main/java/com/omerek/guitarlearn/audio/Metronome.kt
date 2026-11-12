package com.omerek.guitarlearn.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.concurrent.thread

data class MetronomeState(
    val running: Boolean = false,
    val bpm: Int = 70,
    val beatsPerBar: Int = 4,
    /** Çalan vuruş (0'dan başlar), durunca -1. */
    val beat: Int = -1,
    /** Metronomu kimin başlattığı (ör. "song:uskudar"), başka ekranlar ayırt etsin diye. */
    val owner: String = "",
)

/**
 * Örnek hassasiyetinde metronom: tık sesleri ve aradaki sessizlik tek bir ses akışına yazılır,
 * böylece zamanlama telefonun yüküne bağlı olarak kaymaz. Aynı anda tek metronom çalışır.
 */
object Metronome {
    const val MIN_BPM = 40
    const val MAX_BPM = 220

    private val _state = MutableStateFlow(MetronomeState())
    val state: StateFlow<MetronomeState> = _state.asStateFlow()

    private val accent = Synth.toPcm16(Synth.click(1600.0).also { Synth.normalize(it, 0.9f) })
    private val normal = Synth.toPcm16(Synth.click(1000.0).also { Synth.normalize(it, 0.6f) })

    @Volatile private var session = 0

    fun setBpm(bpm: Int) {
        _state.value = _state.value.copy(bpm = bpm.coerceIn(MIN_BPM, MAX_BPM))
    }

    fun setBeatsPerBar(beats: Int) {
        _state.value = _state.value.copy(beatsPerBar = beats.coerceIn(1, 12))
    }

    fun toggle(owner: String = "") {
        if (_state.value.running && _state.value.owner == owner) stop() else start(owner = owner)
    }

    fun start(bpm: Int = _state.value.bpm, beatsPerBar: Int = _state.value.beatsPerBar, owner: String = "") {
        stop()
        val mySession = ++session
        _state.value = MetronomeState(true, bpm.coerceIn(MIN_BPM, MAX_BPM), beatsPerBar, -1, owner)
        thread(name = "metronome", isDaemon = true) { loop(mySession) }
    }

    fun stop() {
        session++
        _state.value = _state.value.copy(running = false, beat = -1, owner = "")
    }

    private fun loop(mySession: Int) {
        val rate = Synth.SAMPLE_RATE
        val minBuf = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(rate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(minBuf)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        try {
            track.play()
            var beat = 0
            while (session == mySession) {
                val s = _state.value
                val samplesPerBeat = rate * 60 / s.bpm
                val beatInBar = beat % s.beatsPerBar
                _state.value = s.copy(beat = beatInBar)
                val click = if (beatInBar == 0 && s.beatsPerBar > 1) accent else normal
                val buffer = ShortArray(samplesPerBeat)
                System.arraycopy(click, 0, buffer, 0, minOf(click.size, buffer.size))
                var offset = 0
                while (offset < buffer.size && session == mySession) {
                    val n = track.write(buffer, offset, minOf(1024, buffer.size - offset))
                    if (n <= 0) break
                    offset += n
                }
                beat++
            }
        } finally {
            try { track.pause(); track.flush(); track.stop() } catch (_: Exception) {}
            track.release()
        }
    }
}
