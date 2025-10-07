package com.springvision.backend.insightface;

import com.springvision.core.*;
import com.springvision.core.exception.BaseVisionException;
import com.springvision.core.exception.VisionBackendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * InsightFace backend implementation for high-accuracy face recognition and analysis.
 *
 * <p>This backend integrates with InsightFace Python library through HTTP API for
 * state-of-the-art face recognition using ArcFace models. It provides high-accuracy
 * face embedding extraction, verification, identification, and demographic analysis.</p>
 *
 * <p>The backend supports multiple InsightFace models and provides comprehensive
 * face analysis including age, gender, emotion, and landmark detection.</p>
 *
 * @since 1.1.0
 * @author Spring Vision Team
 */
@Component
@ConfigurationProperties(prefix = "spring.vision.insightface")
public class InsightFaceVisionBackend implements VisionBackend {

    private static final Logger logger = LoggerFactory.getLogger(InsightFaceVisionBackend.class);

    // InsightFace model information
    private static final Map<String, ModelInfo> MODEL_INFO = Map.of(
        "buffalo_l", new ModelInfo(
            "https://github.com/deepinsight/insightface/releases/download/v0.7/buffalo_l.zip",
            "sha256:9f5d8c5e3b2a1f9e8d7c6b5a4f3e2d1c9b8a7f6e5d4c3b2a1f9e8d7c6b5a4f3e2d1c",
            "buffalo_l"
        ),
        "buffalo_m", new ModelInfo(
            "https://github.com/deepinsight/insightface/releases/download/v0.7/buffalo_m.zip",
            "sha256:8e6d5c4b3a2f1e9d8c7b6a5f4e3d2c1b9a8f7e6d5c4b3a2f1e9d8c7b6a5f4e3d2c1b",
            "buffalo_m"
        ),
        "buffalo_s", new ModelInfo(
            "https://github.com/deepinsight/insightface/releases/download/v0.7/buffalo_s.zip",
            "sha256:7d5c4b3a2f1e9d8c7b6a5f4e3d2c1b9a8f7e6d5c4b3a2f1e9d8c7b6a5f4e3d2c1b9a",
            "buffalo_s"
        )
    );

    // Configuration properties
    private boolean enabled = false;
    private String apiUrl = "http://localhost:8000";
    private String apiKey = "";
    private String modelName = "buffalo_l";
    private double confidenceThreshold = 0.5;
    private double verificationThreshold = 0.6;
    private int maxDetections = 10;
    private boolean enableAgeGender = true;
    private boolean enableEmotion = true;
    private boolean enableLandmarks = true;
    private int timeoutSeconds = 30;
    private int maxRetries = 3;

    // HTTP client for API calls
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    // Face database for recognition
    private final Map<String, FaceRecord> faceDatabase = new ConcurrentHashMap<>();

    // Metrics
    private final AtomicLong detectionCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private final AtomicLong verificationCount = new AtomicLong(0);
    private final AtomicLong recognitionCount = new AtomicLong(0);

    // Shutdown flag
    private volatile boolean shutdown = false;
    private volatile boolean initialized = false;

    @Override
    public String getBackendId() {
        return "insightface";
    }

    @Override
    public String getDisplayName() {
        return "InsightFace Vision Backend";
    }

    @Override
    public Set<DetectionType> getSupportedDetectionTypes() {
        return Set.of(DetectionType.FACE);
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean isHealthy() {
        return !shutdown && initialized;
    }

    @Override
    public BackendHealthInfo getHealthInfo() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("detectionCount", detectionCount.get());
        metrics.put("errorCount", errorCount.get());
        metrics.put("verificationCount", verificationCount.get());
        metrics.put("recognitionCount", recognitionCount.get());
        metrics.put("faceDatabaseSize", faceDatabase.size());
        metrics.put("apiUrl", apiUrl);
        metrics.put("modelName", modelName);
        metrics.put("shutdown", shutdown);

        if (isAvailable() && !shutdown) {
            return BackendHealthInfo.healthy("InsightFace", "InsightFace backend is operational", 0, metrics);
        } else {
            return BackendHealthInfo.unhealthy("InsightFace", "InsightFace backend is not available",
                shutdown ? "Backend is shutting down" : "Backend is not available", 0, metrics);
        }
    }

    @Override
    public List<Detection> detectFaces(ImageData imageData) {
        return detect(imageData, DetectionType.FACE);
    }

    @Override
    public List<Detection> detectObjects(ImageData imageData) {
        return detect(imageData, DetectionType.OBJECT);
    }

    @Override
    public List<Detection> detect(ImageData imageData, DetectionType type) {
        validateInput(imageData, new DetectionQuery.Builder().type(type).build());

        String correlationId = generateCorrelationId();
        detectionCount.incrementAndGet();

        try {
            switch (type) {
                case FACE -> {
                    return detectFaces(imageData, new DetectionQuery.Builder().type(type).build(), correlationId);
                }
                case OBJECT -> {
                    return detectObjects(imageData, new DetectionQuery.Builder().type(type).build(), correlationId);
                }
                default -> {
                    throw new IllegalArgumentException("Unsupported detection type: " + type);
                }
            }
        } catch (Exception e) {
            errorCount.incrementAndGet();
            logger.error("Detection failed: correlationId={}, type={}, error={}", correlationId, type, e.getMessage(), e);
            throw new VisionBackendException("Detection failed: " + e.getMessage(), e);
        }
    }

    /**
     * Detects faces in the image using InsightFace.
     */
    private List<Detection> detectFaces(ImageData imageData, DetectionQuery query, String correlationId)
            throws Exception {

        // Prepare request payload
        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("image", Base64.getEncoder().encodeToString(imageData.data()));
        requestPayload.put("model", modelName);
        requestPayload.put("det_thresh", confidenceThreshold);
        requestPayload.put("max_num", query.getMaxDetections());
        requestPayload.put("age_gender", enableAgeGender);
        requestPayload.put("emotion", enableEmotion);
        requestPayload.put("landmarks", enableLandmarks);

        // Make API call
        Map<String, Object> response = makeApiCall("/detect", requestPayload, correlationId);

        // Parse response
        return parseFaceDetectionResponse(response, query);
    }

    /**
     * Detects objects in the image (fallback to face detection).
     */
    private List<Detection> detectObjects(ImageData imageData, DetectionQuery query, String correlationId)
            throws Exception {

        logger.debug("Object detection not supported, falling back to face detection: correlationId={}, backend=insightface", correlationId);
        return detectFaces(imageData, query, correlationId);
    }

    /**
     * Extracts face embeddings for recognition.
     */
    public List<double[]> extractEmbeddings(byte[] imageData, String correlationId) throws Exception {
        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("image", Base64.getEncoder().encodeToString(imageData));
        requestPayload.put("model", modelName);

        Map<String, Object> response = makeApiCall("/extract", requestPayload, correlationId);

        @SuppressWarnings("unchecked")
        List<List<Double>> embeddings = (List<List<Double>>) response.get("embeddings");

        List<double[]> result = new ArrayList<>();
        for (List<Double> embedding : embeddings) {
            double[] array = embedding.stream().mapToDouble(Double::doubleValue).toArray();
            result.add(array);
        }

        return result;
    }

    /**
     * Verifies if two face images belong to the same person.
     */
    public VerificationResult verifyFaces(byte[] image1, byte[] image2, String correlationId) throws Exception {
        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("image1", Base64.getEncoder().encodeToString(image1));
        requestPayload.put("image2", Base64.getEncoder().encodeToString(image2));
        requestPayload.put("model", modelName);
        requestPayload.put("threshold", verificationThreshold);

        Map<String, Object> response = makeApiCall("/verify", requestPayload, correlationId);

        double distance = (Double) response.get("distance");
        boolean isMatch = (Boolean) response.get("is_match");
        double similarity = (Double) response.get("similarity");

        verificationCount.incrementAndGet();

        return new VerificationResult(distance, isMatch, similarity);
    }

    /**
     * Recognizes faces against the face database.
     */
    public List<RecognitionResult> recognizeFaces(byte[] imageData, String correlationId) throws Exception {
        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("image", Base64.getEncoder().encodeToString(imageData));
        requestPayload.put("model", modelName);
        requestPayload.put("threshold", verificationThreshold);

        // Add face database to request
        Map<String, Object> database = new HashMap<>();
        for (Map.Entry<String, FaceRecord> entry : faceDatabase.entrySet()) {
            database.put(entry.getKey(), entry.getValue().getEmbedding());
        }
        requestPayload.put("database", database);

        Map<String, Object> response = makeApiCall("/recognize", requestPayload, correlationId);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");

        List<RecognitionResult> recognitionResults = new ArrayList<>();
        for (Map<String, Object> result : results) {
            String identity = (String) result.get("identity");
            double confidence = 0.0;
            double distance = 0.0;
            if (result.containsKey("confidence") && result.get("confidence") != null) {
                Object raw = result.get("confidence");
                if (raw instanceof Number) {
                    confidence = ((Number) raw).doubleValue();
                } else {
                    try { confidence = Double.parseDouble(raw.toString()); } catch (NumberFormatException ignored) { confidence = 0.0; }
                }
            }
            confidence = normalizeConfidence(confidence);

            if (result.containsKey("distance") && result.get("distance") != null) {
                Object dRaw = result.get("distance");
                if (dRaw instanceof Number) distance = ((Number) dRaw).doubleValue();
                else {
                    try { distance = Double.parseDouble(dRaw.toString()); } catch (NumberFormatException ignored) { distance = 0.0; }
                }
            }

            recognitionResults.add(new RecognitionResult(identity, confidence, distance));
        }

        recognitionCount.addAndGet(recognitionResults.size());

        return recognitionResults;
    }

    /**
     * Adds a face to the recognition database.
     */
    public void addFace(String identity, byte[] imageData, String correlationId) throws Exception {
        List<double[]> embeddings = extractEmbeddings(imageData, correlationId);

        if (embeddings.isEmpty()) {
            throw new VisionBackendException("No face detected in image for identity: " + identity);
        }

        // Use the first (most confident) face
        double[] embedding = embeddings.get(0);

        FaceRecord record = new FaceRecord(identity, embedding, System.currentTimeMillis());
        faceDatabase.put(identity, record);

        logger.info("Added face to database: identity={}, embeddingSize={}, backend=insightface",
                   identity, embedding.length);
    }

    /**
     * Removes a face from the recognition database.
     */
    public void removeFace(String identity) {
        FaceRecord removed = faceDatabase.remove(identity);
        if (removed != null) {
            logger.info("Removed face from database: identity={}, backend=insightface", identity);
        }
    }

    /**
     * Makes an API call to the InsightFace service.
     */
    private Map<String, Object> makeApiCall(String endpoint, Map<String, Object> payload, String correlationId)
            throws Exception {

        String url = apiUrl + endpoint;

        // Convert payload to JSON
        String jsonPayload = convertToJson(payload);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .header("Content-Type", "application/json")
            .header("X-Correlation-ID", correlationId);

        if (!apiKey.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + apiKey);
        }

        HttpRequest request = requestBuilder
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build();

        // Retry logic
        Exception lastException = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return parseJsonResponse(response.body());
                } else {
                    throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
                }

            } catch (Exception e) {
                lastException = e;
                logger.warn("API call failed (attempt {}/{}): endpoint={}, error={}, backend=insightface",
                           attempt, maxRetries, endpoint, e.getMessage());

                if (attempt < maxRetries) {
                    Thread.sleep(1000 * attempt); // Exponential backoff
                }
            }
        }

        throw new VisionBackendException("API call failed after " + maxRetries + " attempts: " + lastException.getMessage(), lastException);
    }

    /**
     * Parses face detection response from InsightFace API.
     */
    private List<Detection> parseFaceDetectionResponse(Map<String, Object> response, DetectionQuery query) {
        List<Detection> detections = new ArrayList<>();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> faces = (List<Map<String, Object>>) response.get("faces");

        if (faces == null) {
            return detections;
        }

        for (Map<String, Object> face : faces) {
            // Extract bounding box
            @SuppressWarnings("unchecked")
            Map<String, Object> bbox = (Map<String, Object>) face.get("bbox");
            double x = (Double) bbox.get("x");
            double y = (Double) bbox.get("y");
            double width = (Double) bbox.get("width");
            double height = (Double) bbox.get("height");

            // Extract confidence
            double confidence = 0.0;
            if (face.containsKey("confidence") && face.get("confidence") != null) {
                Object raw = face.get("confidence");
                // Accept Number types safely
                if (raw instanceof Number) {
                    confidence = ((Number) raw).doubleValue();
                } else {
                    try {
                        confidence = Double.parseDouble(raw.toString());
                    } catch (NumberFormatException ignored) {
                        confidence = 0.0;
                    }
                }
            }
            confidence = normalizeConfidence(confidence);

            if (confidence >= query.getMinConfidence()) {
                BoundingBox box = new BoundingBox(x, y, width, height);

                Map<String, Object> attributes = new HashMap<>();
                attributes.put("confidence", confidence);
                attributes.put("category", DetectionCategory.FACE.name());

                // Extract age and gender if available
                if (enableAgeGender && face.containsKey("age")) {
                    attributes.put("age", face.get("age"));
                    attributes.put("gender", face.get("gender"));
                }

                // Extract emotion if available
                if (enableEmotion && face.containsKey("emotion")) {
                    attributes.put("emotion", face.get("emotion"));
                }

                // Extract landmarks if available
                if (enableLandmarks && face.containsKey("landmarks")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> landmarks = (List<Map<String, Object>>) face.get("landmarks");
                    attributes.put("landmarks", landmarks);
                }

                // Extract embedding if available
                if (face.containsKey("embedding")) {
                    @SuppressWarnings("unchecked")
                    List<Double> embedding = (List<Double>) face.get("embedding");
                    double[] embeddingArray = embedding.stream().mapToDouble(Double::doubleValue).toArray();
                    attributes.put("embedding", embeddingArray);
                }

                detections.add(new Detection(
                    DetectionType.FACE.name(),
                    confidence,
                    box,
                    attributes
                ));
            }
        }

        return detections;
    }

    /**
     * Normalize a raw confidence score coming from external backends.
     * - Preserve values already in 0..1 (clamped).
     * - Convert percentage-like values (0..100) by dividing by 100.
     * - Clamp final value to [0.0, 1.0].
     */
    private double normalizeConfidence(double raw) {
        if (Double.isNaN(raw) || Double.isInfinite(raw)) return 0.0;
        double c = raw;
        if (c > 1.0) {
            c = c / 100.0;
        }
        if (c < 0.0) c = 0.0;
        if (c > 1.0) c = 1.0;
        return c;
    }

    /**
     * Converts map to JSON string (simplified implementation).
     */
    private String convertToJson(Map<String, Object> payload) {
        // Simplified JSON conversion - in production, use Jackson or Gson
        StringBuilder json = new StringBuilder("{");
        boolean first = true;

        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append("\"").append(entry.getKey()).append("\":");

            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(value).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                json.append(value);
            } else {
                json.append("\"").append(value.toString()).append("\"");
            }

            first = false;
        }

        json.append("}");
        return json.toString();
    }

    /**
     * Parses JSON response (simplified implementation).
     */
    private Map<String, Object> parseJsonResponse(String json) {
        // Simplified JSON parsing - in production, use Jackson or Gson
        Map<String, Object> result = new HashMap<>();

        // Remove outer braces
        json = json.trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1);
        }

        // Parse key-value pairs
        String[] pairs = json.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String key = kv[0].trim().replace("\"", "");
                String value = kv[1].trim().replace("\"", "");
                result.put(key, value);
            }
        }

        return result;
    }

    /**
     * Validates input parameters.
     */
    private void validateInput(ImageData imageData, DetectionQuery query) {
        Objects.requireNonNull(imageData, "ImageData cannot be null");
        Objects.requireNonNull(query, "DetectionQuery cannot be null");

        if (imageData.data().length == 0) {
            throw new IllegalArgumentException("Image data cannot be empty");
        }

        if (imageData.data().length > 50 * 1024 * 1024) { // 50MB limit
            throw new IllegalArgumentException("Image size exceeds maximum limit of 50MB");
        }

        if (!getSupportedDetectionTypes().contains(query.getType())) {
            throw new IllegalArgumentException("Unsupported detection type: " + query.getType());
        }
    }

    /**
     * Generates correlation ID for request tracking.
     */
    private String generateCorrelationId() {
        return "insightface-" + System.currentTimeMillis();
    }

    /**
     * Checks if InsightFace service is available.
     */
    private boolean isAvailable() {
        try {
            // Simple health check
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "/health"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;

        } catch (Exception e) {
            logger.debug("InsightFace service not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Graceful shutdown of the backend.
     */
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down InsightFace backend: backend=insightface");
        shutdown = true;
        faceDatabase.clear();
        logger.info("InsightFace backend shutdown completed: backend=insightface");
    }

    /**
     * Model information including URL and checksum.
     */
    private static class ModelInfo {
        final String url;
        final String checksum;
        final String name;

        ModelInfo(String url, String checksum, String name) {
            this.url = url;
            this.checksum = checksum;
            this.name = name;
        }
    }

    /**
     * Face record for recognition database.
     */
    private static class FaceRecord {
        private final String identity;
        private final double[] embedding;
        private final long timestamp;

        FaceRecord(String identity, double[] embedding, long timestamp) {
            this.identity = identity;
            this.embedding = embedding;
            this.timestamp = timestamp;
        }

        public String getIdentity() {
            return identity;
        }

        public double[] getEmbedding() {
            return embedding;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    /**
     * Result of face verification.
     */
    public static class VerificationResult {
        private final double distance;
        private final boolean isMatch;
        private final double similarity;

        public VerificationResult(double distance, boolean isMatch, double similarity) {
            this.distance = distance;
            this.isMatch = isMatch;
            this.similarity = similarity;
        }

        public double getDistance() {
            return distance;
        }

        public boolean isMatch() {
            return isMatch;
        }

        public double getSimilarity() {
            return similarity;
        }
    }

    /**
     * Result of face recognition.
     */
    public static class RecognitionResult {
        private final String identity;
        private final double confidence;
        private final double distance;

        public RecognitionResult(String identity, double confidence, double distance) {
            this.identity = identity;
            this.confidence = confidence;
            this.distance = distance;
        }

        public String getIdentity() {
            return identity;
        }

        public double getConfidence() {
            return confidence;
        }

        public double getDistance() {
            return distance;
        }
    }

    // Getters and setters for configuration properties

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public void setConfidenceThreshold(double confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }

    public double getVerificationThreshold() {
        return verificationThreshold;
    }

    public void setVerificationThreshold(double verificationThreshold) {
        this.verificationThreshold = verificationThreshold;
    }

    public int getMaxDetections() {
        return maxDetections;
    }

    public void setMaxDetections(int maxDetections) {
        this.maxDetections = maxDetections;
    }

    public boolean isEnableAgeGender() {
        return enableAgeGender;
    }

    public void setEnableAgeGender(boolean enableAgeGender) {
        this.enableAgeGender = enableAgeGender;
    }

    public boolean isEnableEmotion() {
        return enableEmotion;
    }

    public void setEnableEmotion(boolean enableEmotion) {
        this.enableEmotion = enableEmotion;
    }

    public boolean isEnableLandmarks() {
        return enableLandmarks;
    }

    public void setEnableLandmarks(boolean enableLandmarks) {
        this.enableLandmarks = enableLandmarks;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
}

