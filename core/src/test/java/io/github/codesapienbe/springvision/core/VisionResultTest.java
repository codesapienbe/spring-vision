package io.github.codesapienbe.springvision.core;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link VisionResult}.
 */
public class VisionResultTest {

    @Test
    public void testOfAndAccessors() {
        Detection d1 = Detection.of("face", 0.9, new BoundingBox(0.1, 0.1, 0.1, 0.1));
        Detection d2 = Detection.of("face", 0.8, new BoundingBox(0.2, 0.2, 0.1, 0.1));

        VisionResult res = VisionResult.of(DetectionType.FACE, List.of(d1, d2), 0.85, 50);

        assertEquals(DetectionType.FACE, res.detectionType());
        assertTrue(res.hasDetections());
        assertEquals(2, res.detectionCount());
        assertEquals(d1, res.getHighestConfidenceDetection());
        assertEquals(0.05, res.getProcessingTimeSeconds(), 1e-9);
    }

    @Test
    public void testEmptyAndThresholds() {
        VisionResult empty = VisionResult.empty(DetectionType.OBJECT, 10);
        assertFalse(empty.hasDetections());
        assertEquals(0, empty.detectionCount());

        Detection d = Detection.of("obj", 0.7, new BoundingBox(0.0, 0.0, 0.1, 0.1));
        VisionResult res = new VisionResult(DetectionType.OBJECT, List.of(d), 0.7, 100, Instant.now(), Map.of());

        assertEquals(1, res.getDetectionsAboveThreshold(0.5).size());
        assertThrows(IllegalArgumentException.class, () -> res.getDetectionsAboveThreshold(-0.1));
    }

}
