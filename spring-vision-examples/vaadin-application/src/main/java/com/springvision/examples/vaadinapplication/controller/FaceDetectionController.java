package com.springvision.examples.vaadinapplication.controller;

import com.springvision.core.DetectionType;
import com.springvision.core.ImageData;
import com.springvision.core.VisionResult;
import com.springvision.core.VisionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for face detection operations in the Vaadin application.
 *
 * <p>This controller provides the REST API endpoint for face detection that the Vaadin frontend
 * expects. It delegates to the Spring Vision framework for actual processing.</p>
 *
 * <p>The controller includes proper error handling, validation, and structured logging
 * following enterprise standards.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Face detection with file upload
 * POST /api/vision/detect/faces
 * Content-Type: multipart/form-data
 * }</pre>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 * @see VisionTemplate
 */
@RestController
@RequestMapping("/api/vision")
@CrossOrigin(origins = "*")
public class FaceDetectionController {

    private static final Logger logger = LoggerFactory.getLogger(FaceDetectionController.class);

    /**
     * Maximum file size for uploads (50MB as configured in application.yml).
     */
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    /**
     * Supported image content types.
     */
    private static final List<String> SUPPORTED_CONTENT_TYPES = List.of(
        "image/jpeg", "image/jpg", "image/png", "image/gif", "image/bmp", "image/webp"
    );

    /**
     * The vision template for processing operations.
     */
    private final VisionTemplate visionTemplate;

    /**
     * Constructs a new face detection controller.
     *
     * @param visionTemplate the vision template for processing operations
     */
    public FaceDetectionController(VisionTemplate visionTemplate) {
        this.visionTemplate = visionTemplate;
    }

    /**
     * Detects faces in an uploaded image file.
     *
     * <p>This endpoint accepts a multipart file upload and performs face detection
     * on the image. It validates the file size and content type before processing.</p>
     *
     * @param file the uploaded image file
     * @return face detection results in the format expected by the Vaadin frontend
     */
    @PostMapping(value = "/detect/faces", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> detectFacesFromFile(
            @RequestParam("file") MultipartFile file) {

        String correlationId = generateCorrelationId();

        logger.info("Face detection request received from Vaadin", Map.of(
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

            // Perform face detection
            VisionResult result = visionTemplate.detectFaces(imageData);

            // Create response in the format expected by the Vaadin frontend
            Map<String, Object> response = Map.of(
                "correlationId", correlationId,
                "detectionType", DetectionType.FACE.getCode(),
                "detectionCount", result.detectionCount(),
                "averageConfidence", result.averageConfidence(),
                "processingTimeMs", result.processingTimeMs(),
                "detections", result.detections()
            );

            logger.info("Face detection completed successfully for Vaadin", Map.of(
                "correlationId", correlationId,
                "detectionCount", result.detectionCount(),
                "processingTimeMs", result.processingTimeMs()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Face detection failed for Vaadin", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);

            Map<String, Object> errorResponse = Map.of(
                "correlationId", correlationId,
                "detectionType", DetectionType.FACE.getCode(),
                "error", e.getMessage()
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
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
    public ResponseEntity<Map<String, Object>> getHealth() {
        String correlationId = generateCorrelationId();

        logger.debug("Health check request received from Vaadin", Map.of(
            "correlationId", correlationId
        ));

        try {
            // Get health information
            var healthInfo = visionTemplate.getBackendHealthInfo();

            // Create response
            Map<String, Object> response = Map.of(
                "correlationId", correlationId,
                "backendId", healthInfo.backendId(),
                "backendName", visionTemplate.getBackendDisplayName(),
                "backendVersion", visionTemplate.getBackendVersion(),
                "status", healthInfo.status().toString(),
                "statusMessage", healthInfo.statusMessage(),
                "responseTimeMs", healthInfo.responseTimeMs(),
                "supportedDetectionTypes", visionTemplate.getSupportedDetectionTypes().stream()
                    .map(DetectionType::getCode)
                    .toList()
            );

            logger.debug("Health check completed for Vaadin", Map.of(
                "correlationId", correlationId,
                "status", healthInfo.status().toString(),
                "responseTimeMs", healthInfo.responseTimeMs()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Health check failed for Vaadin", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);

            Map<String, Object> errorResponse = Map.of(
                "correlationId", correlationId,
                "status", "UNKNOWN",
                "statusMessage", "Health check failed: " + e.getMessage(),
                "error", e.getMessage()
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
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
            throw new IllegalArgumentException("Unsupported content type: " + contentType +
                ". Supported types: " + String.join(", ", SUPPORTED_CONTENT_TYPES));
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
     * Generates a unique correlation ID for request tracking.
     *
     * @return a unique correlation ID
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
}
