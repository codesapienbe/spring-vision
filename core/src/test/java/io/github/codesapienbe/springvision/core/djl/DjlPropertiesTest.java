package io.github.codesapienbe.springvision.core.djl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for DJL configuration properties.
 */
@SpringBootTest(classes = DjlPropertiesTest.TestConfig.class)
@TestPropertySource(properties = {
    "spring.vision.djl.enabled=true",
    "spring.vision.djl.engine=PyTorch",
    "spring.vision.djl.device=cpu",
    "spring.vision.djl.auto-download=true",
    "spring.vision.djl.max-concurrent-inferences=8",
    "spring.vision.djl.face-detection.confidence-threshold=0.8",
    "spring.vision.djl.object-detection.max-detections=50"
})
class DjlPropertiesTest {

    @Autowired
    private DjlProperties properties;

    @Test
    void testBasicProperties() {
        assertTrue(properties.isEnabled());
        assertEquals("PyTorch", properties.getEngine());
        assertEquals("cpu", properties.getDevice());
        assertTrue(properties.isAutoDownload());
        assertEquals(8, properties.getMaxConcurrentInferences());
    }

    @Test
    void testFaceDetectionProperties() {
        assertNotNull(properties.getFaceDetection());
        assertEquals(0.8f, properties.getFaceDetection().getConfidenceThreshold(), 0.01f);
    }

    @Test
    void testObjectDetectionProperties() {
        assertNotNull(properties.getObjectDetection());
        assertEquals(50, properties.getObjectDetection().getMaxDetections());
    }

    @Test
    void testDefaultValues() {
        DjlProperties defaultProps = new DjlProperties();
        assertTrue(defaultProps.isEnabled());
        assertEquals("PyTorch", defaultProps.getEngine());
        assertEquals("cpu", defaultProps.getDevice());
        assertTrue(defaultProps.isAutoDownload());
    }

    @Test
    void testNestedConfigurationNotNull() {
        assertNotNull(properties.getFaceDetection());
        assertNotNull(properties.getObjectDetection());
        assertNotNull(properties.getPoseEstimation());
        assertNotNull(properties.getActionRecognition());
        assertNotNull(properties.getSegmentation());
        assertNotNull(properties.getFaceRecognition());
    }

    @EnableConfigurationProperties(DjlProperties.class)
    static class TestConfig {
    }
}

