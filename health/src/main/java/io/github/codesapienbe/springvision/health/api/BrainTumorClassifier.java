package io.github.codesapienbe.springvision.health.api;

import io.github.codesapienbe.springvision.core.exception.BaseVisionException;
import io.github.codesapienbe.springvision.core.health.MRIImage;
import io.github.codesapienbe.springvision.core.health.TumorClassificationResult;

import java.util.List;
import java.util.Map;

/**
 * Design-only interface for brain tumor classification from MRI scans.
 * Implementations are expected to use `io.github.codesapienbe.springvision.core.health.MRIImage` and
 * return `io.github.codesapienbe.springvision.core.health.TumorClassificationResult` so backends
 * can be wired through `VisionTemplate` capability interfaces.
 */
public interface BrainTumorClassifier {

    /**
     * Identifier for the classifier (model name/version).
     */
    String getModelId();

    /**
     * Classify a single MRI image and return a result with probabilities and metadata.
     */
    TumorClassificationResult classify(MRIImage image) throws BaseVisionException;

    /**
     * Bulk classify a list of MRI images. Implementations may process in parallel.
     */
    List<TumorClassificationResult> classifyBatch(List<MRIImage> images) throws BaseVisionException;

    /**
     * Optional: return model-specific metadata like expected input shape, preprocessing notes.
     */
    Map<String, String> getModelMetadata();
}
