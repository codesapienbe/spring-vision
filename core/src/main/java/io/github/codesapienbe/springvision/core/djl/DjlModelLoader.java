package io.github.codesapienbe.springvision.core.djl;

import ai.djl.Application;
import ai.djl.MalformedModelException;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * DJL-based model loader that replaces custom model loading with DJL's ModelZoo API.
 *
 * <p>This loader provides a unified way to load models from various sources:</p>
 * <ul>
 *   <li>DJL ModelZoo repositories (djl:// URLs)</li>
 *   <li>Local file system (file:// URLs)</li>
 *   <li>HTTP/HTTPS URLs</li>
 *   <li>S3 buckets (s3:// URLs with AWS extension)</li>
 *   <li>HDFS (hdfs:// URLs with Hadoop extension)</li>
 * </ul>
 *
 * <p>Benefits over custom loading:</p>
 * <ul>
 *   <li>Automatic model version management</li>
 *   <li>Built-in caching and download management</li>
 *   <li>Support for multiple model formats (ONNX, PyTorch, TensorFlow, etc.)</li>
 *   <li>Thread-safe model loading</li>
 *   <li>Progress tracking for downloads</li>
 *   <li>Extensible translator API for pre/post-processing</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 1.0.5
 */
public final class DjlModelLoader {

    private static final Logger logger = LoggerFactory.getLogger(DjlModelLoader.class);

    /**
     * Default cache directory for DJL models.
     */
    private static final Path DEFAULT_CACHE_DIR = Paths.get(
        System.getProperty("user.home", "."),
        ".djl.ai"
    );

    private DjlModelLoader() {
        // Utility class
    }

    /**
     * Loads a model using DJL Criteria builder for maximum flexibility.
     *
     * @param <I>      input type
     * @param <O>      output type
     * @param criteria the model loading criteria
     * @return loaded ZooModel
     * @throws ModelNotFoundException  if the model cannot be found
     * @throws MalformedModelException if a model format is invalid
     * @throws IOException             if I/O error occurs
     */
    public static <I, O> ZooModel<I, O> loadModel(Criteria<I, O> criteria)
        throws ModelNotFoundException, MalformedModelException, IOException {

        logger.info("Loading model with DJL ModelZoo API");

        try {
            ZooModel<I, O> model = criteria.loadModel();
            logger.info("Model loaded successfully: {}", model.getName());
            return model;
        } catch (ModelNotFoundException e) {
            logger.error("Model not found: {}", e.getMessage());
            throw e;
        } catch (MalformedModelException e) {
            logger.error("Malformed model: {}", e.getMessage());
            throw e;
        } catch (IOException e) {
            logger.error("I/O error loading model: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Creates a criteria builder for face detection models.
     *
     * @return criteria builder
     */
    public static Criteria.Builder<ai.djl.modality.cv.Image, ai.djl.modality.cv.output.DetectedObjects>
    faceDetectionCriteria() {

        return Criteria.builder()
            .setTypes(ai.djl.modality.cv.Image.class, ai.djl.modality.cv.output.DetectedObjects.class)
            .optApplication(Application.CV.OBJECT_DETECTION)
            .optProgress(new ProgressBar());
        // Engine is intentionally left unspecified here so callers can optEngine(...) based on configuration
    }

    /**
     * Creates a criteria builder for face recognition/embedding models.
     *
     * @return criteria builder
     */
    public static Criteria.Builder<ai.djl.modality.cv.Image, float[]>
    faceRecognitionCriteria() {

        return Criteria.builder()
            .setTypes(ai.djl.modality.cv.Image.class, float[].class)
            .optApplication(Application.CV.IMAGE_CLASSIFICATION)
            .optProgress(new ProgressBar());
        // Engine is intentionally left unspecified here so callers can optEngine(...) based on configuration
    }

    /**
     * Creates a criteria builder for general object detection models.
     *
     * @return criteria builder
     */
    public static Criteria.Builder<ai.djl.modality.cv.Image, ai.djl.modality.cv.output.DetectedObjects>
    objectDetectionCriteria() {

        return Criteria.builder()
            .setTypes(ai.djl.modality.cv.Image.class, ai.djl.modality.cv.output.DetectedObjects.class)
            .optApplication(Application.CV.OBJECT_DETECTION)
            .optProgress(new ProgressBar());
        // Engine is intentionally left unspecified here so callers can optEngine(...) based on configuration
    }

    /**
     * Loads a model from a local file path using DJL.
     *
     * @param <I>         input type
     * @param <O>         output type
     * @param modelPath   absolute path to the model directory or archive
     * @param modelName   model file prefix
     * @param inputClass  input data class
     * @param outputClass output data class
     * @return loaded ZooModel
     * @throws ModelNotFoundException  if model cannot be found
     * @throws MalformedModelException if model format is invalid
     * @throws IOException             if I/O error occurs
     */
    public static <I, O> ZooModel<I, O> loadFromPath(
        String modelPath,
        String modelName,
        Class<I> inputClass,
        Class<O> outputClass
    ) throws ModelNotFoundException, MalformedModelException, IOException {

        logger.info("Loading model from path: path={}, name={}", modelPath, modelName);

        Path path = Paths.get(modelPath);
        if (!Files.exists(path)) {
            throw new ModelNotFoundException("Model path does not exist: " + modelPath);
        }

        Criteria<I, O> criteria = Criteria.builder()
            .setTypes(inputClass, outputClass)
            .optModelPath(path)
            .optModelName(modelName)
            .optProgress(new ProgressBar())
            .build();

        return loadModel(criteria);
    }

    /**
     * Loads a model from a URL using DJL.
     *
     * @param <I>         input type
     * @param <O>         output type
     * @param modelUrl    URL to the model (http://, https://, s3://, etc.)
     * @param inputClass  input data class
     * @param outputClass output data class
     * @return loaded ZooModel
     * @throws ModelNotFoundException  if model cannot be found
     * @throws MalformedModelException if model format is invalid
     * @throws IOException             if I/O error occurs
     */
    public static <I, O> ZooModel<I, O> loadFromUrl(
        String modelUrl,
        Class<I> inputClass,
        Class<O> outputClass
    ) throws ModelNotFoundException, MalformedModelException, IOException {

        logger.info("Loading model from URL: {}", modelUrl);

        Criteria<I, O> criteria = Criteria.builder()
            .setTypes(inputClass, outputClass)
            .optModelUrls(modelUrl)
            .optProgress(new ProgressBar())
            .build();

        return loadModel(criteria);
    }

    /**
     * Loads a model from DJL ModelZoo using artifact ID.
     *
     * @param <I>         input type
     * @param <O>         output type
     * @param artifactId  the artifact ID (e.g., "ai.djl.pytorch:resnet")
     * @param inputClass  input data class
     * @param outputClass output data class
     * @return loaded ZooModel
     * @throws ModelNotFoundException  if model cannot be found
     * @throws MalformedModelException if model format is invalid
     * @throws IOException             if I/O error occurs
     */
    public static <I, O> ZooModel<I, O> loadFromModelZoo(
        String artifactId,
        Class<I> inputClass,
        Class<O> outputClass
    ) throws ModelNotFoundException, MalformedModelException, IOException {

        logger.info("Loading model from ModelZoo: {}", artifactId);

        Criteria<I, O> criteria = Criteria.builder()
            .setTypes(inputClass, outputClass)
            .optArtifactId(artifactId)
            .optProgress(new ProgressBar())
            .build();

        return loadModel(criteria);
    }

    /**
     * Gets the DJL cache directory.
     *
     * @return cache directory path
     */
    public static Path getCacheDirectory() {
        return DEFAULT_CACHE_DIR;
    }

    /**
     * Checks if a model exists in the cache.
     *
     * @param modelName model name to check
     * @return true if model exists in cache
     */
    public static boolean isModelCached(String modelName) {
        Path cachePath = DEFAULT_CACHE_DIR.resolve(modelName);
        return Files.exists(cachePath);
    }

    /**
     * Clears the DJL model cache.
     *
     * @throws IOException if cache cannot be cleared
     */
    public static void clearCache() throws IOException {
        if (Files.exists(DEFAULT_CACHE_DIR)) {
            logger.info("Clearing DJL model cache: {}", DEFAULT_CACHE_DIR);
            try (var stream = Files.walk(DEFAULT_CACHE_DIR)) {
                stream
                    .sorted((a, b) -> -a.compareTo(b)) // Reverse order to delete files before directories
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            logger.warn("Failed to delete cache file: {}", path, e);
                        }
                    });
            }
        }
    }
}
