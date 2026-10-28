# G-Spoof Pro

Aplikasi Android untuk mengatur lokasi GPS (mock location) dengan peta penuh layar,
tampilan mirip Google Maps, dan panel alamat bergaya Indonesia.

Dibangun dari nol dengan Kotlin + Jetpack Compose. Bukan modifikasi atau repack
aplikasi lain.

## Fitur

- **Peta penuh layar** — osmdroid, 4 gaya tile (Google-style, Esri, OSM, gelap)
- **Set lokasi** — tap peta atau long-press untuk menaruh pin
- **Pencarian alamat** — Nominatim (OpenStreetMap)
- **Reverse geocoding** — format alamat Indonesia: `Jl. Nama No.X, Kecamatan Y, Provinsi`
- **Simulasi rute** — polyline, arah/bearing, kecepatan 0–120 km/h
- **Mode realistis** — akurasi acak 2.4–6.8 m, jitter ±2 m, interval fix 800–1200 ms,
  altitude bervariasi, noise pada vertical/speed/bearing accuracy
- **Foreground service** — injeksi tetap jalan saat app di background
- **Bookmark** — simpan, ganti nama, hapus lokasi favorit
- **Splash screen** — animasi pembuka

## Cara build

### GitHub Actions (otomatis)

Setiap push ke `main` otomatis build debug + release. Hasil APK ada di tab
[Actions](../../actions) sebagai artifact (30 hari).

### Lokal (Termux / Android)

```bash
export ANDROID_HOME=$HOME/android-sdk
export JAVA_HOME=$PREFIX/lib/jvm/java-17-openjdk   # JDK 21 gagal di jlink
./gradlew assembleDebug --no-daemon
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

Catatan Termux: Gradle OOM di atas heap ~768 MB. Tuning khusus device
(`org.gradle.jvmargs`, `aapt2FromMavenOverride`) ditaruh di
`~/.gradle/gradle.properties` — **tidak** di repo, supaya CI tetap jalan normal.

### Signing

Build CI menandatangani APK dengan keystore dari Actions secret
(`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`).
Tanpa secret, APK tetap dibuild tapi tidak ditandatangani.

## Struktur

```
app/src/main/java/com/gspoof/pro/
├── MockEngine.kt      # injeksi lokasi (GPS/NETWORK/fused test provider)
├── SpoofService.kt    # foreground service
├── SpoofBridge.kt     # state global + callback engine
├── AddressFormat.kt   # formatter alamat gaya Indonesia
├── MainActivity.kt    # entry point + splash gate
└── ui/
    ├── MapScreen.kt   # peta, pin, bookmark, rute, panel alamat
    ├── SplashScreen.kt
    └── Theme.kt
```

## Lisensi

Kode proyek ini dilisensikan di bawah **MIT License** — lihat [LICENSE](LICENSE).

## Komponen pihak ketiga

| Komponen | Lisensi | Keterangan |
|---|---|---|
| [osmdroid](https://github.com/osmdroid/osmdroid) | Apache 2.0 | render peta |
| AndroidX / Jetpack Compose | Apache 2.0 | UI toolkit |
| Kotlin & kotlinx.coroutines | Apache 2.0 | bahasa & concurrency |
| Data peta © [OpenStreetMap](https://www.openstreetmap.org/copyright) contributors | ODbL | tile & pencarian alamat |
| Tile raster Google (`mt1.google.com`) | — | endpoint tidak resmi, lihat catatan |

### Catatan atribusi

Peta memakai data OpenStreetMap. Untuk distribusi publik, atribusi
**"© OpenStreetMap contributors"** wajib tampil di layar peta.

Tile dari `mt1.google.com` adalah endpoint tidak resmi dan tidak termasuk lisensi
proyek ini. Untuk penggunaan publik sebaiknya ganti ke penyedia tile resmi
(MapTiler, Stadia Maps, Thunderforest) atau self-host.

## Disclaimer

Aplikasi ini ditujukan untuk pengujian, pengembangan, dan penggunaan pribadi pada
perangkat sendiri. Mock location dapat terdeteksi oleh aplikasi lain
(`Location.isMock()`), dan penggunaan untuk menipu sistem absensi/pelaporan
institusi bisa melanggar kebijakan maupun hukum yang berlaku. Tanggung jawab
penggunaan ada pada pengguna.
