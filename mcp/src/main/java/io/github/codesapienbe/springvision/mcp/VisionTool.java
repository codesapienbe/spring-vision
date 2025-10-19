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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

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

        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            throw new IOException("Image URL is required");
        }

        String trimmed = imageUrl.trim();

        // Support data: URIs (data:image/png;base64,....)
        if (trimmed.startsWith("data:")) {
            int comma = trimmed.indexOf(',');
            if (comma < 0) throw new IOException("Invalid data URI format");
            String metadata = trimmed.substring(5, comma);
            String dataPart = trimmed.substring(comma + 1);
            try {
                byte[] imageBytes;
                if (metadata.contains(";base64")) {
                    imageBytes = Base64.getDecoder().decode(dataPart);
                } else {
                    // Percent-decoded text data (rare for images)
                    String decoded = java.net.URLDecoder.decode(dataPart, StandardCharsets.UTF_8);
                    imageBytes = decoded.getBytes(StandardCharsets.UTF_8);
                }
                if (imageBytes.length > MAX_IMAGE_SIZE_BYTES) {
                    throw new IOException("Image size exceeds maximum allowed size of " + MAX_IMAGE_SIZE_BYTES + " bytes");
                }
                return imageBytes;
            } catch (IllegalArgumentException e) {
                throw new IOException("Invalid base64 data in data URI", e);
            }
        }

        // Try to parse as URI; if no scheme or file:// treat as local file path
        try {
            URI uri = URI.create(trimmed);
            String scheme = uri.getScheme();

            // Local file path (no scheme) or file://
            if (scheme == null || scheme.isEmpty() || "file".equalsIgnoreCase(scheme)) {
                Path path;
                if ("file".equalsIgnoreCase(scheme)) {
                    path = Paths.get(uri);
                } else {
                    path = Paths.get(trimmed);
                }

                if (!Files.exists(path)) {
                    throw new IOException("File not found: " + path.toString());
                }

                long size = Files.size(path);
                if (size > MAX_IMAGE_SIZE_BYTES) {
                    throw new IOException("Image size exceeds maximum allowed size of " + MAX_IMAGE_SIZE_BYTES + " bytes");
                }

                return Files.readAllBytes(path);
            }

            // HTTP/HTTPS download
            if (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")) {
                final int maxAttempts = 3;
                long backoffMs = 500L;

                for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                    log.debug("Download attempt {} for {}", attempt, sanitizeUrlForLogging(imageUrl));

                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(uri)
                        .timeout(REQUEST_TIMEOUT)
                        .header("User-Agent", "SpringVision/1.0 (+https://github.com/codesapienbe/spring-vision)")
                        .header("Accept", "image/*, */*")
                        .GET()
                        .build();

                    try {
                        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                        int status = response.statusCode();

                        if (status == 200) {
                            try (InputStream inputStream = response.body()) {
                                byte[] imageBytes = inputStream.readNBytes(MAX_IMAGE_SIZE_BYTES + 1);
                                if (imageBytes.length > MAX_IMAGE_SIZE_BYTES) {
                                    throw new IOException("Image size exceeds maximum allowed size of " + MAX_IMAGE_SIZE_BYTES + " bytes");
                                }
                                return imageBytes;
                            }
                        }

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

                        throw new IOException("HTTP error " + status);

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Download interrupted: " + e.getMessage(), e);
                    }
                }

                throw new IOException("Failed to download image after " + maxAttempts + " attempts");
            }

            throw new IOException("Unsupported URI scheme: " + scheme);

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

    // Overloads: accept raw image bytes (uploaded files) and delegate to existing URL-based methods
    @Tool(description = "Count faces from raw image bytes. Returns the number of faces detected.")
    @SuppressWarnings("unused")
    public Map<String, Object> countFaces(byte[] imageBytes) {
        try {
            ImageData img = resolveImage(imageBytes);
            VisionResult result = visionTemplate.detectFaces(img);

            Map<String, Object> response = new HashMap<>();
            long duration = 0; // caller computes timing; keep simple here
            response.put("status", "success");
            response.put("count", result.detectionCount());
            response.put("averageConfidence", Math.round(result.averageConfidence() * 10000.0) / 10000.0);
            response.put("processingTimeMs", duration);
            response.put("message", String.format("Detected %d faces", result.detectionCount()));
            return response;
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("count", 0);
            response.put("message", "Failed to process uploaded image bytes: " + e.getMessage());
            return response;
        }
    }

    @Tool(description = "Extract face embeddings from raw image bytes. Returns list of embeddings and metadata.")
    @SuppressWarnings("unused")
    public Map<String, Object> extractEmbeddings(byte[] imageBytes) {
        try {
            ImageData img = resolveImage(imageBytes);
            List<float[]> rawEmbeddings = visionTemplate.extractEmbeddings(img,
                io.github.codesapienbe.springvision.core.DetectionCategory.FACE);
            List<Map<String, Object>> out = new ArrayList<>();
            int idx = 0;
            for (float[] emb : rawEmbeddings) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", "face-" + idx);
                item.put("embedding_base64", Base64.getEncoder().encodeToString(serializeEmbedding(emb)));
                item.put("length", emb.length);
                out.add(item);
                idx++;
            }

            Map<String, Object> response = new HashMap<>();
            long duration = 0;
            response.put("status", "success");
            response.put("count", out.size());
            response.put("embeddings", out);
            response.put("processingTimeMs", duration);
            return response;
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("embeddings", List.of());
            response.put("message", "Failed to process uploaded image bytes: " + e.getMessage());
            return response;
        }
    }

    @Tool(description = "Extract text from uploaded image bytes using OCR. Returns detected text with confidence scores.")
    @SuppressWarnings("unused")
    public Map<String, Object> extractText(byte[] imageBytes) {
        try {
            ImageData img = resolveImage(imageBytes);
            VisionResult result = visionTemplate.extractText(img);

            List<Map<String, Object>> detections = new ArrayList<>();
            StringBuilder fullText = new StringBuilder();
            for (var detection : result.detections()) {
                Map<String, Object> item = new HashMap<>();
                String text = (String) detection.attributes().get("text");
                item.put("text", text);
                item.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);
                item.put("boundingBox", Map.of(
                    "x", detection.boundingBox().x(),
                    "y", detection.boundingBox().y(),
                    "width", detection.boundingBox().width(),
                    "height", detection.boundingBox().height()
                ));
                detections.add(item);
                if (text != null) fullText.append(text).append(" ");
            }

            Map<String, Object> response = new HashMap<>();
            long duration = 0;
            response.put("status", "success");
            response.put("text", fullText.toString().trim());
            response.put("detections", detections);
            response.put("count", result.detectionCount());
            response.put("processingTimeMs", duration);
            return response;
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("text", "");
            response.put("message", "Failed to process uploaded image bytes: " + e.getMessage());
            return response;
        }
    }

    @Tool(description = "Classify an uploaded image into categories. Returns top predictions with confidence scores.")
    @SuppressWarnings("unused")
    public Map<String, Object> classifyImage(byte[] imageBytes, Integer topK) {
        try {
            ImageData img = resolveImage(imageBytes);
            VisionResult result = visionTemplate.classifyImage(img, topK == null ? 5 : topK);

            List<Map<String, Object>> classifications = new ArrayList<>();
            for (var detection : result.detections()) {
                Map<String, Object> item = new HashMap<>();
                item.put("label", detection.label());
                item.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);
                classifications.add(item);
            }

            Map<String, Object> response = new HashMap<>();
            long duration = 0;
            response.put("status", "success");
            response.put("classifications", classifications);
            response.put("topPrediction", classifications.isEmpty() ? null : classifications.get(0).get("label"));
            response.put("count", result.detectionCount());
            response.put("processingTimeMs", duration);
            return response;
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("classifications", List.of());
            response.put("message", "Failed to process uploaded image bytes: " + e.getMessage());
            return response;
        }
    }

    @Tool(description = "Detect objects from uploaded image bytes. Returns detected objects with bounding boxes and confidence scores.")
    @SuppressWarnings("unused")
    public Map<String, Object> detectObjects(byte[] imageBytes) {
        try {
            ImageData img = resolveImage(imageBytes);
            VisionResult result = visionTemplate.detectObjects(img);

            List<Map<String, Object>> objects = new ArrayList<>();
            for (var detection : result.detections()) {
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

            Map<String, Object> response = new HashMap<>();
            long duration = 0;
            response.put("status", "success");
            response.put("objects", objects);
            response.put("count", result.detectionCount());
            response.put("averageConfidence", Math.round(result.averageConfidence() * 10000.0) / 10000.0);
            response.put("processingTimeMs", duration);
            response.put("message", String.format("Detected %d objects", result.detectionCount()));
            return response;
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("objects", List.of());
            response.put("message", "Failed to process uploaded image bytes: " + e.getMessage());
            return response;
        }
    }

    @Tool(description = "Detect poses from uploaded image bytes. Returns detected poses with joint positions and confidence scores.")
    @SuppressWarnings("unused")
    public Map<String, Object> detectPoses(byte[] imageBytes) {
        try {
            ImageData img = resolveImage(imageBytes);
            VisionResult result = visionTemplate.detectPoses(img);

            List<Map<String, Object>> poses = new ArrayList<>();
            for (var detection : result.detections()) {
                Map<String, Object> pose = new HashMap<>();
                pose.put("label", detection.label());
                pose.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);
                pose.put("attributes", detection.attributes());
                poses.add(pose);
            }

            Map<String, Object> response = new HashMap<>();
            long duration = 0;
            response.put("status", "success");
            response.put("poses", poses);
            response.put("count", result.detectionCount());
            response.put("processingTimeMs", duration);
            response.put("message", String.format("Detected %d poses", result.detectionCount()));
            return response;
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("poses", List.of());
            response.put("message", "Failed to process uploaded image bytes: " + e.getMessage());
            return response;
        }
    }

    @Tool(description = "Recognize actions from uploaded image bytes. Returns recognized actions with confidence scores.")
    @SuppressWarnings("unused")
    public Map<String, Object> recognizeActions(byte[] imageBytes) {
        try {
            ImageData img = resolveImage(imageBytes);
            VisionResult result = visionTemplate.recognizeActions(img);

            List<Map<String, Object>> actions = new ArrayList<>();
            for (var detection : result.detections()) {
                Map<String, Object> action = new HashMap<>();
                action.put("action", detection.label());
                action.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);
                actions.add(action);
            }

            Map<String, Object> response = new HashMap<>();
            long duration = 0;
            response.put("status", "success");
            response.put("actions", actions);
            response.put("topAction", actions.isEmpty() ? null : actions.get(0).get("action"));
            response.put("count", result.detectionCount());
            response.put("processingTimeMs", duration);
            return response;
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("actions", List.of());
            response.put("message", "Failed to process uploaded image bytes: " + e.getMessage());
            return response;
        }
    }

    @Tool(description = "Detect NSFW from uploaded image bytes. Returns classification as 'normal' or 'nsfw' with confidence score.")
    @SuppressWarnings("unused")
    public Map<String, Object> detectNSFW(byte[] imageBytes) {
        try {
            ImageData img = resolveImage(imageBytes);
            VisionResult result = visionTemplate.detectNSFW(img);

            if (!result.hasDetections()) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("classification", "unknown");
                response.put("confidence", 0.0);
                response.put("isNSFW", false);
                response.put("processingTimeMs", 0);
                return response;
            }

            var detection = result.detections().get(0);
            boolean isNSFW = (Boolean) detection.attributes().getOrDefault("isNSFW", false);
            String classification = (String) detection.attributes().getOrDefault("classification", detection.label());

            Map<String, Object> response = new HashMap<>();
            long duration = 0;
            response.put("status", "success");
            response.put("classification", classification);
            response.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);
            response.put("isNSFW", isNSFW);
            response.put("processingTimeMs", duration);
            return response;
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("classification", "unknown");
            response.put("message", "Failed to process uploaded image bytes: " + e.getMessage());
            return response;
        }
    }

    @Tool(description = "Detect emotions from uploaded image bytes. Returns detected emotions with confidence scores.")
    @SuppressWarnings("unused")
    public Map<String, Object> detectEmotions(byte[] imageBytes) {
        try {
            ImageData img = resolveImage(imageBytes);
            VisionResult result = visionTemplate.detectEmotions(img);

            List<Map<String, Object>> emotions = new ArrayList<>();
            for (var detection : result.detections()) {
                Map<String, Object> emotion = new HashMap<>();
                emotion.put("emotion", detection.label());
                emotion.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);
                emotion.put("faceIndex", detection.attributes().get("faceIndex"));

                if (detection.boundingBox() != null) {
                    Map<String, Double> bbox = new HashMap<>();
                    bbox.put("x", detection.boundingBox().x());
                    bbox.put("y", detection.boundingBox().y());
                    bbox.put("width", detection.boundingBox().width());
                    bbox.put("height", detection.boundingBox().height());
                    emotion.put("boundingBox", bbox);
                }

                emotions.add(emotion);
            }

            Map<String, Object> response = new HashMap<>();
            long duration = 0;
            response.put("status", "success");
            response.put("emotions", emotions);
            response.put("topEmotion", emotions.isEmpty() ? null : emotions.get(0).get("emotion"));
            response.put("count", result.detectionCount());
            response.put("processingTimeMs", duration);
            return response;
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("emotions", List.of());
            response.put("message", "Failed to process uploaded image bytes: " + e.getMessage());
            return response;
        }
    }

    @Tool(description = "Detect deepfakes from uploaded image bytes. Returns classification as 'real' or 'fake' with confidence score.")
    @SuppressWarnings("unused")
    public Map<String, Object> detectDeepfake(byte[] imageBytes) {
        try {
            ImageData img = resolveImage(imageBytes);
            VisionResult result = visionTemplate.detectDeepfake(img);

            if (!result.hasDetections()) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("classification", "unknown");
                response.put("confidence", 0.0);
                response.put("isFake", false);
                response.put("processingTimeMs", 0);
                return response;
            }

            var detection = result.detections().get(0);
            boolean isFake = (Boolean) detection.attributes().getOrDefault("isFake", false);
            String classification = (String) detection.attributes().getOrDefault("classification", detection.label());
            String manipulationType = (String) detection.attributes().get("manipulationType");

            Map<String, Object> response = new HashMap<>();
            long duration = 0;
            response.put("status", "success");
            response.put("classification", classification);
            response.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);
            response.put("isFake", isFake);
            if (manipulationType != null) response.put("manipulationType", manipulationType);
            response.put("processingTimeMs", duration);
            return response;
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("classification", "unknown");
            response.put("message", "Failed to process uploaded image bytes: " + e.getMessage());
            return response;
        }
    }

    @Tool(description = "Detect hands from uploaded image bytes. Returns detected hands with bounding boxes and confidence scores.")
    @SuppressWarnings("unused")
    public Map<String, Object> detectHands(byte[] imageBytes) {
        try {
            ImageData img = resolveImage(imageBytes);
            VisionResult result = visionTemplate.detectHands(img);

            List<Map<String, Object>> hands = new ArrayList<>();
            for (var detection : result.detections()) {
                Map<String, Object> hand = new HashMap<>();
                hand.put("label", detection.label());
                hand.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);

                if (detection.boundingBox() != null) {
                    Map<String, Double> bbox = new HashMap<>();
                    bbox.put("x", detection.boundingBox().x());
                    bbox.put("y", detection.boundingBox().y());
                    bbox.put("width", detection.boundingBox().width());
                    bbox.put("height", detection.boundingBox().height());
                    hand.put("boundingBox", bbox);
                }

                hands.add(hand);
            }

            Map<String, Object> response = new HashMap<>();
            long duration = 0;
            response.put("status", "success");
            response.put("hands", hands);
            response.put("count", result.detectionCount());
            response.put("processingTimeMs", duration);
            return response;
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("hands", List.of());
            response.put("message", "Failed to process uploaded image bytes: " + e.getMessage());
            return response;
        }
    }

    @Tool(description = "Detect demographics from uploaded image bytes. Returns detected demographics with confidence scores.")
    @SuppressWarnings("unused")
    public Map<String, Object> detectDemographics(byte[] imageBytes) {
        try {
            ImageData img = resolveImage(imageBytes);
            VisionResult result = visionTemplate.detectDemographics(img);

            List<Map<String, Object>> demographics = new ArrayList<>();
            for (var detection : result.detections()) {
                Map<String, Object> demo = new HashMap<>();
                demo.put("gender", detection.label());
                demo.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);
                demo.put("age", detection.attributes().get("age"));
                demo.put("ageRange", detection.attributes().get("ageRange"));
                demo.put("genderConfidence", detection.attributes().get("genderConfidence"));
                demo.put("ageError", detection.attributes().get("ageError"));
                demo.put("faceIndex", detection.attributes().get("faceIndex"));

                if (detection.boundingBox() != null) {
                    Map<String, Double> bbox = new HashMap<>();
                    bbox.put("x", detection.boundingBox().x());
                    bbox.put("y", detection.boundingBox().y());
                    bbox.put("width", detection.boundingBox().width());
                    bbox.put("height", detection.boundingBox().height());
                    demo.put("boundingBox", bbox);
                }

                demographics.add(demo);
            }

            Map<String, Object> response = new HashMap<>();
            long duration = 0;
            response.put("status", "success");
            response.put("demographics", demographics);
            response.put("facesAnalyzed", result.detectionCount());
            response.put("processingTimeMs", duration);
            return response;
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("demographics", List.of());
            response.put("message", "Failed to process uploaded image bytes: " + e.getMessage());
            return response;
        }
    }

    @Tool(description = "Detect falls from uploaded image bytes. Returns fall risk assessment with body orientation and confidence scores.")
    @SuppressWarnings("unused")
    public Map<String, Object> detectFall(byte[] imageBytes) {
        try {
            ImageData img = resolveImage(imageBytes);
            VisionResult result = visionTemplate.detectFall(List.of(img));

            if (!result.hasDetections()) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("fallDetected", false);
                response.put("bodyOrientation", "unknown");
                response.put("riskLevel", "low");
                response.put("message", "No person detected");
                response.put("processingTimeMs", 0);
                return response;
            }

            var detection = result.detections().get(0);
            boolean fallDetected = (Boolean) detection.attributes().getOrDefault("fallDetected", false);
            String bodyOrientation = (String) detection.attributes().getOrDefault("bodyOrientation", "unknown");
            String riskLevel = (String) detection.attributes().getOrDefault("riskLevel", "low");
            Double aspectRatio = (Double) detection.attributes().get("aspectRatio");
            Double headHeight = (Double) detection.attributes().get("headHeight");
            String analysisDetails = (String) detection.attributes().get("analysisDetails");

            Map<String, Object> response = new HashMap<>();
            long duration = 0;
            response.put("status", "success");
            response.put("fallDetected", fallDetected);
            response.put("bodyOrientation", bodyOrientation);
            response.put("riskLevel", riskLevel);
            response.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);
            if (aspectRatio != null) response.put("aspectRatio", Math.round(aspectRatio * 1000.0) / 1000.0);
            if (headHeight != null) response.put("headHeight", Math.round(headHeight * 1000.0) / 1000.0);
            if (analysisDetails != null) response.put("analysisDetails", analysisDetails);
            if (detection.boundingBox() != null) {
                Map<String, Double> bbox = new HashMap<>();
                bbox.put("x", detection.boundingBox().x());
                bbox.put("y", detection.boundingBox().y());
                bbox.put("width", detection.boundingBox().width());
                bbox.put("height", detection.boundingBox().height());
                response.put("boundingBox", bbox);
            }
            response.put("processingTimeMs", duration);
            return response;
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("fallDetected", false);
            response.put("message", "Failed to process uploaded image bytes: " + e.getMessage());
            return response;
        }
    }

    @Tool(description = "Analyze stress from uploaded image bytes. Returns stress assessment with level, score, and indicators.")
    @SuppressWarnings("unused")
    public Map<String, Object> analyzeStress(byte[] imageBytes) {
        try {
            ImageData img = resolveImage(imageBytes);
            VisionResult result = visionTemplate.analyzeStress(List.of(img));

            if (!result.hasDetections()) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("stressLevel", "unknown");
                response.put("stressScore", 0.0);
                response.put("message", "No face detected");
                response.put("processingTimeMs", 0);
                return response;
            }

            var detection = result.detections().get(0);
            String stressLevel = (String) detection.attributes().getOrDefault("stressLevel", "unknown");
            Double stressScore = (Double) detection.attributes().get("stressScore");
            String dominantEmotion = (String) detection.attributes().get("dominantEmotion");
            Double emotionIntensity = (Double) detection.attributes().get("emotionIntensity");
            @SuppressWarnings("unchecked")
            List<String> indicators = (List<String>) detection.attributes().get("indicators");

            Map<String, Object> response = new HashMap<>();
            long duration = 0;
            response.put("status", "success");
            response.put("stressLevel", stressLevel);
            response.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);
            if (stressScore != null) response.put("stressScore", Math.round(stressScore * 1000.0) / 1000.0);
            if (dominantEmotion != null) response.put("dominantEmotion", dominantEmotion);
            if (emotionIntensity != null) response.put("emotionIntensity", Math.round(emotionIntensity * 1000.0) / 1000.0);
            if (indicators != null && !indicators.isEmpty()) response.put("indicators", indicators);
            if (detection.boundingBox() != null) {
                Map<String, Double> bbox = new HashMap<>();
                bbox.put("x", detection.boundingBox().x());
                bbox.put("y", detection.boundingBox().y());
                bbox.put("width", detection.boundingBox().width());
                bbox.put("height", detection.boundingBox().height());
                response.put("boundingBox", bbox);
            }
            response.put("disclaimer", "Not for medical diagnosis - research and wellness monitoring only");
            response.put("processingTimeMs", duration);
            return response;
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("stressLevel", "unknown");
            response.put("message", "Failed to process uploaded image bytes: " + e.getMessage());
            return response;
        }
    }

    @Tool(description = "Extract metadata from uploaded image bytes including EXIF, GPS, and camera information.")
    @SuppressWarnings("unused")
    public Map<String, Object> extractImageMetadata(byte[] imageBytes) {
        try {
            ImageData img = resolveImage(imageBytes);
            VisionResult result = visionTemplate.extractMetadata(img);

            Map<String, Map<String, Object>> metadataGroups = new HashMap<>();
            for (var detection : result.detections()) {
                String type = detection.label();
                Map<String, Object> groupData = new HashMap<>(detection.attributes());
                groupData.remove("backend");
                groupData.remove("type");
                metadataGroups.put(type, groupData);
            }

            Map<String, Object> response = new HashMap<>();
            long duration = 0;
            response.put("status", "success");
            response.put("metadata", metadataGroups);
            response.put("groupCount", metadataGroups.size());
            response.put("processingTimeMs", duration);
            response.put("backend", result.metadata().get("backendId"));
            return response;
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("metadata", Map.of());
            response.put("message", "Failed to process uploaded image bytes: " + e.getMessage());
            return response;
        }
    }

    @Tool(description = "Scan and decode barcodes from uploaded image bytes. Returns barcode format, content, and location.")
    @SuppressWarnings("unused")
    public Map<String, Object> scanBarcode(byte[] imageBytes) {
        try {
            ImageData img = resolveImage(imageBytes);
            VisionResult result = visionTemplate.scanBarcodes(img);

            List<Map<String, Object>> barcodes = new ArrayList<>();
            for (var detection : result.detections()) {
                Map<String, Object> barcodeInfo = new HashMap<>();
                barcodeInfo.put("format", detection.label());
                barcodeInfo.put("content", detection.attributes().get("content"));
                barcodeInfo.put("confidence", detection.confidence());
                var bbox = detection.boundingBox();
                Map<String, Double> location = new HashMap<>();
                location.put("x", bbox.x());
                location.put("y", bbox.y());
                location.put("width", bbox.width());
                location.put("height", bbox.height());
                barcodeInfo.put("location", location);
                if (detection.attributes().containsKey("rawBytes")) {
                    barcodeInfo.put("rawBytesLength", detection.attributes().get("rawBytes"));
                }
                barcodes.add(barcodeInfo);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("count", result.detectionCount());
            response.put("barcodes", barcodes);
            response.put("processingTimeMs", 0);
            response.put("backend", result.metadata().get("backendId"));
            return response;
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("barcodes", List.of());
            response.put("message", "Failed to process uploaded image bytes: " + e.getMessage());
            return response;
        }
    }

    @Tool(description = "Detect security threats from uploaded image bytes. Returns detections with severity assessment and metadata.")
    @SuppressWarnings("unused")
    public Map<String, Object> detectThreats(byte[] imageBytes) {
        try {
            ImageData img = resolveImage(imageBytes);
            VisionResult result = visionTemplate.detectThreats(List.of(img));

            List<Map<String, Object>> threats = new ArrayList<>();
            int highSeverityCount = 0;
            for (var detection : result.detections()) {
                Map<String, Object> threat = new HashMap<>();
                threat.put("label", detection.label());
                threat.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);
                if (detection.boundingBox() != null) {
                    Map<String, Object> bbox = new HashMap<>();
                    bbox.put("x", detection.boundingBox().x());
                    bbox.put("y", detection.boundingBox().y());
                    bbox.put("width", detection.boundingBox().width());
                    bbox.put("height", detection.boundingBox().height());
                    threat.put("boundingBox", bbox);
                }
                String threatType = (String) detection.attributes().get("threatType");
                String severity = (String) detection.attributes().get("severity");
                String weaponClass = (String) detection.attributes().get("weaponClass");
                String description = (String) detection.attributes().get("description");
                threat.put("threatType", threatType);
                threat.put("severity", severity);
                threat.put("weaponClass", weaponClass);
                threat.put("description", description);
                if ("HIGH".equals(severity) || "CRITICAL".equals(severity)) highSeverityCount++;
                threats.add(threat);
            }

            Map<String, Object> response = new HashMap<>();
            long processingTime = 0;
            response.put("status", "success");
            response.put("threats", threats);
            response.put("threatCount", threats.size());
            response.put("highSeverityCount", highSeverityCount);
            response.put("processingTimeMs", processingTime);
            response.put("disclaimer", "For legitimate security and safety use only. Comply with local surveillance laws and privacy regulations.");
            response.put("warning", "False positives may occur. Human verification recommended for critical decisions.");
            return response;
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("threats", List.of());
            response.put("threatCount", 0);
            response.put("message", "Failed to process uploaded image bytes: " + e.getMessage());
            return response;
        }
    }

    @Tool(description = "Authenticate access using raw uploaded image bytes. Returns authorization decision.")
    @SuppressWarnings("unused")
    public Map<String, Object> authenticateAccess(byte[] imageBytes) {
        try {
            ImageData img = resolveImage(imageBytes);
            VisionResult result = visionTemplate.authenticateAccess(img);

            if (!result.hasDetections()) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Authentication failed - no results");
                response.put("authorized", false);
                response.put("processingTimeMs", 0);
                return response;
            }

            var authResult = result.detections().get(0);
            Boolean authorized = (Boolean) authResult.attributes().get("authorized");
            Double confidence = (Double) authResult.attributes().get("confidence");
            Double matchScore = (Double) authResult.attributes().get("matchScore");
            String timestamp = (String) authResult.attributes().get("timestamp");
            String userId = (String) authResult.attributes().get("userId");
            String userName = (String) authResult.attributes().get("userName");
            String reason = (String) authResult.attributes().get("reason");

            Map<String, Object> response = new HashMap<>();
            long processingTime = 0;
            response.put("status", "success");
            response.put("authorized", Boolean.TRUE.equals(authorized));
            response.put("label", authResult.label());
            response.put("confidence", confidence != null ? Math.round(confidence * 10000.0) / 10000.0 : 0.0);
            response.put("matchScore", matchScore != null ? Math.round(matchScore * 10000.0) / 10000.0 : 0.0);
            response.put("timestamp", timestamp);
            response.put("processingTimeMs", processingTime);

            if (Boolean.TRUE.equals(authorized)) {
                response.put("userId", userId);
                response.put("userName", userName);
                response.put("message", "Access granted for user: " + userName);
            } else {
                response.put("reason", reason);
                response.put("message", "Access denied: " + reason);
            }

            response.put("securityNote", "This is a demonstration. Production systems should implement liveness detection and multi-factor authentication.");
            response.put("privacyNote", "Ensure compliance with biometric privacy laws (GDPR, BIPA, etc.)");
            return response;
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("authorized", false);
            response.put("message", "Failed to process uploaded image bytes: " + e.getMessage());
            return response;
        }
    }

    @Tool(description = "Estimate heart rate from uploaded frames provided as raw image bytes.")
    @SuppressWarnings("unused")
    public Map<String, Object> estimateHeartRateFromBytes(java.util.Collection<byte[]> frames) {
        try {
            List<ImageData> imageFrames = new ArrayList<>();
            for (byte[] b : frames) {
                try {
                    imageFrames.add(resolveImage(b));
                } catch (Exception ex) {
                    log.warn("Failed to decode frame bytes", ex);
                }
            }
            if (imageFrames.size() < 100) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Failed to decode enough frames. Decoded: " + imageFrames.size() + ", needed: 100");
                response.put("heartRate", 0.0);
                return response;
            }

            VisionResult result = visionTemplate.estimateHeartRate(imageFrames);
            // reuse existing HTTP-based method's response mapping
            if (!result.hasDetections()) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Heart rate estimation failed - no results");
                response.put("heartRate", 0.0);
                response.put("processingTimeMs", 0);
                return response;
            }

            var detection = result.detections().get(0);
            Map<String, Object> response = new HashMap<>();
            Double heartRate = (Double) detection.attributes().get("heartRate");
            response.put("status", "success");
            response.put("heartRate", heartRate);
            response.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);
            response.put("processingTimeMs", 0);
            return response;
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to process uploaded frames: " + e.getMessage());
            response.put("heartRate", 0.0);
            return response;
        }
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

            byte[] imageBytes;
            if (imageUrl.startsWith("data:") || imageUrl.startsWith("file:") || Files.exists(Paths.get(imageUrl))) {
                // Treat as local or data URI
                imageBytes = downloadImageFromUrl(imageUrl.trim());
            } else {
                imageBytes = downloadImageFromUrl(imageUrl.trim());
            }
            ImageData imgData = ImageData.fromBytes(imageBytes);
            
            // Use VisionTemplate high-level API
            VisionResult result = visionTemplate.detectFaces(imgData);

            long duration = System.currentTimeMillis() - startTime;

            response.put("status", "success");
            response.put("count", result.detectionCount());
            response.put("averageConfidence", Math.round(result.averageConfidence() * 10000.0) / 10000.0);
            response.put("processingTimeMs", duration);
            response.put("message", String.format("Detected %d faces", result.detectionCount()));
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

            ImageData imgData = resolveImage(imageUrl.trim());

            // Use VisionTemplate high-level API
            List<float[]> rawEmbeddings = visionTemplate.extractEmbeddings(imgData, 
                io.github.codesapienbe.springvision.core.DetectionCategory.FACE);
            List<Map<String, Object>> out = new ArrayList<>();

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

            ImageData imgData = resolveImage(imageUrl.trim());

            // Use VisionTemplate high-level API
            VisionResult result = visionTemplate.extractText(imgData);

            List<Map<String, Object>> detections = new ArrayList<>();
            StringBuilder fullText = new StringBuilder();

            for (var detection : result.detections()) {
                Map<String, Object> item = new HashMap<>();
                String text = (String) detection.attributes().get("text");
                item.put("text", text);
                item.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);
                item.put("boundingBox", Map.of(
                    "x", detection.boundingBox().x(),
                    "y", detection.boundingBox().y(),
                    "width", detection.boundingBox().width(),
                    "height", detection.boundingBox().height()
                ));
                detections.add(item);
                if (text != null) {
                    fullText.append(text).append(" ");
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "success");
            response.put("text", fullText.toString().trim());
            response.put("detections", detections);
            response.put("count", result.detectionCount());
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

            ImageData imgData = resolveImage(imageUrl.trim());

            // Use VisionTemplate high-level API
            VisionResult result = visionTemplate.classifyImage(imgData, topK);

            List<Map<String, Object>> classifications = new ArrayList<>();
            for (var detection : result.detections()) {
                Map<String, Object> item = new HashMap<>();
                item.put("label", detection.label());
                item.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);
                classifications.add(item);
            }

            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "success");
            response.put("classifications", classifications);
            response.put("topPrediction", classifications.isEmpty() ? null : classifications.get(0).get("label"));
            response.put("count", result.detectionCount());
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

            ImageData imgData = resolveImage(imageUrl.trim());
            
            // Use VisionTemplate high-level API
            VisionResult result = visionTemplate.detectObjects(imgData);

            List<Map<String, Object>> objects = new ArrayList<>();
            for (var detection : result.detections()) {
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
            response.put("count", result.detectionCount());
            response.put("averageConfidence", Math.round(result.averageConfidence() * 10000.0) / 10000.0);
            response.put("processingTimeMs", duration);
            response.put("message", String.format("Detected %d objects", result.detectionCount()));
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

            ImageData imgData = resolveImage(imageUrl.trim());

            // Use VisionTemplate high-level API
            VisionResult result = visionTemplate.detectPoses(imgData);

            List<Map<String, Object>> poses = new ArrayList<>();
            for (var detection : result.detections()) {
                Map<String, Object> pose = new HashMap<>();
                pose.put("label", detection.label());
                pose.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);
                pose.put("attributes", detection.attributes());
                poses.add(pose);
            }

            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "success");
            response.put("poses", poses);
            response.put("count", result.detectionCount());
            response.put("processingTimeMs", duration);
            response.put("message", String.format("Detected %d poses", result.detectionCount()));
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

            ImageData imgData = resolveImage(imageUrl.trim());

            // Use VisionTemplate high-level API
            VisionResult result = visionTemplate.recognizeActions(imgData);

            List<Map<String, Object>> actions = new ArrayList<>();
            for (var detection : result.detections()) {
                Map<String, Object> action = new HashMap<>();
                action.put("action", detection.label());
                action.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);
                actions.add(action);
            }

            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "success");
            response.put("actions", actions);
            response.put("topAction", actions.isEmpty() ? null : actions.get(0).get("action"));
            response.put("count", result.detectionCount());
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

            // Use VisionTemplate high-level API
            // Extract embeddings from both images
            List<float[]> sourceEmbeddings = visionTemplate.extractEmbeddings(sourceData,
                io.github.codesapienbe.springvision.core.DetectionCategory.FACE);
            List<float[]> targetEmbeddings = visionTemplate.extractEmbeddings(targetData,
                io.github.codesapienbe.springvision.core.DetectionCategory.FACE);

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

            // Use VisionTemplate high-level API
            List<float[]> sourceEmbeddings = visionTemplate.extractEmbeddings(sourceData,
                io.github.codesapienbe.springvision.core.DetectionCategory.FACE);
            List<float[]> targetEmbeddings = visionTemplate.extractEmbeddings(targetData,
                io.github.codesapienbe.springvision.core.DetectionCategory.FACE);

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
            
            // Use VisionTemplate high-level API
            List<float[]> sourceEmbeddings = visionTemplate.extractEmbeddings(sourceData,
                io.github.codesapienbe.springvision.core.DetectionCategory.FACE);

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
                    List<float[]> datasetEmbeddings = visionTemplate.extractEmbeddings(datasetData,
                        io.github.codesapienbe.springvision.core.DetectionCategory.FACE);

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
            
            // Use VisionTemplate high-level API
            List<float[]> sourceEmbeddings = visionTemplate.extractEmbeddings(sourceData,
                io.github.codesapienbe.springvision.core.DetectionCategory.FACE);

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
                    List<float[]> datasetEmbeddings = visionTemplate.extractEmbeddings(datasetData,
                        io.github.codesapienbe.springvision.core.DetectionCategory.FACE);

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

    // Helper: create ImageData directly from bytes (no temp files)
    private ImageData resolveImage(byte[] imageBytes) throws IOException {
        if (imageBytes == null || imageBytes.length == 0) throw new IOException("Image bytes are required and cannot be empty");
        if (imageBytes.length > MAX_IMAGE_SIZE_BYTES) throw new IOException("Image size exceeds maximum allowed size of " + MAX_IMAGE_SIZE_BYTES + " bytes");
        return ImageData.fromBytes(imageBytes);
    }

    // Helper: resolve a string reference (http(s)/data/file/local path) into ImageData
    private ImageData resolveImage(String imageRef) throws IOException {
        byte[] bytes = downloadImageFromUrl(imageRef);
        return ImageData.fromBytes(bytes);
    }

    

    @Tool(description = "Detect NSFW (Not Safe For Work) content in an image. Returns classification as 'normal' or 'nsfw' with confidence score.")
    @SuppressWarnings("unused")
    public Map<String, Object> detectNSFW(String imageUrl) {
        log.info("detectNSFW called",
            StructuredArguments.keyValue("event", "detect_nsfw_start"),
            StructuredArguments.keyValue("url", sanitizeUrlForLogging(imageUrl)));

        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                response.put("status", "error");
                response.put("message", "Image URL is required and cannot be empty");
                response.put("classification", "unknown");
                return response;
            }

            ImageData imgData = resolveImage(imageUrl.trim());

            // Use VisionTemplate high-level API
            VisionResult result = visionTemplate.detectNSFW(imgData);

            if (!result.hasDetections()) {
                response.put("status", "success");
                response.put("classification", "unknown");
                response.put("confidence", 0.0);
                response.put("isNSFW", false);
                response.put("processingTimeMs", System.currentTimeMillis() - startTime);
                return response;
            }

            var detection = result.detections().get(0);
            boolean isNSFW = (Boolean) detection.attributes().getOrDefault("isNSFW", false);
            String classification = (String) detection.attributes().getOrDefault("classification", detection.label());

            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "success");
            response.put("classification", classification);
            response.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);
            response.put("isNSFW", isNSFW);
            response.put("processingTimeMs", duration);
            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "error");
            response.put("message", "Failed to detect NSFW content: " + e.getMessage());
            response.put("classification", "unknown");
            response.put("processingTimeMs", duration);
            log.error("Failed to detect NSFW from URL: {}", sanitizeUrlForLogging(imageUrl), e);
            return response;
        }
    }

    @Tool(description = "Detect emotions from faces in an image. Returns detected emotions with confidence scores (angry, disgust, fear, happy, sad, surprise, neutral).")
    @SuppressWarnings("unused")
    public Map<String, Object> detectEmotions(String imageUrl) {
        log.info("detectEmotions called",
            StructuredArguments.keyValue("event", "detect_emotions_start"),
            StructuredArguments.keyValue("url", sanitizeUrlForLogging(imageUrl)));

        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                response.put("status", "error");
                response.put("message", "Image URL is required and cannot be empty");
                response.put("emotions", List.of());
                return response;
            }

            ImageData imgData = resolveImage(imageUrl.trim());

            // Use VisionTemplate high-level API
            VisionResult result = visionTemplate.detectEmotions(imgData);

            List<Map<String, Object>> emotions = new ArrayList<>();
            for (var detection : result.detections()) {
                Map<String, Object> emotion = new HashMap<>();
                emotion.put("emotion", detection.label());
                emotion.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);
                emotion.put("faceIndex", detection.attributes().get("faceIndex"));
                
                // Include bounding box if available
                if (detection.boundingBox() != null) {
                    Map<String, Double> bbox = new HashMap<>();
                    bbox.put("x", detection.boundingBox().x());
                    bbox.put("y", detection.boundingBox().y());
                    bbox.put("width", detection.boundingBox().width());
                    bbox.put("height", detection.boundingBox().height());
                    emotion.put("boundingBox", bbox);
                }
                
                emotions.add(emotion);
            }

            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "success");
            response.put("emotions", emotions);
            response.put("topEmotion", emotions.isEmpty() ? null : emotions.get(0).get("emotion"));
            response.put("count", result.detectionCount());
            response.put("processingTimeMs", duration);
            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "error");
            response.put("message", "Failed to detect emotions: " + e.getMessage());
            response.put("emotions", List.of());
            response.put("processingTimeMs", duration);
            log.error("Failed to detect emotions from URL: {}", sanitizeUrlForLogging(imageUrl), e);
            return response;
        }
    }

    @Tool(description = "Detect deepfakes in an image. Returns classification as 'real' or 'fake' with confidence score.")
    @SuppressWarnings("unused")
    public Map<String, Object> detectDeepfake(String imageUrl) {
        log.info("detectDeepfake called",
            StructuredArguments.keyValue("event", "detect_deepfake_start"),
            StructuredArguments.keyValue("url", sanitizeUrlForLogging(imageUrl)));

        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                response.put("status", "error");
                response.put("message", "Image URL is required and cannot be empty");
                response.put("classification", "unknown");
                return response;
            }

            ImageData imgData = resolveImage(imageUrl.trim());

            // Use VisionTemplate high-level API
            VisionResult result = visionTemplate.detectDeepfake(imgData);

            if (!result.hasDetections()) {
                response.put("status", "success");
                response.put("classification", "unknown");
                response.put("confidence", 0.0);
                response.put("isFake", false);
                response.put("processingTimeMs", System.currentTimeMillis() - startTime);
                return response;
            }

            var detection = result.detections().get(0);
            boolean isFake = (Boolean) detection.attributes().getOrDefault("isFake", false);
            String classification = (String) detection.attributes().getOrDefault("classification", detection.label());
            String manipulationType = (String) detection.attributes().get("manipulationType");

            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "success");
            response.put("classification", classification);
            response.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);
            response.put("isFake", isFake);
            if (manipulationType != null) {
                response.put("manipulationType", manipulationType);
            }
            response.put("processingTimeMs", duration);
            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "error");
            response.put("message", "Failed to detect deepfake: " + e.getMessage());
            response.put("classification", "unknown");
            response.put("processingTimeMs", duration);
            log.error("Failed to detect deepfake from URL: {}", sanitizeUrlForLogging(imageUrl), e);
            return response;
        }
    }

    @Tool(description = "Detect hands in an image. Returns detected hands with bounding boxes and confidence scores.")
    @SuppressWarnings("unused")
    public Map<String, Object> detectHands(String imageUrl) {
        log.info("detectHands called",
            StructuredArguments.keyValue("event", "detect_hands_start"),
            StructuredArguments.keyValue("url", sanitizeUrlForLogging(imageUrl)));

        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                response.put("status", "error");
                response.put("message", "Image URL is required and cannot be empty");
                response.put("hands", List.of());
                return response;
            }

            byte[] imageBytes = downloadImageFromUrl(imageUrl.trim());
            ImageData imgData = ImageData.fromBytes(imageBytes);

            // Use VisionTemplate high-level API
            VisionResult result = visionTemplate.detectHands(imgData);

            List<Map<String, Object>> hands = new ArrayList<>();
            for (var detection : result.detections()) {
                Map<String, Object> hand = new HashMap<>();
                hand.put("label", detection.label());
                hand.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);
                
                // Include bounding box
                if (detection.boundingBox() != null) {
                    Map<String, Double> bbox = new HashMap<>();
                    bbox.put("x", detection.boundingBox().x());
                    bbox.put("y", detection.boundingBox().y());
                    bbox.put("width", detection.boundingBox().width());
                    bbox.put("height", detection.boundingBox().height());
                    hand.put("boundingBox", bbox);
                }
                
                hands.add(hand);
            }

            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "success");
            response.put("hands", hands);
            response.put("count", result.detectionCount());
            response.put("processingTimeMs", duration);
            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "error");
            response.put("message", "Failed to detect hands: " + e.getMessage());
            response.put("hands", List.of());
            response.put("processingTimeMs", duration);
            log.error("Failed to detect hands from URL: {}", sanitizeUrlForLogging(imageUrl), e);
            return response;
        }
    }

    @Tool(description = "Detect demographics (age and gender) from faces in an image. Returns detected demographics with confidence scores.")
    @SuppressWarnings("unused")
    public Map<String, Object> detectDemographics(String imageUrl) {
        log.info("detectDemographics called",
            StructuredArguments.keyValue("event", "detect_demographics_start"),
            StructuredArguments.keyValue("url", sanitizeUrlForLogging(imageUrl)));

        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                response.put("status", "error");
                response.put("message", "Image URL is required and cannot be empty");
                response.put("demographics", List.of());
                return response;
            }

            byte[] imageBytes = downloadImageFromUrl(imageUrl.trim());
            ImageData imgData = ImageData.fromBytes(imageBytes);

            // Use VisionTemplate high-level API
            VisionResult result = visionTemplate.detectDemographics(imgData);

            List<Map<String, Object>> demographics = new ArrayList<>();
            for (var detection : result.detections()) {
                Map<String, Object> demo = new HashMap<>();
                demo.put("gender", detection.label());
                demo.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);
                demo.put("age", detection.attributes().get("age"));
                demo.put("ageRange", detection.attributes().get("ageRange"));
                demo.put("genderConfidence", detection.attributes().get("genderConfidence"));
                demo.put("ageError", detection.attributes().get("ageError"));
                demo.put("faceIndex", detection.attributes().get("faceIndex"));
                
                // Include bounding box if available
                if (detection.boundingBox() != null) {
                    Map<String, Double> bbox = new HashMap<>();
                    bbox.put("x", detection.boundingBox().x());
                    bbox.put("y", detection.boundingBox().y());
                    bbox.put("width", detection.boundingBox().width());
                    bbox.put("height", detection.boundingBox().height());
                    demo.put("boundingBox", bbox);
                }
                
                demographics.add(demo);
            }

            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "success");
            response.put("demographics", demographics);
            response.put("facesAnalyzed", result.detectionCount());
            response.put("processingTimeMs", duration);
            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "error");
            response.put("message", "Failed to detect demographics: " + e.getMessage());
            response.put("demographics", List.of());
            response.put("processingTimeMs", duration);
            log.error("Failed to detect demographics from URL: {}", sanitizeUrlForLogging(imageUrl), e);
            return response;
        }
    }

    @Tool(description = "Detect falls from body pose analysis. Returns fall risk assessment with body orientation and confidence scores. Useful for elderly care and safety monitoring.")
    @SuppressWarnings("unused")
    public Map<String, Object> detectFall(String imageUrl) {
        log.info("detectFall called",
            StructuredArguments.keyValue("event", "detect_fall_start"),
            StructuredArguments.keyValue("url", sanitizeUrlForLogging(imageUrl)));

        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                response.put("status", "error");
                response.put("message", "Image URL is required and cannot be empty");
                response.put("fallDetected", false);
                return response;
            }

            byte[] imageBytes = downloadImageFromUrl(imageUrl.trim());
            ImageData imgData = ImageData.fromBytes(imageBytes);

            // Use VisionTemplate high-level API - For single image, wrap in list
            VisionResult result = visionTemplate.detectFall(List.of(imgData));

            if (!result.hasDetections()) {
                response.put("status", "success");
                response.put("fallDetected", false);
                response.put("bodyOrientation", "unknown");
                response.put("riskLevel", "low");
                response.put("message", "No person detected");
                response.put("processingTimeMs", System.currentTimeMillis() - startTime);
                return response;
            }

            // Get the first/primary detection
            var detection = result.detections().get(0);
            
            boolean fallDetected = (Boolean) detection.attributes().getOrDefault("fallDetected", false);
            String bodyOrientation = (String) detection.attributes().getOrDefault("bodyOrientation", "unknown");
            String riskLevel = (String) detection.attributes().getOrDefault("riskLevel", "low");
            Double aspectRatio = (Double) detection.attributes().get("aspectRatio");
            Double headHeight = (Double) detection.attributes().get("headHeight");
            String analysisDetails = (String) detection.attributes().get("analysisDetails");

            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "success");
            response.put("fallDetected", fallDetected);
            response.put("bodyOrientation", bodyOrientation);
            response.put("riskLevel", riskLevel);
            response.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);
            
            if (aspectRatio != null) {
                response.put("aspectRatio", Math.round(aspectRatio * 1000.0) / 1000.0);
            }
            if (headHeight != null) {
                response.put("headHeight", Math.round(headHeight * 1000.0) / 1000.0);
            }
            if (analysisDetails != null) {
                response.put("analysisDetails", analysisDetails);
            }
            
            // Include bounding box if available
            if (detection.boundingBox() != null) {
                Map<String, Double> bbox = new HashMap<>();
                bbox.put("x", detection.boundingBox().x());
                bbox.put("y", detection.boundingBox().y());
                bbox.put("width", detection.boundingBox().width());
                bbox.put("height", detection.boundingBox().height());
                response.put("boundingBox", bbox);
            }
            
            response.put("processingTimeMs", duration);
            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "error");
            response.put("message", "Failed to detect fall: " + e.getMessage());
            response.put("fallDetected", false);
            response.put("processingTimeMs", duration);
            log.error("Failed to detect fall from URL: {}", sanitizeUrlForLogging(imageUrl), e);
            return response;
        }
    }

    @Tool(description = "Analyze stress levels from facial expressions. Returns stress assessment with level, score, and indicators. For research and wellness monitoring only - not for medical diagnosis.")
    @SuppressWarnings("unused")
    public Map<String, Object> analyzeStress(String imageUrl) {
        log.info("analyzeStress called",
            StructuredArguments.keyValue("event", "analyze_stress_start"),
            StructuredArguments.keyValue("url", sanitizeUrlForLogging(imageUrl)));

        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                response.put("status", "error");
                response.put("message", "Image URL is required and cannot be empty");
                response.put("stressLevel", "unknown");
                return response;
            }

            byte[] imageBytes = downloadImageFromUrl(imageUrl.trim());
            ImageData imgData = ImageData.fromBytes(imageBytes);

            // Use VisionTemplate high-level API - For single image, wrap in list
            VisionResult result = visionTemplate.analyzeStress(List.of(imgData));

            if (!result.hasDetections()) {
                response.put("status", "success");
                response.put("stressLevel", "unknown");
                response.put("stressScore", 0.0);
                response.put("message", "No face detected");
                response.put("processingTimeMs", System.currentTimeMillis() - startTime);
                return response;
            }

            // Get the first/primary detection
            var detection = result.detections().get(0);
            
            String stressLevel = (String) detection.attributes().getOrDefault("stressLevel", "unknown");
            Double stressScore = (Double) detection.attributes().get("stressScore");
            String dominantEmotion = (String) detection.attributes().get("dominantEmotion");
            Double emotionIntensity = (Double) detection.attributes().get("emotionIntensity");
            @SuppressWarnings("unchecked")
            List<String> indicators = (List<String>) detection.attributes().get("indicators");

            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "success");
            response.put("stressLevel", stressLevel);
            response.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);
            
            if (stressScore != null) {
                response.put("stressScore", Math.round(stressScore * 1000.0) / 1000.0);
            }
            if (dominantEmotion != null) {
                response.put("dominantEmotion", dominantEmotion);
            }
            if (emotionIntensity != null) {
                response.put("emotionIntensity", Math.round(emotionIntensity * 1000.0) / 1000.0);
            }
            if (indicators != null && !indicators.isEmpty()) {
                response.put("indicators", indicators);
            }
            
            // Include bounding box if available
            if (detection.boundingBox() != null) {
                Map<String, Double> bbox = new HashMap<>();
                bbox.put("x", detection.boundingBox().x());
                bbox.put("y", detection.boundingBox().y());
                bbox.put("width", detection.boundingBox().width());
                bbox.put("height", detection.boundingBox().height());
                response.put("boundingBox", bbox);
            }
            
            response.put("disclaimer", "Not for medical diagnosis - research and wellness monitoring only");
            response.put("processingTimeMs", duration);
            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "error");
            response.put("message", "Failed to analyze stress: " + e.getMessage());
            response.put("stressLevel", "unknown");
            response.put("processingTimeMs", duration);
            log.error("Failed to analyze stress from URL: {}", sanitizeUrlForLogging(imageUrl), e);
            return response;
        }
    }

    @Tool(description = "Estimate heart rate from video sequence using remote photoplethysmography (rPPG). Requires minimum 100 frames (5+ seconds). NOT A MEDICAL DEVICE - research/wellness only.")
    @SuppressWarnings("unused")
    public Map<String, Object> estimateHeartRate(java.util.Collection<String> imageUrls) {
        log.info("estimateHeartRate called",
            StructuredArguments.keyValue("event", "estimate_heartrate_start"),
            StructuredArguments.keyValue("frameCount", imageUrls == null ? 0 : imageUrls.size()));

        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            if (imageUrls == null || imageUrls.isEmpty()) {
                response.put("status", "error");
                response.put("message", "Image URLs are required (minimum 100 frames)");
                response.put("heartRate", 0.0);
                return response;
            }

            if (imageUrls.size() < 100) {
                response.put("status", "error");
                response.put("message", "Insufficient frames. Need minimum 100 (5+ seconds). Got: " + imageUrls.size());
                response.put("heartRate", 0.0);
                return response;
            }

            // Download all frames
            List<ImageData> frames = new ArrayList<>();
            int downloadedFrames = 0;
            
            for (String url : imageUrls) {
                try {
                    byte[] imageBytes = downloadImageFromUrl(url.trim());
                    frames.add(ImageData.fromBytes(imageBytes));
                    downloadedFrames++;
                } catch (Exception e) {
                    log.warn("Failed to download frame from URL: {}", sanitizeUrlForLogging(url), e);
                    // Continue with other frames
                }
            }

            if (frames.size() < 100) {
                response.put("status", "error");
                response.put("message", "Failed to download enough frames. Downloaded: " + frames.size() + ", needed: 100");
                response.put("heartRate", 0.0);
                return response;
            }

            // Use VisionTemplate high-level API
            VisionResult result = visionTemplate.estimateHeartRate(frames);

            if (!result.hasDetections()) {
                response.put("status", "error");
                response.put("message", "Heart rate estimation failed - no results");
                response.put("heartRate", 0.0);
                response.put("processingTimeMs", System.currentTimeMillis() - startTime);
                return response;
            }

            // Get the primary detection
            var detection = result.detections().get(0);
            
            // Check for error detection
            if (detection.label().equals("insufficient_data")) {
                String errorMsg = (String) detection.attributes().getOrDefault("message", "Insufficient data");
                response.put("status", "error");
                response.put("message", errorMsg);
                response.put("heartRate", 0.0);
                response.put("validFrames", detection.attributes().get("validFrames"));
                response.put("totalFrames", detection.attributes().get("totalFrames"));
                response.put("processingTimeMs", System.currentTimeMillis() - startTime);
                return response;
            }

            Double heartRate = (Double) detection.attributes().get("heartRate");
            String signalQuality = (String) detection.attributes().get("signalQuality");
            String bpmRange = (String) detection.attributes().get("bpmRange");
            Integer framesAnalyzed = (Integer) detection.attributes().get("framesAnalyzed");
            Integer validFrames = (Integer) detection.attributes().get("validFrames");
            Double duration = (Double) detection.attributes().get("duration");
            String method = (String) detection.attributes().get("method");

            long processingTime = System.currentTimeMillis() - startTime;
            response.put("status", "success");
            response.put("heartRate", heartRate);
            response.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);
            response.put("signalQuality", signalQuality);
            response.put("bpmRange", bpmRange);
            response.put("framesAnalyzed", framesAnalyzed);
            response.put("validFrames", validFrames);
            response.put("duration", Math.round((duration != null ? duration : 0.0) * 100.0) / 100.0);
            response.put("method", method);
            response.put("processingTimeMs", processingTime);
            response.put("disclaimer", "NOT A MEDICAL DEVICE - Research/wellness use only");
            response.put("warning", "Accuracy varies with lighting, motion, and individual factors");
            
            return response;

        } catch (IllegalArgumentException e) {
            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "error");
            response.put("message", e.getMessage());
            response.put("heartRate", 0.0);
            response.put("processingTimeMs", duration);
            log.error("Heart rate estimation validation error: {}", e.getMessage());
            return response;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "error");
            response.put("message", "Failed to estimate heart rate: " + e.getMessage());
            response.put("heartRate", 0.0);
            response.put("processingTimeMs", duration);
            log.error("Failed to estimate heart rate", e);
            return response;
        }
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
            
            // Use VisionTemplate high-level API
            VisionResult result = visionTemplate.detectFaces(imgData);

            long duration = System.currentTimeMillis() - startTime;

            response.put("status", "success");
            response.put("count", result.detectionCount());
            response.put("averageConfidence", Math.round(result.averageConfidence() * 10000.0) / 10000.0);
            response.put("processingTimeMs", duration);
            response.put("message", String.format("Detected %d faces", result.detectionCount()));
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
            
            // Use VisionTemplate high-level API
            List<float[]> rawEmbeddings = visionTemplate.extractEmbeddings(imgData,
                io.github.codesapienbe.springvision.core.DetectionCategory.FACE);
            List<Map<String, Object>> out = new ArrayList<>();

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

    /**
     * Extract metadata from an image including EXIF, GPS, and camera information.
     *
     * @param imageUrl URL of the image to extract metadata from
     * @return Map containing extracted metadata grouped by type (GPS, EXIF, etc.)
     */
    @Tool(description = "Extract metadata from an image including GPS coordinates, EXIF data, camera settings, timestamps, and more. Returns comprehensive metadata grouped by type.")
    @SuppressWarnings("unused")
    public Map<String, Object> extractImageMetadata(String imageUrl) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> response = new HashMap<>();

        log.info("Metadata extraction requested",
            StructuredArguments.keyValue("event", "metadata_extraction_start"),
            StructuredArguments.keyValue("imageUrl", sanitizeUrlForLogging(imageUrl)));

        try {
            // Download image
            byte[] imageData = downloadImageFromUrl(imageUrl);
            ImageData imgData = ImageData.fromBytes(imageData);

            // Use VisionTemplate high-level API
            VisionResult result = visionTemplate.extractMetadata(imgData);

            long duration = System.currentTimeMillis() - startTime;

            // Convert detections to response format
            Map<String, Map<String, Object>> metadataGroups = new HashMap<>();
            
            for (var detection : result.detections()) {
                String type = detection.label(); // "gps", "exif", or "metadata"
                Map<String, Object> groupData = new HashMap<>(detection.attributes());
                
                // Remove internal fields
                groupData.remove("backend");
                groupData.remove("type");
                
                metadataGroups.put(type, groupData);
            }

            response.put("status", "success");
            response.put("metadata", metadataGroups);
            response.put("groupCount", metadataGroups.size());
            response.put("processingTimeMs", duration);
            response.put("backend", result.metadata().get("backendId"));

            log.info("Metadata extraction completed",
                StructuredArguments.keyValue("event", "metadata_extraction_complete"),
                StructuredArguments.keyValue("groupCount", metadataGroups.size()),
                StructuredArguments.keyValue("processingTimeMs", duration));

            return response;

        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "error");
            response.put("message", "Failed to download image: " + e.getMessage());
            response.put("metadata", Map.of());
            response.put("processingTimeMs", duration);
            log.error("Failed to download image for metadata extraction", e);
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
            
            response.put("message", "Failed to extract metadata: " + errorMsg);
            response.put("metadata", Map.of());
            response.put("processingTimeMs", duration);
            log.error("Failed to extract metadata", e);
            return response;
        }
    }

    /**
     * Scan and decode barcodes/QR codes from an image.
     *
     * @param imageUrl URL of the image to scan
     * @return Map containing detected barcodes with format, content, and location
     */
    @Tool(description = "Scan and decode barcodes and QR codes from an image. Supports QR_CODE, EAN-13, Code-128, Data Matrix, and more. Returns barcode format, content, and location.")
    @SuppressWarnings("unused")
    public Map<String, Object> scanBarcode(String imageUrl) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> response = new HashMap<>();

        log.info("Barcode scan requested",
            StructuredArguments.keyValue("event", "barcode_scan_start"),
            StructuredArguments.keyValue("imageUrl", sanitizeUrlForLogging(imageUrl)));

        try {
            // Download image
            byte[] imageData = downloadImageFromUrl(imageUrl);
            ImageData imgData = ImageData.fromBytes(imageData);

            // Use VisionTemplate high-level API
            VisionResult result = visionTemplate.scanBarcodes(imgData);

            long duration = System.currentTimeMillis() - startTime;

            // Convert detections to response format
            List<Map<String, Object>> barcodes = new ArrayList<>();
            for (var detection : result.detections()) {
                Map<String, Object> barcodeInfo = new HashMap<>();
                barcodeInfo.put("format", detection.label());
                barcodeInfo.put("content", detection.attributes().get("content"));
                barcodeInfo.put("confidence", detection.confidence());
                
                // Add bounding box
                var bbox = detection.boundingBox();
                Map<String, Double> location = new HashMap<>();
                location.put("x", bbox.x());
                location.put("y", bbox.y());
                location.put("width", bbox.width());
                location.put("height", bbox.height());
                barcodeInfo.put("location", location);
                
                // Add metadata
                if (detection.attributes().containsKey("rawBytes")) {
                    barcodeInfo.put("rawBytesLength", detection.attributes().get("rawBytes"));
                }
                
                barcodes.add(barcodeInfo);
            }

            response.put("status", "success");
            response.put("count", result.detectionCount());
            response.put("barcodes", barcodes);
            response.put("processingTimeMs", duration);
            response.put("backend", result.metadata().get("backendId"));

            log.info("Barcode scan completed",
                StructuredArguments.keyValue("event", "barcode_scan_complete"),
                StructuredArguments.keyValue("count", barcodes.size()),
                StructuredArguments.keyValue("processingTimeMs", duration));

            return response;

        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "error");
            response.put("message", "Failed to download image: " + e.getMessage());
            response.put("barcodes", List.of());
            response.put("processingTimeMs", duration);
            log.error("Failed to download image for barcode scanning", e);
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
            
            response.put("message", "Failed to scan barcode: " + errorMsg);
            response.put("barcodes", List.of());
            response.put("processingTimeMs", duration);
            log.error("Failed to scan barcode", e);
            return response;
        }
    }

    // ============================================================================
    // Security Capabilities - Batch 4
    // ============================================================================

    /**
     * Detects security threats including weapons, violence, and suspicious objects in an image.
     *
     * <p>Analyzes images for potential security threats such as firearms, knives, violent behavior,
     * and suspicious objects. Returns detections with severity assessment and detailed threat metadata.</p>
     *
     * <p><b>Threat Types Detected:</b></p>
     * <ul>
     *   <li><b>Weapons:</b> Firearms (guns, rifles), knives, bladed weapons</li>
     *   <li><b>Violence:</b> Aggressive behavior, fighting, physical altercations</li>
     *   <li><b>Suspicious Objects:</b> Unattended packages, suspicious items</li>
     * </ul>
     *
     * <p><b>Severity Levels:</b></p>
     * <ul>
     *   <li><b>CRITICAL:</b> Firearms and active threats</li>
     *   <li><b>HIGH:</b> Knives, bladed weapons, violent behavior</li>
     *   <li><b>MEDIUM:</b> Aggressive behavior</li>
     *   <li><b>LOW:</b> Suspicious objects, potential threats</li>
     * </ul>
     *
     * <p><b>⚠️ Important:</b> This tool is designed for legitimate security and safety applications.
     * Ensure compliance with local surveillance laws, privacy regulations, and ethical AI principles.</p>
     *
     * @param imageUrl URL of the image to analyze for threats
     * @return A map containing:
     *         <ul>
     *           <li>{@code status}: "success" or "error"</li>
     *           <li>{@code threats}: List of detected threats with metadata</li>
     *           <li>{@code threatCount}: Total number of threats detected</li>
     *           <li>{@code highSeverityCount}: Number of HIGH or CRITICAL threats</li>
     *           <li>{@code processingTimeMs}: Processing time in milliseconds</li>
     *           <li>{@code disclaimer}: Legal and ethical usage disclaimer</li>
     *         </ul>
     */
    @Tool(description = """
        Detects security threats including weapons, violence, and suspicious objects in an image.
        
        Analyzes images for:
        - Firearms (guns, rifles, handguns) - CRITICAL severity
        - Knives and bladed weapons - HIGH severity
        - Violent behavior and aggression - HIGH/MEDIUM severity
        - Suspicious objects - LOW severity
        
        Returns detections with bounding boxes, severity levels, and confidence scores.
        
        ⚠️ IMPORTANT: For legitimate security use only. Comply with local laws and regulations.
        """)
    @SuppressWarnings("unused")
    public Map<String, Object> detectThreats(String imageUrl) {
        log.info("detectThreats called",
            StructuredArguments.keyValue("event", "detect_threats_start"),
            StructuredArguments.keyValue("imageUrl", imageUrl));

        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                response.put("status", "error");
                response.put("message", "Image URL is required");
                response.put("threats", List.of());
                response.put("threatCount", 0);
                return response;
            }

            // Download image
            byte[] imageBytes = downloadImageFromUrl(imageUrl);
            ImageData imageData = ImageData.fromBytes(imageBytes);

            // Use VisionTemplate high-level API
            VisionResult result = visionTemplate.detectThreats(List.of(imageData));

            // Process detections
            List<Map<String, Object>> threats = new ArrayList<>();
            int highSeverityCount = 0;

            for (var detection : result.detections()) {
                Map<String, Object> threat = new HashMap<>();
                threat.put("label", detection.label());
                threat.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);

                // Add bounding box if present
                if (detection.boundingBox() != null) {
                    Map<String, Object> bbox = new HashMap<>();
                    bbox.put("x", detection.boundingBox().x());
                    bbox.put("y", detection.boundingBox().y());
                    bbox.put("width", detection.boundingBox().width());
                    bbox.put("height", detection.boundingBox().height());
                    threat.put("boundingBox", bbox);
                }

                // Add threat metadata
                String threatType = (String) detection.attributes().get("threatType");
                String severity = (String) detection.attributes().get("severity");
                String weaponClass = (String) detection.attributes().get("weaponClass");
                String description = (String) detection.attributes().get("description");

                threat.put("threatType", threatType);
                threat.put("severity", severity);
                threat.put("weaponClass", weaponClass);
                threat.put("description", description);

                // Count high-severity threats
                if ("HIGH".equals(severity) || "CRITICAL".equals(severity)) {
                    highSeverityCount++;
                }

                threats.add(threat);
            }

            long processingTime = System.currentTimeMillis() - startTime;

            response.put("status", "success");
            response.put("threats", threats);
            response.put("threatCount", threats.size());
            response.put("highSeverityCount", highSeverityCount);
            response.put("processingTimeMs", processingTime);
            response.put("imageUrl", imageUrl);
            response.put("disclaimer", "For legitimate security and safety use only. Comply with local surveillance laws and privacy regulations.");
            response.put("warning", "False positives may occur. Human verification recommended for critical decisions.");

            log.info("detectThreats completed",
                StructuredArguments.keyValue("event", "detect_threats_complete"),
                StructuredArguments.keyValue("threatCount", threats.size()),
                StructuredArguments.keyValue("highSeverityCount", highSeverityCount),
                StructuredArguments.keyValue("processingTimeMs", processingTime));

            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "error");
            response.put("message", "Failed to detect threats: " + e.getMessage());
            response.put("threats", List.of());
            response.put("threatCount", 0);
            response.put("processingTimeMs", duration);
            log.error("Failed to detect threats", e);
            return response;
        }
    }

    /**
     * Authenticates access using biometric face recognition.
     *
     * <p>Performs identity verification by detecting a face in the image, extracting its biometric
     * features, and matching against authorized users. Returns authorization decision with confidence
     * scores and detailed metadata.</p>
     *
     * <p><b>Authentication Flow:</b></p>
     * <ol>
     *   <li>Detect face in the image</li>
     *   <li>Verify image quality and face visibility</li>
     *   <li>Extract biometric face embedding</li>
     *   <li>Match against authorized user database</li>
     *   <li>Return authorization decision</li>
     * </ol>
     *
     * <p><b>Authentication Status:</b></p>
     * <ul>
     *   <li><b>AUTHORIZED:</b> Face matched with high confidence</li>
     *   <li><b>UNAUTHORIZED:</b> No match or confidence too low</li>
     *   <li><b>ERROR:</b> No face detected, multiple faces, or poor quality</li>
     * </ul>
     *
     * <p><b>⚠️ Security Considerations:</b></p>
     * <ul>
     *   <li>Implement liveness detection to prevent photo/video spoofing</li>
     *   <li>Use multi-factor authentication (face + PIN/card)</li>
     *   <li>Log all authentication attempts for audit trail</li>
     *   <li>Encrypt biometric templates at rest and in transit</li>
     *   <li>Comply with biometric privacy laws (GDPR, BIPA, etc.)</li>
     * </ul>
     *
     * @param imageUrl URL of the image containing a face to authenticate
     * @return A map containing:
     *         <ul>
     *           <li>{@code status}: "success" or "error"</li>
     *           <li>{@code authorized}: Boolean indicating if access is granted</li>
     *           <li>{@code userId}: Matched user ID (if authorized)</li>
     *           <li>{@code userName}: Matched user name (if authorized)</li>
     *           <li>{@code confidence}: Face detection confidence</li>
     *           <li>{@code matchScore}: Similarity score with matched user</li>
     *           <li>{@code reason}: Failure reason if unauthorized</li>
     *           <li>{@code timestamp}: Authentication timestamp</li>
     *           <li>{@code processingTimeMs}: Processing time in milliseconds</li>
     *         </ul>
     */
    @Tool(description = """
        Authenticates access using biometric face recognition.
        
        Performs identity verification by:
        1. Detecting face in the image
        2. Extracting biometric features
        3. Matching against authorized users
        4. Returning authorization decision
        
        Returns:
        - authorized: true/false access decision
        - userId: matched user ID (if authorized)
        - confidence: detection confidence score
        - matchScore: similarity with matched user
        - reason: failure reason if unauthorized
        
        ⚠️ SECURITY: For production use:
        - Implement liveness detection (anti-spoofing)
        - Use multi-factor authentication
        - Maintain audit logs
        - Comply with biometric privacy laws
        """)
    @SuppressWarnings("unused")
    public Map<String, Object> authenticateAccess(String imageUrl) {
        log.info("authenticateAccess called",
            StructuredArguments.keyValue("event", "authenticate_access_start"),
            StructuredArguments.keyValue("imageUrl", imageUrl));

        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                response.put("status", "error");
                response.put("message", "Image URL is required");
                response.put("authorized", false);
                return response;
            }

            // Download image
            byte[] imageBytes = downloadImageFromUrl(imageUrl);
            ImageData imageData = ImageData.fromBytes(imageBytes);

            // Use VisionTemplate high-level API
            VisionResult result = visionTemplate.authenticateAccess(imageData);

            if (!result.hasDetections()) {
                response.put("status", "error");
                response.put("message", "Authentication failed - no results");
                response.put("authorized", false);
                response.put("processingTimeMs", System.currentTimeMillis() - startTime);
                return response;
            }

            // Get authentication result
            var authResult = result.detections().get(0);

            Boolean authorized = (Boolean) authResult.attributes().get("authorized");
            Double confidence = (Double) authResult.attributes().get("confidence");
            Double matchScore = (Double) authResult.attributes().get("matchScore");
            String timestamp = (String) authResult.attributes().get("timestamp");
            String userId = (String) authResult.attributes().get("userId");
            String userName = (String) authResult.attributes().get("userName");
            String reason = (String) authResult.attributes().get("reason");

            long processingTime = System.currentTimeMillis() - startTime;

            response.put("status", "success");
            response.put("authorized", Boolean.TRUE.equals(authorized));
            response.put("label", authResult.label());
            response.put("confidence", confidence != null ? Math.round(confidence * 10000.0) / 10000.0 : 0.0);
            response.put("matchScore", matchScore != null ? Math.round(matchScore * 10000.0) / 10000.0 : 0.0);
            response.put("timestamp", timestamp);
            response.put("processingTimeMs", processingTime);
            response.put("imageUrl", imageUrl);

            if (Boolean.TRUE.equals(authorized)) {
                response.put("userId", userId);
                response.put("userName", userName);
                response.put("message", "Access granted for user: " + userName);
            } else {
                response.put("reason", reason);
                response.put("message", "Access denied: " + reason);
            }

            // Security recommendations
            response.put("securityNote", "This is a demonstration. Production systems should implement liveness detection and multi-factor authentication.");
            response.put("privacyNote", "Ensure compliance with biometric privacy laws (GDPR, BIPA, etc.)");

            log.info("authenticateAccess completed",
                StructuredArguments.keyValue("event", "authenticate_access_complete"),
                StructuredArguments.keyValue("authorized", authorized),
                StructuredArguments.keyValue("processingTimeMs", processingTime));

            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            response.put("status", "error");
            response.put("message", "Failed to authenticate access: " + e.getMessage());
            response.put("authorized", false);
            response.put("processingTimeMs", duration);
            log.error("Failed to authenticate access", e);
            return response;
        }
    }
}
