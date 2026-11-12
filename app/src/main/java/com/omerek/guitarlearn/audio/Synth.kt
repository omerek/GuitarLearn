package com.omerek.guitarlearn.audio

/**
 * Basit tel sesi sentezi (toplamsal sentez): temel frekans + sönümlenen üst harmonikler.
 * Perde tam doğrudur, bu yüzden kulakla akort için referans ses olarak da kullanılabilir.
 * Saf Kotlin; çıktı -1..1 aralığında örneklerdir.
 */
object Synth {
    const val SAMPLE_RATE = 44100

    /** Tek bir teli çalar (naylon tele benzer yumuşak bir ton). */
    fun pluck(frequency: Double, seconds: Double = 2.0): FloatArray {
        val total = (SAMPLE_RATE * seconds).toInt()
        val out = FloatArray(total)
        val attack = SAMPLE_RATE * 0.004
        var h = 1
        while (h <= 10 && frequency * h < SAMPLE_RATE / 2.2) {
            val amp = 1.0 / Math.pow(h.toDouble(), 1.3)
            val decayPerSec = 1.6 + 0.9 * h
            val w = 2 * Math.PI * frequency * h / SAMPLE_RATE
            val decayPerSample = Math.exp(-decayPerSec / SAMPLE_RATE)
            var env = amp
            for (n in 0 until total) {
                out[n] += (kotlin.math.sin(w * n) * env).toFloat()
                env *= decayPerSample
            }
            h++
        }
        for (n in 0 until minOf(total, attack.toInt())) out[n] *= (n / attack).toFloat()
        normalize(out, 0.8f)
        return out
    }

    /**
     * Bir akoru yukarıdan aşağı vurur gibi çalar.
     * @param midiNotes çalınacak notalar (susturulan teller hariç), kalın telden inceye.
     */
    fun strum(midiNotes: List<Int>, strumDelayMs: Int = 35, seconds: Double = 2.5): FloatArray {
        val total = (SAMPLE_RATE * seconds).toInt()
        val mix = FloatArray(total)
        midiNotes.forEachIndexed { i, midi ->
            val offset = SAMPLE_RATE * strumDelayMs * i / 1000
            val note = pluck(Tuning.midiToFrequency(midi), seconds)
            for (n in note.indices) {
                val target = n + offset
                if (target >= total) break
                mix[target] += note[n]
            }
        }
        normalize(mix, 0.8f)
        return mix
    }

    /** Metronom tıkı: kısa, sönümlenen bir sinüs. */
    fun click(frequency: Double, millis: Int = 30): FloatArray {
        val n = SAMPLE_RATE * millis / 1000
        return FloatArray(n) { i ->
            val env = 1f - i.toFloat() / n
            (kotlin.math.sin(2 * Math.PI * frequency * i / SAMPLE_RATE) * env * env).toFloat()
        }
    }

    fun normalize(buffer: FloatArray, peak: Float) {
        var max = 0f
        for (v in buffer) if (kotlin.math.abs(v) > max) max = kotlin.math.abs(v)
        if (max == 0f) return
        val k = peak / max
        for (i in buffer.indices) buffer[i] *= k
    }

    fun toPcm16(buffer: FloatArray): ShortArray =
        ShortArray(buffer.size) { (buffer[it].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort() }
}
