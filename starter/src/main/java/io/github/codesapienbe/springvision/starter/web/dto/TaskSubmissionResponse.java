package io.github.codesapienbe.springvision.starter.web.dto;

/**
 * Response DTO for task submission operations.
 *
 * <p>This record represents the response from asynchronous task submission endpoints,
 * containing the correlation ID, task ID, and submission status.</p>
 *
 * @param correlationId the correlation ID for request tracking
 * @param taskId        the unique task identifier (may be null if submission failed)
 * @param status        the submission status message
 * @author Spring Vision Team
 * @since 1.0.0
 */
public record TaskSubmissionResponse(
    String correlationId,
    String taskId,
    String status
) {
}
