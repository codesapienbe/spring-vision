package com.springvision.health.api;

import java.time.Instant;
import java.util.Map;

/**
 * DTO describing a detected fall event.
 */
public final class FallEvent {
    private final String sessionId;
    private final Instant timestamp;
    private final double confidence; // [0..1]
    private final Map<String, String> metadata; // optional (subject id, bounding box, frame index)

    public FallEvent(String sessionId, Instant timestamp, double confidence, Map<String, String> metadata) {
        this.sessionId = sessionId;
        this.timestamp = timestamp;
        this.confidence = confidence;
        this.metadata = metadata;
    }


