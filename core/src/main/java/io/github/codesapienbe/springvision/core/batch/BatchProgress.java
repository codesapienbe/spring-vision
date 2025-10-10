package io.github.codesapienbe.springvision.core.batch;

import io.github.codesapienbe.springvision.core.VisionResult;

import java.time.Instant;
import java.util.List;

/**
 * Represents the progress state of a batch processing operation.
 *
 * <p>This class provides immutable progress information for batch vision processing,
 * including the current status, completion percentage, and any relevant metadata.</p>
 *
 * <p>The progress can be in one of several states:</p>
 * <ul>
 *   <li>STARTED - Batch processing has begun</li>
 *   <li>RUNNING - Batch processing is in progress</li>
 *   <li>COMPLETED - Batch processing has finished successfully</li>
 *   <li>FAILED - Batch processing has failed</li>
 *   <li>CANCELLED - Batch processing was cancelled</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @see BatchVisionProcessor
 * @since 1.0.0
 */
public final class BatchProgress {

    /**
     * Enumeration of possible batch processing states.
     */
    public enum Status {
        /**
         * The batch processing has started.
         */
        STARTED,
        /**
         * The batch processing is currently running.
         */
        RUNNING,
        /**
         * The batch processing has completed successfully.
         */
        COMPLETED,
        /**
         * The batch processing has failed.
         */
        FAILED,
        /**
         * The batch processing has been cancelled.
         */
        CANCELLED
    }

    private final String batchId;
    private final Status status;
    private final double completionPercentage;
    private final String message;
    private final Instant timestamp;
    private final List<VisionResult> results;
    private final Throwable error;

    /**
     * Creates a new BatchProgress instance.
     *
     * @param batchId              the batch identifier
     * @param status               the current status
     * @param completionPercentage the completion percentage (0.0 to 1.0)
     * @param message              the progress message
     * @param timestamp            the timestamp
     * @param results              the vision results (may be null)
     * @param error                the error (may be null)
     */
    private BatchProgress(String batchId, Status status, double completionPercentage,
                          String message, Instant timestamp, List<VisionResult> results, Throwable error) {
        this.batchId = batchId;
        this.status = status;
        this.completionPercentage = Math.max(0.0, Math.min(1.0, completionPercentage));
        this.message = message;
        this.timestamp = timestamp;
        this.results = results;
        this.error = error;
    }

    /**
     * Creates a progress instance for a started batch.
     *
     * @param batchId     the batch identifier
     * @param totalImages the total number of images to process
     * @return a new BatchProgress instance
     */
    public static BatchProgress started(String batchId, int totalImages) {
        return new BatchProgress(batchId, Status.STARTED, 0.0,
            String.format("Started processing %d images", totalImages),
            Instant.now(), null, null);
    }

    /**
     * Creates a progress instance for a running batch.
     *
     * @param batchId              the batch identifier
     * @param completionPercentage the completion percentage
     * @param message              the progress message
     * @return a new BatchProgress instance
     */
    public static BatchProgress running(String batchId, double completionPercentage, String message) {
        return new BatchProgress(batchId, Status.RUNNING, completionPercentage,
            message, Instant.now(), null, null);
    }

    /**
     * Creates a progress instance for a completed batch.
     *
     * @param batchId the batch identifier
     * @param results the vision results
     * @return a new BatchProgress instance
     */
    public static BatchProgress completed(String batchId, List<VisionResult> results) {
        return new BatchProgress(batchId, Status.COMPLETED, 1.0,
            String.format("Completed processing %d images", results.size()),
            Instant.now(), results, null);
    }

    /**
     * Creates a progress instance for a failed batch.
     *
     * @param batchId the batch identifier
     * @param error   the error that occurred
     * @return a new BatchProgress instance
     */
    public static BatchProgress failed(String batchId, Throwable error) {
        return new BatchProgress(batchId, Status.FAILED, 0.0,
            String.format("Failed: %s", error.getMessage()),
            Instant.now(), null, error);
    }

    /**
     * Creates a progress instance for a cancelled batch.
     *
     * @param batchId the batch identifier
     * @return a new BatchProgress instance
     */
    public static BatchProgress cancelled(String batchId) {
        return new BatchProgress(batchId, Status.CANCELLED, 0.0,
            "Batch processing cancelled",
            Instant.now(), null, null);
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
     * Gets the current status.
     *
     * @return the current status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Gets the completion percentage.
     *
     * @return the completion percentage (0.0 to 1.0)
     */
    public double getCompletionPercentage() {
        return completionPercentage;
    }

    /**
     * Gets the progress message.
     *
     * @return the progress message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the timestamp.
     *
     * @return the timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the vision results.
     *
     * @return the vision results, or null if not available
     */
    public List<VisionResult> getResults() {
        return results;
    }

    /**
     * Gets the error that occurred.
     *
     * @return the error, or null if no error occurred
     */
    public Throwable getError() {
        return error;
    }

    /**
     * Checks if the batch is completed.
     *
     * @return true if the batch is completed, false otherwise
     */
    public boolean isCompleted() {
        return status == Status.COMPLETED;
    }

    /**
     * Checks if the batch has failed.
     *
     * @return true if the batch has failed, false otherwise
     */
    public boolean isFailed() {
        return status == Status.FAILED;
    }

    /**
     * Checks if the batch is cancelled.
     *
     * @return true if the batch is cancelled, false otherwise
     */
    public boolean isCancelled() {
        return status == Status.CANCELLED;
    }

    /**
     * Checks if the batch is still running.
     *
     * @return true if the batch is running, false otherwise
     */
    public boolean isRunning() {
        return status == Status.RUNNING || status == Status.STARTED;
    }

    @Override
    public String toString() {
        return String.format("BatchProgress{batchId='%s', status=%s, completion=%.2f%%, message='%s'}",
            batchId, status, completionPercentage * 100, message);
    }
}
