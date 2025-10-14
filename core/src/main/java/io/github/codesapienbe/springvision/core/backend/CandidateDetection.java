package io.github.codesapienbe.springvision.core.backend;

import org.bytedeco.opencv.opencv_core.Rect;

import java.util.HashMap;
import java.util.Map;

/**
 * Candidate detection structure for fusion processing.
 */
class CandidateDetection {
    final Rect rect;
    final float confidence;
    final String detector;
    final Map<String, Object> attributes;
    double qualityScore;

    CandidateDetection(Rect rect, float confidence, String detector, Map<String, Object> attributes) {
        this.rect = rect;
        this.confidence = confidence;
        this.detector = detector;
        this.attributes = new HashMap<>(attributes);
        this.qualityScore = 0.0;
    }
}
