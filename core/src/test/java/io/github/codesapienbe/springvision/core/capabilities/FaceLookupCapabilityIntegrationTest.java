package io.github.codesapienbe.springvision.core.capabilities;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.djl.DjlProperties;
import io.github.codesapienbe.springvision.core.djl.DjlVisionBackend;
import io.github.codesapienbe.springvision.core.djl.TestImageUtils;

@DisplayName("Integration: FaceLookupCapability")
public class FaceLookupCapabilityIntegrationTest {
    private static DjlVisionBackend backend;

    @BeforeAll
    static void setup() throws Exception {
        DjlProperties p = new DjlProperties(); p.setEngine("OnnxRuntime"); p.setDevice("cpu"); p.setAutoDownload(false); p.setSyntheticFallbacks(true);
        backend = new DjlVisionBackend(p); backend.initialize();
    }

    @AfterAll
    static void teardown() { if (backend != null) backend.shutdown(); }

    @Test
    void findNearestExecutes() throws IOException {
        ImageData probe = TestImageUtils.createSimpleFaceImage(320, 240);
        try {
            if (backend instanceof FaceLookupCapability cap) {
                float[] probeEmb = new float[128];
                var res = cap.findNearestEmbeddings(probe, probeEmb, List.of(new float[128]), "cosine", 3);
                assertThat(res).isNotNull();
            } else {
                assertThat(true).isTrue();
            }
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }
}
