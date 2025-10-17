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
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal MCP tool exposing only face counting and embedding extraction.
 *
 * <p>This tool integrates with the Spring Vision framework to provide computer vision
 * capabilities through the Model Context Protocol (MCP). It supports multiple backends
 * and provides comprehensive vision analysis including face detection, object recognition,
 * and embedding extraction.</p>
 *
 * <p>The tool automatically selects the best available backend:
 * <ul>
 *   <li>{@code DjlVisionBackend} - Modern AI-powered vision using Deep Java Library (default)</li>
 *   <li>{@code CyberSecurityBackend} - Specialized security threat detection</li>
 *   <li>{@code RoboticsBackend} - Industrial automation and robotics vision</li>
 * </ul></p>
 */
@Component
public class VisionTool {

    private static final Logger log = LoggerFactory.getLogger(VisionTool.class);

    private final VisionTemplate visionTemplate;

    // Read similarity threshold from application properties with sensible default
    @Value("${spring.vision.djl.face-recognition.similarity-threshold:0.5}")
    private double configuredSimilarityThreshold;

    private static final int MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private final HttpClient httpClient;

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

            final int maxAttempts = 3;
            long backoffMs = 500L; // initial backoff

            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                log.debug("Download attempt {} for {}", attempt, sanitizeUrlForLogging(imageUrl));

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(REQUEST_TIMEOUT)
                    // Provide a friendly User-Agent and accept header to improve compatibility with some hosts
                    .header("User-Agent", "SpringVision/1.0 (+https://github.com/codesapienbe/spring-vision)")
                    .header("Accept", "image/*, */*")
                    .GET()
                    .build();

                try {
                    HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                    int status = response.statusCode();

                    // Successful
                    if (status == 200) {
                        try (InputStream inputStream = response.body()) {
                            byte[] imageBytes = inputStream.readNBytes(MAX_IMAGE_SIZE_BYTES + 1);
                            if (imageBytes.length > MAX_IMAGE_SIZE_BYTES) {
                                throw new IOException("Image size exceeds maximum allowed size of " + MAX_IMAGE_SIZE_BYTES + " bytes");
                            }
                            return imageBytes;
                        }
                    }

                    // Retry on rate-limit or server errors
                    if (status == 429 || (status >= 500 && status < 600)) {
                        log.warn("Transient HTTP error while downloading image: {} - attempt {}/{}", sanitizeUrlForLogging(imageUrl), attempt, maxAttempts);
                        if (attempt == maxAttempts) {
                            throw new IOException("HTTP error " + status);
                        }

                        try {
                            Thread.sleep(backoffMs + (long) (Math.random() * 200L));
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new IOException("Download interrupted: " + ie.getMessage(), ie);
                        }
                        backoffMs *= 2L;
                        continue;
                    }

                    // Non-retriable status
                    throw new IOException("HTTP error " + status);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Download interrupted: " + e.getMessage(), e);
                }
            }

            throw new IOException("Failed to download image after " + maxAttempts + " attempts");

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
                item.put("embedding_base64", Base64.getEncoder().encodeToString(serializeEmbedding(emb)));
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

    @Tool(description = "Extract text from an image using OCR. Returns detected text with confidence scores.")
    @SuppressWarnings("unused")
    public Map<String, Object> extractText(String imageUrl) {
        log.info("extractText called",
            StructuredArguments.keyValue("event", "extract_text_start"),
            StructuredArguments.keyValue("url", sanitizeUrlForLogging(imageUrl)));

        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                response.put("status", "error");
                response.put("message", "Image URL is required and cannot be empty");
                response.put("text", "");
                return response;
            }

            byte[] imageBytes = downloadImageFromUrl(imageUrl.trim());
            ImageData imgData = ImageData.fromBytes(imageBytes);

            // Check if backend supports OCR
            if (!(visionTemplate.backend() instanceof io.github.codesapienbe.springvision.core.capabilities.OcrCapability)) {
                response.put("status", "error");
                response.put("message", "Current backend does not support OCR");
                response.put("text", "");
                return response;
            }

            io.github.codesapienbe.springvision.core.capabilities.OcrCapability ocrBackend =
                (io.github.codesapienbe.springvision.core.capabilities.OcrCapability) visionTemplate.backend();

            List<io.github.codesapienbe.springvision.core.capabilities.OcrCapability.TextDetection> textDetections =
                ocrBackend.extractText(imgData);

            List<Map<String, Object>> detections = new ArrayList<>();
            StringBuilder fullText = new StringBuilder();

            for (io.github.codesapienbe.springvision.core.capabilities.OcrCapability.TextDetection detection : textDetections) {
                Map<String, Object> item = new HashMap<>();
                item.put("text", detection.text());
                item.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);
                item.put("boundingBox", detection.boundingBox());
                detections.add(item);
                fullText.append(detection.text()).append(" ");
            }

            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "success");
            response.put("text", fullText.toString().trim());
            response.put("detections", detections);
            response.put("count", detections.size());
            response.put("processingTimeMs", duration);
            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "error");

            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isBlank()) {
                errorMsg = e.getClass().getSimpleName();
                Throwable cause = e.getCause();
                if (cause != null && cause.getMessage() != null) {
                    errorMsg += ": " + cause.getMessage();
                }
            }

            response.put("message", "Failed to extract text: " + errorMsg);
            response.put("text", "");
            response.put("processingTimeMs", duration);
            log.error("Failed to extract text from URL: {}", sanitizeUrlForLogging(imageUrl), e);
            return response;
        }
    }

    @Tool(description = "Classify an image into categories. Returns top predictions with confidence scores.")
    @SuppressWarnings("unused")
    public Map<String, Object> classifyImage(String imageUrl, Integer topK) {
        if (topK == null) topK = 5;

        log.info("classifyImage called",
            StructuredArguments.keyValue("event", "classify_image_start"),
            StructuredArguments.keyValue("url", sanitizeUrlForLogging(imageUrl)),
            StructuredArguments.keyValue("topK", topK));

        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                response.put("status", "error");
                response.put("message", "Image URL is required and cannot be empty");
                response.put("classifications", List.of());
                return response;
            }

            byte[] imageBytes = downloadImageFromUrl(imageUrl.trim());
            ImageData imgData = ImageData.fromBytes(imageBytes);

            // Check if backend supports image classification
            if (!(visionTemplate.backend() instanceof io.github.codesapienbe.springvision.core.capabilities.ImageClassificationCapability)) {
                response.put("status", "error");
                response.put("message", "Current backend does not support image classification");
                response.put("classifications", List.of());
                return response;
            }

            io.github.codesapienbe.springvision.core.capabilities.ImageClassificationCapability classificationBackend =
                (io.github.codesapienbe.springvision.core.capabilities.ImageClassificationCapability) visionTemplate.backend();

            io.github.codesapienbe.springvision.core.capabilities.ImageClassificationCapability.ClassificationResult result =
                classificationBackend.classifyImage(imgData, topK);

            List<Map<String, Object>> classifications = new ArrayList<>();
            for (io.github.codesapienbe.springvision.core.capabilities.ImageClassificationCapability.Classification classification : result.classifications()) {
                Map<String, Object> item = new HashMap<>();
                item.put("label", classification.label());
                item.put("confidence", Math.round(classification.confidence() * 10000.0) / 10000.0);
                classifications.add(item);
            }

            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "success");
            response.put("classifications", classifications);
            response.put("topPrediction", classifications.isEmpty() ? null : classifications.get(0).get("label"));
            response.put("count", classifications.size());
            response.put("processingTimeMs", duration);
            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "error");

            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isBlank()) {
                errorMsg = e.getClass().getSimpleName();
                Throwable cause = e.getCause();
                if (cause != null && cause.getMessage() != null) {
                    errorMsg += ": " + cause.getMessage();
                }
            }

            response.put("message", "Failed to classify image: " + errorMsg);
            response.put("classifications", List.of());
            response.put("processingTimeMs", duration);
            log.error("Failed to classify image from URL: {}", sanitizeUrlForLogging(imageUrl), e);
            return response;
        }
    }

    @Tool(description = "Detect objects in an image. Returns detected objects with bounding boxes and confidence scores.")
    @SuppressWarnings("unused")
    public Map<String, Object> detectObjects(String imageUrl) {
        log.info("detectObjects called",
            StructuredArguments.keyValue("event", "detect_objects_start"),
            StructuredArguments.keyValue("url", sanitizeUrlForLogging(imageUrl)));

        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                response.put("status", "error");
                response.put("message", "Image URL is required and cannot be empty");
                response.put("objects", List.of());
                return response;
            }

            byte[] imageBytes = downloadImageFromUrl(imageUrl.trim());
            ImageData imgData = ImageData.fromBytes(imageBytes);
            VisionResult detections = visionTemplate.detectObjects(imgData);

            List<Map<String, Object>> objects = new ArrayList<>();
            for (var detection : detections.detections()) {
                Map<String, Object> obj = new HashMap<>();
                obj.put("label", detection.label());
                obj.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);

                if (detection.boundingBox() != null) {
                    Map<String, Object> bbox = new HashMap<>();
                    bbox.put("x", detection.boundingBox().x());
                    bbox.put("y", detection.boundingBox().y());
                    bbox.put("width", detection.boundingBox().width());
                    bbox.put("height", detection.boundingBox().height());
                    obj.put("boundingBox", bbox);
                }

                objects.add(obj);
            }

            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "success");
            response.put("objects", objects);
            response.put("count", objects.size());
            response.put("averageConfidence", Math.round(detections.averageConfidence() * 10000.0) / 10000.0);
            response.put("processingTimeMs", duration);
            response.put("message", String.format("Detected %d objects", objects.size()));
            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "error");
            response.put("objects", List.of());
            response.put("count", 0);
            response.put("message", "Failed to detect objects: " + e.getMessage());
            response.put("processingTimeMs", duration);
            log.error("Failed to detect objects from URL: {}", sanitizeUrlForLogging(imageUrl), e);
            return response;
        }
    }

    @Tool(description = "Detect human poses in an image. Returns detected poses with joint positions and confidence scores.")
    @SuppressWarnings("unused")
    public Map<String, Object> detectPoses(String imageUrl) {
        log.info("detectPoses called",
            StructuredArguments.keyValue("event", "detect_poses_start"),
            StructuredArguments.keyValue("url", sanitizeUrlForLogging(imageUrl)));

        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                response.put("status", "error");
                response.put("message", "Image URL is required and cannot be empty");
                response.put("poses", List.of());
                return response;
            }

            byte[] imageBytes = downloadImageFromUrl(imageUrl.trim());
            ImageData imgData = ImageData.fromBytes(imageBytes);

            // Check if backend supports pose estimation
            if (!(visionTemplate.backend() instanceof io.github.codesapienbe.springvision.core.capabilities.PoseEstimationCapability)) {
                response.put("status", "error");
                response.put("message", "Current backend does not support pose estimation");
                response.put("poses", List.of());
                return response;
            }

            io.github.codesapienbe.springvision.core.capabilities.PoseEstimationCapability poseBackend =
                (io.github.codesapienbe.springvision.core.capabilities.PoseEstimationCapability) visionTemplate.backend();

            List<io.github.codesapienbe.springvision.core.Detection> poseDetections = poseBackend.detectPoses(imgData);

            List<Map<String, Object>> poses = new ArrayList<>();
            for (var detection : poseDetections) {
                Map<String, Object> pose = new HashMap<>();
                pose.put("label", detection.label());
                pose.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);
                pose.put("attributes", detection.attributes());
                poses.add(pose);
            }

            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "success");
            response.put("poses", poses);
            response.put("count", poses.size());
            response.put("processingTimeMs", duration);
            response.put("message", String.format("Detected %d poses", poses.size()));
            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "error");

            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isBlank()) {
                errorMsg = e.getClass().getSimpleName();
                Throwable cause = e.getCause();
                if (cause != null && cause.getMessage() != null) {
                    errorMsg += ": " + cause.getMessage();
                }
            }

            response.put("message", "Failed to detect poses: " + errorMsg);
            response.put("poses", List.of());
            response.put("processingTimeMs", duration);
            log.error("Failed to detect poses from URL: {}", sanitizeUrlForLogging(imageUrl), e);
            return response;
        }
    }

    @Tool(description = "Detect and recognize actions in an image. Returns recognized actions with confidence scores.")
    @SuppressWarnings("unused")
    public Map<String, Object> recognizeActions(String imageUrl) {
        log.info("recognizeActions called",
            StructuredArguments.keyValue("event", "recognize_actions_start"),
            StructuredArguments.keyValue("url", sanitizeUrlForLogging(imageUrl)));

        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                response.put("status", "error");
                response.put("message", "Image URL is required and cannot be empty");
                response.put("actions", List.of());
                return response;
            }

            byte[] imageBytes = downloadImageFromUrl(imageUrl.trim());
            ImageData imgData = ImageData.fromBytes(imageBytes);

            // Check if backend supports action recognition
            if (!(visionTemplate.backend() instanceof io.github.codesapienbe.springvision.core.capabilities.ActionRecognitionCapability)) {
                response.put("status", "error");
                response.put("message", "Current backend does not support action recognition");
                response.put("actions", List.of());
                return response;
            }

            io.github.codesapienbe.springvision.core.capabilities.ActionRecognitionCapability actionBackend =
                (io.github.codesapienbe.springvision.core.capabilities.ActionRecognitionCapability) visionTemplate.backend();

            List<io.github.codesapienbe.springvision.core.Detection> actionDetections = actionBackend.recognizeActions(imgData);

            List<Map<String, Object>> actions = new ArrayList<>();
            for (var detection : actionDetections) {
                Map<String, Object> action = new HashMap<>();
                action.put("action", detection.label());
                action.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);
                actions.add(action);
            }

            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "success");
            response.put("actions", actions);
            response.put("topAction", actions.isEmpty() ? null : actions.get(0).get("action"));
            response.put("count", actions.size());
            response.put("processingTimeMs", duration);
            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "error");

            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isBlank()) {
                errorMsg = e.getClass().getSimpleName();
                Throwable cause = e.getCause();
                if (cause != null && cause.getMessage() != null) {
                    errorMsg += ": " + cause.getMessage();
                }
            }

            response.put("message", "Failed to recognize actions: " + errorMsg);
            response.put("actions", List.of());
            response.put("processingTimeMs", duration);
            log.error("Failed to recognize actions from URL: {}", sanitizeUrlForLogging(imageUrl), e);
            return response;
        }
    }

    @Tool(description = "Verify if two face images belong to the same person. Returns similarity score and match result.")
    @SuppressWarnings("unused")
    public Map<String, Object> verifyFaces(String sourceImageUrl, String targetImageUrl) {
        log.info("verifyFaces called",
            StructuredArguments.keyValue("event", "verify_faces_start"),
            StructuredArguments.keyValue("sourceUrl", sanitizeUrlForLogging(sourceImageUrl)),
            StructuredArguments.keyValue("targetUrl", sanitizeUrlForLogging(targetImageUrl)));

        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            if (sourceImageUrl == null || sourceImageUrl.trim().isEmpty()) {
                response.put("status", "error");
                response.put("message", "Source image URL is required and cannot be empty");
                response.put("isMatch", false);
                return response;
            }

            if (targetImageUrl == null || targetImageUrl.trim().isEmpty()) {
                response.put("status", "error");
                response.put("message", "Target image URL is required and cannot be empty");
                response.put("isMatch", false);
                return response;
            }

            // Download both images
            byte[] sourceBytes = downloadImageFromUrl(sourceImageUrl.trim());
            byte[] targetBytes = downloadImageFromUrl(targetImageUrl.trim());

            ImageData sourceData = ImageData.fromBytes(sourceBytes);
            ImageData targetData = ImageData.fromBytes(targetBytes);

            // Extract embeddings from both images
            List<float[]> sourceEmbeddings = visionTemplate.extractEmbeddings(sourceData);
            List<float[]> targetEmbeddings = visionTemplate.extractEmbeddings(targetData);

            if (sourceEmbeddings.isEmpty()) {
                response.put("status", "error");
                response.put("message", "No faces detected in source image");
                response.put("isMatch", false);
                return response;
            }

            if (targetEmbeddings.isEmpty()) {
                response.put("status", "error");
                response.put("message", "No faces detected in target image");
                response.put("isMatch", false);
                return response;
            }

            // Use the highest confidence face (first embedding)
            float[] sourceEmbedding = sourceEmbeddings.get(0);
            float[] targetEmbedding = targetEmbeddings.get(0);

            // Delegate evaluation to a shared helper to keep logic consistent with other methods
            return evaluateSimilarityAndBuildResponse(sourceEmbedding, targetEmbedding, sourceEmbeddings.size(), targetEmbeddings.size(), startTime);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "error");

            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isBlank()) {
                errorMsg = e.getClass().getSimpleName();
                Throwable cause = e.getCause();
                if (cause != null && cause.getMessage() != null) {
                    errorMsg += ": " + cause.getMessage();
                }
            }

            response.put("message", "Failed to verify faces: " + errorMsg);
            response.put("isMatch", false);
            response.put("processingTimeMs", duration);
            log.error("Failed to verify faces - source: {}, target: {}",
                sanitizeUrlForLogging(sourceImageUrl), sanitizeUrlForLogging(targetImageUrl), e);
            return response;
        }
    }

    // New: support file uploads (raw bytes) for verification
    @Tool(description = "Verify if two face images (uploaded as raw bytes) belong to the same person. Returns similarity score and match result.")
    @SuppressWarnings("unused")
    public Map<String, Object> verifyFacesFromBytes(byte[] sourceImageBytes, byte[] targetImageBytes) {
        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            if (sourceImageBytes == null || sourceImageBytes.length == 0) {
                response.put("status", "error");
                response.put("message", "Source image bytes are required and cannot be empty");
                response.put("isMatch", false);
                return response;
            }

            if (targetImageBytes == null || targetImageBytes.length == 0) {
                response.put("status", "error");
                response.put("message", "Target image bytes are required and cannot be empty");
                response.put("isMatch", false);
                return response;
            }

            ImageData sourceData = ImageData.fromBytes(sourceImageBytes);
            ImageData targetData = ImageData.fromBytes(targetImageBytes);

            List<float[]> sourceEmbeddings = visionTemplate.extractEmbeddings(sourceData);
            List<float[]> targetEmbeddings = visionTemplate.extractEmbeddings(targetData);

            if (sourceEmbeddings.isEmpty()) {
                response.put("status", "error");
                response.put("message", "No faces detected in source image");
                response.put("isMatch", false);
                return response;
            }

            if (targetEmbeddings.isEmpty()) {
                response.put("status", "error");
                response.put("message", "No faces detected in target image");
                response.put("isMatch", false);
                return response;
            }

            float[] sourceEmbedding = sourceEmbeddings.get(0);
            float[] targetEmbedding = targetEmbeddings.get(0);

            return evaluateSimilarityAndBuildResponse(sourceEmbedding, targetEmbedding, sourceEmbeddings.size(), targetEmbeddings.size(), startTime);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "error");
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isBlank()) {
                errorMsg = e.getClass().getSimpleName();
                Throwable cause = e.getCause();
                if (cause != null && cause.getMessage() != null) {
                    errorMsg += ": " + cause.getMessage();
                }
            }
            response.put("message", "Failed to verify faces from bytes: " + errorMsg);
            response.put("isMatch", false);
            response.put("processingTimeMs", duration);
            log.error("Failed to verify faces from bytes", e);
            return response;
        }
    }

    // New helper: unify similarity evaluation and response creation
    private Map<String, Object> evaluateSimilarityAndBuildResponse(float[] sourceEmbedding, float[] targetEmbedding, int sourceFacesCount, int targetFacesCount, long startTime) {
        Map<String, Object> response = new HashMap<>();
        // Use VectorService if available for consistent calculations, otherwise VectorUtils
        Map<String, Object> metrics = computeSimilarityMetrics(sourceEmbedding, targetEmbedding);
        double cosineSimilarity = ((Number) metrics.getOrDefault("cosineSimilarity", 0.0)).doubleValue();
        double euclideanDistance = ((Number) metrics.getOrDefault("euclideanDistance", 0.0)).doubleValue();
        double manhattanDistance = ((Number) metrics.getOrDefault("manhattanDistance", 0.0)).doubleValue();
        double euclideanSimilarity = ((Number) metrics.getOrDefault("euclideanSimilarity", 0.0)).doubleValue();
        double manhattanSimilarity = ((Number) metrics.getOrDefault("manhattanSimilarity", 0.0)).doubleValue();
        double combinedSimilarity = ((Number) metrics.getOrDefault("combinedSimilarity", 0.0)).doubleValue();

        double matchThreshold = this.configuredSimilarityThreshold;
        boolean isMatch = combinedSimilarity >= matchThreshold;

        long duration = System.currentTimeMillis() - startTime;
        response.put("status", "success");
        response.put("isMatch", isMatch);
        response.put("similarity", Math.round(combinedSimilarity * 10000.0) / 10000.0);
        response.put("metrics", Map.of(
            "cosineSimilarity", Math.round(cosineSimilarity * 10000.0) / 10000.0,
            "euclideanSimilarity", Math.round(euclideanSimilarity * 10000.0) / 10000.0,
            "manhattanSimilarity", Math.round(manhattanSimilarity * 10000.0) / 10000.0,
            "euclideanDistance", Math.round(euclideanDistance * 10000.0) / 10000.0,
            "manhattanDistance", Math.round(manhattanDistance * 10000.0) / 10000.0
        ));
        response.put("threshold", matchThreshold);
        response.put("sourceFacesCount", sourceFacesCount);
        response.put("targetFacesCount", targetFacesCount);
        response.put("processingTimeMs", duration);
        response.put("message", isMatch ? "Faces match" : "Faces do not match");
        return response;
    }

    @Tool(description = "Lookup matching faces in a dataset. Returns URLs of images containing matching faces sorted by similarity.")
    @SuppressWarnings("unused")
    public Map<String, Object> lookupFaces(String sourceImageUrl, java.util.Set<String> datasetImageUrls) {
        log.info("lookupFaces called",
            StructuredArguments.keyValue("event", "lookup_faces_start"),
            StructuredArguments.keyValue("sourceUrl", sanitizeUrlForLogging(sourceImageUrl)),
            StructuredArguments.keyValue("datasetSize", datasetImageUrls != null ? datasetImageUrls.size() : 0));

        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            if (sourceImageUrl == null || sourceImageUrl.trim().isEmpty()) {
                response.put("status", "error");
                response.put("message", "Source image URL is required and cannot be empty");
                response.put("matches", List.of());
                return response;
            }

            if (datasetImageUrls == null || datasetImageUrls.isEmpty()) {
                response.put("status", "error");
                response.put("message", "Dataset image URLs are required and cannot be empty");
                response.put("matches", List.of());
                return response;
            }

            // Extract embedding from source image
            byte[] sourceBytes = downloadImageFromUrl(sourceImageUrl.trim());
            ImageData sourceData = ImageData.fromBytes(sourceBytes);
            List<float[]> sourceEmbeddings = visionTemplate.extractEmbeddings(sourceData);

            if (sourceEmbeddings.isEmpty()) {
                response.put("status", "error");
                response.put("message", "No faces detected in source image");
                response.put("matches", List.of());
                return response;
            }

            // Use the highest confidence face (first embedding)
            float[] sourceEmbedding = sourceEmbeddings.getFirst();

            // Process dataset images and calculate similarities
            List<Map<String, Object>> matches = new ArrayList<>();
            int processedCount = 0;
            int errorCount = 0;

            for (String datasetUrl : datasetImageUrls) {
                try {
                    byte[] datasetBytes = downloadImageFromUrl(datasetUrl.trim());
                    ImageData datasetData = ImageData.fromBytes(datasetBytes);
                    List<float[]> datasetEmbeddings = visionTemplate.extractEmbeddings(datasetData);

                    if (!datasetEmbeddings.isEmpty()) {
                        float[] datasetEmbedding = datasetEmbeddings.getFirst();

                        // Calculate similarity metrics using shared utilities
                        Map<String, Object> metrics = computeSimilarityMetrics(sourceEmbedding, datasetEmbedding);
                        double combinedSimilarity = ((Number) metrics.getOrDefault("combinedSimilarity", 0.0)).doubleValue();

                        // Only include matches above threshold
                        double matchThreshold = this.configuredSimilarityThreshold;
                        if (combinedSimilarity >= matchThreshold) {
                            Map<String, Object> match = new HashMap<>();
                            match.put("imageUrl", datasetUrl);
                            match.put("similarity", Math.round(combinedSimilarity * 10000.0) / 10000.0);
                            match.put("facesDetected", datasetEmbeddings.size());
                            match.put("metrics", Map.of(
                                "cosine", Math.round(((Number) metrics.getOrDefault("cosineSimilarity", 0.0)).doubleValue() * 10000.0) / 10000.0,
                                "euclidean", Math.round(((Number) metrics.getOrDefault("euclideanSimilarity", 0.0)).doubleValue() * 10000.0) / 10000.0,
                                "manhattan", Math.round(((Number) metrics.getOrDefault("manhattanSimilarity", 0.0)).doubleValue() * 10000.0) / 10000.0
                            ));
                            matches.add(match);
                        }
                    }
                    processedCount++;
                } catch (Exception e) {
                    errorCount++;
                    log.warn("Failed to process dataset image: {}", sanitizeUrlForLogging(datasetUrl), e);
                }
            }

            // Sort matches by similarity (highest first)
            matches.sort((a, b) -> Double.compare(
                ((Number) b.get("similarity")).doubleValue(),
                ((Number) a.get("similarity")).doubleValue()
            ));

            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "success");
            response.put("matches", matches);
            response.put("matchCount", matches.size());
            response.put("sourceFacesCount", sourceEmbeddings.size());
            response.put("datasetSize", datasetImageUrls.size());
            response.put("processedCount", processedCount);
            response.put("errorCount", errorCount);
            response.put("processingTimeMs", duration);
            response.put("message", String.format("Found %d matching faces in dataset", matches.size()));
            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "error");

            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isBlank()) {
                errorMsg = e.getClass().getSimpleName();
                Throwable cause = e.getCause();
                if (cause != null && cause.getMessage() != null) {
                    errorMsg += ": " + cause.getMessage();
                }
            }

            response.put("message", "Failed to lookup faces: " + errorMsg);
            response.put("matches", List.of());
            response.put("processingTimeMs", duration);
            log.error("Failed to lookup faces from source: {}", sanitizeUrlForLogging(sourceImageUrl), e);
            return response;
        }
    }

    // New: support file uploads (raw bytes) for lookup
    @Tool(description = "Lookup matching faces in a dataset where images are provided as raw bytes (file uploads). Returns matches sorted by similarity.")
    @SuppressWarnings("unused")
    public Map<String, Object> lookupFacesFromBytes(byte[] sourceImageBytes, java.util.Collection<byte[]> datasetImageBytes) {
        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            if (sourceImageBytes == null || sourceImageBytes.length == 0) {
                response.put("status", "error");
                response.put("message", "Source image bytes are required and cannot be empty");
                response.put("matches", List.of());
                return response;
            }

            if (datasetImageBytes == null || datasetImageBytes.isEmpty()) {
                response.put("status", "error");
                response.put("message", "Dataset image bytes are required and cannot be empty");
                response.put("matches", List.of());
                return response;
            }

            ImageData sourceData = ImageData.fromBytes(sourceImageBytes);
            List<float[]> sourceEmbeddings = visionTemplate.extractEmbeddings(sourceData);

            if (sourceEmbeddings.isEmpty()) {
                response.put("status", "error");
                response.put("message", "No faces detected in source image");
                response.put("matches", List.of());
                return response;
            }

            float[] sourceEmbedding = sourceEmbeddings.get(0);

            List<Map<String, Object>> matches = new ArrayList<>();
            int processedCount = 0;
            int errorCount = 0;

            for (byte[] datasetBytes : datasetImageBytes) {
                try {
                    ImageData datasetData = ImageData.fromBytes(datasetBytes);
                    List<float[]> datasetEmbeddings = visionTemplate.extractEmbeddings(datasetData);

                    if (!datasetEmbeddings.isEmpty()) {
                        float[] datasetEmbedding = datasetEmbeddings.get(0);

                        Map<String, Object> metrics = computeSimilarityMetrics(sourceEmbedding, datasetEmbedding);
                        double combinedSimilarity = ((Number) metrics.getOrDefault("combinedSimilarity", 0.0)).doubleValue();

                        double matchThreshold = this.configuredSimilarityThreshold;
                        if (combinedSimilarity >= matchThreshold) {
                            Map<String, Object> match = new HashMap<>();
                            match.put("imageBytes", datasetBytes); // retain raw bytes if caller needs them
                            match.put("similarity", Math.round(combinedSimilarity * 10000.0) / 10000.0);
                            match.put("facesDetected", datasetEmbeddings.size());
                            match.put("metrics", Map.of(
                                "cosine", Math.round(((Number) metrics.getOrDefault("cosineSimilarity", 0.0)).doubleValue() * 10000.0) / 10000.0,
                                "euclidean", Math.round(((Number) metrics.getOrDefault("euclideanSimilarity", 0.0)).doubleValue() * 10000.0) / 10000.0,
                                "manhattan", Math.round(((Number) metrics.getOrDefault("manhattanSimilarity", 0.0)).doubleValue() * 10000.0) / 10000.0
                            ));
                            matches.add(match);
                        }
                    }
                    processedCount++;
                } catch (Exception e) {
                    errorCount++;
                    log.warn("Failed to process dataset image bytes", e);
                }
            }

            // Sort matches by similarity (highest first)
            matches.sort((a, b) -> Double.compare(
                ((Number) b.get("similarity")).doubleValue(),
                ((Number) a.get("similarity")).doubleValue()
            ));

            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "success");
            response.put("matches", matches);
            response.put("matchCount", matches.size());
            response.put("sourceFacesCount", sourceEmbeddings.size());
            response.put("datasetSize", datasetImageBytes.size());
            response.put("processedCount", processedCount);
            response.put("errorCount", errorCount);
            response.put("processingTimeMs", duration);
            response.put("message", String.format("Found %d matching faces in dataset", matches.size()));
            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "error");

            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isBlank()) {
                errorMsg = e.getClass().getSimpleName();
                Throwable cause = e.getCause();
                if (cause != null && cause.getMessage() != null) {
                    errorMsg += ": " + cause.getMessage();
                }
            }

            response.put("message", "Failed to lookup faces from bytes: " + errorMsg);
            response.put("matches", List.of());
            response.put("processingTimeMs", duration);
            log.error("Failed to lookup faces from bytes", e);
            return response;
        }
    }

    // Shared metric/serialization helpers delegating to VectorService when available,
    // otherwise falling back to VectorUtils.
    private Map<String, Object> computeSimilarityMetrics(float[] a, float[] b) {
        try {
            if (visionTemplate != null && visionTemplate.vectorService() != null) {
                return visionTemplate.vectorService().computeSimilarityMetrics(a, b);
            }
        } catch (Exception ignored) {
        }
        // Inline fallback implementation to avoid runtime classloading issues
        Map<String, Object> m = new HashMap<>();
        if (a == null || b == null || a.length != b.length) {
            m.put("cosineSimilarity", 0.0);
            m.put("euclideanDistance", Double.POSITIVE_INFINITY);
            m.put("manhattanDistance", Double.POSITIVE_INFINITY);
            m.put("euclideanSimilarity", 0.0);
            m.put("manhattanSimilarity", 0.0);
            m.put("combinedSimilarity", 0.0);
            return m;
        }

        double dot = 0.0, na = 0.0, nb = 0.0;
        double euclidSum = 0.0, manhattan = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
            double d = a[i] - b[i];
            euclidSum += d * d;
            manhattan += Math.abs(d);
        }
        double cosine = 0.0;
        if (na > 0 && nb > 0) cosine = dot / (Math.sqrt(na) * Math.sqrt(nb));
        double euclid = Math.sqrt(euclidSum);
        double euclidSim = 1.0 / (1.0 + euclid);
        double manhattanSim = 1.0 / (1.0 + manhattan / a.length);
        double combined = (cosine * 0.5) + (euclidSim * 0.3) + (manhattanSim * 0.2);

        m.put("cosineSimilarity", cosine);
        m.put("euclideanDistance", euclid);
        m.put("manhattanDistance", manhattan);
        m.put("euclideanSimilarity", euclidSim);
        m.put("manhattanSimilarity", manhattanSim);
        m.put("combinedSimilarity", combined);
        return m;
    }

    private byte[] serializeEmbedding(float[] arr) {
        try {
            if (visionTemplate != null && visionTemplate.vectorService() != null) {
                return visionTemplate.vectorService().embeddingToBytes(arr);
            }
        } catch (Exception ignored) {
        }
        if (arr == null) return new byte[0];
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

    @Tool(description = "Count faces from raw image bytes. Returns the number of faces detected.")
    @SuppressWarnings("unused")
    public Map<String, Object> countFacesFromBytes(byte[] imageBytes) {
        log.info("countFacesFromBytes called",
            StructuredArguments.keyValue("event", "count_faces_from_bytes_start"));

        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            if (imageBytes == null || imageBytes.length == 0) {
                response.put("status", "error");
                response.put("count", 0);
                response.put("message", "Image bytes are required and cannot be empty");
                return response;
            }

            if (imageBytes.length > MAX_IMAGE_SIZE_BYTES) {
                response.put("status", "error");
                response.put("count", 0);
                response.put("message", "Image size exceeds maximum allowed size of " + MAX_IMAGE_SIZE_BYTES + " bytes");
                return response;
            }

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
            log.error("Failed to count faces from bytes", e);
            return response;
        }
    }

    @Tool(description = "Extract face embeddings from raw image bytes. Returns list of embeddings and metadata.")
    @SuppressWarnings("unused")
    public Map<String, Object> extractEmbeddingsFromBytes(byte[] imageBytes) {
        log.info("extractEmbeddingsFromBytes called",
            StructuredArguments.keyValue("event", "extract_embeddings_from_bytes_start"));

        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            if (imageBytes == null || imageBytes.length == 0) {
                response.put("status", "error");
                response.put("message", "Image bytes are required and cannot be empty");
                response.put("embeddings", List.of());
                return response;
            }

            if (imageBytes.length > MAX_IMAGE_SIZE_BYTES) {
                response.put("status", "error");
                response.put("message", "Image size exceeds maximum allowed size of " + MAX_IMAGE_SIZE_BYTES + " bytes");
                response.put("embeddings", List.of());
                return response;
            }

            ImageData imgData = ImageData.fromBytes(imageBytes);
            List<float[]> rawEmbeddings = visionTemplate.extractEmbeddings(imgData);
            List<Map<String, Object>> out = new java.util.ArrayList<>();

            int idx = 0;
            for (float[] emb : rawEmbeddings) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", "face-" + idx);
                item.put("embedding_base64", Base64.getEncoder().encodeToString(serializeEmbedding(emb)));
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
            log.error("Failed to extract embeddings from bytes", e);
            return response;
        }
    }

}
