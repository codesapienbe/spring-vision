package io.github.codesapienbe.springvision.core.batch;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.codesapienbe.springvision.core.DetectionType;
import io.github.codesapienbe.springvision.core.ImageData;

/**
 * Represents a batch processing job with its configuration and state.
 *
 * <p>This class encapsulates all the information needed to process a batch of images,
 * including the images themselves, detection type, parameters, and current progress.</p>
 *
 * <p>The batch job maintains its state throughout the processing lifecycle and provides
 * methods to check status and cancel the operation.</p>
 *
 * @author Spring Vision Team
 * @see BatchVisionProcessor
 * @see BatchProgress
 * @since 1.0.0
 */
public final class BatchJob {

    private final String batchId;
    private final List<ImageData> images;
    private final DetectionType detectionType;
    private final Map<String, Object> parameters;
    private final int batchSize;
    private final AtomicBoolean cancelled;
    private volatile BatchProgress progress;

    /**
     * Creates a new BatchJob instance.
     *
     * @param batchId       the batch identifier
     * @param images        the list of images to process
     * @param detectionType the detection type to perform
     * @param parameters    the processing parameters
     * @param batchSize     the batch size
     */
    public BatchJob(String batchId, List<ImageData> images, DetectionType detectionType,
                    Map<String, Object> parameters, int batchSize) {
        this.batchId = batchId;
        this.images = List.copyOf(images); // Defensive copy
        this.detectionType = detectionType;
        this.parameters = Map.copyOf(parameters); // Defensive copy
        this.batchSize = batchSize;
        this.cancelled = new AtomicBoolean(false);
        this.progress = BatchProgress.started(batchId, images.size());
    }

    /**
     * Gets the batch identifier.
     *
     * @return the batch identifier
     */
    public String getBatchId() {
        return batchId;
    }

    /**
     * Gets the list of images to process.
     *
     * @return the list of images
     */
    public List<ImageData> getImages() {
        return images;
    }

    /**
     * Gets the detection type.
     *
     * @return the detection type
     */
    public DetectionType getDetectionType() {
        return detectionType;
    }

    /**
     * Gets the processing parameters.
     *
     * @return the processing parameters
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }

    /**
     * Gets the batch size.
     *
     * @return the batch size
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * Gets the current progress.
     *
     * @return the current progress
     */
    public BatchProgress getProgress() {
        return progress;
    }

    /**
     * Sets the current progress.
     *
     * @param progress the new progress
     */
    public void setProgress(BatchProgress progress) {
        this.progress = progress;
    }

    /**
     * Checks if the batch has been cancelled.
     *
     * @return true if the batch has been cancelled, false otherwise
     */
    public boolean isCancelled() {
        return cancelled.get();
    }

    /**
     * Cancels the batch processing.
     *
     * @return true if the batch was successfully cancelled, false if it was already cancelled
     */
    public boolean cancel() {
        return cancelled.compareAndSet(false, true);
    }

    /**
     * Gets the total number of images in this batch.
     *
     * @return the total number of images
     */
    public int getTotalImages() {
        return images.size();
    }

    @Override
    public String toString() {
        return String.format("BatchJob{batchId='%s', detectionType=%s, totalImages=%d, batchSize=%d, cancelled=%s}",
            batchId, detectionType, images.size(), batchSize, cancelled.get());
    }
}
