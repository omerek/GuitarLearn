package com.omerek.guitarlearn

import com.omerek.guitarlearn.data.Chords
import com.omerek.guitarlearn.data.LessonBlock
import com.omerek.guitarlearn.data.Lessons
import com.omerek.guitarlearn.data.Segment
import com.omerek.guitarlearn.data.SongParser
import com.omerek.guitarlearn.data.Songs
import com.omerek.guitarlearn.data.TabParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentTest {
    @Test
    fun everySongChordExistsInLibrary() {
        for (song in Songs.all) for (c in song.chords) {
            assertNotNull("${song.id}: '$c' kütüphanede yok", Chords.get(c))
        }
    }

    @Test
    fun everyLessonReferenceResolves() {
        for (l in Lessons.all) for (b in l.blocks) when (b) {
            is LessonBlock.Chords -> b.names.forEach { assertNotNull("${l.id}: $it", Chords.get(it)) }
            is LessonBlock.ChordChange -> {
                assertNotNull(Chords.get(b.from)); assertNotNull(Chords.get(b.to))
            }
            is LessonBlock.SongLink -> assertNotNull("${l.id}: ${b.songId}", Songs.get(b.songId))
            is LessonBlock.Tab -> assertTrue("${l.id}: ${b.title}", TabParser.parse(b.tab).isNotEmpty())
            is LessonBlock.Strum -> assertEquals(0, b.pattern.length % 2)
            else -> {}
        }
    }

    @Test
    fun idsAreUnique() {
        assertEquals(Lessons.all.size, Lessons.all.map { it.id }.toSet().size)
        assertEquals(Songs.all.size, Songs.all.map { it.id }.toSet().size)
        assertEquals(Chords.all.size, Chords.all.map { it.name }.toSet().size)
    }

    @Test
    fun parsesChordLine() {
        val seg = SongParser.parseSegments("[Am]Üsküdar'a gider iken [Dm]aldı")
        assertEquals(listOf(Segment("Am", "Üsküdar'a gider iken "), Segment("Dm", "aldı")), seg)
        val lead = SongParser.parseSegments("Oh [C]Lord [G]")
        assertEquals(listOf(Segment(null, "Oh "), Segment("C", "Lord "), Segment("G", "")), lead)
    }

    @Test
    fun parsesTab() {
        val tab = """
            e|-0-----|
            B|---1---|
            G|-----12|
            D|-------|
            A|-------|
            E|-------|
        """.trimIndent()
        val events = TabParser.parse(tab)
        assertEquals(listOf(64, 60, 67), events.map { it.midiNotes.single() })
    }

    @Test
    fun chordMidiNotes() {
        assertEquals(listOf(40, 47, 52, 55, 59, 64), Chords.get("Em")!!.midiNotes)
        assertEquals(listOf(48, 52, 55, 60, 64), Chords.get("C")!!.midiNotes)
    }
}
