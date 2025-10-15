package io.github.codesapienbe.springvision.mcp;

import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import io.github.codesapienbe.springvision.core.VisionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
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
    private static final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(REQUEST_TIMEOUT)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    /**
     * Constructs a new VisionTool with the given VisionTemplate.
     *
     * @param visionTemplate The VisionTemplate to use for performing vision operations.
     */
    public VisionTool(VisionTemplate visionTemplate) {
        this.visionTemplate = visionTemplate;
        log.info("VisionTool initialized with VisionTemplate backend");
    }

    /**
     * Downloads an image from a URL with security constraints and validation.
     *
     * @param imageUrl The URL of the image to download.
     * @return A byte array containing the image data.
     * @throws IOException if the image cannot be downloaded or validation fails.
     */
    private byte[] downloadImageFromUrl(String imageUrl) throws IOException {
        log.debug("Attempting to download image from URL: {}", sanitizeUrlForLogging(imageUrl));

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

                log.debug("Successfully downloaded image: {} bytes", imageBytes.length);
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
        log.info("countFaces called with URL: {}", sanitizeUrlForLogging(imageUrl));

        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            // Validate input
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                log.warn("countFaces called with empty or null imageUrl");
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
                log.debug("Image downloaded successfully, size: {} bytes", imageBytes.length);
            } catch (IOException e) {
                log.error("Failed to download image from URL: {}", sanitizeUrlForLogging(imageUrl), e);
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
                log.debug("ImageData created successfully");
            } catch (Exception e) {
                log.error("Failed to create ImageData from downloaded bytes", e);
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
                log.debug("Face detection completed");
            } catch (Exception e) {
                log.error("Face detection failed", e);
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

            log.info("countFaces completed successfully: {} faces detected in {}ms", faceCount, duration);
            return response;

        } catch (Exception e) {
            // Catch-all for any unexpected errors
            long duration = System.currentTimeMillis() - startTime;
            log.error("Unexpected error in countFaces after {}ms", duration, e);

            response.put("status", "error");
            response.put("count", 0);
            response.put("message", "An unexpected error occurred while processing the request");
            response.put("error", "Internal error: " + e.getMessage());
            response.put("processingTimeMs", duration);
            return response;
        }
    }
}
