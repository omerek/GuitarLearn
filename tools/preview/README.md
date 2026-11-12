# Ekran önizlemeleri

Uygulamanın Compose ekranlarını masaüstü Compose ile PNG olarak çizer (emülatör gerekmez).
Android'e özgü parçalar (`stubs/`) sahte sürümlerle değiştirilir.

```
../../gradlew -p tools/preview run      # depo kökünden
```

Çıktı: `tools/preview/build/shots/*.png`. GitHub Actions'ta "ekran-goruntuleri" eseri olarak da yüklenir.
