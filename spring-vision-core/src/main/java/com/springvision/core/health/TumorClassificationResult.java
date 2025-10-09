package com.springvision.core.health;

import java.util.Map;

public final class TumorClassificationResult {
    private final TumorType predictedType;
    private final double confidence;
    private final Map<TumorType, Double> probabilities;
    private final byte[] segmentationMask;

    public TumorClassificationResult(TumorType predictedType, double confidence, Map<TumorType, Double> probabilities, byte[] segmentationMask) {
        this.predictedType = predictedType;
        this.confidence = confidence;
        this.probabilities = probabilities;
        this.segmentationMask = segmentationMask == null ? null : segmentationMask.clone();
    }

    public TumorType getPredictedType() { return predictedType; }
    public double getConfidence() { return confidence; }
    public Map<TumorType, Double> getProbabilities() { return probabilities; }
    public byte[] getSegmentationMask() { return segmentationMask == null ? null : segmentationMask.clone(); }
}

