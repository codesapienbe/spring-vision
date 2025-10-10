package io.github.codesapienbe.springvision.core.health;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public final class FallEvent {
    private final String sessionId;
    private final Instant timestamp;
    private final double confidence;
    private final Map<String, String> metadata;

    public FallEvent(String sessionId, Instant timestamp, double confidence, Map<String, String> metadata) {
        this.sessionId = sessionId;
        this.timestamp = timestamp;
        this.confidence = confidence;
        this.metadata = metadata;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public double getConfidence() {
        return confidence;
    }

    public Optional<Map<String, String>> getMetadata() {
        return Optional.ofNullable(metadata);
    }
}

