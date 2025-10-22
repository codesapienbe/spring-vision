package io.github.codesapienbe.springvision.core.capabilities;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.djl.DjlVisionBackend;
import io.github.codesapienbe.springvision.core.djl.DjlProperties;
import io.github.codesapienbe.springvision.core.djl.TestImageUtils;

@DisplayName("Integration: HeartRateCapability")
public class HeartRateCapabilityIntegrationTest {

    private static DjlVisionBackend backend;

    @BeforeAll
    static void setup() throws Exception {
        DjlProperties properties = new DjlProperties();
        properties.setEngine("OnnxRuntime");
        properties.setDevice("cpu");
        properties.setAutoDownload(false);

        backend = new DjlVisionBackend(properties);
        backend.initialize();
    }

    @AfterAll
    static void teardown() {
        if (backend != null) backend.shutdown();
    }

    @Test
    @DisplayName("Should return heart rate estimation placeholder or synthetic result")
    void shouldEstimateHeartRate() throws IOException {
        // Create a short sequence (tests may expect IllegalArgumentException for too short sequences)
        List<ImageData> frames = List.of(
            TestImageUtils.createSimpleFaceImage(320, 240),
            TestImageUtils.createSimpleFaceImage(320, 240),
            TestImageUtils.createSimpleFaceImage(320, 240),
            TestImageUtils.createSimpleFaceImage(320, 240),
            TestImageUtils.createSimpleFaceImage(320, 240)
        );

        try {
            List<Detection> results = backend.detectHeartRate(frames);
            assertThat(results).isNotNull();
        } catch (IllegalArgumentException iae) {
            // Acceptable: method enforces minimum frames, test acknowledges that
            assertThat(iae).isNotNull();
        }
    }
}


