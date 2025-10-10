package io.github.codesapienbe.springvision.core.async;

import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.DetectionType;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents a vision processing task.
 *
 * <p>This class encapsulates a vision processing task with its associated
 * metadata, progress tracking, and cancellation support.</p>
 *
 * @author Spring Vision Team
 * @see AsyncVisionProcessor
 * @see TaskProgress
 * @since 1.0.0
 */
public class VisionTask {

    private final String taskId;
    private final ImageData imageData;
    private final DetectionType detectionType;
    private final Map<String, Object> parameters;
    private final long startTime;
    private final AtomicBoolean cancelled;

    private volatile TaskProgress progress;

    /**
     * Creates a new VisionTask with the specified parameters.
     *
     * @param taskId        the task identifier
     * @param imageData     the image data to process
     * @param detectionType the detection type to perform
     * @param parameters    the processing parameters
     */
    public VisionTask(String taskId, ImageData imageData, DetectionType detectionType, Map<String, Object> parameters) {
        this.taskId = Objects.requireNonNull(taskId, "Task ID must not be null");
        this.imageData = Objects.requireNonNull(imageData, "Image data must not be null");
        this.detectionType = Objects.requireNonNull(detectionType, "Detection type must not be null");
        this.parameters = parameters != null ? Map.copyOf(parameters) : Map.of();
        this.startTime = System.currentTimeMillis();
        this.cancelled = new AtomicBoolean(false);
        this.progress = TaskProgress.created(taskId);
    }

    /**
     * Gets the task identifier.
     *
     * @return the task identifier
     */
    public String getTaskId() {
        return taskId;
    }

    /**
     * Gets the image data to process.
     *
     * @return the image data
     */
    public ImageData getImageData() {
        return imageData;
    }

    /**
     * Gets the detection type to perform.
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
     * Gets the task start time.
     *
     * @return the start time in milliseconds
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Gets the current progress of this task.
     *
     * @return the current progress
     */
    public TaskProgress getProgress() {
        return progress;
    }

    /**
     * Sets the progress of this task.
     *
     * @param progress the new progress
     */
    public void setProgress(TaskProgress progress) {
        this.progress = Objects.requireNonNull(progress, "Progress must not be null");
    }

    /**
     * Checks if this task has been cancelled.
     *
     * @return true if cancelled, false otherwise
     */
    public boolean isCancelled() {
        return cancelled.get();
    }

    /**
     * Cancels this task.
     *
     * @return true if the task was cancelled, false if it was already cancelled
     */
    public boolean cancel() {
        return cancelled.compareAndSet(false, true);
    }

    /**
     * Gets the elapsed time since the task started.
     *
     * @return the elapsed time in milliseconds
     */
    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * Gets a parameter value by key.
     *
     * @param key the parameter key
     * @return the parameter value, or null if not found
     */
    public Object getParameter(String key) {
        return parameters.get(key);
    }

    /**
     * Gets a parameter value by key with a default value.
     *
     * @param key          the parameter key
     * @param defaultValue the default value to return if key not found
     * @return the parameter value, or the default value if not found
     */
    public Object getParameter(String key, Object defaultValue) {
        return parameters.getOrDefault(key, defaultValue);
    }

    /**
     * Checks if this task has a specific parameter.
     *
     * @param key the parameter key to check
     * @return true if the parameter exists, false otherwise
     */
    public boolean hasParameter(String key) {
        return parameters.containsKey(key);
    }

    @Override
    public String toString() {
        return String.format("VisionTask{id='%s', type=%s, cancelled=%s, elapsed=%dms}",
            taskId, detectionType, cancelled.get(), getElapsedTime());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        VisionTask that = (VisionTask) obj;
        return Objects.equals(taskId, that.taskId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId);
    }
}
