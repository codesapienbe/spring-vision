package io.github.codesapienbe.springvision.core.capabilities;

import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.djl.DjlVisionBackend;
import io.github.codesapienbe.springvision.core.djl.DjlProperties;
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
 * Integration tests for all DJL capabilities.
 */
class CapabilitiesIntegrationTest {

    private DjlVisionBackend backend;

    @BeforeEach
    void setUp() {
        DjlProperties properties = new DjlProperties();
        properties.setEnabled(true);
        properties.setEngine("PyTorch");
        properties.setDevice("cpu");
        properties.setAutoDownload(false);

        backend = new DjlVisionBackend(properties);
    }

    @Test
    void testFaceDetectionCapability() {
        assertTrue(backend instanceof FaceDetectionCapability);

        FaceDetectionCapability capability = (FaceDetectionCapability) backend;
        assertNotNull(capability);
    }

    @Test
    void testObjectDetectionCapability() {
        assertTrue(backend instanceof ObjectDetectionCapability);

        ObjectDetectionCapability capability = (ObjectDetectionCapability) backend;
        assertNotNull(capability);
    }

    @Test
    void testPoseEstimationCapability() {
        assertTrue(backend instanceof PoseEstimationCapability);

        PoseEstimationCapability capability = (PoseEstimationCapability) backend;
        assertNotNull(capability);
    }

    @Test
    void testActionRecognitionCapability() {
        assertTrue(backend instanceof ActionRecognitionCapability);

        ActionRecognitionCapability capability = (ActionRecognitionCapability) backend;
        assertNotNull(capability);
    }

    @Test
    void testSegmentationCapability() {
        assertTrue(backend instanceof SegmentationCapability);

        SegmentationCapability capability = (SegmentationCapability) backend;
        assertNotNull(capability);
    }

    @Test
    void testEmbeddingCapability() {
        assertTrue(backend instanceof EmbeddingCapability);

        EmbeddingCapability capability = (EmbeddingCapability) backend;
        assertNotNull(capability);
    }

    @Test
    @Disabled("Requires model initialization")
    void testMultipleCapabilitiesOnSameImage() throws Exception {
        // Initialize backend
        backend.initialize();

        byte[] imageData = createTestImage();
        ImageData image = new ImageData(imageData, "image/png", System.currentTimeMillis(), "test");

        // Test multiple capabilities
        List<Detection> faces = backend.detectFaces(image);
        List<Detection> objects = backend.detectObjects(image);
        float[] embeddings = backend.extractEmbeddings(image);

        assertNotNull(faces);
        assertNotNull(objects);
        assertNotNull(embeddings);
    }

    @Test
    void testCapabilityInterfaceImplementation() {
        // Verify all capability interfaces are properly implemented
        assertInstanceOf(FaceDetectionCapability.class, backend);
        assertInstanceOf(ObjectDetectionCapability.class, backend);
        assertInstanceOf(PoseEstimationCapability.class, backend);
        assertInstanceOf(ActionRecognitionCapability.class, backend);
        assertInstanceOf(SegmentationCapability.class, backend);
        assertInstanceOf(EmbeddingCapability.class, backend);
    }

    private byte[] createTestImage() {
        try {
            BufferedImage image = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, 300, 300);
            g.setColor(Color.BLUE);
            g.fillRect(100, 100, 100, 100);
            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

