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
import io.github.codesapienbe.springvision.core.djl.DjlProperties;
import io.github.codesapienbe.springvision.core.djl.DjlVisionBackend;
import io.github.codesapienbe.springvision.core.djl.TestImageUtils;

@DisplayName("Integration: HandDetectionCapability")
public class HandDetectionCapabilityIntegrationTest {
    private static DjlVisionBackend backend;

    @BeforeAll
    static void setup() throws Exception {
        DjlProperties p = new DjlProperties();
        p.setEngine("OnnxRuntime");
        p.setDevice("cpu");
        p.setAutoDownload(false);
        backend = new DjlVisionBackend(p);
        backend.initialize();
    }

    @AfterAll
    static void teardown() { if (backend != null) backend.shutdown(); }

    @Test
    void detectHandsReturnsList() throws IOException {
        ImageData img = TestImageUtils.createPersonSilhouetteImage(320, 240);
        List<Detection> res = backend.detectHands(img);
        assertThat(res).isNotNull();
    }
}
