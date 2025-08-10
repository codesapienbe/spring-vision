package com.deepface.core;

import com.deepface.enums.DetectorBackend;
import com.deepface.enums.ModelType;

public record VerificationResult(
    boolean verified,
    double distance,
    double threshold,
    ModelType model,
    DetectorBackend detector,
    long processingTimeMs
) {}
