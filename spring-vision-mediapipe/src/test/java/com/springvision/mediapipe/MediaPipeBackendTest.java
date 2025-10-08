package com.springvision.mediapipe;

import com.springvision.core.DetectionType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MediaPipeBackendTest {

    @Test
    void testBackendId() {
        MediaPipeBackend backend = new MediaPipeBackend();
        assertEquals("mediapipe", backend.getBackendId());
    }

    @Test
    void testDisplayName() {
        MediaPipeBackend backend = new MediaPipeBackend();
        assertEquals("MediaPipe Backend", backend.getDisplayName());
    }

    @Test
    void testSupportedDetectionTypes() {
        MediaPipeBackend backend = new MediaPipeBackend();
        assertTrue(backend.getSupportedDetectionTypes().contains(DetectionType.HAND));
        assertTrue(backend.getSupportedDetectionTypes().contains(DetectionType.LANDMARK));
        assertTrue(backend.getSupportedDetectionTypes().contains(DetectionType.POSE));
    }

    @Test
    void testIsHealthy() {
        MediaPipeBackend backend = new MediaPipeBackend();
        assertTrue(backend.isHealthy()); // Placeholder
    }
}
