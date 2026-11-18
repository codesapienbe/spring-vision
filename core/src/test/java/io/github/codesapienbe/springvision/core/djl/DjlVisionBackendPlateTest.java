package io.github.codesapienbe.springvision.core.djl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.opencv.SyntheticPlates;

/**
 * recognizeLicensePlates on synthetic Belgian plates; needs only the OpenCV natives and the bundled
 * Tesseract data, so the backend is marked initialised without loading DJL models.
 */
@DisplayName("DjlVisionBackend — licence-plate recognition")
class DjlVisionBackendPlateTest {

    private DjlVisionBackend backend;

    @BeforeEach
    void setUp() throws Exception {
        assumeTrue(new TesseractRunner(null, null).isAvailable(), "bundled tessdata not present");
        backend = new DjlVisionBackend();
        Field initialized = DjlVisionBackend.class.getDeclaredField("initialized");
        initialized.setAccessible(true);
        initialized.setBoolean(backend, true);
    }

    @AfterEach
    void tearDown() {
        if (backend != null) {
            backend.shutdown();
        }
    }

    @Test
    @DisplayName("reads a current-series Belgian plate and validates its format")
    void readsCurrentSeriesPlate() throws Exception {
        List<Detection> plates = backend.recognizeLicensePlates(ImageData.fromBytes(SyntheticPlates.scene("1-ABC-123")));

        assertThat(plates).anySatisfy(plate -> {
            assertThat(plate.label()).isEqualTo("1-ABC-123");
            assertThat(plate.attributes()).containsEntry("formatValid", true).containsEntry("country", "BE");
            assertThat(plate.confidence()).isBetween(0.0, 1.0);
            assertThat(plate.boundingBox().x()).isBetween(0.3, 0.4);
        });
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"1-ABC-123", "2-XYZ-789", "1-HWM-405", "ABC-123", "123-ABC"})
    @DisplayName("reads every series, also from a blurred, downscaled JPEG")
    void readsEverySeriesCleanAndDegraded(String text) throws Exception {
        byte[] clean = SyntheticPlates.scene(text);
        for (ImageData image : List.of(ImageData.fromBytes(clean),
            ImageData.fromBytes(SyntheticPlates.degraded(clean), "image/jpeg"))) {
            List<Detection> plates = backend.recognizeLicensePlates(image);
            assertThat(plates).hasSize(1);
            assertThat(plates.get(0).label()).isEqualTo(text);
            assertThat(plates.get(0).attributes()).containsEntry("formatValid", true)
                .containsEntry("confidenceKind", "weakest-character");
        }
    }

    @Test
    @DisplayName("finds no plate in a scene without one")
    void findsNothingWithoutPlate() throws Exception {
        assertThat(backend.recognizeLicensePlates(ImageData.fromBytes(SyntheticPlates.scene(null)))).isEmpty();
    }
}
