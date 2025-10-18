package io.github.codesapienbe.springvision.core;

import io.github.codesapienbe.springvision.core.djl.DjlVisionBackend;
import io.github.codesapienbe.springvision.core.capabilities.*;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;
import io.github.codesapienbe.springvision.core.exception.VisionProcessingException;
import io.github.codesapienbe.springvision.core.exception.VisionUnsupportedException;
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
 * @param backend       the vision backend to use for processing operations
 * @param vectorService the vector service for similarity search operations
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
        this(new DjlVisionBackend());
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
     * Generates a unique correlation ID for tracking operations.
     *
     * @return a unique correlation ID
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }


}
