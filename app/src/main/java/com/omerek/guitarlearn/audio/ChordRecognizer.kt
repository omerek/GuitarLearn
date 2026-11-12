package com.omerek.guitarlearn.audio

import com.omerek.guitarlearn.data.Chord
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Mikrofondan gelen sesle "doğru akor çalındı mı?" sorusunu cevaplar. Saf Kotlin, Android'e bağlı değil.
 *
 * Yöntem: tele vuruş anı (ani ses yükselmesi) yakalanır, hemen ardından gelen ~0,3 saniyelik sesin
 * frekans dağılımı 12 notaya (Do, Do#, ... Si) toplanır (kroma). Bu dağılım her akorun nota şablonuyla
 * karşılaştırılır; beklenen akor en iyi eşleşenlerden biriyse "doğru" sayılır.
 */
object ChordTemplates {
    /**
     * Akorun kroma şablonu. Gerçek tel sesinde üst harmonikler de duyulduğu için her notaya
     * oktav (2. harmonik), beşli (3.) ve büyük üçlü (5.) katkısı da eklenir.
     */
    fun template(chord: Chord): FloatArray = template(chord.midiNotes)

    fun template(midiNotes: List<Int>): FloatArray {
        val t = FloatArray(12)
        for (m in midiNotes) {
            t[m % 12] += 1.0f + 0.5f + 0.25f // 1., 2. ve 4. harmonik aynı nota
            t[(m + 7) % 12] += 0.33f // 3. harmonik
            t[(m + 4) % 12] += 0.15f // 5. harmonik
        }
        return normalize(t)
    }

    fun normalize(v: FloatArray): FloatArray {
        var s = 0f
        for (x in v) s += x * x
        val n = sqrt(s)
        if (n > 0f) for (i in v.indices) v[i] /= n
        return v
    }

    fun cosine(a: FloatArray, b: FloatArray): Float {
        var dot = 0f; var na = 0f; var nb = 0f
        for (i in 0 until 12) { dot += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i] }
        return if (na == 0f || nb == 0f) 0f else dot / sqrt(na * nb)
    }
}

/** Bir vuruşun değerlendirmesi. */
data class ChordMatch(
    val expected: String,
    val expectedScore: Float,
    val best: String,
    val bestScore: Float,
    val ok: Boolean,
)

class ChordMatcher(
    private val candidates: List<Chord>,
    /** Beklenen akorun en az bu kadar benzemesi gerekir (0..1). */
    var minScore: Float = 0.80f,
    /** Beklenen akor en iyi eşleşmeden en fazla bu kadar geride kalabilir. */
    var margin: Float = 0.03f,
) {
    private val templates = candidates.associate { it.name to ChordTemplates.template(it) }

    fun match(chroma: FloatArray, expected: Chord): ChordMatch {
        val expTemplate = templates[expected.name] ?: ChordTemplates.template(expected)
        val expScore = ChordTemplates.cosine(chroma, expTemplate)
        var bestName = expected.name
        var bestScore = expScore
        for ((name, t) in templates) {
            val s = ChordTemplates.cosine(chroma, t)
            if (s > bestScore) { bestScore = s; bestName = name }
        }
        val ok = expScore >= minScore && expScore >= bestScore - margin
        return ChordMatch(expected.name, expScore, bestName, bestScore, ok)
    }
}

/**
 * Ses örneklerini parça parça alır, tele vuruşları yakalar ve her vuruş için kroma üretir.
 * @param onStrum her vuruştan ~0,35 sn sonra 12 elemanlı, normalize edilmiş kroma ile çağrılır.
 */
class StrumAnalyzer(
    private val sampleRate: Int = Synth.SAMPLE_RATE,
    private val onStrum: (FloatArray) -> Unit,
) {
    private val windowSize = 8192
    private val hop = 1024
    private val fft = Fft(windowSize)
    private val hann = FloatArray(windowSize) { (0.5 - 0.5 * cos(2 * PI * it / (windowSize - 1))).toFloat() }

    /** Her FFT kutusunun ait olduğu nota (0..11) ve ağırlığı; kapsam dışı kutular -1. */
    private val binPitch = IntArray(windowSize / 2)
    private val binWeight = FloatArray(windowSize / 2)

    private val history = FloatArray(windowSize)
    private val hopBuf = FloatArray(hop)
    private var hopFill = 0
    private var samples = 0L

    private var slowRms = 0f
    private var onsetAt = -1L
    private var lastOnset = -1_000_000L
    private val chromaSum = FloatArray(12)
    private var chromaFrames = 0

    /** Vuruş sayılması için gereken en düşük ses seviyesi (RMS, 0..1). */
    var minLevel = 0.012f

    init {
        for (k in binPitch.indices) {
            val f = k.toDouble() * sampleRate / windowSize
            if (f < 75.0 || f > 1100.0) { binPitch[k] = -1; continue }
            val midi = 69 + 12 * ln(f / 440.0) / ln(2.0)
            val nearest = midi.roundToInt()
            val dev = midi - nearest // -0.5..0.5 yarım ses
            binPitch[k] = ((nearest % 12) + 12) % 12
            // Notanın tam ortasına yakın kutular daha çok sayılır
            binWeight[k] = cos(PI * dev).let { (it * it).toFloat() }
        }
    }

    fun reset() {
        history.fill(0f); hopFill = 0; samples = 0; slowRms = 0f; onsetAt = -1; lastOnset = -1_000_000L
        chromaSum.fill(0f); chromaFrames = 0
    }

    fun feed(data: FloatArray, count: Int = data.size) {
        var i = 0
        while (i < count) {
            val n = minOf(hop - hopFill, count - i)
            System.arraycopy(data, i, hopBuf, hopFill, n)
            hopFill += n; i += n
            if (hopFill == hop) { processHop(); hopFill = 0 }
        }
    }

    private fun processHop() {
        System.arraycopy(history, hop, history, 0, windowSize - hop)
        System.arraycopy(hopBuf, 0, history, windowSize - hop, hop)
        samples += hop

        var s = 0f
        for (v in hopBuf) s += v * v
        val rms = sqrt(s / hop)

        val refractory = sampleRate / 4 // iki vuruş arası en az 0,25 sn
        val isOnset = rms > minLevel && rms > slowRms * 2.2f + 0.002f && samples - lastOnset > refractory
        slowRms = if (rms > slowRms) slowRms * 0.7f + rms * 0.3f else slowRms * 0.93f + rms * 0.07f
        if (isOnset) {
            onsetAt = samples
            lastOnset = samples
            chromaSum.fill(0f); chromaFrames = 0
            return
        }
        if (onsetAt < 0) return

        // Vuruşun ilk ~50 ms'si (tırnak/pena gürültüsü) atlanır; pencere dolunca iki kez ölçülür
        val since = samples - onsetAt
        val first = sampleRate / 20 + windowSize
        val second = first + windowSize / 2
        if (since == roundUp(first) || since == roundUp(second)) {
            val c = chroma()
            for (k in 0 until 12) chromaSum[k] += c[k]
            chromaFrames++
            if (chromaFrames == 2) {
                onsetAt = -1
                if (rms > minLevel * 0.3f) onStrum(ChordTemplates.normalize(chromaSum.copyOf()))
            }
        }
    }

    private fun roundUp(n: Int): Long = ((n + hop - 1) / hop * hop).toLong()

    /** Geçmiş penceresinin kroması (normalize edilmiş). */
    fun chroma(): FloatArray {
        val re = DoubleArray(windowSize) { (history[it] * hann[it]).toDouble() }
        val im = DoubleArray(windowSize)
        fft.transform(re, im)
        val mag = FloatArray(binPitch.size)
        var max = 0f
        for (k in binPitch.indices) {
            if (binPitch[k] < 0) continue
            mag[k] = sqrt(re[k] * re[k] + im[k] * im[k]).toFloat()
            if (mag[k] > max) max = mag[k]
        }
        val c = FloatArray(12)
        if (max == 0f) return c
        // Yalnızca belirgin tepeler sayılır: pencere sızıntısı ve gürültü kromaya karışmaz
        val floor = max * 0.04f
        for (k in 1 until binPitch.size - 1) {
            val p = binPitch[k]
            if (p < 0 || mag[k] < floor || mag[k] < mag[k - 1] || mag[k] < mag[k + 1]) continue
            // Karekök: güçlü bas notaların her şeyi bastırmasını önler
            c[p] += sqrt(mag[k]) * binWeight[k]
        }
        return ChordTemplates.normalize(c)
    }
}

/** Basit, yerinde çalışan radix-2 FFT. */
class Fft(private val n: Int) {
    private val cosT = DoubleArray(n / 2) { cos(2 * PI * it / n) }
    private val sinT = DoubleArray(n / 2) { -sin(2 * PI * it / n) }

    init { require(n > 0 && n and (n - 1) == 0) { "FFT boyu 2'nin kuvveti olmalı" } }

    fun transform(re: DoubleArray, im: DoubleArray) {
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
            j = j xor bit
            if (i < j) {
                var t = re[i]; re[i] = re[j]; re[j] = t
                t = im[i]; im[i] = im[j]; im[j] = t
            }
        }
        var len = 2
        while (len <= n) {
            val step = n / len
            val half = len / 2
            var i = 0
            while (i < n) {
                for (k in 0 until half) {
                    val wr = cosT[k * step]; val wi = sinT[k * step]
                    val a = i + k; val b = a + half
                    val xr = re[b] * wr - im[b] * wi
                    val xi = re[b] * wi + im[b] * wr
                    re[b] = re[a] - xr; im[b] = im[a] - xi
                    re[a] += xr; im[a] += xi
                }
                i += len
            }
            len = len shl 1
        }
    }
}
