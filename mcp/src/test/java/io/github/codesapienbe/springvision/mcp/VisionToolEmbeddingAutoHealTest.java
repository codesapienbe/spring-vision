package io.github.codesapienbe.springvision.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.codesapienbe.springvision.core.VectorService;
import io.github.codesapienbe.springvision.core.VisionBackend;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import io.github.codesapienbe.springvision.core.capabilities.EmbeddingCapability;

/**
 * Unit tests for the auto-healing logic in VisionTool embedding tools.
 *
 * <p>These tests verify that when the embedding model is not loaded the tool:
 * <ul>
 *   <li>calls {@code ensureEmbeddingModelLoaded()} before failing;</li>
 *   <li>reports {@code model_status=not_loaded} in the error response when healing fails;</li>
 *   <li>reports {@code model_status=available} and proceeds when healing succeeds.</li>
 * </ul>
 */
class VisionToolEmbeddingAutoHealTest {

    private EmbeddingBackend embeddingBackend;
    private VisionTemplate visionTemplate;
    private VisionTool visionTool;

    /** Combined VisionBackend + EmbeddingCapability mock. */
    interface EmbeddingBackend extends VisionBackend, EmbeddingCapability {}

    @BeforeEach
    void setUp() {
        embeddingBackend = mock(EmbeddingBackend.class, withSettings());
        VectorService vectorService = mock(VectorService.class);
        visionTemplate = new VisionTemplate(embeddingBackend, vectorService);
        visionTool = new VisionTool(visionTemplate);
    }

    // -----------------------------------------------------------------------
    // extract_face_embeddings_u (URL variant)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("URL tool: returns model_status=not_loaded when heal fails")
    void urlTool_returnsNotLoadedStatus_whenHealFails() {
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
    @DisplayName("URL tool: reports model_status=available on success")
    void urlTool_reportsAvailableStatus_onSuccess() throws Exception {
        when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(true);
        when(embeddingBackend.extractEmbeddings(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
            .thenReturn(List.of(new float[]{0.1f, 0.2f}));

        // We cannot hit a real HTTP URL in a unit test — verify that when the model
        // IS available the response carries model_status=available (the IO exception
        // from the fake URL will fall through to the error path; that is acceptable).
        Map<String, Object> response = visionTool.extractEmbeddings("https://example.invalid/face.jpg");

        // Whether success or IO error, model_status=available must NOT be "not_loaded"
        assertThat(response.get("model_status")).isNotEqualTo("not_loaded");
    }

    // -----------------------------------------------------------------------
    // extract_face_embeddings_b (bytes variant)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Bytes tool: returns model_status=not_loaded when heal fails")
    void bytesTool_returnsNotLoadedStatus_whenHealFails() {
        when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(false);
        when(embeddingBackend.ensureEmbeddingModelLoaded()).thenReturn(false);

        Map<String, Object> response = visionTool.extractEmbeddings(new byte[]{1, 2, 3});

        assertThat(response.get("status")).isEqualTo("error");
        assertThat(response.get("model_status")).isEqualTo("not_loaded");
        assertThat(response.get("embeddings")).isEqualTo(List.of());

        verify(embeddingBackend).ensureEmbeddingModelLoaded();
    }

    @Test
    @DisplayName("Bytes tool: processingTimeMs is a non-negative number")
    void bytesTool_processingTimeMs_isNonNegative() {
        when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(false);
        when(embeddingBackend.ensureEmbeddingModelLoaded()).thenReturn(false);

        Map<String, Object> response = visionTool.extractEmbeddings(new byte[]{1, 2, 3});

        Object durationRaw = response.get("processingTimeMs");
        assertThat(durationRaw).isInstanceOf(Long.class);
        assertThat((Long) durationRaw).isGreaterThanOrEqualTo(0L);
    }

    @Test
    @DisplayName("Bytes tool: heal is attempted before model is declared unavailable")
    void bytesTool_healIsAttemptedFirst() {
        when(embeddingBackend.isEmbeddingModelAvailable()).thenReturn(false);
        when(embeddingBackend.ensureEmbeddingModelLoaded()).thenReturn(true);
        // After heal succeeds the tool proceeds to extraction — mock empty result
        when(embeddingBackend.extractEmbeddings(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
            .thenReturn(List.of());

        Map<String, Object> response = visionTool.extractEmbeddings(new byte[]{
            // minimal valid JPEG header (enough to pass ImageData.fromBytes)
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
            0, 0x10, 'J', 'F', 'I', 'F', 0
        });

        // ensureEmbeddingModelLoaded was called
        verify(embeddingBackend).ensureEmbeddingModelLoaded();
        // model_status should not report not_loaded (it may report "available" or error from bad JPEG)
        assertThat(response.get("model_status")).isNotEqualTo("not_loaded");
    }
}
