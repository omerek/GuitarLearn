/*
 * Gitar LED kartı — Wemos/Lolin ESP32-S2 Mini (USB) ya da ESP32-S3FH4R2 kartları (USB + Bluetooth)
 *
 * Telefon (Gitar Öğren uygulaması) USB-OTG ile bağlanır; kart USB seri port (CDC) olarak görünür.
 * ESP32-S3'te ayrıca Bluetooth Low Energy (Nordic UART servisi, adı "GitarLED") ile kablosuz bağlanılır.
 * Aynı kod iki kart için de derlenir; kart türü derleyici tarafından seçilir.
 * Uygulama hangi perdede hangi telin basılacağını gönderir, kart WS2812B LED'leri yakar.
 * Gitara takılı iki düğme uygulamaya "önceki / sonraki akor" bilgisini yollar.
 *
 * Komutlar (her biri bir satır, '\n' ile biter):
 *   ?                          -> "GITARLED 1 <ledSayısı>" cevabı
 *   N <sayı>                   -> LED sayısı (1..MAX_LEDS)
 *   B <0..255>                 -> parlaklık
 *   C                          -> hepsini söndür
 *   S 0=00E040 1=1E64FF/FFC800 -> kareyi ayarla; '/' ile ayrılan renkler sırayla döner
 *   T                          -> test animasyonu
 *   A                          -> "bağlandı" animasyonu
 * Karttan giden:
 *   K P  /  K N                -> önceki / sonraki düğmesine basıldı
 */
#include <Adafruit_NeoPixel.h>

// ---- Bağlantılar (README'deki şemaya bak) ----
#if CONFIG_IDF_TARGET_ESP32S3
#define HAS_BLE 1
const int LED_PIN = 4;         // WS2812B DIN (330 ohm direnç üzerinden)
const int BTN_PREV_PIN = 5;    // önceki akor düğmesi -> GND
const int BTN_NEXT_PIN = 6;    // sonraki akor düğmesi -> GND
const int BOARD_LED_PIN = -1;  // S3 kartlarının yerleşik LED'i farklı; kullanılmıyor
#else
#define HAS_BLE 0
const int LED_PIN = 16;        // WS2812B DIN (330 ohm direnç üzerinden)
const int BTN_PREV_PIN = 11;   // önceki akor düğmesi -> GND
const int BTN_NEXT_PIN = 12;   // sonraki akor düğmesi -> GND
const int BOARD_LED_PIN = 15;  // kart üzerindeki mavi LED
#endif

#if HAS_BLE
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLE2902.h>
#include <freertos/queue.h>

#define BLE_NAME "GitarLED"
#define UART_SERVICE "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
#define UART_RX "6e400002-b5a3-f393-e0a9-e50e24dcca9e"  // telefon -> kart
#define UART_TX "6e400003-b5a3-f393-e0a9-e50e24dcca9e"  // kart -> telefon

BLECharacteristic *bleTx = nullptr;
volatile bool bleConnected = false;
volatile bool bleJustDisconnected = false;
QueueHandle_t bleQueue;  // Bluetooth'tan gelen baytlar (BLE görevi -> loop)

class ServerEvents : public BLEServerCallbacks {
  void onConnect(BLEServer *) override { bleConnected = true; }
  void onDisconnect(BLEServer *) override {
    bleConnected = false;
    bleJustDisconnected = true;
  }
};

class RxEvents : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic *c) override {
    auto v = c->getValue();
    for (size_t i = 0; i < v.length(); i++) {
      char ch = v[i];
      xQueueSend(bleQueue, &ch, 0);
    }
  }
};

void setupBle() {
  bleQueue = xQueueCreate(2048, 1);
  BLEDevice::init(BLE_NAME);
  BLEDevice::setMTU(185);
  BLEServer *server = BLEDevice::createServer();
  server->setCallbacks(new ServerEvents());
  BLEService *service = server->createService(UART_SERVICE);
  bleTx = service->createCharacteristic(UART_TX, BLECharacteristic::PROPERTY_NOTIFY);
  bleTx->addDescriptor(new BLE2902());
  BLECharacteristic *rx = service->createCharacteristic(
      UART_RX, BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_WRITE_NR);
  rx->setCallbacks(new RxEvents());
  service->start();
  BLEAdvertising *adv = BLEDevice::getAdvertising();
  adv->addServiceUUID(UART_SERVICE);
  adv->setScanResponse(true);
  BLEDevice::startAdvertising();
}
#else
const bool bleConnected = false;
#endif

const int MAX_LEDS = 150;
const int MAX_COLORS = 6;
const unsigned long CYCLE_MS = 400;  // aynı perdede birden fazla tel varsa renk değiştirme aralığı

Adafruit_NeoPixel strip(MAX_LEDS, LED_PIN, NEO_GRB + NEO_KHZ800);

int numLeds = 12;
uint8_t brightness = 60;

uint32_t colors[MAX_LEDS][MAX_COLORS];
uint8_t colorCount[MAX_LEDS];

unsigned long lastCycle = 0;
uint8_t phase = 0;
bool dirty = true;
bool wasConnected = false;

String line;

struct Button {
  int pin;
  const char *msg;
  bool pressed;
  unsigned long changedAt;
};
Button buttons[2] = {{BTN_PREV_PIN, "K P", false, 0}, {BTN_NEXT_PIN, "K N", false, 0}};

// ---------------------------------------------------------------------------
// Yardımcılar

// Telefona bir satır gönderir (bağlı olan her yoldan)
void sendLine(const char *text) {
  if (Serial) Serial.println(text);
#if HAS_BLE
  if (bleConnected && bleTx) {
    String s = String(text) + "\n";
    bleTx->setValue((uint8_t *)s.c_str(), s.length());
    bleTx->notify();
  }
#endif
}

uint32_t scaleColor(uint32_t c, float k) {
  uint8_t r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
  return strip.Color((uint8_t)(r * k), (uint8_t)(g * k), (uint8_t)(b * k));
}

void clearFrame() {
  for (int i = 0; i < MAX_LEDS; i++) colorCount[i] = 0;
  dirty = true;
}

void render() {
  strip.setBrightness(brightness);
  strip.clear();
  for (int i = 0; i < numLeds; i++) {
    if (colorCount[i] == 0) continue;
    uint32_t c = colors[i][phase % colorCount[i]];
    strip.setPixelColor(i, (c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF);
  }
  strip.show();
  dirty = false;
}

// Tel renkleri (uygulamayla aynı): 1 yeşil, 2 mavi, 3 sarı, 4 mor, 5 turkuaz, 6 beyaz
const uint32_t STRING_COLORS[6] = {0x00E040, 0x1E64FF, 0xFFC800, 0xC000FF, 0x00D8D8, 0xFFFFFF};

// ---------------------------------------------------------------------------
// Animasyonlar

// Açılış: tel renkleri perde perde ilerleyen bir dalga, sonra yavaşça söner
void bootAnimation() {
  strip.setBrightness(brightness);
  for (int step = 0; step < numLeds + 6; step++) {
    strip.clear();
    for (int t = 0; t < 6; t++) {
      int i = step - t;
      if (i >= 0 && i < numLeds) strip.setPixelColor(i, scaleColor(STRING_COLORS[t], 1.0f - t * 0.12f));
    }
    strip.show();
    delay(45);
  }
  for (int k = 10; k >= 0; k--) {
    for (int i = 0; i < numLeds; i++) strip.setPixelColor(i, scaleColor(STRING_COLORS[i % 6], k / 10.0f * 0.5f));
    strip.show();
    delay(35);
  }
  strip.clear();
  strip.show();
}

// Bağlandı: ortadan dışa yeşil açılır, iki kez yanıp söner
void connectedAnimation() {
  strip.setBrightness(brightness);
  int mid = numLeds / 2;
  for (int r = 0; r <= mid + 1; r++) {
    strip.clear();
    for (int i = 0; i < numLeds; i++) if (abs(i - mid) <= r) strip.setPixelColor(i, 0x00E040);
    strip.show();
    delay(40);
  }
  for (int n = 0; n < 2; n++) {
    strip.clear(); strip.show(); delay(120);
    strip.fill(0x00E040, 0, numLeds); strip.show(); delay(120);
  }
  strip.clear();
  strip.show();
  dirty = true;
}

// Test: her LED sırayla 6 tel rengini gösterir (kablolama sırasını kontrol etmek için)
void testAnimation() {
  strip.setBrightness(brightness);
  for (int t = 0; t < 6; t++) {
    for (int i = 0; i < numLeds; i++) {
      strip.clear();
      strip.setPixelColor(i, STRING_COLORS[t]);
      strip.show();
      delay(30);
    }
  }
  strip.fill(0xFFFFFF, 0, numLeds);
  strip.show();
  delay(400);
  dirty = true;
}

// Bağlantı beklerken 1. perdedeki LED mavi nefes alır
void idleBreath() {
  float k = (sin(millis() / 600.0f) + 1.0f) * 0.5f;
  strip.setBrightness(brightness);
  strip.clear();
  strip.setPixelColor(0, scaleColor(0x1E64FF, 0.08f + 0.5f * k));
  strip.show();
}

// ---------------------------------------------------------------------------
// Komutlar

void parseFrame(const String &s) {
  clearFrame();
  int pos = 1;  // "S" sonrası
  while (pos < (int)s.length()) {
    while (pos < (int)s.length() && s[pos] == ' ') pos++;
    int end = s.indexOf(' ', pos);
    if (end < 0) end = s.length();
    String tok = s.substring(pos, end);
    pos = end;
    int eq = tok.indexOf('=');
    if (eq <= 0) continue;
    int idx = tok.substring(0, eq).toInt();
    if (idx < 0 || idx >= MAX_LEDS) continue;
    String rest = tok.substring(eq + 1);
    uint8_t n = 0;
    int start = 0;
    while (start < (int)rest.length() && n < MAX_COLORS) {
      int slash = rest.indexOf('/', start);
      if (slash < 0) slash = rest.length();
      colors[idx][n++] = strtoul(rest.substring(start, slash).c_str(), nullptr, 16);
      start = slash + 1;
    }
    colorCount[idx] = n;
  }
  phase = 0;
  lastCycle = millis();
  dirty = true;
}

void handleLine(String s) {
  s.trim();
  if (s.length() == 0) return;
  char cmd = s[0];
  switch (cmd) {
    case '?': {
      char buf[32];
      snprintf(buf, sizeof(buf), "GITARLED 1 %d", numLeds);
      sendLine(buf);
      break;
    }
    case 'N': {
      int n = s.substring(1).toInt();
      if (n >= 1 && n <= MAX_LEDS) {
        strip.clear(); strip.show();
        numLeds = n;
        dirty = true;
      }
      break;
    }
    case 'B':
      brightness = constrain(s.substring(1).toInt(), 0, 255);
      dirty = true;
      break;
    case 'C':
      clearFrame();
      break;
    case 'S':
      parseFrame(s);
      break;
    case 'T':
      testAnimation();
      break;
    case 'A':
      connectedAnimation();
      break;
  }
}

void feedChar(String &buf, char c) {
  if (c == '\n') {
    handleLine(buf);
    buf = "";
  } else if (buf.length() < 2000) {
    buf += c;
  }
}

void readInputs() {
  while (Serial.available()) feedChar(line, Serial.read());
#if HAS_BLE
  static String bleLine;
  char c;
  while (xQueueReceive(bleQueue, &c, 0) == pdTRUE) feedChar(bleLine, c);
  if (bleJustDisconnected) {
    bleJustDisconnected = false;
    bleLine = "";
    delay(200);
    BLEDevice::startAdvertising();  // yeniden bağlanılabilsin
  }
#endif
}

void readButtons() {
  unsigned long now = millis();
  for (auto &b : buttons) {
    bool down = digitalRead(b.pin) == LOW;
    if (down != b.pressed && now - b.changedAt > 30) {  // 30 ms parazit süzgeci
      b.pressed = down;
      b.changedAt = now;
      if (down) sendLine(b.msg);
      if (BOARD_LED_PIN >= 0) digitalWrite(BOARD_LED_PIN, down ? HIGH : LOW);
    }
  }
}

// ---------------------------------------------------------------------------

void setup() {
  pinMode(BTN_PREV_PIN, INPUT_PULLUP);
  pinMode(BTN_NEXT_PIN, INPUT_PULLUP);
  if (BOARD_LED_PIN >= 0) pinMode(BOARD_LED_PIN, OUTPUT);
  Serial.begin(115200);
  strip.begin();
  strip.setBrightness(brightness);
  strip.clear();
  strip.show();
  clearFrame();
  bootAnimation();
#if HAS_BLE
  setupBle();
#endif
}

void loop() {
  readInputs();
  readButtons();

  // USB'de uygulama portu açınca (DTR) ya da Bluetooth bağlanınca true olur
  bool connected = (bool)Serial || bleConnected;
  if (!connected) {
    if (wasConnected) clearFrame();
    wasConnected = false;
    idleBreath();
    delay(15);
    return;
  }
  wasConnected = true;

  if (millis() - lastCycle >= CYCLE_MS) {
    lastCycle = millis();
    phase++;
    for (int i = 0; i < numLeds; i++) if (colorCount[i] > 1) { dirty = true; break; }
  }
  if (dirty) render();
  delay(2);
}
