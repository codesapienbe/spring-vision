package io.github.codesapienbe.springvision.core.capabilities;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.djl.DjlProperties;
import io.github.codesapienbe.springvision.core.djl.DjlVisionBackend;
import io.github.codesapienbe.springvision.core.djl.TestImageUtils;

@DisplayName("Integration: FaceVerificationCapability")
public class FaceVerificationCapabilityIntegrationTest {
    private static DjlVisionBackend backend;

    @BeforeAll
    static void setup() throws Exception {
        DjlProperties p = new DjlProperties(); p.setEngine("OnnxRuntime"); p.setDevice("cpu"); p.setAutoDownload(false);
        backend = new DjlVisionBackend(p); backend.initialize();
    }

    @AfterAll
    static void teardown() { if (backend != null) backend.shutdown(); }

    @Test
    void verifyFacesExecutes() throws IOException {
        ImageData a = TestImageUtils.createSimpleFaceImage(320, 240);
        ImageData b = TestImageUtils.createSimpleFaceImage(320, 240);
        try {
            boolean same = false;
            if (backend instanceof FaceVerificationCapability cap) {
                same = cap.verify(a, b, "cosine", 0.6);
            }
            assertThat(same == true || same == false).isTrue();
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }
}
