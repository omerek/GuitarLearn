package com.omerek.guitarlearn.led

import com.omerek.guitarlearn.data.Chord

/**
 * Akoru gitarın sapına dizilmiş adreslenebilir LED'lere (WS2812B) çevirir. Saf Kotlin, Android'e bağlı değil.
 *
 * Tel numaraları gitarcıların kullandığı gibidir: 1 = en ince (tutarken en altta kalan) tel, 6 = en kalın tel.
 */
object StringColors {
    /** Tel renkleri, RGB (0xRRGGBB). Kırmızı yalnızca susturulan teller için ayrıldı. */
    private val rgb = mapOf(
        1 to 0x00E040, // yeşil
        2 to 0x1E64FF, // mavi
        3 to 0xFFC800, // sarı
        4 to 0xC000FF, // mor
        5 to 0x00D8D8, // turkuaz
        6 to 0xFFFFFF, // beyaz
    )

    val names = mapOf(1 to "yeşil", 2 to "mavi", 3 to "sarı", 4 to "mor", 5 to "turkuaz", 6 to "beyaz")

    const val MUTED = 0xFF2020

    fun of(stringNumber: Int): Int = rgb.getValue(stringNumber)
}

/** Bir akorda basılacak tek bir nokta. */
data class FretNote(val stringNumber: Int, val fret: Int)

/** LED'lerin sapa nasıl dizildiği. */
enum class LedArrangement(val title: String) {
    /** Her perdede tek LED; renk hangi tel olduğunu söyler. Aynı perdede birden çok tel varsa renkler sırayla döner. */
    PER_FRET("Perde başına 1 LED"),

    /** Her perdede 6 LED (her tele bir tane). */
    PER_STRING("Perde başına 6 LED (her tel ayrı)"),
}

/**
 * LED ayarları.
 * @param fretCount sapa takılı perde sayısı (1. perdeden başlayarak).
 * @param firstStringFirst PER_STRING düzeninde her perdenin ilk LED'i 1. tel mi (true) yoksa 6. tel mi.
 * @param zigzag PER_STRING düzeninde şerit her perdede yön değiştiriyor mu (yılan gibi döşeme).
 */
data class LedConfig(
    val arrangement: LedArrangement = LedArrangement.PER_FRET,
    val fretCount: Int = 12,
    val brightness: Int = 60,
    val firstStringFirst: Boolean = true,
    val zigzag: Boolean = false,
) {
    val ledCount: Int get() = if (arrangement == LedArrangement.PER_FRET) fretCount else fretCount * 6
}

/** USB bağlantı durumu. */
enum class LedStatus { NO_DEVICE, NEEDS_PERMISSION, CONNECTED, ERROR }

/** Kartla nasıl konuşuluyor. */
enum class LedTransport(val title: String) { USB("USB"), BLE("Bluetooth") }

data class LedState(
    val status: LedStatus = LedStatus.NO_DEVICE,
    val message: String = "Kart bağlı değil",
    /** Kartın bildirdiği yazılım cevabı geldiyse true. */
    val firmwareOk: Boolean = false,
    val transport: LedTransport? = null,
    /** Kart yazılımının sürümü ve kart türü ("S2" / "S3"); eski yazılımlar bildirmez. */
    val firmwareVersion: String? = null,
    val board: String? = null,
)

/** Kart yazılımı güncellemesinin durumu. */
sealed interface FirmwareUpdateState {
    data object Idle : FirmwareUpdateState
    data class Running(val progress: Float) : FirmwareUpdateState
    data object Done : FirmwareUpdateState
    data class Failed(val message: String) : FirmwareUpdateState
}

/** Karttan gelen "?" cevabı: LED sayısı, sürüm ve kart türü. */
data class FirmwareHello(val ledCount: Int, val version: String?, val board: String?)

/** Telefona indirilen .bin güncelleme dosyasının denetimi. Saf Kotlin. */
object FirmwareImage {
    /** ESP32 uygulama görüntüsünün başındaki çip numarasından kart türü. */
    fun chipOf(bin: ByteArray): String? {
        if (bin.size < 24 || (bin[0].toInt() and 0xFF) != 0xE9) return null
        return when ((bin[12].toInt() and 0xFF) or ((bin[13].toInt() and 0xFF) shl 8)) {
            0 -> "ESP32"
            2 -> "S2"
            5 -> "C3"
            9 -> "S3"
            else -> "?"
        }
    }

    /** Dosyada sorun varsa kullanıcıya gösterilecek açıklama, yoksa null. */
    fun problem(bin: ByteArray, board: String?): String? {
        val chip = chipOf(bin) ?: return "Bu bir kart yazılımı dosyası değil (.bin olmalı)."
        // Önyükleyici dahil "merged" dosyada 0x8000'de bölüm tablosu bulunur
        if (bin.size > 0x8002 && (bin[0x8000].toInt() and 0xFF) == 0xAA && (bin[0x8001].toInt() and 0xFF) == 0x50) {
            return "Bu dosya önyükleyici dahil birleşik dosya. Yalnızca uygulama dosyasını (firmware.bin / .ino.bin) seç."
        }
        if (bin.size > 0x1F0000) return "Dosya çok büyük (${bin.size / 1024} KB)."
        if (board != null && chip != board) return "Bu dosya ESP32-$chip için, kartın ise ESP32-$board."
        return null
    }

    fun md5Hex(bin: ByteArray): String =
        java.security.MessageDigest.getInstance("MD5").digest(bin).joinToString("") { "%02x".format(it) }
}

/** Bluetooth taramasında bulunan bir LED kartı. */
data class BleCandidate(val name: String, val address: String, val rssi: Int)

/** Bir LED ve göstereceği renkler (birden fazlaysa kart sırayla döndürür). */
data class LedPixel(val index: Int, val colors: List<Int>)

object LedLayout {
    /** Akordaki basılan noktalar (açık ve susturulan teller hariç). Chord.frets kalın telden inceye sıralıdır. */
    fun pressedNotes(chord: Chord): List<FretNote> =
        chord.frets.mapIndexedNotNull { i, f -> if (f > 0) FretNote(stringNumber = 6 - i, fret = f) else null }

    /** Susturulan tel numaraları. */
    fun mutedStrings(chord: Chord): List<Int> =
        chord.frets.mapIndexedNotNull { i, f -> if (f < 0) 6 - i else null }

    /** Akoru LED karesine çevirir. Ayarlanan perde sayısının dışında kalan notalar atlanır. */
    fun pixels(chord: Chord, config: LedConfig): List<LedPixel> = pixels(pressedNotes(chord), config)

    fun pixels(notes: List<FretNote>, config: LedConfig): List<LedPixel> {
        val inRange = notes.filter { it.fret in 1..config.fretCount }
        return when (config.arrangement) {
            LedArrangement.PER_FRET -> inRange
                .groupBy { it.fret }
                .toSortedMap()
                .map { (fret, list) ->
                    // İnce telden kalına doğru dönsün; aynı tel bir kez
                    val colors = list.map { it.stringNumber }.distinct().sorted().map(StringColors::of)
                    LedPixel(fret - 1, colors)
                }
            LedArrangement.PER_STRING -> inRange
                .distinctBy { it.stringNumber to it.fret }
                .map { LedPixel(perStringIndex(it, config), listOf(StringColors.of(it.stringNumber))) }
                .sortedBy { it.index }
        }
    }

    private fun perStringIndex(note: FretNote, config: LedConfig): Int {
        val row = note.fret - 1
        var pos = if (config.firstStringFirst) note.stringNumber - 1 else 6 - note.stringNumber
        if (config.zigzag && row % 2 == 1) pos = 5 - pos
        return row * 6 + pos
    }
}

/**
 * Karta giden metin protokolü (her komut bir satır, '\n' ile biter):
 *  ?                         -> kart "GITARLED 1 <ledSayısı>" diye cevap verir
 *  N <sayı>                  -> LED sayısı (1..300)
 *  B <0..255>                -> parlaklık
 *  C                         -> hepsini söndür
 *  S 0=00E040 1=1E64FF/FFC800 -> kareyi ayarla (listede olmayanlar söner); '/' ile ayrılan renkler sırayla döner
 *  T                         -> test animasyonu
 *  A                         -> "bağlandı" animasyonu
 * Karttan uygulamaya:
 *  GITARLED 1 <n>            -> "?" cevabı
 *  K P / K N                 -> gitardaki önceki / sonraki düğmesine basıldı
 */
enum class LedButton { PREVIOUS, NEXT }

object LedProtocol {
    fun hello(): String = "?\n"
    fun count(n: Int): String = "N $n\n"
    fun brightness(b: Int): String = "B ${b.coerceIn(0, 255)}\n"
    fun clear(): String = "C\n"
    fun test(): String = "T\n"
    fun connectedAnimation(): String = "A\n"

    /** Karttan gelen düğme satırını çözer; düğme değilse null. */
    fun parseButton(line: String): LedButton? = when (line.trim()) {
        "K P" -> LedButton.PREVIOUS
        "K N" -> LedButton.NEXT
        else -> null
    }

    fun frame(pixels: List<LedPixel>): String {
        if (pixels.isEmpty()) return clear()
        return pixels.joinToString(" ", prefix = "S ", postfix = "\n") { p ->
            "${p.index}=" + p.colors.joinToString("/") { "%06X".format(it and 0xFFFFFF) }
        }
    }

    /** Kartın "?" cevabını çözer; tanınmazsa null. */
    fun parseHello(line: String): Int? = parseHelloInfo(line)?.ledCount

    /** "GITARLED 1 <n> [sürüm] [S2|S3]" */
    fun parseHelloInfo(line: String): FirmwareHello? {
        val parts = line.trim().split(Regex("\\s+"))
        if (parts.size < 3 || parts[0] != "GITARLED") return null
        val n = parts[2].toIntOrNull() ?: return null
        return FirmwareHello(n, parts.getOrNull(3), parts.getOrNull(4))
    }

    /** Güncelleme başlangıcı: bayt sayısı ve MD5. Ardından ham baytlar gönderilir. */
    fun updateStart(size: Int, md5: String): String = "U $size $md5\n"

    /** Karttan gelen güncelleme cevabı mı ("U OK", "U ACK n", "U DONE", "U ERR ...")? */
    fun isUpdateReply(line: String): Boolean = line.startsWith("U ")
}
