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
import io.github.codesapienbe.springvision.core.djl.DjlProperties;
import io.github.codesapienbe.springvision.core.djl.DjlVisionBackend;
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
    @DisplayName("TDD: detectFaces should return non-empty list when models available (fail first)")
    @EnabledIf("modelsAvailable")
    void tdd_detectFaces_shouldReturnNonEmptyList() throws IOException {
        ImageData image = TestImageUtils.createSimpleFaceImage(320, 240);

        // TDD: this should fail initially — we expect at least one detection when models are loaded
        List<Detection> detections = ((io.github.codesapienbe.springvision.core.capabilities.FaceDetectionCapability) backend).detectFaces(image);

        // Require non-empty results to drive implementation work
        assertThat(detections).isNotNull();
        assertThat(detections).isNotEmpty();
    }

    static boolean modelsAvailable() {
        return modelsAvailable;
    }
}
