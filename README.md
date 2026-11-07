# Prediksi Fase Bulan

Aplikasi Android (Java, MVVM) untuk klasifikasi fase bulan dari foto, menggabungkan data lingkungan dari Stormglass, dan memberi rekomendasi waktu memancing.

## Prasyarat
- Android Studio (AGP 8.1.0-beta04 sesuai proyek ini)
- Android SDK 34
- Device/emulator Android

## Konfigurasi API
Tambahkan API key Stormglass ke `local.properties`:

```
STORMGLASS_API_KEY=ISI_API_KEY_ANDA
```

## Model ML
Letakkan file berikut di `app/src/main/assets/`:
- `model_moon_phase_3phase.tflite`
- `class_names.txt` (sudah disediakan)

## Struktur Utama
- `data/` untuk Room + Retrofit
- `ml/` untuk TensorFlow Lite
- `domain/` untuk RecommendationEngine
- `ui/` untuk Activity + ViewModel
- `utils/` untuk utilitas lokasi, jaringan, dan waktu

## Catatan
- Jika data cuaca lebih dari 6 jam, aplikasi akan memberi peringatan.
- Semua teks UI menggunakan Bahasa Indonesia.
