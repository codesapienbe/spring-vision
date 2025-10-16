package io.github.codesapienbe.springvision.core.djl;

import io.github.codesapienbe.springvision.core.*;
import io.github.codesapienbe.springvision.core.exception.VisionBackendException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for DjlVisionBackend.
 * <p>
 * Note: These tests require actual model downloads and may be slow on first run.
 * Use @Disabled annotation to skip in CI environments without model access.
 */
class DjlVisionBackendIntegrationTest {

    private DjlVisionBackend backend;
    private DjlProperties properties;

    @BeforeEach
    void setUp() {
        properties = new DjlProperties();
        properties.setEnabled(true);
        properties.setEngine("PyTorch");
        properties.setDevice("cpu");
        properties.setAutoDownload(false); // Don't auto-download in tests

        backend = new DjlVisionBackend(properties);
    }

    @Test
    void testBackendInitialization() {
        assertNotNull(backend);
        assertEquals("djl", backend.getBackendId());
        assertEquals("DJL Vision Backend", backend.getDisplayName());
    }

    @Test
    void testHealthCheckBeforeInitialization() {
        assertFalse(backend.isHealthy());

        BackendHealthInfo healthInfo = backend.getHealthInfo();
        assertNotNull(healthInfo);
        assertEquals("djl", healthInfo.getBackendId());
    }

    @Test
    void testDetectionWithoutInitializationThrowsException() {
        byte[] imageData = createTestImageBytes();
        ImageData image = new ImageData(imageData, "image/png", System.currentTimeMillis(), "test");

        assertThrows(VisionBackendException.class, () -> backend.detectFaces(image));
    }

    @Test
    void testSupportedDetectionTypes() {
        var types = backend.getSupportedDetectionTypes();

        assertNotNull(types);
        assertTrue(types.contains(DetectionType.FACE));
        assertTrue(types.contains(DetectionType.OBJECT));
        assertTrue(types.contains(DetectionType.BODY));
        assertTrue(types.contains(DetectionType.SCENE));
    }

    @Test
    void testVersionInformation() {
        String version = backend.getVersion();

        assertNotNull(version);
        assertTrue(version.contains("DJL"));
        assertTrue(version.contains("1.0.5"));
    }

    @Test
    void testShutdownGracefully() {
        assertDoesNotThrow(() -> backend.shutdown());
    }

    @Test
    @Disabled("Requires model download - enable for full integration testing")
    void testFaceDetectionWithRealModel() throws Exception {
        // This test requires actual model files
        backend.initialize();

        byte[] imageData = createTestImageWithFaces();
        ImageData image = new ImageData(imageData, "image/png", System.currentTimeMillis(), "test");

        List<Detection> detections = backend.detectFaces(image);

        assertNotNull(detections);
        // Additional assertions based on test image content
    }

    @Test
    @Disabled("Requires model download - enable for full integration testing")
    void testObjectDetectionWithRealModel() throws Exception {
        backend.initialize();

        byte[] imageData = createTestImageWithObjects();
        ImageData image = new ImageData(imageData, "image/png", System.currentTimeMillis(), "test");

        List<Detection> detections = backend.detectObjects(image);

        assertNotNull(detections);
    }

    // Helper methods to create test images

    private byte[] createTestImageBytes() {
        try {
            BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, 200, 200);
            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] createTestImageWithFaces() {
        // Create a test image with synthetic face-like shapes
        try {
            BufferedImage image = new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, 400, 400);

            // Draw a simple face representation
            g.setColor(Color.BLACK);
            g.fillOval(150, 150, 100, 120); // Head
            g.setColor(Color.WHITE);
            g.fillOval(170, 180, 20, 20);   // Left eye
            g.fillOval(210, 180, 20, 20);   // Right eye
            g.setColor(Color.BLACK);
            g.drawArc(180, 220, 40, 20, 0, -180); // Mouth

            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] createTestImageWithObjects() {
        // Create a test image with simple shapes representing objects
        try {
            BufferedImage image = new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, 400, 400);

            // Draw some simple shapes
            g.setColor(Color.BLUE);
            g.fillRect(50, 50, 100, 100);    // Rectangle
            g.setColor(Color.RED);
            g.fillOval(250, 250, 80, 80);    // Circle

            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

