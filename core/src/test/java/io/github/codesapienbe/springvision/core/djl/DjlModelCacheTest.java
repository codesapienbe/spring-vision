package io.github.codesapienbe.springvision.core.djl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DJL model caching and loading mechanisms.
 */
class DjlModelCacheTest {

    @TempDir
    Path tempDir;

    @Test
    void testGetCacheDirectory() {
        Path cacheDir = DjlModelLoader.getCacheDirectory();

        assertNotNull(cacheDir);
        assertTrue(cacheDir.toString().contains(".djl.ai"));
    }

    @Test
    void testIsModelCachedWithNonExistentModel() {
        boolean cached = DjlModelLoader.isModelCached("non-existent-model-" + System.currentTimeMillis());

        assertFalse(cached);
    }

    @Test
    void testFaceDetectionCriteriaBuilder() {
        var builder = DjlModelLoader.faceDetectionCriteria();

        assertNotNull(builder);

        var criteria = builder.build();
        assertNotNull(criteria);
    }

    @Test
    void testFaceRecognitionCriteriaBuilder() {
        var builder = DjlModelLoader.faceRecognitionCriteria();

        assertNotNull(builder);

        var criteria = builder.build();
        assertNotNull(criteria);
    }

    @Test
    void testObjectDetectionCriteriaBuilder() {
        var builder = DjlModelLoader.objectDetectionCriteria();

        assertNotNull(builder);

        var criteria = builder.build();
        assertNotNull(criteria);
    }

    @Test
    void testClearCacheDoesNotThrow() {
        assertDoesNotThrow(() -> {
            DjlModelLoader.clearCache();
        });
    }

    @Test
    void testMultipleCriteriaBuilders() {
        // Ensure creating multiple criteria builders doesn't cause issues
        for (int i = 0; i < 10; i++) {
            assertDoesNotThrow(() -> {
                DjlModelLoader.faceDetectionCriteria();
                DjlModelLoader.faceRecognitionCriteria();
                DjlModelLoader.objectDetectionCriteria();
            });
        }
    }

    @Test
    void testCriteriaBuilderCustomization() {
        var builder = DjlModelLoader.faceDetectionCriteria();

        // Test that builder can be customized
        assertDoesNotThrow(() -> {
            builder
                .optEngine("PyTorch")
                .build();
        });
    }

    @Test
    void testCacheDirectoryExists() {
        Path cacheDir = DjlModelLoader.getCacheDirectory();

        // The directory should be a valid path
        assertNotNull(cacheDir);
        assertNotNull(cacheDir.getFileName());
    }
}

