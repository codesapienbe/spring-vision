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

@DisplayName("Integration: ImageClassificationCapability")
public class ImageClassificationCapabilityIntegrationTest {
    private static DjlVisionBackend backend;

    @BeforeAll
    static void setup() throws Exception {
        DjlProperties p = new DjlProperties(); 
        p.setSyntheticFallbacks(true); // enable synthetic fallbacks for these capability TDD tests
        p.setEngine("OnnxRuntime");
        p.setDevice("cpu");
        p.setAutoDownload(false);
        p.setSyntheticFallbacks(true); // enable synthetic fallbacks for these capability TDD tests
        backend = new DjlVisionBackend(p);
        backend.initialize();
    }

    @AfterAll
    static void teardown() { if (backend != null) backend.shutdown(); }

    @Test
    void classifyImageReturnsResult() throws IOException {
        ImageData img = TestImageUtils.createGeometricShapeImage(224, 224, "circle");
        var res = backend.classifyImage(img, 3);
        assertThat(res).isNotNull();
    }
}
