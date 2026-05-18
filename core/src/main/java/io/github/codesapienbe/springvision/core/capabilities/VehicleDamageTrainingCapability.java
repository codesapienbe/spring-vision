package io.github.codesapienbe.springvision.core.capabilities;

import java.util.Map;

import io.github.codesapienbe.springvision.core.BoundingBox;
import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;

/**
 * Capability interface for on-the-go vehicle damage classifier training.
 *
 * <p>Implementations maintain an in-memory sample buffer. When the buffer reaches the
 * configured {@code minBatchSize}, a DJL training step runs in-process against a
 * 22-class CNN (14 original + 8 extended taxonomy). Checkpoints are saved to disk so
 * the trained head survives restarts.</p>
 *
 * <p><b>Class taxonomy (indices 0-21):</b><br>
 * 0-13: original vineetsarpal/yolov11n-car-damage classes<br>
 * 14-21: extended classes — scratch, paint-damage, broken-component, missing-panel,
 * flood-damage, burn-damage, flat-tire, cracked-bumper</p>
 */
public interface VehicleDamageTrainingCapability {

    /**
     * Adds a labeled image to the training buffer and persists it to the dataset
     * directory in YOLO format. Triggers a DJL training step when the buffer is full.
     *
     * @param imageData   the image to label
     * @param damageClass one of the 22 class names (see taxonomy above)
     * @param boundingBox normalized [0,1] bbox, or {@code null} for full-image label
     * @return number of samples now in the buffer
     */
    int submitDamageLabel(ImageData imageData, String damageClass, BoundingBox boundingBox);

    /**
     * Runs the auxiliary classifier on a single image.
     *
     * @param imageData the image to classify
     * @return detection with label + confidence, or {@code null} if classifier not ready
     */
    Detection classifyDamageAuxiliary(ImageData imageData);

    /**
     * Returns sample counts per class from the on-disk dataset directory.
     *
     * @return map from class name to number of labeled samples on disk
     */
    Map<String, Long> getDamageDatasetStats();

    /**
     * Returns {@code true} when the classifier has been trained at least once and
     * can produce predictions.
     */
    boolean isAuxiliaryClassifierReady();

    /**
     * Exports the trained classifier checkpoint to {@code targetDir} for JAR bundling.
     * The checkpoint is written under {@code targetDir/damage-classifier/}.
     *
     * @throws IllegalStateException if classifier has not been trained yet
     * @throws java.io.IOException   on I/O failure
     */
    void exportClassifierArtifact(java.nio.file.Path targetDir) throws java.io.IOException;
}
