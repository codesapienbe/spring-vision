package io.github.codesapienbe.springvision.core.djl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ai.djl.modality.cv.Image;
import ai.djl.repository.zoo.ZooModel;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.exception.VisionBackendException;
import io.github.codesapienbe.springvision.core.exception.VisionProcessingException;

/**
 * Unit tests for the three guard clauses in {@link DjlVisionBackend#detectNSFW(ImageData)}.
 *
 * <p>The happy path is exercised by the network-dependent integration test in
 * {@code mcp/VisionToolIntegrationTest}; this class pins the loud-failure
 * contract for uninitialised state and bad input so future refactors cannot
 * regress to silent "normal" classification.
 */
@DisplayName("DjlVisionBackend.detectNSFW — guard clauses")
class DjlVisionBackendNsfwGuardsTest {

    private DjlVisionBackend backend;

    @BeforeEach
    void setUp() {
        backend = new DjlVisionBackend();
    }

    @Test
    @DisplayName("throws VisionBackendException(not_initialized) when backend uninitialised")
    void throwsWhenBackendNotInitialized() {
        ImageData img = new ImageData(new byte[] {1, 2, 3}, "image/jpeg", 3, "jpeg");

        assertThatThrownBy(() -> backend.detectNSFW(img))
            .isInstanceOf(VisionBackendException.class)
            .extracting(ex -> ((VisionBackendException) ex).getOperation())
            .isEqualTo("not_initialized");
    }

    @Test
    @DisplayName("throws VisionBackendException(model_not_initialized) when initialised but model missing")
    void throwsWhenModelNotLoaded() throws Exception {
        setInitialized(backend, true);
        // nsfwModel left as null

        ImageData img = new ImageData(new byte[] {1, 2, 3}, "image/jpeg", 3, "jpeg");

        assertThatThrownBy(() -> backend.detectNSFW(img))
            .isInstanceOf(VisionBackendException.class)
            .extracting(ex -> ((VisionBackendException) ex).getOperation())
            .isEqualTo("model_not_initialized");
    }

    @Test
    @DisplayName("throws VisionProcessingException(null_input) when ImageData is null")
    void throwsWhenImageDataNull() throws Exception {
        setInitialized(backend, true);
        setNsfwModel(backend, mockZooModel());

        assertThatThrownBy(() -> backend.detectNSFW(null))
            .isInstanceOf(VisionProcessingException.class)
            .extracting(ex -> ((VisionProcessingException) ex).getOperation())
            .isEqualTo("null_input");
    }

    @Test
    @DisplayName("throws VisionProcessingException(null_input) when image bytes are null")
    void throwsWhenImageBytesNull() throws Exception {
        setInitialized(backend, true);
        setNsfwModel(backend, mockZooModel());

        ImageData img = new ImageData(null, "image/jpeg", 0, "jpeg");

        assertThatThrownBy(() -> backend.detectNSFW(img))
            .isInstanceOf(VisionProcessingException.class)
            .extracting(ex -> ((VisionProcessingException) ex).getOperation())
            .isEqualTo("null_input");
    }

    @Test
    @DisplayName("throws VisionProcessingException(null_input) when image bytes are empty")
    void throwsWhenImageBytesEmpty() throws Exception {
        setInitialized(backend, true);
        setNsfwModel(backend, mockZooModel());

        ImageData img = new ImageData(new byte[0], "image/jpeg", 0, "jpeg");

        assertThatThrownBy(() -> backend.detectNSFW(img))
            .isInstanceOf(VisionProcessingException.class)
            .extracting(ex -> ((VisionProcessingException) ex).getOperation())
            .isEqualTo("null_input");
    }

    // -----------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static ZooModel<Image, float[]> mockZooModel() {
        return (ZooModel<Image, float[]>) mock(ZooModel.class);
    }

    private static void setInitialized(DjlVisionBackend target, boolean value) throws Exception {
        Field f = DjlVisionBackend.class.getDeclaredField("initialized");
        f.setAccessible(true);
        f.setBoolean(target, value);
    }

    private static void setNsfwModel(DjlVisionBackend target, ZooModel<Image, float[]> model) throws Exception {
        Field f = DjlVisionBackend.class.getDeclaredField("nsfwModel");
        f.setAccessible(true);
        f.set(target, model);
    }
}
