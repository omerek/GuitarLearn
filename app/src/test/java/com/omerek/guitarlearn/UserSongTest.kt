package com.omerek.guitarlearn

import com.omerek.guitarlearn.data.ChordName
import com.omerek.guitarlearn.data.ChordSheetImporter
import com.omerek.guitarlearn.data.Chords
import com.omerek.guitarlearn.data.LyricTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserSongTest {
    @Test
    fun normalizesChordNames() {
        assertEquals("A", ChordName.normalize("a"))
        assertEquals("Am", ChordName.normalize("am"))
        assertEquals("F#m", ChordName.normalize("f#m"))
        assertEquals("Cmaj7", ChordName.normalize("CM7"))
        assertEquals("Cmaj7", ChordName.normalize("cmaj7"))
        assertEquals("Bb", ChordName.normalize("B♭"))
        assertEquals("G/B", ChordName.normalize("g/b"))
        assertEquals("Dsus4", ChordName.normalize("Dsus"))
        assertNull(ChordName.normalize("Merhaba"))
        assertNull(ChordName.normalize("H"))
    }

    @Test
    fun parsesChordList() {
        val p = ChordName.parseList("a, g,e  am;xyz, A")
        assertEquals(listOf("A", "G", "E", "Am"), p.chords)
        assertEquals(listOf("xyz"), p.invalid)
    }

    @Test
    fun libraryLookupHandlesSpellingAndEnharmonics() {
        assertEquals("Am", Chords.get("am")?.name)
        assertEquals("Bb", Chords.get("A#")?.name)
        assertEquals("Ab", Chords.get("G#")?.name)
        assertEquals("D/F#", Chords.get("D/F#")?.name)
        assertEquals("A", Chords.get("A/C#")?.name) // bas notalı yoksa ana akor
        assertNotNull(Chords.get("F#m"))
        assertNull(Chords.get("Bbm9"))
    }

    @Test
    fun importsChordsOverLyrics() {
        val sheet = """
            [Verse]
            Am              Dm     Am
            Üsküdar'a gider iken aldı

            G   C
            Intro: no
            E7
        """.trimIndent()
        assertTrue(ChordSheetImporter.looksLikeChordSheet(sheet))
        val out = ChordSheetImporter.convert(sheet)
        assertEquals(
            """
            # Verse
            [Am]Üsküdar'a gider [Dm]iken al[Am]dı

            [G]Intr[C]o: no
            [E7]
            """.trimIndent(),
            out,
        )
        assertFalse(ChordSheetImporter.isChordLine("Am I the one"))
        assertTrue(ChordSheetImporter.isChordLine("| Am | G | x2"))
    }

    @Test
    fun placesChordsOnWords() {
        var text = "Kâtip uykudan uyanmış\n\n# Nakarat\nel ne karışır"
        text = LyricTokens.setChord(text, 0, 1, "Am")
        assertEquals("Kâtip [Am]uykudan uyanmış\n\n# Nakarat\nel ne karışır", text)
        text = LyricTokens.setChord(text, 3, 0, "E")
        text = LyricTokens.appendChord(text, 3, "Am")
        assertEquals("Kâtip [Am]uykudan uyanmış\n\n# Nakarat\n[E]el ne karışır [Am]", text)
        // aynı akora tekrar dokunmak kaldırır
        text = LyricTokens.setChord(text, 0, 1, "Am")
        assertEquals("Kâtip uykudan uyanmış\n\n# Nakarat\n[E]el ne karışır [Am]", text)
        // akor-tek parçayı kaldırmak onu siler
        val words = (LyricTokens.parse(text)[3] as LyricTokens.Line.Words).words
        text = LyricTokens.setChord(text, 3, words.lastIndex, null)
        assertEquals("Kâtip uykudan uyanmış\n\n# Nakarat\n[E]el ne karışır", text)
        // ortadan bölünmüş kelime korunur
        assertEquals("Oh, Su[C]sanna", LyricTokens.serialize(LyricTokens.parse("Oh, Su[C]sanna")))
    }
}

class BackupTest {
    @Test
    fun roundTrip() {
        val songs = listOf(
            com.omerek.guitarlearn.data.UserSong(
                id = "a1", title = "Şarkı \"tırnaklı\"", artist = "Ben", bpm = 92, beatsPerBar = 3,
                chords = listOf("A", "G#m"), content = "[A]Bir satır\n\n# Nakarat\n\t[G#m]iki \\ ters", updatedAt = 1700000000000,
            ),
            com.omerek.guitarlearn.data.UserSong(id = "b2", title = "Boş"),
        )
        val text = com.omerek.guitarlearn.data.UserSongBackup.encode(songs)
        assertEquals(songs, com.omerek.guitarlearn.data.UserSongBackup.decode(text))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsForeignFile() {
        com.omerek.guitarlearn.data.UserSongBackup.decode("""{"foo": 1}""")
    }
}
