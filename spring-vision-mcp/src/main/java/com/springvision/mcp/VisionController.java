package com.springvision.mcp;

import com.springvision.core.ImageData;
import com.springvision.core.VisionTemplate;
import com.springvision.core.VisionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for Spring Vision Computer Vision Tools.
 * Provides HTTP endpoints for computer vision operations that can be used by MCP clients
 * or other applications. This serves as a foundation for MCP support.
 */
@RestController
@RequestMapping("/api/vision")
public class VisionController {

    @Autowired
    private VisionTemplate visionTemplate;

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Spring Vision MCP Server");
        response.put("version", "1.0.0");
        return ResponseEntity.ok(response);
    }

    /**
     * Detect objects in an image.
     */
    @PostMapping("/detect")
    public ResponseEntity<?> detectObjects(
            @RequestParam("image") MultipartFile imageFile,
            @RequestParam(value = "detectionType", required = false, defaultValue = "FACE") String detectionType) {

        try {
            byte[] imageBytes = imageFile.getBytes();
            ImageData imgData = ImageData.fromBytes(imageBytes);

            VisionResult detections;
            if ("FACE".equalsIgnoreCase(detectionType)) {
                detections = visionTemplate.detectFaces(imgData);
            } else if ("OBJECT".equalsIgnoreCase(detectionType)) {
                detections = visionTemplate.detectObjects(imgData);
            } else {
                // Default to face detection
                detections = visionTemplate.detectFaces(imgData);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("detections", detections);
            response.put("count", detections.detections().size());
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to process image: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Detection failed: " + e.getMessage()));
        }
    }

    /**
     * Extract text from an image (OCR).
     */
    @PostMapping("/ocr")
    public ResponseEntity<?> extractText(@RequestParam("image") MultipartFile imageFile) {

        try {
            byte[] imageBytes = imageFile.getBytes();
            ImageData imgData = ImageData.fromBytes(imageBytes);

            var textDetections = visionTemplate.detect(imgData, com.springvision.core.DetectionType.TEXT);

            StringBuilder text = new StringBuilder();
            for (var detection : textDetections.detections()) {
                String detectedText = (String) detection.getAttribute("text");
                if (detectedText != null) {
                    text.append(detectedText).append(" ");
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("text", text.toString().trim());
            response.put("detections", textDetections);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to process image: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "OCR failed: " + e.getMessage()));
        }
    }

    /**
     * Recognize faces in an image.
     */
    @PostMapping("/faces")
    public ResponseEntity<?> recognizeFaces(@RequestParam("image") MultipartFile imageFile) {

        try {
            byte[] imageBytes = imageFile.getBytes();
            ImageData imgData = ImageData.fromBytes(imageBytes);

            var faceDetections = visionTemplate.detect(imgData, com.springvision.core.DetectionType.FACE);

            Map<String, Object> response = new HashMap<>();
            response.put("faces", faceDetections);
            response.put("count", faceDetections.detections().size());

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to process image: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Face recognition failed: " + e.getMessage()));
        }
    }

    /**
     * Process base64 encoded image for detection.
     */
    @PostMapping("/detect/base64")
    public ResponseEntity<?> detectObjectsBase64(
            @RequestBody Map<String, Object> request,
            @RequestParam(value = "detectionType", required = false, defaultValue = "FACE") String detectionType) {

        try {
            String base64Image = (String) request.get("image");
            if (base64Image == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Missing 'image' field in request body"));
            }

            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            ImageData imgData = ImageData.fromBytes(imageBytes);

            VisionResult detections;
            if ("FACE".equalsIgnoreCase(detectionType)) {
                detections = visionTemplate.detectFaces(imgData);
            } else if ("OBJECT".equalsIgnoreCase(detectionType)) {
                detections = visionTemplate.detectObjects(imgData);
            } else {
                // Default to face detection
                detections = visionTemplate.detectFaces(imgData);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("detections", detections);
            response.put("count", detections.detections().size());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid base64 image data"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Detection failed: " + e.getMessage()));
        }
    }
}
