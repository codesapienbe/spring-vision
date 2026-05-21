package io.github.codesapienbe.springvision.core.djl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.VisionBackend;
import io.github.codesapienbe.springvision.core.VisionResult;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import io.github.codesapienbe.springvision.core.config.VisionAutoConfiguration;
import io.github.codesapienbe.springvision.core.exception.VisionUnsupportedException;

/**
 * Integration tests for license-plate recognition.
 *
 * <p>The detector model {@code yolov8n-license-plate.onnx} must be placed under
 * {@code core/models/license-plate/} for the model-bound tests to run. When the
 * model is absent the tests assert the documented unavailable-path behaviour
 * (a {@link VisionUnsupportedException} with a clear sourcing message) so the
 * contract is still exercised in CI.</p>
 *
 * <p>To source the model: download from huggingface.co/keremberke/yolov8n-license-plate
 * and export to ONNX with
 * {@code yolo export model=best.pt format=onnx imgsz=640 simplify=True}.</p>
 */
@DisplayName("License plate recognition integration tests")
@Tag("memory-intensive")
class LicensePlateRecognitionIntegrationTest {

    private VisionTemplate visionTemplate;
    private DjlVisionBackend backend;

    @BeforeEach
    void setUp() {
        System.setProperty("ai.djl.offline", "true");

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(
            new MapPropertySource("test-props",
                java.util.Map.of("vision.metrics.enabled", "false",
                    "vision.health.enabled", "false")));
        context.register(VisionAutoConfiguration.class);
        context.refresh();

        visionTemplate = context.getBean(VisionTemplate.class);
        backend = (DjlVisionBackend) context.getBean(VisionBackend.class);
    }

    @Test
    @DisplayName("recognizeLicensePlates throws VisionUnsupportedException when the detector model is not bundled")
    void recognizeLicensePlatesFailsLoudlyWhenModelMissing() {
        assumeTrue(!backend.isLicensePlateRecognitionAvailable(),
            "license-plate model is bundled — this test verifies the absent-model path");
        ImageData image = solidColorImage(640, 480, Color.WHITE);
        assertThatThrownBy(() -> visionTemplate.recognizeLicensePlates(image))
            .isInstanceOf(VisionUnsupportedException.class)
            .hasMessageContaining("yolov8n-license-plate");
    }

    @Test
    @DisplayName("recognizeLicensePlates returns non-null result when the detector model is loaded")
    void recognizeLicensePlatesReturnsResultWhenModelAvailable() {
        assumeTrue(backend.isLicensePlateRecognitionAvailable(),
            "yolov8n-license-plate.onnx not bundled or OCR unavailable");
        ImageData image = solidColorImage(640, 480, Color.DARK_GRAY);
        VisionResult result = visionTemplate.recognizeLicensePlates(image);

        assertThat(result).isNotNull();
        assertThat(result.detections()).isNotNull();
        for (Detection d : result.detections()) {
            assertThat(d.confidence()).isBetween(0.0, 1.0);
            assertThat(d.boundingBox()).isNotNull();
            assertThat(d.attributes()).containsKeys("plateText", "rawOcrText", "detectionConfidence");
        }
    }

    @Test
    @DisplayName("recognizeLicensePlates reads a real-world UK plate from /tmp/damaged-car.jpg when available")
    void recognizeRealWorldPlate() throws Exception {
        assumeTrue(backend.isLicensePlateRecognitionAvailable(),
            "yolov8n-license-plate.onnx not bundled or OCR unavailable");
        Path sample = Path.of("/tmp/damaged-car.jpg");
        assumeTrue(Files.exists(sample),
            "sample image /tmp/damaged-car.jpg not present; skip");
        byte[] bytes = Files.readAllBytes(sample);
        VisionResult result = visionTemplate.recognizeLicensePlates(ImageData.fromBytes(bytes));

        System.out.println("PLATE_TEST plates=" + result.detections().size());
        for (Detection d : result.detections()) {
            System.out.printf("PLATE_TEST text='%s' rawOcr='%s' conf=%.3f bbox=%s%n",
                d.label(),
                d.attributes().get("rawOcrText"),
                d.confidence(),
                d.boundingBox());
        }
        assertThat(result.detections()).isNotEmpty();
    }

    private static ImageData solidColorImage(int w, int h, Color color) {
        try {
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(color);
            g.fillRect(0, 0, w, h);
            g.dispose();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return ImageData.fromBytes(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
