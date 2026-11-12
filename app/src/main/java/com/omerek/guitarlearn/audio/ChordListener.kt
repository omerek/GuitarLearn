package com.omerek.guitarlearn.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.concurrent.thread

/**
 * Mikrofonu dinler; her tele vuruşta kromayı [onStrum] ile bildirir (ses iş parçacığından çağrılır).
 * RECORD_AUDIO izni alındıktan sonra start() çağrılmalıdır.
 */
class ChordListener(private val onStrum: (FloatArray) -> Unit) {
    private val sampleRate = 44100
    @Volatile private var running = false
    private var worker: Thread? = null

    @SuppressLint("MissingPermission")
    fun start() {
        if (running) return
        running = true
        worker = thread(name = "chord-listener", isDaemon = true) {
            val minBuf = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val record = try {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuf, 8192) * 2,
                )
            } catch (e: Exception) {
                running = false
                return@thread
            }
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release(); running = false; return@thread
            }
            val analyzer = StrumAnalyzer(sampleRate) { if (running) onStrum(it) }
            val shorts = ShortArray(1024)
            val floats = FloatArray(1024)
            try {
                record.startRecording()
                while (running) {
                    val n = record.read(shorts, 0, shorts.size)
                    if (n <= 0) continue
                    for (i in 0 until n) floats[i] = shorts[i] / 32768f
                    analyzer.feed(floats, n)
                }
            } finally {
                try { record.stop() } catch (_: Exception) {}
                record.release()
            }
        }
    }

    fun stop() {
        running = false
        worker?.join(500)
        worker = null
    }
}
