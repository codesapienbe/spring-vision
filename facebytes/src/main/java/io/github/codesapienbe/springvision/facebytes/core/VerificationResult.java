package io.github.codesapienbe.springvision.facebytes.core;

import io.github.codesapienbe.springvision.facebytes.enums.DetectorBackend;
import io.github.codesapienbe.springvision.facebytes.enums.ModelType;

public record VerificationResult(
    boolean verified,
    double distance,
    double threshold,
    ModelType model,
    DetectorBackend detector,
    long processingTimeMs
) {
}
