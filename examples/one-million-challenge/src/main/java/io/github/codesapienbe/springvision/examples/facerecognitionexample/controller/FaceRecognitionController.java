package io.github.codesapienbe.springvision.examples.facerecognitionexample.controller;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.VisionBackend;
import io.github.codesapienbe.springvision.core.recognition.FaceDatabaseBuilder;
import io.github.codesapienbe.springvision.core.recognition.FaceEmbeddingIndex;
import io.github.codesapienbe.springvision.core.recognition.FaceRecognitionEngine;

/**
 * REST controller for face recognition operations.
 *
 * <p>This controller provides endpoints for:</p>
 * <ul>
 *   <li>Building face databases from photo directories</li>
 *   <li>Performing face recognition queries</li>
 *   <li>Batch processing of multiple query images</li>
 *   <li>System status and performance monitoring</li>
 *   <li>Health checks and diagnostics</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/recognition")
@CrossOrigin(origins = "*")
public class FaceRecognitionController {

    private static final Logger logger = LoggerFactory.getLogger(FaceRecognitionController.class);

    // Size and timeout limits
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 15000;
    private static final int MAX_DOWNLOAD_BYTES = 50 * 1024 * 1024;

    // Supported image types
    private static final List<String> SUPPORTED_CONTENT_TYPES = List.of(
        "image/jpeg", "image/jpg", "image/png", "image/bmp", "image/gif", "image/webp"
    );

    private final FaceRecognitionEngine recognitionEngine;
    private final FaceDatabaseBuilder databaseBuilder;
    private final FaceEmbeddingIndex embeddingIndex;
    private final VisionBackend visionBackend;

    public FaceRecognitionController(
        @Autowired(required = false) FaceRecognitionEngine recognitionEngine,
        @Autowired(required = false) FaceDatabaseBuilder databaseBuilder,
        @Autowired(required = false) FaceEmbeddingIndex embeddingIndex,
        @Autowired(required = false) VisionBackend visionBackend) {

        this.recognitionEngine = recognitionEngine;
        this.databaseBuilder = databaseBuilder;
        this.embeddingIndex = embeddingIndex;
        this.visionBackend = visionBackend;

        logger.info("Face Recognition Controller initialized");
        logger.info("- Recognition Engine: {}", recognitionEngine != null ? "Available" : "Not configured");
        logger.info("- Database Builder: {}", databaseBuilder != null ? "Available" : "Not configured");
        logger.info("- Embedding Index: {}", embeddingIndex != null ? "Available" : "Not configured");
    }

    /**
     * Perform face recognition on uploaded image.
     */
    @PostMapping(value = "/search/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> searchFacesFromUpload(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "topK", defaultValue = "10") int topK,
        @RequestParam(value = "minSimilarity", defaultValue = "0.6") double minSimilarity) {

        String correlationId = generateCorrelationId();
        logger.info("Face recognition upload request: correlationId={}, file={}, topK={}",
            correlationId, file.getOriginalFilename(), topK);

        try {
            // Validate request
            validateFaceRecognitionAvailable();
            validateFile(file);
            validateTopK(topK);

            // Convert to ImageData
            ImageData imageData = convertToImageData(file);

            // Perform recognition
            long startTime = System.currentTimeMillis();
            List<FaceRecognitionEngine.FaceMatch> matches =
                recognitionEngine.findMatches(imageData, topK);
            long queryTime = System.currentTimeMillis() - startTime;

            // Filter by minimum similarity
            List<FaceRecognitionEngine.FaceMatch> filteredMatches = matches.stream()
                .filter(match -> match.similarity() >= minSimilarity)
                .toList();

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("correlationId", correlationId);
            response.put("success", true);
            response.put("queryTime", queryTime);
            response.put("totalMatches", matches.size());
            response.put("filteredMatches", filteredMatches.size());
            response.put("matches", filteredMatches.stream().map(this::matchToMap).toList());
            response.put("timestamp", Instant.now().toString());

            logger.info("Face recognition completed: correlationId={}, matches={}/{}, queryTime={}ms",
                correlationId, filteredMatches.size(), matches.size(), queryTime);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Face recognition failed: correlationId={}, error={}", correlationId, e.getMessage());
            return createErrorResponse(correlationId, "Face recognition failed: " + e.getMessage());
        }
    }

    /**
     * Perform face recognition from image URL.
     */
    @PostMapping("/search/url")
    public ResponseEntity<Map<String, Object>> searchFacesFromUrl(
        @RequestParam("imageUrl") String imageUrl,
        @RequestParam(value = "topK", defaultValue = "10") int topK,
        @RequestParam(value = "minSimilarity", defaultValue = "0.6") double minSimilarity) {

        String correlationId = generateCorrelationId();
        logger.info("Face recognition URL request: correlationId={}, url={}, topK={}",
            correlationId, imageUrl, topK);

        try {
            // Validate request
            validateFaceRecognitionAvailable();
            validateImageUrl(imageUrl);
            validateTopK(topK);

            // Download image
            byte[] imageBytes = downloadImageBytes(imageUrl);
            String contentType = guessContentTypeFromUrl(imageUrl);
            ImageData imageData = ImageData.fromBytes(imageBytes, contentType);

            // Perform recognition
            long startTime = System.currentTimeMillis();
            List<FaceRecognitionEngine.FaceMatch> matches =
                recognitionEngine.findMatches(imageData, topK);
            long queryTime = System.currentTimeMillis() - startTime;

            // Filter by minimum similarity
            List<FaceRecognitionEngine.FaceMatch> filteredMatches = matches.stream()
                .filter(match -> match.similarity() >= minSimilarity)
                .toList();

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("correlationId", correlationId);
            response.put("success", true);
            response.put("queryTime", queryTime);
            response.put("totalMatches", matches.size());
            response.put("filteredMatches", filteredMatches.size());
            response.put("matches", filteredMatches.stream().map(this::matchToMap).toList());
            response.put("imageUrl", imageUrl);
            response.put("timestamp", Instant.now().toString());

            logger.info("Face recognition completed: correlationId={}, matches={}/{}, queryTime={}ms",
                correlationId, filteredMatches.size(), matches.size(), queryTime);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Face recognition from URL failed: correlationId={}, error={}", correlationId, e.getMessage());
            return createErrorResponse(correlationId, "Face recognition failed: " + e.getMessage());
        }
    }

    /**
     * Build face database from directory path.
     */
    @PostMapping("/database/build")
    public ResponseEntity<Map<String, Object>> buildDatabase(
        @RequestParam("directoryPath") String directoryPath,
        @RequestParam(value = "async", defaultValue = "true") boolean async) {

        String correlationId = generateCorrelationId();
        logger.info("Database build request: correlationId={}, path={}, async={}",
            correlationId, directoryPath, async);

        try {
            // Validate request
            validateDatabaseBuilderAvailable();

            Path photoDirectory = Paths.get(directoryPath);
            if (!java.nio.file.Files.exists(photoDirectory) ||
                !java.nio.file.Files.isDirectory(photoDirectory)) {
                throw new IllegalArgumentException("Directory does not exist: " + directoryPath);
            }

            if (async) {
                // Start async processing
                CompletableFuture<FaceDatabaseBuilder.ProcessingResult> future =
                    databaseBuilder.processPhotoDirectory(photoDirectory, progress -> {
                        logger.info("Build progress: {:.1f}% - Files: {}/{}, Faces: {}",
                            progress.progressPercentage() * 100,
                            progress.processedFiles(), progress.totalFiles(),
                            progress.facesExtracted());
                    });

                Map<String, Object> response = new HashMap<>();
                response.put("correlationId", correlationId);
                response.put("success", true);
                response.put("message", "Database build started asynchronously");
                response.put("directoryPath", directoryPath);
                response.put("async", true);
                response.put("timestamp", Instant.now().toString());

                return ResponseEntity.accepted().body(response);

            } else {
                // Synchronous processing
                long startTime = System.currentTimeMillis();

                CompletableFuture<FaceDatabaseBuilder.ProcessingResult> future =
                    databaseBuilder.processPhotoDirectory(photoDirectory, progress -> {
                        // Log progress for sync requests too
                        if (progress.processedFiles() % 50 == 0) {
                            logger.info("Build progress: {:.1f}%", progress.progressPercentage() * 100);
                        }
                    });

                FaceDatabaseBuilder.ProcessingResult result =
                    future.get(60, java.util.concurrent.TimeUnit.MINUTES);

                long totalTime = System.currentTimeMillis() - startTime;

                Map<String, Object> response = new HashMap<>();
                response.put("correlationId", correlationId);
                response.put("success", result.success());
                response.put("totalFiles", result.filesProcessed());
                response.put("processedFiles", result.filesProcessed());
                response.put("facesExtracted", result.facesExtracted());
                response.put("errors", result.errors());
                response.put("processingTimeMs", totalTime);
                response.put("processingRate", result.getProcessingRate());
                response.put("facesPerFile", result.getFacesPerFile());
                response.put("directoryPath", directoryPath);
                response.put("timestamp", Instant.now().toString());

                if (!result.success()) {
                    response.put("errorMessage", "Processing completed with errors");
                }

                return ResponseEntity.ok(response);
            }

        } catch (Exception e) {
            logger.error("Database build failed: correlationId={}, error={}", correlationId, e.getMessage());
            return createErrorResponse(correlationId, "Database build failed: " + e.getMessage());
        }
    }

    /**
     * Get system status and statistics.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();

        try {
            // System availability
            status.put("recognitionAvailable", recognitionEngine != null);
            status.put("databaseBuilderAvailable", databaseBuilder != null);
            status.put("embeddingIndexAvailable", embeddingIndex != null);

            // Recognition engine stats
            if (recognitionEngine != null) {
                var engineStats = recognitionEngine.getStatistics();
                Map<String, Object> recognitionStats = new HashMap<>();
                recognitionStats.put("totalIndexedFaces", engineStats.totalIndexedFaces());
                recognitionStats.put("qualityThreshold", engineStats.qualityThreshold());
                recognitionStats.put("similarityThreshold", engineStats.similarityThreshold());
                status.put("recognitionEngine", recognitionStats);
            }

            // Index statistics
            if (embeddingIndex != null) {
                var indexStats = embeddingIndex.getStatistics();
                Map<String, Object> indexInfo = new HashMap<>();
                indexInfo.put("totalEmbeddings", indexStats.totalEmbeddings());
                indexInfo.put("embeddingDimension", indexStats.embeddingDimension());
                indexInfo.put("memoryUsageMB", indexStats.memoryUsageBytes() / 1024.0 / 1024.0);
                indexInfo.put("averageQueryTimeMs", indexStats.averageQueryTimeMillis());
                indexInfo.put("totalQueries", indexStats.totalQueries());
                indexInfo.put("ready", embeddingIndex.isReady());
                status.put("embeddingIndex", indexInfo);
            }

            // Backend health
            if (visionBackend != null) {
                var healthInfo = visionBackend.getHealthInfo();
                Map<String, Object> backendInfo = new HashMap<>();
                backendInfo.put("backendId", visionBackend.getBackendId());
                backendInfo.put("displayName", visionBackend.getDisplayName());
                backendInfo.put("version", visionBackend.getVersion());
                backendInfo.put("healthy", visionBackend.isHealthy());
                backendInfo.put("status", healthInfo.status());
                backendInfo.put("responseTime", healthInfo.responseTimeMs());
                backendInfo.put("message", healthInfo.statusMessage());
                status.put("backend", backendInfo);
            }

            status.put("timestamp", Instant.now().toString());
            status.put("success", true);

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            logger.error("Error retrieving system status: {}", e.getMessage());
            status.put("success", false);
            status.put("error", e.getMessage());
            return ResponseEntity.ok(status);
        }
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> health = new HashMap<>();

        boolean systemHealthy = true;

        // Check recognition engine
        if (recognitionEngine != null) {
            try {
                recognitionEngine.getStatistics();
                health.put("recognitionEngine", "UP");
            } catch (Exception e) {
                health.put("recognitionEngine", "DOWN");
                systemHealthy = false;
            }
        } else {
            health.put("recognitionEngine", "NOT_CONFIGURED");
        }

        // Check embedding index
        if (embeddingIndex != null) {
            health.put("embeddingIndex", embeddingIndex.isReady() ? "UP" : "DOWN");
            if (!embeddingIndex.isReady()) {
                systemHealthy = false;
            }
        } else {
            health.put("embeddingIndex", "NOT_CONFIGURED");
        }

        // Check backend
        if (visionBackend != null) {
            health.put("backend", visionBackend.isHealthy() ? "UP" : "DOWN");
            if (!visionBackend.isHealthy()) {
                systemHealthy = false;
            }
        } else {
            health.put("backend", "NOT_CONFIGURED");
        }

        health.put("status", systemHealthy ? "UP" : "DOWN");
        health.put("timestamp", Instant.now().toString());

        HttpStatus status = systemHealthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(health);
    }

    // Helper methods

    private void validateFaceRecognitionAvailable() {
        if (recognitionEngine == null) {
            throw new IllegalStateException("Face recognition engine not available. Enable with spring.vision.recognition.enabled=true");
        }
    }

    private void validateDatabaseBuilderAvailable() {
        if (databaseBuilder == null) {
            throw new IllegalStateException("Database builder not available. Enable with spring.vision.recognition.enabled=true");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds limit: " + MAX_FILE_SIZE + " bytes");
        }

        String contentType = file.getContentType();
        if (contentType == null || !SUPPORTED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported file type: " + contentType);
        }
    }

    private void validateImageUrl(String imageUrl) {
        if (!isHttpUrl(imageUrl)) {
            throw new IllegalArgumentException("Invalid image URL");
        }

        try {
            URL url = new URL(imageUrl);
            validatePublicHost(url);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid or unsafe URL: " + e.getMessage());
        }
    }

    private void validateTopK(int topK) {
        if (topK <= 0 || topK > 100) {
            throw new IllegalArgumentException("topK must be between 1 and 100");
        }
    }

    private ImageData convertToImageData(MultipartFile file) throws IOException {
        return ImageData.fromBytes(file.getBytes(), file.getContentType());
    }

    private Map<String, Object> matchToMap(FaceRecognitionEngine.FaceMatch match) {
        Map<String, Object> map = new HashMap<>();
        map.put("photoId", match.photoId());
        map.put("similarity", match.similarity());
        map.put("confidence", match.confidence());
        map.put("distance", match.distance());
        map.put("qualityScore", match.confidence());
        return map;
    }

    private String generateCorrelationId() {
        return "rec-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private ResponseEntity<Map<String, Object>> createErrorResponse(String correlationId, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("correlationId", correlationId);
        response.put("success", false);
        response.put("error", message);
        response.put("timestamp", Instant.now().toString());
        return ResponseEntity.badRequest().body(response);
    }

    private byte[] downloadImageBytes(String urlString) throws IOException {
        URL url = new URL(urlString);
        validatePublicHost(url);

        try {
            return java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofMillis(CONNECT_TIMEOUT_MS))
                .build()
                .send(java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(urlString))
                        .timeout(java.time.Duration.ofMillis(READ_TIMEOUT_MS))
                        .GET()
                        .build(),
                    java.net.http.HttpResponse.BodyHandlers.ofByteArray())
                .body();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IOException("HTTP request interrupted", ie);
        }
    }

    private boolean isHttpUrl(String s) {
        return s != null && (s.startsWith("http://") || s.startsWith("https://"));
    }

    private void validatePublicHost(URL url) throws IOException {
        String host = url.getHost();
        if (host == null || host.equals("localhost") || host.equals("127.0.0.1") ||
            host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")) {
            throw new IOException("Local/private host not allowed: " + host);
        }
    }

    private String guessContentTypeFromUrl(String url) {
        String lower = url.toLowerCase();
        if (lower.contains(".jpg") || lower.contains(".jpeg")) return "image/jpeg";
        if (lower.contains(".png")) return "image/png";
        if (lower.contains(".bmp")) return "image/bmp";
        if (lower.contains(".gif")) return "image/gif";
        if (lower.contains(".webp")) return "image/webp";
        return "image/jpeg"; // Default
    }
}
