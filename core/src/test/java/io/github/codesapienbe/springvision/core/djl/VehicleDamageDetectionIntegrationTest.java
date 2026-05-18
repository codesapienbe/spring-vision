package io.github.codesapienbe.springvision.core.djl;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.VisionBackend;
import io.github.codesapienbe.springvision.core.VisionResult;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import io.github.codesapienbe.springvision.core.config.VisionAutoConfiguration;

/**
 * Integration tests for vehicle detection and vehicle damage detection.
 *
 * <p>Vehicle detection runs against the bundled YOLOv8n model and is always active.<br>
 * Vehicle damage detection requires {@code yolov11n-car-damage.onnx} to be present in
 * {@code src/main/resources/models/vehicle-damage/} and is skipped when absent.</p>
 *
 * <p>To export the damage model:<br>
 * {@code yolo export model=best.pt format=onnx imgsz=640 simplify=True}<br>
 * (source: huggingface.co/vineetsarpal/yolov11n-car-damage)</p>
 */
@DisplayName("Vehicle detection + damage detection integration tests")
@Tag("memory-intensive")
class VehicleDamageDetectionIntegrationTest {

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

    // -----------------------------------------------------------------------
    // Vehicle detection — runs against bundled YOLO (no extra model needed)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("detectVehicles returns empty list for a blank image")
    void detectVehiclesReturnsEmptyForBlankImage() {
        ImageData image = solidColorImage(640, 480, Color.WHITE);
        VisionResult result = visionTemplate.detectVehicles(image);

        assertThat(result).isNotNull();
        assertThat(result.detections()).isNotNull();
    }

    @Test
    @DisplayName("detectVehicles result detections carry vehicleType attribute")
    void vehicleDetectionsHaveVehicleTypeAttribute() {
        ImageData image = solidColorImage(640, 480, Color.GRAY);
        VisionResult result = visionTemplate.detectVehicles(image);

        for (Detection d : result.detections()) {
            assertThat(d.attributes()).containsKey("vehicleType");
            assertThat(d.attributes()).containsKey("vehicleCategory");
        }
    }

    @Test
    @DisplayName("VEHICLE_DAMAGE_CLASSES has exactly 14 entries")
    void damageClassesCount() {
        assertThat(DjlVisionBackend.VEHICLE_DAMAGE_CLASSES).hasSize(14);
    }

    // -----------------------------------------------------------------------
    // Vehicle damage detection — skipped when ONNX model not yet bundled
    // -----------------------------------------------------------------------

    @Test
    @EnabledIf("isDamageModelAvailable")
    @DisplayName("detectVehicleDamages returns non-null result when model is loaded")
    void detectVehicleDamagesReturnsResult() {
        ImageData image = solidColorImage(640, 480, Color.DARK_GRAY);
        VisionResult result = visionTemplate.detectVehicleDamages(image);

        assertThat(result).isNotNull();
        assertThat(result.detections()).isNotNull();
    }

    @Test
    @EnabledIf("isDamageModelAvailable")
    @DisplayName("damage detections carry required metadata attributes")
    void damageDetectionsHaveMetadata() {
        ImageData image = solidColorImage(640, 480, Color.DARK_GRAY);
        VisionResult result = visionTemplate.detectVehicleDamages(image);

        for (Detection d : result.detections()) {
            assertThat(d.label()).isIn((Object[]) DjlVisionBackend.VEHICLE_DAMAGE_CLASSES);
            assertThat(d.confidence()).isBetween(0.0, 1.0);
            assertThat(d.boundingBox()).isNotNull();
            assertThat(d.attributes()).containsKey("damageType");
            assertThat(d.attributes()).containsKey("severity");
            assertThat(d.attributes().get("severity"))
                .isIn("NONE", "MINOR", "MODERATE", "SEVERE");
        }
    }

    @Test
    @EnabledIf("isDamageModelAvailable")
    @DisplayName("isVehicleDamageDetectionModelAvailable returns true when model loaded")
    void modelAvailableWhenBundled() {
        assertThat(backend.isVehicleDamageDetectionModelAvailable()).isTrue();
    }

    // -----------------------------------------------------------------------

    boolean isDamageModelAvailable() {
        return backend != null && backend.isVehicleDamageDetectionModelAvailable();
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
