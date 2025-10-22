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

@DisplayName("Integration: ThreatDetectionCapability")
public class ThreatDetectionCapabilityIntegrationTest {
    private static DjlVisionBackend backend;

    @BeforeAll
    static void setup() throws Exception {
        DjlProperties p = new DjlProperties(); p.setEngine("OnnxRuntime"); p.setDevice("cpu"); p.setAutoDownload(false); p.setSyntheticFallbacks(true);
        backend = new DjlVisionBackend(p); backend.initialize();
    }

    @AfterAll
    static void teardown() { if (backend != null) backend.shutdown(); }

    @Test
    void detectThreatProcessesImages() throws IOException {
        List<ImageData> seq = List.of(TestImageUtils.createRectangleImage(320, 240, java.awt.Color.GRAY));
        List<Detection> res = backend.detectThreat(seq);
        assertThat(res).isNotNull();
    }
}
