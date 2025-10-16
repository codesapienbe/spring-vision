package io.github.codesapienbe.springvision.core.djl;

import ai.djl.MalformedModelException;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DjlModelLoader.
 */
class DjlModelLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void testFaceDetectionCriteriaBuilder() {
        Criteria.Builder<Image, DetectedObjects> builder = DjlModelLoader.faceDetectionCriteria();

        assertNotNull(builder);

        // Build criteria and verify it's valid
        Criteria<Image, DetectedObjects> criteria = builder.build();
        assertNotNull(criteria);
    }

    @Test
    void testFaceRecognitionCriteriaBuilder() {
        Criteria.Builder<Image, float[]> builder = DjlModelLoader.faceRecognitionCriteria();

        assertNotNull(builder);

        // Build criteria and verify it's valid
        Criteria<Image, float[]> criteria = builder.build();
        assertNotNull(criteria);
    }

    @Test
    void testGetCacheDirectory() {
        Path cacheDir = DjlModelLoader.getCacheDirectory();

        assertNotNull(cacheDir);
        assertTrue(cacheDir.toString().contains(".djl.ai"));
    }

    @Test
    void testIsModelCachedReturnsFalseForNonExistentModel() {
        boolean cached = DjlModelLoader.isModelCached("non-existent-model-xyz");

        assertFalse(cached);
    }

    @Test
    void testLoadFromPathThrowsExceptionForNonExistentPath() {
        Path nonExistentPath = tempDir.resolve("non-existent");

        assertThrows(ModelNotFoundException.class, () -> {
            DjlModelLoader.loadFromPath(
                nonExistentPath.toString(),
                "model",
                Image.class,
                DetectedObjects.class
            );
        });
    }

    @Test
    void testLoadFromPathThrowsExceptionForEmptyDirectory() throws IOException {
        Path emptyDir = tempDir.resolve("empty");
        Files.createDirectories(emptyDir);

        assertThrows(Exception.class, () -> {
            DjlModelLoader.loadFromPath(
                emptyDir.toString(),
                "model",
                Image.class,
                DetectedObjects.class
            );
        });
    }

    @Test
    void testClearCacheDoesNotThrowException() {
        assertDoesNotThrow(() -> {
            DjlModelLoader.clearCache();
        });
    }

    @Test
    void testLoadFromUrlWithInvalidUrl() {
        String invalidUrl = "https://invalid-url-that-does-not-exist.com/model.zip";

        assertThrows(Exception.class, () -> {
            DjlModelLoader.loadFromUrl(
                invalidUrl,
                Image.class,
                DetectedObjects.class
            );
        });
    }

    @Test
    void testLoadFromModelZooWithInvalidArtifactId() {
        String invalidArtifact = "invalid:artifact:id:that:does:not:exist";

        assertThrows(ModelNotFoundException.class, () -> {
            DjlModelLoader.loadFromModelZoo(
                invalidArtifact,
                Image.class,
                DetectedObjects.class
            );
        });
    }
}

