package com.omerek.guitarlearn

import com.omerek.guitarlearn.audio.PitchDetector
import com.omerek.guitarlearn.audio.Synth
import com.omerek.guitarlearn.audio.Tuning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PitchDetectorTest {
    private val size = 4096
    private val detector = PitchDetector(Synth.SAMPLE_RATE, size)

    private fun window(signal: FloatArray, offset: Int = 2000) = signal.copyOfRange(offset, offset + size)

    @Test
    fun detectsEveryOpenStringWithin2Cents() {
        for (s in Tuning.standard) {
            val signal = Synth.pluck(s.frequency, seconds = 0.5)
            val f = detector.detect(window(signal))
            val cents = Tuning.cents(f.toDouble(), s.frequency)
            assertTrue("${s.note}${s.octave}: $f Hz ($cents sent)", abs(cents) < 2.0)
        }
    }

    @Test
    fun detectsSlightlyDetunedString() {
        val target = 110.0 * Math.pow(2.0, -15 / 1200.0) // 15 sent pes La
        val f = detector.detect(window(Synth.pluck(target, 0.5)))
        assertEquals(-15.0, Tuning.cents(f.toDouble(), 110.0), 2.0)
        assertEquals("A", Tuning.closestString(f.toDouble()).note)
    }

    @Test
    fun silenceGivesNoPitch() {
        val f = detector.detect(FloatArray(size))
        assertEquals(-1f, f)
    }

    @Test
    fun noteNames() {
        val n = Tuning.nearestNote(440.0)
        assertEquals("A", n.name); assertEquals(4, n.octave); assertEquals("La", n.solfege)
        assertEquals("E", Tuning.nearestNote(82.41).name)
        assertEquals(2, Tuning.nearestNote(82.41).octave)
    }
}
