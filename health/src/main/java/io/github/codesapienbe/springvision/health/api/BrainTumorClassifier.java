package io.github.codesapienbe.springvision.health.api;

import io.github.codesapienbe.springvision.core.exception.BaseVisionException;
import io.github.codesapienbe.springvision.core.health.MRIImage;
import io.github.codesapienbe.springvision.core.health.TumorClassificationResult;

import java.util.List;
import java.util.Map;

/**
 * Defines the contract for a brain tumor classification system that analyzes MRI scans.
 * <p>
 * This interface is intended for backends that can take an {@link MRIImage} and classify
 * whether it contains a tumor, and if so, what type. Implementations are expected to use
 * deep learning models trained on medical imaging data. To ensure compatibility with the
 * core framework, implementations should use the specified input and output types, allowing
 * them to be integrated as capabilities within the {@code VisionTemplate}.
 *
 * @author Spring Vision Team
 * @since 1.1.0
 */
public interface BrainTumorClassifier {

    /**
     * Returns a unique identifier for the classification model being used, which may
     * include its name and version (e.g., "tumor-classifier-v2.1").
     *
     * @return The model identifier.
     */
    String getModelId();

    /**
     * Classifies a single MRI image to determine the presence and type of a brain tumor.
     *
     * @param image The {@link MRIImage} to be classified.
     * @return A {@link TumorClassificationResult} containing the classification, probabilities,
     *         and any additional metadata.
     * @throws BaseVisionException if an error occurs during the classification process.
     */
    TumorClassificationResult classify(MRIImage image) throws BaseVisionException;

    /**
     * Classifies a batch of MRI images. Implementations of this method may leverage
     * parallel processing to handle multiple images efficiently.
     *
     * @param images A list of {@link MRIImage}s to be classified.
     * @return A list of {@link TumorClassificationResult}s, one for each image in the batch.
     * @throws BaseVisionException if an error occurs during the batch classification process.
     */
    List<TumorClassificationResult> classifyBatch(List<MRIImage> images) throws BaseVisionException;

    /**
     * Returns a map of metadata about the classification model, which can be useful
     * for understanding its requirements and capabilities. This may include information
     * like the expected input image dimensions, preprocessing steps, or the set of
     * labels it can predict.
     *
     * @return A map containing model-specific metadata.
     */
    Map<String, String> getModelMetadata();
}
