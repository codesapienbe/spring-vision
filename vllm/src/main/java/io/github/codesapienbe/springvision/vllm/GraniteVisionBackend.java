package io.github.codesapienbe.springvision.vllm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.codesapienbe.springvision.core.BackendHealthInfo;
import io.github.codesapienbe.springvision.core.BoundingBox;
import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.DetectionType;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.VisionBackend;
import io.github.codesapienbe.springvision.core.capabilities.ObjectDetectionCapability;
import io.github.codesapienbe.springvision.core.capabilities.TextOcrCapability;
import io.github.codesapienbe.springvision.core.exception.VisionBackendException;
import io.github.codesapienbe.springvision.core.exception.VisionProcessingException;
import io.github.codesapienbe.springvision.vllm.config.VllmProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Granite Vision backend powered by vLLM with IBM Granite 3.2 Vision model.
 *
 * <p>This backend provides vision language model capabilities through vLLM,
 * supporting multimodal understanding including object detection, scene analysis,
 * visual question answering, and image captioning using IBM's Granite 3.2 Vision model.</p>
 *
 * <p>The backend communicates with a vLLM server via OpenAI-compatible API,
 * sending images and prompts to generate detailed descriptions and analysis.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * VllmProperties properties = new VllmProperties();
 * GraniteVisionBackend backend = new GraniteVisionBackend(properties);
 * backend.initialize();
 *
 * ImageData imageData = ImageData.fromBytes(imageBytes);
 * List<Detection> objects = backend.detectObjects(imageData);
 * }</pre>
 *
 * @author Spring Vision Team
 * @see VisionBackend
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(prefix = "spring.vision.vllm", name = "enabled", havingValue = "true")
public class GraniteVisionBackend implements VisionBackend, ObjectDetectionCapability, TextOcrCapability {

    private static final Logger logger = LoggerFactory.getLogger(GraniteVisionBackend.class);

    private static final String BACKEND_ID = "granite-vision";
    private static final String DISPLAY_NAME = "Granite Vision Backend (vLLM)";
    private static final String VERSION = "1.0.0";

    private static final Duration HEALTH_CHECK_TIMEOUT = Duration.ofSeconds(5);

    private final String baseUrl;
    private final String model;
    private final Duration timeout;
    private final int maxTokens;
    private final double temperature;
    private final double topP;
    private final double confidenceThreshold;
    private final boolean enableStreaming;

    private WebClient webClient;
    private final ObjectMapper objectMapper;

    private volatile boolean initialized = false;
    private volatile BackendHealthInfo.HealthStatus healthStatus = BackendHealthInfo.HealthStatus.UNKNOWN;
    private volatile String healthErrorMessage = "Backend not initialized";
    private volatile long lastHealthCheckTime = 0L;

    /**
     * Creates a new Granite Vision backend with default configuration.
     */
    public GraniteVisionBackend() {
        this(new VllmProperties());
    }

    /**
     * Creates a new Granite Vision backend from properties configuration.
     *
     * @param properties the vLLM properties configuration
     */
    public GraniteVisionBackend(VllmProperties properties) {
        this.baseUrl = Objects.requireNonNull(properties.baseUrl(), "Base URL must not be null").trim();
        this.model = Objects.requireNonNull(properties.model(), "Model must not be null").trim();
        this.timeout = Objects.requireNonNull(properties.timeout(), "Timeout must not be null");
        this.maxTokens = properties.maxTokens();
        this.temperature = properties.temperature();
        this.topP = properties.topP();
        this.confidenceThreshold = properties.confidenceThreshold();
        this.enableStreaming = properties.enableStreaming();

        if (this.baseUrl.isEmpty()) {
            throw new IllegalArgumentException("Base URL must not be empty");
        }
        if (this.model.isEmpty()) {
            throw new IllegalArgumentException("Model must not be empty");
        }
        if (this.timeout.isNegative() || this.timeout.isZero()) {
            throw new IllegalArgumentException("Timeout must be positive");
        }

        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(50 * 1024 * 1024)) // 50MB for images
            .build();
        this.objectMapper = new ObjectMapper();

        initialize();
    }

    /**
     * Initializes the backend by performing a health check.
     */
    public void initialize() {
        try {
            logger.info("Initializing Granite Vision Backend with model: {}", model);
            performHealthCheck();
            this.initialized = true;
            logger.info("Granite Vision Backend initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize Granite Vision Backend", e);
            this.healthStatus = BackendHealthInfo.HealthStatus.UNHEALTHY;
            this.healthErrorMessage = "Initialization failed: " + e.getMessage();
        }
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
            DetectionType.OBJECT,
            DetectionType.TEXT
        );
    }

    @Override
    public boolean isHealthy() {
        return initialized && healthStatus == BackendHealthInfo.HealthStatus.HEALTHY;
    }

    @Override
    public BackendHealthInfo getHealthInfo() {
        return new BackendHealthInfo(
            BACKEND_ID,
            healthStatus,
            "Granite Vision Backend Status",
            healthErrorMessage,
            Instant.ofEpochMilli(lastHealthCheckTime),
            0L,
            Map.of(
                "model", model,
                "baseUrl", baseUrl,
                "initialized", String.valueOf(initialized)
            )
        );
    }

    /**
     * Performs a health check against the vLLM server.
     */
    private void performHealthCheck() {
        try {
            String response = webClient.get()
                .uri("/health")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(HEALTH_CHECK_TIMEOUT)
                .block();

            this.healthStatus = BackendHealthInfo.HealthStatus.HEALTHY;
            this.healthErrorMessage = null;
            this.lastHealthCheckTime = System.currentTimeMillis();
            logger.debug("Health check passed: {}", response);
        } catch (Exception e) {
            this.healthStatus = BackendHealthInfo.HealthStatus.UNHEALTHY;
            this.healthErrorMessage = "Health check failed: " + e.getMessage();
            this.lastHealthCheckTime = System.currentTimeMillis();
            logger.error("Health check failed", e);
            throw new VisionBackendException("vLLM server health check failed", e);
        }
    }

    @Override
    public List<Detection> detectObjects(ImageData imageData) {
        String prompt = "Analyze this image and list all objects you can see. " +
            "For each object, provide: the object name, its approximate location (left, center, right, top, bottom), " +
            "and your confidence level (0-100%). Format: Object: [name], Location: [location], Confidence: [percentage]";

        String response = queryVisionModel(imageData, prompt);
        return parseObjectDetections(response);
    }

    @Override
    public List<Detection> detectText(ImageData imageData) {
        String prompt = "Extract all visible text from this image. " +
            "List each text element with its approximate location and confidence level. " +
            "Format: Text: [content], Location: [location], Confidence: [percentage]";

        String response = queryVisionModel(imageData, prompt);
        return parseTextDetections(response);
    }

    /**
     * Analyzes a scene in the provided image.
     *
     * @param imageData the image to analyze
     * @return description of the scene
     */
    public String analyzeScene(ImageData imageData) {
        String prompt = "Describe this scene in detail. Include information about the setting, objects, people (if any), " +
            "activities, time of day, weather conditions, and overall atmosphere.";

        return queryVisionModel(imageData, prompt);
    }

    /**
     * Answers a question about the provided image.
     *
     * @param imageData the image to analyze
     * @param question  the question to answer
     * @return the answer
     */
    public String answerQuestion(ImageData imageData, String question) {
        String prompt = "Answer the following question about this image: " + question;
        return queryVisionModel(imageData, prompt);
    }

    /**
     * Queries the vision model with an image and prompt.
     *
     * @param imageData the image data
     * @param prompt    the text prompt
     * @return the model's response
     */
    private String queryVisionModel(ImageData imageData, String prompt) {
        if (!initialized) {
            throw new VisionBackendException("Backend not initialized");
        }

        try {
            // Convert image to base64
            String base64Image = Base64.getEncoder().encodeToString(imageData.data());

            // Build request body following OpenAI's vision API format
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", temperature);
            requestBody.put("top_p", topP);

            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");

            List<Object> content = new ArrayList<>();
            content.add(Map.of("type", "text", "text", prompt));
            content.add(Map.of(
                "type", "image_url",
                "image_url", Map.of("url", "data:image/jpeg;base64," + base64Image)
            ));

            message.put("content", content);
            messages.add(message);
            requestBody.put("messages", messages);

            logger.debug("Sending request to vLLM with prompt: {}", prompt);

            String response = webClient.post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(timeout)
                .block();

            return extractContentFromResponse(response);

        } catch (Exception e) {
            logger.error("Error querying vision model", e);
            throw new VisionProcessingException("Failed to query vision model: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts the text content from the API response.
     */
    private String extractContentFromResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                return choices.get(0).path("message").path("content").asText();
            }
            throw new VisionProcessingException("No content in response");
        } catch (JsonProcessingException e) {
            throw new VisionProcessingException("Failed to parse response: " + e.getMessage(), e);
        }
    }

    /**
     * Parses object detections from the model's text response.
     */
    private List<Detection> parseObjectDetections(String response) {
        List<Detection> detections = new ArrayList<>();
        String[] lines = response.split("\n");

        for (String line : lines) {
            if (line.trim().isEmpty()) continue;

            try {
                // Simple parsing - can be enhanced with more sophisticated NLP
                if (line.toLowerCase().contains("object:")) {
                    String objectName = extractValue(line, "object:");
                    String location = extractValue(line, "location:");
                    double confidence = extractConfidence(line);

                    if (objectName != null && !objectName.isEmpty()) {
                        Detection detection = Detection.of(
                            objectName,
                            confidence,
                            createBoundingBoxFromLocation(location),
                            "source", "granite-vision"
                        );
                        detections.add(detection);
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to parse detection line: {}", line, e);
            }
        }

        return detections;
    }

    /**
     * Parses text detections from the model's text response.
     */
    private List<Detection> parseTextDetections(String response) {
        List<Detection> detections = new ArrayList<>();
        String[] lines = response.split("\n");

        for (String line : lines) {
            if (line.trim().isEmpty()) continue;

            try {
                if (line.toLowerCase().contains("text:")) {
                    String text = extractValue(line, "text:");
                    String location = extractValue(line, "location:");
                    double confidence = extractConfidence(line);

                    if (text != null && !text.isEmpty()) {
                        Detection detection = Detection.of(
                            text,
                            confidence,
                            createBoundingBoxFromLocation(location),
                            "source", "granite-vision"
                        );
                        detections.add(detection);
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to parse text detection line: {}", line, e);
            }
        }

        return detections;
    }

    /**
     * Extracts a value from a formatted line.
     */
    private String extractValue(String line, String key) {
        int startIdx = line.toLowerCase().indexOf(key.toLowerCase());
        if (startIdx == -1) return null;

        startIdx += key.length();
        int endIdx = line.indexOf(",", startIdx);
        if (endIdx == -1) endIdx = line.length();

        return line.substring(startIdx, endIdx).trim();
    }

    /**
     * Extracts confidence percentage from a line.
     */
    private double extractConfidence(String line) {
        try {
            String confStr = extractValue(line, "confidence:");
            if (confStr != null) {
                confStr = confStr.replaceAll("[^0-9.]", "");
                double conf = Double.parseDouble(confStr);
                // If percentage, convert to 0-1 range
                if (conf > 1.0) {
                    conf = conf / 100.0;
                }
                return Math.max(0.0, Math.min(1.0, conf));
            }
        } catch (Exception e) {
            logger.debug("Failed to extract confidence from line: {}", line);
        }
        return confidenceThreshold;
    }

    /**
     * Creates a bounding box from a textual location description.
     * This is a simplified implementation - a real implementation would need
     * more sophisticated parsing or structured output from the model.
     */
    private BoundingBox createBoundingBoxFromLocation(String location) {
        if (location == null) {
            return new BoundingBox(0, 0, 0, 0);
        }

        // Simple mapping of location descriptions to approximate regions
        String loc = location.toLowerCase();
        int x = 0, y = 0, width = 100, height = 100;

        if (loc.contains("left")) x = 0;
        else if (loc.contains("center")) x = 40;
        else if (loc.contains("right")) x = 60;

        if (loc.contains("top")) y = 0;
        else if (loc.contains("middle")) y = 40;
        else if (loc.contains("bottom")) y = 60;

        return new BoundingBox(x, y, width, height);
    }
}

