package com.omerek.guitarlearn.data

/** Ders içindeki bir blok. */
sealed interface LessonBlock {
    data class Text(val text: String) : LessonBlock
    data class Tip(val text: String) : LessonBlock
    data class Chords(val names: List<String>) : LessonBlock
    data class Tab(val title: String, val tab: String, val bpm: Int = 70) : LessonBlock

    /** pattern: 8 karakter (sekizlikler): D = aşağı, U = yukarı, - = boş. */
    data class Strum(val title: String, val pattern: String, val bpm: Int) : LessonBlock
    data class ChordChange(val from: String, val to: String) : LessonBlock
    data class SongLink(val songId: String) : LessonBlock
    data class OpenTool(val tool: Tool) : LessonBlock
}

enum class Tool { TUNER, METRONOME }

data class Lesson(
    val id: String,
    val unit: String,
    val title: String,
    val summary: String,
    val blocks: List<LessonBlock>,
)

object Lessons {
    private fun t(s: String) = LessonBlock.Text(s.trimIndent())
    private fun tip(s: String) = LessonBlock.Tip(s.trimIndent())

    val all: List<Lesson> = listOf(
        Lesson(
            "l01", "1. Bölüm · Hazırlık", "Gitarını tanı",
            "Gitarın parçaları, tel numaraları, oturuş ve parmak adları.",
            listOf(
                t("""
                    Klasik gitarın üç ana bölümü vardır: gövde, sap ve kafa. Sapın üzerindeki metal çubuklara perde teli denir; iki perde teli arasındaki boşluğa "perde" diyoruz. 1. perde kafaya en yakın olandır.
                """),
                t("""
                    Teller kalından inceye numaralanır:
                    6. tel: Mi (E) – en kalın
                    5. tel: La (A)
                    4. tel: Re (D)
                    3. tel: Sol (G)
                    2. tel: Si (B)
                    1. tel: Mi (e) – en ince
                    Akılda tutmak için: "Mi-La-Re-Sol-Si-Mi".
                """),
                t("""
                    Sol el parmakları (perde basan el):
                    1 = işaret, 2 = orta, 3 = yüzük, 4 = serçe.

                    Sağ el parmakları (klasik gitar geleneği, İspanyolca harfler):
                    p = başparmak, i = işaret, m = orta, a = yüzük.
                """),
                tip("""
                    Oturuş: Klasik gitarda gövdeyi sol bacağına koyup sol ayağının altına bir basamak (ya da kalın bir kitap) koymak sapı yukarı kaldırır ve bileğini rahatlatır. Sırtını dik tut, omuzlarını gevşet.
                """),
                tip("""
                    Sol el tırnaklarını kısa tut; uzun tırnak teli tam basmanı engeller. Perdeyi, perde teline yakın bastığında daha az güçle temiz ses alırsın.
                """),
            ),
        ),
        Lesson(
            "l02", "1. Bölüm · Hazırlık", "Gitarını akort et",
            "Her çalışmaya akortla başla. Uygulamadaki akort ediciyi kullanmayı öğren.",
            listOf(
                t("""
                    Akort, her telin doğru notayı vermesi demektir. Akortsuz bir gitarla doğru akorları bassan bile kulağa yanlış gelir.

                    Nasıl yapılır:
                    1. Akort ekranını aç ve mikrofon iznini ver.
                    2. Bir teli tek başına çal, ses sönmeden ekrana bak.
                    3. İbre solda ise (pes) burguyu teli sıkacak yönde, sağda ise (tiz) gevşetecek yönde çevir.
                    4. İbre ortaya gelip yeşil yanınca o tel tamamdır. 6 tel için tekrarla.
                """),
                tip("""
                    Naylon teller ilk haftalarda çok esner ve sık sık akordu kaçar; bu normaldir. Yeni tel taktıysan günde birkaç kez akort etmen gerekebilir.
                """),
                tip("""
                    Teli her zaman hedef notanın biraz altından yukarı doğru getir. Fazla sıktıysan önce gevşet, sonra tekrar yukarı çık; akort daha uzun süre kalır.
                """),
                LessonBlock.OpenTool(Tool.TUNER),
            ),
        ),
        Lesson(
            "l03", "1. Bölüm · Hazırlık", "Akor şeması ve tab okuma",
            "Uygulamadaki tüm derslerin dili: akor şemaları ve tablar.",
            listOf(
                t("""
                    Akor şeması, gitar sapının dik çevrilmiş bir resmidir. Dikey çizgiler teller (solda kalın Mi, sağda ince Mi), yatay çizgiler perdelerdir. En üstteki kalın çizgi gitarın kafa tarafını gösterir.

                    • Noktalar: parmağını basacağın yer; içindeki sayı hangi parmakla basacağını söyler.
                    • O: tel açık çalınır (hiçbir yere basmadan).
                    • X: o tel çalınmaz.
                """),
                LessonBlock.Chords(listOf("Em")),
                t("""
                    Tab (tablatür), melodi ve parmak çalımı için kullanılır. 6 yatay çizgi 6 teli gösterir; EN ÜSTTEKİ çizgi İNCE Mi telidir (e), en alttaki kalın Mi (E). Çizgi üzerindeki sayı basacağın perdeyi gösterir; 0 açık tel demektir. Soldan sağa doğru okunur.
                """),
                LessonBlock.Tab("Açık teller: kalından inceye ve geri", Tabs.openStrings, 80),
                t("""
                    Isınma egzersizi ("örümcek"): her telde 1, 2, 3 ve 4. perdeleri sırasıyla 1, 2, 3 ve 4. parmaklarla bas. Yavaş ve temiz çal; hız sonra gelir.
                """),
                LessonBlock.Tab("Örümcek egzersizi", Tabs.spider, 70),
                tip("Bir tabın yanındaki ▶ düğmesine basarak nasıl duyulması gerektiğini dinleyebilirsin."),
            ),
        ),
        Lesson(
            "l04", "2. Bölüm · İlk akorlar", "İlk iki akor: Em ve Am",
            "Sadece iki-üç parmakla çalınan, en kolay iki akor.",
            listOf(
                t("""
                    İlk akorların Mi minör (Em) ve La minör (Am). İkisi de birbirine çok benzer: Em'deki 2. ve 3. parmakları bir tel aşağı (inceye doğru) kaydırıp işaret parmağını 2. tel 1. perdeye eklersen Am olur.
                """),
                LessonBlock.Chords(listOf("Em", "Am")),
                t("""
                    Önce her akoru basıp telleri tek tek çal. Boğuk ya da vızıldayan tel varsa: parmağını perde teline yaklaştır, parmak ucuyla ve dik bas, komşu tellere değme.
                """),
                LessonBlock.Strum("Dört aşağı vuruş", "D-D-D-D-", 60),
                LessonBlock.ChordChange("Em", "Am"),
                tip("Akor değiştirirken parmakları tek tek değil, hepsini aynı anda kaldırıp koymaya çalış. Ortak hareket eden parmakları (burada 2 ve 3) bir arada tut."),
                LessonBlock.SongLink("etut-em-am"),
            ),
        ),
        Lesson(
            "l05", "2. Bölüm · İlk akorlar", "Majör akorlar: E ve A",
            "Minörün \"hüzünlü\" sesinden majörün \"parlak\" sesine.",
            listOf(
                t("""
                    Mi majör (E), Em'e tek bir parmak eklemektir: işaret parmağını 3. tel 1. perdeye koy. İkisini art arda çalıp farkı dinle: minör hüzünlü, majör parlak duyulur.

                    La majör (A) için üç parmak aynı perdede (2. perde), 4., 3. ve 2. tellere yan yana dizilir. Klasik gitarın sapı geniş olduğu için bu akor rahat sığar.
                """),
                LessonBlock.Chords(listOf("E", "A")),
                LessonBlock.ChordChange("E", "A"),
                LessonBlock.ChordChange("Am", "E"),
                tip("Her gün 5 dakika akor geçişi çalışmak, haftada bir saat çalışmaktan çok daha etkilidir."),
            ),
        ),
        Lesson(
            "l06", "2. Bölüm · İlk akorlar", "D ve Dm",
            "Sadece ince dört tel ile çalınan akorlar.",
            listOf(
                t("""
                    Re majör (D) ve Re minör (Dm) sadece ilk 4 telle çalınır; 5. ve 6. telleri çalma. Vuruşa 4. telden (Re) başla.
                """),
                LessonBlock.Chords(listOf("D", "Dm")),
                LessonBlock.ChordChange("D", "A"),
                LessonBlock.ChordChange("Am", "Dm"),
                LessonBlock.SongLink("etut-e-a-d"),
            ),
        ),
        Lesson(
            "l07", "3. Bölüm · Ritim ve ilk şarkılar", "Ritim kalıpları",
            "Aşağı-yukarı vuruşlarla akorlara hareket kat.",
            listOf(
                t("""
                    Ritim için sayarak çal: "1 ve 2 ve 3 ve 4 ve". Sayılar (1, 2, 3, 4) aşağı vuruş, "ve"ler yukarı vuruştur. El hep aşağı-yukarı sallanır; bazen tellere değer, bazen boşa geçer. Böylece tempo hiç bozulmaz.

                    Klasik gitarda vuruşu başparmağın ya da işaret parmağının tırnağıyla yapabilirsin; mızrap kullanmak da serbest.
                """),
                LessonBlock.Strum("1) Sadece aşağı", "D-D-D-D-", 70),
                LessonBlock.Strum("2) Son vuruşta yukarı ekle", "D-D-D-DU", 70),
                LessonBlock.Strum("3) Klasik pop ritmi", "D-DU-UDU", 70),
                LessonBlock.OpenTool(Tool.METRONOME),
                tip("Önce metronomu 60'a ayarla. Hiç hata yapmadan 4 ölçü çalabiliyorsan 5-10 BPM artır."),
            ),
        ),
        Lesson(
            "l08", "3. Bölüm · Ritim ve ilk şarkılar", "İlk türkün: Üsküdar'a Gider İken",
            "Am, Dm ve E ile tanıdık bir türkü çal.",
            listOf(
                t("""
                    Şarkı ekranında akorlar, değişmeleri gereken hecenin hemen üstünde yazar. Akor değişimini o hecede yap ve bir sonraki değişime kadar ritme devam et.

                    Şarkı ekranındaki "Kaydır" düğmesi metni yavaşça kaydırır, "Metronom" ise şarkının temposunda tık sesi verir. Bir akorun adına dokunursan şemasını görürsün.
                """),
                LessonBlock.Chords(listOf("Am", "Dm", "E")),
                LessonBlock.ChordChange("Am", "Dm"),
                LessonBlock.SongLink("uskudar"),
            ),
        ),
        Lesson(
            "l09", "4. Bölüm · Akor dağarcığı", "C ve G",
            "Şarkıların çoğunda geçen iki akor.",
            listOf(
                t("""
                    Do majör (C) bir merdiven gibidir: 3. parmak 5. tel 3. perde, 2. parmak 4. tel 2. perde, 1. parmak 2. tel 1. perde. 6. teli çalma.

                    Sol majör (G) için 2. parmak 6. tel 3. perde, 1. parmak 5. tel 2. perde, 3. parmak 1. tel 3. perde. Tüm telleri çal.
                """),
                LessonBlock.Chords(listOf("C", "G")),
                LessonBlock.ChordChange("C", "G"),
                LessonBlock.ChordChange("G", "D"),
                LessonBlock.SongLink("kumbaya"),
            ),
        ),
        Lesson(
            "l10", "4. Bölüm · Akor dağarcığı", "Yedili akorlar",
            "G7, D7, E7, A7 ve Am7: renk ve hareket katan akorlar.",
            listOf(
                t("""
                    Yedili akorlar bir sonraki akora "çeken" bir gerilim taşır. Örneğin G7 genellikle C'ye, D7 G'ye, E7 Am'ye çözülür. Çoğu, bildiğin bir akordan bir parmağı kaldırarak ya da kaydırarak çalınır.
                """),
                LessonBlock.Chords(listOf("G7", "D7", "E7", "A7", "Am7")),
                LessonBlock.ChordChange("G", "D7"),
                LessonBlock.ChordChange("G7", "C"),
                LessonBlock.SongLink("iyi-ki-dogdun"),
                LessonBlock.SongLink("amazing-grace"),
            ),
        ),
        Lesson(
            "l11", "5. Bölüm · Melodi ve parmak çalımı", "Tab ile melodi çal",
            "Tek tek notalarla tanıdık melodiler.",
            listOf(
                t("""
                    Şimdi tabları kullanarak melodi çalacağız. Notaları sağ elinde i ve m parmaklarını sırayla kullanarak (i-m-i-m) çal; bu klasik gitar tekniğinin temelidir.

                    Önce ▶ ile dinle, sonra yavaşça kendin çal.
                """),
                LessonBlock.Tab("Neşeye Övgü (Beethoven) – 1. cümle", Tabs.odeToJoy1, 90),
                LessonBlock.Tab("Neşeye Övgü – 2. cümle", Tabs.odeToJoy2, 90),
                LessonBlock.Tab("Daha Uyu Küçük Yıldız (geleneksel)", Tabs.twinkle, 90),
                tip("Sol elde 1. perde için 1. parmağı, 3. perde için 3. parmağı kullan. Böylece el hiç kaymaz."),
            ),
        ),
        Lesson(
            "l12", "5. Bölüm · Melodi ve parmak çalımı", "Arpej: p-i-m-a",
            "Klasik gitarın en güzel sesi: akoru tel tel çalmak.",
            listOf(
                t("""
                    Arpej, akorun notalarını tek tek çalmaktır. Sağ elde her parmağın bir teli vardır:
                    p (başparmak) → bas teller (6, 5 veya 4. tel; akorun kök notası)
                    i (işaret) → 3. tel
                    m (orta) → 2. tel
                    a (yüzük) → 1. tel

                    Aşağıdaki desen: p-i-m-a-m-i.
                """),
                LessonBlock.Tab("Am arpeji (p-i-m-a-m-i)", Tabs.arpeggioAm, 90),
                LessonBlock.Tab("C arpeji", Tabs.arpeggioC, 90),
                LessonBlock.Tab("E arpeji", Tabs.arpeggioE, 90),
                LessonBlock.Tab("Bas + akor (p sonra i-m-a birlikte)", Tabs.pimaChords, 80),
                tip("Akor şeklini önce sol elde bas, sonra arpeje başla. Bas teli başparmakla, diğerlerini parmak uçlarıyla teli avuç içine doğru çekerek çal."),
            ),
        ),
        Lesson(
            "l13", "6. Bölüm · İleri adımlar", "F akoruna giden yol",
            "Önce kolay Fmaj7, sonra barre F.",
            listOf(
                t("""
                    F akoru çoğu yeni başlayanın ilk büyük engelidir, çünkü barre gerektirir: işaret parmağın 1. perdede birden çok teli birden basar.

                    Önce Fmaj7 ile başla. Neredeyse aynı yerde durur ve çoğu şarkıda F yerine kullanılabilir. Rahatladığında barre F'yi dene.
                """),
                LessonBlock.Chords(listOf("Fmaj7", "F")),
                LessonBlock.ChordChange("C", "Fmaj7"),
                LessonBlock.ChordChange("Am", "F"),
                tip("Barre'de işaret parmağını tam düz değil, kemik tarafı tellere gelecek şekilde hafif yana yatır. Gücü elinden değil, kolunun ağırlığından al."),
                LessonBlock.SongLink("aksam-etudu"),
            ),
        ),
        Lesson(
            "l14", "6. Bölüm · İleri adımlar", "Em tonunda çalmak: B7",
            "B7 ile Em tonundaki şarkıları tamamla.",
            listOf(
                t("""
                    Em tonundaki şarkıların çoğu Em, Am, D ve B7 akorlarıyla çalınır. B7 dört parmak gerektirir ama barre içermez.
                """),
                LessonBlock.Chords(listOf("B7", "Em", "Am", "D")),
                LessonBlock.ChordChange("Em", "B7"),
                LessonBlock.SongLink("canakkale"),
                tip("Artık uygulamadaki akor kütüphanesi ve şarkı ekranıyla kendi şarkılarını da çalabilirsin. Bir şarkının akorlarını bulduğunda bilmediğin akoru kütüphanede ara."),
            ),
        ),
    )

    private val byId = all.associateBy { it.id }
    fun get(id: String): Lesson? = byId[id]
}
