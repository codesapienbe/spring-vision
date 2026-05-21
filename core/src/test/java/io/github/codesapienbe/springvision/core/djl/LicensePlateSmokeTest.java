package io.github.codesapienbe.springvision.core.djl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.VisionBackend;
import io.github.codesapienbe.springvision.core.VisionResult;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import io.github.codesapienbe.springvision.core.config.VisionAutoConfiguration;

/**
 * End-to-end smoke test: load the real damaged-car frame from /tmp and run
 * detect + OCR. Intentionally not tagged {@code memory-intensive} so it runs
 * under the default Surefire invocation when the sample image is present.
 * Skipped when the image is absent or the plate model isn't bundled.
 */
@DisplayName("License plate smoke test against /tmp/damaged-car.jpg")
class LicensePlateSmokeTest {

    @Test
    @DisplayName("recognizes a UK plate end-to-end")
    void smokeTest() throws Exception {
        Path sample = Path.of("/tmp/damaged-car.jpg");
        assumeTrue(Files.exists(sample), "sample /tmp/damaged-car.jpg not present");
        System.setProperty("ai.djl.offline", "true");

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.getEnvironment().getPropertySources().addFirst(
            new MapPropertySource("test-props",
                java.util.Map.of("vision.metrics.enabled", "false",
                    "vision.health.enabled", "false")));
        ctx.register(VisionAutoConfiguration.class);
        ctx.refresh();

        VisionTemplate template = ctx.getBean(VisionTemplate.class);
        DjlVisionBackend backend = (DjlVisionBackend) ctx.getBean(VisionBackend.class);

        assumeTrue(backend.isLicensePlateRecognitionAvailable(),
            "license-plate model or OCR runtime not available");

        byte[] bytes = Files.readAllBytes(sample);
        VisionResult result = template.recognizeLicensePlates(ImageData.fromBytes(bytes));

        System.out.println("[PLATE_SMOKE] plates=" + result.detections().size());
        for (Detection d : result.detections()) {
            System.out.printf("[PLATE_SMOKE] text='%s' rawOcr='%s' conf=%.3f bbox=%s%n",
                d.label(),
                d.attributes().get("rawOcrText"),
                d.confidence(),
                d.boundingBox());
        }
        assertThat(result.detections()).isNotEmpty();
        ctx.close();
    }
}
