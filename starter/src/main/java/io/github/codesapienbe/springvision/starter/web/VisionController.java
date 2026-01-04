package io.github.codesapienbe.springvision.starter.web;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import io.github.codesapienbe.springvision.core.DetectionCategory;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.VisionBackend;
import io.github.codesapienbe.springvision.core.VisionResult;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import io.github.codesapienbe.springvision.core.capabilities.EmbeddingCapability;
import net.logstash.logback.argument.StructuredArguments;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Async REST API Controller for Spring Vision capabilities.
 *
 * Provides comprehensive computer vision operations including:
 * - Face detection and recognition
 * - Object detection
 * - Text extraction (OCR)
 * - Image classification
 * - Pose detection
 * - Action recognition
 * - NSFW content detection
 * - Emotion detection
 * - Demographics analysis
 * - Barcode/QR code scanning
 * - Security threat detection
 * - Fall detection
 * - Stress analysis
 * - Heart rate estimation
 * - Biometric authentication
 */
@RestController
@RequestMapping("/api/vision")
@CrossOrigin(origins = "*")
public class VisionController {

    private static final Logger log = LoggerFactory.getLogger(VisionController.class);

    private final VisionTemplate visionTemplate;
    private final HttpClient httpClient;

    private static final int MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    @Value("${spring.vision.djl.face-recognition.similarity-threshold:0.6}")
    private double configuredSimilarityThreshold = 0.6;

    public VisionController(VisionTemplate visionTemplate) {
        this.visionTemplate = visionTemplate;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

        log.info("VisionController initialized",
            StructuredArguments.keyValue("event", "vision_controller_init"),
            StructuredArguments.keyValue("backend", visionTemplate.getBackendId()),
            StructuredArguments.keyValue("max_image_size_bytes", MAX_IMAGE_SIZE_BYTES));
    }


    // ========== Face Detection & Recognition ==========

    /**
     * Count faces in an image from URL or uploaded file.
     *
     * @param imageUrl Optional image URL
     * @param file Optional uploaded image file
     * @return Face count with confidence scores
     */
    @PostMapping(value = "/faces/count", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> countFaces(
            @RequestParam(required = false) String imageUrl,
            @RequestPart(required = false) MultipartFile file) {

        log.info("countFaces called",
            StructuredArguments.keyValue("event", "count_faces_start"),
            StructuredArguments.keyValue("hasUrl", imageUrl != null),
            StructuredArguments.keyValue("hasFile", file != null));

        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            Map<String, Object> response = new HashMap<>();

            try {
                ImageData imgData = resolveImageSource(imageUrl, file);
                VisionResult result = visionTemplate.detectFaces(imgData);

                long duration = System.currentTimeMillis() - startTime;

                response.put("status", "success");
                response.put("count", result.detectionCount());
                response.put("averageConfidence", Math.round(result.averageConfidence() * 10000.0) / 10000.0);
                response.put("processingTimeMs", duration);
                response.put("message", String.format("Detected %d faces", result.detectionCount()));

                return ResponseEntity.ok(response);

            } catch (IllegalArgumentException e) {
                response.put("status", "error");
                response.put("count", 0);
                response.put("message", e.getMessage());
                return ResponseEntity.badRequest().body(response);

            } catch (Exception e) {
                log.error("Failed to count faces", e);
                response.put("status", "error");
                response.put("count", 0);
                response.put("message", "Failed to count faces: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Extract face embeddings from an image.
     *
     * @param imageUrl Optional image URL
     * @param file Optional uploaded image file
     * @return List of face embeddings with metadata
     */
    @PostMapping(value = "/faces/embeddings", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> extractFaceEmbeddings(
            @RequestParam(required = false) String imageUrl,
            @RequestPart(required = false) MultipartFile file) {

        log.info("extractFaceEmbeddings called",
            StructuredArguments.keyValue("event", "extract_embeddings_start"));

        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            Map<String, Object> response = new HashMap<>();

            try {
                ImageData imgData = resolveImageSource(imageUrl, file);
                List<float[]> rawEmbeddings = extractEmbeddingsFromTemplate(imgData, DetectionCategory.FACE);

                List<Map<String, Object>> embeddings = new ArrayList<>();
                for (int i = 0; i < rawEmbeddings.size(); i++) {
                    float[] emb = rawEmbeddings.get(i);
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", "face-" + i);
                    item.put("embedding_base64", Base64.getEncoder().encodeToString(serializeEmbedding(emb)));
                    item.put("length", emb.length);
                    embeddings.add(item);
                }

                long duration = System.currentTimeMillis() - startTime;
                response.put("status", "success");
                response.put("count", embeddings.size());
                response.put("embeddings", embeddings);
                response.put("processingTimeMs", duration);

                return ResponseEntity.ok(response);

            } catch (IllegalArgumentException e) {
                response.put("status", "error");
                response.put("embeddings", List.of());
                response.put("message", e.getMessage());
                return ResponseEntity.badRequest().body(response);

            } catch (Exception e) {
                log.error("Failed to extract embeddings", e);
                response.put("status", "error");
                response.put("embeddings", List.of());
                response.put("message", "Failed to extract embeddings: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Classify an image into categories.
     *
     * @param imageUrl Optional image URL
     * @param file Optional uploaded image file
     * @param topK Number of top predictions to return (default: 5)
     * @return Top predictions with confidence scores
     */
    @PostMapping(value = "/classify", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> classifyImage(
            @RequestParam(required = false) String imageUrl,
            @RequestPart(required = false) MultipartFile file,
            @RequestParam(defaultValue = "5") Integer topK) {

        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            Map<String, Object> response = new HashMap<>();

            try {
                ImageData imgData = resolveImageSource(imageUrl, file);
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

                return ResponseEntity.ok(response);

            } catch (Exception e) {
                log.error("Failed to classify image", e);
                response.put("status", "error");
                response.put("classifications", List.of());
                response.put("message", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    // ========== Object & Scene Analysis ==========

    /**
     * Detect objects in an image.
     *
     * @param imageUrl Optional image URL
     * @param file Optional uploaded image file
     * @return Detected objects with bounding boxes and confidence scores
     */
    @PostMapping(value = "/objects/detect", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> detectObjects(
            @RequestParam(required = false) String imageUrl,
            @RequestPart(required = false) MultipartFile file) {

        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            Map<String, Object> response = new HashMap<>();

            try {
                ImageData imgData = resolveImageSource(imageUrl, file);
                VisionResult result = visionTemplate.detectObjects(imgData);

                List<Map<String, Object>> objects = new ArrayList<>();
                for (var detection : result.detections()) {
                    Map<String, Object> obj = new HashMap<>();
                    obj.put("label", detection.label());
                    obj.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);

                    if (detection.boundingBox() != null) {
                        obj.put("boundingBox", Map.of(
                            "x", detection.boundingBox().x(),
                            "y", detection.boundingBox().y(),
                            "width", detection.boundingBox().width(),
                            "height", detection.boundingBox().height()
                        ));
                    }
                    objects.add(obj);
                }

                long duration = System.currentTimeMillis() - startTime;
                response.put("status", "success");
                response.put("objects", objects);
                response.put("count", result.detectionCount());
                response.put("averageConfidence", Math.round(result.averageConfidence() * 10000.0) / 10000.0);
                response.put("processingTimeMs", duration);

                return ResponseEntity.ok(response);

            } catch (Exception e) {
                log.error("Failed to detect objects", e);
                response.put("status", "error");
                response.put("objects", List.of());
                response.put("message", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    // ========== Text & Document Analysis ==========

    /**
     * Extract text from an image using OCR.
     *
     * @param imageUrl Optional image URL
     * @param file Optional uploaded image file
     * @return Detected text with confidence scores and bounding boxes
     */
    @PostMapping(value = "/text/extract", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> extractText(
            @RequestParam(required = false) String imageUrl,
            @RequestPart(required = false) MultipartFile file) {

        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            Map<String, Object> response = new HashMap<>();

            try {
                ImageData imgData = resolveImageSource(imageUrl, file);
                VisionResult result = visionTemplate.extractText(imgData);

                List<Map<String, Object>> detections = new ArrayList<>();
                StringBuilder fullText = new StringBuilder();

                for (var detection : result.detections()) {
                    Map<String, Object> item = new HashMap<>();
                    String text = (String) detection.attributes().get("text");
                    item.put("text", text);
                    item.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);
                    if (detection.boundingBox() != null) {
                        item.put("boundingBox", Map.of(
                            "x", detection.boundingBox().x(),
                            "y", detection.boundingBox().y(),
                            "width", detection.boundingBox().width(),
                            "height", detection.boundingBox().height()
                        ));
                    }
                    detections.add(item);
                    if (text != null) fullText.append(text).append(" ");
                }

                long duration = System.currentTimeMillis() - startTime;
                response.put("status", "success");
                response.put("text", fullText.toString().trim());
                response.put("detections", detections);
                response.put("count", result.detectionCount());
                response.put("processingTimeMs", duration);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
                log.error("Failed to extract text", e);
                response.put("status", "error");
                response.put("text", "");
                response.put("message", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Scan and decode barcodes/QR codes from an image.
     *
     * @param imageUrl Optional image URL
     * @param file Optional uploaded image file
     * @return Detected barcodes with format, content, and location
     */
    @PostMapping(value = "/barcode/scan", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> scanBarcode(
            @RequestParam(required = false) String imageUrl,
            @RequestPart(required = false) MultipartFile file) {

        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            Map<String, Object> response = new HashMap<>();

            try {
                ImageData imgData = resolveImageSource(imageUrl, file);
                VisionResult result = visionTemplate.scanBarcodes(imgData);

                List<Map<String, Object>> barcodes = new ArrayList<>();
                for (var detection : result.detections()) {
                    Map<String, Object> barcodeInfo = new HashMap<>();
                    barcodeInfo.put("format", detection.label());
                    barcodeInfo.put("content", detection.attributes().get("content"));
                    barcodeInfo.put("confidence", detection.confidence());
                    if (detection.boundingBox() != null) {
                        barcodeInfo.put("location", Map.of(
                            "x", detection.boundingBox().x(),
                            "y", detection.boundingBox().y(),
                            "width", detection.boundingBox().width(),
                            "height", detection.boundingBox().height()
                        ));
                    }
                    barcodes.add(barcodeInfo);
                }

                long duration = System.currentTimeMillis() - startTime;
                response.put("status", "success");
                response.put("count", result.detectionCount());
                response.put("barcodes", barcodes);
                response.put("processingTimeMs", duration);

                return ResponseEntity.ok(response);
            } catch (Exception e) {
                log.error("Failed to scan barcode", e);
                response.put("status", "error");
                response.put("barcodes", List.of());
                response.put("message", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    // ========== Pose & Action Recognition ==========

    /**
     * Detect human poses in an image.
     *
     * @param imageUrl Optional image URL
     * @param file Optional uploaded image file
     * @return Detected poses with joint positions and confidence scores
     */
    @PostMapping(value = "/poses/detect", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> detectPoses(
            @RequestParam(required = false) String imageUrl,
            @RequestPart(required = false) MultipartFile file) {

        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            Map<String, Object> response = new HashMap<>();

            try {
                ImageData imgData = resolveImageSource(imageUrl, file);
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

                return ResponseEntity.ok(response);
            } catch (Exception e) {
                log.error("Failed to detect poses", e);
                response.put("status", "error");
                response.put("poses", List.of());
                response.put("message", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Recognize actions in an image.
     *
     * @param imageUrl Optional image URL
     * @param file Optional uploaded image file
     * @return Recognized actions with confidence scores
     */
    @PostMapping(value = "/actions/recognize", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> recognizeActions(
            @RequestParam(required = false) String imageUrl,
            @RequestPart(required = false) MultipartFile file) {

        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            Map<String, Object> response = new HashMap<>();

            try {
                ImageData imgData = resolveImageSource(imageUrl, file);
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

                return ResponseEntity.ok(response);
            } catch (Exception e) {
                log.error("Failed to recognize actions", e);
                response.put("status", "error");
                response.put("actions", List.of());
                response.put("message", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    // ========== Content Moderation ==========

    /**
     * Detect NSFW content in an image.
     *
     * @param imageUrl Optional image URL
     * @param file Optional uploaded image file
     * @return Classification as 'normal' or 'nsfw' with confidence score
     */
    @PostMapping(value = "/content/nsfw", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> detectNSFW(
            @RequestParam(required = false) String imageUrl,
            @RequestPart(required = false) MultipartFile file) {

        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            Map<String, Object> response = new HashMap<>();

            try {
                ImageData imgData = resolveImageSource(imageUrl, file);
                VisionResult result = visionTemplate.detectNSFW(imgData);
                long duration = System.currentTimeMillis() - startTime;

                if (!result.hasDetections()) {
                    response.put("status", "success");
                    response.put("classification", "unknown");
                    response.put("confidence", 0.0);
                    response.put("isNSFW", false);
                    response.put("processingTimeMs", duration);
                    return ResponseEntity.ok(response);
                }

                var detection = result.detections().get(0);
                boolean isNSFW = (Boolean) detection.attributes().getOrDefault("isNSFW", false);
                String classification = (String) detection.attributes().getOrDefault("classification", detection.label());
                response.put("status", "success");
                response.put("classification", classification);
                response.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);
                response.put("isNSFW", isNSFW);
                response.put("processingTimeMs", duration);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
                log.error("Failed to detect NSFW", e);
                response.put("status", "error");
                response.put("classification", "unknown");
                response.put("message", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    // ========== Emotion & Demographics ==========

    /**
     * Detect emotions in faces.
     *
     * @param imageUrl Optional image URL
     * @param file Optional uploaded image file
     * @return Detected emotions with confidence scores
     */
    @PostMapping(value = "/emotions/detect", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> detectEmotions(
            @RequestParam(required = false) String imageUrl,
            @RequestPart(required = false) MultipartFile file) {

        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            Map<String, Object> response = new HashMap<>();

            try {
                ImageData imgData = resolveImageSource(imageUrl, file);
                VisionResult result = visionTemplate.detectEmotions(imgData);

                List<Map<String, Object>> emotions = new ArrayList<>();
                for (var detection : result.detections()) {
                    Map<String, Object> emotion = new HashMap<>();
                    emotion.put("emotion", detection.label());
                    emotion.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);
                    emotion.put("faceIndex", detection.attributes().get("faceIndex"));

                    if (detection.boundingBox() != null) {
                        emotion.put("boundingBox", Map.of(
                            "x", detection.boundingBox().x(),
                            "y", detection.boundingBox().y(),
                            "width", detection.boundingBox().width(),
                            "height", detection.boundingBox().height()
                        ));
                    }
                    emotions.add(emotion);
                }

                long duration = System.currentTimeMillis() - startTime;
                response.put("status", "success");
                response.put("emotions", emotions);
                response.put("topEmotion", emotions.isEmpty() ? null : emotions.get(0).get("emotion"));
                response.put("count", result.detectionCount());
                response.put("processingTimeMs", duration);

                return ResponseEntity.ok(response);

            } catch (Exception e) {
                log.error("Failed to detect emotions", e);
                response.put("status", "error");
                response.put("emotions", List.of());
                response.put("message", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    // ========== Helper Methods ==========

    /**
     * Resolve image source from URL or uploaded file.
     */
    private ImageData resolveImageSource(String imageUrl, MultipartFile file) throws IOException {
        if (file != null && !file.isEmpty()) {
            byte[] bytes = file.getBytes();
            if (bytes.length > MAX_IMAGE_SIZE_BYTES) {
                throw new IllegalArgumentException("Image size exceeds maximum allowed size of " + MAX_IMAGE_SIZE_BYTES + " bytes");
            }
            return ImageData.fromBytes(bytes);
        }

        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            byte[] bytes = downloadImageFromUrl(imageUrl.trim());
            return ImageData.fromBytes(bytes);
        }

        throw new IllegalArgumentException("Either imageUrl or file must be provided");
    }

    /**
     * Download image from URL with retry logic.
     */
    protected byte[] downloadImageFromUrl(String imageUrl) throws IOException {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            throw new IOException("Image URL is required");
        }

        String trimmed = imageUrl.trim();

        // Support data URIs
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
                    String decoded = java.net.URLDecoder.decode(dataPart, StandardCharsets.UTF_8);
                    imageBytes = decoded.getBytes(StandardCharsets.UTF_8);
                }

                if (imageBytes.length > MAX_IMAGE_SIZE_BYTES) {
                    throw new IOException("Image size exceeds maximum allowed size");
                }
                return imageBytes;
            } catch (IllegalArgumentException e) {
                throw new IOException("Invalid base64 data in data URI", e);
            }
        }

        // Handle file paths and HTTP(S) URLs
        try {
            URI uri = URI.create(trimmed);
            String scheme = uri.getScheme();

            if (scheme == null || scheme.isEmpty() || "file".equalsIgnoreCase(scheme)) {
                Path path = "file".equalsIgnoreCase(scheme) ? Paths.get(uri) : Paths.get(trimmed);

                if (!Files.exists(path)) {
                    throw new IOException("File not found: " + path);
                }

                long size = Files.size(path);
                if (size > MAX_IMAGE_SIZE_BYTES) {
                    throw new IOException("Image size exceeds maximum allowed size");
                }

                return Files.readAllBytes(path);
            }

            if (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")) {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(REQUEST_TIMEOUT)
                    .header("User-Agent", "SpringVision/1.0")
                    .header("Accept", "image/*, */*")
                    .GET()
                    .build();

                try {
                    HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

                    if (response.statusCode() == 200) {
                        try (InputStream inputStream = response.body()) {
                            byte[] imageBytes = inputStream.readNBytes(MAX_IMAGE_SIZE_BYTES + 1);
                            if (imageBytes.length > MAX_IMAGE_SIZE_BYTES) {
                                throw new IOException("Image size exceeds maximum allowed size");
                            }
                            return imageBytes;
                        }
                    }

                    throw new IOException("HTTP error " + response.statusCode());

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Download interrupted", e);
                }
            }

            throw new IOException("Unsupported URI scheme: " + scheme);

        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid URL format", e);
        }
    }

    /**
     * Extract embeddings using VisionTemplate.
     */
    private List<float[]> extractEmbeddingsFromTemplate(ImageData img, DetectionCategory category) {
        try {
            if (visionTemplate != null) {
                VisionBackend backend = visionTemplate.backend();
                if (backend instanceof EmbeddingCapability cap) {
                    try {
                        List<float[]> out = cap.extractEmbeddings(img, category);
                        if (out != null) return out;
                    } catch (Exception e) {
                        log.warn("Backend extractEmbeddings failed", e);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to access backend directly for embeddings", e);
        }

        try {
            List<float[]> res = visionTemplate.extractEmbeddings(img, category);
            if (res != null) return res;
        } catch (Exception e) {
            log.debug("Template extractEmbeddings failed, returning empty list", e);
        }

        return List.of();
    }

    /**
     * Serialize float embedding to bytes.
     */
    private byte[] serializeEmbedding(float[] arr) {
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
}
