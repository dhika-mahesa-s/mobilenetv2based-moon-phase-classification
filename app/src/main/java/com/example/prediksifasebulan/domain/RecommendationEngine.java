package com.example.prediksifasebulan.domain;

import com.example.prediksifasebulan.data.local.entity.WeatherData;

import java.util.ArrayList;
import java.util.List;

public class RecommendationEngine {

    private static final double WAVE_VERY_CALM    = 0.5;   // < 0.5m  → +2
    private static final double WAVE_CALM         = 1.0;   // 0.5–1.0 → +1
    private static final double WAVE_MODERATE     = 2.0;   // 1.0–2.0 → 0
    private static final double WAVE_HIGH         = 3.0;   // 2.0–3.0 → -1 ; >3.0 → VETO
    // WAVE_VETO = WAVE_HIGH (>3.0)

    private static final double WIND_CALM         = 10.0;  // < 10     → +1
    private static final double WIND_MODERATE     = 20.0;  // 10–20    → 0
    private static final double WIND_STRONG       = 40.0;  // 20–40    → -1 ; >40 → VETO
    // WIND_VETO = WIND_STRONG (>40.0)

    private static final double VIS_GOOD          = 9.0;   // > 9km  (~5 nmi)  → +1
    private static final double VIS_MODERATE      = 4.0;   // 4–9km  (~2-5nmi) → 0
    private static final double VIS_POOR          = 1.0;   // 1–4km  (~0.5-2nmi) → -1
    // VIS_VETO = VIS_POOR (<1.0) → VETO

    // Gelombang 2.0–3.0m: output maksimum = NOT_RECOMMENDED (Kurang Direkomendasikan)
    private static final double WAVE_FLOOR_NOT_RECOMMENDED = 2.0;

    // Gelombang 1.5–2.0m: output maksimum = MODERATE (Cukup Baik)
    private static final double WAVE_FLOOR_MODERATE        = 1.5;

    // -------------------------------------------------------------------------
    // Entry point utama
    // -------------------------------------------------------------------------

    public static RecommendationResult getRecommendation(String moonPhase, WeatherData weather) {
        int moonScore = scoreMoonPhase(moonPhase);
        int environmentScore = 0;
        boolean dangerous = false;
        List<VetoReason> dangerReasons = new ArrayList<>();

        if (weather != null) {
            double waveHeight  = weather.getWaveHeight();
            double windSpeed   = weather.getWindSpeed();
            double visibility  = weather.getVisibility();

            // --- Scoring gelombang ---
            if (waveHeight > WAVE_HIGH) {
                dangerous = true;
                dangerReasons.add(VetoReason.WAVE_HEIGHT_EXCEEDED);
            } else if (waveHeight >= WAVE_MODERATE) {
                // 2.0 – 3.0 m
                environmentScore -= 1;
            } else if (waveHeight >= WAVE_CALM) {
                // 1.0 – 2.0 m
                environmentScore += 0;
            } else if (waveHeight >= WAVE_VERY_CALM) {
                // 0.5 – 1.0 m
                environmentScore += 1;
            } else {
                // < 0.5 m
                environmentScore += 2;
            }

            // --- Scoring angin ---
            if (windSpeed > WIND_STRONG) {
                dangerous = true;
                dangerReasons.add(VetoReason.WIND_SPEED_EXCEEDED);
            } else if (windSpeed >= WIND_MODERATE) {
                // 20 – 40 km/h
                environmentScore -= 1;
            } else if (windSpeed >= WIND_CALM) {
                // 10 – 20 km/h
                environmentScore += 0;
            } else {
                // < 10 km/h
                environmentScore += 1;
            }

            // --- Scoring visibilitas ---
            if (visibility < VIS_POOR) {
                // < 1 km — VETO baru berdasarkan Liu dkk. (2024) & USCG standard
                dangerous = true;
                dangerReasons.add(VetoReason.LOW_VISIBILITY);
            } else if (visibility < VIS_MODERATE) {
                // 1 – 4 km
                environmentScore -= 1;
            } else if (visibility < VIS_GOOD) {
                // 4 – 9 km
                environmentScore += 0;
            } else {
                // >= 9 km
                environmentScore += 1;
            }
        }

        int totalScore = moonScore + environmentScore;

        RecommendationLevel level;
        if (dangerous) {
            level = RecommendationLevel.DANGEROUS;
        } else if (totalScore >= 4) {
            level = RecommendationLevel.HIGHLY_RECOMMENDED;
        } else if (totalScore >= 2) {
            level = RecommendationLevel.RECOMMENDED;
        } else if (totalScore >= 0) {
            level = RecommendationLevel.MODERATE;
        } else {
            level = RecommendationLevel.NOT_RECOMMENDED;
        }

        if (!dangerous && weather != null) {
            double waveHeight = weather.getWaveHeight();

            if (waveHeight >= WAVE_FLOOR_NOT_RECOMMENDED) {
                // Gelombang 2.0–3.0m: level tidak boleh lebih baik dari NOT_RECOMMENDED
                if (level == RecommendationLevel.HIGHLY_RECOMMENDED
                        || level == RecommendationLevel.RECOMMENDED
                        || level == RecommendationLevel.MODERATE) {
                    level = RecommendationLevel.NOT_RECOMMENDED;
                    dangerReasons.add(VetoReason.WAVE_RISK);
                }
            } else if (waveHeight >= WAVE_FLOOR_MODERATE) {
                // Gelombang 1.5–2.0m: level tidak boleh lebih baik dari MODERATE
                if (level == RecommendationLevel.HIGHLY_RECOMMENDED
                        || level == RecommendationLevel.RECOMMENDED) {
                    level = RecommendationLevel.MODERATE;
                    dangerReasons.add(VetoReason.WAVE_MODERATE_CEILING);
                }
            }
        }

        return new RecommendationResult(
                level, moonScore, environmentScore, totalScore,
                dangerous, dangerReasons
        );
    }

    private static int scoreMoonPhase(String moonPhase) {
        if (moonPhase == null) return 0;
        switch (moonPhase) {
            case "New_Moon":
            case "Wanning_Cresent_Moon":
                return 2;
            case "Waxing_Cresent_Moon":
            case "Third_Quarter_Moon":
                return 1;
            case "First_Quarter_Moon":
            case "Waxing_Gibbous_Moon":
            case "Full_Moon":          // revisi: 0 (sebelumnya -1)
                return 0;
            case "Waning_Gibbous_Moon":
                return -1;
            default:
                return 0;
        }
    }

}
