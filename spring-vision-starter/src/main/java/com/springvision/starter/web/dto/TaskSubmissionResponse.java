package com.springvision.starter.web.dto;

public record TaskSubmissionResponse(
        String correlationId,
        String taskId,
        String status
) {}
