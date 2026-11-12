# Gitar LED kartı (ESP32-S2 Mini + WS2812B)

Telefon, Gitar Öğren uygulamasıyla USB-OTG kablosu üzerinden karta bağlanır. Uygulama hangi perdede hangi tele
basılacağını gönderir, kart da perdelerdeki LED'leri yakar. Gitara takılan iki düğme önceki ve sonraki akora geçirir.

## Malzemeler

| Parça | Not |
|---|---|
| Wemos/Lolin ESP32-S2 Mini (S2FN4R2) | USB-C, yerleşik USB |
| WS2812B 5 V LED şerit (60 LED/m) | Her perdeye 1 LED. Başlangıç için 5 LED yeterli, istediğin kadar eklenebilir |
| 5 V harici güç kaynağı | 1 A, 16 LED'e kadar yeter. Hesap aşağıda |
| 330 Ω direnç | Veri hattına seri |
| 1000 µF / 10 V kondansatör | LED beslemesine, şeridin girişine |
| 2 adet basmalı düğme (anlık) | Önceki ve sonraki akor |
| USB-C OTG kablosu/adaptörü | Telefon ↔ kart |
| (İsteğe bağlı) 74AHCT125 | 3,3 V → 5 V seviye çevirici; LED'ler titrerse |

## Bağlantı şeması

```
 Telefon ──USB-C OTG──► ESP32-S2 Mini (kart beslemesini telefondan alır)

 ESP32-S2 Mini                          WS2812B şerit (1. LED = 1. perde)
 ┌────────────┐
 │ GPIO16 ────┼──[330Ω]────────────────► DIN
 │ GND ───────┼──────────────┬─────────► GND ◄─── 5V kaynak (−)
 │            │              │           5V  ◄─── 5V kaynak (+)
 │ GPIO11 ────┼──[düğme]──┐  │                 (5V ile GND arasına 1000µF, + ucu 5V'a)
 │ GPIO12 ────┼──[düğme]──┤  │
 │            │           └──┘ (düğmelerin diğer ucu GND)
 └────────────┘
```

| Kart pini | Nereye | Görev |
|---|---|---|
| GPIO 16 | 330 Ω üzerinden 1. LED'in DIN ucu | LED verisi |
| GPIO 11 | Düğme → GND | Önceki akor |
| GPIO 12 | Düğme → GND | Sonraki akor |
| GND | Harici 5 V kaynağın (−) ucu ve LED GND | **Ortak toprak şart** |
| 5V / VBUS | **Bağlama** | Harici 5 V'u buraya verme (aşağıya bak) |

Düğmeler için direnç gerekmez, kartın iç pull-up direnci kullanılıyor. Pin numaraları
`GitarLed.ino` dosyasının başında yazıyor, istersen oradan değiştirebilirsin.

## LED yerleşimi

- Şerit **1. perdeden başlar** (sapın başı, eşik tarafı) ve perde sırasıyla gövdeye doğru ilerler. DIN ucu 1. perdede olmalı.
- Her perdede bir LED var. LED'in **rengi hangi tele basılacağını** söyler:
  1. tel (en ince, tutarken en altta) **yeşil**, 2. tel **mavi**, 3. tel **sarı**, 4. tel **mor**, 5. tel **turkuaz**, 6. tel **beyaz**.
- Aynı perdede birden fazla tele basılacaksa (ör. La majörde 2. perde) o LED bu renkler arasında 0,4 saniyede bir döner.
- Boş teller yanmaz. Susturulan teller ekranda kırmızı X ile gösterilir.
- Yeni LED eklerken zincirin sonuna ekle (DOUT → yeni LED'in DIN'i), sonra uygulamada
  **Akorlar → LED → Takılı perde sayısı** değerini artır. Kartı yeniden programlamak gerekmez.
- 60 LED/m şeritte LED'ler 16,7 mm aralıklı. Perde araları bundan geniş olduğu için şeridi her LED'den kesip
  aralara üç telli kısa kablo (5V, GND, DATA) lehimlemen gerekir.

## Güç

- Bir WS2812B tam beyazda en fazla ~60 mA çeker. 12 LED = en kötü durumda 0,72 A, 24 LED = 1,44 A.
- Uygulama aynı anda en fazla 4-5 LED yakıyor ve varsayılan parlaklık 60/255. Gerçek tüketim 100 mA'in altında kalır.
  Yine de kaynağı en kötü duruma göre seç: **5 V 1 A** kaynak 16 LED'e kadar, **5 V 2 A** kaynak 24 LED'e kadar yeter.
- **Harici 5 V'u kartın 5V/VBUS pinine bağlama.** Telefon da o hatta 5 V veriyor, ikisi çakışır ve telefona geri
  akım gidebilir. Kart telefondan, LED'ler harici kaynaktan beslenir. **Yalnızca GND'ler birleştirilir.**
- 1000 µF kondansatörü şeridin girişine, 5V ve GND arasına koy. 330 Ω direnci karta yakın tarafa koy.
- Kart 3,3 V veri çıkışı veriyor. WS2812B çoğu zaman bununla çalışır. LED'ler titrer veya yanlış renk gösterirse
  veri hattına 74AHCT125 (3,3 V → 5 V) seviye çevirici ekle.
- Kart ile ilk LED arasındaki veri kablosunu kısa tut (< 50 cm). Mümkünse GND ile birlikte bükülü çift olarak götür.

## Yazılımı yükleme

### Arduino IDE

1. **Dosya → Tercihler → Ek kart yöneticisi URL'leri** alanına şunu ekle:
   `https://raw.githubusercontent.com/espressif/arduino-esp32/gh-pages/package_esp32_index.json`
2. **Kart yöneticisi**'nden "esp32 by Espressif Systems" paketini kur.
3. **Kütüphane yöneticisi**'nden "Adafruit NeoPixel" kütüphanesini kur.
4. `firmware/GitarLed/GitarLed.ino` dosyasını aç.
5. Kart olarak **LOLIN S2 Mini** seç, **USB CDC On Boot: Enabled** ayarını yap.
6. Kartı bilgisayara tak ve **Yükle**'ye bas.
   Port görünmezse kartı yükleme moduna al: **0** düğmesini basılı tut, **RST**'ye bas, sonra 0'ı bırak.
   Yükleme bitince RST'ye bir kez daha bas.

### PlatformIO

```
cd firmware/GitarLed
pio run -t upload
```

## Çalışma

- **Açılış:** Kart enerjilenince tel renkleri perdeler boyunca bir dalga gibi akar, sonra söner.
- **Bekleme:** Uygulama bağlı değilken 1. perdedeki LED mavi renkte yavaşça nefes alıp verir.
- **Bağlandı:** Uygulama bağlandığında yeşil ışık ortadan iki yana açılır ve iki kez yanıp söner.
- **Test:** Uygulamada **LED → LED testi** her LED'i sırayla 6 tel renginde yakar. Kablolama sırasını kontrol etmek için kullan.
- Kart takılınca Android "Gitar Öğren açılsın mı?" diye sorar. "Her zaman" seçersen bir daha sormaz.

## Haberleşme protokolü

USB seri port (CDC). Her komut tek satırdır ve `\n` ile biter.

| Telefon → kart | Anlamı |
|---|---|
| `?` | Kart `GITARLED 1 <ledSayısı>` cevabını verir |
| `N 12` | LED sayısı |
| `B 60` | Parlaklık (0-255) |
| `S 0=00E040 1=1E64FF/FFC800` | Kare: 0. LED yeşil, 1. LED mavi ile sarı arasında döner. Listede olmayan LED'ler söner |
| `C` | Hepsini söndür |
| `T` | Test animasyonu |
| `A` | "Bağlandı" animasyonu |

| Kart → telefon | Anlamı |
|---|---|
| `K P` | Önceki düğmesine basıldı |
| `K N` | Sonraki düğmesine basıldı |
