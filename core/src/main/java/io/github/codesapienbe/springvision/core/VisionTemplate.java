package io.github.codesapienbe.springvision.core;

import io.github.codesapienbe.springvision.core.exception.BaseVisionException;
import io.github.codesapienbe.springvision.core.exception.VisionProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.ArrayList;

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
public class VisionTemplate {

    private static final Logger logger = LoggerFactory.getLogger(VisionTemplate.class);

    private final VisionBackend backend;
    private final VectorService vectorService;

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
        this(new io.github.codesapienbe.springvision.core.backend.OpenCvVisionBackend());
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
    public VisionBackend getBackend() {
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
    public java.util.Set<DetectionType> getSupportedDetectionTypes() {
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
    public String storeFaceEmbedding(String personId, float[] embedding, String modelName, String imageHash, Double confidence, java.util.Map<String, Object> metadata) {
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
    public java.util.List<java.util.Map<String, Object>> lookupFaces(float[] queryEmbedding, String modelName, String metric, Double threshold, Integer limit, java.util.Set<String> includePersonIds, java.util.Set<String> excludePersonIds) {
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
        switch (detectionType) {
            case FACE -> {
                if (b instanceof io.github.codesapienbe.springvision.core.capabilities.FaceDetectionCapability cap) {
                    List<Detection> detections = cap.detectFaces(imageData);
                    return VisionResult.of(detectionType, detections,
                        detections.isEmpty() ? 0.0 : detections.stream().mapToDouble(Detection::confidence).average().orElse(0.0),
                        0);
                }
            }
            case OBJECT -> {
                if (b instanceof io.github.codesapienbe.springvision.core.capabilities.ObjectDetectionCapability cap) {
                    List<Detection> detections = cap.detectObjects(imageData);
                    return VisionResult.of(detectionType, detections,
                        detections.isEmpty() ? 0.0 : detections.stream().mapToDouble(Detection::confidence).average().orElse(0.0),
                        0);
                }
            }
            case TEXT -> {
                if (b instanceof io.github.codesapienbe.springvision.core.capabilities.TextOcrCapability cap) {
                    List<Detection> detections = cap.detectText(imageData);
                    return VisionResult.of(detectionType, detections,
                        detections.isEmpty() ? 0.0 : detections.stream().mapToDouble(Detection::confidence).average().orElse(0.0),
                        0);
                }
            }
            case BARCODE -> {
                if (b instanceof io.github.codesapienbe.springvision.core.capabilities.BarcodeCapability cap) {
                    List<Detection> detections = cap.detectBarcodes(imageData);
                    return VisionResult.of(detectionType, detections,
                        detections.isEmpty() ? 0.0 : detections.stream().mapToDouble(Detection::confidence).average().orElse(0.0),
                        0);
                }
            }
            case LANDMARK -> {
                if (b instanceof io.github.codesapienbe.springvision.core.capabilities.LandmarkDetectionCapability cap) {
                    List<Detection> detections = cap.detectLandmarks(imageData);
                    return VisionResult.of(detectionType, detections,
                        detections.isEmpty() ? 0.0 : detections.stream().mapToDouble(Detection::confidence).average().orElse(0.0),
                        0);
                }
            }
            case POSE -> {
                if (b instanceof io.github.codesapienbe.springvision.core.capabilities.PoseEstimationCapability cap) {
                    List<Detection> detections = cap.detectPoses(imageData);
                    return VisionResult.of(detectionType, detections,
                        detections.isEmpty() ? 0.0 : detections.stream().mapToDouble(Detection::confidence).average().orElse(0.0),
                        0);
                }
            }
            case HAND -> {
                if (b instanceof io.github.codesapienbe.springvision.core.capabilities.HandDetectionCapability cap) {
                    List<Detection> detections = cap.detectHands(imageData);
                    return VisionResult.of(detectionType, detections,
                        detections.isEmpty() ? 0.0 : detections.stream().mapToDouble(Detection::confidence).average().orElse(0.0),
                        0);
                }
            }
            case CUSTOM -> { /* fall through to backend */ }
        }
        List<Detection> detections = backend.detect(imageData, detectionType);
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
    public java.util.List<VisionResult> detectMultiple(ImageData imageData, java.util.List<DetectionType> detectionTypes)
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
            java.util.List<List<Detection>> detectionLists = backend.detectMultiple(imageData, detectionTypes);
            long processingTime = System.currentTimeMillis() - startTime;

            // Convert List<List<Detection>> to List<VisionResult>
            java.util.List<VisionResult> results = new ArrayList<>();
            for (int i = 0; i < detectionTypes.size(); i++) {
                List<Detection> detections = detectionLists.get(i);
                DetectionType detectionType = detectionTypes.get(i);

                VisionResult result = VisionResult.of(detectionType, detections,
                    detections.isEmpty() ? 0.0 : detections.stream().mapToDouble(Detection::confidence).average().orElse(0.0),
                    processingTime);
                results.add(result);
            }

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
            List<Detection> detections = backend.detect(imageData, query.getType());
            long processingTime = System.currentTimeMillis() - startTime;

            // Convert List<Detection> to VisionResult
            VisionResult result = VisionResult.of(query.getType(), detections,
                detections.isEmpty() ? 0.0 : detections.stream().mapToDouble(Detection::confidence).average().orElse(0.0),
                processingTime);

            logger.info("Detection query completed", Map.of(
                "correlationId", correlationId,
                "detectionType", query.getType().getDisplayName(),
                "detectionCount", detections.size(),
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
     * Performs a generic detection on byte array using a rich query.
     *
     * @param imageBytes the image data to process
     * @param query      the detection query
     * @return the vision result
     * @throws BaseVisionException if detection fails
     */
    public VisionResult detect(byte[] imageBytes, DetectionQuery query) throws BaseVisionException {
        ImageData imageData = ImageData.fromBytes(imageBytes);
        return detect(imageData, query);
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
        List<float[]> embeddings = backend.extractEmbeddings(imageData);
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
        boolean result = backend.verify(a, b, metric, threshold);
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
        if (backend instanceof io.github.codesapienbe.springvision.core.capabilities.AnnotationCapability cap) {
            io.github.codesapienbe.springvision.core.AnnotationRequest req = new io.github.codesapienbe.springvision.core.AnnotationRequest.Builder()
                .action(io.github.codesapienbe.springvision.core.AnnotationRequest.Action.OBSCURE)
                .categories(java.util.Set.of(io.github.codesapienbe.springvision.core.DetectionCategory.FACE))
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
        throw new io.github.codesapienbe.springvision.core.exception.VisionProcessingException(
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
    public ImageData annotate(ImageData imageData, io.github.codesapienbe.springvision.core.AnnotationRequest request) throws BaseVisionException {
        if (imageData == null) {
            throw new IllegalArgumentException("Image data must not be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("Annotation request must not be null");
        }
        if (!(backend instanceof io.github.codesapienbe.springvision.core.capabilities.AnnotationCapability cap)) {
            throw new io.github.codesapienbe.springvision.core.exception.VisionProcessingException(
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
    public ImageData tag(ImageData imageData, String label, java.util.Set<DetectionCategory> categories) throws BaseVisionException {
        io.github.codesapienbe.springvision.core.AnnotationRequest req = new io.github.codesapienbe.springvision.core.AnnotationRequest.Builder()
            .action(io.github.codesapienbe.springvision.core.AnnotationRequest.Action.TAG)
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
    public ImageData mark(ImageData imageData, java.util.Set<DetectionCategory> categories) throws BaseVisionException {
        io.github.codesapienbe.springvision.core.AnnotationRequest req = new io.github.codesapienbe.springvision.core.AnnotationRequest.Builder()
            .action(io.github.codesapienbe.springvision.core.AnnotationRequest.Action.MARK)
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
    public java.util.List<io.github.codesapienbe.springvision.core.Detection> detectHeartRate(java.util.List<ImageData> imageDataList) throws BaseVisionException {
        if (backend instanceof io.github.codesapienbe.springvision.core.capabilities.HeartRateCapability cap) {
            return cap.detectHeartRate(imageDataList);
        }
        throw new io.github.codesapienbe.springvision.core.exception.VisionProcessingException(
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
    public java.util.List<io.github.codesapienbe.springvision.core.Detection> detectFall(java.util.List<ImageData> imageDataList) throws BaseVisionException {
        if (backend instanceof io.github.codesapienbe.springvision.core.capabilities.FallDetectionCapability cap) {
            return cap.detectFall(imageDataList);
        }
        throw new io.github.codesapienbe.springvision.core.exception.VisionProcessingException(
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
    public java.util.List<io.github.codesapienbe.springvision.core.Detection> detectStress(java.util.List<ImageData> imageDataList) throws BaseVisionException {
        if (backend instanceof io.github.codesapienbe.springvision.core.capabilities.StressAnalysisCapability cap) {
            return cap.detectStress(imageDataList);
        }
        throw new io.github.codesapienbe.springvision.core.exception.VisionProcessingException(
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
}
