package io.github.codesapienbe.springvision.robotics;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.DetectionType;
import io.github.codesapienbe.springvision.core.ImageData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RoboticsBackend.
 *
 * @author Spring Vision Team
 * @since 2.0.0
 */
class RoboticsBackendTest {

    private RoboticsBackend roboticsBackend;

    @BeforeEach
    void setUp() {
        roboticsBackend = new RoboticsBackend();
    }

    @Test
    void testBackendMetadata() {
        assertEquals("robotics", roboticsBackend.getBackendId());
        assertEquals("Robotics & Industrial Automation Backend", roboticsBackend.getDisplayName());
        assertEquals("2.0.0", roboticsBackend.getVersion());
    }

    @Test
    void testSupportedDetectionTypes() {
        var supportedTypes = roboticsBackend.getSupportedDetectionTypes();

        assertTrue(supportedTypes.contains(DetectionType.DEFECT));
        assertTrue(supportedTypes.contains(DetectionType.ROBOTIC_GUIDANCE));
        assertTrue(supportedTypes.contains(DetectionType.COMPONENT_VERIFICATION));
        assertTrue(supportedTypes.contains(DetectionType.OBJECT));
    }

    @Test
    void testHealthCheck() {
        assertTrue(roboticsBackend.isHealthy());

        var healthInfo = roboticsBackend.getHealthInfo();
        assertNotNull(healthInfo);
        assertEquals("robotics", healthInfo.backendId());
    }

    @Test
    void testDetectDefectsWithNullInput() {
        List<Detection> result = roboticsBackend.detectDefects(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testDetectDefectsWithEmptyList() {
        List<Detection> result = roboticsBackend.detectDefects(List.of());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testProvideGuidanceWithNullInput() {
        List<Detection> result = roboticsBackend.provideGuidance(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testProvideGuidanceWithEmptyList() {
        List<Detection> result = roboticsBackend.provideGuidance(List.of());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testVerifyComponentsWithNullInput() {
        List<Detection> result = roboticsBackend.verifyComponents(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testVerifyComponentsWithEmptyList() {
        List<Detection> result = roboticsBackend.verifyComponents(List.of());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testSupportsDetectionType() {
        assertTrue(roboticsBackend.supportsDetectionType(DetectionType.DEFECT));
        assertTrue(roboticsBackend.supportsDetectionType(DetectionType.ROBOTIC_GUIDANCE));
        assertTrue(roboticsBackend.supportsDetectionType(DetectionType.COMPONENT_VERIFICATION));
        assertFalse(roboticsBackend.supportsDetectionType(DetectionType.FACE));
    }
}

