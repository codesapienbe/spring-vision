package com.springvision.core.health;

import java.io.InputStream;
import java.time.Duration;
import java.util.Optional;

/**
 * Core abstraction for video input used by health-related capabilities.
 * Implementations or adapters should convert external sources into this shape.
 */
public interface HealthVideoSource {

    String getId();

    Optional<InputStream> nextFrameAsStream();

    Optional<Duration> preferredFrameInterval();

    void close();
}

