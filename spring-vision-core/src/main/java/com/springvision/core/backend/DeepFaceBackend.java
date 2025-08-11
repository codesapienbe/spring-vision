package com.springvision.core.backend;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springvision.core.BackendHealthInfo;
import com.springvision.core.BoundingBox;
import com.springvision.core.Detection;
import com.springvision.core.DetectionType;
import com.springvision.core.ImageData;
import com.springvision.core.VisionBackend;
import com.springvision.core.VisionResult;
import com.springvision.core.exception.BaseVisionException;

/**
 * VisionBackend implementation that integrates with an external DeepFace REST API.
 *
 * <p>This backend forwards image processing requests to a running DeepFace service
 * (e.g., dockerized) via HTTP. It focuses on face detection by using the
 * /represent endpoint to obtain facial regions and confidence scores, which are
 * then mapped to Spring Vision domain objects.</p>
 */
public final class DeepFaceBackend implements VisionBackend {

    private static final Logger logger = LoggerFactory.getLogger(DeepFaceBackend.class);

    private static final String BACKEND_ID = "deepface";
    private static final String DISPLAY_NAME = "DeepFace REST Backend";
    private static final String VERSION = "1.0.0";

    // Base folder name where the repo is unpacked by Maven build
    private static final String DEEPFACE_FOLDER_NAME = "deepface";
    private static final String SCRIPTS_SUBFOLDER = "scripts";
    private static final String SERVICE_SCRIPT = "service.sh";

    private static final AtomicBoolean serviceStartAttempted = new AtomicBoolean(false);
    private static volatile Process deepFaceProcess;

    private final String baseUrl;
    private final String defaultModelName;
    private final String defaultDetectorBackend;
    private final String defaultDistanceMetric;
    private final Duration connectTimeout;
    private final Duration readTimeout;

    // Endpoint-specific read timeouts
    private final Duration representReadTimeout;
    private final Duration verifyReadTimeout;
    private final Duration analyzeReadTimeout;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Creates a new DeepFace backend using provided configuration values.
     *
     * @param baseUrl Base URL of the DeepFace REST API (e.g., http://localhost:5005)
     * @param defaultModelName Default model name for requests (e.g., "Facenet")
     * @param defaultDetectorBackend Default detector backend (e.g., "mtcnn")
     * @param defaultDistanceMetric Default distance metric (e.g., "cosine")
     * @param connectTimeoutMillis Connection timeout in milliseconds
     * @param readTimeoutMillis Read timeout in milliseconds (used for all endpoints)
     */
    public DeepFaceBackend(
            String baseUrl,
            String defaultModelName,
            String defaultDetectorBackend,
            String defaultDistanceMetric,
            long connectTimeoutMillis,
            long readTimeoutMillis
    ) {
        this(
            baseUrl,
            defaultModelName,
            defaultDetectorBackend,
            defaultDistanceMetric,
            connectTimeoutMillis,
            readTimeoutMillis,
            readTimeoutMillis,
            readTimeoutMillis
        );
    }

    /**
     * Creates a new DeepFace backend using endpoint-specific read timeouts.
     *
     * @param baseUrl Base URL of the DeepFace REST API (e.g., http://localhost:5005)
     * @param defaultModelName Default model name for requests (e.g., "Facenet")
     * @param defaultDetectorBackend Default detector backend (e.g., "mtcnn")
     * @param defaultDistanceMetric Default distance metric (e.g., "cosine")
     * @param connectTimeoutMillis Connection timeout in milliseconds
     * @param representReadTimeoutMillis Read timeout in milliseconds for /represent
     * @param verifyReadTimeoutMillis Read timeout in milliseconds for /verify
     * @param analyzeReadTimeoutMillis Read timeout in milliseconds for /analyze
     */
    public DeepFaceBackend(
            String baseUrl,
            String defaultModelName,
            String defaultDetectorBackend,
            String defaultDistanceMetric,
            long connectTimeoutMillis,
            long representReadTimeoutMillis,
            long verifyReadTimeoutMillis,
            long analyzeReadTimeoutMillis
    ) {
        this.baseUrl = ensureNoTrailingSlash(baseUrl);
        this.defaultModelName = Optional.ofNullable(defaultModelName).orElse("Facenet");
        this.defaultDetectorBackend = Optional.ofNullable(defaultDetectorBackend).orElse("mtcnn");
        this.defaultDistanceMetric = Optional.ofNullable(defaultDistanceMetric).orElse("cosine");
        this.connectTimeout = Duration.ofMillis(Math.max(1, connectTimeoutMillis));
        // Keep legacy aggregate read timeout for backward compatibility/health checks
        this.readTimeout = Duration.ofMillis(Math.max(1, Math.max(Math.max(representReadTimeoutMillis, verifyReadTimeoutMillis), analyzeReadTimeoutMillis)));
        this.representReadTimeout = Duration.ofMillis(Math.max(1, representReadTimeoutMillis));
        this.verifyReadTimeout = Duration.ofMillis(Math.max(1, verifyReadTimeoutMillis));
        this.analyzeReadTimeout = Duration.ofMillis(Math.max(1, analyzeReadTimeoutMillis));
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(this.connectTimeout)
                .build();
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
        return Set.of(DetectionType.FACE);
    }

    @Override
    public boolean isHealthy() {
        try {
            // Perform a lightweight GET request to the base URL to verify reachability
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .timeout(connectTimeout)
                    .GET()
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int code = response.statusCode();
            return code >= 200 && code < 500; // treat any reachable response as healthy
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public BackendHealthInfo getHealthInfo() {
        long start = System.currentTimeMillis();
        try {
            boolean ok = isHealthy();
            long took = System.currentTimeMillis() - start;
            if (ok) {
                return BackendHealthInfo.healthy(BACKEND_ID, "DeepFace reachable", took, Map.of(
                        "displayName", DISPLAY_NAME,
                        "version", VERSION,
                        "baseUrl", baseUrl
                ));
            }
            return BackendHealthInfo.unhealthy(BACKEND_ID, "DeepFace unreachable",
                    "Failed to reach DeepFace service at " + baseUrl, System.currentTimeMillis() - start);
        } catch (Exception e) {
            long took = System.currentTimeMillis() - start;
            return BackendHealthInfo.unhealthy(BACKEND_ID, "DeepFace health check failed",
                    sanitize(e.getMessage()), took);
        }
    }

    @Override
    public VisionResult detectFaces(ImageData imageData) throws BaseVisionException {
        if (imageData == null || imageData.isEmpty()) {
            throw new IllegalArgumentException("ImageData must not be null or empty");
        }

        long start = System.currentTimeMillis();
        String correlationId = generateCorrelationId();
        try {
            // Ensure DeepFace service is up before making the API call
            ensureServiceRunning(correlationId);

            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageData.data()));
            if (img == null) {
                throw new IllegalArgumentException("Unsupported or corrupt image data");
            }

            String dataUri = buildDataUri(imageData);
            String requestBody = objectMapper.createObjectNode()
                    .put("img", dataUri)
                    .put("model_name", defaultModelName)
                    .put("detector_backend", defaultDetectorBackend)
                    .toString();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/represent"))
                    .timeout(representReadTimeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status == 400) {
                long took = System.currentTimeMillis() - start;
                logger.warn("DeepFace represent returned 400", Map.of(
                        "component", BACKEND_ID,
                        "endpoint", "/represent",
                        "correlation_id", correlationId,
                        "elapsed_ms", took
                ));
                return VisionResult.empty(DetectionType.FACE, took);
            }
            if (status < 200 || status >= 300) {
                long took = System.currentTimeMillis() - start;
                logger.error("DeepFace represent call failed", Map.of(
                        "component", BACKEND_ID,
                        "endpoint", "/represent",
                        "status", status,
                        "correlation_id", correlationId,
                        "elapsed_ms", took
                ));
                return VisionResult.empty(DetectionType.FACE, took);
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode results = root.get("results");
            if (results == null || !results.isArray() || results.isEmpty()) {
                long took = System.currentTimeMillis() - start;
                logger.warn("DeepFace represent returned no results", Map.of(
                        "component", BACKEND_ID,
                        "endpoint", "/represent",
                        "correlation_id", correlationId,
                        "elapsed_ms", took
                ));
                return VisionResult.empty(DetectionType.FACE, took);
            }

            List<Detection> detections = new ArrayList<>();
            for (JsonNode node : results) {
                double confidence = optionalDouble(node, "face_confidence").orElse(1.0);
                // Support both "facial_area" (single) and "facial_areas" (verify style)
                JsonNode area = node.has("facial_area") ? node.get("facial_area") : node.get("facial_areas");
                if (area == null || area.isNull()) {
                    // Some versions use "region" or nested structures; skip if not found
                    continue;
                }
                int x = optionalInt(area, "x").orElse(optionalInt(area, "left").orElse(0));
                int y = optionalInt(area, "y").orElse(optionalInt(area, "top").orElse(0));
                int w = optionalInt(area, "w").orElseGet(() -> {
                    Integer right = optionalInt(area, "right").orElse(null);
                    return (right != null) ? Math.max(0, right - x) : 0;
                });
                int h = optionalInt(area, "h").orElseGet(() -> {
                    Integer bottom = optionalInt(area, "bottom").orElse(null);
                    return (bottom != null) ? Math.max(0, bottom - y) : 0;
                });

                double nx = clamp01((double) x / img.getWidth());
                double ny = clamp01((double) y / img.getHeight());
                double nw = clamp01((double) w / img.getWidth());
                double nh = clamp01((double) h / img.getHeight());

                detections.add(new Detection("face", clamp01(confidence), new BoundingBox(nx, ny, nw, nh), Map.of()));
            }

            double avg = detections.isEmpty() ? 0.0 : detections.stream().mapToDouble(Detection::confidence).average().orElse(0.0);
            long took = System.currentTimeMillis() - start;
            return new VisionResult(DetectionType.FACE, detections, avg, took, Instant.now(), Map.of(
                    "backend", BACKEND_ID,
                    "source", "deepface"
            ));
        } catch (Exception e) {
            long took = System.currentTimeMillis() - start;
            String msg = sanitize(e.getMessage());
            logger.error("DeepFace detectFaces failed", Map.of(
                    "component", BACKEND_ID,
                    "endpoint", "/represent",
                    "correlation_id", correlationId,
                    "elapsed_ms", took,
                    "error", msg
            ));
            return VisionResult.empty(DetectionType.FACE, took);
        }
    }

    @Override
    public VisionResult detectObjects(ImageData imageData) throws BaseVisionException {
        throw new UnsupportedOperationException("Object detection not supported by DeepFace backend");
    }

    private static String buildDataUri(ImageData imageData) {
        String mime = Optional.ofNullable(imageData.mimeType()).orElse(ImageData.DEFAULT_JPEG_MIME_TYPE);
        String base64 = Base64.getEncoder().encodeToString(imageData.data());
        return "data:" + mime + ";base64," + base64;
    }

    private static String ensureNoTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:5005";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }

    private static Optional<Double> optionalDouble(JsonNode node, String field) {
        try {
            if (node != null && node.has(field) && node.get(field).isNumber()) {
                return Optional.of(node.get(field).asDouble());
            }
        } catch (Exception ignored) {}
        return Optional.empty();
    }

    private static Optional<Integer> optionalInt(JsonNode node, String field) {
        try {
            if (node != null && node.has(field) && node.get(field).isNumber()) {
                return Optional.of(node.get(field).asInt());
            }
        } catch (Exception ignored) {}
        return Optional.empty();
    }

    private static String sanitize(String message) {
        if (message == null) return "";
        // Basic sanitization to avoid leaking sensitive data into logs
        if (message.length() > 512) {
            return message.substring(0, 512) + "...";
        }
        return message;
    }

    private static String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    private void ensureServiceRunning(String correlationId) {
        if (isHealthy()) {
            return;
        }
        if (!serviceStartAttempted.compareAndSet(false, true)) {
            // Another attempt has been made previously; do not spam starts
            waitForServiceReadiness(correlationId, Duration.ofSeconds(25));
            return;
        }

        Path script = resolveServiceScriptPath();
        if (script == null) {
            logger.warn("DeepFace service.sh not found; skipping auto-start", Map.of(
                    "component", BACKEND_ID,
                    "correlation_id", correlationId
            ));
            return;
        }
        try {
            startDeepFaceServiceProcess(script, correlationId);
        } catch (Exception ex) {
            logger.error("Failed to start DeepFace service", Map.of(
                    "component", BACKEND_ID,
                    "correlation_id", correlationId,
                    "error", sanitize(ex.getMessage())
            ));
            return;
        }
        // Wait briefly for the service to come up
        waitForServiceReadiness(correlationId, Duration.ofSeconds(25));
    }

    private void waitForServiceReadiness(String correlationId, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        long sleepMs = 500;
        while (System.nanoTime() < deadline) {
            if (isHealthy()) {
                logger.info("DeepFace service is ready", Map.of(
                        "component", BACKEND_ID,
                        "correlation_id", correlationId
                ));
                return;
            }
            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
            sleepMs = Math.min(2000, sleepMs + 250);
        }
        logger.warn("DeepFace service did not become ready in time", Map.of(
                "component", BACKEND_ID,
                "correlation_id", correlationId,
                "timeout_ms", timeout.toMillis()
        ));
    }

    private Path resolveServiceScriptPath() {
        // Start from user.dir and walk up to 6 levels to find deepface/scripts/service.sh
        Path start = Paths.get(System.getProperty("user.dir"));
        for (int i = 0; i < 6; i++) {
            Path candidate = start.resolve(DEEPFACE_FOLDER_NAME)
                    .resolve(SCRIPTS_SUBFOLDER)
                    .resolve(SERVICE_SCRIPT);
            if (Files.isRegularFile(candidate) && Files.isReadable(candidate)) {
                // Extra safety: ensure the path contains the deepface folder component
                try {
                    String canonical = candidate.toFile().getCanonicalPath();
                    if (canonical.contains(File.separator + DEEPFACE_FOLDER_NAME + File.separator)) {
                        return candidate;
                    }
                } catch (IOException ignored) {
                    // fall through
                }
            }
            start = start.getParent() != null ? start.getParent() : start;
        }
        return null;
    }

    private void startDeepFaceServiceProcess(Path scriptPath, String correlationId) throws IOException {
        if (deepFaceProcess != null && deepFaceProcess.isAlive()) {
            return;
        }

        File scriptsDir = scriptPath.getParent().toFile();
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        ProcessBuilder pb;
        if (isWindows) {
            // Attempt to use Git Bash if available on PATH
            pb = new ProcessBuilder("bash", "-lc", "./" + SERVICE_SCRIPT);
        } else {
            pb = new ProcessBuilder("bash", "./" + SERVICE_SCRIPT);
        }
        pb.directory(scriptsDir);
        pb.redirectErrorStream(true);

        Map<String, String> env = pb.environment();
        env.putIfAbsent("PYTHONUNBUFFERED", "1");
        env.putIfAbsent("DEEPFACE_PORT", String.valueOf(URI.create(baseUrl).getPort() == -1 ? 5005 : URI.create(baseUrl).getPort()));

        deepFaceProcess = pb.start();
        consumeProcessOutputAsync(deepFaceProcess, correlationId);

        logger.info("Started DeepFace service.sh", Map.of(
                "component", BACKEND_ID,
                "working_dir", scriptsDir.getAbsolutePath(),
                "correlation_id", correlationId
        ));
    }

    private void consumeProcessOutputAsync(Process process, String correlationId) {
        ExecutorService executor = Executors.newFixedThreadPool(1);
        executor.submit(() -> {
            try (InputStream is = process.getInputStream();
                 InputStreamReader isr = new InputStreamReader(is);
                 BufferedReader br = new BufferedReader(isr)) {
                String line;
                while ((line = br.readLine()) != null) {
                    String sanitized = sanitize(line);
                    if (!sanitized.isBlank()) {
                        logger.debug(sanitized, Map.of(
                                "component", BACKEND_ID,
                                "correlation_id", correlationId,
                                "source", "deepface-service"
                        ));
                    }
                }
            } catch (IOException ignored) {
                // ignore
            }
        });
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
