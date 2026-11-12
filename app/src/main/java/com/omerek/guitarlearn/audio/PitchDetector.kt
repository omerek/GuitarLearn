package com.omerek.guitarlearn.audio

import kotlin.math.sqrt

/**
 * YIN perde (frekans) tespiti. Saf Kotlin; Android'e bağımlı değil, birim testiyle doğrulanır.
 * de Cheveigné & Kawahara (2002) makalesindeki adımları izler.
 */
class PitchDetector(
    private val sampleRate: Int,
    private val bufferSize: Int,
    private val threshold: Float = 0.15f,
    minFrequency: Float = 60f,
    maxFrequency: Float = 1000f,
) {
    private val halfSize = bufferSize / 2
    private val yinBuffer = FloatArray(halfSize)
    private val minTau = (sampleRate / maxFrequency).toInt().coerceAtLeast(2)
    private val maxTau = (sampleRate / minFrequency).toInt().coerceAtMost(halfSize - 1)

    /** Sinyalin ortalama karekök (RMS) seviyesi; sessizliği ayırt etmek için. */
    fun rms(samples: FloatArray): Float {
        var sum = 0.0
        for (s in samples) sum += s * s
        return sqrt(sum / samples.size).toFloat()
    }

    /** Hz cinsinden frekans döndürür; perde bulunamazsa -1. */
    fun detect(samples: FloatArray): Float {
        require(samples.size >= bufferSize) { "En az $bufferSize örnek gerekli" }

        // 1-2. Fark fonksiyonu
        for (tau in 0 until halfSize) yinBuffer[tau] = 0f
        for (tau in 1..maxTau) {
            var sum = 0f
            for (i in 0 until halfSize) {
                val delta = samples[i] - samples[i + tau]
                sum += delta * delta
            }
            yinBuffer[tau] = sum
        }

        // 3. Birikimli ortalama normalize fark
        yinBuffer[0] = 1f
        var runningSum = 0f
        for (tau in 1..maxTau) {
            runningSum += yinBuffer[tau]
            yinBuffer[tau] = if (runningSum == 0f) 1f else yinBuffer[tau] * tau / runningSum
        }

        // 4. Mutlak eşik
        var tauEstimate = -1
        var tau = minTau
        while (tau <= maxTau) {
            if (yinBuffer[tau] < threshold) {
                while (tau + 1 <= maxTau && yinBuffer[tau + 1] < yinBuffer[tau]) tau++
                tauEstimate = tau
                break
            }
            tau++
        }
        if (tauEstimate == -1) return -1f

        // 5. Parabolik interpolasyon
        val betterTau = parabolic(tauEstimate)
        return sampleRate / betterTau
    }

    private fun parabolic(tau: Int): Float {
        val x0 = if (tau < 1) tau else tau - 1
        val x2 = if (tau + 1 <= maxTau) tau + 1 else tau
        if (x0 == tau) return if (yinBuffer[tau] <= yinBuffer[x2]) tau.toFloat() else x2.toFloat()
        if (x2 == tau) return if (yinBuffer[tau] <= yinBuffer[x0]) tau.toFloat() else x0.toFloat()
        val s0 = yinBuffer[x0]
        val s1 = yinBuffer[tau]
        val s2 = yinBuffer[x2]
        val denom = 2f * (2f * s1 - s2 - s0)
        if (denom == 0f) return tau.toFloat()
        return tau + (s2 - s0) / denom
    }
}
