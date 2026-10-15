# FuwaNime

Aplikasi Android untuk streaming anime, ditulis ulang dengan **Jetpack Compose + Material 3**.
Fork dari [YuzakiNetwork/fuwanime](https://github.com/YuzakiNetwork/fuwanime).

## Download

APK terbaru ada di [Releases](../../releases). Build otomatis dari setiap commit ada di tab
[Actions](../../actions) → pilih run → bagian *Artifacts*.

## Fitur

- **Beranda**: carousel unggulan, *Lanjutkan Nonton*, anime yang tayang hari ini dan musim ini, terpopuler, pintasan genre
- **Jelajah**: pencarian dengan riwayat, filter genre dan status, urutan populer/A–Z, scroll tanpa batas
- **Jadwal**: tab per hari beserta jam rilis
- **Detail**: daftar episode **lengkap** dalam rentang (1–30, 31–60, …), pencarian nomor episode, tanda sudah ditonton, progres, tombol lanjutkan, favorit
- **Pemutar** (Media3 ExoPlayer):
  - Pilih kualitas dan kecepatan (0.5×–2×), mode pas/isi layar
  - Ketuk dua kali untuk maju/mundur
  - Geser vertikal untuk kecerahan (kiri) dan volume (kanan) saat layar penuh
  - Hitung mundur otomatis ke episode berikutnya
  - Picture-in-Picture (otomatis saat keluar aplikasi)
  - Melanjutkan dari posisi terakhir
- **Koleksi**: riwayat tontonan dengan progres, daftar favorit
- **Pengaturan**: tema sistem/terang/gelap, Material You, hitam AMOLED, kualitas default, opsi pemutar

## Build

Butuh JDK 17+ dan Android SDK 36.

```bash
gradle assembleDebug
```

CI (`.github/workflows/build.yml`) mem-build APK release bertanda tangan di setiap push ke `main`.
Push tag `v*` (misalnya `v2.0.0`) untuk otomatis membuat GitHub Release.
Keystore diambil dari secret repo `KEYSTORE_BASE64` dan `KEYSTORE_PASSWORD`.

## Struktur

```
com.fuwanime
├── data/          repository, model domain
│   ├── remote/    Retrofit API + DTO
│   └── local/     Room (favorit, riwayat, pencarian) + DataStore (pengaturan)
├── ui/            satu paket per layar (Screen + ViewModel), theme, components, navigation
└── util/          format angka/waktu, factory ViewModel
```

## Catatan

Konten berasal dari API pihak ketiga. Aplikasi ini tidak meng-host video apa pun.
