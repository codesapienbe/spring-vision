package io.github.codesapienbe.springvision.core.djl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.codesapienbe.springvision.core.BackendHealthInfo;
import io.github.codesapienbe.springvision.core.DetectionType;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.exception.VisionBackendException;

/**
 * Unit tests for DjlVisionBackend.
 */
class DjlVisionBackendTest {

    private DjlVisionBackend backend;

    @BeforeEach
    void setUp() {
        backend = new DjlVisionBackend();
    }

    @Test
    void testGetBackendId() {
        assertEquals("djl", backend.getBackendId());
    }

    @Test
    void testGetDisplayName() {
        assertEquals("DJL Vision Backend", backend.getDisplayName());
    }

    @Test
    void testGetVersion() {
        assertNotNull(backend.getVersion());
        assertTrue(backend.getVersion().contains("DJL"));
    }

    @Test
    void testGetSupportedDetectionTypes() {
        Set<DetectionType> types = backend.getSupportedDetectionTypes();

        assertNotNull(types);
        assertTrue(types.contains(DetectionType.FACE));
    }

    @Test
    void testIsHealthyBeforeInitialization() {
        assertFalse(backend.isHealthy());
    }

    @Test
    void testGetHealthInfoBeforeInitialization() {
        BackendHealthInfo healthInfo = backend.getHealthInfo();

        assertNotNull(healthInfo);
    }

    @Test
    void testDetectFacesThrowsExceptionWhenNotInitialized() {
        byte[] imageBytes = new byte[]{1, 2, 3, 4};
        ImageData imageData = new ImageData(imageBytes, "image/jpeg", System.currentTimeMillis(), "test");

        assertThrows(VisionBackendException.class, () -> {
            backend.detectFaces(imageData);
        });
    }

    @Test
    void testShutdownWhenNotInitializedDoesNotThrow() {
        assertDoesNotThrow(() -> {
            backend.shutdown();
        });
    }
}
