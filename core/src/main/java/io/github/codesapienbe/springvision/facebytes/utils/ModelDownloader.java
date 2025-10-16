package io.github.codesapienbe.springvision.facebytes.utils;

import io.github.codesapienbe.springvision.facebytes.exceptions.DeepFaceException;
import io.github.codesapienbe.springvision.facebytes.enums.ModelType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/**
 * Utility class for managing ONNX models for FaceBytes.
 * Models are loaded from classpath (bundled in JAR during Maven build).
 * NO runtime downloads - all models must be present in classpath.
 *
 * @author FaceBytes Team
 * @since 1.0.0
 * @deprecated Use direct classpath loading via ModelManager instead
 */
@Deprecated(forRemoval = true)
public final class ModelDownloader {

    private ModelDownloader() {
    }

    /**
     * Extracts a model from classpath to a temporary file.
     * Models are bundled in JAR at build time via maven-download-plugin.
     *
     * @param modelType the type of model to load
     * @return the path to the extracted model file
     * @throws DeepFaceException if the model cannot be found in classpath
     */
    public static String ensureModelAvailable(ModelType modelType) throws DeepFaceException {
        String classpathLocation = getClasspathLocation(modelType);

        try (InputStream is = ModelDownloader.class.getResourceAsStream(classpathLocation)) {
            if (is == null) {
                Logs.error("ModelDownloader", "model.not_found_in_classpath", null,
                    Map.of("model", modelType.name(),
                        "classpath", classpathLocation,
                        "action", "Run: mvn clean install to download models"));
                throw new DeepFaceException("Model not found in classpath: " + modelType +
                    ". Run 'mvn clean install' to download models during build.");
            }

            // Extract to a temp file (ONNX Runtime requires a file path)
            String fileName = getModelFileName(modelType);
            Path tempFile = Files.createTempFile("spring-vision-", "-" + fileName);
            Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
            tempFile.toFile().deleteOnExit();

            Logs.info("ModelDownloader", "model.extracted_from_classpath",
                Map.of("model", modelType.name(),
                    "classpath", classpathLocation,
                    "temp_path", tempFile.toAbsolutePath().toString()));

            return tempFile.toAbsolutePath().toString();

        } catch (IOException e) {
            Logs.error("ModelDownloader", "model.extraction_failed", e,
                Map.of("model", modelType.name()));
            throw new DeepFaceException("Failed to extract model from classpath: " + modelType, e);
        }
    }

    /**
     * Gets the classpath location for a model type.
     *
     * @param modelType the type of model
     * @return the classpath resource path
     */
    private static String getClasspathLocation(ModelType modelType) {
        String fileName = getModelFileName(modelType);
        return "/models/facebytes/" + fileName;
    }

    /**
     * Gets the filename for a model type.
     *
     * @param modelType the type of model
     * @return the filename
     */
    private static String getModelFileName(ModelType modelType) {
        return switch (modelType) {
            case VGG_FACE -> "vggface.onnx";
            case ARCFACE -> "arcface.onnx";
            case FACENET, FACENET512 -> "facenet128.onnx";
            case OPEN_FACE -> "openface.onnx";
            case SFACE -> "sface.onnx";
            case DEEP_FACE -> "deepface.onnx";
            default -> modelType.name().toLowerCase() + ".onnx";
        };
    }

    /**
     * @deprecated No cleanup needed - temp files are deleted on JVM exit
     */
    @Deprecated(forRemoval = true)
    public static void cleanupModel(ModelType modelType) {
        // No-op: temp files are automatically cleaned up on JVM exit
        Logs.debug("ModelDownloader", "cleanup.noop",
            Map.of("model", modelType.name(), "reason", "temp files auto-deleted"));
    }

    /**
     * @deprecated Models are loaded from the classpath, no cache directory used
     */
    @Deprecated(forRemoval = true)
    public static String getCacheDirectory() {
        return "classpath:/models/facebytes/";
    }
}
