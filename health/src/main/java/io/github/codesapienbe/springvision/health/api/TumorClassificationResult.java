package io.github.codesapienbe.springvision.health.api;

import java.util.Map;

/**
 * Represents the result of a brain tumor classification.
 * This class is an immutable Data Transfer Object (DTO) that encapsulates the predicted tumor type,
 * the confidence of the prediction, the probabilities for each possible tumor type, and an optional
 * segmentation mask.
 */
public final class TumorClassificationResult {
    private final TumorType predictedType;
    private final double confidence; // 0..1
    private final Map<TumorType, Double> probabilities; // per-class probabilities
    private final byte[] segmentationMask; // optional (encoded PNG/bytes), may be null

    /**
     * Constructs a new TumorClassificationResult.
     *
     * @param predictedType The most likely tumor type predicted by the model.
     * @param confidence The confidence score (from 0.0 to 1.0) for the predicted type.
     * @param probabilities A map containing the probability for each tumor type.
     * @param segmentationMask An optional byte array representing a segmentation mask of the tumor, typically as an encoded image (e.g., PNG).
     */
    public TumorClassificationResult(TumorType predictedType, double confidence, Map<TumorType, Double> probabilities, byte[] segmentationMask) {
        this.predictedType = predictedType;
        this.confidence = confidence;
        this.probabilities = probabilities;
        this.segmentationMask = segmentationMask == null ? null : segmentationMask.clone();
    }

    /**
     * Gets the predicted tumor type.
     *
     * @return The predicted {@link TumorType}.
     */
    public TumorType getPredictedType() {
        return predictedType;
    }

    /**
     * Gets the confidence of the prediction.
     *
     * @return The confidence score, a value between 0.0 and 1.0.
     */
    public double getConfidence() {
        return confidence;
    }

    /**
     * Gets the probabilities for each tumor type.
     *
     * @return A map where keys are {@link TumorType}s and values are their corresponding probabilities.
     */
    public Map<TumorType, Double> getProbabilities() {
        return probabilities;
    }

    /**
     * Gets the segmentation mask for the detected tumor.
     *
     * @return A byte array representing the segmentation mask, or null if not available. A defensive copy is returned.
     */
    public byte[] getSegmentationMask() {
        return segmentationMask == null ? null : segmentationMask.clone();
    }
}
