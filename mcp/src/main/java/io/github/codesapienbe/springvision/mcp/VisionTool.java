package io.github.codesapienbe.springvision.mcp;

import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.VisionTemplate;
import io.github.codesapienbe.springvision.core.VisionResult;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
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

    /**
     * Helper method to download an image from a URL and return the bytes.
     */
    private byte[] downloadImageFromUrl(String imageUrl) throws IOException {
        try {
            URI uri = URI.create(imageUrl);
            URL url = uri.toURL();
            try (InputStream inputStream = url.openStream()) {
                return inputStream.readAllBytes();
            }
        } catch (Exception e) {
            throw new IOException("Failed to download image from URL: " + e.getMessage(), e);
        }
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

    @Tool(description = "Detect objects from an image URL")
    public Map<String, Object> detectUrl(String imageUrl, String detectionType) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return Map.of("error", "Missing or empty 'imageUrl' argument");
        }
        try {
            byte[] imageBytes = downloadImageFromUrl(imageUrl);
            return detect(imageBytes, detectionType);
        } catch (IOException e) {
            return Map.of("error", "Failed to download image from URL: " + e.getMessage());
        }
    }

    @Tool(description = "Run OCR on an image (byte[])")
    public Map<String, Object> ocr(byte[] imageBytes) {
        try {
            ImageData imgData = ImageData.fromBytes(imageBytes);
            var textDetections = visionTemplate.detect(imgData, io.github.codesapienbe.springvision.core.DetectionType.TEXT);

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

    @Tool(description = "Run OCR on an image from a URL")
    public Map<String, Object> ocrUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return Map.of("error", "Missing or empty 'imageUrl' argument");
        }
        try {
            byte[] imageBytes = downloadImageFromUrl(imageUrl);
            return ocr(imageBytes);
        } catch (IOException e) {
            return Map.of("error", "Failed to download image from URL: " + e.getMessage());
        }
    }

    @Tool(description = "Recognize faces in an image (byte[])")
    public Map<String, Object> faces(byte[] imageBytes) {
        try {
            ImageData imgData = ImageData.fromBytes(imageBytes);
            var faceDetections = visionTemplate.detect(imgData, io.github.codesapienbe.springvision.core.DetectionType.FACE);
            Map<String, Object> response = new HashMap<>();
            response.put("faces", faceDetections);
            response.put("count", faceDetections.detections().size());
            return response;
        } catch (Exception e) {
            return Map.of("error", "Face recognition failed: " + e.getMessage());
        }
    }

    @Tool(description = "Recognize faces in an image from a URL")
    public Map<String, Object> facesUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return Map.of("error", "Missing or empty 'imageUrl' argument");
        }
        try {
            byte[] imageBytes = downloadImageFromUrl(imageUrl);
            return faces(imageBytes);
        } catch (IOException e) {
            return Map.of("error", "Failed to download image from URL: " + e.getMessage());
        }
    }

    @Tool(description = "Extract face embeddings from an image (byte[]). Returns a list of embedding vectors for each detected face.")
    public Map<String, Object> extractEmbeddings(byte[] imageBytes) {
        try {
            ImageData imgData = ImageData.fromBytes(imageBytes);
            var embeddings = visionTemplate.extractEmbeddings(imgData);

            Map<String, Object> response = new HashMap<>();
            response.put("embeddings", embeddings);
            response.put("count", embeddings != null ? embeddings.size() : 0);
            if (embeddings != null && !embeddings.isEmpty()) {
                response.put("embeddingDimension", embeddings.get(0).length);
            }
            return response;
        } catch (Exception e) {
            return Map.of("error", "Embedding extraction failed: " + e.getMessage());
        }
    }

    @Tool(description = "Extract face embeddings from a base64 encoded image. Returns a list of embedding vectors for each detected face.")
    public Map<String, Object> extractEmbeddingsBase64(String base64Image) {
        if (base64Image == null) {
            return Map.of("error", "Missing 'base64Image' argument");
        }
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            return extractEmbeddings(imageBytes);
        } catch (IllegalArgumentException e) {
            return Map.of("error", "Invalid base64 image data");
        }
    }

    @Tool(description = "Extract face embeddings from an image URL. Returns a list of embedding vectors for each detected face.")
    public Map<String, Object> extractEmbeddingsUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return Map.of("error", "Missing or empty 'imageUrl' argument");
        }
        try {
            byte[] imageBytes = downloadImageFromUrl(imageUrl);
            return extractEmbeddings(imageBytes);
        } catch (IOException e) {
            return Map.of("error", "Failed to download image from URL: " + e.getMessage());
        }
    }
}
