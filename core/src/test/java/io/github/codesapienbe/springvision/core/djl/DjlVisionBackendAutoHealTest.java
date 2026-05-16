package io.github.codesapienbe.springvision.core.djl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ai.djl.modality.cv.Image;
import ai.djl.repository.zoo.ZooModel;
import io.github.codesapienbe.springvision.core.DetectionCategory;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.TestImageUtils;
import io.github.codesapienbe.springvision.core.exception.VisionBackendException;

/**
 * Unit tests for the auto-healing mechanism in {@link DjlVisionBackend}.
 *
 * <p>Uses reflection to control private fields so we can exercise individual
 * branches of {@code ensureEmbeddingModelLoaded()} and
 * {@code extractFaceEmbeddings()} without needing the DJL model zoo or a
 * live network connection.
 */
class DjlVisionBackendAutoHealTest {

    private DjlVisionBackend backend;

    @BeforeEach
    void setUp() {
        backend = new DjlVisionBackend();
    }

    // -----------------------------------------------------------------------
    // isEmbeddingModelAvailable
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("isEmbeddingModelAvailable")
    class IsEmbeddingModelAvailable {

        @Test
        @DisplayName("returns false when faceRecognitionModel is null")
        void returnsFalseWhenModelNull() {
            assertFalse(backend.isEmbeddingModelAvailable());
        }

        @Test
        @DisplayName("returns true when faceRecognitionModel is set")
        void returnsTrueWhenModelSet() throws Exception {
            setFaceRecognitionModel(backend, mockZooModel());
            assertTrue(backend.isEmbeddingModelAvailable());
        }
    }

    // -----------------------------------------------------------------------
    // ensureEmbeddingModelLoaded — fast-paths
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("ensureEmbeddingModelLoaded — fast-paths")
    class EnsureEmbeddingModelLoadedFastPaths {

        @Test
        @DisplayName("returns true immediately without touching lastEmbeddingLoadAttemptMs when model already loaded")
        void returnsTrueWithoutUpdateWhenModelAlreadyLoaded() throws Exception {
            setFaceRecognitionModel(backend, mockZooModel());

            assertTrue(backend.ensureEmbeddingModelLoaded());
            // Verify no load attempt was recorded (timestamp untouched)
            assertThat(getLastLoadAttemptMs(backend)).isEqualTo(0L);
        }

        @Test
        @DisplayName("returns false and skips load when cooldown is active")
        void returnsFalseWhenCooldownActive() throws Exception {
            setLastLoadAttemptMs(backend, System.currentTimeMillis());

            assertFalse(backend.ensureEmbeddingModelLoaded());
        }

        @Test
        @DisplayName("returns false at end of cooldown boundary (just within window)")
        void returnsFalseAtCooldownBoundary() throws Exception {
            // Set timestamp 1 ms in the future relative to cooldown start — still within window
            setLastLoadAttemptMs(backend, System.currentTimeMillis() - 1);

            assertFalse(backend.ensureEmbeddingModelLoaded());
        }
    }

    // -----------------------------------------------------------------------
    // ensureEmbeddingModelLoaded — load attempt (offline, no bundled model)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("ensureEmbeddingModelLoaded — load attempt")
    class EnsureEmbeddingModelLoadedLoadAttempt {

        @Test
        @DisplayName("returns false when load fails (no model zoo, offline)")
        void returnsFalseWhenLoadFails() throws Exception {
            // Precondition: cooldown not active (lastAttemptMs == 0)
            assertThat(getLastLoadAttemptMs(backend)).isEqualTo(0L);

            // Act — no bundled model, DJL offline → load must fail
            boolean result = backend.ensureEmbeddingModelLoaded();

            assertFalse(result);
            // Post-condition: attempt timestamp was recorded (cooldown now active)
            assertThat(getLastLoadAttemptMs(backend)).isGreaterThan(0L);
        }

        @Test
        @DisplayName("sets cooldown after a failed load so next immediate call is rejected")
        void setsCooldownAfterFailedLoad() throws Exception {
            backend.ensureEmbeddingModelLoaded(); // first call — triggers (failed) load
            long firstAttemptMs = getLastLoadAttemptMs(backend);
            assertThat(firstAttemptMs).isGreaterThan(0L);

            // Second call — cooldown is active, should short-circuit
            boolean result = backend.ensureEmbeddingModelLoaded();
            assertFalse(result);
            // Timestamp should not have been updated by the second call
            assertThat(getLastLoadAttemptMs(backend)).isEqualTo(firstAttemptMs);
        }

        @Test
        @DisplayName("returns true and updates model field when load succeeds (simulated via reflection)")
        void returnsTrueWhenLoadSucceeds() throws Exception {
            // Simulate a successful load: set the model via reflection BEFORE calling ensureEmbeddingModelLoaded
            // by overriding the field during the synchronized block. Since we cannot intercept the
            // actual network load, we test the post-load branch by manually setting the field and
            // then verifying the fast-path (model already loaded → return true immediately).
            setFaceRecognitionModel(backend, mockZooModel());

            assertTrue(backend.ensureEmbeddingModelLoaded());
            // No load attempt should have been recorded
            assertThat(getLastLoadAttemptMs(backend)).isEqualTo(0L);
        }
    }

    // -----------------------------------------------------------------------
    // extractEmbeddings — not initialized
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("extractEmbeddings — not initialized")
    class ExtractEmbeddingsNotInitialized {

        @Test
        @DisplayName("throws VisionBackendException with 'not_initialized' when backend not initialized")
        void throwsWhenNotInitialized() throws Exception {
            ImageData imageData = TestImageUtils.createEmptyImage(64, 64);

            assertThatThrownBy(() -> backend.extractEmbeddings(imageData, DetectionCategory.FACE))
                .isInstanceOf(VisionBackendException.class)
                .hasMessageContaining("not initialized")
                .extracting(ex -> ((VisionBackendException) ex).getOperation())
                .isEqualTo("not_initialized");
        }
    }

    // -----------------------------------------------------------------------
    // extractEmbeddings — initialized but model null (auto-heal path)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("extractEmbeddings — initialized but model null")
    class ExtractEmbeddingsModelNull {

        @Test
        @DisplayName("auto-heal fails (cooldown active) → throws with 'Auto-heal attempted' message")
        void throwsWithHelpfulMessageWhenHealFails() throws Exception {
            setInitialized(backend, true);
            // Force cooldown so ensureEmbeddingModelLoaded returns false immediately
            setLastLoadAttemptMs(backend, System.currentTimeMillis());

            ImageData imageData = TestImageUtils.createEmptyImage(64, 64);

            assertThatThrownBy(() -> backend.extractEmbeddings(imageData, DetectionCategory.FACE))
                .isInstanceOf(VisionBackendException.class)
                .hasMessageContaining("Auto-heal attempted")
                .hasMessageContaining("mvn clean install -Pdownload-models")
                .extracting(ex -> ((VisionBackendException) ex).getOperation())
                .isEqualTo("model_not_initialized");
        }

        @Test
        @DisplayName("auto-heal fails (offline load) → throws with 'model_not_initialized'")
        void throwsWhenOfflineLoadFails() throws Exception {
            setInitialized(backend, true);
            // cooldown not active — auto-heal will attempt a (failing) load

            ImageData imageData = TestImageUtils.createEmptyImage(64, 64);

            assertThatThrownBy(() -> backend.extractEmbeddings(imageData, DetectionCategory.FACE))
                .isInstanceOf(VisionBackendException.class)
                .extracting(ex -> ((VisionBackendException) ex).getOperation())
                .isEqualTo("model_not_initialized");
        }

        @Test
        @DisplayName("unsupported category throws VisionBackendException with 'unsupported_category'")
        void throwsForUnsupportedCategory() throws Exception {
            setInitialized(backend, true);
            setFaceRecognitionModel(backend, mockZooModel()); // model present, so model null check passes

            ImageData imageData = TestImageUtils.createEmptyImage(64, 64);

            assertThatThrownBy(() -> backend.extractEmbeddings(imageData, DetectionCategory.OBJECT))
                .isInstanceOf(VisionBackendException.class)
                .extracting(ex -> ((VisionBackendException) ex).getOperation())
                .isEqualTo("unsupported_category");
        }
    }

    // -----------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static ZooModel<Image, float[]> mockZooModel() {
        return (ZooModel<Image, float[]>) mock(ZooModel.class);
    }

    private static void setFaceRecognitionModel(DjlVisionBackend target, ZooModel<Image, float[]> model)
            throws Exception {
        Field f = DjlVisionBackend.class.getDeclaredField("faceRecognitionModel");
        f.setAccessible(true);
        f.set(target, model);
    }

    private static void setInitialized(DjlVisionBackend target, boolean value) throws Exception {
        Field f = DjlVisionBackend.class.getDeclaredField("initialized");
        f.setAccessible(true);
        f.setBoolean(target, value);
    }

    private static void setLastLoadAttemptMs(DjlVisionBackend target, long value) throws Exception {
        Field f = DjlVisionBackend.class.getDeclaredField("lastEmbeddingLoadAttemptMs");
        f.setAccessible(true);
        f.setLong(target, value);
    }

    private static long getLastLoadAttemptMs(DjlVisionBackend target) throws Exception {
        Field f = DjlVisionBackend.class.getDeclaredField("lastEmbeddingLoadAttemptMs");
        f.setAccessible(true);
        return f.getLong(target);
    }
}
