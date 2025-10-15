package io.github.codesapienbe.springvision.mcp;

import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import io.github.codesapienbe.springvision.core.VisionResult;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides a simple and secure MCP tool for counting faces in images from URLs.
 * This class wraps the {@link VisionTemplate} to offer face counting functionality.
 *
 * @see VisionTemplate
 * @see Tool
 */
@Component
public class VisionTool {

    private static final Logger log = LoggerFactory.getLogger(VisionTool.class);

    private final VisionTemplate visionTemplate;

    // Security constraints
    private static final int MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private final HttpClient httpClient;

    /**
     * No-arg constructor used by frameworks that require a default constructor.
     * Delegates to a default VisionTemplate for convenience in tests and tools.
     */
    public VisionTool() {
        this(new VisionTemplate());
    }

    /**
     * Constructs a new VisionTool with the given VisionTemplate and a default HttpClient.
     *
     * @param visionTemplate The VisionTemplate to use for performing vision operations.
     */
    @Autowired
    public VisionTool(VisionTemplate visionTemplate) {
        this(visionTemplate, HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build());
    }

    /**
     * Package-visible constructor used for testing to inject a custom HttpClient.
     */
    VisionTool(VisionTemplate visionTemplate, HttpClient httpClient) {
        this.visionTemplate = visionTemplate;
        this.httpClient = httpClient == null ? HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build() : httpClient;
        log.info("VisionTool initialized",
            StructuredArguments.keyValue("event", "vision_tool_init"),
            StructuredArguments.keyValue("backend", "VisionTemplate"),
            StructuredArguments.keyValue("max_image_size_bytes", MAX_IMAGE_SIZE_BYTES),
            StructuredArguments.keyValue("request_timeout_seconds", REQUEST_TIMEOUT.getSeconds())
        );
    }

    /**
     * Downloads an image from a URL with security constraints and validation.
     * <p>
     * Made protected, so unit tests can override this behaviour (serve local images) without
     * performing network calls. Production behaviour remains the same.
     *
     * @param imageUrl The URL of the image to download.
     * @return A byte array containing the image data.
     * @throws IOException if the image cannot be downloaded or validation fails.
     */
    protected byte[] downloadImageFromUrl(String imageUrl) throws IOException {
        log.debug("Attempting to download image",
            StructuredArguments.keyValue("event", "image_download_start"),
            StructuredArguments.keyValue("url", sanitizeUrlForLogging(imageUrl))
        );

        try {
            // Validate URL format
            URI uri = URI.create(imageUrl);

            // Security: Only allow HTTP/HTTPS protocols
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                throw new IOException("Only HTTP and HTTPS protocols are allowed");
            }

            // Security: Block localhost and private IP ranges
            String host = uri.getHost();
            if (host == null || isLocalOrPrivateHost(host)) {
                throw new IOException("Cannot access local or private network addresses");
            }

            // Download with size limit
            HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                throw new IOException("HTTP error " + response.statusCode());
            }

            // Read with size limit
            try (InputStream inputStream = response.body()) {
                byte[] imageBytes = inputStream.readNBytes(MAX_IMAGE_SIZE_BYTES + 1);

                if (imageBytes.length > MAX_IMAGE_SIZE_BYTES) {
                    throw new IOException("Image size exceeds maximum allowed size of " + MAX_IMAGE_SIZE_BYTES + " bytes");
                }

                log.debug("Successfully downloaded image",
                    StructuredArguments.keyValue("event", "image_download_success"),
                    StructuredArguments.keyValue("size_bytes", imageBytes.length)
                );
                return imageBytes;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Download interrupted: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid URL format: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IOException("Failed to download image: " + e.getMessage(), e);
        }
    }

    /**
     * Checks if a host is localhost or in a private IP range.
     */
    private boolean isLocalOrPrivateHost(String host) {
        String lowerHost = host.toLowerCase();
        return lowerHost.equals("localhost")
            || lowerHost.equals("127.0.0.1")
            || lowerHost.equals("0.0.0.0")
            || lowerHost.equals("::1")
            || lowerHost.startsWith("192.168.")
            || lowerHost.startsWith("10.")
            || lowerHost.matches("172\\.(1[6-9]|2[0-9]|3[0-1])\\..*");
    }

    /**
     * Sanitizes URL for logging by removing sensitive query parameters.
     */
    private String sanitizeUrlForLogging(String url) {
        if (url == null) return "null";
        int queryIndex = url.indexOf('?');
        return queryIndex > 0 ? url.substring(0, queryIndex) + "?..." : url;
    }

    /**
     * Counts the number of faces in an image from a given URL.
     * This is a simple, secure, and well-logged operation that always returns a response.
     *
     * @param imageUrl The URL of the image to analyze (must be HTTP or HTTPS).
     * @return A map containing the count of faces detected, status, and descriptive message.
     * Always returns a response, even on error.
     */
    @Tool(description = "Count faces in an image from a URL. Returns the number of faces detected.")
    @SuppressWarnings("unused")
    public Map<String, Object> countFaces(String imageUrl) {
        log.info("countFaces called",
            StructuredArguments.keyValue("event", "count_faces_start"),
            StructuredArguments.keyValue("url", sanitizeUrlForLogging(imageUrl))
        );

        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            // Validate input
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                log.warn("countFaces validation failed",
                    StructuredArguments.keyValue("event", "count_faces_validation_error"),
                    StructuredArguments.keyValue("error", "empty_url")
                );
                response.put("status", "error");
                response.put("count", 0);
                response.put("message", "Image URL is required and cannot be empty");
                response.put("error", "Missing or empty imageUrl parameter");
                return response;
            }

            // Download image
            byte[] imageBytes;
            try {
                imageBytes = downloadImageFromUrl(imageUrl.trim());
                log.debug("Image downloaded for face detection",
                    StructuredArguments.keyValue("event", "image_download_complete"),
                    StructuredArguments.keyValue("size_bytes", imageBytes.length)
                );
            } catch (IOException e) {
                log.error("Failed to download image",
                    StructuredArguments.keyValue("event", "image_download_error"),
                    StructuredArguments.keyValue("url", sanitizeUrlForLogging(imageUrl)),
                    StructuredArguments.keyValue("error", e.getMessage())
                );
                response.put("status", "error");
                response.put("count", 0);
                response.put("message", "Failed to download image from URL");
                response.put("error", e.getMessage());
                return response;
            }

            // Create ImageData
            ImageData imgData;
            try {
                imgData = ImageData.fromBytes(imageBytes);
                log.debug("ImageData created",
                    StructuredArguments.keyValue("event", "image_data_created")
                );
            } catch (Exception e) {
                log.error("Failed to create ImageData",
                    StructuredArguments.keyValue("event", "image_data_error"),
                    StructuredArguments.keyValue("error", e.getMessage())
                );
                response.put("status", "error");
                response.put("count", 0);
                response.put("message", "Invalid image format or corrupted image data");
                response.put("error", "Failed to parse image: " + e.getMessage());
                return response;
            }

            // Detect faces
            VisionResult detections;
            try {
                detections = visionTemplate.detectFaces(imgData);
                log.debug("Face detection completed",
                    StructuredArguments.keyValue("event", "face_detection_complete")
                );
            } catch (Exception e) {
                log.error("Face detection failed",
                    StructuredArguments.keyValue("event", "face_detection_error"),
                    StructuredArguments.keyValue("error", e.getMessage())
                );
                response.put("status", "error");
                response.put("count", 0);
                response.put("message", "Face detection operation failed");
                response.put("error", "Detection error: " + e.getMessage());
                return response;
            }

            // Build success response
            int faceCount = detections.detections().size();
            double avgConfidence = detections.averageConfidence();
            long duration = System.currentTimeMillis() - startTime;

            String message;
            if (faceCount == 0) {
                message = "No faces detected in the image";
            } else if (faceCount == 1) {
                message = String.format("Detected 1 face with %.1f%% confidence", avgConfidence * 100);
            } else {
                message = String.format("Detected %d faces with average confidence of %.1f%%",
                    faceCount, avgConfidence * 100);
            }

            response.put("status", "success");
            response.put("count", faceCount);
            response.put("message", message);
            response.put("averageConfidence", Math.round(avgConfidence * 10000.0) / 10000.0);
            response.put("processingTimeMs", duration);

            log.info("countFaces completed successfully",
                StructuredArguments.keyValue("event", "count_faces_success"),
                StructuredArguments.keyValue("face_count", faceCount),
                StructuredArguments.keyValue("avg_confidence", Math.round(avgConfidence * 10000.0) / 10000.0),
                StructuredArguments.keyValue("processing_time_ms", duration)
            );
            return response;

        } catch (Exception e) {
            // Catch-all for any unexpected errors
            long duration = System.currentTimeMillis() - startTime;
            log.error("Unexpected error in countFaces",
                StructuredArguments.keyValue("event", "count_faces_unexpected_error"),
                StructuredArguments.keyValue("processing_time_ms", duration),
                StructuredArguments.keyValue("error", e.getMessage()),
                StructuredArguments.keyValue("error_type", e.getClass().getSimpleName())
            );

            response.put("status", "error");
            response.put("count", 0);
            response.put("message", "An unexpected error occurred while processing the request");
            response.put("error", "Internal error: " + e.getMessage());
            response.put("processingTimeMs", duration);
            return response;
        }
    }
}
