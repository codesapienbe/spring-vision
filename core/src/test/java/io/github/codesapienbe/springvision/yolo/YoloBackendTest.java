package io.github.codesapienbe.springvision.yolo;

import io.github.codesapienbe.springvision.core.DetectionType;
import io.github.codesapienbe.springvision.yolo.config.YoloProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class YoloBackendTest {

    @Test
    void testBackendId() {
        YoloProperties properties = new YoloProperties();
        YoloBackend backend = new YoloBackend(properties);
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
