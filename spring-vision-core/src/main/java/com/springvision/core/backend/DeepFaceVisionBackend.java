package com.springvision.core.backend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springvision.core.BackendHealthInfo;
import com.springvision.core.BoundingBox;
import com.springvision.core.Detection;
import com.springvision.core.DetectionQuery;
import com.springvision.core.DetectionType;
import com.springvision.core.ImageData;
import com.springvision.core.VisionBackend;
import com.springvision.core.VisionResult;
import com.springvision.core.exception.BaseVisionException;
import com.springvision.core.exception.VisionBackendException;
import com.springvision.core.exception.VisionProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * DeepFace-based vision backend that connects to the serengil/deepface Docker container API.
 *
 * <p>This backend provides face detection, recognition, and analysis capabilities by
 * communicating with the DeepFace API server running in a Docker container. It supports
 * multiple detection types including face detection, face recognition, age/gender analysis,
 * and emotion detection.</p>
 *
 * <p>The backend communicates with the DeepFace API using Spring WebClient and handles
 * JSON responses. It includes comprehensive error handling, timeout management, and
 * health monitoring.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * DeepFaceVisionBackend backend = new DeepFaceVisionBackend("http://localhost:5000");
 * backend.initialize();
 *
 * ImageData imageData = ImageData.fromBytes(imageBytes);
 * VisionResult result = backend.detectFaces(imageData);
 * }</pre>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 * @see VisionBackend
 */
public class DeepFaceVisionBackend implements VisionBackend, com.springvision.core.capabilities.FaceDetectionCapability {

    private static final Logger logger = LoggerFactory.getLogger(DeepFaceVisionBackend.class);

    private static final String BACKEND_ID = "deepface";
    private static final String DISPLAY_NAME = "DeepFace Vision Backend";
    private static final String VERSION = "1.0.0";

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration HEALTH_CHECK_TIMEOUT = Duration.ofSeconds(5);
    private static final int MAX_RETRIES = 3;
    private static final Duration RETRY_DELAY = Duration.ofMillis(500);

    private final String baseUrl;
    private final Duration timeout;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    private volatile boolean initialized = false;
    private volatile BackendHealthInfo.HealthStatus healthStatus = BackendHealthInfo.HealthStatus.UNKNOWN;
    private volatile String healthErrorMessage = "Backend not initialized";
    private volatile long lastHealthCheckTime = 0L;

    /**
     * Creates a new DeepFace backend with default configuration.
     *
     * <p>Uses the default DeepFace API URL (http://localhost:5000) and default timeout.</p>
     */
    public DeepFaceVisionBackend() {
        this("http://localhost:5000");
    }

    /**
     * Creates a new DeepFace backend with the specified API URL.
     *
     * @param baseUrl the base URL of the DeepFace API server
     */
    public DeepFaceVisionBackend(String baseUrl) {
        this(baseUrl, DEFAULT_TIMEOUT);
    }

    /**
     * Creates a new DeepFace backend with the specified API URL and timeout.
     *
     * @param baseUrl the base URL of the DeepFace API server
     * @param timeout the timeout for API requests
     */
    public DeepFaceVisionBackend(String baseUrl, Duration timeout) {
        this.baseUrl = Objects.requireNonNull(baseUrl, "Base URL must not be null").trim();
        this.timeout = Objects.requireNonNull(timeout, "Timeout must not be null");

        if (this.baseUrl.isEmpty()) {
            throw new IllegalArgumentException("Base URL must not be empty");
        }
        if (this.timeout.isNegative() || this.timeout.isZero()) {
            throw new IllegalArgumentException("Timeout must be positive");
        }

        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
            .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getBackendId() {
        return BACKEND_ID;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public Set<DetectionType> getSupportedDetectionTypes() {
        return Set.of(
            DetectionType.FACE,
            DetectionType.OBJECT // DeepFace can detect faces as objects
        );
    }

    @Override
    public boolean isHealthy() {
        if (!initialized) {
            return false;
        }

        // Check health every 30 seconds to avoid excessive API calls
        long now = System.currentTimeMillis();
        if (now - lastHealthCheckTime < 30000) {
            return healthStatus == BackendHealthInfo.HealthStatus.HEALTHY;
        }

        try {
            performHealthCheck();
            return healthStatus == BackendHealthInfo.HealthStatus.HEALTHY;
        } catch (Exception e) {
            logger.warn("Health check failed: {}", e.getMessage());
            healthStatus = BackendHealthInfo.HealthStatus.UNHEALTHY;
            healthErrorMessage = "Health check failed: " + e.getMessage();
            return false;
        }
    }

    @Override
    public BackendHealthInfo getHealthInfo() {
        long startTime = System.currentTimeMillis();

        try {
            if (!initialized) {
                return BackendHealthInfo.unhealthy(
                    getBackendId(),
                    "Backend not initialized",
                    "DeepFace backend has not been initialized",
                    System.currentTimeMillis() - startTime
                );
            }

            if (isHealthy()) {
                return BackendHealthInfo.healthy(
                    getBackendId(),
                    "DeepFace API is healthy and responding",
                    System.currentTimeMillis() - startTime
                );
            } else {
                return BackendHealthInfo.unhealthy(
                    getBackendId(),
                    "DeepFace API is unhealthy",
                    healthErrorMessage,
                    System.currentTimeMillis() - startTime
                );
            }
        } catch (Exception e) {
            return BackendHealthInfo.unhealthy(
                getBackendId(),
                "Health check failed",
                e.getMessage(),
                System.currentTimeMillis() - startTime
            );
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
    public void initialize() throws BaseVisionException {
        if (initialized) {
            logger.warn("DeepFace backend already initialized");
            return;
        }

        logger.info("Initializing DeepFace backend with API URL: {}", baseUrl);

        try {
            // Perform initial health check
            performHealthCheck();

            if (healthStatus != BackendHealthInfo.HealthStatus.HEALTHY) {
                throw new VisionBackendException(
                    "DeepFace API is not healthy",
                    "deepface_health_check_failed",
                    null
                );
            }

            initialized = true;
            logger.info("DeepFace backend initialized successfully");

        } catch (Exception e) {
            logger.error("Failed to initialize DeepFace backend: {}", e.getMessage(), e);
            throw new VisionBackendException(
                "Failed to initialize DeepFace backend: " + e.getMessage(),
                "deepface_initialization_failed",
                null,
                e
            );
        }
    }

    @Override
    public void shutdown() throws BaseVisionException {
        if (!initialized) {
            return;
        }

        logger.info("Shutting down DeepFace backend");
        initialized = false;
        healthStatus = BackendHealthInfo.HealthStatus.UNKNOWN;
        logger.info("DeepFace backend shut down successfully");
    }

    /**
     * Performs face analysis including age, gender, emotion, and race detection.
     *
     * @param imageData the image data to analyze
     * @return analysis results as metadata
     * @throws BaseVisionException if analysis fails
     */
    public Map<String, Object> analyzeFace(ImageData imageData) throws BaseVisionException {
        validateState();
        Objects.requireNonNull(imageData, "Image data must not be null");

        String correlationId = generateCorrelationId();
        logger.debug("Analyzing face with DeepFace API [{}]", correlationId);

        try {
            String base64Image = Base64.getEncoder().encodeToString(imageData.data());
            JsonNode response = callDeepFaceAnalyze(base64Image, correlationId);
            return parseAnalysisResults(response, correlationId);
        } catch (Exception e) {
            logger.error("Face analysis failed [{}]: {}", correlationId, e.getMessage(), e);
            throw new VisionProcessingException(
                "Face analysis failed: " + e.getMessage(),
                "deepface_analysis_error",
                DetectionType.FACE.getCode(),
                e
            );
        }
    }

    /**
     * Performs face verification between two images.
     *
     * @param image1Data the first image data
     * @param image2Data the second image data
     * @return verification result with similarity score
     * @throws BaseVisionException if verification fails
     */
    public Map<String, Object> verifyFaces(ImageData image1Data, ImageData image2Data) throws BaseVisionException {
        validateState();
        Objects.requireNonNull(image1Data, "First image data must not be null");
        Objects.requireNonNull(image2Data, "Second image data must not be null");

        String correlationId = generateCorrelationId();
        logger.debug("Verifying faces with DeepFace API [{}]", correlationId);

        try {
            String base64Image1 = Base64.getEncoder().encodeToString(image1Data.data());
            String base64Image2 = Base64.getEncoder().encodeToString(image2Data.data());

            JsonNode response = callDeepFaceVerify(base64Image1, base64Image2, correlationId);
            return parseVerificationResults(response, correlationId);
        } catch (Exception e) {
            logger.error("Face verification failed [{}]: {}", correlationId, e.getMessage(), e);
            throw new VisionProcessingException(
                "Face verification failed: " + e.getMessage(),
                "deepface_verification_error",
                DetectionType.FACE.getCode(),
                e
            );
        }
    }

    private void validateState() throws VisionBackendException {
        if (!initialized) {
            throw new VisionBackendException(
                "Backend not initialized",
                "backend_not_initialized",
                null
            );
        }
    }

    private void performHealthCheck() throws Exception {
        long startTime = System.currentTimeMillis();

        try {
            // Simple health check - try to call the analyze endpoint with minimal data
            Map<String, Object> requestBody = Map.of(
                "img_path", "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==",
                "actions", List.of("age")
            );

            webClient.post()
                .uri("/analyze")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(HEALTH_CHECK_TIMEOUT)
                .block();

            healthStatus = BackendHealthInfo.HealthStatus.HEALTHY;
            healthErrorMessage = null;
            logger.debug("Health check successful: {}ms", System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            healthStatus = BackendHealthInfo.HealthStatus.UNHEALTHY;
            healthErrorMessage = "Health check failed: " + e.getMessage();
            throw e;
        } finally {
            lastHealthCheckTime = System.currentTimeMillis();
        }
    }

    private JsonNode callDeepFaceAnalyze(String base64Image, String correlationId) throws BaseVisionException {
        Map<String, Object> requestBody = Map.of(
            "img_path", "data:image/jpeg;base64," + base64Image,
            "actions", List.of("age", "gender", "emotion", "race")
        );

        return webClient.post()
            .uri("/analyze")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .timeout(timeout)
            .map(response -> {
                try {
                    return objectMapper.readTree(response);
                } catch (JsonProcessingException e) {
                    throw new VisionProcessingException("Failed to parse DeepFace response", "deepface_parse_error", null, e);
                }
            })
            .block();
    }

    private JsonNode callDeepFaceVerify(String base64Image1, String base64Image2, String correlationId) throws BaseVisionException {
        Map<String, Object> requestBody = Map.of(
            "img1_path", "data:image/jpeg;base64," + base64Image1,
            "img2_path", "data:image/jpeg;base64," + base64Image2
        );

        return webClient.post()
            .uri("/verify")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .timeout(timeout)
            .map(response -> {
                try {
                    return objectMapper.readTree(response);
                } catch (JsonProcessingException e) {
                    throw new VisionProcessingException("Failed to parse DeepFace response", "deepface_parse_error", null, e);
                }
            })
            .block();
    }

    private List<Detection> parseFaceDetections(JsonNode response, String correlationId) {
        List<Detection> detections = new ArrayList<>();

        if (response.has("results") && response.get("results").isArray()) {
            JsonNode resultsArray = response.get("results");

            for (JsonNode result : resultsArray) {
                try {
                    Detection detection = parseFaceDetection(result);
                    detections.add(detection);
                } catch (Exception e) {
                    logger.warn("Failed to parse face detection [{}]: {}", correlationId, e.getMessage());
                }
            }
        }

        return detections;
    }

    private Detection parseFaceDetection(JsonNode result) {
        // Extract bounding box from region
        JsonNode region = result.get("region");
        double x = region.get("x").asDouble();
        double y = region.get("y").asDouble();
        double width = region.get("w").asDouble();
        double height = region.get("h").asDouble();

        // Normalize coordinates (assuming they're in pixels)
        BoundingBox boundingBox = BoundingBox.fromPixels(
            (int) x, (int) y, (int) width, (int) height,
            1000, 1000 // Default image size for normalization
        );

        // Extract confidence score
        double confidence = result.has("face_confidence") ? result.get("face_confidence").asDouble() : 0.8;

        // Extract additional attributes
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("category", com.springvision.core.DetectionCategory.FACE.name());
        if (result.has("age")) attributes.put("age", result.get("age").asInt());
        if (result.has("dominant_gender")) attributes.put("gender", result.get("dominant_gender").asText());
        if (result.has("dominant_emotion")) attributes.put("emotion", result.get("dominant_emotion").asText());
        if (result.has("dominant_race")) attributes.put("race", result.get("dominant_race").asText());

        return new Detection("face", confidence, boundingBox, attributes);
    }

    private Map<String, Object> parseAnalysisResults(JsonNode response, String correlationId) {
        Map<String, Object> results = new HashMap<>();

        if (response.has("results") && response.get("results").isArray() && response.get("results").size() > 0) {
            JsonNode firstResult = response.get("results").get(0);

            if (firstResult.has("age")) results.put("age", firstResult.get("age").asInt());
            if (firstResult.has("dominant_gender")) results.put("gender", firstResult.get("dominant_gender").asText());
            if (firstResult.has("dominant_emotion")) results.put("emotion", firstResult.get("dominant_emotion").asText());
            if (firstResult.has("dominant_race")) results.put("race", firstResult.get("dominant_race").asText());
        }

        return results;
    }

    private Map<String, Object> parseVerificationResults(JsonNode response, String correlationId) {
        Map<String, Object> results = new HashMap<>();

        if (response.has("verified")) results.put("verified", response.get("verified").asBoolean());
        if (response.has("distance")) results.put("distance", response.get("distance").asDouble());
        if (response.has("threshold")) results.put("threshold", response.get("threshold").asDouble());
        if (response.has("model")) results.put("model", response.get("model").asText());
        if (response.has("similarity_metric")) results.put("similarity_metric", response.get("similarity_metric").asText());

        return results;
    }

    private double calculateAverageConfidence(List<Detection> detections) {
        if (detections.isEmpty()) {
            return 0.0;
        }

        return detections.stream()
            .mapToDouble(Detection::confidence)
            .average()
            .orElse(0.0);
    }

    private String generateCorrelationId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Gets the base URL of the DeepFace API.
     *
     * @return the base URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Gets the timeout for API requests.
     *
     * @return the timeout duration
     */
    public Duration getTimeout() {
        return timeout;
    }

    @Override
    public String toString() {
        return String.format("DeepFaceVisionBackend{baseUrl='%s', timeout=%s, initialized=%s}",
            baseUrl, timeout, initialized);
    }
}
