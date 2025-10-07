package com.springvision.backend.compreface;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springvision.core.BackendHealthInfo;
import com.springvision.core.BoundingBox;
import com.springvision.core.Detection;
import com.springvision.core.DetectionType;
import com.springvision.core.ImageData;
import com.springvision.core.VisionBackend;
import com.springvision.core.exception.BaseVisionException;
import com.springvision.core.exception.VisionBackendException;
import com.springvision.core.exception.VisionProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.Duration;
import java.util.*;

/**
 * CompreFace-based vision backend that connects to the exadel/compreface Docker container API.
 *
 * <p>This backend provides face detection, recognition, and analysis capabilities by
 * communicating with the CompreFace API server running in a Docker container. It supports
 * multiple detection types including face detection, face recognition, and face analysis.</p>
 *
 * <p>The backend communicates with the CompreFace API using Spring WebClient and handles
 * JSON responses. It includes comprehensive error handling, timeout management, and
 * health monitoring.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * CompreFaceVisionBackend backend = new CompreFaceVisionBackend("http://localhost:8000");
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
public class CompreFaceVisionBackend implements VisionBackend {

    private static final Logger logger = LoggerFactory.getLogger(CompreFaceVisionBackend.class);

    private static final String BACKEND_ID = "compreface";
    private static final String DISPLAY_NAME = "CompreFace Vision Backend";
    private static final String VERSION = "1.0.0";

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration HEALTH_CHECK_TIMEOUT = Duration.ofSeconds(10); // Increased for CompreFace startup
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
     * Creates a new CompreFace backend with default configuration.
     *
     * <p>Uses the default CompreFace API URL (http://localhost:8000) and default timeout.</p>
     */
    public CompreFaceVisionBackend() {
        this("http://localhost:8000");
    }

    /**
     * Creates a new CompreFace backend with the specified API URL.
     *
     * @param baseUrl the base URL of the CompreFace API server
     */
    public CompreFaceVisionBackend(String baseUrl) {
        this(baseUrl, DEFAULT_TIMEOUT);
    }

    /**
     * Creates a new CompreFace backend with the specified API URL and timeout.
     *
     * @param baseUrl the base URL of the CompreFace API server
     * @param timeout the timeout for API requests
     */
    public CompreFaceVisionBackend(String baseUrl, Duration timeout) {
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
            DetectionType.OBJECT // CompreFace can detect faces as objects
        );
    }

    @Override
    public boolean isHealthy() {
        if (!initialized) {
            return false;
        }

        // Check health every 60 seconds to avoid excessive API calls (CompreFace is slower)
        long now = System.currentTimeMillis();
        if (now - lastHealthCheckTime < 60000) {
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
                    "CompreFace backend has not been initialized",
                    System.currentTimeMillis() - startTime
                );
            }

            if (isHealthy()) {
                return BackendHealthInfo.healthy(
                    getBackendId(),
                    "CompreFace API is healthy and responding",
                    System.currentTimeMillis() - startTime
                );
            } else {
                return BackendHealthInfo.unhealthy(
                    getBackendId(),
                    "CompreFace API is unhealthy",
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
        validateState();
        Objects.requireNonNull(imageData, "Image data must not be null");

        String correlationId = generateCorrelationId();
        logger.debug("Detecting faces with CompreFace API [{}]", correlationId);

        try {
            String base64Image = Base64.getEncoder().encodeToString(imageData.data());
            JsonNode response = callCompreFaceDetect(base64Image, correlationId);
            List<Detection> detections = parseFaceDetections(response, correlationId);

            logger.info("Detected {} faces using CompreFace backend [{}]", detections.size(), correlationId);
            return detections;

        } catch (Exception e) {
            logger.error("Face detection failed [{}]: {}", correlationId, e.getMessage(), e);
            throw new VisionProcessingException(
                "Face detection failed: " + e.getMessage(),
                "compreface_detection_error",
                DetectionType.FACE.getCode(),
                e
            );
        }
    }

    @Override
    public List<Detection> detectObjects(ImageData imageData) {
        // CompreFace primarily supports face detection
        logger.debug("Object detection not supported by CompreFace backend");
        return Collections.emptyList();
    }

    @Override
    public void initialize() throws BaseVisionException {
        if (initialized) {
            logger.warn("CompreFace backend already initialized");
            return;
        }

        logger.info("Initializing CompreFace backend with API URL: {}", baseUrl);
        logger.info("Note: CompreFace may take up to 45 seconds to start. Please be patient...");

        try {
            // Wait for CompreFace to be ready (with retries)
            waitForCompreFaceStartup();

            // Perform initial health check
            performHealthCheck();

            if (healthStatus != BackendHealthInfo.HealthStatus.HEALTHY) {
                throw new VisionBackendException(
                    "CompreFace API is not healthy",
                    "compreface_health_check_failed",
                    null
                );
            }

            initialized = true;
            logger.info("CompreFace backend initialized successfully");

        } catch (Exception e) {
            logger.error("Failed to initialize CompreFace backend: {}", e.getMessage(), e);
            throw new VisionBackendException(
                "Failed to initialize CompreFace backend: " + e.getMessage(),
                "compreface_initialization_failed",
                null,
                e
            );
        }
    }

    /**
     * Waits for CompreFace to be ready for requests.
     * CompreFace takes approximately 45 seconds to start up.
     */
    private void waitForCompreFaceStartup() throws Exception {
        int maxAttempts = 12; // 12 attempts * 5 seconds = 60 seconds total
        int attempt = 0;

        while (attempt < maxAttempts) {
            try {
                logger.debug("Checking CompreFace startup status (attempt {}/{})", attempt + 1, maxAttempts);

                // Try a simple health check
                webClient.get()
                    .uri("/api/v1/recognition/detect")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

                logger.info("CompreFace is ready for requests");
                return;

            } catch (Exception e) {
                attempt++;
                if (attempt >= maxAttempts) {
                    throw new Exception("CompreFace failed to start within expected time: " + e.getMessage(), e);
                }

                logger.debug("CompreFace not ready yet (attempt {}/{}), waiting 5 seconds...", attempt, maxAttempts);
                Thread.sleep(5000); // Wait 5 seconds between attempts
            }
        }
    }

    @Override
    public void shutdown() throws BaseVisionException {
        if (!initialized) {
            return;
        }

        logger.info("Shutting down CompreFace backend");
        initialized = false;
        healthStatus = BackendHealthInfo.HealthStatus.UNKNOWN;
        logger.info("CompreFace backend shut down successfully");
    }

    /**
     * Performs face recognition to identify known faces.
     *
     * @param imageData the image data to recognize
     * @return recognition results with identified faces
     * @throws BaseVisionException if recognition fails
     */
    public Map<String, Object> recognizeFaces(ImageData imageData) throws BaseVisionException {
        validateState();
        Objects.requireNonNull(imageData, "Image data must not be null");

        String correlationId = generateCorrelationId();
        logger.debug("Recognizing faces with CompreFace API [{}]", correlationId);

        try {
            String base64Image = Base64.getEncoder().encodeToString(imageData.data());
            JsonNode response = callCompreFaceRecognize(base64Image, correlationId);
            return parseRecognitionResults(response, correlationId);
        } catch (Exception e) {
            logger.error("Face recognition failed [{}]: {}", correlationId, e.getMessage(), e);
            throw new VisionProcessingException(
                "Face recognition failed: " + e.getMessage(),
                "compreface_recognition_error",
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
        logger.debug("Verifying faces with CompreFace API [{}]", correlationId);

        try {
            String base64Image1 = Base64.getEncoder().encodeToString(image1Data.data());
            String base64Image2 = Base64.getEncoder().encodeToString(image2Data.data());

            JsonNode response = callCompreFaceVerify(base64Image1, base64Image2, correlationId);
            return parseVerificationResults(response, correlationId);
        } catch (Exception e) {
            logger.error("Face verification failed [{}]: {}", correlationId, e.getMessage(), e);
            throw new VisionProcessingException(
                "Face verification failed: " + e.getMessage(),
                "compreface_verification_error",
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
            // Simple health check - try to call the detect endpoint with minimal data
            // Note: CompreFace may take longer to respond during startup
            Map<String, Object> requestBody = Map.of(
                "file", "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg=="
            );

            webClient.post()
                .uri("/api/v1/recognition/detect")
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

    private JsonNode callCompreFaceDetect(String base64Image, String correlationId) throws BaseVisionException {
        Map<String, Object> requestBody = Map.of(
            "file", "data:image/jpeg;base64," + base64Image
        );

        return webClient.post()
            .uri("/api/v1/recognition/detect")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .timeout(timeout)
            .map(response -> {
                try {
                    return objectMapper.readTree(response);
                } catch (JsonProcessingException e) {
                    throw new VisionProcessingException("Failed to parse CompreFace response", "compreface_parse_error", null, e);
                }
            })
            .block();
    }

    private JsonNode callCompreFaceRecognize(String base64Image, String correlationId) throws BaseVisionException {
        Map<String, Object> requestBody = Map.of(
            "file", "data:image/jpeg;base64," + base64Image
        );

        return webClient.post()
            .uri("/api/v1/recognition/recognize")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .timeout(timeout)
            .map(response -> {
                try {
                    return objectMapper.readTree(response);
                } catch (JsonProcessingException e) {
                    throw new VisionProcessingException("Failed to parse CompreFace response", "compreface_parse_error", null, e);
                }
            })
            .block();
    }

    private JsonNode callCompreFaceVerify(String base64Image1, String base64Image2, String correlationId) throws BaseVisionException {
        Map<String, Object> requestBody = Map.of(
            "source_image", "data:image/jpeg;base64," + base64Image1,
            "target_image", "data:image/jpeg;base64," + base64Image2
        );

        return webClient.post()
            .uri("/api/v1/verification/verify")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .timeout(timeout)
            .map(response -> {
                try {
                    return objectMapper.readTree(response);
                } catch (JsonProcessingException e) {
                    throw new VisionProcessingException("Failed to parse CompreFace response", "compreface_parse_error", null, e);
                }
            })
            .block();
    }

    private List<Detection> parseFaceDetections(JsonNode response, String correlationId) {
        List<Detection> detections = new ArrayList<>();

        if (response.has("result") && response.get("result").isArray()) {
            JsonNode resultsArray = response.get("result");

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
        // Extract bounding box from bbox
        JsonNode bbox = result.get("bbox");
        double x = bbox.get("x_min").asDouble();
        double y = bbox.get("y_min").asDouble();
        double width = bbox.get("x_max").asDouble() - x;
        double height = bbox.get("y_max").asDouble() - y;

        // Normalize coordinates (assuming they're in pixels)
        BoundingBox boundingBox = BoundingBox.fromPixels(
            (int) x, (int) y, (int) width, (int) height,
            1000, 1000 // Default image size for normalization
        );

        // Extract confidence score (normalize to 0.0 - 1.0 range)
        double confidence = result.has("confidence") ? result.get("confidence").asDouble() : 0.8;
        confidence = normalizeConfidence(confidence);

        // Extract additional attributes
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("category", com.springvision.core.DetectionCategory.FACE.name());
        if (result.has("age")) attributes.put("age", result.get("age").asInt());
        if (result.has("gender")) attributes.put("gender", result.get("gender").asText());
        if (result.has("mask")) attributes.put("mask", result.get("mask").asText());

        return new Detection("face", confidence, boundingBox, attributes);
    }

    /**
     * Normalize a raw confidence score coming from external backends.
     * - If value is already in 0..1 range it's returned as-is (clamped).
     * - If value looks like a percentage (0..100) it will be divided by 100.
     * - The result is clamped into [0.0, 1.0].
     */
    private double normalizeConfidence(double raw) {
        if (Double.isNaN(raw) || Double.isInfinite(raw)) return 0.0;
        double c = raw;
        if (c > 1.0) {
            // Treat as percentage-like value
            c = c / 100.0;
        }
        // Clamp
        if (c < 0.0) c = 0.0;
        if (c > 1.0) c = 1.0;
        return c;
    }

    private Map<String, Object> parseRecognitionResults(JsonNode response, String correlationId) {
        Map<String, Object> results = new HashMap<>();

        if (response.has("result") && response.get("result").isArray()) {
            JsonNode resultsArray = response.get("result");
            List<Map<String, Object>> recognitions = new ArrayList<>();

            for (JsonNode result : resultsArray) {
                Map<String, Object> recognition = new HashMap<>();
                if (result.has("subject")) recognition.put("subject", result.get("subject").asText());
                if (result.has("confidence")) recognition.put("confidence", normalizeConfidence(result.get("confidence").asDouble()));
                if (result.has("bbox")) {
                    JsonNode bbox = result.get("bbox");
                    Map<String, Object> bboxMap = new HashMap<>();
                    bboxMap.put("x_min", bbox.get("x_min").asDouble());
                    bboxMap.put("y_min", bbox.get("y_min").asDouble());
                    bboxMap.put("x_max", bbox.get("x_max").asDouble());
                    bboxMap.put("y_max", bbox.get("y_max").asDouble());
                    recognition.put("bbox", bboxMap);
                }
                recognitions.add(recognition);
            }
            results.put("recognitions", recognitions);
        }

        return results;
    }

    private Map<String, Object> parseVerificationResults(JsonNode response, String correlationId) {
        Map<String, Object> results = new HashMap<>();

        if (response.has("result")) {
            JsonNode result = response.get("result");
            if (result.has("verified")) results.put("verified", result.get("verified").asBoolean());
            if (result.has("confidence")) results.put("confidence", normalizeConfidence(result.get("confidence").asDouble()));
            if (result.has("similarity")) results.put("similarity", result.get("similarity").asDouble());
        }

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
     * Gets the base URL of the CompreFace API.
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
        return String.format("CompreFaceVisionBackend{baseUrl='%s', timeout=%s, initialized=%s}",
            baseUrl, timeout, initialized);
    }
}
