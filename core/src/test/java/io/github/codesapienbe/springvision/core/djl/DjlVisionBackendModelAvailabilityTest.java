package io.github.codesapienbe.springvision.core.djl;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;

/**
 * Strict integration test that requires DJL models to be available and the backend to initialize.
 * This test will fail if native/model artifacts cannot be downloaded or loaded.
 */
public class DjlVisionBackendModelAvailabilityTest {

    private static DjlVisionBackend backend;

    @BeforeAll
    public static void setup() throws Exception {
        // Prevent DJL from attempting runtime downloads (models / JNI) during test runs.
        // DJL checks the `ai.djl.offline` system property; also opt out of telemetry.
        System.setProperty("ai.djl.offline", "true");
        System.setProperty("OPT_OUT_TRACKING", "true");

        DjlProperties props = new DjlProperties();
        props.setEngine("OnnxRuntime");
        props.setDevice("cpu");
        // Disable automatic downloads from the model zoo in tests
        props.setAutoDownload(false);

        backend = new DjlVisionBackend(props);

        // This must succeed for the test to proceed; rethrow any failures so the test fails.
        backend.initialize();
    }

    @AfterAll
    public static void teardown() {
        if (backend != null) backend.shutdown();
    }

    private ImageData makeTestImage(int w, int h, Color rectColor) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, 0, w, h);
        g.setColor(rectColor);
        g.fillRect(w/4, h/4, w/2, h/2);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpeg", baos);
        byte[] bytes = baos.toByteArray();
        return ImageData.fromBytes(bytes, ImageData.DEFAULT_JPEG_MIME_TYPE);
    }

    @Test
    public void testBackendIsHealthyAfterInitialize() {
        Assertions.assertTrue(backend.isHealthy(), "Backend should report healthy after initialize");
    }

    @Test
    public void testDetectObjects_runsSuccessfully() throws Exception {
        ImageData img = makeTestImage(640, 480, Color.BLUE);
        List<Detection> objs = backend.detectObjects(img);
        Assertions.assertNotNull(objs);
    }

    @Test
    public void testDetectFaces_runsSuccessfully() throws Exception {
        ImageData img = makeTestImage(320, 240, Color.RED);
        List<Detection> faces = backend.detectFaces(img);
        Assertions.assertNotNull(faces);
    }

    @Test
    public void testExtractEmbeddings_runsSuccessfully() throws Exception {
        ImageData img = makeTestImage(320, 240, Color.GREEN);
        List<float[]> embs = backend.extractEmbeddings(img, io.github.codesapienbe.springvision.core.DetectionCategory.FACE);
        Assertions.assertNotNull(embs);
    }
}
