package com.omerek.guitarlearn.audio

import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

/** Bir gitar teli: numara (6 = en kalın), nota adı, Türkçe adı ve frekans. */
data class GuitarString(
    val number: Int,
    val note: String,
    val solfege: String,
    val octave: Int,
    val frequency: Double,
)

object Tuning {
    /** Standart akort (klasik ve akustik gitarda aynı): E2 A2 D3 G3 B3 E4 */
    val standard = listOf(
        GuitarString(6, "E", "Mi", 2, 82.41),
        GuitarString(5, "A", "La", 2, 110.00),
        GuitarString(4, "D", "Re", 3, 146.83),
        GuitarString(3, "G", "Sol", 3, 196.00),
        GuitarString(2, "B", "Si", 3, 246.94),
        GuitarString(1, "E", "Mi", 4, 329.63),
    )

    private val noteNames = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    private val solfegeNames = listOf("Do", "Do#", "Re", "Re#", "Mi", "Fa", "Fa#", "Sol", "Sol#", "La", "La#", "Si")

    /** İki frekans arasındaki fark, sent cinsinden (100 sent = 1 yarım ses). */
    fun cents(frequency: Double, target: Double): Double = 1200.0 * log2(frequency / target)

    /** Frekansa en yakın teli bulur (otomatik mod). */
    fun closestString(frequency: Double): GuitarString =
        standard.minBy { abs(cents(frequency, it.frequency)) }

    /** Frekansa en yakın nota (ör. "A", "La", 2, cents). A4 = 440 Hz. */
    fun nearestNote(frequency: Double): NoteInfo {
        val midi = 69 + 12 * log2(frequency / 440.0)
        val rounded = midi.roundToInt()
        val idx = ((rounded % 12) + 12) % 12
        val exact = 440.0 * 2.0.pow((rounded - 69) / 12.0)
        return NoteInfo(noteNames[idx], solfegeNames[idx], rounded / 12 - 1, cents(frequency, exact))
    }

    /** MIDI nota numarasından frekans. */
    fun midiToFrequency(midi: Int): Double = 440.0 * 2.0.pow((midi - 69) / 12.0)

    /** Açık tellerin MIDI numaraları, kalın telden ince tele (6..1). */
    val openStringMidi = intArrayOf(40, 45, 50, 55, 59, 64)
}

data class NoteInfo(val name: String, val solfege: String, val octave: Int, val cents: Double)
