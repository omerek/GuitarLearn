package com.omerek.guitarlearn.data

/**
 * Şarkı metnini kelime kelime düzenlemek için: her kelimenin üstünde en fazla bir akor durur.
 * Metin her zaman "[Am]söz" biçiminde saklanır; bu sınıf onu kelimelere bölüp geri birleştirir.
 */
object LyricTokens {
    /** chord: kelimenin üstündeki akor; text: kelime ve ardındaki boşluk (akor-tek parçalarda boş). */
    data class Word(val chord: String?, val text: String)

    sealed interface Line {
        data class Words(val words: List<Word>) : Line
        data class Raw(val text: String) : Line // başlık ya da boş satır, olduğu gibi kalır
    }

    private val wordRegex = Regex("""\S+\s*|\s+""")

    fun parse(content: String): List<Line> = content.lines().map { raw ->
        if (raw.isBlank() || raw.trimStart().startsWith("#")) {
            Line.Raw(raw)
        } else {
            Line.Words(
                SongParser.parseSegments(raw).flatMap { seg ->
                    val pieces = wordRegex.findAll(seg.text).map { it.value }.toList().ifEmpty { listOf("") }
                    pieces.mapIndexed { i, w -> Word(if (i == 0) seg.chord else null, w) }
                },
            )
        }
    }

    fun serialize(lines: List<Line>): String = lines.joinToString("\n") { line ->
        when (line) {
            is Line.Raw -> line.text
            is Line.Words -> buildString {
                line.words.forEach { w ->
                    if (w.chord != null) append("[").append(w.chord).append("]")
                    append(w.text)
                    // Ardışık akor-tek parçalar birbirine yapışmasın
                    if (w.text.isEmpty() && w.chord != null) append(" ")
                }
            }.trimEnd()
        }
    }

    /** Bir kelimenin akorunu ayarlar; aynı akor zaten varsa ya da chord null ise kaldırır. */
    fun setChord(content: String, lineIndex: Int, wordIndex: Int, chord: String?): String {
        val lines = parse(content).toMutableList()
        val line = lines.getOrNull(lineIndex) as? Line.Words ?: return content
        val words = line.words.toMutableList()
        val w = words.getOrNull(wordIndex) ?: return content
        val newChord = if (chord == null || w.chord == chord) null else chord
        if (newChord == null && w.text.isEmpty()) words.removeAt(wordIndex) else words[wordIndex] = w.copy(chord = newChord)
        lines[lineIndex] = Line.Words(words)
        return serialize(lines)
    }

    /** Satırın sonuna yalnız bir akor ekler (ör. sözün bittiği yerde akor değişimi). */
    fun appendChord(content: String, lineIndex: Int, chord: String): String {
        val lines = parse(content).toMutableList()
        val line = lines.getOrNull(lineIndex) ?: return content
        lines[lineIndex] = when (line) {
            is Line.Words -> Line.Words(
                line.words.mapIndexed { i, w -> if (i == line.words.lastIndex && !w.text.endsWith(" ")) w.copy(text = w.text + " ") else w } +
                    Word(chord, ""),
            )
            is Line.Raw -> if (line.text.isBlank()) Line.Words(listOf(Word(chord, ""))) else line
        }
        return serialize(lines)
    }
}
