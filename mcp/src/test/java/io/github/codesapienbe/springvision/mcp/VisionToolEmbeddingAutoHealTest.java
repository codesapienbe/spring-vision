package io.github.codesapienbe.springvision.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.codesapienbe.springvision.core.DetectionCategory;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.VectorService;
import io.github.codesapienbe.springvision.core.VisionBackend;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import io.github.codesapienbe.springvision.core.capabilities.EmbeddingCapability;
import io.github.codesapienbe.springvision.core.exception.VisionBackendException;
import io.github.codesapienbe.springvision.core.exception.VisionProcessingException;

/**
 * Unit tests for the auto-healing logic in {@link VisionTool} embedding tools.
 *
 * <p>All failure paths now throw exceptions (no silent JSON error returns) per the
 * "No data is better than wrong data" policy. Tests verify that:
 * <ul>
 *   <li>Model-not-loaded → {@link VisionBackendException} thrown</li>
 *   <li>null/empty input → {@link VisionProcessingException} thrown</li>
 *   <li>Backend exceptions propagate unmodified</li>
 *   <li>Success paths return the expected response map</li>
 * </ul>
 */
class VisionToolEmbeddingAutoHealTest {

    /** Combined VisionBackend + EmbeddingCapability mock interface. */
    interface EmbeddingBackend extends VisionBackend, EmbeddingCapability {}

    private EmbeddingBackend embeddingBackend;
    private VectorService vectorService;
    private VisionTool visionTool;

    @BeforeEach
    void setUp() {
        embeddingBackend = mock(EmbeddingBackend.class, withSettings());
        vectorService = mock(VectorService.class);
        when(vectorService.embeddingToBytes(any())).thenReturn(new byte[0]);
        VisionTemplate template = new VisionTemplate(embeddingBackend, vectorService);
        visionTool = new VisionTool(template);
    }

    // ============================= embeddingModelStatus =============================

    @Nested
    @DisplayName("embeddingModelStatus (via tool output)")
    class EmbeddingModelStatus {

        @Test
        @DisplayName("returns success with model_status=available when model is loaded")
        void reportsAvailableWhenModelLoaded() {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(true);
            when(embeddingBackend.extractEmbeddings(any(), any())).thenReturn(List.of());

            Map<String, Object> response = visionTool.extractEmbeddings(buildMinimalPng());

            assertThat(response.get("status")).isEqualTo("success");
            assertThat(response.get("model_status")).isEqualTo("available");
        }

        @Test
        @DisplayName("throws VisionBackendException when model unavailable and heal fails")
        void reportsNotLoadedWhenModelUnavailableAndHealFails() {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(false);
            when(embeddingBackend.ensureEmbeddingModelLoaded()).thenReturn(false);

            assertThatThrownBy(() -> visionTool.extractEmbeddings(new byte[]{1, 2, 3}))
                .isInstanceOf(VisionBackendException.class)
                .hasMessageContaining("Auto-heal attempted");
        }

        @Test
        @DisplayName("throws VisionUnsupportedException for non-EmbeddingCapability backend")
        void throwsForNonEmbeddingCapabilityBackend() {
            VisionBackend plainBackend = mock(VisionBackend.class);
            VectorService vs = mock(VectorService.class);
            when(vs.embeddingToBytes(any())).thenReturn(new byte[0]);
            VisionTemplate plainTemplate = new VisionTemplate(plainBackend, vs);
            VisionTool plainTool = new VisionTool(plainTemplate);

            assertThatThrownBy(() -> plainTool.extractEmbeddings(buildMinimalPng()))
                .isInstanceOf(io.github.codesapienbe.springvision.core.exception.VisionUnsupportedException.class);
        }
    }

    // ============================= healEmbeddingModel =============================

    @Nested
    @DisplayName("healEmbeddingModel (via tool output)")
    class HealEmbeddingModel {

        @Test
        @DisplayName("heal succeeds: ensureEmbeddingModelLoaded is called and tool proceeds")
        void healSucceedsAndToolProceeds() {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(false);
            when(embeddingBackend.ensureEmbeddingModelLoaded()).thenReturn(true);
            when(embeddingBackend.extractEmbeddings(any(), any())).thenReturn(List.of());

            Map<String, Object> response = visionTool.extractEmbeddings(buildMinimalPng());

            verify(embeddingBackend).ensureEmbeddingModelLoaded();
            assertThat(response.get("status")).isEqualTo("success");
            assertThat(response.get("model_status")).isNotEqualTo("not_loaded");
        }

        @Test
        @DisplayName("heal fails: throws VisionBackendException with actionable message")
        void healFailsAndErrorIsReturned() {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(false);
            when(embeddingBackend.ensureEmbeddingModelLoaded()).thenReturn(false);

            assertThatThrownBy(() -> visionTool.extractEmbeddings(new byte[]{1, 2, 3}))
                .isInstanceOf(VisionBackendException.class)
                .hasMessageContaining("mvn clean install -Pdownload-models");

            verify(embeddingBackend).ensureEmbeddingModelLoaded();
        }

        @Test
        @DisplayName("heal throws: exception is caught internally; model still not loaded → VisionBackendException")
        void healExceptionIsCaughtSafely() {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(false);
            when(embeddingBackend.ensureEmbeddingModelLoaded())
                .thenThrow(new RuntimeException("simulated load failure"));

            // healEmbeddingModel() catches the exception and returns false.
            // The not-loaded state then throws VisionBackendException.
            assertThatThrownBy(() -> visionTool.extractEmbeddings(new byte[]{1, 2, 3}))
                .isInstanceOf(VisionBackendException.class);
        }

        @Test
        @DisplayName("heal is NOT called when model is already available")
        void healNotCalledWhenModelAvailable() {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(true);
            when(embeddingBackend.extractEmbeddings(any(), any())).thenReturn(List.of());

            visionTool.extractEmbeddings(buildMinimalPng());

            verify(embeddingBackend, never()).ensureEmbeddingModelLoaded();
        }
    }

    // ============================= extractEmbeddingsFromTemplate =============================

    @Nested
    @DisplayName("extractEmbeddingsFromTemplate (via tool output)")
    class ExtractEmbeddingsFromTemplate {

        @Test
        @DisplayName("returns embeddings when backend succeeds")
        void returnsEmbeddingsOnBackendSuccess() {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(true);
            float[] vec = {0.1f, 0.2f, 0.3f};
            when(embeddingBackend.extractEmbeddings(any(ImageData.class), any(DetectionCategory.class)))
                .thenReturn(List.of(vec));

            Map<String, Object> response = visionTool.extractEmbeddings(buildMinimalPng());

            assertThat(response.get("status")).isEqualTo("success");
            assertThat(response.get("count")).isEqualTo(1);
        }

        @Test
        @DisplayName("propagates VisionBackendException from backend — not silently swallowed")
        void propagatesVisionBackendExceptionFromBackend() {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(true);
            when(embeddingBackend.extractEmbeddings(any(ImageData.class), any(DetectionCategory.class)))
                .thenThrow(new VisionBackendException("model_not_initialized", "model_not_initialized", null));

            assertThatThrownBy(() -> visionTool.extractEmbeddings(buildMinimalPng()))
                .isInstanceOf(VisionBackendException.class)
                .hasMessageContaining("model_not_initialized");
        }

        @Test
        @DisplayName("propagates RuntimeException from backend — not swallowed")
        void propagatesRuntimeExceptionFromBackend() {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(true);
            when(embeddingBackend.extractEmbeddings(any(ImageData.class), any(DetectionCategory.class)))
                .thenThrow(new IllegalStateException("internal error"));

            assertThatThrownBy(() -> visionTool.extractEmbeddings(buildMinimalPng()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("internal error");
        }

        @Test
        @DisplayName("throws VisionUnsupportedException when backend is not EmbeddingCapability")
        void throwsForNonEmbeddingCapabilityBackend() {
            VisionBackend plain = mock(VisionBackend.class);
            VectorService vs = mock(VectorService.class);
            when(vs.embeddingToBytes(any())).thenReturn(new byte[0]);
            VisionTemplate tpl = new VisionTemplate(plain, vs);
            VisionTool tool = new VisionTool(tpl);

            assertThatThrownBy(() -> tool.extractEmbeddings(buildMinimalPng()))
                .isInstanceOf(io.github.codesapienbe.springvision.core.exception.VisionUnsupportedException.class);
        }
    }

    // ============================= extractEmbeddings(byte[]) =============================

    @Nested
    @DisplayName("extractEmbeddings(byte[]) — all branches")
    class ExtractEmbeddingsBytesTool {

        @Test
        @DisplayName("throws VisionBackendException with actionable message when heal fails")
        void throwsNotLoadedExceptionWhenHealFails() {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(false);
            when(embeddingBackend.ensureEmbeddingModelLoaded()).thenReturn(false);

            assertThatThrownBy(() -> visionTool.extractEmbeddings(new byte[]{1, 2, 3}))
                .isInstanceOf(VisionBackendException.class)
                .hasMessageContaining("mvn clean install -Pdownload-models");

            verify(embeddingBackend).ensureEmbeddingModelLoaded();
        }

        @Test
        @DisplayName("heal is attempted and tool proceeds when heal succeeds")
        void proceedsAfterSuccessfulHeal() {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(false);
            when(embeddingBackend.ensureEmbeddingModelLoaded()).thenReturn(true);
            when(embeddingBackend.extractEmbeddings(any(), any())).thenReturn(List.of());

            Map<String, Object> response = visionTool.extractEmbeddings(buildMinimalPng());

            verify(embeddingBackend).ensureEmbeddingModelLoaded();
            assertThat(response.get("model_status")).isNotEqualTo("not_loaded");
        }

        @Test
        @DisplayName("processingTimeMs is a non-negative Long in success response")
        void processingTimeMsIsNonNegativeLong() {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(true);
            when(embeddingBackend.extractEmbeddings(any(), any())).thenReturn(List.of());

            Map<String, Object> response = visionTool.extractEmbeddings(buildMinimalPng());

            assertThat(response.get("processingTimeMs"))
                .isInstanceOf(Long.class)
                .satisfies(v -> assertThat((Long) v).isGreaterThanOrEqualTo(0L));
        }

        @Test
        @DisplayName("processingTimeMs is present and non-negative on success path")
        void processingTimeMsPresentOnSuccess() {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(true);
            when(embeddingBackend.extractEmbeddings(any(), any())).thenReturn(List.of());

            Map<String, Object> response = visionTool.extractEmbeddings(buildMinimalPng());

            assertThat(response.get("processingTimeMs"))
                .isInstanceOf(Long.class)
                .satisfies(v -> assertThat((Long) v).isGreaterThanOrEqualTo(0L));
        }

        @Test
        @DisplayName("model_status=available is in success response")
        void modelStatusAvailableInSuccessResponse() {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(true);
            when(embeddingBackend.extractEmbeddings(any(), any())).thenReturn(List.of());

            Map<String, Object> response = visionTool.extractEmbeddings(buildMinimalPng());

            assertThat(response.get("status")).isEqualTo("success");
            assertThat(response.get("model_status")).isEqualTo("available");
        }

        @Test
        @DisplayName("throws VisionProcessingException for null input")
        void throwsForNullInput() {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(true);

            assertThatThrownBy(() -> visionTool.extractEmbeddings((byte[]) null))
                .isInstanceOf(VisionProcessingException.class);
        }

        @Test
        @DisplayName("throws VisionProcessingException for empty byte array")
        void throwsForEmptyBytes() {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(true);

            assertThatThrownBy(() -> visionTool.extractEmbeddings(new byte[0]))
                .isInstanceOf(VisionProcessingException.class);
        }
    }

    // ============================= extractEmbeddings(String) =============================

    @Nested
    @DisplayName("extractEmbeddings(String) — all branches")
    class ExtractEmbeddingsUrlTool {

        @Test
        @DisplayName("throws VisionBackendException with actionable message when heal fails")
        void throwsNotLoadedExceptionWhenHealFails() {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(false);
            when(embeddingBackend.ensureEmbeddingModelLoaded()).thenReturn(false);

            assertThatThrownBy(() -> visionTool.extractEmbeddings("https://example.com/face.jpg"))
                .isInstanceOf(VisionBackendException.class)
                .hasMessageContaining("mvn clean install -Pdownload-models");

            verify(embeddingBackend).ensureEmbeddingModelLoaded();
        }

        @Test
        @DisplayName("throws VisionProcessingException when model is available but URL fails")
        void throwsWhenModelAvailableButUrlFails() {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(true);

            assertThatThrownBy(() -> visionTool.extractEmbeddings("https://example.invalid/face.jpg"))
                .isInstanceOf(VisionProcessingException.class);
        }

        @Test
        @DisplayName("throws VisionBackendException when model not loaded (URL branch)")
        void throwsNotLoadedInUrlBranch() {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(false);
            when(embeddingBackend.ensureEmbeddingModelLoaded()).thenReturn(false);

            assertThatThrownBy(() -> visionTool.extractEmbeddings("https://example.com/img.png"))
                .isInstanceOf(VisionBackendException.class);
        }

        @Test
        @DisplayName("returns error JSON for null URL (null/empty validation is non-throwing)")
        void returnsErrorForNullUrl() {
            Map<String, Object> response = visionTool.extractEmbeddings((String) null);

            assertThat(response.get("status")).isEqualTo("error");
            assertThat(response.get("embeddings")).isEqualTo(List.of());
        }

        @Test
        @DisplayName("returns error JSON for empty URL (null/empty validation is non-throwing)")
        void returnsErrorForEmptyUrl() {
            Map<String, Object> response = visionTool.extractEmbeddings("   ");

            assertThat(response.get("status")).isEqualTo("error");
            assertThat(response.get("embeddings")).isEqualTo(List.of());
        }

        @Test
        @DisplayName("throws VisionBackendException (not return JSON) when heal fails")
        void throwsRatherThanReturnsErrorWhenHealFails() {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(false);
            when(embeddingBackend.ensureEmbeddingModelLoaded()).thenReturn(false);

            assertThatThrownBy(() -> visionTool.extractEmbeddings("https://example.com/face.jpg"))
                .isInstanceOf(VisionBackendException.class);
        }

        @Test
        @DisplayName("ensureEmbeddingModelLoaded NOT called when model is already available")
        void healNotCalledWhenModelAvailable() {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(true);

            assertThatThrownBy(() -> visionTool.extractEmbeddings("https://example.invalid/face.jpg"))
                .isInstanceOf(VisionProcessingException.class);

            verify(embeddingBackend, never()).ensureEmbeddingModelLoaded();
        }
    }

    // ============================= helpers =============================

    /**
     * Builds the minimal valid 1×1 white PNG bytes so {@code ImageData.fromBytes}
     * succeeds and we can test the post-model-check extraction path.
     */
    static byte[] buildMinimalPng() {
        try {
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                1, 1, java.awt.image.BufferedImage.TYPE_INT_RGB);
            img.setRGB(0, 0, 0xFFFFFF);
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(img, "png", baos);
            return baos.toByteArray();
        } catch (java.io.IOException e) {
            throw new RuntimeException("Could not create test PNG", e);
        }
    }
}
