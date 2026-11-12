package com.omerek.guitarlearn.data

/** Kullanıcının yazdığı akor adlarını düzgün biçime çevirir: "am" → "Am", "f#M7" → "F#maj7", "g/b" → "G/B". */
object ChordName {
    private val pattern = Regex("""^([A-Ga-g])([#b♯♭]?)([^/\s]*)(?:/([A-Ga-g])([#b♯♭]?))?$""")

    /** Bilinen ek yazımları → standart ek. Anahtarlar küçük harfli. */
    private val suffixes = mapOf(
        "" to "", "maj" to "", "major" to "",
        "m" to "m", "min" to "m", "minor" to "m", "-" to "m",
        "7" to "7", "dom7" to "7",
        "m7" to "m7", "min7" to "m7", "-7" to "m7",
        "maj7" to "maj7", "ma7" to "maj7", "j7" to "maj7", "δ" to "maj7", "δ7" to "maj7",
        "mmaj7" to "mmaj7",
        "6" to "6", "m6" to "m6", "9" to "9", "m9" to "m9", "maj9" to "maj9", "11" to "11", "13" to "13",
        "add9" to "add9", "add2" to "add9", "madd9" to "madd9",
        "sus" to "sus4", "sus4" to "sus4", "sus2" to "sus2", "7sus4" to "7sus4",
        "dim" to "dim", "°" to "dim", "o" to "dim", "dim7" to "dim7", "o7" to "dim7",
        "m7b5" to "m7b5", "ø" to "m7b5",
        "aug" to "aug", "+" to "aug", "5" to "5",
    )

    /** Geçerli bir akor adıysa standart yazımını, değilse null döndürür. */
    fun normalize(raw: String): String? {
        val text = raw.trim().replace('♯', '#').replace('♭', 'b')
        val m = pattern.find(text) ?: return null
        val (root, acc, suffixRaw, bassRoot, bassAcc) = m.destructured
        val suffix = when {
            // Büyük M tek başına "majör" demektir (CM7 = Cmaj7); küçük m minördür.
            suffixRaw == "M" -> ""
            suffixRaw.startsWith("M") && !suffixRaw.startsWith("Maj", ignoreCase = false) && suffixRaw.length > 1 ->
                suffixes["maj" + suffixRaw.drop(1).lowercase()] ?: return null
            else -> suffixes[suffixRaw.lowercase()] ?: return null
        }
        val bass = if (bassRoot.isNotEmpty()) "/" + bassRoot.uppercase() + bassAcc else ""
        return root.uppercase() + acc + suffix + bass
    }

    /** "a, g, Em; d7" gibi bir listeyi ayırır. Tanınanlar ve tanınmayanlar ayrı döner. */
    fun parseList(text: String): ParsedChords {
        val known = mutableListOf<String>()
        val unknown = mutableListOf<String>()
        text.split(Regex("""[,;\s]+""")).filter { it.isNotBlank() }.forEach { token ->
            val n = normalize(token)
            if (n == null) unknown += token else if (n !in known) known += n
        }
        return ParsedChords(known, unknown)
    }

    data class ParsedChords(val chords: List<String>, val invalid: List<String>)
}
