package io.github.codesapienbe.springvision.starter.web;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.github.codesapienbe.springvision.core.DetectionType;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.VisionResult;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import io.github.codesapienbe.springvision.starter.web.dto.DetectionRequest;
import io.github.codesapienbe.springvision.starter.web.dto.DetectionResponse;
import io.github.codesapienbe.springvision.starter.web.dto.HealthResponse;
import io.github.codesapienbe.springvision.starter.web.dto.MultipleDetectionRequest;
import io.github.codesapienbe.springvision.starter.web.dto.MultipleDetectionResponse;
import io.github.codesapienbe.springvision.starter.web.dto.TaskSubmissionResponse;

/**
 * REST controller for Spring Vision operations.
 *
 * <p>This controller provides REST API endpoints for computer vision operations
 * including face detection, object detection, and health monitoring. It supports
 * both file uploads and direct image data processing.</p>
 *
 * <p>All endpoints include proper error handling, validation, and logging.
 * Responses include correlation IDs for request tracking and debugging.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Face detection with file upload
 * POST /api/vision/detect/faces
 * Content-Type: multipart/form-data
 *
 * // Object detection with JSON payload
 * POST /api/vision/detect/objects
 * Content-Type: application/json
 *
 * // Health check
 * GET /api/vision/health
 * }</pre>
 *
 * @author Spring Vision Team
 * @see VisionTemplate
 * @see DetectionRequest
 * @see DetectionResponse
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/vision")
@CrossOrigin(origins = "*")
public class VisionController {

    private static final Logger logger = LoggerFactory.getLogger(VisionController.class);
    
    // File upload constraints
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> SUPPORTED_CONTENT_TYPES = List.of(
        "image/jpeg", "image/jpg", "image/png", "image/gif", "image/bmp", "image/webp"
    );

    /**
     * The vision template for processing operations.
     */
    private final VisionTemplate visionTemplate;

    /**
     * Constructs a new vision controller.
     *
     * @param visionTemplate the vision template for processing operations
     */
    public VisionController(VisionTemplate visionTemplate) {
        this.visionTemplate = visionTemplate;
    }


    /**
     * Counts faces in an uploaded image (simple count response).
     * @param file The uploaded image file.
     * @return Simple count response.
     */
    @PostMapping(value = "/faces/count", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> countFaces(@RequestParam("file") MultipartFile file) {
        String correlationId = generateCorrelationId();
        long startTime = System.currentTimeMillis();

        try {
            validateFile(file);
            ImageData imageData = convertToImageData(file);
            VisionResult result = visionTemplate.detectFaces(imageData);

            Map<String, Object> response = Map.of(
                "correlationId", correlationId,
                "status", "success",
                "count", result.detectionCount(),
                "averageConfidence", Math.round(result.averageConfidence() * 10000.0) / 10000.0,
                "processingTimeMs", System.currentTimeMillis() - startTime,
                "message", String.format("Detected %d faces", result.detectionCount())
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                "correlationId", correlationId,
                "status", "error",
                "count", 0,
                "message", "Failed to count faces: " + e.getMessage(),
                "processingTimeMs", System.currentTimeMillis() - startTime
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Extracts face embeddings from an uploaded image.
     * @param file The uploaded image file.
     * @return Embeddings response with base64-encoded vectors.
     */
    @PostMapping(value = "/faces/embeddings", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> extractEmbeddings(@RequestParam("file") MultipartFile file) {
        String correlationId = generateCorrelationId();
        long startTime = System.currentTimeMillis();

        try {
            validateFile(file);
            ImageData imageData = convertToImageData(file);
            List<float[]> embeddings = visionTemplate.extractEmbeddings(imageData,
                io.github.codesapienbe.springvision.core.DetectionCategory.FACE);

            List<Map<String, Object>> embeddingsList = new java.util.ArrayList<>();
            for (int i = 0; i < embeddings.size(); i++) {
                float[] emb = embeddings.get(i);
                Map<String, Object> item = Map.of(
                    "id", "face-" + i,
                    "embedding_base64", java.util.Base64.getEncoder().encodeToString(serializeEmbedding(emb)),
                    "length", emb.length
                );
                embeddingsList.add(item);
            }

            Map<String, Object> response = Map.of(
                "correlationId", correlationId,
                "status", "success",
                "count", embeddings.size(),
                "embeddings", embeddingsList,
                "processingTimeMs", System.currentTimeMillis() - startTime
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                "correlationId", correlationId,
                "status", "error",
                "message", "Failed to extract embeddings: " + e.getMessage(),
                "embeddings", List.of(),
                "processingTimeMs", System.currentTimeMillis() - startTime
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Classifies an image into categories.
     * @param file The uploaded image file.
     * @param topK Number of top predictions to return (default: 5).
     * @return Classification results.
     */
    @PostMapping(value = "/classify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> classifyImage(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "topK", defaultValue = "5") Integer topK) {

        String correlationId = generateCorrelationId();
        long startTime = System.currentTimeMillis();

        try {
            validateFile(file);
            ImageData imageData = convertToImageData(file);
            VisionResult result = visionTemplate.classifyImage(imageData, topK);

            List<Map<String, Object>> classifications = new java.util.ArrayList<>();
            for (var detection : result.detections()) {
                classifications.add(Map.of(
                    "label", detection.label(),
                    "confidence", Math.round(detection.confidence() * 10000.0) / 10000.0
                ));
            }

            Map<String, Object> response = Map.of(
                "correlationId", correlationId,
                "status", "success",
                "classifications", classifications,
                "topPrediction", classifications.isEmpty() ? null : classifications.get(0).get("label"),
                "count", result.detectionCount(),
                "processingTimeMs", System.currentTimeMillis() - startTime
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                "correlationId", correlationId,
                "status", "error",
                "message", "Failed to classify image: " + e.getMessage(),
                "classifications", List.of(),
                "processingTimeMs", System.currentTimeMillis() - startTime
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Detects human poses in an image.
     * @param file The uploaded image file.
     * @return Pose detection results.
     */
    @PostMapping(value = "/detect/poses", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> detectPoses(@RequestParam("file") MultipartFile file) {
        String correlationId = generateCorrelationId();
        long startTime = System.currentTimeMillis();

        try {
            validateFile(file);
            ImageData imageData = convertToImageData(file);
            VisionResult result = visionTemplate.detectPoses(imageData);

            List<Map<String, Object>> poses = new java.util.ArrayList<>();
            for (var detection : result.detections()) {
                poses.add(Map.of(
                    "label", detection.label(),
                    "confidence", Math.round(detection.confidence() * 10000.0) / 10000.0,
                    "attributes", detection.attributes()
                ));
            }

            Map<String, Object> response = Map.of(
                "correlationId", correlationId,
                "status", "success",
                "poses", poses,
                "count", result.detectionCount(),
                "processingTimeMs", System.currentTimeMillis() - startTime,
                "message", String.format("Detected %d poses", result.detectionCount())
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                "correlationId", correlationId,
                "status", "error",
                "message", "Failed to detect poses: " + e.getMessage(),
                "poses", List.of(),
                "processingTimeMs", System.currentTimeMillis() - startTime
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Recognizes actions in an image.
     * @param file The uploaded image file.
     * @return Action recognition results.
     */
    @PostMapping(value = "/recognize/actions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> recognizeActions(@RequestParam("file") MultipartFile file) {
        String correlationId = generateCorrelationId();
        long startTime = System.currentTimeMillis();

        try {
            validateFile(file);
            ImageData imageData = convertToImageData(file);
            VisionResult result = visionTemplate.recognizeActions(imageData);

            List<Map<String, Object>> actions = new java.util.ArrayList<>();
            for (var detection : result.detections()) {
                actions.add(Map.of(
                    "action", detection.label(),
                    "confidence", Math.round(detection.confidence() * 10000.0) / 10000.0
                ));
            }

            Map<String, Object> response = Map.of(
                "correlationId", correlationId,
                "status", "success",
                "actions", actions,
                "topAction", actions.isEmpty() ? null : actions.get(0).get("action"),
                "count", result.detectionCount(),
                "processingTimeMs", System.currentTimeMillis() - startTime
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                "correlationId", correlationId,
                "status", "error",
                "message", "Failed to recognize actions: " + e.getMessage(),
                "actions", List.of(),
                "processingTimeMs", System.currentTimeMillis() - startTime
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Detects NSFW (Not Safe For Work) content in an image.
     * @param file The uploaded image file.
     * @return NSFW detection result.
     */
    @PostMapping(value = "/detect/nsfw", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> detectNSFW(@RequestParam("file") MultipartFile file) {
        String correlationId = generateCorrelationId();
        long startTime = System.currentTimeMillis();

        try {
            validateFile(file);
            ImageData imageData = convertToImageData(file);
            VisionResult result = visionTemplate.detectNSFW(imageData);

            if (!result.hasDetections()) {
                Map<String, Object> response = Map.of(
                    "correlationId", correlationId,
                    "status", "success",
                    "classification", "unknown",
                    "confidence", 0.0,
                    "isNSFW", false,
                    "processingTimeMs", System.currentTimeMillis() - startTime
                );
                return ResponseEntity.ok(response);
            }

            var detection = result.detections().get(0);
            boolean isNSFW = (Boolean) detection.attributes().getOrDefault("isNSFW", false);
            String classification = (String) detection.attributes().getOrDefault("classification", detection.label());

            Map<String, Object> response = Map.of(
                "correlationId", correlationId,
                "status", "success",
                "classification", classification,
                "confidence", Math.round(detection.confidence() * 10000.0) / 10000.0,
                "isNSFW", isNSFW,
                "processingTimeMs", System.currentTimeMillis() - startTime
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                "correlationId", correlationId,
                "status", "error",
                "message", "Failed to detect NSFW content: " + e.getMessage(),
                "classification", "unknown",
                "processingTimeMs", System.currentTimeMillis() - startTime
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Detects emotions from faces in an image.
     * @param file The uploaded image file.
     * @return Emotion detection results.
     */
    @PostMapping(value = "/detect/emotions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> detectEmotions(@RequestParam("file") MultipartFile file) {
        String correlationId = generateCorrelationId();
        long startTime = System.currentTimeMillis();

        try {
            validateFile(file);
            ImageData imageData = convertToImageData(file);
            VisionResult result = visionTemplate.detectEmotions(imageData);

            List<Map<String, Object>> emotions = new java.util.ArrayList<>();
            for (var detection : result.detections()) {
                java.util.Map<String, Object> emotion = new java.util.HashMap<>();
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

            Map<String, Object> response = Map.of(
                "correlationId", correlationId,
                "status", "success",
                "emotions", emotions,
                "topEmotion", emotions.isEmpty() ? null : emotions.get(0).get("emotion"),
                "count", result.detectionCount(),
                "processingTimeMs", System.currentTimeMillis() - startTime
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                "correlationId", correlationId,
                "status", "error",
                "message", "Failed to detect emotions: " + e.getMessage(),
                "emotions", List.of(),
                "processingTimeMs", System.currentTimeMillis() - startTime
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Detects deepfakes in an image.
     * @param file The uploaded image file.
     * @return Deepfake detection result.
     */
    @PostMapping(value = "/detect/deepfake", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> detectDeepfake(@RequestParam("file") MultipartFile file) {
        String correlationId = generateCorrelationId();
        long startTime = System.currentTimeMillis();

        try {
            validateFile(file);
            ImageData imageData = convertToImageData(file);
            VisionResult result = visionTemplate.detectDeepfake(imageData);

            if (!result.hasDetections()) {
                Map<String, Object> response = Map.of(
                    "correlationId", correlationId,
                    "status", "success",
                    "classification", "unknown",
                    "confidence", 0.0,
                    "isFake", false,
                    "processingTimeMs", System.currentTimeMillis() - startTime
                );
                return ResponseEntity.ok(response);
            }

            var detection = result.detections().get(0);
            boolean isFake = (Boolean) detection.attributes().getOrDefault("isFake", false);
            String classification = (String) detection.attributes().getOrDefault("classification", detection.label());
            String manipulationType = (String) detection.attributes().get("manipulationType");

            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("correlationId", correlationId);
            response.put("status", "success");
            response.put("classification", classification);
            response.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);
            response.put("isFake", isFake);
            if (manipulationType != null) {
                response.put("manipulationType", manipulationType);
            }
            response.put("processingTimeMs", System.currentTimeMillis() - startTime);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                "correlationId", correlationId,
                "status", "error",
                "message", "Failed to detect deepfake: " + e.getMessage(),
                "classification", "unknown",
                "processingTimeMs", System.currentTimeMillis() - startTime
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Detects hands in an image.
     * @param file The uploaded image file.
     * @return Hand detection results.
     */
    @PostMapping(value = "/detect/hands", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> detectHands(@RequestParam("file") MultipartFile file) {
        String correlationId = generateCorrelationId();
        long startTime = System.currentTimeMillis();

        try {
            validateFile(file);
            ImageData imageData = convertToImageData(file);
            VisionResult result = visionTemplate.detectHands(imageData);

            List<Map<String, Object>> hands = new java.util.ArrayList<>();
            for (var detection : result.detections()) {
                java.util.Map<String, Object> hand = new java.util.HashMap<>();
                hand.put("label", detection.label());
                hand.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);

                if (detection.boundingBox() != null) {
                    hand.put("boundingBox", Map.of(
                        "x", detection.boundingBox().x(),
                        "y", detection.boundingBox().y(),
                        "width", detection.boundingBox().width(),
                        "height", detection.boundingBox().height()
                    ));
                }
                hands.add(hand);
            }

            Map<String, Object> response = Map.of(
                "correlationId", correlationId,
                "status", "success",
                "hands", hands,
                "count", result.detectionCount(),
                "processingTimeMs", System.currentTimeMillis() - startTime
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                "correlationId", correlationId,
                "status", "error",
                "message", "Failed to detect hands: " + e.getMessage(),
                "hands", List.of(),
                "processingTimeMs", System.currentTimeMillis() - startTime
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Detects demographics (age and gender) from faces in an image.
     * @param file The uploaded image file.
     * @return Demographics detection results.
     */
    @PostMapping(value = "/detect/demographics", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> detectDemographics(@RequestParam("file") MultipartFile file) {
        String correlationId = generateCorrelationId();
        long startTime = System.currentTimeMillis();

        try {
            validateFile(file);
            ImageData imageData = convertToImageData(file);
            VisionResult result = visionTemplate.detectDemographics(imageData);

            List<Map<String, Object>> demographics = new java.util.ArrayList<>();
            for (var detection : result.detections()) {
                java.util.Map<String, Object> demo = new java.util.HashMap<>();
                demo.put("gender", detection.label());
                demo.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);
                demo.put("age", detection.attributes().get("age"));
                demo.put("ageRange", detection.attributes().get("ageRange"));
                demo.put("genderConfidence", detection.attributes().get("genderConfidence"));
                demo.put("ageError", detection.attributes().get("ageError"));
                demo.put("faceIndex", detection.attributes().get("faceIndex"));

                if (detection.boundingBox() != null) {
                    demo.put("boundingBox", Map.of(
                        "x", detection.boundingBox().x(),
                        "y", detection.boundingBox().y(),
                        "width", detection.boundingBox().width(),
                        "height", detection.boundingBox().height()
                    ));
                }
                demographics.add(demo);
            }

            Map<String, Object> response = Map.of(
                "correlationId", correlationId,
                "status", "success",
                "demographics", demographics,
                "facesAnalyzed", result.detectionCount(),
                "processingTimeMs", System.currentTimeMillis() - startTime
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                "correlationId", correlationId,
                "status", "error",
                "message", "Failed to detect demographics: " + e.getMessage(),
                "demographics", List.of(),
                "processingTimeMs", System.currentTimeMillis() - startTime
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Detects falls from body pose analysis.
     * @param file The uploaded image file.
     * @return Fall detection result.
     */
    @PostMapping(value = "/detect/fall", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> detectFall(@RequestParam("file") MultipartFile file) {
        String correlationId = generateCorrelationId();
        long startTime = System.currentTimeMillis();

        try {
            validateFile(file);
            ImageData imageData = convertToImageData(file);
            VisionResult result = visionTemplate.detectFall(List.of(imageData));

            if (!result.hasDetections()) {
                Map<String, Object> response = Map.of(
                    "correlationId", correlationId,
                    "status", "success",
                    "fallDetected", false,
                    "bodyOrientation", "unknown",
                    "riskLevel", "low",
                    "message", "No person detected",
                    "processingTimeMs", System.currentTimeMillis() - startTime
                );
                return ResponseEntity.ok(response);
            }

            var detection = result.detections().get(0);
            boolean fallDetected = (Boolean) detection.attributes().getOrDefault("fallDetected", false);
            String bodyOrientation = (String) detection.attributes().getOrDefault("bodyOrientation", "unknown");
            String riskLevel = (String) detection.attributes().getOrDefault("riskLevel", "low");
            Double aspectRatio = (Double) detection.attributes().get("aspectRatio");
            Double headHeight = (Double) detection.attributes().get("headHeight");
            String analysisDetails = (String) detection.attributes().get("analysisDetails");

            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("correlationId", correlationId);
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
            if (detection.boundingBox() != null) {
                response.put("boundingBox", Map.of(
                    "x", detection.boundingBox().x(),
                    "y", detection.boundingBox().y(),
                    "width", detection.boundingBox().width(),
                    "height", detection.boundingBox().height()
                ));
            }
            response.put("processingTimeMs", System.currentTimeMillis() - startTime);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                "correlationId", correlationId,
                "status", "error",
                "message", "Failed to detect fall: " + e.getMessage(),
                "fallDetected", false,
                "processingTimeMs", System.currentTimeMillis() - startTime
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Analyzes stress levels from facial expressions.
     * @param file The uploaded image file.
     * @return Stress analysis result.
     */
    @PostMapping(value = "/analyze/stress", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> analyzeStress(@RequestParam("file") MultipartFile file) {
        String correlationId = generateCorrelationId();
        long startTime = System.currentTimeMillis();

        try {
            validateFile(file);
            ImageData imageData = convertToImageData(file);
            VisionResult result = visionTemplate.analyzeStress(List.of(imageData));

            if (!result.hasDetections()) {
                Map<String, Object> response = Map.of(
                    "correlationId", correlationId,
                    "status", "success",
                    "stressLevel", "unknown",
                    "stressScore", 0.0,
                    "message", "No face detected",
                    "processingTimeMs", System.currentTimeMillis() - startTime
                );
                return ResponseEntity.ok(response);
            }

            var detection = result.detections().get(0);
            String stressLevel = (String) detection.attributes().getOrDefault("stressLevel", "unknown");
            Double stressScore = (Double) detection.attributes().get("stressScore");
            String dominantEmotion = (String) detection.attributes().get("dominantEmotion");
            Double emotionIntensity = (Double) detection.attributes().get("emotionIntensity");
            @SuppressWarnings("unchecked")
            List<String> indicators = (List<String>) detection.attributes().get("indicators");

            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("correlationId", correlationId);
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
            if (detection.boundingBox() != null) {
                response.put("boundingBox", Map.of(
                    "x", detection.boundingBox().x(),
                    "y", detection.boundingBox().y(),
                    "width", detection.boundingBox().width(),
                    "height", detection.boundingBox().height()
                ));
            }
            response.put("disclaimer", "Not for medical diagnosis - research and wellness monitoring only");
            response.put("processingTimeMs", System.currentTimeMillis() - startTime);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                "correlationId", correlationId,
                "status", "error",
                "message", "Failed to analyze stress: " + e.getMessage(),
                "stressLevel", "unknown",
                "processingTimeMs", System.currentTimeMillis() - startTime
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Extracts image metadata including EXIF, GPS, and camera information.
     * @param file The uploaded image file.
     * @return Metadata extraction result.
     */
    @PostMapping(value = "/metadata/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> extractMetadata(@RequestParam("file") MultipartFile file) {
        String correlationId = generateCorrelationId();
        long startTime = System.currentTimeMillis();

        try {
            validateFile(file);
            ImageData imageData = convertToImageData(file);
            VisionResult result = visionTemplate.extractMetadata(imageData);

            Map<String, Map<String, Object>> metadataGroups = new java.util.HashMap<>();
            for (var detection : result.detections()) {
                String type = detection.label(); // "gps", "exif", or "metadata"
                Map<String, Object> groupData = new java.util.HashMap<>(detection.attributes());
                groupData.remove("backend");
                groupData.remove("type");
                metadataGroups.put(type, groupData);
            }

            Map<String, Object> response = Map.of(
                "correlationId", correlationId,
                "status", "success",
                "metadata", metadataGroups,
                "groupCount", metadataGroups.size(),
                "processingTimeMs", System.currentTimeMillis() - startTime,
                "backend", result.metadata().get("backendId")
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                "correlationId", correlationId,
                "status", "error",
                "message", "Failed to extract metadata: " + e.getMessage(),
                "metadata", Map.of(),
                "processingTimeMs", System.currentTimeMillis() - startTime
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Detects security threats including weapons, violence, and suspicious objects.
     * @param file The uploaded image file.
     * @return Threat detection results.
     */
    @PostMapping(value = "/security/threats", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> detectThreats(@RequestParam("file") MultipartFile file) {
        String correlationId = generateCorrelationId();
        long startTime = System.currentTimeMillis();

        try {
            validateFile(file);
            ImageData imageData = convertToImageData(file);
            VisionResult result = visionTemplate.detectThreats(List.of(imageData));

            List<Map<String, Object>> threats = new java.util.ArrayList<>();
            int highSeverityCount = 0;

            for (var detection : result.detections()) {
                java.util.Map<String, Object> threat = new java.util.HashMap<>();
                threat.put("label", detection.label());
                threat.put("confidence", Math.round(detection.confidence() * 10000.0) / 10000.0);

                if (detection.boundingBox() != null) {
                    threat.put("boundingBox", Map.of(
                        "x", detection.boundingBox().x(),
                        "y", detection.boundingBox().y(),
                        "width", detection.boundingBox().width(),
                        "height", detection.boundingBox().height()
                    ));
                }

                String threatType = (String) detection.attributes().get("threatType");
                String severity = (String) detection.attributes().get("severity");
                String weaponClass = (String) detection.attributes().get("weaponClass");
                String description = (String) detection.attributes().get("description");

                threat.put("threatType", threatType);
                threat.put("severity", severity);
                threat.put("weaponClass", weaponClass);
                threat.put("description", description);

                if ("HIGH".equals(severity) || "CRITICAL".equals(severity)) {
                    highSeverityCount++;
                }

                threats.add(threat);
            }

            Map<String, Object> response = Map.of(
                "correlationId", correlationId,
                "status", "success",
                "threats", threats,
                "threatCount", threats.size(),
                "highSeverityCount", highSeverityCount,
                "processingTimeMs", System.currentTimeMillis() - startTime,
                "disclaimer", "For legitimate security and safety use only. Comply with local surveillance laws and privacy regulations.",
                "warning", "False positives may occur. Human verification recommended for critical decisions."
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                "correlationId", correlationId,
                "status", "error",
                "message", "Failed to detect threats: " + e.getMessage(),
                "threats", List.of(),
                "threatCount", 0,
                "processingTimeMs", System.currentTimeMillis() - startTime
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Authenticates access using biometric face recognition.
     * @param file The uploaded image file.
     * @return Authentication result.
     */
    @PostMapping(value = "/security/authenticate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> authenticateAccess(@RequestParam("file") MultipartFile file) {
        String correlationId = generateCorrelationId();
        long startTime = System.currentTimeMillis();

        try {
            validateFile(file);
            ImageData imageData = convertToImageData(file);
            VisionResult result = visionTemplate.authenticateAccess(imageData);

            if (!result.hasDetections()) {
                Map<String, Object> response = Map.of(
                    "correlationId", correlationId,
                    "status", "error",
                    "message", "Authentication failed - no results",
                    "authorized", false,
                    "processingTimeMs", System.currentTimeMillis() - startTime
                );
                return ResponseEntity.ok(response);
            }

            var authResult = result.detections().get(0);
            Boolean authorized = (Boolean) authResult.attributes().get("authorized");
            Double confidence = (Double) authResult.attributes().get("confidence");
            Double matchScore = (Double) authResult.attributes().get("matchScore");
            String timestamp = (String) authResult.attributes().get("timestamp");
            String userId = (String) authResult.attributes().get("userId");
            String userName = (String) authResult.attributes().get("userName");
            String reason = (String) authResult.attributes().get("reason");

            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("correlationId", correlationId);
            response.put("status", "success");
            response.put("authorized", Boolean.TRUE.equals(authorized));
            response.put("label", authResult.label());
            response.put("confidence", confidence != null ? Math.round(confidence * 10000.0) / 10000.0 : 0.0);
            response.put("matchScore", matchScore != null ? Math.round(matchScore * 10000.0) / 10000.0 : 0.0);
            response.put("timestamp", timestamp);
            response.put("processingTimeMs", System.currentTimeMillis() - startTime);

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

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                "correlationId", correlationId,
                "status", "error",
                "message", "Failed to authenticate access: " + e.getMessage(),
                "authorized", false,
                "processingTimeMs", System.currentTimeMillis() - startTime
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Helper method for serializing embeddings
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

    // ===== Original Vision Endpoints =====

    @PostMapping(value = "/detect/faces", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VisionResult> detectFacesFromFile(
        @RequestParam("file") MultipartFile file,
        @RequestParam(name = "minConfidence", required = false) Double minConfidence) {
        
        String correlationId = generateCorrelationId();
        logger.info("Face detection request received", Map.of(
            "correlationId", correlationId,
            "fileName", file.getOriginalFilename(),
            "fileSize", file.getSize(),
            "contentType", file.getContentType(),
            "minConfidence", minConfidence
        ));

        try {
            validateFile(file);
            ImageData imageData = convertToImageData(file);
            VisionResult result = visionTemplate.detectFaces(imageData);

            // Filter by confidence if specified
            if (minConfidence != null) {
                List<io.github.codesapienbe.springvision.core.Detection> filteredDetections = result.detections().stream()
                    .filter(detection -> detection.confidence() >= minConfidence)
                    .toList();
                
                // Create new VisionResult with filtered detections
                result = VisionResult.of(
                    result.detectionType(),
                    filteredDetections,
                    filteredDetections.isEmpty() ? 0.0 : 
                        filteredDetections.stream().mapToDouble(io.github.codesapienbe.springvision.core.Detection::confidence).average().orElse(0.0),
                    result.processingTimeMs(),
                    Map.of("correlationId", correlationId, "filtered", true)
                );
            } else {
                // Add correlation ID to metadata
                result = VisionResult.of(
                    result.detectionType(),
                    result.detections(),
                    result.averageConfidence(),
                    result.processingTimeMs(),
                    Map.of("correlationId", correlationId)
                );
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Face detection failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(VisionResult.empty(DetectionType.FACE, 0L));
        }
    }

    @PostMapping(value = "/detect/faces", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VisionResult> detectFacesFromData(
        @RequestBody DetectionRequest request,
        @RequestParam(name = "minConfidence", required = false) Double minConfidence) {
        
        String correlationId = generateCorrelationId();
        logger.info("Face detection request received (JSON)", Map.of(
            "correlationId", correlationId,
            "imageSize", request.getImageData() != null ? request.getImageData().length : 0,
            "minConfidence", minConfidence
        ));

        try {
            ImageData imageData = ImageData.fromBytes(request.getImageData());
            VisionResult result = visionTemplate.detectFaces(imageData);

            // Filter by confidence if specified
            if (minConfidence != null) {
                List<io.github.codesapienbe.springvision.core.Detection> filteredDetections = result.detections().stream()
                    .filter(detection -> detection.confidence() >= minConfidence)
                    .toList();
                
                // Create new VisionResult with filtered detections
                result = VisionResult.of(
                    result.detectionType(),
                    filteredDetections,
                    filteredDetections.isEmpty() ? 0.0 : 
                        filteredDetections.stream().mapToDouble(io.github.codesapienbe.springvision.core.Detection::confidence).average().orElse(0.0),
                    result.processingTimeMs(),
                    Map.of("correlationId", correlationId, "filtered", true)
                );
            } else {
                // Add correlation ID to metadata
                result = VisionResult.of(
                    result.detectionType(),
                    result.detections(),
                    result.averageConfidence(),
                    result.processingTimeMs(),
                    Map.of("correlationId", correlationId)
                );
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Face detection failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(VisionResult.empty(DetectionType.FACE, 0L));
        }
    }

    @PostMapping(value = "/detect/objects", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VisionResult> detectObjectsFromFile(@RequestParam("file") MultipartFile file) {
        String correlationId = generateCorrelationId();
        logger.info("Object detection request received", Map.of(
            "correlationId", correlationId,
            "fileName", file.getOriginalFilename(),
            "fileSize", file.getSize(),
            "contentType", file.getContentType()
        ));

        try {
            validateFile(file);
            ImageData imageData = convertToImageData(file);
            VisionResult result = visionTemplate.detectObjects(imageData);

            // Add correlation ID to metadata
            result = VisionResult.of(
                result.detectionType(),
                result.detections(),
                result.averageConfidence(),
                result.processingTimeMs(),
                Map.of("correlationId", correlationId)
            );

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Object detection failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(VisionResult.empty(DetectionType.OBJECT, 0L));
        }
    }

    @PostMapping(value = "/detect/objects", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VisionResult> detectObjectsFromData(@RequestBody DetectionRequest request) {
        String correlationId = generateCorrelationId();
        logger.info("Object detection request received (JSON)", Map.of(
            "correlationId", correlationId,
            "imageSize", request.getImageData() != null ? request.getImageData().length : 0
        ));

        try {
            ImageData imageData = ImageData.fromBytes(request.getImageData());
            VisionResult result = visionTemplate.detectObjects(imageData);

            // Add correlation ID to metadata
            result = VisionResult.of(
                result.detectionType(),
                result.detections(),
                result.averageConfidence(),
                result.processingTimeMs(),
                Map.of("correlationId", correlationId)
            );

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Object detection failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(VisionResult.empty(DetectionType.OBJECT, 0L));
        }
    }

    @PostMapping(value = "/detect/barcodes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VisionResult> detectBarcodesFromFile(@RequestParam("file") MultipartFile file) {
        String correlationId = generateCorrelationId();
        logger.info("Barcode detection request received", Map.of(
            "correlationId", correlationId,
            "fileName", file.getOriginalFilename(),
            "fileSize", file.getSize(),
            "contentType", file.getContentType()
        ));

        try {
            validateFile(file);
            ImageData imageData = convertToImageData(file);
            VisionResult result = visionTemplate.scanBarcodes(imageData);

            // Add correlation ID to metadata
            result = VisionResult.of(
                result.detectionType(),
                result.detections(),
                result.averageConfidence(),
                result.processingTimeMs(),
                Map.of("correlationId", correlationId)
            );

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Barcode detection failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(VisionResult.empty(DetectionType.BARCODE, 0L));
        }
    }

    @PostMapping(value = "/detect/barcodes", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VisionResult> detectBarcodesFromData(@RequestBody DetectionRequest request) {
        String correlationId = generateCorrelationId();
        logger.info("Barcode detection request received (JSON)", Map.of(
            "correlationId", correlationId,
            "imageSize", request.getImageData() != null ? request.getImageData().length : 0
        ));

        try {
            ImageData imageData = ImageData.fromBytes(request.getImageData());
            VisionResult result = visionTemplate.scanBarcodes(imageData);

            // Add correlation ID to metadata
            result = VisionResult.of(
                result.detectionType(),
                result.detections(),
                result.averageConfidence(),
                result.processingTimeMs(),
                Map.of("correlationId", correlationId)
            );

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Barcode detection failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(VisionResult.empty(DetectionType.BARCODE, 0L));
        }
    }

    @PostMapping(value = "/detect/text", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VisionResult> detectTextFromFile(@RequestParam("file") MultipartFile file) {
        String correlationId = generateCorrelationId();
        logger.info("Text detection request received", Map.of(
            "correlationId", correlationId,
            "fileName", file.getOriginalFilename(),
            "fileSize", file.getSize(),
            "contentType", file.getContentType()
        ));

        try {
            validateFile(file);
            ImageData imageData = convertToImageData(file);
            VisionResult result = visionTemplate.extractText(imageData);

            // Add correlation ID to metadata
            result = VisionResult.of(
                result.detectionType(),
                result.detections(),
                result.averageConfidence(),
                result.processingTimeMs(),
                Map.of("correlationId", correlationId)
            );

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Text detection failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(VisionResult.empty(DetectionType.TEXT, 0L));
        }
    }

    @PostMapping(value = "/detect/text", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VisionResult> detectTextFromData(@RequestBody DetectionRequest request) {
        String correlationId = generateCorrelationId();
        logger.info("Text detection request received (JSON)", Map.of(
            "correlationId", correlationId,
            "imageSize", request.getImageData() != null ? request.getImageData().length : 0
        ));

        try {
            ImageData imageData = ImageData.fromBytes(request.getImageData());
            VisionResult result = visionTemplate.extractText(imageData);

            // Add correlation ID to metadata
            result = VisionResult.of(
                result.detectionType(),
                result.detections(),
                result.averageConfidence(),
                result.processingTimeMs(),
                Map.of("correlationId", correlationId)
            );

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Text detection failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(VisionResult.empty(DetectionType.TEXT, 0L));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> getHealth() {
        String correlationId = generateCorrelationId();
        logger.debug("Health check request received", Map.of("correlationId", correlationId));

        try {
            var healthInfo = visionTemplate.getBackendHealthInfo();
            HealthResponse response = new HealthResponse(
                correlationId,
                healthInfo.backendId(),
                "Vision Backend",
                "1.0.8",
                healthInfo.status().toString(),
                healthInfo.statusMessage(),
                healthInfo.responseTimeMs(),
                List.of("face", "object", "text", "barcode"),
                null
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Health check failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new HealthResponse(correlationId, "unknown", "Vision Backend", "1.0.8", "DOWN", "Health check failed", 0L, List.of(), e.getMessage()));
        }
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getInfo() {
        String correlationId = generateCorrelationId();
        logger.debug("Info request received", Map.of("correlationId", correlationId));

        try {
            var healthInfo = visionTemplate.getBackendHealthInfo();
            Map<String, Object> response = Map.of(
                "correlationId", correlationId,
                "status", "success",
                "backend", Map.of(
                    "id", healthInfo.backendId(),
                    "status", healthInfo.status().toString(),
                    "responseTimeMs", healthInfo.responseTimeMs(),
                    "supportedDetectionTypes", List.of("face", "object", "text", "barcode")
                ),
                "version", "1.0.8",
                "message", "Info retrieved successfully"
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Info request failed", Map.of(
                "correlationId", correlationId,
                "error", e.getClass().getSimpleName()
            ), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "correlationId", correlationId,
                    "status", "error",
                    "message", e.getMessage()
                ));
        }
    }

    // ===== Capability-Based Detection Helper Methods =====

    /**
     * Executes capability-based detection for a given DetectionType.
     * This replaces the deprecated visionTemplate.detect() pattern.
     *
     * @param imageData     the image data to process
     * @param detectionType the type of detection to perform
     * @return VisionResult containing detections
     */
    private VisionResult executeCapabilityDetection(ImageData imageData, DetectionType detectionType) {
        long startTime = System.currentTimeMillis();
        List<io.github.codesapienbe.springvision.core.Detection> detections;

        // Route to appropriate capability based on detection type
        switch (detectionType) {
            case FACE -> {
                io.github.codesapienbe.springvision.core.capabilities.FaceDetectionCapability backend =
                    (io.github.codesapienbe.springvision.core.capabilities.FaceDetectionCapability) visionTemplate.backend();
                detections = backend.detectFaces(imageData);
            }
            case OBJECT -> {
                io.github.codesapienbe.springvision.core.capabilities.ObjectDetectionCapability backend =
                    (io.github.codesapienbe.springvision.core.capabilities.ObjectDetectionCapability) visionTemplate.backend();
                detections = backend.detectObjects(imageData);
            }
            case TEXT -> {
                io.github.codesapienbe.springvision.core.capabilities.OcrCapability backend =
                    (io.github.codesapienbe.springvision.core.capabilities.OcrCapability) visionTemplate.backend();
                List<io.github.codesapienbe.springvision.core.capabilities.OcrCapability.TextDetection> textDetections = backend.extractText(imageData);
                // Convert TextDetection to Detection
                detections = textDetections.stream()
                    .map(td -> new io.github.codesapienbe.springvision.core.Detection(
                        td.text(),
                        td.confidence(),
                        td.boundingBox() != null ? convertToBox(td.boundingBox()) : null,
                        Map.of("text", td.text(), "attributes", td.attributes())
                    ))
                    .toList();
            }
            case BARCODE -> {
                io.github.codesapienbe.springvision.core.capabilities.BarcodeCapability backend =
                    (io.github.codesapienbe.springvision.core.capabilities.BarcodeCapability) visionTemplate.backend();
                detections = backend.detectBarcodes(imageData);
            }
            default -> throw new UnsupportedOperationException("Unsupported detection type: " + detectionType);
        }

        long processingTime = System.currentTimeMillis() - startTime;
        double avgConfidence = detections.isEmpty() ? 0.0 :
            detections.stream().mapToDouble(io.github.codesapienbe.springvision.core.Detection::confidence).average().orElse(0.0);

        return VisionResult.of(detectionType, detections, avgConfidence, processingTime);
    }

    /**
     * Executes multiple capability-based detections.
     *
     * @param imageData      the image data to process
     * @param detectionTypes the list of detection types to perform
     * @return list of VisionResults
     */
    private List<VisionResult> executeMultipleCapabilityDetections(ImageData imageData, List<DetectionType> detectionTypes) {
        return detectionTypes.stream()
            .map(type -> executeCapabilityDetection(imageData, type))
            .toList();
    }

    /**
     * Helper to convert OCR bounding box map to BoundingBox.
     */
    private io.github.codesapienbe.springvision.core.BoundingBox convertToBox(Map<String, Object> bboxMap) {
        try {
            double x = ((Number) bboxMap.get("x")).doubleValue();
            double y = ((Number) bboxMap.get("y")).doubleValue();
            double width = ((Number) bboxMap.get("width")).doubleValue();
            double height = ((Number) bboxMap.get("height")).doubleValue();
            return new io.github.codesapienbe.springvision.core.BoundingBox(x, y, width, height);
        } catch (Exception e) {
            return new io.github.codesapienbe.springvision.core.BoundingBox(0, 0, 0, 0);
        }
    }

    /**
     * Validates uploaded file for size and content type.
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required and cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of " + MAX_FILE_SIZE + " bytes");
        }

        String contentType = file.getContentType();
        if (contentType == null || !SUPPORTED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported file type: " + contentType + ". Supported types: " + SUPPORTED_CONTENT_TYPES);
        }
    }

    /**
     * Converts MultipartFile to ImageData.
     */
    private ImageData convertToImageData(MultipartFile file) throws IOException {
        byte[] imageBytes = file.getBytes();
        return ImageData.fromBytes(imageBytes);
    }

    /**
     * Parses detection types from comma-separated string.
     */
    private List<DetectionType> parseDetectionTypes(String detectionTypes) {
        if (detectionTypes == null || detectionTypes.trim().isEmpty()) {
            return List.of(DetectionType.FACE, DetectionType.OBJECT);
        }

        return Arrays.stream(detectionTypes.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(String::toUpperCase)
            .map(DetectionType::valueOf)
            .toList();
    }

    /**
     * Parses detection types from list of strings.
     */
    private List<DetectionType> parseDetectionTypesFromList(List<String> detectionTypes) {
        if (detectionTypes == null || detectionTypes.isEmpty()) {
            return List.of(DetectionType.FACE, DetectionType.OBJECT);
        }

        return detectionTypes.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(String::toUpperCase)
            .map(DetectionType::valueOf)
            .toList();
    }

    /**
     * Generates a unique correlation ID for request tracking.
     */
    private String generateCorrelationId() {
        return "req_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId();
    }
}
