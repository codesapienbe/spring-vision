package com.springvision.core;

import com.springvision.core.exception.BaseVisionException;
import com.springvision.core.exception.VisionProcessingException;
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
 * @since 1.0.0
 * @see VisionBackend
 * @see VisionResult
 * @see ImageData
 * @see DetectionType
 */
public class VisionTemplate {

    private static final Logger logger = LoggerFactory.getLogger(VisionTemplate.class);

    private final VisionBackend backend;

    /** Constructs a new VisionTemplate with the specified backend. */
    public VisionTemplate(VisionBackend backend) {
        this.backend = Objects.requireNonNull(backend, "Vision backend must not be null");
        logger.info("Initialized VisionTemplate with backend: {}", backend.getBackendId());
    }

    /** Gets the underlying vision backend. */
    public VisionBackend getBackend() {
        return backend;
    }

    /** Gets the backend identifier. */
    public String getBackendId() {
        return backend.getBackendId();
    }

    /** Gets the backend display name. */
    public String getBackendDisplayName() {
        return backend.getDisplayName();
    }

    /** Gets the backend version. */
    public String getBackendVersion() {
        return backend.getVersion();
    }

    /** Gets the set of detection types supported by this backend. */
    public java.util.Set<DetectionType> getSupportedDetectionTypes() {
        return backend.getSupportedDetectionTypes();
    }

    /** Checks if this backend supports a specific detection type. */
    public boolean supportsDetectionType(DetectionType detectionType) {
        return backend.supportsDetectionType(detectionType);
    }

    /** Checks if the backend is healthy. */
    public boolean isBackendHealthy() {
        return backend.isHealthy();
    }

    /** Gets detailed health information about the backend. */
    public BackendHealthInfo getBackendHealthInfo() {
        return backend.getHealthInfo();
    }

    /** Detects faces in the provided image data. */
    public VisionResult detectFaces(ImageData imageData) throws BaseVisionException {
        return detect(imageData, DetectionType.FACE);
    }

    /** Detects faces in the provided byte array. */
    public VisionResult detectFaces(byte[] imageBytes) throws BaseVisionException {
        ImageData imageData = ImageData.fromBytes(imageBytes);
        return detectFaces(imageData);
    }

    /** Detects objects in the provided image data. */
    public VisionResult detectObjects(ImageData imageData) throws BaseVisionException {
        return detect(imageData, DetectionType.OBJECT);
    }

    /** Detects objects in the provided byte array. */
    public VisionResult detectObjects(byte[] imageBytes) throws BaseVisionException {
        ImageData imageData = ImageData.fromBytes(imageBytes);
        return detectObjects(imageData);
    }

    /** Performs a generic detection operation based on the specified detection type. */
    public VisionResult detect(ImageData imageData, DetectionType detectionType) throws BaseVisionException {
        Objects.requireNonNull(imageData, "Image data must not be null");
        Objects.requireNonNull(detectionType, "Detection type must not be null");

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

    /** Performs a generic detection operation on byte array data. */
    public VisionResult detect(byte[] imageBytes, DetectionType detectionType) throws BaseVisionException {
        ImageData imageData = ImageData.fromBytes(imageBytes);
        return detect(imageData, detectionType);
    }

    // Capability-aware routing (optional, non-breaking)
    private VisionResult routeViaCapabilitiesIfAvailable(ImageData imageData, DetectionType detectionType) throws BaseVisionException {
        Object b = this.backend;
        switch (detectionType) {
            case FACE -> {
                if (b instanceof com.springvision.core.capabilities.FaceDetectionCapability cap) {
                    List<Detection> detections = cap.detectFaces(imageData);
                    return VisionResult.of(detectionType, detections,
                        detections.isEmpty() ? 0.0 : detections.stream().mapToDouble(Detection::confidence).average().orElse(0.0),
                        0);
                }
            }
            case OBJECT -> {
                if (b instanceof com.springvision.core.capabilities.ObjectDetectionCapability cap) {
                    List<Detection> detections = cap.detectObjects(imageData);
                    return VisionResult.of(detectionType, detections,
                        detections.isEmpty() ? 0.0 : detections.stream().mapToDouble(Detection::confidence).average().orElse(0.0),
                        0);
                }
            }
            case TEXT -> {
                if (b instanceof com.springvision.core.capabilities.TextOcrCapability cap) {
                    List<Detection> detections = cap.detectText(imageData);
                    return VisionResult.of(detectionType, detections,
                        detections.isEmpty() ? 0.0 : detections.stream().mapToDouble(Detection::confidence).average().orElse(0.0),
                        0);
                }
            }
            case BARCODE -> {
                if (b instanceof com.springvision.core.capabilities.BarcodeCapability cap) {
                    List<Detection> detections = cap.detectBarcodes(imageData);
                    return VisionResult.of(detectionType, detections,
                        detections.isEmpty() ? 0.0 : detections.stream().mapToDouble(Detection::confidence).average().orElse(0.0),
                        0);
                }
            }
            case LANDMARK -> {
                if (b instanceof com.springvision.core.capabilities.LandmarkDetectionCapability cap) {
                    List<Detection> detections = cap.detectLandmarks(imageData);
                    return VisionResult.of(detectionType, detections,
                        detections.isEmpty() ? 0.0 : detections.stream().mapToDouble(Detection::confidence).average().orElse(0.0),
                        0);
                }
            }
            case POSE -> {
                if (b instanceof com.springvision.core.capabilities.PoseEstimationCapability cap) {
                    List<Detection> detections = cap.detectPoses(imageData);
                    return VisionResult.of(detectionType, detections,
                        detections.isEmpty() ? 0.0 : detections.stream().mapToDouble(Detection::confidence).average().orElse(0.0),
                        0);
                }
            }
            case HAND -> {
                if (b instanceof com.springvision.core.capabilities.HandDetectionCapability cap) {
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

    /** Performs multiple detection types on the provided image data. */
    public java.util.List<VisionResult> detectMultiple(ImageData imageData, java.util.List<DetectionType> detectionTypes)
            throws BaseVisionException {
        Objects.requireNonNull(imageData, "Image data must not be null");
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

    /** Performs a generic detection using a rich query. */
    public VisionResult detect(ImageData imageData, DetectionQuery query) throws BaseVisionException {
        Objects.requireNonNull(imageData, "Image data must not be null");
        Objects.requireNonNull(query, "Detection query must not be null");
        Objects.requireNonNull(query.getType(), "Detection query type must not be null");

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

    /** Performs a generic detection on byte array using a rich query. */
    public VisionResult detect(byte[] imageBytes, DetectionQuery query) throws BaseVisionException {
        ImageData imageData = ImageData.fromBytes(imageBytes);
        return detect(imageData, query);
    }

    /** Extracts face embeddings using the backend's implementation or default support. */
    public List<float[]> extractEmbeddings(ImageData imageData) throws BaseVisionException {
        Objects.requireNonNull(imageData, "Image data must not be null");
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

    /** Verifies whether two images belong to the same identity. */
    public boolean verify(ImageData a, ImageData b, String metric, double threshold) throws BaseVisionException {
        Objects.requireNonNull(a, "First image must not be null");
        Objects.requireNonNull(b, "Second image must not be null");
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

    /** Obscures faces in the provided image data. */
    public ImageData obscureFaces(ImageData imageData) throws BaseVisionException {
        Objects.requireNonNull(imageData, "Image data must not be null");
        String correlationId = generateCorrelationId();
        long startTime = System.currentTimeMillis();
        logger.info("Starting face obscuring", Map.of(
            "correlationId", correlationId,
            "imageSize", imageData.getSizeInBytes(),
            "imageFormat", imageData.format(),
            "backendId", getBackendId()
        ));
        if (backend instanceof com.springvision.core.capabilities.AnnotationCapability cap) {
            com.springvision.core.AnnotationRequest req = new com.springvision.core.AnnotationRequest.Builder()
                .action(com.springvision.core.AnnotationRequest.Action.OBSCURE)
                .categories(java.util.Set.of(com.springvision.core.DetectionCategory.FACE))
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
        throw new com.springvision.core.exception.VisionProcessingException(
            "Annotation capability not supported by backend",
            "annotate_not_supported",
            "annotate",
            null
        );
    }

    /** Generic annotate call for clients wanting TAG/MARK/OBSCURE with categories. */
    public ImageData annotate(ImageData imageData, com.springvision.core.AnnotationRequest request) throws BaseVisionException {
        Objects.requireNonNull(imageData, "Image data must not be null");
        Objects.requireNonNull(request, "Annotation request must not be null");
        if (!(backend instanceof com.springvision.core.capabilities.AnnotationCapability cap)) {
            throw new com.springvision.core.exception.VisionProcessingException(
                "Annotation capability not supported by backend",
                "annotate_not_supported",
                "annotate",
                null
            );
        }
        return cap.annotate(imageData, request);
    }

    /** Convenience: draw labels for detections of categories using TAG action. */
    public ImageData tag(ImageData imageData, String label, java.util.Set<DetectionCategory> categories) throws BaseVisionException {
        com.springvision.core.AnnotationRequest req = new com.springvision.core.AnnotationRequest.Builder()
            .action(com.springvision.core.AnnotationRequest.Action.TAG)
            .label(label)
            .categories(categories)
            .build();
        return annotate(imageData, req);
    }

    /** Convenience: draw rectangles for detections of categories using MARK action. */
    public ImageData mark(ImageData imageData, java.util.Set<DetectionCategory> categories) throws BaseVisionException {
        com.springvision.core.AnnotationRequest req = new com.springvision.core.AnnotationRequest.Builder()
            .action(com.springvision.core.AnnotationRequest.Action.MARK)
            .categories(categories)
            .build();
        return annotate(imageData, req);
    }

    /** Generates a unique correlation ID for tracking operations. */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
}
