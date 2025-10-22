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

@DisplayName("Integration: DemographicsCapability")
public class DemographicsCapabilityIntegrationTest {

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
    @DisplayName("Should return mock demographics for detected faces")
    void shouldReturnMockDemographics() throws IOException {
        ImageData image = TestImageUtils.createSimpleFaceImage(320, 240);

        List<Detection> demos = backend.detectDemographics(image);

        assertThat(demos).isNotNull();
        assertThat(demos).isInstanceOf(List.class);
    }
}


