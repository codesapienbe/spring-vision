package io.github.codesapienbe.springvision.starter.web.dto;

public record TaskSubmissionResponse(
    String correlationId,
    String taskId,
    String status
) {
}
