package com.omerek.guitarlearn

import com.omerek.guitarlearn.audio.ChordMatcher
import com.omerek.guitarlearn.audio.StrumAnalyzer
import com.omerek.guitarlearn.audio.Synth
import com.omerek.guitarlearn.data.Chord
import com.omerek.guitarlearn.data.Chords
import com.omerek.guitarlearn.led.LedArrangement
import com.omerek.guitarlearn.led.LedConfig
import com.omerek.guitarlearn.led.LedLayout
import com.omerek.guitarlearn.led.LedPixel
import com.omerek.guitarlearn.led.LedProtocol
import com.omerek.guitarlearn.led.StringColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LedAndChordTest {
    private fun chord(name: String): Chord = Chords.get(name)!!

    @Test
    fun perFretLayoutGroupsStringsOnSameFret() {
        // La majör: 4, 3, 2. teller 2. perdede -> tek LED (index 1), üç renk
        val px = LedLayout.pixels(chord("A"), LedConfig())
        assertEquals(listOf(LedPixel(1, listOf(StringColors.of(2), StringColors.of(3), StringColors.of(4)))), px)
    }

    @Test
    fun firstStringIsThinnest() {
        // F majörde 1. tel (en ince) 1. perdede
        val notes = LedLayout.pressedNotes(chord("F"))
        assertTrue(notes.any { it.stringNumber == 1 && it.fret == 1 })
        assertTrue(notes.any { it.stringNumber == 6 && it.fret == 1 })
        assertEquals(listOf(6), LedLayout.mutedStrings(chord("Am")))
        assertEquals(listOf(6, 5), LedLayout.mutedStrings(chord("D")))
    }

    @Test
    fun perStringLayoutIndexes() {
        val cfg = LedConfig(arrangement = LedArrangement.PER_STRING, fretCount = 5)
        // Em: 5. ve 4. tel 2. perdede -> satır 1, pozisyon 4 ve 3
        assertEquals(listOf(9, 10), LedLayout.pixels(chord("Em"), cfg).map { it.index })
        val zig = cfg.copy(zigzag = true)
        assertEquals(listOf(7, 8), LedLayout.pixels(chord("Em"), zig).map { it.index })
    }

    @Test
    fun notesBeyondFretCountAreSkipped() {
        val px = LedLayout.pixels(chord("Bm"), LedConfig(fretCount = 3))
        assertEquals(listOf(1, 2), px.map { it.index })
    }

    @Test
    fun protocolFrame() {
        val frame = LedProtocol.frame(listOf(LedPixel(0, listOf(0x00E040)), LedPixel(2, listOf(0x1E64FF, 0xFFC800))))
        assertEquals("S 0=00E040 2=1E64FF/FFC800\n", frame)
        assertEquals("C\n", LedProtocol.frame(emptyList()))
        assertEquals(12, LedProtocol.parseHello("GITARLED 1 12\r\n"))
        assertNull(LedProtocol.parseHello("merhaba"))
    }

    /** Sessizlik + vuruş + biraz gürültü üretip analizörden geçirir. */
    private fun strumChroma(notes: List<Int>, seed: Int = 1): List<FloatArray> {
        val rnd = Random(seed)
        val sr = Synth.SAMPLE_RATE
        val audio = FloatArray(sr / 2) + Synth.strum(notes, seconds = 1.5).map { it * 0.4f }.toFloatArray()
        for (i in audio.indices) audio[i] += (rnd.nextFloat() - 0.5f) * 0.004f
        val out = mutableListOf<FloatArray>()
        val analyzer = StrumAnalyzer(sr) { out += it }
        // Mikrofondan geliyormuş gibi küçük parçalarla besle
        var i = 0
        while (i < audio.size) {
            val n = minOf(777, audio.size - i)
            analyzer.feed(audio.copyOfRange(i, i + n))
            i += n
        }
        return out
    }

    @Test
    fun everyLibraryChordIsRecognized() {
        val matcher = ChordMatcher(Chords.all)
        for (c in Chords.all) {
            val events = strumChroma(c.midiNotes)
            assertEquals("${c.name}: tek vuruş bekleniyordu", 1, events.size)
            val m = matcher.match(events[0], c)
            assertTrue("${c.name} tanınmadı: $m", m.ok)
        }
    }

    @Test
    fun wrongChordIsRejected() {
        val matcher = ChordMatcher(Chords.all)
        val pairs = listOf("Am" to "Em", "Em" to "Am", "C" to "G", "G" to "D", "D" to "Am", "E" to "Am", "Dm" to "C")
        for ((played, expected) in pairs) {
            val chroma = strumChroma(chord(played).midiNotes).single()
            val m = matcher.match(chroma, chord(expected))
            assertFalse("$played çalındı ama $expected kabul edildi: $m", m.ok)
        }
    }

    @Test
    fun silenceProducesNoStrum() {
        val analyzer = StrumAnalyzer(Synth.SAMPLE_RATE) { error("vuruş olmamalı") }
        val rnd = Random(3)
        analyzer.feed(FloatArray(Synth.SAMPLE_RATE * 2) { (rnd.nextFloat() - 0.5f) * 0.004f })
    }
}
