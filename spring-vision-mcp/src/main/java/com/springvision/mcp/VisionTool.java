package com.springvision.mcp;

import com.springvision.core.ImageData;
import com.springvision.core.VisionTemplate;
import com.springvision.core.VisionResult;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * VisionTool: provides tool methods that can be exposed to MCP clients via Spring AI.
 */
@Component
public class VisionTool {

    private final VisionTemplate visionTemplate;

    public VisionTool(VisionTemplate visionTemplate) {
        this.visionTemplate = visionTemplate;
    }

    @Tool(description = "Detect objects in an image. Accepts raw bytes and optional detectionType (FACE|OBJECT)")
    public Map<String, Object> detect(byte[] imageBytes, String detectionType) {
        Map<String, Object> response = new HashMap<>();
        try {
            ImageData imgData = ImageData.fromBytes(imageBytes);
            VisionResult detections;
            if (detectionType == null || "FACE".equalsIgnoreCase(detectionType)) {
                detections = visionTemplate.detectFaces(imgData);
            } else if ("OBJECT".equalsIgnoreCase(detectionType)) {
                detections = visionTemplate.detectObjects(imgData);
            } else {
                detections = visionTemplate.detectFaces(imgData);
            }
            response.put("detections", detections);
            response.put("count", detections.detections().size());
            return response;
        } catch (Exception e) {
            return Map.of("error", "Detection failed: " + e.getMessage());
        }
    }

    @Tool(description = "Detect objects from a base64 encoded image payload")
    public Map<String, Object> detectBase64(String base64Image, String detectionType) {
        if (base64Image == null) {
            return Map.of("error", "Missing 'image' argument");
        }
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            return detect(imageBytes, detectionType);
        } catch (IllegalArgumentException e) {
            return Map.of("error", "Invalid base64 image data");
        }
    }

    @Tool(description = "Run OCR on an image (byte[])")
    public Map<String, Object> ocr(byte[] imageBytes) {
        try {
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
            return response;
        } catch (Exception e) {
            return Map.of("error", "OCR failed: " + e.getMessage());
        }
    }

    @Tool(description = "Recognize faces in an image (byte[])")
    public Map<String, Object> faces(byte[] imageBytes) {
        try {
            ImageData imgData = ImageData.fromBytes(imageBytes);
            var faceDetections = visionTemplate.detect(imgData, com.springvision.core.DetectionType.FACE);
            Map<String, Object> response = new HashMap<>();
            response.put("faces", faceDetections);
            response.put("count", faceDetections.detections().size());
            return response;
        } catch (Exception e) {
            return Map.of("error", "Face recognition failed: " + e.getMessage());
        }
    }
}
