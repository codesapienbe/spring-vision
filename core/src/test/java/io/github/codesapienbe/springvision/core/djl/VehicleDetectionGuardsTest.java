package io.github.codesapienbe.springvision.core.djl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.exception.VisionBackendException;
import io.github.codesapienbe.springvision.core.exception.VisionUnsupportedException;

/**
 * Unit tests for guard clauses in {@link DjlVisionBackend#detectVehicles} and
 * {@link DjlVisionBackend#detectVehicleDamages}. Happy paths are covered by the
 * integration tests that require the ONNX model to be bundled.
 */
@DisplayName("Vehicle detection guard clauses")
class VehicleDetectionGuardsTest {

    private DjlVisionBackend backend;
    private ImageData dummyImage;

    @BeforeEach
    void setUp() {
        backend = new DjlVisionBackend();
        dummyImage = new ImageData(new byte[]{1, 2, 3}, "image/jpeg", 3, "jpeg");
    }

    @Nested
    @DisplayName("detectVehicles")
    class DetectVehiclesGuards {

        @Test
        @DisplayName("throws VisionBackendException when backend not initialised")
        void throwsWhenNotInitialized() {
            assertThatThrownBy(() -> backend.detectVehicles(dummyImage))
                .isInstanceOf(VisionBackendException.class)
                .extracting(ex -> ((VisionBackendException) ex).getOperation())
                .isEqualTo("not_initialized");
        }

        @Test
        @DisplayName("throws VisionBackendException when object detection model missing")
        void throwsWhenObjectModelMissing() throws Exception {
            setInitialized(backend, true);
            // objectDetectionModel stays null

            assertThatThrownBy(() -> backend.detectVehicles(dummyImage))
                .isInstanceOf(VisionBackendException.class)
                .extracting(ex -> ((VisionBackendException) ex).getOperation())
                .isEqualTo("model_not_initialized");
        }

        @Test
        @DisplayName("isVehicleDetectionModelAvailable returns false when model is null")
        void availabilityFalseWhenModelNull() {
            assertThat(backend.isVehicleDetectionModelAvailable()).isFalse();
        }
    }

    @Nested
    @DisplayName("detectVehicleDamages")
    class DetectVehicleDamagesGuards {

        @Test
        @DisplayName("throws VisionBackendException when backend not initialised")
        void throwsWhenNotInitialized() {
            assertThatThrownBy(() -> backend.detectVehicleDamages(dummyImage))
                .isInstanceOf(VisionBackendException.class)
                .extracting(ex -> ((VisionBackendException) ex).getOperation())
                .isEqualTo("not_initialized");
        }

        @Test
        @DisplayName("throws VisionUnsupportedException when damage model not bundled")
        void throwsWhenDamageModelMissing() throws Exception {
            setInitialized(backend, true);
            // vehicleDamageModel stays null

            assertThatThrownBy(() -> backend.detectVehicleDamages(dummyImage))
                .isInstanceOf(VisionUnsupportedException.class)
                .hasMessageContaining("yolov11n-car-damage");
        }

        @Test
        @DisplayName("isVehicleDamageDetectionModelAvailable returns false when model is null")
        void availabilityFalseWhenModelNull() {
            assertThat(backend.isVehicleDamageDetectionModelAvailable()).isFalse();
        }
    }

    @Nested
    @DisplayName("VEHICLE_DAMAGE_CLASSES constant")
    class DamageClassesContract {

        @Test
        @DisplayName("contains exactly 14 classes matching vineetsarpal/yolov11n-car-damage")
        void has14Classes() {
            assertThat(DjlVisionBackend.VEHICLE_DAMAGE_CLASSES).hasSize(14);
        }

        @Test
        @DisplayName("includes expected damage class names")
        void containsExpectedClasses() {
            assertThat(DjlVisionBackend.VEHICLE_DAMAGE_CLASSES).contains(
                "bonnet-dent",
                "doorouter-dent",
                "front-bumper-dent",
                "Headlight-damage",
                "Taillight-Damage",
                "Front-windscreen-damage",
                "Rear-windscreen-Damage"
            );
        }
    }

    // -----------------------------------------------------------------------

    private static void setInitialized(DjlVisionBackend target, boolean value) throws Exception {
        Field f = DjlVisionBackend.class.getDeclaredField("initialized");
        f.setAccessible(true);
        f.setBoolean(target, value);
    }
}
