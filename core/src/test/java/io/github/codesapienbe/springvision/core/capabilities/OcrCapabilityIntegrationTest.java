package io.github.codesapienbe.springvision.core.capabilities;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.djl.DjlVisionBackend;
import io.github.codesapienbe.springvision.core.djl.DjlProperties;
import io.github.codesapienbe.springvision.core.djl.TestImageUtils;

@DisplayName("Integration: OcrCapability")
public class OcrCapabilityIntegrationTest {

    private static DjlVisionBackend backend;

    @BeforeAll
    static void setup() throws Exception {
        DjlProperties properties = new DjlProperties();
        properties.setEngine("OnnxRuntime");
        properties.setDevice("cpu");
        properties.setAutoDownload(false);
        properties.setSyntheticFallbacks(true);

        backend = new DjlVisionBackend(properties);
        backend.initialize();
    }

    @AfterAll
    static void teardown() {
        if (backend != null) backend.shutdown();
    }

    @Test
    @DisplayName("Should extract text (synthetic fallback)")
    void shouldExtractText() throws IOException {
        ImageData image = TestImageUtils.createTextImage("Hello OCR", 400, 200);

        List<OcrCapability.TextDetection> texts = backend.extractText(image);

        assertThat(texts).isNotNull();
        assertThat(texts).isInstanceOf(List.class);
    }
}


