# Gitar Öğren 🎸

Klasik (ve akustik) gitar için sade bir Android uygulaması: sıfırdan başlayıp bir şarkının akorlarına bakarak çalabilecek seviyeye getiren dersler ve mikrofonlu akort edici.

## Özellikler

- **Akort edici:** Mikrofonla telin perdesini ölçer (YIN algoritması), otomatik tel tanıma, ±sent göstergesi, her tel için referans ses. Standart akort: Mi-La-Re-Sol-Si-Mi.
- **14 adımlık ders yolu:** gitarı tanıma → akort → şema/tab okuma → Em, Am, E, A, D, Dm, C, G → ritim kalıpları → yedili akorlar → tab ile melodi → p-i-m-a arpej → F/Fmaj7 → B7. İlerleme telefonda saklanır.
- **1 dakikalık akor geçişi egzersizi:** rekorların kaydedilir.
- **Akor kütüphanesi:** 18 akor; şema, parmak numaraları, ipucu ve sesli dinleme.
- **Şarkılar:** akorlar ilgili hecenin üstünde; otomatik kaydırma, şarkı temposunda metronom, akora dokununca şema. Yalnızca anonim türküler, kamu malı şarkılar ve özgün etütler.
- **Şarkıda takip modu:** Sıradaki akor ekranda ve gitardaki LED'lerde görünür. Mikrofon tele vuruşu dinler, doğru akoru duyunca sonraki akora geçer. Elle ya da gitardaki düğmelerle de geçilebilir.
- **Gitar LED'leri (isteğe bağlı):** Perdelere takılan WS2812B LED'ler, USB-OTG ile bağlanan ESP32-S2 Mini üzerinden hangi perdede hangi tele basılacağını gösterir. Her telin kendi rengi vardır. Kablolama, güç ve kart yazılımı için [firmware/README.md](firmware/README.md) dosyasına bak.
- **Metronom:** 40–220 BPM, 2/4–6/4, dokunarak tempo bulma.
- İnternet, hesap ya da reklam yok.

## APK'yı telefona kurma

1. Depodaki **Releases → "Gitar Öğren – son sürüm"** sayfasından `GitarOgren.apk` dosyasını telefonda indir.
   (Ya da **Actions** sekmesinde son başarılı çalışmanın "GitarOgren-apk" eserini indir.)
2. Dosyayı aç; Android "bilinmeyen kaynaklardan yükleme" izni isterse ver.
3. Akort ekranında mikrofon iznini ver.

## Geliştirme

- Kotlin + Jetpack Compose, minSdk 24 (Android 7.0), targetSdk 35.
- Android Studio ile aç ya da `./gradlew assembleDebug`.
- Birim testleri: `./gradlew testDebugUnitTest` (perde tespiti, içerik tutarlılığı, şarkı/tab ayrıştırıcıları).

```
app/src/main/java/com/omerek/guitarlearn/
├── audio/   PitchDetector (YIN), TunerEngine (mikrofon), Synth, SoundPlayer, Metronome
├── data/    Chords, Lessons, Songs, Tabs, SongParser, TabParser, Progress
└── ui/      GuitarApp (alt menü), screens/, components/ (akor şeması, tab, vuruş kalıbı…)
```

### Şarkı ekleme

`data/Songs.kt` içine yeni bir `Song` ekle. Biçim: `# Bölüm adı`, akorlar köşeli parantezde ilgili hecenin önünde: `[Am]Üsküdar'a gider iken [Dm]aldı da bir yağ[Am]mur`. Kullanılan her akorun `Chords.kt` içinde olması gerekir (test bunu denetler). Telifli şarkı sözü eklememeye dikkat et.
