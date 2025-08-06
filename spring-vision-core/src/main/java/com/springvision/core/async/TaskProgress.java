package com.springvision.core.async;

import com.springvision.core.VisionResult;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents the progress of a vision processing task.
 *
 * <p>This class encapsulates the current state and progress information
 * for a vision processing task, including status, completion percentage,
 * and result information.</p>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 * @see VisionTask
 * @see AsyncVisionProcessor
 */
public class TaskProgress {

    private final String taskId;
    private final TaskStatus status;
    private final double completionPercentage;
    private final String message;
    private final VisionResult result;
    private final Throwable error;
    private final Instant timestamp;

    /**
     * Creates a new TaskProgress with the specified parameters.
     *
     * @param taskId the task identifier
     * @param status the task status
     * @param completionPercentage the completion percentage (0.0 to 1.0)
     * @param message the progress message
     * @param result the vision result (if completed)
     * @param error the error (if failed)
     * @param timestamp the timestamp
     */
    private TaskProgress(String taskId, TaskStatus status, double completionPercentage,
                        String message, VisionResult result, Throwable error, Instant timestamp) {
        this.taskId = Objects.requireNonNull(taskId, "Task ID must not be null");
        this.status = Objects.requireNonNull(status, "Status must not be null");
        this.completionPercentage = Math.max(0.0, Math.min(1.0, completionPercentage));
        this.message = message;
        this.result = result;
        this.error = error;
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp must not be null");
    }

    /**
     * Creates a progress for a newly created task.
     *
     * @param taskId the task identifier
     * @return the task progress
     */
    public static TaskProgress created(String taskId) {
        return new TaskProgress(taskId, TaskStatus.CREATED, 0.0, "Task created", null, null, Instant.now());
    }

    /**
     * Creates a progress for a started task.
     *
     * @param taskId the task identifier
     * @return the task progress
     */
    public static TaskProgress started(String taskId) {
        return new TaskProgress(taskId, TaskStatus.RUNNING, 0.1, "Task started", null, null, Instant.now());
    }

    /**
     * Creates a progress for a running task.
     *
     * @param taskId the task identifier
     * @param completionPercentage the completion percentage
     * @param message the progress message
     * @return the task progress
     */
    public static TaskProgress running(String taskId, double completionPercentage, String message) {
        return new TaskProgress(taskId, TaskStatus.RUNNING, completionPercentage, message, null, null, Instant.now());
    }

    /**
     * Creates a progress for a completed task.
     *
     * @param taskId the task identifier
     * @param result the vision result
     * @return the task progress
     */
    public static TaskProgress completed(String taskId, VisionResult result) {
        return new TaskProgress(taskId, TaskStatus.COMPLETED, 1.0, "Task completed", result, null, Instant.now());
    }

    /**
     * Creates a progress for a failed task.
     *
     * @param taskId the task identifier
     * @param error the error that caused the failure
     * @return the task progress
     */
    public static TaskProgress failed(String taskId, Throwable error) {
        return new TaskProgress(taskId, TaskStatus.FAILED, 0.0, "Task failed", null, error, Instant.now());
    }

    /**
     * Creates a progress for a cancelled task.
     *
     * @param taskId the task identifier
     * @return the task progress
     */
    public static TaskProgress cancelled(String taskId) {
        return new TaskProgress(taskId, TaskStatus.CANCELLED, 0.0, "Task cancelled", null, null, Instant.now());
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
     * Gets the task status.
     *
     * @return the task status
     */
    public TaskStatus getStatus() {
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
     * Gets the vision result (if completed).
     *
     * @return the vision result, or null if not completed
     */
    public VisionResult getResult() {
        return result;
    }

    /**
     * Gets the error (if failed).
     *
     * @return the error, or null if not failed
     */
    public Throwable getError() {
        return error;
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
     * Checks if the task is completed.
     *
     * @return true if completed, false otherwise
     */
    public boolean isCompleted() {
        return status == TaskStatus.COMPLETED;
    }

    /**
     * Checks if the task is running.
     *
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return status == TaskStatus.RUNNING;
    }

    /**
     * Checks if the task has failed.
     *
     * @return true if failed, false otherwise
     */
    public boolean isFailed() {
        return status == TaskStatus.FAILED;
    }

    /**
     * Checks if the task has been cancelled.
     *
     * @return true if cancelled, false otherwise
     */
    public boolean isCancelled() {
        return status == TaskStatus.CANCELLED;
    }

    /**
     * Checks if the task is in a terminal state (completed, failed, or cancelled).
     *
     * @return true if in terminal state, false otherwise
     */
    public boolean isTerminal() {
        return status == TaskStatus.COMPLETED ||
               status == TaskStatus.FAILED ||
               status == TaskStatus.CANCELLED;
    }

    /**
     * Gets the completion percentage as a percentage string.
     *
     * @return the completion percentage as a string (e.g., "75%")
     */
    public String getCompletionPercentageString() {
        return String.format("%.1f%%", completionPercentage * 100);
    }

    @Override
    public String toString() {
        return String.format("TaskProgress{taskId='%s', status=%s, completion=%.1f%%, message='%s'}",
                           taskId, status, completionPercentage * 100, message);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        TaskProgress that = (TaskProgress) obj;
        return Objects.equals(taskId, that.taskId) &&
               status == that.status &&
               Double.compare(that.completionPercentage, completionPercentage) == 0 &&
               Objects.equals(message, that.message) &&
               Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, status, completionPercentage, message, timestamp);
    }

    /**
     * Task status enumeration.
     */
    public enum TaskStatus {
        /**
         * Task has been created but not yet started.
         */
        CREATED("created"),

        /**
         * Task is currently running.
         */
        RUNNING("running"),

        /**
         * Task has completed successfully.
         */
        COMPLETED("completed"),

        /**
         * Task has failed with an error.
         */
        FAILED("failed"),

        /**
         * Task has been cancelled.
         */
        CANCELLED("cancelled");

        private final String value;

        TaskStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
