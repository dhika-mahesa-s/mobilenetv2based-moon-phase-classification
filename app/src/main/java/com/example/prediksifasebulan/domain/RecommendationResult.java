package com.example.prediksifasebulan.domain;

import java.util.List;

public class RecommendationResult {
    private final RecommendationLevel recommendationLevel;
    private final int moonPhaseScore;
    private final int environmentScore;
    private final int totalScore;
    private final boolean isDangerous;
    private final List<VetoReason> dangerReasons;

    public RecommendationResult(RecommendationLevel recommendationLevel,
                                int moonPhaseScore,
                                int environmentScore,
                                int totalScore,
                                boolean isDangerous,
                                List<VetoReason> dangerReasons) {
        this.recommendationLevel = recommendationLevel;
        this.moonPhaseScore = moonPhaseScore;
        this.environmentScore = environmentScore;
        this.totalScore = totalScore;
        this.isDangerous = isDangerous;
        this.dangerReasons = dangerReasons;
    }

    public RecommendationLevel getRecommendationLevel() {
        return recommendationLevel;
    }

    public int getMoonPhaseScore() {
        return moonPhaseScore;
    }

    public int getEnvironmentScore() {
        return environmentScore;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public boolean isDangerous() {
        return isDangerous;
    }

    public List<VetoReason> getDangerReasons() {
        return dangerReasons;
    }
}
