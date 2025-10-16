package io.github.codesapienbe.springvision.core.djl;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import io.github.codesapienbe.springvision.core.*;
import io.github.codesapienbe.springvision.core.capabilities.FaceDetectionCapability;
import io.github.codesapienbe.springvision.core.capabilities.ObjectDetectionCapability;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;
import io.github.codesapienbe.springvision.core.exception.VisionBackendException;
import io.github.codesapienbe.springvision.core.exception.VisionProcessingException;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

/**
 * DJL-based vision backend for face detection and recognition.
 *
 * <p>This backend uses Deep Java Library (DJL) to provide:</p>
 * <ul>
 *   <li>Unified model loading from multiple sources</li>
 *   <li>Support for PyTorch, ONNX, TensorFlow models</li>
 *   <li>Built-in model versioning and caching</li>
 *   <li>Thread-safe inference</li>
 *   <li>Automatic GPU acceleration when available</li>
 * </ul>
 *
 * <p>This backend can be used as a replacement for custom model loading
 * or alongside existing backends like OpenCV.</p>
 *
 * @author Spring Vision Team
 * @since 1.0.5
 */
@Component
@ConditionalOnProperty(prefix = "spring.vision.djl", name = "enabled", havingValue = "true")
public class DjlVisionBackend implements VisionBackend, FaceDetectionCapability, ObjectDetectionCapability {

    private static final Logger logger = LoggerFactory.getLogger(DjlVisionBackend.class);

    public static final String BACKEND_ID = "djl";
    public static final String DISPLAY_NAME = "DJL Vision Backend";
    public static final String VERSION = "1.0.0-DJL";

    private ZooModel<Image, DetectedObjects> faceDetectionModel;
    private ZooModel<Image, float[]> faceRecognitionModel;
    private ZooModel<Image, DetectedObjects> objectDetectionModel;

    private final DjlProperties properties;

    // Lifecycle and health tracking
    private boolean initialized = false;
    private BackendHealthInfo.HealthStatus healthStatus = BackendHealthInfo.HealthStatus.UNKNOWN;
    private String healthErrorMessage = "Backend not initialized";
    private long lastHealthCheckTime;

    // Constructors
    public DjlVisionBackend() {
        this.properties = new DjlProperties();
    }

    public DjlVisionBackend(DjlProperties properties) {
        this.properties = properties != null ? properties : new DjlProperties();
    }

    @Override
    public String getBackendId() {
        return BACKEND_ID;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public Set<DetectionType> getSupportedDetectionTypes() {
        return Set.of(DetectionType.FACE, DetectionType.OBJECT);
    }

    @Override
    public boolean isHealthy() {
        return healthStatus == BackendHealthInfo.HealthStatus.HEALTHY && initialized;
    }

    @Override
    public BackendHealthInfo getHealthInfo() {
        long responseTime = System.currentTimeMillis() - lastHealthCheckTime;

        if (healthStatus == BackendHealthInfo.HealthStatus.HEALTHY) {
            return BackendHealthInfo.healthy(getBackendId(),
                "DJL backend is operational", responseTime);
        } else {
            return BackendHealthInfo.unhealthy(getBackendId(),
                "DJL backend is not operational", healthErrorMessage, responseTime);
        }
    }

    @Override
    public void initialize() throws BaseVisionException {
        logger.info("Initializing DJL vision backend");

        try {
            // Load models configured for DJL
            loadFaceDetectionModel();
            loadObjectDetectionModel();

            initialized = true;
            healthStatus = BackendHealthInfo.HealthStatus.HEALTHY;
            healthErrorMessage = null;
            lastHealthCheckTime = System.currentTimeMillis();

            logger.info("DJL vision backend initialized successfully");

        } catch (Exception e) {
            logger.error("Failed to initialize DJL backend", e);
            initialized = false;
            healthStatus = BackendHealthInfo.HealthStatus.UNHEALTHY;
            healthErrorMessage = "Initialization failed: " + e.getMessage();
            throw new VisionBackendException(
                "Failed to initialize DJL backend",
                "initialization_failed",
                null,
                e
            );
        }
    }

    private void loadFaceDetectionModel() throws ModelNotFoundException, MalformedModelException, IOException {
        logger.info("Loading face detection model with DJL");

        String engine = properties.getEngine();

        // If a specific model is configured, try to load it
        String configured = properties.getFaceDetectionModel();
        if (configured != null && !configured.isBlank()) {
            Criteria.Builder<Image, DetectedObjects> builder = DjlModelLoader.faceDetectionCriteria();
            if (engine != null && !engine.isBlank()) builder.optEngine(engine);

            if (configured.startsWith("http://") || configured.startsWith("https://") || configured.startsWith("s3://")) {
                builder.optModelUrls(configured);
            } else if (configured.contains(":")) { // likely artifact id
                builder.optArtifactId(configured);
            } else {
                java.nio.file.Path p = java.nio.file.Paths.get(configured);
                if (java.nio.file.Files.exists(p)) {
                    builder.optModelPath(p);
                } else {
                    logger.warn("Configured faceDetectionModel not found, falling back to ModelZoo: {}", configured);
                }
            }

            faceDetectionModel = DjlModelLoader.loadModel(builder.build());
            logger.info("Face detection model loaded: {}", faceDetectionModel.getName());
            return;
        }

        // Fallback to ModelZoo default
        Criteria<Image, DetectedObjects> criteria = DjlModelLoader.faceDetectionCriteria()
            .optFilter("backbone", "resnet50")
            .optEngine(engine != null && !engine.isBlank() ? engine : "PyTorch")
            .build();

        faceDetectionModel = DjlModelLoader.loadModel(criteria);
        logger.info("Face detection model loaded from ModelZoo");
    }

    private void loadObjectDetectionModel() throws ModelNotFoundException, MalformedModelException, IOException {
        logger.info("Loading object detection model with DJL");

        String engine = properties.getEngine();
        String configured = properties.getObjectDetectionModel();

        Criteria.Builder<Image, DetectedObjects> builder = DjlModelLoader.objectDetectionCriteria();
        if (engine != null && !engine.isBlank()) builder.optEngine(engine);

        if (configured != null && !configured.isBlank()) {
            if (configured.startsWith("http://") || configured.startsWith("https://") || configured.startsWith("s3://")) {
                builder.optModelUrls(configured);
            } else if (configured.contains(":")) { // artifact id
                builder.optArtifactId(configured);
            } else {
                java.nio.file.Path p = java.nio.file.Paths.get(configured);
                if (java.nio.file.Files.exists(p)) {
                    builder.optModelPath(p);
                } else {
                    logger.warn("Configured objectDetectionModel not found, using ModelZoo default: {}", configured);
                }
            }
        } else {
            // provide a reasonable default filter hint; DJL will choose a suitable model
            builder.optFilter("backbone", "resnet50");
        }

        objectDetectionModel = DjlModelLoader.loadModel(builder.build());
        logger.info("Object detection model loaded: {}", objectDetectionModel.getName());
    }

    @Override
    public List<Detection> detectFaces(ImageData imageData) throws BaseVisionException {
        DetectionQuery query = new DetectionQuery.Builder()
            .type(DetectionType.FACE)
            .build();
        return detectFaces(imageData, query);
    }

    /**
     * Detects faces with query parameters for filtering.
     */
    public List<Detection> detectFaces(ImageData imageData, DetectionQuery query) throws BaseVisionException {
        if (!initialized) {
            throw new VisionBackendException(
                "Backend not initialized",
                "not_initialized",
                null
            );
        }

        validateInput(imageData, query);

        String correlationId = generateCorrelationId();

        try {
            logger.debug("DJL face detection requested: correlationId={}, backend=djl", correlationId);

            // Convert ImageData to DJL Image
            Image djlImage = ImageFactory.getInstance()
                .fromInputStream(new ByteArrayInputStream(imageData.data()));

            // Run inference
            try (Predictor<Image, DetectedObjects> predictor = faceDetectionModel.newPredictor()) {
                DetectedObjects detections = predictor.predict(djlImage);

                // Convert DJL detections to Spring Vision detections
                List<Detection> results = convertDetections(detections, query);

                logger.info("DJL face detection completed: {} faces detected, correlationId={}, backend=djl",
                    results.size(), correlationId);

                return results;
            }

        } catch (IOException e) {
            throw new VisionProcessingException(
                "Failed to load image",
                "image_load_failed",
                DetectionType.FACE.name(),
                e
            );
        } catch (TranslateException e) {
            throw new VisionProcessingException(
                "Failed to process image",
                "inference_failed",
                DetectionType.FACE.name(),
                e
            );
        }
    }

    /**
     * Detects objects with query parameters for filtering.
     */
    public List<Detection> detectObjects(ImageData imageData, DetectionQuery query) throws BaseVisionException {
        if (!initialized) {
            throw new VisionBackendException(
                "Backend not initialized",
                "not_initialized",
                null
            );
        }

        validateInput(imageData, query);

        String correlationId = generateCorrelationId();
        try {
            logger.debug("DJL object detection requested: correlationId={}, backend=djl", correlationId);

            Image djlImage = ImageFactory.getInstance()
                .fromInputStream(new ByteArrayInputStream(imageData.data()));

            try (Predictor<Image, DetectedObjects> predictor = objectDetectionModel.newPredictor()) {
                DetectedObjects detections = predictor.predict(djlImage);
                List<Detection> results = convertDetections(detections, query);

                logger.info("DJL object detection completed: {} objects detected, correlationId={}, backend=djl",
                    results.size(), correlationId);

                return results;
            }
        } catch (IOException e) {
            throw new VisionProcessingException(
                "Failed to load image",
                "image_load_failed",
                DetectionType.OBJECT.name(),
                e
            );
        } catch (TranslateException e) {
            throw new VisionProcessingException(
                "Failed to process image",
                "inference_failed",
                DetectionType.OBJECT.name(),
                e
            );
        }
    }

    @Override
    public List<Detection> detectObjects(ImageData imageData) {
        DetectionQuery query = new DetectionQuery.Builder()
            .type(DetectionType.OBJECT)
            .minConfidence(properties.getConfidenceThreshold())
            .build();
        try {
            return detectObjects(imageData, query);
        } catch (BaseVisionException e) {
            throw new io.github.codesapienbe.springvision.core.exception.VisionBackendException(
                "Object detection failed",
                "detect_objects",
                null,
                e
            );
        }
    }

    /**
     * Converts DJL DetectedObjects to Spring Vision Detection list with filtering.
     */
    private List<Detection> convertDetections(DetectedObjects detections, DetectionQuery query) {
        List<Detection> results = new ArrayList<>();

        // Iterate over detected objects
        List<ai.djl.modality.cv.output.DetectedObjects.DetectedObject> items = detections.items();
        for (ai.djl.modality.cv.output.DetectedObjects.DetectedObject obj : items) {
            // Apply confidence threshold filtering
            if (obj.getProbability() < query.getMinConfidence()) {
                continue;
            }

            ai.djl.modality.cv.output.BoundingBox bbox = obj.getBoundingBox();
            ai.djl.modality.cv.output.Rectangle rect = bbox.getBounds();

            // Convert to Spring Vision BoundingBox using normalized coordinates (0..1)
            io.github.codesapienbe.springvision.core.BoundingBox svBbox =
                new io.github.codesapienbe.springvision.core.BoundingBox(
                    rect.getX(),
                    rect.getY(),
                    rect.getWidth(),
                    rect.getHeight()
                );

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("backend", BACKEND_ID);
            String modelName = switch (query.getType()) {
                case OBJECT -> (objectDetectionModel != null ? objectDetectionModel.getName() : "unknown");
                case FACE -> (faceDetectionModel != null ? faceDetectionModel.getName() : "unknown");
                default -> "unknown";
            };
            attributes.put("model", modelName);

            Detection detection = new Detection(
                obj.getClassName(),
                obj.getProbability(),
                svBbox,
                attributes
            );

            results.add(detection);

            // Apply max detections limit
            if (query.getMaxDetections() > 0 && results.size() >= query.getMaxDetections()) {
                break;
            }
        }

        logger.debug("Converted {} detections with filtering (minConfidence={}, maxDetections={})",
            results.size(), query.getMinConfidence(), query.getMaxDetections());
        return results;
    }

    /**
     * Validates input parameters.
     */
    private void validateInput(ImageData imageData, DetectionQuery query) {
        Objects.requireNonNull(imageData, "ImageData cannot be null");
        Objects.requireNonNull(query, "DetectionQuery cannot be null");

        if (imageData.data() == null || imageData.data().length == 0) {
            throw new IllegalArgumentException("Image data cannot be empty");
        }

        if (imageData.data().length > 50 * 1024 * 1024) { // 50MB limit
            throw new IllegalArgumentException("Image size exceeds maximum limit of 50MB");
        }

        if (!getSupportedDetectionTypes().contains(query.getType())) {
            throw new IllegalArgumentException("Unsupported detection type: " + query.getType());
        }
    }

    /**
     * Generates a unique correlation ID for tracking requests.
     */
    private String generateCorrelationId() {
        return "djl-" + System.currentTimeMillis() + "-" + Thread.currentThread().getId();
    }

    @Override
    @PreDestroy
    public void shutdown() throws BaseVisionException {
        if (!initialized) {
            return;
        }

        logger.info("Shutting down DJL backend");

        try {
            if (faceDetectionModel != null) {
                faceDetectionModel.close();
                faceDetectionModel = null;
            }

            if (faceRecognitionModel != null) {
                faceRecognitionModel.close();
                faceRecognitionModel = null;
            }

            if (objectDetectionModel != null) {
                objectDetectionModel.close();
                objectDetectionModel = null;
            }

            initialized = false;
            healthStatus = BackendHealthInfo.HealthStatus.UNKNOWN;

            logger.info("DJL backend shut down successfully");

        } catch (Exception e) {
            logger.error("Error during DJL backend shutdown", e);
            throw new VisionBackendException(
                "Failed to shutdown DJL backend",
                "shutdown_failed",
                null,
                e
            );
        }
    }
}
