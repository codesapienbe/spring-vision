package io.github.codesapienbe.springvision.starter.web;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.github.codesapienbe.springvision.core.DetectionType;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.VisionResult;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import io.github.codesapienbe.springvision.starter.web.dto.DetectionRequest;
import io.github.codesapienbe.springvision.starter.web.dto.DetectionResponse;
import io.github.codesapienbe.springvision.starter.web.dto.HealthResponse;
import io.github.codesapienbe.springvision.starter.web.dto.MultipleDetectionRequest;
import io.github.codesapienbe.springvision.starter.web.dto.MultipleDetectionResponse;
import io.github.codesapienbe.springvision.starter.web.dto.TaskSubmissionResponse;

/**
 * REST controller for Spring Vision operations.
 *
 * <p>This controller provides REST API endpoints for computer vision operations
 * including face detection, object detection, and health monitoring. It supports
 * both file uploads and direct image data processing.</p>
 *
 * <p>All endpoints include proper error handling, validation, and logging.
 * Responses include correlation IDs for request tracking and debugging.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Face detection with file upload
 * POST /api/vision/detect/faces
 * Content-Type: multipart/form-data
 *
 * // Object detection with JSON payload
 * POST /api/vision/detect/objects
 * Content-Type: application/json
 *
 * // Health check
 * GET /api/vision/health
 * }</pre>
 *
 * @author Spring Vision Team
 * @see VisionTemplate
 * @see DetectionRequest
 * @see DetectionResponse
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/vision")
@CrossOrigin(origins = "*")
public class VisionController {

    private static final Logger logger = LoggerFactory.getLogger(VisionController.class);

    /**
     * Maximum file size for uploads (10MB).
     */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * Supported image content types.
     */
    private static final List<String> SUPPORTED_CONTENT_TYPES = List.of(
        "image/jpeg", "image/jpg", "image/png", "image/gif", "image/bmp"
    );

    /**
     * The vision template for processing operations.
     */
    private final VisionTemplate visionTemplate;

    /**
     * Constructs a new vision controller.
     *
     * @param visionTemplate the vision template for processing operations
     */
    public VisionController(VisionTemplate visionTemplate) {
        this.visionTemplate = visionTemplate;
    }

    /**
     * Detects faces in an uploaded image file.
     *
     * <p>This endpoint accepts a multipart file upload and performs face detection
     * on the image. It validates the file size and content type before processing.</p>
     *
     * @param file          the uploaded image file
     * @param minConfidence the minimum confidence threshold for detections (optional)
     * @return face detection results
     */
    @PostMapping(value = "/detect/faces", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DetectionResponse> detectFacesFromFile(
        @RequestParam("file") MultipartFile file,
        @RequestParam(name = "minConfidence", required = false) Double minConfidence) {

        String correlationId = generateCorrelationId();

        logger.info("Face detection request received", Map.of(
            "correlationId", correlationId,
            "fileName", file.getOriginalFilename(),
            "fileSize", file.getSize(),
            "contentType", file.getContentType(),
            "minConfidence", minConfidence
        ));

        try {
            // Validate file
            validateFile(file);

            // Convert to ImageData
            ImageData imageData = convertToImageData(file);

            // Perform face detection using capability-based approach
            VisionResult result = executeCapabilityDetection(imageData, DetectionType.FACE);
            List<io.github.codesapienbe.springvision.core.Detection> detections = result.detections();
            if (minConfidence != null) {
                double thr = Math.max(0.0, Math.min(1.0, minConfidence));
                detections = detections.stream().filter(d -> d.confidence() >= thr).toList();
            }

            // Create response
            DetectionResponse response = DetectionResponse.builder()
                .correlationId(correlationId)
                .detectionType(DetectionType.FACE.getCode())
                .detectionCount(detections.size())
                .averageConfidence(result.averageConfidence())
                .processingTimeMs(result.processingTimeMs())
                .detections(detections)
                .build();

            logger.info("Face detection completed successfully", Map.of(
                "correlationId", correlationId,
                "detectionCount", detections.size(),
                "processingTimeMs", result.processingTimeMs()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Face detection failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);

            DetectionResponse errorResponse = DetectionResponse.builder()
                .correlationId(correlationId)
                .detectionType(DetectionType.FACE.getCode())
                .error(e.getMessage())
                .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
        }
    }

    /**
     * Detects faces in image data provided in the request body.
     *
     * <p>This endpoint accepts image data as a JSON payload and performs face detection.
     * The image data should be base64 encoded.</p>
     *
     * @param request       the detection request containing image data
     * @param minConfidence the minimum confidence threshold for detections (optional)
     * @return face detection results
     */
    @PostMapping(value = "/detect/faces", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DetectionResponse> detectFacesFromData(
        @RequestBody DetectionRequest request,
        @RequestParam(name = "minConfidence", required = false) Double minConfidence) {

        String correlationId = generateCorrelationId();

        logger.info("Face detection request received", Map.of(
            "correlationId", correlationId,
            "imageSize", request.getImageData().length,
            "minConfidence", minConfidence
        ));

        try {
            // Convert to ImageData
            ImageData imageData = ImageData.fromBytes(request.getImageData());

            // Perform face detection using capability-based approach
            VisionResult result = executeCapabilityDetection(imageData, DetectionType.FACE);
            List<io.github.codesapienbe.springvision.core.Detection> detections = result.detections();
            if (minConfidence != null) {
                double thr = Math.max(0.0, Math.min(1.0, minConfidence));
                detections = detections.stream().filter(d -> d.confidence() >= thr).toList();
            }

            // Create response
            DetectionResponse response = DetectionResponse.builder()
                .correlationId(correlationId)
                .detectionType(DetectionType.FACE.getCode())
                .detectionCount(detections.size())
                .averageConfidence(result.averageConfidence())
                .processingTimeMs(result.processingTimeMs())
                .detections(detections)
                .build();

            logger.info("Face detection completed successfully", Map.of(
                "correlationId", correlationId,
                "detectionCount", detections.size(),
                "processingTimeMs", result.processingTimeMs()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Face detection failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);

            DetectionResponse errorResponse = DetectionResponse.builder()
                .correlationId(correlationId)
                .detectionType(DetectionType.FACE.getCode())
                .error(e.getMessage())
                .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
        }
    }

    /**
     * Asynchronously detects faces in an uploaded image file.
     * @param file The uploaded image file.
     * @return A CompletableFuture containing the face detection results.
     */
    @PostMapping(value = "/async/detect/faces", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Async("visionAsyncExecutor")
    public CompletableFuture<ResponseEntity<DetectionResponse>> detectFacesFromFileAsync(
        @RequestParam("file") MultipartFile file) {

        String correlationId = generateCorrelationId();

        logger.info("Async face detection request received", Map.of(
            "correlationId", correlationId,
            "fileName", file.getOriginalFilename(),
            "fileSize", file.getSize(),
            "contentType", file.getContentType()
        ));

        try {
            // Validate file
            validateFile(file);

            // Convert to ImageData
            ImageData imageData = convertToImageData(file);

            return CompletableFuture.supplyAsync(() -> {
                VisionResult result = executeCapabilityDetection(imageData, DetectionType.FACE);
                DetectionResponse response = DetectionResponse.builder()
                    .correlationId(correlationId)
                    .detectionType(DetectionType.FACE.getCode())
                    .detectionCount(result.detectionCount())
                    .averageConfidence(result.averageConfidence())
                    .processingTimeMs(result.processingTimeMs())
                    .detections(result.detections())
                    .build();
                logger.info("Async face detection completed successfully", Map.of(
                    "correlationId", correlationId,
                    "detectionCount", result.detectionCount(),
                    "processingTimeMs", result.processingTimeMs()
                ));
                return ResponseEntity.ok(response);
            });

        } catch (Exception e) {
            logger.error("Async face detection failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);

            DetectionResponse errorResponse = DetectionResponse.builder()
                .correlationId(correlationId)
                .detectionType(DetectionType.FACE.getCode())
                .error(e.getMessage())
                .build();

            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse));
        }
    }

    /**
     * Asynchronously detects faces in image data provided in the request body.
     * @param request The detection request containing image data.
     * @return A CompletableFuture containing the face detection results.
     */
    @PostMapping(value = "/async/detect/faces", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Async("visionAsyncExecutor")
    public CompletableFuture<ResponseEntity<DetectionResponse>> detectFacesFromDataAsync(
        @RequestBody DetectionRequest request) {

        String correlationId = generateCorrelationId();

        logger.info("Async face detection request received", Map.of(
            "correlationId", correlationId,
            "imageSize", request.getImageData().length
        ));

        try {
            // Convert to ImageData
            ImageData imageData = ImageData.fromBytes(request.getImageData());

            return CompletableFuture.supplyAsync(() -> {
                VisionResult result = executeCapabilityDetection(imageData, DetectionType.FACE);
                DetectionResponse response = DetectionResponse.builder()
                    .correlationId(correlationId)
                    .detectionType(DetectionType.FACE.getCode())
                    .detectionCount(result.detectionCount())
                    .averageConfidence(result.averageConfidence())
                    .processingTimeMs(result.processingTimeMs())
                    .detections(result.detections())
                    .build();
                logger.info("Async face detection completed successfully", Map.of(
                    "correlationId", correlationId,
                    "detectionCount", result.detectionCount(),
                    "processingTimeMs", result.processingTimeMs()
                ));
                return ResponseEntity.ok(response);
            });

        } catch (Exception e) {
            logger.error("Async face detection failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);

            DetectionResponse errorResponse = DetectionResponse.builder()
                .correlationId(correlationId)
                .detectionType(DetectionType.FACE.getCode())
                .error(e.getMessage())
                .build();

            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse));
        }
    }

    /**
     * Detects objects in an uploaded image file.
     *
     * <p>This endpoint accepts a multipart file upload and performs object detection
     * on the image. It validates the file size and content type before processing.</p>
     *
     * @param file the uploaded image file
     * @return object detection results
     */
    @PostMapping(value = "/detect/objects", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DetectionResponse> detectObjectsFromFile(
        @RequestParam("file") MultipartFile file) {

        String correlationId = generateCorrelationId();

        logger.info("Object detection request received", Map.of(
            "correlationId", correlationId,
            "fileName", file.getOriginalFilename(),
            "fileSize", file.getSize(),
            "contentType", file.getContentType()
        ));

        try {
            // Validate file
            validateFile(file);

            // Convert to ImageData
            ImageData imageData = convertToImageData(file);

            // Perform object detection using capability-based approach
            VisionResult result = executeCapabilityDetection(imageData, DetectionType.OBJECT);

            // Create response
            DetectionResponse response = DetectionResponse.builder()
                .correlationId(correlationId)
                .detectionType(DetectionType.OBJECT.getCode())
                .detectionCount(result.detectionCount())
                .averageConfidence(result.averageConfidence())
                .processingTimeMs(result.processingTimeMs())
                .detections(result.detections())
                .build();

            logger.info("Object detection completed successfully", Map.of(
                "correlationId", correlationId,
                "detectionCount", result.detectionCount(),
                "processingTimeMs", result.processingTimeMs()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Object detection failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);

            DetectionResponse errorResponse = DetectionResponse.builder()
                .correlationId(correlationId)
                .detectionType(DetectionType.OBJECT.getCode())
                .error(e.getMessage())
                .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
        }
    }

    /**
     * Detects objects in image data provided in the request body.
     *
     * <p>This endpoint accepts image data as a JSON payload and performs object detection.
     * The image data should be base64 encoded.</p>
     *
     * @param request the detection request containing image data
     * @return object detection results
     */
    @PostMapping(value = "/detect/objects", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DetectionResponse> detectObjectsFromData(
        @RequestBody DetectionRequest request) {

        String correlationId = generateCorrelationId();

        logger.info("Object detection request received", Map.of(
            "correlationId", correlationId,
            "imageSize", request.getImageData().length
        ));

        try {
            // Convert to ImageData
            ImageData imageData = ImageData.fromBytes(request.getImageData());

            // Perform object detection using capability-based approach
            VisionResult result = executeCapabilityDetection(imageData, DetectionType.OBJECT);

            // Create response
            DetectionResponse response = DetectionResponse.builder()
                .correlationId(correlationId)
                .detectionType(DetectionType.OBJECT.getCode())
                .detectionCount(result.detectionCount())
                .averageConfidence(result.averageConfidence())
                .processingTimeMs(result.processingTimeMs())
                .detections(result.detections())
                .build();

            logger.info("Object detection completed successfully", Map.of(
                "correlationId", correlationId,
                "detectionCount", result.detectionCount(),
                "processingTimeMs", result.processingTimeMs()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Object detection failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);

            DetectionResponse errorResponse = DetectionResponse.builder()
                .correlationId(correlationId)
                .detectionType(DetectionType.OBJECT.getCode())
                .error(e.getMessage())
                .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
        }
    }

    /**
     * Asynchronously detects objects in an uploaded image file.
     * @param file The uploaded image file.
     * @return A CompletableFuture containing the object detection results.
     */
    @PostMapping(value = "/async/detect/objects", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Async("visionAsyncExecutor")
    public CompletableFuture<ResponseEntity<DetectionResponse>> detectObjectsFromFileAsync(
        @RequestParam("file") MultipartFile file) {

        String correlationId = generateCorrelationId();

        logger.info("Async object detection request received", Map.of(
            "correlationId", correlationId,
            "fileName", file.getOriginalFilename(),
            "fileSize", file.getSize(),
            "contentType", file.getContentType()
        ));

        try {
            validateFile(file);
            ImageData imageData = convertToImageData(file);
            VisionResult result = executeCapabilityDetection(imageData, DetectionType.OBJECT);

            DetectionResponse response = DetectionResponse.builder()
                .correlationId(correlationId)
                .detectionType(DetectionType.OBJECT.getCode())
                .detectionCount(result.detectionCount())
                .averageConfidence(result.averageConfidence())
                .processingTimeMs(result.processingTimeMs())
                .detections(result.detections())
                .build();

            logger.info("Async object detection completed successfully", Map.of(
                "correlationId", correlationId,
                "detectionCount", result.detectionCount(),
                "processingTimeMs", result.processingTimeMs()
            ));

            return CompletableFuture.completedFuture(ResponseEntity.ok(response));

        } catch (Exception e) {
            logger.error("Async object detection failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);

            DetectionResponse errorResponse = DetectionResponse.builder()
                .correlationId(correlationId)
                .detectionType(DetectionType.OBJECT.getCode())
                .error(e.getMessage())
                .build();

            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse));
        }
    }

    /**
     * Asynchronously detects objects in image data provided in the request body.
     * @param request The detection request containing image data.
     * @return A CompletableFuture containing the object detection results.
     */
    @PostMapping(value = "/async/detect/objects", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Async("visionAsyncExecutor")
    public CompletableFuture<ResponseEntity<DetectionResponse>> detectObjectsFromDataAsync(
        @RequestBody DetectionRequest request) {

        String correlationId = generateCorrelationId();

        logger.info("Async object detection request received", Map.of(
            "correlationId", correlationId,
            "imageSize", request.getImageData().length
        ));

        try {
            ImageData imageData = ImageData.fromBytes(request.getImageData());
            VisionResult result = executeCapabilityDetection(imageData, DetectionType.OBJECT);

            DetectionResponse response = DetectionResponse.builder()
                .correlationId(correlationId)
                .detectionType(DetectionType.OBJECT.getCode())
                .detectionCount(result.detectionCount())
                .averageConfidence(result.averageConfidence())
                .processingTimeMs(result.processingTimeMs())
                .detections(result.detections())
                .build();

            logger.info("Async object detection completed successfully", Map.of(
                "correlationId", correlationId,
                "detectionCount", result.detectionCount(),
                "processingTimeMs", result.processingTimeMs()
            ));

            return CompletableFuture.completedFuture(ResponseEntity.ok(response));

        } catch (Exception e) {
            logger.error("Async object detection failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);

            DetectionResponse errorResponse = DetectionResponse.builder()
                .correlationId(correlationId)
                .detectionType(DetectionType.OBJECT.getCode())
                .error(e.getMessage())
                .build();

            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse));
        }
    }

    /**
     * Performs multiple detection types on an uploaded image file.
     *
     * <p>This endpoint accepts a multipart file upload and performs multiple
     * detection types on the image. It validates the file size and content type
     * before processing.</p>
     *
     * @param file           the uploaded image file
     * @param detectionTypes the comma-separated detection types to run
     * @return multiple detection results
     */
    @PostMapping(value = "/detect/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MultipleDetectionResponse> detectMultipleFromFile(
        @RequestParam("file") MultipartFile file,
        @RequestParam("detectionTypes") String detectionTypes) {

        String correlationId = generateCorrelationId();

        logger.info("Multiple detection request received", Map.of(
            "correlationId", correlationId,
            "fileName", file.getOriginalFilename(),
            "detectionTypes", detectionTypes
        ));

        try {
            // Validate file
            validateFile(file);

            // Parse detection types
            List<DetectionType> types = parseDetectionTypes(detectionTypes);

            // Convert to ImageData
            ImageData imageData = convertToImageData(file);

            // Perform multiple detections
            List<VisionResult> results = executeMultipleCapabilityDetections(imageData, types);

            // Create response
            MultipleDetectionResponse response = MultipleDetectionResponse.builder()
                .correlationId(correlationId)
                .detectionTypes(types.stream().map(DetectionType::getCode).toList())
                .results(results.stream()
                    .map(result -> DetectionResponse.builder()
                        .correlationId(correlationId)
                        .detectionType(result.detectionType().getCode())
                        .detectionCount(result.detectionCount())
                        .averageConfidence(result.averageConfidence())
                        .processingTimeMs(result.processingTimeMs())
                        .detections(result.detections())
                        .build())
                    .toList())
                .build();

            logger.info("Multiple detection completed successfully", Map.of(
                "correlationId", correlationId,
                "detectionTypes", types.size(),
                "totalDetections", results.stream().mapToInt(VisionResult::detectionCount).sum()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Multiple detection failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);

            MultipleDetectionResponse errorResponse = MultipleDetectionResponse.builder()
                .correlationId(correlationId)
                .error(e.getMessage())
                .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
        }
    }

    /**
     * Performs multiple detection types on image data provided in the request body.
     *
     * <p>This endpoint accepts image data as a JSON payload and performs multiple
     * detection types. The image data should be base64 encoded.</p>
     *
     * @param request the multiple detection request
     * @return multiple detection results
     */
    @PostMapping(value = "/detect/multiple", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MultipleDetectionResponse> detectMultipleFromData(
        @RequestBody MultipleDetectionRequest request) {

        String correlationId = generateCorrelationId();

        logger.info("Multiple detection request received", Map.of(
            "correlationId", correlationId,
            "imageSize", request.getImageData().length,
            "detectionTypes", request.getDetectionTypes()
        ));

        try {
            // Convert to ImageData
            ImageData imageData = ImageData.fromBytes(request.getImageData());

            // Parse detection types
            List<DetectionType> types = parseDetectionTypesFromList(request.getDetectionTypes());

            // Perform multiple detections
            List<VisionResult> results = executeMultipleCapabilityDetections(imageData, types);

            // Create response
            MultipleDetectionResponse response = MultipleDetectionResponse.builder()
                .correlationId(correlationId)
                .detectionTypes(request.getDetectionTypes())
                .results(results.stream()
                    .map(result -> DetectionResponse.builder()
                        .correlationId(correlationId)
                        .detectionType(result.detectionType().getCode())
                        .detectionCount(result.detectionCount())
                        .averageConfidence(result.averageConfidence())
                        .processingTimeMs(result.processingTimeMs())
                        .detections(result.detections())
                        .build())
                    .toList())
                .build();

            logger.info("Multiple detection completed successfully", Map.of(
                "correlationId", correlationId,
                "detectionTypes", types.size(),
                "totalDetections", results.stream().mapToInt(VisionResult::detectionCount).sum()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Multiple detection failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);

            MultipleDetectionResponse errorResponse = MultipleDetectionResponse.builder()
                .correlationId(correlationId)
                .error(e.getMessage())
                .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
        }
    }

    /**
     * Asynchronously performs multiple detection types on an uploaded image file.
     * @param file The uploaded image file.
     * @param detectionTypes The comma-separated list of detection types.
     * @return A CompletableFuture with the response.
     */
    @PostMapping(value = "/async/detect/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Async("visionAsyncExecutor")
    public CompletableFuture<ResponseEntity<MultipleDetectionResponse>> detectMultipleFromFileAsync(
        @RequestParam("file") MultipartFile file,
        @RequestParam("detectionTypes") String detectionTypes) {

        String correlationId = generateCorrelationId();

        logger.info("Async multiple detection request received", Map.of(
            "correlationId", correlationId,
            "fileName", file.getOriginalFilename(),
            "detectionTypes", detectionTypes
        ));

        try {
            validateFile(file);
            List<DetectionType> types = parseDetectionTypes(detectionTypes);
            ImageData imageData = convertToImageData(file);
            List<VisionResult> results = executeMultipleCapabilityDetections(imageData, types);

            MultipleDetectionResponse response = MultipleDetectionResponse.builder()
                .correlationId(correlationId)
                .detectionTypes(types.stream().map(DetectionType::getCode).toList())
                .results(results.stream()
                    .map(result -> DetectionResponse.builder()
                        .correlationId(correlationId)
                        .detectionType(result.detectionType().getCode())
                        .detectionCount(result.detectionCount())
                        .averageConfidence(result.averageConfidence())
                        .processingTimeMs(result.processingTimeMs())
                        .detections(result.detections())
                        .build())
                    .toList())
                .build();

            logger.info("Async multiple detection completed successfully", Map.of(
                "correlationId", correlationId,
                "detectionTypes", types.size(),
                "totalDetections", results.stream().mapToInt(VisionResult::detectionCount).sum()
            ));

            return CompletableFuture.completedFuture(ResponseEntity.ok(response));

        } catch (Exception e) {
            logger.error("Async multiple detection failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);

            MultipleDetectionResponse errorResponse = MultipleDetectionResponse.builder()
                .correlationId(correlationId)
                .error(e.getMessage())
                .build();

            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse));
        }
    }

    /**
     * Asynchronously performs multiple detection types from a JSON request.
     * @param request The multiple detection request.
     * @return A CompletableFuture with the response.
     */
    @PostMapping(value = "/async/detect/multiple", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Async("visionAsyncExecutor")
    public CompletableFuture<ResponseEntity<MultipleDetectionResponse>> detectMultipleFromDataAsync(
        @RequestBody MultipleDetectionRequest request) {

        String correlationId = generateCorrelationId();

        logger.info("Async multiple detection request received", Map.of(
            "correlationId", correlationId,
            "imageSize", request.getImageData().length,
            "detectionTypes", request.getDetectionTypes()
        ));

        try {
            ImageData imageData = ImageData.fromBytes(request.getImageData());
            List<DetectionType> types = parseDetectionTypesFromList(request.getDetectionTypes());
            List<VisionResult> results = executeMultipleCapabilityDetections(imageData, types);

            MultipleDetectionResponse response = MultipleDetectionResponse.builder()
                .correlationId(correlationId)
                .detectionTypes(request.getDetectionTypes())
                .results(results.stream()
                    .map(result -> DetectionResponse.builder()
                        .correlationId(correlationId)
                        .detectionType(result.detectionType().getCode())
                        .detectionCount(result.detectionCount())
                        .averageConfidence(result.averageConfidence())
                        .processingTimeMs(result.processingTimeMs())
                        .detections(result.detections())
                        .build())
                    .toList())
                .build();

            logger.info("Async multiple detection completed successfully", Map.of(
                "correlationId", correlationId,
                "detectionTypes", types.size(),
                "totalDetections", results.stream().mapToInt(VisionResult::detectionCount).sum()
            ));

            return CompletableFuture.completedFuture(ResponseEntity.ok(response));

        } catch (Exception e) {
            logger.error("Async multiple detection failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);

            MultipleDetectionResponse errorResponse = MultipleDetectionResponse.builder()
                .correlationId(correlationId)
                .error(e.getMessage())
                .build();

            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse));
        }
    }

    /**
     * Submits a face detection task.
     * @param file The uploaded image file.
     * @return A response entity with the task submission response.
     */
    @PostMapping(value = "/tasks/detect/faces", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TaskSubmissionResponse> submitFaceDetectionTask(@RequestParam("file") MultipartFile file) {
        String correlationId = generateCorrelationId();

        logger.info("Submit face detection task", Map.of(
            "correlationId", correlationId,
            "fileName", file.getOriginalFilename(),
            "fileSize", file.getSize(),
            "contentType", file.getContentType()
        ));

        try {
            validateFile(file);
            ImageData imageData = convertToImageData(file);

            var handle = asyncVisionProcessor.processAsyncWithHandle(
                imageData,
                DetectionType.FACE,
                Map.of("correlationId", correlationId),
                null
            );

            return ResponseEntity.accepted().body(new TaskSubmissionResponse(correlationId, handle.taskId(), "accepted"));
        } catch (Exception e) {
            logger.error("Submit face detection task failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new TaskSubmissionResponse(correlationId, null, e.getMessage()));
        }
    }

    /**
     * Submits a face detection task from a JSON request.
     * @param request The detection request.
     * @return A response entity with the task submission response.
     */
    @PostMapping(value = "/tasks/detect/faces", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TaskSubmissionResponse> submitFaceDetectionTaskJson(@RequestBody DetectionRequest request) {
        String correlationId = generateCorrelationId();

        logger.info("Submit face detection task (json)", Map.of(
            "correlationId", correlationId,
            "imageSize", request.getImageData() != null ? request.getImageData().length : 0
        ));

        try {
            ImageData imageData = ImageData.fromBytes(request.getImageData());

            var handle = asyncVisionProcessor.processAsyncWithHandle(
                imageData,
                DetectionType.FACE,
                Map.of("correlationId", correlationId),
                null
            );

            return ResponseEntity.accepted().body(new TaskSubmissionResponse(correlationId, handle.taskId(), "accepted"));
        } catch (Exception e) {
            logger.error("Submit face detection task failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new TaskSubmissionResponse(correlationId, null, e.getMessage()));
        }
    }

    /**
     * Submits a barcode detection task.
     * @param file The uploaded image file.
     * @return A response entity with the task submission response.
     */
    @PostMapping(value = "/tasks/detect/barcodes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TaskSubmissionResponse> submitBarcodeDetectionTask(@RequestParam("file") MultipartFile file) {
        String correlationId = generateCorrelationId();
        logger.info("Submit barcode detection task", Map.of(
            "correlationId", correlationId,
            "fileName", file.getOriginalFilename(),
            "fileSize", file.getSize(),
            "contentType", file.getContentType()
        ));

        try {
            validateFile(file);
            ImageData imageData = convertToImageData(file);

            var handle = asyncVisionProcessor.processAsyncWithHandle(
                imageData,
                DetectionType.BARCODE,
                Map.of("correlationId", correlationId),
                null
            );

            return ResponseEntity.accepted().body(new TaskSubmissionResponse(correlationId, handle.taskId(), "accepted"));
        } catch (Exception e) {
            logger.error("Submit barcode detection task failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new TaskSubmissionResponse(correlationId, null, e.getMessage()));
        }
    }

    /**
     * Submits a barcode detection task from a JSON request.
     * @param request The detection request.
     * @return A response entity with the task submission response.
     */
    @PostMapping(value = "/tasks/detect/barcodes", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TaskSubmissionResponse> submitBarcodeDetectionTaskJson(@RequestBody DetectionRequest request) {
        String correlationId = generateCorrelationId();
        logger.info("Submit barcode detection task (json)", Map.of(
            "correlationId", correlationId,
            "imageSize", request.getImageData() != null ? request.getImageData().length : 0
        ));

        try {
            ImageData imageData = ImageData.fromBytes(request.getImageData());

            var handle = asyncVisionProcessor.processAsyncWithHandle(
                imageData,
                DetectionType.BARCODE,
                Map.of("correlationId", correlationId),
                null
            );

            return ResponseEntity.accepted().body(new TaskSubmissionResponse(correlationId, handle.taskId(), "accepted"));
        } catch (Exception e) {
            logger.error("Submit barcode detection task failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new TaskSubmissionResponse(correlationId, null, e.getMessage()));
        }
    }

    /**
     * Submits a text detection task.
     * @param file The uploaded image file.
     * @return A response entity with the task submission response.
     */
    @PostMapping(value = "/tasks/detect/text", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TaskSubmissionResponse> submitTextDetectionTask(@RequestParam("file") MultipartFile file) {
        String correlationId = generateCorrelationId();
        logger.info("Submit text detection task", Map.of(
            "correlationId", correlationId,
            "fileName", file.getOriginalFilename(),
            "fileSize", file.getSize(),
            "contentType", file.getContentType()
        ));

        try {
            validateFile(file);
            ImageData imageData = convertToImageData(file);

            var handle = asyncVisionProcessor.processAsyncWithHandle(
                imageData,
                DetectionType.TEXT,
                Map.of("correlationId", correlationId),
                null
            );

            return ResponseEntity.accepted().body(new TaskSubmissionResponse(correlationId, handle.taskId(), "accepted"));
        } catch (Exception e) {
            logger.error("Submit text detection task failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new TaskSubmissionResponse(correlationId, null, e.getMessage()));
        }
    }

    /**
     * Submits a text detection task from a JSON request.
     * @param request The detection request.
     * @return A response entity with the task submission response.
     */
    @PostMapping(value = "/tasks/detect/text", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TaskSubmissionResponse> submitTextDetectionTaskJson(@RequestBody DetectionRequest request) {
        String correlationId = generateCorrelationId();
        logger.info("Submit text detection task (json)", Map.of(
            "correlationId", correlationId,
            "imageSize", request.getImageData() != null ? request.getImageData().length : 0
        ));

        try {
            ImageData imageData = ImageData.fromBytes(request.getImageData());

            var handle = asyncVisionProcessor.processAsyncWithHandle(
                imageData,
                DetectionType.TEXT,
                Map.of("correlationId", correlationId),
                null
            );

            return ResponseEntity.accepted().body(new TaskSubmissionResponse(correlationId, handle.taskId(), "accepted"));
        } catch (Exception e) {
            logger.error("Submit text detection task failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new TaskSubmissionResponse(correlationId, null, e.getMessage()));
        }
    }

    /**
     * Submits an annotation task.
     * @param file The uploaded image file.
     * @param action The annotation action.
     * @param label The label for the annotation.
     * @param categoriesCsv The comma-separated list of categories.
     * @return A response entity with the task submission response.
     */
    @PostMapping(value = "/tasks/annotate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TaskSubmissionResponse> submitAnnotateTask(
        @RequestParam("file") MultipartFile file,
        @RequestParam("action") String action,
        @RequestParam(value = "label", required = false) String label,
        @RequestParam(value = "categories", defaultValue = "FACE") String categoriesCsv) {

        String correlationId = generateCorrelationId();
        logger.info("Submit annotate task", Map.of(
            "correlationId", correlationId,
            "fileName", file.getOriginalFilename(),
            "action", action,
            "label", label,
            "categories", categoriesCsv
        ));

        try {
            validateFile(file);

            io.github.codesapienbe.springvision.core.AnnotationRequest.Action annotationAction = io.github.codesapienbe.springvision.core.AnnotationRequest.Action.valueOf(action.toUpperCase());
            java.util.Set<io.github.codesapienbe.springvision.core.DetectionCategory> cats = java.util.Arrays.stream(categoriesCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(io.github.codesapienbe.springvision.core.DetectionCategory::valueOf)
                .collect(java.util.stream.Collectors.toSet());

            io.github.codesapienbe.springvision.core.AnnotationRequest req = new io.github.codesapienbe.springvision.core.AnnotationRequest.Builder()
                .action(annotationAction)
                .categories(cats)
                .label(label)
                .build();

            ImageData imageData = convertToImageData(file);

            var handle = asyncVisionProcessor.processAsyncWithHandle(
                imageData,
                DetectionType.CUSTOM,
                Map.of("correlationId", correlationId, "annotationRequest", req),
                null
            );

            return ResponseEntity.accepted().body(new TaskSubmissionResponse(correlationId, handle.taskId(), "accepted"));
        } catch (Exception e) {
            logger.error("Submit annotate task failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new TaskSubmissionResponse(correlationId, null, e.getMessage()));
        }
    }

    /**
     * Gets the health status of the vision backend.
     *
     * <p>This endpoint provides health information about the vision backend
     * including status, response time, and supported detection types.</p>
     *
     * @return health status information
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> getHealth() {
        String correlationId = generateCorrelationId();

        logger.debug("Health check request received", Map.of(
            "correlationId", correlationId
        ));

        try {
            // Get health information
            var healthInfo = visionTemplate.getBackendHealthInfo();

            // Create response
            HealthResponse response = HealthResponse.builder()
                .correlationId(correlationId)
                .backendId(healthInfo.backendId())
                .backendName(visionTemplate.getBackendDisplayName())
                .backendVersion(visionTemplate.getBackendVersion())
                .status(healthInfo.status().toString())
                .statusMessage(healthInfo.statusMessage())
                .responseTimeMs(healthInfo.responseTimeMs())
                .supportedDetectionTypes(visionTemplate.getSupportedDetectionTypes().stream()
                    .map(DetectionType::getCode)
                    .toList())
                .build();

            logger.debug("Health check completed", Map.of(
                "correlationId", correlationId,
                "status", healthInfo.status().toString(),
                "responseTimeMs", healthInfo.responseTimeMs()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Health check failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);

            HealthResponse errorResponse = HealthResponse.builder()
                .correlationId(correlationId)
                .status("UNKNOWN")
                .statusMessage("Health check failed: " + e.getMessage())
                .error(e.getMessage())
                .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
        }
    }

    /**
     * Gets information about the vision backend.
     *
     * <p>This endpoint provides general information about the vision backend
     * including capabilities and configuration.</p>
     *
     * @return backend information
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getInfo() {
        String correlationId = generateCorrelationId();

        logger.debug("Info request received", Map.of(
            "correlationId", correlationId
        ));

        try {
            Map<String, Object> info = Map.of(
                "correlationId", correlationId,
                "backendId", visionTemplate.getBackendId(),
                "backendName", visionTemplate.getBackendDisplayName(),
                "backendVersion", visionTemplate.getBackendVersion(),
                "supportedDetectionTypes", visionTemplate.getSupportedDetectionTypes().stream()
                    .map(DetectionType::getCode)
                    .toList(),
                "isHealthy", visionTemplate.isBackendHealthy()
            );

            logger.debug("Info request completed", Map.of(
                "correlationId", correlationId
            ));

            return ResponseEntity.ok(info);

        } catch (Exception e) {
            logger.error("Info request failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);

            Map<String, Object> errorInfo = Map.of(
                "correlationId", correlationId,
                "error", e.getMessage()
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorInfo);
        }
    }

    /**
     * Validates an uploaded file.
     *
     * @param file the file to validate
     * @throws IllegalArgumentException if the file is invalid
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum limit of " + MAX_FILE_SIZE + " bytes");
        }

        String contentType = file.getContentType();
        if (contentType == null || !SUPPORTED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported content type: " + contentType);
        }
    }

    /**
     * Converts a multipart file to ImageData.
     *
     * @param file the multipart file
     * @return the ImageData
     * @throws IOException if the file cannot be read
     */
    private ImageData convertToImageData(MultipartFile file) throws IOException {
        byte[] imageBytes = file.getBytes();
        return ImageData.fromBytes(imageBytes);
    }

    /**
     * Parses detection types from a comma-separated string.
     *
     * @param detectionTypes the comma-separated detection types
     * @return the list of detection types
     * @throws IllegalArgumentException if the detection types are invalid
     */
    private List<DetectionType> parseDetectionTypes(String detectionTypes) {
        if (detectionTypes == null || detectionTypes.trim().isEmpty()) {
            throw new IllegalArgumentException("Detection types cannot be empty");
        }

        return List.of(detectionTypes.split(","))
            .stream()
            .map(String::trim)
            .map(type -> {
                try {
                    return DetectionType.fromCode(type);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid detection type: " + type);
                }
            })
            .toList();
    }

    /**
     * Parses detection types from a list of strings into a list of DetectionType.
     *
     * @param detectionTypes the list of detection type strings
     * @return the list of DetectionType
     * @throws IllegalArgumentException if the detection types are invalid
     */
    private List<DetectionType> parseDetectionTypesFromList(List<String> detectionTypes) {
        if (detectionTypes == null || detectionTypes.isEmpty()) {
            throw new IllegalArgumentException("Detection types cannot be empty");
        }

        return detectionTypes.stream()
            .map(String::trim)
            .map(type -> {
                try {
                    return DetectionType.fromCode(type);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid detection type: " + type);
                }
            })
            .toList();
    }

    /**
     * Generates a unique correlation ID for request tracking.
     *
     * @return a unique correlation ID
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    // --- Advanced detection types: barcodes and text ---

    /**
     * Detects barcodes in an uploaded image file.
     * @param file The uploaded image file.
     * @return A response entity with the detection response.
     */
    @PostMapping(value = "/detect/barcodes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DetectionResponse> detectBarcodesFromFile(@RequestParam("file") MultipartFile file) {
        String correlationId = generateCorrelationId();
        logger.info("Barcode detection request received", Map.of(
            "correlationId", correlationId,
            "fileName", file.getOriginalFilename(),
            "fileSize", file.getSize(),
            "contentType", file.getContentType()
        ));

        try {
            validateFile(file);
            ImageData imageData = convertToImageData(file);
            VisionResult result = executeCapabilityDetection(imageData, io.github.codesapienbe.springvision.core.DetectionType.BARCODE);

            DetectionResponse response = DetectionResponse.builder()
                .correlationId(correlationId)
                .detectionType(io.github.codesapienbe.springvision.core.DetectionType.BARCODE.getCode())
                .detectionCount(result.detectionCount())
                .averageConfidence(result.averageConfidence())
                .processingTimeMs(result.processingTimeMs())
                .detections(result.detections())
                .build();

            logger.info("Barcode detection completed successfully", Map.of(
                "correlationId", correlationId,
                "detectionCount", result.detectionCount(),
                "processingTimeMs", result.processingTimeMs()
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Barcode detection failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);

            DetectionResponse errorResponse = DetectionResponse.builder()
                .correlationId(correlationId)
                .detectionType(io.github.codesapienbe.springvision.core.DetectionType.BARCODE.getCode())
                .error(e.getMessage())
                .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Detects barcodes in image data provided in the request body.
     * @param request The detection request.
     * @return A response entity with the detection response.
     */
    @PostMapping(value = "/detect/barcodes", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DetectionResponse> detectBarcodesFromData(@RequestBody DetectionRequest request) {
        String correlationId = generateCorrelationId();
        logger.info("Barcode detection request received", Map.of(
            "correlationId", correlationId,
            "imageSize", request.getImageData() != null ? request.getImageData().length : 0
        ));

        try {
            ImageData imageData = ImageData.fromBytes(request.getImageData());
            VisionResult result = executeCapabilityDetection(imageData, io.github.codesapienbe.springvision.core.DetectionType.BARCODE);

            DetectionResponse response = DetectionResponse.builder()
                .correlationId(correlationId)
                .detectionType(io.github.codesapienbe.springvision.core.DetectionType.BARCODE.getCode())
                .detectionCount(result.detectionCount())
                .averageConfidence(result.averageConfidence())
                .processingTimeMs(result.processingTimeMs())
                .detections(result.detections())
                .build();

            logger.info("Barcode detection completed successfully", Map.of(
                "correlationId", correlationId,
                "detectionCount", result.detectionCount(),
                "processingTimeMs", result.processingTimeMs()
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Barcode detection failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);

            DetectionResponse errorResponse = DetectionResponse.builder()
                .correlationId(correlationId)
                .detectionType(io.github.codesapienbe.springvision.core.DetectionType.BARCODE.getCode())
                .error(e.getMessage())
                .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Detects text in an uploaded image file.
     * @param file The uploaded image file.
     * @return A response entity with the detection response.
     */
    @PostMapping(value = "/detect/text", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DetectionResponse> detectTextFromFile(@RequestParam("file") MultipartFile file) {
        String correlationId = generateCorrelationId();
        logger.info("Text detection request received", Map.of(
            "correlationId", correlationId,
            "fileName", file.getOriginalFilename(),
            "fileSize", file.getSize(),
            "contentType", file.getContentType()
        ));

        try {
            validateFile(file);
            ImageData imageData = convertToImageData(file);
            VisionResult result = executeCapabilityDetection(imageData, io.github.codesapienbe.springvision.core.DetectionType.TEXT);

            DetectionResponse response = DetectionResponse.builder()
                .correlationId(correlationId)
                .detectionType(io.github.codesapienbe.springvision.core.DetectionType.TEXT.getCode())
                .detectionCount(result.detectionCount())
                .averageConfidence(result.averageConfidence())
                .processingTimeMs(result.processingTimeMs())
                .detections(result.detections())
                .build();

            logger.info("Text detection completed successfully", Map.of(
                "correlationId", correlationId,
                "detectionCount", result.detectionCount(),
                "processingTimeMs", result.processingTimeMs()
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Text detection failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);

            DetectionResponse errorResponse = DetectionResponse.builder()
                .correlationId(correlationId)
                .detectionType(io.github.codesapienbe.springvision.core.DetectionType.TEXT.getCode())
                .error(e.getMessage())
                .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Detects text in image data provided in the request body.
     * @param request The detection request.
     * @return A response entity with the detection response.
     */
    @PostMapping(value = "/detect/text", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DetectionResponse> detectTextFromData(@RequestBody DetectionRequest request) {
        String correlationId = generateCorrelationId();
        logger.info("Text detection request received", Map.of(
            "correlationId", correlationId,
            "imageSize", request.getImageData() != null ? request.getImageData().length : 0
        ));

        try {
            ImageData imageData = ImageData.fromBytes(request.getImageData());
            VisionResult result = executeCapabilityDetection(imageData, io.github.codesapienbe.springvision.core.DetectionType.TEXT);

            DetectionResponse response = DetectionResponse.builder()
                .correlationId(correlationId)
                .detectionType(io.github.codesapienbe.springvision.core.DetectionType.TEXT.getCode())
                .detectionCount(result.detectionCount())
                .averageConfidence(result.averageConfidence())
                .processingTimeMs(result.processingTimeMs())
                .detections(result.detections())
                .build();

            logger.info("Text detection completed successfully", Map.of(
                "correlationId", correlationId,
                "detectionCount", result.detectionCount(),
                "processingTimeMs", result.processingTimeMs()
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Text detection failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);

            DetectionResponse errorResponse = DetectionResponse.builder()
                .correlationId(correlationId)
                .detectionType(io.github.codesapienbe.springvision.core.DetectionType.TEXT.getCode())
                .error(e.getMessage())
                .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Annotates an image.
     * @param file The uploaded image file.
     * @param action The annotation action.
     * @param label The label for the annotation.
     * @param categoriesCsv The comma-separated list of categories.
     * @return A response entity with the task submission response.
     */
    @PostMapping(value = "/annotate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TaskSubmissionResponse> annotateImageEndpoint(
        @RequestParam("file") MultipartFile file,
        @RequestParam("action") String action,
        @RequestParam(value = "label", required = false) String label,
        @RequestParam(value = "categories", defaultValue = "FACE") String categoriesCsv) {

        String correlationId = generateCorrelationId();
        logger.info("Submit annotate task", Map.of(
            "correlationId", correlationId,
            "fileName", file.getOriginalFilename(),
            "action", action,
            "label", label,
            "categories", categoriesCsv
        ));

        try {
            validateFile(file);

            io.github.codesapienbe.springvision.core.AnnotationRequest.Action annotationAction = io.github.codesapienbe.springvision.core.AnnotationRequest.Action.valueOf(action.toUpperCase());
            java.util.Set<io.github.codesapienbe.springvision.core.DetectionCategory> cats = java.util.Arrays.stream(categoriesCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(io.github.codesapienbe.springvision.core.DetectionCategory::valueOf)
                .collect(java.util.stream.Collectors.toSet());

            io.github.codesapienbe.springvision.core.AnnotationRequest req = new io.github.codesapienbe.springvision.core.AnnotationRequest.Builder()
                .action(annotationAction)
                .categories(cats)
                .label(label)
                .build();

            ImageData imageData = convertToImageData(file);

            var handle = asyncVisionProcessor.processAsyncWithHandle(
                imageData,
                DetectionType.CUSTOM,
                Map.of("correlationId", correlationId, "annotationRequest", req),
                null
            );

            return ResponseEntity.accepted().body(new TaskSubmissionResponse(correlationId, handle.taskId(), "accepted"));
        } catch (Exception e) {
            logger.error("Submit annotate task failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new TaskSubmissionResponse(correlationId, null, e.getMessage()));
        }
    }

    /**
     * Detects objects in an image using a query.
     * @param file The uploaded image file.
     * @param detectionType The type of detection to perform.
     * @param minConfidence The minimum confidence threshold.
     * @param maxDetections The maximum number of detections.
     * @return A response entity with the detection response.
     */
    @PostMapping(value = "/detect/query", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DetectionResponse> detectWithQueryEndpoint(
        @RequestParam("file") MultipartFile file,
        @RequestParam("detectionType") String detectionType,
        @RequestParam(value = "minConfidence", defaultValue = "0.5") double minConfidence,
        @RequestParam(value = "maxDetections", defaultValue = "100") int maxDetections) {

        String correlationId = generateCorrelationId();
        logger.info("Detect with query request received", Map.of(
            "correlationId", correlationId,
            "fileName", file.getOriginalFilename(),
            "detectionType", detectionType,
            "minConfidence", minConfidence,
            "maxDetections", maxDetections
        ));

        try {
            io.github.codesapienbe.springvision.core.DetectionType type = io.github.codesapienbe.springvision.core.DetectionType.fromCode(detectionType);
            if (type == null) {
                return ResponseEntity.badRequest().body(DetectionResponse.builder().correlationId(correlationId).error("Invalid detection type: " + detectionType).build());
            }

            io.github.codesapienbe.springvision.core.DetectionQuery.Builder qb = new io.github.codesapienbe.springvision.core.DetectionQuery.Builder()
                .type(type)
                .minConfidence(minConfidence)
                .maxDetections(maxDetections);

            io.github.codesapienbe.springvision.core.DetectionQuery query = qb.build();
            ImageData imageData = ImageData.fromBytes(file.getBytes());
            // Note: Query parameters (minConfidence, maxDetections) are not directly supported by capabilities
            // They should be applied as filters after detection
            VisionResult result = executeCapabilityDetection(imageData, type);

            DetectionResponse response = DetectionResponse.builder()
                .correlationId(correlationId)
                .detectionType(type.getCode())
                .detectionCount(result.detectionCount())
                .averageConfidence(result.averageConfidence())
                .processingTimeMs(result.processingTimeMs())
                .detections(result.detections())
                .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Detect with query failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(DetectionResponse.builder().correlationId(correlationId).error(e.getMessage()).build());
        }
    }

    // --- Async variants for advanced detection types and annotate ---

    /**
     * Asynchronously detects barcodes in an uploaded image file.
     * @param file The uploaded image file.
     * @return A CompletableFuture with the response.
     */
    @PostMapping(value = "/async/detect/barcodes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Async("visionAsyncExecutor")
    public CompletableFuture<ResponseEntity<DetectionResponse>> detectBarcodesFromFileAsync(@RequestParam("file") MultipartFile file) {
        String correlationId = generateCorrelationId();
        logger.info("Async barcode detection request received", Map.of(
            "correlationId", correlationId,
            "fileName", file.getOriginalFilename(),
            "fileSize", file.getSize(),
            "contentType", file.getContentType()
        ));

        try {
            validateFile(file);
            ImageData imageData = convertToImageData(file);
            return CompletableFuture.supplyAsync(() -> {
                VisionResult result = executeCapabilityDetection(imageData, io.github.codesapienbe.springvision.core.DetectionType.BARCODE);
                DetectionResponse response = DetectionResponse.builder()
                    .correlationId(correlationId)
                    .detectionType(io.github.codesapienbe.springvision.core.DetectionType.BARCODE.getCode())
                    .detectionCount(result.detectionCount())
                    .averageConfidence(result.averageConfidence())
                    .processingTimeMs(result.processingTimeMs())
                    .detections(result.detections())
                    .build();
                logger.info("Async barcode detection completed successfully", Map.of(
                    "correlationId", correlationId,
                    "detectionCount", result.detectionCount(),
                    "processingTimeMs", result.processingTimeMs()
                ));
                return ResponseEntity.ok(response);
            });
        } catch (Exception e) {
            logger.error("Async barcode detection failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);
            DetectionResponse errorResponse = DetectionResponse.builder()
                .correlationId(correlationId)
                .detectionType(io.github.codesapienbe.springvision.core.DetectionType.BARCODE.getCode())
                .error(e.getMessage())
                .build();
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
        }
    }

    /**
     * Asynchronously detects barcodes in image data provided in the request body.
     * @param request The detection request.
     * @return A CompletableFuture with the response.
     */
    @PostMapping(value = "/async/detect/barcodes", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Async("visionAsyncExecutor")
    public CompletableFuture<ResponseEntity<DetectionResponse>> detectBarcodesFromDataAsync(@RequestBody DetectionRequest request) {
        String correlationId = generateCorrelationId();
        logger.info("Async barcode detection request received", Map.of(
            "correlationId", correlationId,
            "imageSize", request.getImageData() != null ? request.getImageData().length : 0
        ));

        try {
            ImageData imageData = ImageData.fromBytes(request.getImageData());
            return CompletableFuture.supplyAsync(() -> {
                VisionResult result = executeCapabilityDetection(imageData, io.github.codesapienbe.springvision.core.DetectionType.BARCODE);
                DetectionResponse response = DetectionResponse.builder()
                    .correlationId(correlationId)
                    .detectionType(io.github.codesapienbe.springvision.core.DetectionType.BARCODE.getCode())
                    .detectionCount(result.detectionCount())
                    .averageConfidence(result.averageConfidence())
                    .processingTimeMs(result.processingTimeMs())
                    .detections(result.detections())
                    .build();
                logger.info("Async barcode detection completed successfully", Map.of(
                    "correlationId", correlationId,
                    "detectionCount", result.detectionCount(),
                    "processingTimeMs", result.processingTimeMs()
                ));
                return ResponseEntity.ok(response);
            });
        } catch (Exception e) {
            logger.error("Async barcode detection failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);
            DetectionResponse errorResponse = DetectionResponse.builder()
                .correlationId(correlationId)
                .detectionType(io.github.codesapienbe.springvision.core.DetectionType.BARCODE.getCode())
                .error(e.getMessage())
                .build();
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
        }
    }

    /**
     * Asynchronously detects text in an uploaded image file.
     * @param file The uploaded image file.
     * @return A CompletableFuture with the response.
     */
    @PostMapping(value = "/async/detect/text", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Async("visionAsyncExecutor")
    public CompletableFuture<ResponseEntity<DetectionResponse>> detectTextFromFileAsync(@RequestParam("file") MultipartFile file) {
        String correlationId = generateCorrelationId();
        logger.info("Async text detection request received", Map.of(
            "correlationId", correlationId,
            "fileName", file.getOriginalFilename(),
            "fileSize", file.getSize(),
            "contentType", file.getContentType()
        ));

        try {
            validateFile(file);
            ImageData imageData = convertToImageData(file);
            return CompletableFuture.supplyAsync(() -> {
                VisionResult result = executeCapabilityDetection(imageData, io.github.codesapienbe.springvision.core.DetectionType.TEXT);
                DetectionResponse response = DetectionResponse.builder()
                    .correlationId(correlationId)
                    .detectionType(io.github.codesapienbe.springvision.core.DetectionType.TEXT.getCode())
                    .detectionCount(result.detectionCount())
                    .averageConfidence(result.averageConfidence())
                    .processingTimeMs(result.processingTimeMs())
                    .detections(result.detections())
                    .build();
                logger.info("Async text detection completed successfully", Map.of(
                    "correlationId", correlationId,
                    "detectionCount", result.detectionCount(),
                    "processingTimeMs", result.processingTimeMs()
                ));
                return ResponseEntity.ok(response);
            });
        } catch (Exception e) {
            logger.error("Async text detection failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);
            DetectionResponse errorResponse = DetectionResponse.builder()
                .correlationId(correlationId)
                .detectionType(io.github.codesapienbe.springvision.core.DetectionType.TEXT.getCode())
                .error(e.getMessage())
                .build();
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
        }
    }

    /**
     * Asynchronously detects text in image data provided in the request body.
     * @param request The detection request.
     * @return A CompletableFuture with the response.
     */
    @PostMapping(value = "/async/detect/text", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Async("visionAsyncExecutor")
    public CompletableFuture<ResponseEntity<DetectionResponse>> detectTextFromDataAsync(@RequestBody DetectionRequest request) {
        String correlationId = generateCorrelationId();
        logger.info("Async text detection request received", Map.of(
            "correlationId", correlationId,
            "imageSize", request.getImageData() != null ? request.getImageData().length : 0
        ));

        try {
            ImageData imageData = ImageData.fromBytes(request.getImageData());
            return CompletableFuture.supplyAsync(() -> {
                VisionResult result = executeCapabilityDetection(imageData, io.github.codesapienbe.springvision.core.DetectionType.TEXT);
                DetectionResponse response = DetectionResponse.builder()
                    .correlationId(correlationId)
                    .detectionType(io.github.codesapienbe.springvision.core.DetectionType.TEXT.getCode())
                    .detectionCount(result.detectionCount())
                    .averageConfidence(result.averageConfidence())
                    .processingTimeMs(result.processingTimeMs())
                    .detections(result.detections())
                    .build();
                logger.info("Async text detection completed successfully", Map.of(
                    "correlationId", correlationId,
                    "detectionCount", result.detectionCount(),
                    "processingTimeMs", result.processingTimeMs()
                ));
                return ResponseEntity.ok(response);
            });
        } catch (Exception e) {
            logger.error("Async text detection failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);
            DetectionResponse errorResponse = DetectionResponse.builder()
                .correlationId(correlationId)
                .detectionType(io.github.codesapienbe.springvision.core.DetectionType.TEXT.getCode())
                .error(e.getMessage())
                .build();
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
        }
    }

    /**
     * Asynchronously annotates an image.
     * @param file The uploaded image file.
     * @param action The annotation action.
     * @param label The label for the annotation.
     * @param categoriesCsv The comma-separated list of categories.
     * @return A CompletableFuture with the response.
     */
    @PostMapping(value = "/async/annotate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Async("visionAsyncExecutor")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> annotateImageAsync(
        @RequestParam("file") MultipartFile file,
        @RequestParam("action") String action,
        @RequestParam(value = "label", required = false) String label,
        @RequestParam(value = "categories", defaultValue = "FACE") String categoriesCsv) {

        String correlationId = generateCorrelationId();
        logger.info("Async annotate request received", Map.of(
            "correlationId", correlationId,
            "fileName", file.getOriginalFilename(),
            "action", action,
            "label", label,
            "categories", categoriesCsv
        ));

        try {
            io.github.codesapienbe.springvision.core.AnnotationRequest.Action annotationAction = io.github.codesapienbe.springvision.core.AnnotationRequest.Action.valueOf(action.toUpperCase());
            java.util.Set<io.github.codesapienbe.springvision.core.DetectionCategory> cats = java.util.Arrays.stream(categoriesCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(io.github.codesapienbe.springvision.core.DetectionCategory::valueOf)
                .collect(java.util.stream.Collectors.toSet());

            io.github.codesapienbe.springvision.core.AnnotationRequest req = new io.github.codesapienbe.springvision.core.AnnotationRequest.Builder()
                .action(annotationAction)
                .categories(cats)
                .label(label)
                .build();

            validateFile(file);
            ImageData imageData = ImageData.fromBytes(file.getBytes());

            return CompletableFuture.supplyAsync(() -> {
                try {
                    visionTemplate.annotate(imageData, req);
                    Map<String, Object> payload = Map.of(
                        "action", action,
                        "categories", cats.stream().map(Enum::name).toList(),
                        "label", label,
                        "annotated", true
                    );
                    logger.info("Async annotate completed", Map.of("correlationId", correlationId));
                    return ResponseEntity.ok(payload);
                } catch (Exception ex) {
                    logger.error("Async annotate failed", Map.of("correlationId", correlationId, "error", ex.getClass().getSimpleName()), ex);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
                }
            });

        } catch (Exception e) {
            logger.error("Async annotate request preparation failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage())));
        }
    }

    // ===== Capability-Based Detection Helper Methods =====

    /**
     * Executes capability-based detection for a given DetectionType.
     * This replaces the deprecated visionTemplate.detect() pattern.
     *
     * @param imageData     the image data to process
     * @param detectionType the type of detection to perform
     * @return VisionResult containing detections
     */
    private VisionResult executeCapabilityDetection(ImageData imageData, DetectionType detectionType) {
        long startTime = System.currentTimeMillis();
        List<io.github.codesapienbe.springvision.core.Detection> detections;

        // Route to appropriate capability based on detection type
        switch (detectionType) {
            case FACE -> {
                io.github.codesapienbe.springvision.core.capabilities.FaceDetectionCapability backend =
                    (io.github.codesapienbe.springvision.core.capabilities.FaceDetectionCapability) visionTemplate.backend();
                detections = backend.detectFaces(imageData);
            }
            case OBJECT -> {
                io.github.codesapienbe.springvision.core.capabilities.ObjectDetectionCapability backend =
                    (io.github.codesapienbe.springvision.core.capabilities.ObjectDetectionCapability) visionTemplate.backend();
                detections = backend.detectObjects(imageData);
            }
            case TEXT -> {
                io.github.codesapienbe.springvision.core.capabilities.OcrCapability backend =
                    (io.github.codesapienbe.springvision.core.capabilities.OcrCapability) visionTemplate.backend();
                List<io.github.codesapienbe.springvision.core.capabilities.OcrCapability.TextDetection> textDetections = backend.extractText(imageData);
                // Convert TextDetection to Detection
                detections = textDetections.stream()
                    .map(td -> new io.github.codesapienbe.springvision.core.Detection(
                        td.text(),
                        td.confidence(),
                        td.boundingBox() != null ? convertToBox(td.boundingBox()) : null,
                        Map.of("text", td.text(), "attributes", td.attributes())
                    ))
                    .toList();
            }
            case BARCODE -> {
                io.github.codesapienbe.springvision.core.capabilities.BarcodeCapability backend =
                    (io.github.codesapienbe.springvision.core.capabilities.BarcodeCapability) visionTemplate.backend();
                detections = backend.detectBarcodes(imageData);
            }
            default -> throw new UnsupportedOperationException("Unsupported detection type: " + detectionType);
        }

        long processingTime = System.currentTimeMillis() - startTime;
        double avgConfidence = detections.isEmpty() ? 0.0 :
            detections.stream().mapToDouble(io.github.codesapienbe.springvision.core.Detection::confidence).average().orElse(0.0);

        return VisionResult.of(detectionType, detections, avgConfidence, processingTime);
    }

    /**
     * Executes multiple capability-based detections.
     *
     * @param imageData      the image data to process
     * @param detectionTypes the list of detection types to perform
     * @return list of VisionResults
     */
    private List<VisionResult> executeMultipleCapabilityDetections(ImageData imageData, List<DetectionType> detectionTypes) {
        return detectionTypes.stream()
            .map(type -> executeCapabilityDetection(imageData, type))
            .toList();
    }

    /**
     * Helper to convert OCR bounding box map to BoundingBox.
     */
    private io.github.codesapienbe.springvision.core.BoundingBox convertToBox(Map<String, Object> bboxMap) {
        try {
            double x = ((Number) bboxMap.get("x")).doubleValue();
            double y = ((Number) bboxMap.get("y")).doubleValue();
            double width = ((Number) bboxMap.get("width")).doubleValue();
            double height = ((Number) bboxMap.get("height")).doubleValue();
            return new io.github.codesapienbe.springvision.core.BoundingBox(x, y, width, height);
        } catch (Exception e) {
            return new io.github.codesapienbe.springvision.core.BoundingBox(0, 0, 0, 0);
        }
    }
}
