package io.github.codesapienbe.springvision.examples.gwtapplication.controller;

import io.github.codesapienbe.springvision.core.DetectionType;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.VisionResult;
import io.github.codesapienbe.springvision.core.VisionTemplate;
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
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

/**
 * REST controller for face detection operations in the GWT application.
 *
 * <p>This controller provides the REST API endpoint for face detection that the GWT frontend
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
 * }
 *
 * // Face detection with URL
 * POST /api/vision/detect/faces/url?imageUrl=https://...
 * }</pre>
 *
 * @author Spring Vision Team
 * @see VisionTemplate
 * @since 1.0.0
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
     * Network limits for URL downloads
     */
    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 15000;
    private static final int MAX_DOWNLOAD_BYTES = 50 * 1024 * 1024;

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
     * @return face detection results in the format expected by the GWT frontend
     */
    @PostMapping(value = "/detect/faces", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> detectFacesFromFile(
        @RequestParam("file") MultipartFile file) {

        String correlationId = generateCorrelationId();

        logger.info("Face detection request received", Map.of(
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
            io.github.codesapienbe.springvision.core.DetectionQuery query = new io.github.codesapienbe.springvision.core.DetectionQuery.Builder()
                .type(io.github.codesapienbe.springvision.core.DetectionType.FACE)
                .categories(java.util.Set.of(io.github.codesapienbe.springvision.core.DetectionCategory.FACE))
                .build();
            VisionResult result = visionTemplate.detect(imageData, query);

            // Create response in the format expected by the GWT frontend
            Map<String, Object> response = Map.of(
                "correlationId", correlationId,
                "detectionType", DetectionType.FACE.getCode(),
                "detectionCount", result.detectionCount(),
                "averageConfidence", result.averageConfidence(),
                "processingTimeMs", result.processingTimeMs(),
                "detections", result.detections()
            );

            logger.info("Face detection completed successfully", Map.of(
                "correlationId", correlationId,
                "detectionCount", result.detectionCount(),
                "processingTimeMs", result.processingTimeMs()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Face detection failed", Map.of(
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
     * Detects faces from a public image URL (in-memory download).
     */
    @PostMapping(value = "/detect/faces/url")
    public ResponseEntity<Map<String, Object>> detectFacesFromUrl(@RequestParam("imageUrl") String imageUrl) {
        String correlationId = generateCorrelationId();
        logger.info("Face detection URL request received", Map.of(
            "correlationId", correlationId,
            "imageUrl", imageUrl
        ));
        try {
            if (imageUrl == null || imageUrl.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "correlationId", correlationId,
                    "error", "imageUrl must be provided"
                ));
            }
            byte[] bytes = downloadImageBytes(imageUrl.trim());
            ImageData imageData = ImageData.fromBytes(bytes);

            io.github.codesapienbe.springvision.core.DetectionQuery query = new io.github.codesapienbe.springvision.core.DetectionQuery.Builder()
                .type(io.github.codesapienbe.springvision.core.DetectionType.FACE)
                .categories(java.util.Set.of(io.github.codesapienbe.springvision.core.DetectionCategory.FACE))
                .build();
            VisionResult result = visionTemplate.detect(imageData, query);

            Map<String, Object> response = Map.of(
                "correlationId", correlationId,
                "detectionType", DetectionType.FACE.getCode(),
                "detectionCount", result.detectionCount(),
                "averageConfidence", result.averageConfidence(),
                "processingTimeMs", result.processingTimeMs(),
                "detections", result.detections()
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body(Map.of(
                "correlationId", correlationId,
                "error", iae.getMessage()
            ));
        } catch (IOException ioe) {
            logger.error("URL download failed", Map.of("correlationId", correlationId, "error", ioe.getMessage()));
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                "correlationId", correlationId,
                "error", "Failed to download image"
            ));
        } catch (Exception e) {
            logger.error("Face detection failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "correlationId", correlationId,
                    "error", e.getMessage()
                ));
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

        logger.debug("Health check request received", Map.of(
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

    private ImageData convertToImageData(MultipartFile file) throws IOException {
        byte[] imageBytes = file.getBytes();
        return ImageData.fromBytes(imageBytes);
    }

    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    private byte[] downloadImageBytes(String urlString) throws IOException {
        if (!isHttpUrl(urlString)) {
            throw new IllegalArgumentException("Only HTTP/HTTPS URLs are supported");
        }
        URL url = URI.create(urlString).toURL();
        validatePublicHost(url);
        URLConnection conn = url.openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestProperty("User-Agent", "spring-vision-gwt-example/1.0");
        String ct = conn.getContentType();
        if (ct != null && !ct.toLowerCase().startsWith("image/")) {
            throw new IllegalArgumentException("URL does not point to an image content type");
        }
        int contentLength = conn.getContentLength();
        if (contentLength > 0 && contentLength > MAX_DOWNLOAD_BYTES) {
            throw new IllegalArgumentException("Remote content too large");
        }
        try (InputStream in = conn.getInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int read;
            int total = 0;
            while ((read = in.read(buf)) != -1) {
                total += read;
                if (total > MAX_DOWNLOAD_BYTES) {
                    throw new IllegalArgumentException("Download exceeds size limit");
                }
                baos.write(buf, 0, read);
            }
            return baos.toByteArray();
        }
    }

    private boolean isHttpUrl(String s) {
        String v = s == null ? "" : s.trim().toLowerCase();
        return v.startsWith("http://") || v.startsWith("https://");
    }

    private void validatePublicHost(URL url) throws IOException {
        String host = url.getHost();
        if (host == null || host.isBlank()) throw new IOException("Invalid URL host");
        InetAddress[] addrs;
        try {
            addrs = InetAddress.getAllByName(host);
        } catch (Exception e) {
            throw new IOException("Failed to resolve host: " + host);
        }
        for (InetAddress a : addrs) {
            if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress() || a.isSiteLocalAddress()) {
                throw new IOException("Refusing to connect to non-public address: " + a.getHostAddress());
            }
        }
    }
}
