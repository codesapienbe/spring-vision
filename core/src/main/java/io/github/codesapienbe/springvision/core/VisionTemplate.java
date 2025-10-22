package io.github.codesapienbe.springvision.core;

import io.github.codesapienbe.springvision.core.djl.DjlVisionBackend;
import io.github.codesapienbe.springvision.core.capabilities.AccessAuthenticationCapability;
import io.github.codesapienbe.springvision.core.capabilities.ActionRecognitionCapability;
import io.github.codesapienbe.springvision.core.capabilities.AnnotationCapability;
import io.github.codesapienbe.springvision.core.capabilities.BarcodeCapability;
import io.github.codesapienbe.springvision.core.capabilities.DeepfakeDetectionCapability;
import io.github.codesapienbe.springvision.core.capabilities.DemographicsCapability;
import io.github.codesapienbe.springvision.core.capabilities.EmbeddingCapability;
import io.github.codesapienbe.springvision.core.capabilities.EmotionDetectionCapability;
import io.github.codesapienbe.springvision.core.capabilities.FaceDetectionCapability;
import io.github.codesapienbe.springvision.core.capabilities.FallDetectionCapability;
import io.github.codesapienbe.springvision.core.capabilities.HandDetectionCapability;
import io.github.codesapienbe.springvision.core.capabilities.HeartRateCapability;
import io.github.codesapienbe.springvision.core.capabilities.ImageClassificationCapability;
import io.github.codesapienbe.springvision.core.capabilities.MetaDataExtractionCapability;
import io.github.codesapienbe.springvision.core.capabilities.NSFWDetectionCapability;
import io.github.codesapienbe.springvision.core.capabilities.ObjectDetectionCapability;
import io.github.codesapienbe.springvision.core.capabilities.OcrCapability;
import io.github.codesapienbe.springvision.core.capabilities.PoseEstimationCapability;
import io.github.codesapienbe.springvision.core.capabilities.StressAnalysisCapability;
import io.github.codesapienbe.springvision.core.capabilities.ThreatDetectionCapability;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;
import io.github.codesapienbe.springvision.core.exception.VisionProcessingException;
import io.github.codesapienbe.springvision.core.exception.VisionUnsupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

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

    // ============================================================================
    // High-Level Detection Methods - Return VisionResult
    // ============================================================================

    /**
     * Detects faces in an image.
     *
     * @param imageData the image data to process
     * @return VisionResult containing face detections
     * @throws VisionUnsupportedException if backend doesn't support face detection
     */
    public VisionResult detectFaces(ImageData imageData) {
        if (!(backend instanceof FaceDetectionCapability capability)) {
            throw new VisionUnsupportedException("Face detection not supported by backend: " + getBackendId());
        }
        long startTime = System.currentTimeMillis();
        List<Detection> detections = capability.detectFaces(imageData);
        return buildResult(DetectionType.FACE, detections, startTime);
    }

    /**
     * Detects objects in an image.
     *
     * @param imageData the image data to process
     * @return VisionResult containing object detections
     * @throws VisionUnsupportedException if backend doesn't support object detection
     */
    public VisionResult detectObjects(ImageData imageData) {
        if (!(backend instanceof ObjectDetectionCapability capability)) {
            throw new VisionUnsupportedException("Object detection not supported by backend: " + getBackendId());
        }
        long startTime = System.currentTimeMillis();
        List<Detection> detections = capability.detectObjects(imageData);
        return buildResult(DetectionType.OBJECT, detections, startTime);
    }

    /**
     * Extracts text from an image using OCR.
     *
     * @param imageData the image data to process
     * @return VisionResult containing text detections
     * @throws VisionUnsupportedException if backend doesn't support OCR
     */
    public VisionResult extractText(ImageData imageData) {
        if (!(backend instanceof OcrCapability capability)) {
            throw new VisionUnsupportedException("OCR not supported by backend: " + getBackendId());
        }
        long startTime = System.currentTimeMillis();
        List<OcrCapability.TextDetection> textDetections = capability.extractText(imageData);
        
        // Convert TextDetection to Detection
        List<Detection> detections = textDetections.stream()
            .map(td -> new Detection(
                td.text(),
                td.confidence(),
                td.boundingBox() != null ? convertBoundingBox(td.boundingBox()) : new BoundingBox(0, 0, 0, 0),
                Map.of("text", td.text(), "attributes", td.attributes())
            ))
            .toList();
        
        return buildResult(DetectionType.TEXT, detections, startTime);
    }

    /**
     * Classifies an image into categories.
     *
     * @param imageData the image data to process
     * @param topK the number of top predictions to return
     * @return VisionResult containing classification results
     * @throws VisionUnsupportedException if backend doesn't support image classification
     */
    public VisionResult classifyImage(ImageData imageData, int topK) {
        if (!(backend instanceof ImageClassificationCapability capability)) {
            throw new VisionUnsupportedException("Image classification not supported by backend: " + getBackendId());
        }
        long startTime = System.currentTimeMillis();
        ImageClassificationCapability.ClassificationResult result = capability.classifyImage(imageData, topK);
        
        // Convert Classifications to Detection
        List<Detection> detections = result.classifications().stream()
            .map(c -> new Detection(
                c.label(),
                c.confidence(),
                new BoundingBox(0, 0, 0, 0), // No bounding box for classification
                Map.of("classification", c.label())
            ))
            .toList();
        
        return buildResult(DetectionType.IMAGE_CLASSIFICATION, detections, startTime);
    }

    /**
     * Detects poses in an image.
     *
     * @param imageData the image data to process
     * @return VisionResult containing pose detections
     * @throws VisionUnsupportedException if backend doesn't support pose estimation
     */
    public VisionResult detectPoses(ImageData imageData) {
        if (!(backend instanceof PoseEstimationCapability capability)) {
            throw new VisionUnsupportedException("Pose estimation not supported by backend: " + getBackendId());
        }
        long startTime = System.currentTimeMillis();
        List<Detection> detections = capability.detectPoses(imageData);
        return buildResult(DetectionType.POSE, detections, startTime);
    }

    /**
     * Recognizes actions in an image.
     *
     * @param imageData the image data to process
     * @return VisionResult containing action detections
     * @throws VisionUnsupportedException if backend doesn't support action recognition
     */
    public VisionResult recognizeActions(ImageData imageData) {
        if (!(backend instanceof ActionRecognitionCapability capability)) {
            throw new VisionUnsupportedException("Action recognition not supported by backend: " + getBackendId());
        }
        long startTime = System.currentTimeMillis();
        List<Detection> detections = capability.recognizeActions(imageData);
        return buildResult(DetectionType.ACTION_RECOGNITION, detections, startTime);
    }

    /**
     * Detects NSFW content in an image.
     *
     * @param imageData the image data to process
     * @return VisionResult containing NSFW detection results
     * @throws VisionUnsupportedException if backend doesn't support NSFW detection
     */
    public VisionResult detectNSFW(ImageData imageData) {
        if (!(backend instanceof NSFWDetectionCapability capability)) {
            throw new VisionUnsupportedException("NSFW detection not supported by backend: " + getBackendId());
        }
        long startTime = System.currentTimeMillis();
        List<Detection> detections = capability.detectNSFW(imageData);
        return buildResult(DetectionType.NSFW, detections, startTime);
    }

    /**
     * Detects emotions in an image.
     *
     * @param imageData the image data to process
     * @return VisionResult containing emotion detections
     * @throws VisionUnsupportedException if backend doesn't support emotion detection
     */
    public VisionResult detectEmotions(ImageData imageData) {
        if (!(backend instanceof EmotionDetectionCapability capability)) {
            throw new VisionUnsupportedException("Emotion detection not supported by backend: " + getBackendId());
        }
        long startTime = System.currentTimeMillis();
        List<Detection> detections = capability.detectEmotions(imageData);
        return buildResult(DetectionType.EMOTION, detections, startTime);
    }

    /**
     * Detects deepfakes in an image.
     *
     * @param imageData the image data to process
     * @return VisionResult containing deepfake detection results
     * @throws VisionUnsupportedException if backend doesn't support deepfake detection
     */
    public VisionResult detectDeepfake(ImageData imageData) {
        if (!(backend instanceof DeepfakeDetectionCapability capability)) {
            throw new VisionUnsupportedException("Deepfake detection not supported by backend: " + getBackendId());
        }
        long startTime = System.currentTimeMillis();
        List<Detection> detections = capability.detectDeepfake(imageData);
        return buildResult(DetectionType.DEEPFAKE, detections, startTime);
    }

    /**
     * Detects hands in an image.
     *
     * @param imageData the image data to process
     * @return VisionResult containing hand detections
     * @throws VisionUnsupportedException if backend doesn't support hand detection
     */
    public VisionResult detectHands(ImageData imageData) {
        if (!(backend instanceof HandDetectionCapability capability)) {
            throw new VisionUnsupportedException("Hand detection not supported by backend: " + getBackendId());
        }
        long startTime = System.currentTimeMillis();
        List<Detection> detections = capability.detectHands(imageData);
        return buildResult(DetectionType.HAND, detections, startTime);
    }

    /**
     * Detects demographics (age, gender) in an image.
     *
     * @param imageData the image data to process
     * @return VisionResult containing demographics detections
     * @throws VisionUnsupportedException if backend doesn't support demographics detection
     */
    public VisionResult detectDemographics(ImageData imageData) {
        if (!(backend instanceof DemographicsCapability capability)) {
            throw new VisionUnsupportedException("Demographics detection not supported by backend: " + getBackendId());
        }
        long startTime = System.currentTimeMillis();
        List<Detection> detections = capability.detectDemographics(imageData);
        return buildResult(DetectionType.DEMOGRAPHICS, detections, startTime);
    }

    /**
     * Detects falls in a sequence of images.
     *
     * @param imageSequence the sequence of images to analyze
     * @return VisionResult containing fall detection results
     * @throws VisionUnsupportedException if backend doesn't support fall detection
     */
    public VisionResult detectFall(List<ImageData> imageSequence) {
        if (!(backend instanceof FallDetectionCapability capability)) {
            throw new VisionUnsupportedException("Fall detection not supported by backend: " + getBackendId());
        }
        long startTime = System.currentTimeMillis();
        List<Detection> detections = capability.detectFall(imageSequence);
        return buildResult(DetectionType.FALL, detections, startTime);
    }

    /**
     * Analyzes stress levels in a sequence of images.
     *
     * @param imageSequence the sequence of images to analyze
     * @return VisionResult containing stress analysis results
     * @throws VisionUnsupportedException if backend doesn't support stress analysis
     */
    public VisionResult analyzeStress(List<ImageData> imageSequence) {
        if (!(backend instanceof StressAnalysisCapability capability)) {
            throw new VisionUnsupportedException("Stress analysis not supported by backend: " + getBackendId());
        }
        long startTime = System.currentTimeMillis();
        List<Detection> detections = capability.detectStress(imageSequence);
        return buildResult(DetectionType.STRESS, detections, startTime);
    }

    /**
     * Estimates heart rate from a sequence of images.
     *
     * @param imageSequence the sequence of images to analyze
     * @return VisionResult containing heart rate estimation
     * @throws VisionUnsupportedException if backend doesn't support heart rate detection
     */
    public VisionResult estimateHeartRate(List<ImageData> imageSequence) {
        if (!(backend instanceof HeartRateCapability capability)) {
            throw new VisionUnsupportedException("Heart rate detection not supported by backend: " + getBackendId());
        }
        long startTime = System.currentTimeMillis();
        List<Detection> detections = capability.detectHeartRate(imageSequence);
        return buildResult(DetectionType.HEART_RATE, detections, startTime);
    }

    /**
     * Scans and decodes barcodes in an image.
     *
     * @param imageData the image data to process
     * @return VisionResult containing barcode detections
     * @throws VisionUnsupportedException if backend doesn't support barcode detection
     */
    public VisionResult scanBarcodes(ImageData imageData) {
        if (!(backend instanceof BarcodeCapability capability)) {
            throw new VisionUnsupportedException("Barcode detection not supported by backend: " + getBackendId());
        }
        long startTime = System.currentTimeMillis();
        List<Detection> detections = capability.detectBarcodes(imageData);
        return buildResult(DetectionType.BARCODE, detections, startTime);
    }

    /**
     * Extracts metadata from an image.
     *
     * @param imageData the image data to process
     * @return VisionResult containing metadata detections
     * @throws VisionUnsupportedException if backend doesn't support metadata extraction
     */
    public VisionResult extractMetadata(ImageData imageData) {
        if (!(backend instanceof MetaDataExtractionCapability capability)) {
            throw new VisionUnsupportedException("Metadata extraction not supported by backend: " + getBackendId());
        }
        long startTime = System.currentTimeMillis();
        List<Detection> detections = capability.extractMetaData(imageData);
        return buildResult(DetectionType.METADATA_EXTRACTION, detections, startTime);
    }

    /**
     * Detects security threats in a sequence of images.
     *
     * @param imageSequence the sequence of images to analyze
     * @return VisionResult containing threat detections
     * @throws VisionUnsupportedException if backend doesn't support threat detection
     */
    public VisionResult detectThreats(List<ImageData> imageSequence) {
        if (!(backend instanceof ThreatDetectionCapability capability)) {
            throw new VisionUnsupportedException("Threat detection not supported by backend: " + getBackendId());
        }
        long startTime = System.currentTimeMillis();
        List<Detection> detections = capability.detectThreat(imageSequence);
        return buildResult(DetectionType.THREAT, detections, startTime);
    }

    /**
     * Authenticates access using face recognition.
     *
     * @param imageData the image data to process
     * @return VisionResult containing authentication results
     * @throws VisionUnsupportedException if backend doesn't support access authentication
     */
    public VisionResult authenticateAccess(ImageData imageData) {
        if (!(backend instanceof AccessAuthenticationCapability capability)) {
            throw new VisionUnsupportedException("Access authentication not supported by backend: " + getBackendId());
        }
        long startTime = System.currentTimeMillis();
        List<Detection> detections = capability.authenticateAccess(imageData);
        return buildResult(DetectionType.ACCESS_AUTH, detections, startTime);
    }

    /**
     * Extracts embeddings from an image for a specific category.
     *
     * @param imageData the image data to process
     * @param category the detection category (FACE, OBJECT, etc.)
     * @return list of embedding vectors
     * @throws VisionUnsupportedException if backend doesn't support embedding extraction
     */
    public List<float[]> extractEmbeddings(ImageData imageData, DetectionCategory category) {
        if (!(backend instanceof EmbeddingCapability capability)) {
            throw new VisionUnsupportedException("Embedding extraction not supported by backend: " + getBackendId());
        }
        return capability.extractEmbeddings(imageData, category);
    }

    // ============================================================================
    // Helper Methods
    // ============================================================================

    /**
     * Builds a VisionResult from detections.
     *
     * @param detectionType the type of detection
     * @param detections the list of detections
     * @param startTime the start time in milliseconds
     * @return VisionResult instance
     */
    private VisionResult buildResult(DetectionType detectionType, List<Detection> detections, long startTime) {
        long processingTime = System.currentTimeMillis() - startTime;
        double avgConfidence = detections.isEmpty() ? 0.0 :
            detections.stream().mapToDouble(Detection::confidence).average().orElse(0.0);
        
        Map<String, Object> metadata = Map.of(
            "backendId", getBackendId(),
            "backendVersion", getBackendVersion()
        );
        
        return VisionResult.of(detectionType, detections, avgConfidence, processingTime, metadata);
    }

    /**
     * Converts OCR bounding box map to BoundingBox object.
     *
     * @param bboxMap the bounding box map
     * @return BoundingBox instance
     */
    private BoundingBox convertBoundingBox(Map<String, Object> bboxMap) {
        try {
            double x = ((Number) bboxMap.get("x")).doubleValue();
            double y = ((Number) bboxMap.get("y")).doubleValue();
            double width = ((Number) bboxMap.get("width")).doubleValue();
            double height = ((Number) bboxMap.get("height")).doubleValue();
            return new BoundingBox(x, y, width, height);
        } catch (Exception e) {
            logger.warn("Failed to convert bounding box map: {}", e.getMessage());
            return new BoundingBox(0, 0, 0, 0);
        }
    }
}
