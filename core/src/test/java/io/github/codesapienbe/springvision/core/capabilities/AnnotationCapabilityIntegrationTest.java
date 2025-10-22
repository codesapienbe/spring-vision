package io.github.codesapienbe.springvision.core.capabilities;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.codesapienbe.springvision.core.AnnotationRequest;
import io.github.codesapienbe.springvision.core.DetectionCategory;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.djl.DjlProperties;
import io.github.codesapienbe.springvision.core.djl.DjlVisionBackend;
import io.github.codesapienbe.springvision.core.djl.TestImageUtils;

@DisplayName("Integration: AnnotationCapability")
public class AnnotationCapabilityIntegrationTest {
    private static DjlVisionBackend backend;

    @BeforeAll
    static void setup() throws Exception {
        DjlProperties p = new DjlProperties(); p.setEngine("OnnxRuntime"); p.setDevice("cpu"); p.setAutoDownload(false);
        backend = new DjlVisionBackend(p); backend.initialize();
    }

    @AfterAll
    static void teardown() { if (backend != null) backend.shutdown(); }

    @Test
    void annotateExecutes() throws IOException {
        ImageData img = TestImageUtils.createRectangleImage(320, 240, java.awt.Color.PINK);
        AnnotationRequest req = new AnnotationRequest.Builder().action(AnnotationRequest.Action.TAG).label("test").categories(Set.of(DetectionCategory.FACE)).build();
        ImageData out = backend.annotate(img, req);
        assertThat(out).isNotNull();
    }
}
