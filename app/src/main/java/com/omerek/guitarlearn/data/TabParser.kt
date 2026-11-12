package com.omerek.guitarlearn.data

import com.omerek.guitarlearn.audio.Tuning

/**
 * Basit tab okuyucu. Satırlar yukarıdan aşağı e, B, G, D, A, E (1. telden 6. tele) olmalı.
 * Her sütunda basılan notaları bir "olay" olarak döndürür (aynı sütundakiler birlikte çalınır).
 */
object TabParser {
    /** Bir tab olayı: birlikte çalınan MIDI notaları. */
    data class TabEvent(val midiNotes: List<Int>)

    fun parse(tab: String): List<TabEvent> {
        val lines = tab.lines().map { it.trim() }.filter { it.length > 2 && it[1] == '|' }
        if (lines.size != 6) return emptyList()
        // satır 0 = 1. tel (ince e) -> openStringMidi[5]
        val bodies = lines.map { it.substring(2) }
        val width = bodies.maxOf { it.length }
        val events = mutableListOf<TabEvent>()
        var col = 0
        while (col < width) {
            val notes = mutableListOf<Int>()
            var consumed = 1
            for ((row, body) in bodies.withIndex()) {
                if (col >= body.length || !body[col].isDigit()) continue
                // önceki karakter rakamsa bu iki haneli sayının devamıdır, atla
                if (col > 0 && body[col - 1].isDigit()) continue
                var end = col
                while (end < body.length && body[end].isDigit()) end++
                val fret = body.substring(col, end).toInt()
                consumed = maxOf(consumed, end - col)
                notes += Tuning.openStringMidi[5 - row] + fret
            }
            if (notes.isNotEmpty()) events += TabEvent(notes.sortedDescending())
            col += consumed
        }
        return events
    }
}
