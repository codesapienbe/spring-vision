package com.springvision.deepface;

import com.springvision.core.*;
import com.springvision.core.capabilities.FaceDetectionCapability;
import com.springvision.core.capabilities.EmbeddingCapability;
import com.springvision.core.exception.BaseVisionException;
import com.springvision.core.exception.VisionBackendException;
import com.springvision.core.exception.VisionProcessingException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.*;

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
 * DeepFaceBackend backend = new DeepFaceBackend("http://localhost:5000");
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
@Component
@ConfigurationProperties(prefix = "deepface")
public class DeepFaceBackend implements VisionBackend, FaceDetectionCapability, EmbeddingCapability {

    private static final Logger logger = LoggerFactory.getLogger(DeepFaceBackend.class);

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
    public DeepFaceBackend() {
        this("http://localhost:5000");
    }

    /**
     * Creates a new DeepFace backend with the specified API URL.
     *
     * @param baseUrl the base URL of the DeepFace API server
     */
    public DeepFaceBackend(String baseUrl) {
        this(baseUrl, DEFAULT_TIMEOUT);
    }

    /**
     * Creates a new DeepFace backend with the specified API URL and timeout.
     *
     * @param baseUrl the base URL of the DeepFace API server
     * @param timeout the timeout for API requests
     */
    public DeepFaceBackend(String baseUrl, Duration timeout) {
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
                return new BackendHealthInfo(
                    getBackendId(),
                    BackendHealthInfo.HealthStatus.UNHEALTHY,
                    "Backend not initialized",
                    "DeepFace backend has not been initialized",
                    java.time.Instant.now(),
                    System.currentTimeMillis() - startTime,
                    java.util.Collections.emptyMap()
                );
            }

            if (isHealthy()) {
                return new BackendHealthInfo(
                    getBackendId(),
                    BackendHealthInfo.HealthStatus.HEALTHY,
                    "DeepFace API is healthy and responding",
                    null,
                    java.time.Instant.now(),
                    System.currentTimeMillis() - startTime,
                    java.util.Collections.emptyMap()
                );
            } else {
                return new BackendHealthInfo(
                    getBackendId(),
                    BackendHealthInfo.HealthStatus.UNHEALTHY,
                    "DeepFace API is unhealthy",
                    healthErrorMessage,
                    java.time.Instant.now(),
                    System.currentTimeMillis() - startTime,
                    java.util.Collections.emptyMap()
                );
            }
        } catch (Exception e) {
            return new BackendHealthInfo(
                getBackendId(),
                BackendHealthInfo.HealthStatus.UNHEALTHY,
                "Health check failed",
                e.getMessage(),
                java.time.Instant.now(),
                System.currentTimeMillis() - startTime,
                java.util.Collections.emptyMap()
            );
        }
    }

    @Override
    public List<Detection> detectFaces(ImageData imageData) {
        validateState();
        Objects.requireNonNull(imageData, "Image data must not be null");

        String correlationId = generateCorrelationId();
        logger.debug("Detecting faces with DeepFace API [{}]", correlationId);

        try {
            String base64Image = Base64.getEncoder().encodeToString(imageData.data());
            JsonNode response = callDeepFaceAnalyze(base64Image, correlationId);
            List<Detection> detections = parseFaceDetections(response, correlationId);

            logger.info("Detected {} faces using DeepFace backend [{}]", detections.size(), correlationId);
            return detections;

        } catch (Exception e) {
            logger.error("Face detection failed [{}]: {}", correlationId, e.getMessage(), e);
            throw new VisionProcessingException(
                "Face detection failed: " + e.getMessage(),
                "deepface_detection_error",
                DetectionType.FACE.name(),
                e
            );
        }
    }

    @Override
    public List<Detection> detectObjects(ImageData imageData) {
        // DeepFace can detect faces as objects, so reuse face detection
        return detectFaces(imageData);
    }

    @Override
    public List<float[]> extractEmbeddings(ImageData imageData, DetectionCategory subject) {
        // TODO: Implement embedding extraction from DeepFace
        // This would call the DeepFace API to get face embeddings
        return List.of();
    }

    @Override
    public boolean verify(ImageData image1, ImageData image2, String metric, double threshold) {
        try {
            // Create multipart form data
            MultiValueMap<String, Object> parts = new org.springframework.util.LinkedMultiValueMap<>();
            parts.add("img1_path", new ByteArrayResource(image1.data()) {
                @Override
                public String getFilename() {
                    return "image1.jpg";
                }
            });
            parts.add("img2_path", new ByteArrayResource(image2.data()) {
                @Override
                public String getFilename() {
                    return "image2.jpg";
                }
            });

            String response = webClient.post()
                .uri("/verify")
                .body(BodyInserters.fromMultipartData(parts))
                .retrieve()
                .bodyToMono(String.class)
                .block();

            Map<String, Object> json = objectMapper.readValue(response, Map.class);
            boolean verified = (Boolean) json.get("verified");
            double distance = ((Number) json.get("distance")).doubleValue();
            // Assuming distance is lower for similar faces
            return verified && distance < threshold;
        } catch (Exception e) {
            throw new VisionProcessingException("Failed to verify with DeepFace", e);
        }
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
        attributes.put("category", DetectionCategory.FACE.name());
        if (result.has("age")) attributes.put("age", result.get("age").asInt());
        if (result.has("dominant_gender")) attributes.put("gender", result.get("dominant_gender").asText());
        if (result.has("dominant_emotion")) attributes.put("emotion", result.get("dominant_emotion").asText());
        if (result.has("dominant_race")) attributes.put("race", result.get("dominant_race").asText());

        return new Detection("face", confidence, boundingBox, attributes);
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
        return String.format("DeepFaceBackend{baseUrl='%s', timeout=%s, initialized=%s}",
            baseUrl, timeout, initialized);
    }
}
