package io.github.codesapienbe.springvision.compreface;

import io.github.codesapienbe.springvision.core.DetectionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompreFaceBackendTest {

    @Test
    void testBackendId() {
        CompreFaceBackend backend = new CompreFaceBackend();
        assertEquals("compreface", backend.getBackendId());
    }

    @Test
    void testDisplayName() {
        CompreFaceBackend backend = new CompreFaceBackend();
        assertEquals("CompreFace Backend", backend.getDisplayName());
    }

    @Test
    void testSupportedDetectionTypes() {
        CompreFaceBackend backend = new CompreFaceBackend();
        assertTrue(backend.getSupportedDetectionTypes().contains(DetectionType.FACE));
    }

    @Test
    void testIsHealthy() {
        CompreFaceBackend backend = new CompreFaceBackend();
        assertTrue(backend.isHealthy()); // Placeholder
    }
}
