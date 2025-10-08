package com.springvision.yolo;

import com.springvision.core.DetectionType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class YoloBackendTest {

    @Test
    void testBackendId() {
        YoloBackend backend = new YoloBackend();
        assertEquals("yolo", backend.getBackendId());
    }

    @Test
    void testDisplayName() {
        YoloBackend backend = new YoloBackend();
        assertEquals("YOLO Backend", backend.getDisplayName());
    }

    @Test
    void testSupportedDetectionTypes() {
        YoloBackend backend = new YoloBackend();
        assertTrue(backend.getSupportedDetectionTypes().contains(DetectionType.OBJECT));
    }

    @Test
    void testIsHealthy() {
        YoloBackend backend = new YoloBackend();
        assertTrue(backend.isHealthy()); // Placeholder
    }
}
