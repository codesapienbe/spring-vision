package io.github.codesapienbe.springvision.core.health;

import java.time.Instant;
import java.util.Optional;

/**
 * Minimal core DTO representing a heart-rate sample.
 */
public final class HeartRateResult {
    private final Instant timestamp;
    private final double bpm;
    private final double confidence;
    private final byte[] rawSignal;

    public HeartRateResult(Instant timestamp, double bpm, double confidence, byte[] rawSignal) {
        this.timestamp = timestamp;
        this.bpm = bpm;
        this.confidence = confidence;
        this.rawSignal = rawSignal == null ? null : rawSignal.clone();
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public double getBpm() {
        return bpm;
    }

    public double getConfidence() {
        return confidence;
    }

    public Optional<byte[]> getRawSignal() {
        return rawSignal == null ? Optional.empty() : Optional.of(rawSignal.clone());
    }
}

