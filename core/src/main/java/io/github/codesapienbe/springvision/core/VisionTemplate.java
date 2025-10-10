package io.github.codesapienbe.springvision.core;

import io.github.codesapienbe.springvision.core.backend.OpenCvVisionBackend;
import io.github.codesapienbe.springvision.core.capabilities.*;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;
import io.github.codesapienbe.springvision.core.exception.VisionProcessingException;
import io.github.codesapienbe.springvision.core.exception.VisionUnsupportedException;
import io.github.codesapienbe.springvision.core.util.EmbeddingSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Template for computer vision operations providing a unified interface
 * across different vision backends (OpenCV, MediaPipe, YOLO, etc.).
 *
 * <p>This template follows the Spring template pattern, providing a
 * consistent API regardless of the underlying vision backend implementation.
 * It handles common concerns like error handling, metrics collection, and
 * result transformation.</p>
 *
 * <p>The template is designed to be used as a Spring bean and provides
 * both synchronous and asynchronous methods for vision operations. It
 * includes comprehensive logging, error handling, and performance monitoring.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @Autowired
 * private VisionTemplate visionTemplate;
 *
 * public void detectFaces(byte[] imageData) {
 *     ImageData data = ImageData.fromBytes(imageData);
 *     VisionResult result = visionTemplate.detectFaces(data);
 *     // Process VisionResult: bounding boxes, confidence, etc.
 * }
 * }</pre>
 *
 * @author Spring Vision Team
 * @see VisionBackend
 * @see VisionResult
 * @see ImageData
 * @see DetectionType
 * @since 1.0.0
 */
public record VisionTemplate(VisionBackend backend, VectorService vectorService) {

    private static final Logger logger = LoggerFactory.getLogger(VisionTemplate.class);

    /**
     * Constructs a new VisionTemplate with the specified backend.
     *
     * @param backend the vision backend to use
     */
    public VisionTemplate(VisionBackend backend) {
        this(backend, null);
    }

    /**
     * No-arg constructor used by Spring tests and frameworks that require a default
     * constructor. This wires a reasonable default backend (OpenCV) for test/demo
     * purposes only. Production users should explicitly construct VisionTemplate
     * with their preferred `VisionBackend` implementation.
     */
    public VisionTemplate() {
        this(new OpenCvVisionBackend());
    }

    /**
     * New constructor that accepts an optional VectorService implementation.
     *
     * @param backend       the vision backend to use
     * @param vectorService the vector service to use
     */
    public VisionTemplate(VisionBackend backend, VectorService vectorService) {
        this.backend = Objects.requireNonNull(backend, "Vision backend must not be null");
        this.vectorService = vectorService;
        logger.info("Initialized VisionTemplate with backend: {}", backend.getBackendId());
    }

    /**
     * Gets the underlying vision backend.
     *
     * @return the vision backend
     */
    @Override
    public VisionBackend backend() {
        return backend;
    }

    /**
     * Gets the backend identifier.
     *
     * @return the backend identifier
     */
    public String getBackendId() {
        return backend.getBackendId();
    }

    /**
     * Gets the backend display name.
     *
     * @return the backend display name
     */
    public String getBackendDisplayName() {
        return backend.getDisplayName();
    }

    /**
     * Gets the backend version.
     *
     * @return the backend version
     */
    public String getBackendVersion() {
        return backend.getVersion();
    }

    /**
     * Gets the set of detection types supported by this backend.
     *
     * @return the set of supported detection types
     */
    public Set<DetectionType> getSupportedDetectionTypes() {
        return backend.getSupportedDetectionTypes();
    }

    /**
     * Checks if this backend supports a specific detection type.
     *
     * @param detectionType the detection type to check
     * @return true if supported, false otherwise
     */
    public boolean supportsDetectionType(DetectionType detectionType) {
        return backend.supportsDetectionType(detectionType);
    }

    /**
     * Checks if the backend is healthy.
     *
     * @return true if healthy, false otherwise
     */
    public boolean isBackendHealthy() {
        return backend.isHealthy();
    }

    /**
     * Gets detailed health information about the backend.
     *
     * @return the backend health info
     */
    public BackendHealthInfo getBackendHealthInfo() {
        return backend.getHealthInfo();
    }

    /**
     * Detects faces in the provided image data.
     *
     * @param imageData the image data to process
     * @return the vision result
     * @throws BaseVisionException if detection fails
     */
    public VisionResult detectFaces(ImageData imageData) throws BaseVisionException {
        return detect(imageData, DetectionType.FACE);
    }

    /**
     * Store an embedding using the configured VectorService (if available).
     *
     * @param personId   the person ID
     * @param embedding  the embedding vector
     * @param modelName  the model name
     * @param imageHash  the image hash
     * @param confidence the detection confidence
     * @param metadata   additional metadata
     * @return the stored embedding ID
     */
    public String storeFaceEmbedding(String personId, float[] embedding, String modelName, String imageHash, Double confidence, Map<String, Object> metadata) {
        if (vectorService == null) throw new UnsupportedOperationException("No VectorService configured");
        return vectorService.storeFaceEmbedding(personId, embedding, modelName, imageHash, confidence, metadata);
    }

    /**
     * Lookup similar faces using the configured VectorService (if available).
     *
     * @param queryEmbedding   the query embedding
     * @param modelName        the model name
     * @param metric           the distance metric
     * @param threshold        the similarity threshold
     * @param limit            the maximum number of results
     * @param includePersonIds person IDs to include
     * @param excludePersonIds person IDs to exclude
     * @return a list of similar faces
     */
    public List<Map<String, Object>> lookupFaces(float[] queryEmbedding, String modelName, String metric, Double threshold, Integer limit, Set<String> includePersonIds, Set<String> excludePersonIds) {
        if (vectorService == null) throw new UnsupportedOperationException("No VectorService configured");
        return vectorService.findSimilarFaces(queryEmbedding, modelName, metric, threshold, limit, includePersonIds, excludePersonIds);
    }

    /**
     * Detects faces in the provided byte array.
     *
     * @param imageBytes the image data to process
     * @return the vision result
     * @throws BaseVisionException if detection fails
     */
    public VisionResult detectFaces(byte[] imageBytes) throws BaseVisionException {
        ImageData imageData = ImageData.fromBytes(imageBytes);
        return detectFaces(imageData);
    }

    /**
     * Detects objects in the provided image data.
     *
     * @param imageData the image data to process
     * @return the vision result
     * @throws BaseVisionException if detection fails
     */
    public VisionResult detectObjects(ImageData imageData) throws BaseVisionException {
        return detect(imageData, DetectionType.OBJECT);
    }

    /**
     * Detects objects in the provided byte array.
     *
     * @param imageBytes the image data to process
     * @return the vision result
     * @throws BaseVisionException if detection fails
     */
    public VisionResult detectObjects(byte[] imageBytes) throws BaseVisionException {
        ImageData imageData = ImageData.fromBytes(imageBytes);
        return detectObjects(imageData);
    }

    /**
     * Performs a generic detection operation based on the specified detection type.
     *
     * @param imageData     the image data to process
     * @param detectionType the detection type to perform
     * @return the vision result
     * @throws BaseVisionException if detection fails
     */
    public VisionResult detect(ImageData imageData, DetectionType detectionType) throws BaseVisionException {
        if (imageData == null) {
            throw new IllegalArgumentException("Image data must not be null");
        }
        if (detectionType == null) {
            throw new IllegalArgumentException("Detection type must not be null");
        }

        if (!supportsDetectionType(detectionType)) {
            throw new IllegalArgumentException(
                String.format("Detection type '%s' is not supported by backend '%s'",
                    detectionType.getDisplayName(), getBackendId()));
        }

        String correlationId = generateCorrelationId();
        long startTime = System.currentTimeMillis();

        logger.info("Starting {} detection", Map.of(
            "correlationId", correlationId,
            "detectionType", detectionType.getDisplayName(),
            "imageSize", imageData.getSizeInBytes(),
            "imageFormat", imageData.format(),
            "backendId", getBackendId()
        ));

        try {
            VisionResult result = routeViaCapabilitiesIfAvailable(imageData, detectionType);
            long processingTime = System.currentTimeMillis() - startTime;

            logger.info("{} detection completed", Map.of(
                "correlationId", correlationId,
                "detectionType", detectionType.getDisplayName(),
                "detectionsFound", result.detectionCount(),
                "averageConfidence", result.averageConfidence(),
                "processingTimeMs", processingTime,
                "backendId", getBackendId()
            ));

            return result;

        } catch (BaseVisionException e) {
            long processingTime = System.currentTimeMillis() - startTime;

            logger.error("{} detection failed", Map.of(
                "correlationId", correlationId,
                "detectionType", detectionType.getDisplayName(),
                "processingTimeMs", processingTime,
                "backendId", getBackendId(),
                "error", e.getClass().getSimpleName()
            ), e);

            throw e;

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;

            logger.error("Unexpected error during {} detection", Map.of(
                "correlationId", correlationId,
                "detectionType", detectionType.getDisplayName(),
                "processingTimeMs", processingTime,
                "backendId", getBackendId(),
                "error", e.getClass().getSimpleName()
            ), e);

            throw new VisionProcessingException(
                String.format("Unexpected error during %s detection", detectionType.getDisplayName()),
                detectionType.getDisplayName().toLowerCase(),
                detectionType.getCode(),
                e
            );
        }
    }

    /**
     * Performs a generic detection operation on byte array data.
     *
     * @param imageBytes    the image data to process
     * @param detectionType the detection type to perform
     * @return the vision result
     * @throws BaseVisionException if detection fails
     */
    public VisionResult detect(byte[] imageBytes, DetectionType detectionType) throws BaseVisionException {
        ImageData imageData = ImageData.fromBytes(imageBytes);
        return detect(imageData, detectionType);
    }

    // Capability-aware routing (optional, non-breaking)
    private VisionResult routeViaCapabilitiesIfAvailable(ImageData imageData, DetectionType detectionType) throws BaseVisionException {
        Object b = this.backend;
        List<Detection> detections = null;

        switch (detectionType) {
            case FACE -> {
                if (b instanceof FaceDetectionCapability cap) {
                    detections = cap.detectFaces(imageData);
                } else {
                    throw new VisionUnsupportedException(
                        String.format("Face detection is not supported by backend '%s'", getBackendId()),
                        "detectFaces", null);
                }
            }
            case OBJECT -> {
                if (b instanceof ObjectDetectionCapability cap) {
                    detections = cap.detectObjects(imageData);
                } else {
                    throw new VisionUnsupportedException(
                        String.format("Object detection is not supported by backend '%s'", getBackendId()),
                        "detectObjects", null);
                }
            }
            case TEXT -> {
                if (b instanceof TextOcrCapability cap) {
                    detections = cap.detectText(imageData);
                } else {
                    throw new VisionUnsupportedException(
                        String.format("Text detection is not supported by backend '%s'", getBackendId()),
                        "detectText", null);
                }
            }
            case BARCODE -> {
                if (b instanceof BarcodeCapability cap) {
                    detections = cap.detectBarcodes(imageData);
                } else {
                    throw new VisionUnsupportedException(
                        String.format("Barcode detection is not supported by backend '%s'", getBackendId()),
                        "detectBarcodes", null);
                }
            }
            case LANDMARK -> {
                if (b instanceof LandmarkDetectionCapability cap) {
                    detections = cap.detectLandmarks(imageData);
                } else {
                    throw new VisionUnsupportedException(
                        String.format("Landmark detection is not supported by backend '%s'", getBackendId()),
                        "detectLandmarks", null);
                }
            }
            case POSE -> {
                if (b instanceof PoseEstimationCapability cap) {
                    detections = cap.detectPoses(imageData);
                } else {
                    throw new VisionUnsupportedException(
                        String.format("Pose detection is not supported by backend '%s'", getBackendId()),
                        "detectPoses", null);
                }
            }
            case HAND -> {
                if (b instanceof HandDetectionCapability cap) {
                    detections = cap.detectHands(imageData);
                } else {
                    throw new VisionUnsupportedException(
                        String.format("Hand detection is not supported by backend '%s'", getBackendId()),
                        "detectHands", null);
                }
            }
            case CUSTOM -> {
                throw new VisionUnsupportedException(
                    String.format("Custom detection is not supported by backend '%s'", getBackendId()),
                    "detectCustom", null);
            }
            default -> {
                throw new VisionUnsupportedException(
                    "Unsupported detection type: " + detectionType, "detect", detectionType.name());
            }
        }

        return VisionResult.of(detectionType, detections,
            detections.isEmpty() ? 0.0 : detections.stream().mapToDouble(Detection::confidence).average().orElse(0.0),
            0);
    }

    /**
     * Performs multiple detection types on the provided image data.
     *
     * @param imageData      the image data to process
     * @param detectionTypes the list of detection types to perform
     * @return a list of vision results
     * @throws BaseVisionException if detection fails
     */
    public List<VisionResult> detectMultiple(ImageData imageData, List<DetectionType> detectionTypes)
        throws BaseVisionException {
        if (imageData == null) {
            throw new IllegalArgumentException("Image data must not be null");
        }
        if (detectionTypes == null || detectionTypes.isEmpty()) {
            throw new IllegalArgumentException("Detection types must not be null or empty");
        }

        String correlationId = generateCorrelationId();
        long startTime = System.currentTimeMillis();

        logger.info("Starting multi-detection", Map.of(
            "correlationId", correlationId,
            "detectionTypes", detectionTypes.stream().map(DetectionType::getDisplayName).toList(),
            "imageSize", imageData.getSizeInBytes(),
            "imageFormat", imageData.format(),
            "backendId", getBackendId()
        ));

        try {
            // Route each detection type individually through capabilities
            List<VisionResult> results = new ArrayList<>();
            for (DetectionType detectionType : detectionTypes) {
                VisionResult result = routeViaCapabilitiesIfAvailable(imageData, detectionType);
                results.add(result);
            }

            long processingTime = System.currentTimeMillis() - startTime;
            int totalDetections = results.stream().mapToInt(VisionResult::detectionCount).sum();

            logger.info("Multi-detection completed", Map.of(
                "correlationId", correlationId,
                "detectionTypes", detectionTypes.stream().map(DetectionType::getDisplayName).toList(),
                "totalDetections", totalDetections,
                "processingTimeMs", processingTime,
                "backendId", getBackendId()
            ));
            return results;
        } catch (BaseVisionException e) {
            long processingTime = System.currentTimeMillis() - startTime;
            logger.error("Multi-detection failed", Map.of(
                "correlationId", correlationId,
                "detectionTypes", detectionTypes.stream().map(DetectionType::getDisplayName).toList(),
                "processingTimeMs", processingTime,
                "backendId", getBackendId(),
                "error", e.getClass().getSimpleName()
            ), e);
            throw e;
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            logger.error("Unexpected error during multi-detection", Map.of(
                "correlationId", correlationId,
                "detectionTypes", detectionTypes.stream().map(DetectionType::getDisplayName).toList(),
                "processingTimeMs", processingTime,
                "backendId", getBackendId(),
                "error", e.getClass().getSimpleName()
            ), e);
            throw new VisionProcessingException(
                "Unexpected error during multi-detection",
                "multi-detect",
                "multi",
                e
            );
        }
    }

    /**
     * Performs a generic detection using a rich query.
     *
     * @param imageData the image data to process
     * @param query     the detection query
     * @return the vision result
     * @throws BaseVisionException if detection fails
     */
    public VisionResult detect(ImageData imageData, DetectionQuery query) throws BaseVisionException {
        if (imageData == null) {
            throw new IllegalArgumentException("Image data must not be null");
        }
        if (query == null) {
            throw new IllegalArgumentException("Detection query must not be null");
        }
        if (query.getType() == null) {
            throw new IllegalArgumentException("Detection query type must not be null");
        }

        if (!supportsDetectionType(query.getType())) {
            throw new IllegalArgumentException(
                String.format("Detection type '%s' is not supported by backend '%s'",
                    query.getType().getDisplayName(), getBackendId()));
        }

        String correlationId = generateCorrelationId();
        long startTime = System.currentTimeMillis();

        logger.info("Starting detection query", Map.of(
            "correlationId", correlationId,
            "detectionType", query.getType().getDisplayName(),
            "categories", query.getCategories().stream().map(Enum::name).toList(),
            "minConfidence", query.getMinConfidence(),
            "maxDetections", query.getMaxDetections(),
            "hasRoi", query.getRoi() != null,
            "imageSize", imageData.getSizeInBytes(),
            "imageFormat", imageData.format(),
            "backendId", getBackendId()
        ));

        try {
            // Route through capability-based detection
            VisionResult result = routeViaCapabilitiesIfAvailable(imageData, query.getType());
            long processingTime = System.currentTimeMillis() - startTime;

            logger.info("Detection query completed", Map.of(
                "correlationId", correlationId,
                "detectionType", query.getType().getDisplayName(),
                "detectionCount", result.detectionCount(),
                "processingTimeMs", processingTime,
                "backendId", getBackendId()
            ));
            return result;
        } catch (BaseVisionException e) {
            long processingTime = System.currentTimeMillis() - startTime;
            logger.error("Detection query failed", Map.of(
                "correlationId", correlationId,
                "detectionType", query.getType().getDisplayName(),
                "processingTimeMs", processingTime,
                "backendId", getBackendId(),
                "error", e.getClass().getSimpleName()
            ), e);
            throw e;
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            logger.error("Unexpected error during detection query", Map.of(
                "correlationId", correlationId,
                "detectionType", query.getType().getDisplayName(),
                "processingTimeMs", processingTime,
                "backendId", getBackendId(),
                "error", e.getClass().getSimpleName()
            ), e);
            throw new VisionProcessingException(
                String.format("Unexpected error during %s detection", query.getType().getDisplayName()),
                query.getType().getDisplayName().toLowerCase(),
                query.getType().getCode(),
                e
            );
        }
    }

    /**
     * Extracts face embeddings using the backend's implementation or default support.
     *
     * @param imageData the image data to process
     * @return a list of face embeddings
     * @throws BaseVisionException if embedding extraction fails
     */
    public List<float[]> extractEmbeddings(ImageData imageData) throws BaseVisionException {
        if (imageData == null) {
            throw new IllegalArgumentException("Image data must not be null");
        }
        String correlationId = generateCorrelationId();
        long startTime = System.currentTimeMillis();
        logger.info("Starting embedding extraction", Map.of(
            "correlationId", correlationId,
            "imageSize", imageData.getSizeInBytes(),
            "imageFormat", imageData.format(),
            "backendId", getBackendId()
        ));

        // Route through EmbeddingCapability if available
        List<float[]> embeddings;
        if (backend instanceof EmbeddingCapability cap) {
            embeddings = cap.extractEmbeddings(imageData, DetectionCategory.FACE);
        } else {
            // Fall back to default support (e.g., FaceBytes)
            embeddings = EmbeddingSupport.defaultExtractEmbeddings(imageData);
        }

        long processingTime = System.currentTimeMillis() - startTime;
        logger.info("Embedding extraction completed", Map.of(
            "correlationId", correlationId,
            "embeddingsCount", embeddings == null ? 0 : embeddings.size(),
            "processingTimeMs", processingTime,
            "backendId", getBackendId()
        ));
        return embeddings;
    }

    /**
     * Verifies whether two images belong to the same identity.
     *
     * @param a         the first image
     * @param b         the second image
     * @param metric    the distance metric
     * @param threshold the similarity threshold
     * @return true if the images are a match, false otherwise
     * @throws BaseVisionException if verification fails
     */
    public boolean verify(ImageData a, ImageData b, String metric, double threshold) throws BaseVisionException {
        if (a == null) {
            throw new IllegalArgumentException("First image must not be null");
        }
        if (b == null) {
            throw new IllegalArgumentException("Second image must not be null");
        }
        String correlationId = generateCorrelationId();
        long startTime = System.currentTimeMillis();
        logger.info("Starting verification", Map.of(
            "correlationId", correlationId,
            "metric", metric,
            "threshold", threshold,
            "backendId", getBackendId()
        ));

        // Route through FaceVerificationCapability if available
        boolean result;
        if (backend instanceof FaceVerificationCapability cap) {
            result = cap.verify(a, b, metric, threshold);
        } else {
            // Fall back to default support
            result = EmbeddingSupport.defaultVerify(a, b, metric, threshold);
        }

        long processingTime = System.currentTimeMillis() - startTime;
        logger.info("Verification completed", Map.of(
            "correlationId", correlationId,
            "isMatch", result,
            "processingTimeMs", processingTime,
            "backendId", getBackendId()
        ));
        return result;
    }

    /**
     * Obscures faces in the provided image data.
     *
     * @param imageData the image data to process
     * @return the modified image data
     * @throws BaseVisionException if obscuring fails
     */
    public ImageData obscureFaces(ImageData imageData) throws BaseVisionException {
        if (imageData == null) {
            throw new IllegalArgumentException("Image data must not be null");
        }
        String correlationId = generateCorrelationId();
        long startTime = System.currentTimeMillis();
        logger.info("Starting face obscuring", Map.of(
            "correlationId", correlationId,
            "imageSize", imageData.getSizeInBytes(),
            "imageFormat", imageData.format(),
            "backendId", getBackendId()
        ));
        if (backend instanceof AnnotationCapability cap) {
            AnnotationRequest req = new AnnotationRequest.Builder()
                .action(AnnotationRequest.Action.OBSCURE)
                .categories(Set.of(DetectionCategory.FACE))
                .build();
            ImageData result = cap.annotate(imageData, req);
            long processingTime = System.currentTimeMillis() - startTime;
            logger.info("Face obscuring completed", Map.of(
                "correlationId", correlationId,
                "processingTimeMs", processingTime,
                "backendId", getBackendId()
            ));
            return result;
        }
        throw new VisionProcessingException(
            "Annotation capability not supported by backend",
            "annotate_not_supported",
            "annotate",
            null
        );
    }

    /**
     * Generic annotate call for clients wanting TAG/MARK/OBSCURE with categories.
     *
     * @param imageData the image data to process
     * @param request   the annotation request
     * @return the modified image data
     * @throws BaseVisionException if annotation fails
     */
    public ImageData annotate(ImageData imageData, AnnotationRequest request) throws BaseVisionException {
        if (imageData == null) {
            throw new IllegalArgumentException("Image data must not be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("Annotation request must not be null");
        }
        if (!(backend instanceof AnnotationCapability cap)) {
            throw new VisionProcessingException(
                "Annotation capability not supported by backend",
                "annotate_not_supported",
                "annotate",
                null
            );
        }
        return cap.annotate(imageData, request);
    }

    /**
     * Convenience: draw labels for detections of categories using TAG action.
     *
     * @param imageData  the image data to process
     * @param label      the label to draw
     * @param categories the categories to tag
     * @return the modified image data
     * @throws BaseVisionException if tagging fails
     */
    public ImageData tag(ImageData imageData, String label, Set<DetectionCategory> categories) throws BaseVisionException {
        AnnotationRequest req = new AnnotationRequest.Builder()
            .action(AnnotationRequest.Action.TAG)
            .label(label)
            .categories(categories)
            .build();
        return annotate(imageData, req);
    }

    /**
     * Convenience: draw rectangles for detections of categories using MARK action.
     *
     * @param imageData  the image data to process
     * @param categories the categories to mark
     * @return the modified image data
     * @throws BaseVisionException if marking fails
     */
    public ImageData mark(ImageData imageData, Set<DetectionCategory> categories) throws BaseVisionException {
        AnnotationRequest req = new AnnotationRequest.Builder()
            .action(AnnotationRequest.Action.MARK)
            .categories(categories)
            .build();
        return annotate(imageData, req);
    }

    /**
     * Detect heart-rate estimates from a temporal list of images using backend capability when available.
     *
     * @param imageDataList the list of images to process
     * @return a list of detections
     * @throws BaseVisionException if detection fails
     */
    public List<Detection> detectHeartRate(List<ImageData> imageDataList) throws BaseVisionException {
        if (backend instanceof HeartRateCapability cap) {
            return cap.detectHeartRate(imageDataList);
        }
        throw new VisionProcessingException(
            "Heart rate capability not supported by backend",
            "detectHeartRate",
            "heart-rate"
        );
    }

    /**
     * Detect falls from a temporal list of images using backend capability when available.
     *
     * @param imageDataList the list of images to process
     * @return a list of detections
     * @throws BaseVisionException if detection fails
     */
    public List<Detection> detectFall(List<ImageData> imageDataList) throws BaseVisionException {
        if (backend instanceof FallDetectionCapability cap) {
            return cap.detectFall(imageDataList);
        }
        throw new VisionProcessingException(
            "Fall detection capability not supported by backend",
            "detectFall",
            "fall"
        );
    }

    /**
     * Detect stress estimates from a temporal list of images using backend capability when available.
     *
     * @param imageDataList the list of images to process
     * @return a list of detections
     * @throws BaseVisionException if detection fails
     */
    public List<Detection> detectStress(List<ImageData> imageDataList) throws BaseVisionException {
        if (backend instanceof StressAnalysisCapability cap) {
            return cap.detectStress(imageDataList);
        }
        throw new VisionProcessingException(
            "Stress analysis capability not supported by backend",
            "detectStress",
            "stress"
        );
    }

    /**
     * Generates a unique correlation ID for tracking operations.
     *
     * @return a unique correlation ID
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    // --- Cybersecurity capabilities wrappers ---

    /**
     * Detects security threats from a list of images if the backend exposes ThreatDetectionCapability.
     *
     * @param imageDataList the list of image data to analyze for threats
     * @return the list of detected threats
     * @throws VisionUnsupportedException if capability not available
     */
    public List<Detection> detectThreat(List<ImageData> imageDataList) {
        Objects.requireNonNull(imageDataList, "imageDataList must not be null");
        if (backend instanceof ThreatDetectionCapability cap) {
            return cap.detectThreat(imageDataList);
        }
        throw new VisionUnsupportedException(
            String.format("Threat detection is not supported by backend '%s'", getBackendId()),
            "detectThreat", "cyber-threat");
    }

    /**
     * Detects eavesdropping (shoulder surfing) attempts from a sequence of images
     * if the backend exposes EavesdroppingDetectionCapability.
     *
     * @param imageDataList the list of image data frames to analyze for eavesdropping attempts
     * @return a list of detections describing potential eavesdropping events
     * @throws VisionUnsupportedException if capability not available
     */
    public List<Detection> detectEavesdropping(List<ImageData> imageDataList) {
        Objects.requireNonNull(imageDataList, "imageDataList must not be null");
        if (backend instanceof EavesdroppingDetectionCapability cap) {
            return cap.detectEavesdropping(imageDataList);
        }
        throw new VisionUnsupportedException(
            String.format("Eavesdropping detection is not supported by backend '%s'", getBackendId()),
            "detectEavesdropping", "cyber-eavesdropping");
    }

    /**
     * Authenticates access from a single image if the backend exposes AccessAuthenticationCapability.
     *
     * @param imageData the image containing the subject to authenticate
     * @return a list of detections describing authentication results
     * @throws VisionUnsupportedException if capability not available
     */
    public List<Detection> authenticateAccess(ImageData imageData) {
        Objects.requireNonNull(imageData, "imageData must not be null");
        if (backend instanceof AccessAuthenticationCapability cap) {
            return cap.authenticateAccess(imageData);
        }
        throw new VisionUnsupportedException(
            String.format("Access authentication is not supported by backend '%s'", getBackendId()),
            "authenticateAccess", "cyber-auth");
    }
}
