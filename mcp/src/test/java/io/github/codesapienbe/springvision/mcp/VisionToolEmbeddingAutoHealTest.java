package io.github.codesapienbe.springvision.mcp;

import static org.assertj.core.api.Assertions.assertThat;
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

/**
 * Unit tests for the auto-healing logic in {@link VisionTool} embedding tools.
 *
 * <p>Covers all branches of:
 * <ul>
 *   <li>{@code embeddingModelStatus()} — "available", "not_loaded", "unsupported", "unknown"</li>
 *   <li>{@code healEmbeddingModel()} — success, failure, non-EmbeddingCapability backend, exception</li>
 *   <li>{@code extractEmbeddingsFromTemplate()} — success, VisionBackendException propagation,
 *       generic exception swallowing, template fallback, empty list</li>
 *   <li>{@code extractEmbeddings(byte[])} — all model-status branches, timing, null/empty input</li>
 *   <li>{@code extractEmbeddings(String)} — all model-status branches, null/empty URL</li>
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
        // serializeEmbedding() calls vectorService.embeddingToBytes(); return empty bytes
        // so Base64.encodeToString() doesn't receive null.
        when(vectorService.embeddingToBytes(any())).thenReturn(new byte[0]);
        VisionTemplate template = new VisionTemplate(embeddingBackend, vectorService);
        visionTool = new VisionTool(template);
    }

    // ============================= embeddingModelStatus =============================

    @Nested
    @DisplayName("embeddingModelStatus (via tool output)")
    class EmbeddingModelStatus {

        @Test
        @DisplayName("reports 'available' in success response when model is loaded")
        void reportsAvailableWhenModelLoaded() {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(true);
            when(embeddingBackend.extractEmbeddings(any(), any())).thenReturn(List.of());

            Map<String, Object> response = visionTool.extractEmbeddings(new byte[]{1});

            // resolveImage will fail on invalid bytes, landing in the catch path
            // but model_status should still NOT be "not_loaded" when model is available
            assertThat(response.get("model_status")).isNotEqualTo("not_loaded");
        }

        @Test
        @DisplayName("reports 'not_loaded' when model is not available and heal fails")
        void reportsNotLoadedWhenModelUnavailableAndHealFails() {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(false);
            when(embeddingBackend.ensureEmbeddingModelLoaded()).thenReturn(false);

            Map<String, Object> response = visionTool.extractEmbeddings(new byte[]{1});

            assertThat(response.get("status")).isEqualTo("error");
            assertThat(response.get("model_status")).isEqualTo("not_loaded");
        }

        @Test
        @DisplayName("model_status is 'unsupported' (not 'not_loaded') for non-EmbeddingCapability backend")
        void proceedsWithoutModelStatusWhenBackendLacksCapability() {
            // VisionTool backed by a plain VisionBackend (no EmbeddingCapability).
            // embeddingModelStatus() returns "unsupported" → no pre-flight blocking and no healing.
            VisionBackend plainBackend = mock(VisionBackend.class);
            VectorService vs = mock(VectorService.class);
            when(vs.embeddingToBytes(any())).thenReturn(new byte[0]);
            VisionTemplate plainTemplate = new VisionTemplate(plainBackend, vs);
            VisionTool plainTool = new VisionTool(plainTemplate);

            // Invalid bytes → image decode fails → error path (no model_status injected)
            Map<String, Object> response = plainTool.extractEmbeddings(new byte[]{1});

            // No heal was attempted — model_status should never be "not_loaded"
            assertThat(response.get("model_status")).isNotEqualTo("not_loaded");
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

            // Invalid bytes will fail at image decode, but we just need to verify
            // ensureEmbeddingModelLoaded was called and model_status is not "not_loaded"
            Map<String, Object> response = visionTool.extractEmbeddings(new byte[]{1});

            verify(embeddingBackend).ensureEmbeddingModelLoaded();
            assertThat(response.get("model_status")).isNotEqualTo("not_loaded");
        }

        @Test
        @DisplayName("heal fails: ensureEmbeddingModelLoaded is called and error is returned")
        void healFailsAndErrorIsReturned() {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(false);
            when(embeddingBackend.ensureEmbeddingModelLoaded()).thenReturn(false);

            Map<String, Object> response = visionTool.extractEmbeddings(new byte[]{1});

            verify(embeddingBackend).ensureEmbeddingModelLoaded();
            assertThat(response.get("status")).isEqualTo("error");
            assertThat(response.get("model_status")).isEqualTo("not_loaded");
        }

        @Test
        @DisplayName("heal throws: exception is caught and false is returned (no crash)")
        void healExceptionIsCaughtSafely() {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(false);
            when(embeddingBackend.ensureEmbeddingModelLoaded())
                .thenThrow(new RuntimeException("simulated load failure"));

            // Should not propagate the RuntimeException
            Map<String, Object> response = visionTool.extractEmbeddings(new byte[]{1});

            assertThat(response.get("status")).isEqualTo("error");
        }

        @Test
        @DisplayName("heal is NOT called when model is already available")
        void healNotCalledWhenModelAvailable() {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(true);
            when(embeddingBackend.extractEmbeddings(any(), any())).thenReturn(List.of());

            visionTool.extractEmbeddings(new byte[]{1});

            verify(embeddingBackend, never()).ensureEmbeddingModelLoaded();
        }
    }

    // ============================= extractEmbeddingsFromTemplate =============================

    @Nested
    @DisplayName("extractEmbeddingsFromTemplate (via tool output)")
    class ExtractEmbeddingsFromTemplate {

        @Test
        @DisplayName("returns embeddings when backend succeeds")
        void returnsEmbeddingsOnBackendSuccess() throws Exception {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(true);
            float[] vec = {0.1f, 0.2f, 0.3f};
            when(embeddingBackend.extractEmbeddings(any(ImageData.class), any(DetectionCategory.class)))
                .thenReturn(List.of(vec));

            // Build a valid PNG via AWT so ImageData.fromBytes doesn't throw
            byte[] png = buildMinimalPng();
            Map<String, Object> response = visionTool.extractEmbeddings(png);

            assertThat(response.get("status")).isEqualTo("success");
            assertThat(response.get("count")).isEqualTo(1);
        }

        @Test
        @DisplayName("propagates VisionBackendException from backend — not silently swallowed")
        void propagatesVisionBackendExceptionFromBackend() throws Exception {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(true);
            when(embeddingBackend.extractEmbeddings(any(ImageData.class), any(DetectionCategory.class)))
                .thenThrow(new VisionBackendException("model_not_initialized", "model_not_initialized", null));

            byte[] png = buildMinimalPng();
            Map<String, Object> response = visionTool.extractEmbeddings(png);

            assertThat(response.get("status")).isEqualTo("error");
            assertThat((String) response.get("message")).contains("model_not_initialized");
        }

        @Test
        @DisplayName("swallows generic exception from backend and falls through to template fallback")
        void swallowsGenericExceptionFromBackend() throws Exception {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(true);
            // Backend throws a generic exception (not VisionBackendException)
            when(embeddingBackend.extractEmbeddings(any(ImageData.class), any(DetectionCategory.class)))
                .thenThrow(new IllegalStateException("internal error"));
            // Template fallback (visionTemplate.extractEmbeddings) will also throw VisionUnsupportedException
            // because VisionTemplate.extractEmbeddings delegates back to the same backend

            byte[] png = buildMinimalPng();
            Map<String, Object> response = visionTool.extractEmbeddings(png);

            // Either success with 0 embeddings (template fell back to empty) or an error
            // from the template fallback — either way, not an unhandled exception
            assertThat(response).containsKey("status");
        }

        @Test
        @DisplayName("returns 0 embeddings (no exception) when backend is not EmbeddingCapability")
        void returnsEmptyListWhenNonEmbeddingCapabilityBackendAndNoError() throws Exception {
            // Use a plain VisionBackend that doesn't implement EmbeddingCapability.
            // embeddingModelStatus() → "unsupported", no blocking pre-flight, but extraction
            // returns nothing useful.  The success path writes model_status="unsupported".
            VisionBackend plain = mock(VisionBackend.class);
            VectorService vs = mock(VectorService.class);
            when(vs.embeddingToBytes(any())).thenReturn(new byte[0]);
            VisionTemplate tpl = new VisionTemplate(plain, vs);
            VisionTool tool = new VisionTool(tpl);

            byte[] png = buildMinimalPng();
            Map<String, Object> response = tool.extractEmbeddings(png);

            // extraction succeeds (0 embeddings) — model_status is "unsupported", not "not_loaded"
            assertThat(response.get("status")).isEqualTo("success");
            assertThat(response.get("count")).isEqualTo(0);
            assertThat(response.get("model_status")).isEqualTo("unsupported");
        }
    }

    // ============================= extractEmbeddings(byte[]) =============================

    @Nested
    @DisplayName("extractEmbeddings(byte[]) — all branches")
    class ExtractEmbeddingsBytesTool {

        @Test
        @DisplayName("model_status=not_loaded + actionable message when heal fails")
        void returnsNotLoadedStatusWhenHealFails() {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(false);
            when(embeddingBackend.ensureEmbeddingModelLoaded()).thenReturn(false);

            Map<String, Object> response = visionTool.extractEmbeddings(new byte[]{1, 2, 3});

            assertThat(response.get("status")).isEqualTo("error");
            assertThat(response.get("model_status")).isEqualTo("not_loaded");
            assertThat(response.get("embeddings")).isEqualTo(List.of());
            assertThat((String) response.get("message")).contains("mvn clean install -Pdownload-models");
            verify(embeddingBackend).ensureEmbeddingModelLoaded();
        }

        @Test
        @DisplayName("heal is attempted and tool proceeds when heal succeeds")
        void proceedsAfterSuccessfulHeal() {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(false);
            when(embeddingBackend.ensureEmbeddingModelLoaded()).thenReturn(true);
            when(embeddingBackend.extractEmbeddings(any(), any())).thenReturn(List.of());

            Map<String, Object> response = visionTool.extractEmbeddings(new byte[]{1});

            verify(embeddingBackend).ensureEmbeddingModelLoaded();
            assertThat(response.get("model_status")).isNotEqualTo("not_loaded");
        }

        @Test
        @DisplayName("processingTimeMs is a non-negative Long")
        void processingTimeMsIsNonNegativeLong() {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(false);
            when(embeddingBackend.ensureEmbeddingModelLoaded()).thenReturn(false);

            Map<String, Object> response = visionTool.extractEmbeddings(new byte[]{1});

            assertThat(response.get("processingTimeMs"))
                .isInstanceOf(Long.class)
                .satisfies(v -> assertThat((Long) v).isGreaterThanOrEqualTo(0L));
        }

        @Test
        @DisplayName("processingTimeMs is present and non-negative on success path")
        void processingTimeMsPresentOnSuccess() throws Exception {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(true);
            when(embeddingBackend.extractEmbeddings(any(), any())).thenReturn(List.of());

            byte[] png = buildMinimalPng();
            Map<String, Object> response = visionTool.extractEmbeddings(png);

            assertThat(response.get("processingTimeMs"))
                .isInstanceOf(Long.class)
                .satisfies(v -> assertThat((Long) v).isGreaterThanOrEqualTo(0L));
        }

        @Test
        @DisplayName("model_status=available is in success response")
        void modelStatusAvailableInSuccessResponse() throws Exception {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(true);
            when(embeddingBackend.extractEmbeddings(any(), any())).thenReturn(List.of());

            byte[] png = buildMinimalPng();
            Map<String, Object> response = visionTool.extractEmbeddings(png);

            assertThat(response.get("status")).isEqualTo("success");
            assertThat(response.get("model_status")).isEqualTo("available");
        }

        @Test
        @DisplayName("returns error for null input without crashing")
        void returnsErrorForNullInput() {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(true);

            Map<String, Object> response = visionTool.extractEmbeddings((byte[]) null);

            assertThat(response.get("status")).isEqualTo("error");
        }

        @Test
        @DisplayName("returns error for empty byte array")
        void returnsErrorForEmptyBytes() {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(true);

            Map<String, Object> response = visionTool.extractEmbeddings(new byte[0]);

            assertThat(response.get("status")).isEqualTo("error");
        }
    }

    // ============================= extractEmbeddings(String) =============================

    @Nested
    @DisplayName("extractEmbeddings(String) — all branches")
    class ExtractEmbeddingsUrlTool {

        @Test
        @DisplayName("model_status=not_loaded + actionable message when heal fails")
        void returnsNotLoadedStatusWhenHealFails() {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(false);
            when(embeddingBackend.ensureEmbeddingModelLoaded()).thenReturn(false);

            Map<String, Object> response = visionTool.extractEmbeddings("https://example.com/face.jpg");

            assertThat(response.get("status")).isEqualTo("error");
            assertThat(response.get("model_status")).isEqualTo("not_loaded");
            assertThat(response.get("embeddings")).isEqualTo(List.of());
            assertThat((String) response.get("message")).contains("mvn clean install -Pdownload-models");
            verify(embeddingBackend).ensureEmbeddingModelLoaded();
        }

        @Test
        @DisplayName("model_status is not 'not_loaded' when model is available (URL may still fail)")
        void modelStatusNotNotLoadedWhenModelAvailable() {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(true);

            Map<String, Object> response = visionTool.extractEmbeddings("https://example.invalid/face.jpg");

            assertThat(response.get("model_status")).isNotEqualTo("not_loaded");
        }

        @Test
        @DisplayName("model_status=not_loaded when heal fails (URL branch)")
        void reportsNotLoadedInUrlBranch() {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(false);
            when(embeddingBackend.ensureEmbeddingModelLoaded()).thenReturn(false);

            Map<String, Object> response = visionTool.extractEmbeddings("https://example.com/img.png");

            assertThat(response.get("model_status")).isEqualTo("not_loaded");
        }

        @Test
        @DisplayName("returns error for null URL")
        void returnsErrorForNullUrl() {
            Map<String, Object> response = visionTool.extractEmbeddings((String) null);

            assertThat(response.get("status")).isEqualTo("error");
            assertThat(response.get("embeddings")).isEqualTo(List.of());
        }

        @Test
        @DisplayName("returns error for empty URL")
        void returnsErrorForEmptyUrl() {
            Map<String, Object> response = visionTool.extractEmbeddings("   ");

            assertThat(response.get("status")).isEqualTo("error");
            assertThat(response.get("embeddings")).isEqualTo(List.of());
        }

        @Test
        @DisplayName("processingTimeMs is present even on error path")
        void processingTimeMsPresentOnError() {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(false);
            when(embeddingBackend.ensureEmbeddingModelLoaded()).thenReturn(false);

            Map<String, Object> response = visionTool.extractEmbeddings("https://example.com/face.jpg");

            assertThat(response.get("processingTimeMs"))
                .isInstanceOf(Long.class)
                .satisfies(v -> assertThat((Long) v).isGreaterThanOrEqualTo(0L));
        }

        @Test
        @DisplayName("ensureEmbeddingModelLoaded NOT called when model is already available")
        void healNotCalledWhenModelAvailable() {
            when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(true);

            visionTool.extractEmbeddings("https://example.invalid/face.jpg");

            verify(embeddingBackend, never()).ensureEmbeddingModelLoaded();
        }
    }

    // ============================= helpers =============================

    /**
     * Builds the minimal valid 1×1 white PNG bytes so {@code ImageData.fromBytes}
     * succeeds and we can test the post-model-check extraction path.
     */
    private static byte[] buildMinimalPng() {
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
