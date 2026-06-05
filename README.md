# 🌙 Moon Phase Classification with MobileNetV2

> Undergraduate thesis project — Moon phase classification system using transfer learning, deployed as an Android app for fishing schedule recommendations targeting traditional Indonesian fishermen.

---

## 📌 Overview

This project builds an end-to-end system that classifies moon phases from images using a fine-tuned MobileNetV2 model, then combines the classification result with real-time ocean environmental data to generate fishing schedule recommendations. The system is deployed as an offline-first Android application.

**Thesis Title:**
Klasifikasi Fase Bulan Berbasis Convolutional Neural Network (CNN) dengan Arsitektur MobileNetV2 untuk Optimalisasi Jadwal Penangkapan Ikan

---

## 🌕 Moon Phase Classes

The model classifies images into **8 moon phase categories**:

| Class | Label |
|---|---|
| New Moon | `New_Moon` |
| Waxing Crescent | `Waxing_Cresent_Moon` |
| First Quarter | `First_Quarter_Moon` |
| Waxing Gibbous | `Waxing_Gibbous_Moon` |
| Full Moon | `Full_Moon` |
| Waning Gibbous | `Waning_Gibbous_Moon` |
| Third Quarter | `Third_Quarter_Moon` |
| Waning Crescent | `Wanning_Cresent_Moon` |

---

## 📊 Dataset

- **Source:** Roboflow Universe (4 project repositories, 6 versions combined)
- **Deduplication:** MD5 hash-based duplicate removal → **2,905 unique images**
- **Split Strategy:** Stratified split 70:20:10

| Split | Count |
|---|---|
| Train | 2,033 |
| Validation | 581 |
| Test | 291 |

---

## 🧠 Model Architecture

- **Backbone:** MobileNetV2 pretrained on ImageNet (`include_top=False`)
- **Input size:** 224×224×3 (RGB)
- **Classification Head:**

```
MobileNetV2 (frozen/partially unfrozen)
    ↓
GlobalAveragePooling2D       → 1280-dim vector
BatchNormalization
Dropout(0.20)
Dense(256, activation='relu', L2=0.001)
BatchNormalization
Dropout(0.15)
Dense(8, activation='softmax')  → 8 moon phase classes
```

---

## 🏋️ Training Strategy

Training was conducted in **3 progressive fine-tuning phases**:

| | Phase 1 | Phase 2 | Phase 3 |
|---|---|---|---|
| **Strategy** | Feature extraction | Fine-tuning | Deep fine-tuning |
| **Frozen layers** | All backbone | All except last 20 | All except last 70 |
| **Learning rate** | 1e-3 (Cosine Annealing) | 2e-5 | 1e-5 |
| **Epochs** | 30 | 30 | 20 |
| **Augmentation** | Moderate | Moderate | Light |

**Key design decisions:**
- `RandomFlip` augmentation was intentionally **removed** — horizontal/vertical flipping corrupts label semantics (e.g., flipping Waxing Crescent produces a Waning Crescent appearance)
- Phase 2 & 3 used `optimizer.learning_rate.assign()` instead of recompiling to **preserve Adam optimizer state**
- Batch Normalization layers were explicitly frozen during backbone unfreeze to prevent representation corruption

---

## 📈 Results

### Overall Performance

| Metric | Value |
|---|---|
| Validation Accuracy (Phase 3) | **91.91%** |
| Test Accuracy | **88.32%** |
| Macro F1-Score | **0.88** |

### Per-Class F1-Score

| Class | F1-Score |
|---|---|
| Wanning Crescent | **0.95** |
| Third Quarter | 0.93 |
| Waxing Crescent | 0.92 |
| First Quarter | 0.89 |
| New Moon | 0.87 |
| Waning Gibbous | 0.86 |
| Waxing Gibbous | 0.85 |
| Full Moon | 0.80 |

> Full Moon achieved the lowest F1-score, primarily due to visual similarity with Waning Gibbous — both phases display a nearly fully illuminated lunar disk with subtle differences.

---

## 📱 Android Application

The model is deployed as an offline-first Android app targeting traditional Indonesian fishermen.

**Tech Stack:**

| Component | Technology |
|---|---|
| Language | Java |
| Architecture | MVVM |
| ML Inference | TensorFlow Lite (.tflite) |
| Local Storage | Room Database |
| Environmental Data | Stormglass.io API |

**App Flow:**

```
Photo of moon
    ↓
Resize to 224×224 → Normalize to [-1, 1]
    ↓
TFLite inference (offline)
    ↓
Moon phase label + confidence score
    ↓
Combined with environmental data (wave height, wind speed, visibility)
    ↓
Rule-based Recommendation Engine
    ↓
Fishing schedule recommendation
```

**Recommendation levels:** 🟢 Sangat Direkomendasikan · 🔵 Direkomendasikan · 🟡 Cukup Baik · 🟠 Kurang Direkomendasikan · 🔴 Berbahaya

---

## 🚀 How to Run

### Requirements

- Python 3.8+
- TensorFlow 2.x
- Google Colab (recommended) or local GPU environment

### Steps

1. **Clone this repository**
   ```bash
   git clone https://github.com/YOUR_USERNAME/moon-phase-classification.git
   cd moon-phase-classification
   ```

2. **Install dependencies**
   ```bash
   pip install -r requirements.txt
   ```

3. **Prepare the dataset**
   - Download from Roboflow Universe (look at DATASET.MD for the links and how to download them)
   - Place in the `dataset/` directory following the structure below

4. **Run the notebook**
   - Open `moon_phase_classification.ipynb` in Jupyter or Google Colab
   - Run all cells sequentially
   - Training checkpoints will be saved to `models/`

5. **Export to TFLite**
   - The final cell converts the trained model to `.tflite` format for Android deployment

---

## 📁 Project Structure

```
moon-phase-classification/
│
├── moon_phase_classification.ipynb   # Main training notebook
├── requirements.txt                  # Python dependencies
├── README.md
│
├── dataset/                          # Dataset directory (not included in repo)
│   ├── train/
│   ├── val/
│   └── test/
│
└── models/                           # Saved models
    ├── best_model_phase1.keras
    ├── best_model_phase2.keras
    ├── best_model_phase3.keras
    └── moon_phase_model.tflite       # Final TFLite model for Android
```

> **Note:** The `dataset/` folder is not included in this repository due to size constraints. Please follow the dataset preparation steps in the notebook.

---

## 📚 Key References

| Reference | Topic |
|---|---|
| Sandler et al. (2018) | MobileNetV2 architecture |
| Howard et al. (2017) | Depthwise separable convolutions |
| Ioffe & Szegedy (2015) | Batch Normalization |
| Kingma & Ba (2015) | Adam optimizer |
| Srivastava et al. (2014) | Dropout regularization |
| Müller et al. (2019) | Label smoothing |
| Zeng (2024) | Data augmentation survey |
| Ciaglia et al. (2022) | Roboflow benchmark |
| Lee et al. (2019) | TFLite mobile inference |
| Iqbal et al. (2023) | Traditional fishing vessel safety |
| Irvana et al. (2024) | Wave height operational limits |
| Kurup et al. (2024) | Wind speed safety thresholds |
| Liu et al. (2024) | Visibility and maritime collision |
| Suhendar et al. (2016) | Moon phase effect on fish catch |
| Wicaksono et al. (2025) | Tidal influence on fishing productivity |

---

## 👤 Author

**Dhika Mahesa Syaputra**
Undergraduate Thesis — System Information
Riau University · 2026

---

## 📄 License

This project is created for academic purposes. Please contact the author before using any part of this work.
