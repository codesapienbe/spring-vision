package io.github.codesapienbe.springvision.core.djl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import io.github.codesapienbe.springvision.core.TestImageUtils;

/**
 * Emotion detection with the real FER+ model. Runs only when the model is bundled
 * ({@code -Pdownload-models}).
 */
@EnabledIf("emotionModelAvailable")
class DjlEmotionDetectionIntegrationTest {

    private static DjlVisionBackend backend;

    static boolean emotionModelAvailable() {
        return ClasspathModels.isModelAvailable(ModelCatalog.EMOTION);
    }

    @BeforeAll
    static void startBackend() {
        DjlProperties properties = new DjlProperties();
        properties.getModels().setLazy(true);
        backend = new DjlVisionBackend(properties);
        backend.initialize();
    }

    @AfterAll
    static void stopBackend() {
        if (backend != null) {
            backend.shutdown();
        }
    }

    @Test
    @DisplayName("An image without a face yields no emotions, never a whole-frame guess")
    void noFaceNoEmotion() throws Exception {
        assertThat(backend.detectEmotions(TestImageUtils.createTextImage("NO FACE HERE", 640, 240))).isEmpty();
    }
}
