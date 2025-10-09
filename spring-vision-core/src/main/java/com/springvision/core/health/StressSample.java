package com.springvision.core.health;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public final class StressSample {
    private final Instant timestamp;
    private final double stressScore;
    private final double confidence;
    private final Map<String, Double> expressionProbabilities;

    public StressSample(Instant timestamp, double stressScore, double confidence, Map<String, Double> expressionProbabilities) {
        this.timestamp = timestamp;
        this.stressScore = stressScore;
        this.confidence = confidence;
        this.expressionProbabilities = expressionProbabilities;
    }

    public Instant getTimestamp() { return timestamp; }
    public double getStressScore() { return stressScore; }
    public double getConfidence() { return confidence; }
    public Optional<Map<String, Double>> getExpressionProbabilities() { return Optional.ofNullable(expressionProbabilities); }
}

