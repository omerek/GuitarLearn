package com.omerek.guitarlearn.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.concurrent.thread

/** Akort edicinin anlık durumu. frequency <= 0 ise ses algılanmıyor. */
data class TunerReading(val frequency: Float = -1f, val level: Float = 0f)

/**
 * Mikrofonu dinler ve perdeyi YIN ile hesaplar. start()/stop() ekran görünürlüğüne bağlanır.
 * RECORD_AUDIO izni alındıktan sonra çağrılmalıdır.
 */
class TunerEngine {
    private val sampleRate = 44100
    private val windowSize = 4096
    private val hop = 2048
    private val detector = PitchDetector(sampleRate, windowSize, threshold = 0.15f, minFrequency = 60f, maxFrequency = 1000f)

    private val _reading = MutableStateFlow(TunerReading())
    val reading: StateFlow<TunerReading> = _reading.asStateFlow()

    @Volatile private var running = false
    private var worker: Thread? = null

    @SuppressLint("MissingPermission")
    fun start() {
        if (running) return
        running = true
        worker = thread(name = "tuner", isDaemon = true) {
            val minBuf = AudioRecord.getMinBufferSize(
                sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
            )
            val record = try {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuf, windowSize * 2) * 2,
                )
            } catch (e: Exception) {
                running = false
                return@thread
            }
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release(); running = false; return@thread
            }
            val shortBuf = ShortArray(hop)
            val window = FloatArray(windowSize)
            val recent = ArrayDeque<Float>()
            var silentFrames = 0
            try {
                record.startRecording()
                while (running) {
                    var read = 0
                    while (read < hop && running) {
                        val n = record.read(shortBuf, read, hop - read)
                        if (n <= 0) break
                        read += n
                    }
                    if (read < hop) continue
                    // Pencereyi kaydır, yeni örnekleri sona ekle
                    System.arraycopy(window, hop, window, 0, windowSize - hop)
                    for (i in 0 until hop) window[windowSize - hop + i] = shortBuf[i] / 32768f

                    val level = detector.rms(window)
                    val f = if (level > 0.01f) detector.detect(window) else -1f
                    if (f > 0) {
                        silentFrames = 0
                        recent.addLast(f)
                        if (recent.size > 5) recent.removeFirst()
                        val median = recent.sorted()[recent.size / 2]
                        _reading.value = TunerReading(median, level)
                    } else {
                        silentFrames++
                        // Tel söndükten sonra yaklaşık yarım saniye son değeri tut
                        if (silentFrames > 10) {
                            recent.clear()
                            _reading.value = TunerReading(-1f, level)
                        }
                    }
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
        _reading.value = TunerReading()
    }
}
