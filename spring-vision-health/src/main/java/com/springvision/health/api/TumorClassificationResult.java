package com.springvision.health.api;

import java.util.Map;

/**
 * Result DTO for brain tumor classification.
 */
public final class TumorClassificationResult {
    private final TumorType predictedType;
    private final double confidence; // 0..1
    private final Map<TumorType, Double> probabilities; // per-class probabilities
    private final byte[] segmentationMask; // optional (encoded PNG/bytes), may be null

    public TumorClassificationResult(TumorType predictedType, double confidence, Map<TumorType, Double> probabilities, byte[] segmentationMask) {
        this.predictedType = predictedType;
        this.confidence = confidence;
        this.probabilities = probabilities;
        this.segmentationMask = segmentationMask == null ? null : segmentationMask.clone();
    }

    public TumorType getPredictedType() {
        return predictedType;
    }

    public double getConfidence() {
        return confidence;
    }

    public Map<TumorType, Double> getProbabilities() {
        return probabilities;
    }

    public byte[] getSegmentationMask() {
        return segmentationMask == null ? null : segmentationMask.clone();
    }
}

