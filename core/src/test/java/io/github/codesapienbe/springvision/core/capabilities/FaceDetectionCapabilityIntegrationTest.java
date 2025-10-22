package io.github.codesapienbe.springvision.core.capabilities;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.djl.DjlVisionBackend;
import io.github.codesapienbe.springvision.core.djl.DjlProperties;
import io.github.codesapienbe.springvision.core.djl.TestImageUtils;

@DisplayName("Integration: FaceDetectionCapability")
public class FaceDetectionCapabilityIntegrationTest {

    private static DjlVisionBackend backend;
    private static boolean modelsAvailable = false;

    @BeforeAll
    static void setup() throws Exception {
        // Keep test simple: check for retinaface model presence using existing helper
        modelsAvailable = io.github.codesapienbe.springvision.core.djl.YoloLoader.isModelAvailable("retinaface/retinaface.pt");

        DjlProperties properties = new DjlProperties();
        properties.setEngine("PyTorch");
        properties.setDevice("cpu");
        properties.setAutoDownload(false);

        backend = new DjlVisionBackend(properties);
        backend.initialize();
    }

    @AfterAll
    static void teardown() {
        if (backend != null) {
            backend.shutdown();
        }
    }

    @Test
    @DisplayName("Should expose FaceDetectionCapability methods via backend")
    void shouldExposeFaceDetectionCapability() {
        assertThat(backend).isNotNull();
        assertThat(backend instanceof io.github.codesapienbe.springvision.core.capabilities.FaceDetectionCapability).isTrue();
    }

    @Test
    @DisplayName("TDD: detectFaces should return list (initially expect failure)")
    @EnabledIf("modelsAvailable")
    void tdd_detectFaces_shouldReturnList() throws IOException {
        ImageData image = TestImageUtils.createSimpleFaceImage(320, 240);

        // This test follows TDD: we expect integration to fail until implemented
        List<Detection> detections = ((io.github.codesapienbe.springvision.core.capabilities.FaceDetectionCapability) backend).detectFaces(image);

        // Minimal assertions - let it fail if behavior is missing or returns null
        assertThat(detections).isNotNull();
    }

    static boolean modelsAvailable() {
        return modelsAvailable;
    }
}


