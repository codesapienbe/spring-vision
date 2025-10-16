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
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal MCP tool exposing only face counting and embedding extraction.
 *
 * <p>This tool delegates to {@link VisionTemplate}, which can coordinate multiple
 * vision backends. For example:
 * <ul>
 *   <li>{@code OpenCvVisionBackend} - handles face detection and other vision operations</li>
 * </ul>
 *
 * <p>Enable backend in {@code application.yml}:
 * <pre>
 * spring:
 *   vision:
 *     opencv:
 *       enabled: true
 * </pre>
 *
 * <p>The {@code VisionTemplate} will automatically route operations to the OpenCV backend
 * which is the default backend for all vision capabilities.
 */
@Component
public class VisionTool {

    private static final Logger log = LoggerFactory.getLogger(VisionTool.class);

    private final VisionTemplate visionTemplate;

    private static final int MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private final HttpClient httpClient;

    public VisionTool() {
        this(new VisionTemplate());
    }

    @Autowired
    public VisionTool(VisionTemplate visionTemplate) {
        this(visionTemplate, HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build());
    }

    VisionTool(VisionTemplate visionTemplate, HttpClient httpClient) {
        this.visionTemplate = visionTemplate;
        this.httpClient = httpClient == null ? HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build() : httpClient;
        log.info("VisionTool initialized",
            StructuredArguments.keyValue("event", "vision_tool_init"),
            StructuredArguments.keyValue("backend", visionTemplate.getBackendId()),
            StructuredArguments.keyValue("max_image_size_bytes", MAX_IMAGE_SIZE_BYTES),
            StructuredArguments.keyValue("request_timeout_seconds", REQUEST_TIMEOUT.getSeconds()));
    }

    protected byte[] downloadImageFromUrl(String imageUrl) throws IOException {
        log.debug("Attempting to download image",
            StructuredArguments.keyValue("event", "image_download_start"),
            StructuredArguments.keyValue("url", sanitizeUrlForLogging(imageUrl)));

        try {
            URI uri = URI.create(imageUrl);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                throw new IOException("Only HTTP and HTTPS protocols are allowed");
            }

            HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) throw new IOException("HTTP error " + response.statusCode());

            try (InputStream inputStream = response.body()) {
                byte[] imageBytes = inputStream.readNBytes(MAX_IMAGE_SIZE_BYTES + 1);
                if (imageBytes.length > MAX_IMAGE_SIZE_BYTES) {
                    throw new IOException("Image size exceeds maximum allowed size of " + MAX_IMAGE_SIZE_BYTES + " bytes");
                }
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

    private String sanitizeUrlForLogging(String url) {
        if (url == null) return "null";
        int queryIndex = url.indexOf('?');
        return queryIndex > 0 ? url.substring(0, queryIndex) + "?..." : url;
    }

    @Tool(description = "Count faces in an image from a URL. Returns the number of faces detected.")
    @SuppressWarnings("unused")
    public Map<String, Object> countFaces(String imageUrl) {
        log.info("countFaces called",
            StructuredArguments.keyValue("event", "count_faces_start"),
            StructuredArguments.keyValue("url", sanitizeUrlForLogging(imageUrl)));

        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                response.put("status", "error");
                response.put("count", 0);
                response.put("message", "Image URL is required and cannot be empty");
                return response;
            }

            byte[] imageBytes = downloadImageFromUrl(imageUrl.trim());
            ImageData imgData = ImageData.fromBytes(imageBytes);
            VisionResult detections = visionTemplate.detectFaces(imgData);

            int faceCount = detections.detections().size();
            double avgConfidence = detections.averageConfidence();
            long duration = System.currentTimeMillis() - startTime;

            response.put("status", "success");
            response.put("count", faceCount);
            response.put("averageConfidence", Math.round(avgConfidence * 10000.0) / 10000.0);
            response.put("processingTimeMs", duration);
            response.put("message", String.format("Detected %d faces", faceCount));
            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "error");
            response.put("count", 0);
            response.put("message", "Failed to count faces: " + e.getMessage());
            response.put("processingTimeMs", duration);
            return response;
        }
    }

    @Tool(description = "Extract face embeddings from an image URL. Returns list of embeddings and metadata.")
    @SuppressWarnings("unused")
    public Map<String, Object> extractEmbeddings(String imageUrl) {
        log.info("extractEmbeddings called",
            StructuredArguments.keyValue("event", "extract_embeddings_start"),
            StructuredArguments.keyValue("url", sanitizeUrlForLogging(imageUrl)));

        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                response.put("status", "error");
                response.put("message", "Image URL is required and cannot be empty");
                response.put("embeddings", List.of());
                return response;
            }

            byte[] imageBytes = downloadImageFromUrl(imageUrl.trim());
            ImageData imgData = ImageData.fromBytes(imageBytes);

            List<float[]> rawEmbeddings = visionTemplate.extractEmbeddings(imgData);
            List<Map<String, Object>> out = new java.util.ArrayList<>();

            int idx = 0;
            for (float[] emb : rawEmbeddings) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", "face-" + idx);
                item.put("embedding_base64", Base64.getEncoder().encodeToString(floatArrayToBytes(emb)));
                item.put("length", emb.length);
                out.add(item);
                idx++;
            }

            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "success");
            response.put("count", out.size());
            response.put("embeddings", out);
            response.put("processingTimeMs", duration);
            return response;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "error");

            // Handle null exception messages by using the exception class name or cause chain
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isBlank()) {
                errorMsg = e.getClass().getSimpleName();
                Throwable cause = e.getCause();
                if (cause != null && cause.getMessage() != null) {
                    errorMsg += ": " + cause.getMessage();
                }
            }

            response.put("message", "Failed to extract embeddings: " + errorMsg);
            response.put("embeddings", List.of());
            response.put("processingTimeMs", duration);

            // Log full error for debugging
            log.error("Failed to extract embeddings from URL: {}", sanitizeUrlForLogging(imageUrl), e);

            return response;
        }
    }

    private static byte[] floatArrayToBytes(float[] arr) {
        byte[] out = new byte[arr.length * 4];
        for (int i = 0; i < arr.length; i++) {
            int bits = Float.floatToIntBits(arr[i]);
            out[i * 4] = (byte) ((bits >> 24) & 0xFF);
            out[i * 4 + 1] = (byte) ((bits >> 16) & 0xFF);
            out[i * 4 + 2] = (byte) ((bits >> 8) & 0xFF);
            out[i * 4 + 3] = (byte) (bits & 0xFF);
        }
        return out;
    }
}
