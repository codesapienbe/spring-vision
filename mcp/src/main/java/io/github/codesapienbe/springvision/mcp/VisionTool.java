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
import java.util.List;
import java.util.ArrayList;

/**
 * VisionTool: provides tool methods that can be exposed to MCP clients via Spring AI.
 */
@Component
public class VisionTool {

    private final VisionTemplate visionTemplate;

    // Default similarity threshold for face comparison (cosine similarity)
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.6;

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

    /**
     * Calculate cosine similarity between two embedding vectors.
     * Returns a value between -1 and 1, where 1 means identical.
     */
    private double cosineSimilarity(float[] embedding1, float[] embedding2) {
        if (embedding1.length != embedding2.length) {
            throw new IllegalArgumentException("Embeddings must have the same dimension");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < embedding1.length; i++) {
            dotProduct += embedding1[i] * embedding2[i];
            norm1 += embedding1[i] * embedding1[i];
            norm2 += embedding2[i] * embedding2[i];
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
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

    @Tool(description = "Compare faces from multiple image URLs to determine if they show the same person. Returns similarity scores and a match verdict.")
    public Map<String, Object> compareFacesFromUrls(List<String> imageUrls, Double threshold) {
        if (imageUrls == null || imageUrls.size() < 2) {
            return Map.of("error", "At least 2 image URLs are required for comparison");
        }

        double similarityThreshold = threshold != null ? threshold : DEFAULT_SIMILARITY_THRESHOLD;

        try {
            // Extract embeddings from all images
            List<List<float[]>> allEmbeddings = new ArrayList<>();
            List<String> imageLabels = new ArrayList<>();

            for (int i = 0; i < imageUrls.size(); i++) {
                String imageUrl = imageUrls.get(i);
                if (imageUrl == null || imageUrl.trim().isEmpty()) {
                    return Map.of("error", "Image URL at index " + i + " is empty or null");
                }

                byte[] imageBytes = downloadImageFromUrl(imageUrl);
                ImageData imgData = ImageData.fromBytes(imageBytes);
                List<float[]> embeddings = visionTemplate.extractEmbeddings(imgData);

                if (embeddings == null || embeddings.isEmpty()) {
                    return Map.of("error", "No face detected in image " + (i + 1) + ": " + imageUrl);
                }

                allEmbeddings.add(embeddings);
                imageLabels.add("Image " + (i + 1));
            }

            return performFaceComparison(allEmbeddings, imageLabels, similarityThreshold);

        } catch (Exception e) {
            return Map.of("error", "Face comparison failed: " + e.getMessage());
        }
    }

    @Tool(description = "Compare faces from multiple base64-encoded images to determine if they show the same person. Returns similarity scores and a match verdict.")
    public Map<String, Object> compareFacesFromBase64(List<String> base64Images, Double threshold) {
        if (base64Images == null || base64Images.size() < 2) {
            return Map.of("error", "At least 2 base64 images are required for comparison");
        }

        double similarityThreshold = threshold != null ? threshold : DEFAULT_SIMILARITY_THRESHOLD;

        try {
            // Extract embeddings from all images
            List<List<float[]>> allEmbeddings = new ArrayList<>();
            List<String> imageLabels = new ArrayList<>();

            for (int i = 0; i < base64Images.size(); i++) {
                String base64Image = base64Images.get(i);
                if (base64Image == null || base64Image.trim().isEmpty()) {
                    return Map.of("error", "Base64 image at index " + i + " is empty or null");
                }

                byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                ImageData imgData = ImageData.fromBytes(imageBytes);
                List<float[]> embeddings = visionTemplate.extractEmbeddings(imgData);

                if (embeddings == null || embeddings.isEmpty()) {
                    return Map.of("error", "No face detected in image " + (i + 1));
                }

                allEmbeddings.add(embeddings);
                imageLabels.add("Image " + (i + 1));
            }

            return performFaceComparison(allEmbeddings, imageLabels, similarityThreshold);

        } catch (Exception e) {
            return Map.of("error", "Face comparison failed: " + e.getMessage());
        }
    }

    /**
     * Performs pairwise face comparison between all images.
     * Uses the first detected face from each image for comparison.
     */
    private Map<String, Object> performFaceComparison(List<List<float[]>> allEmbeddings,
                                                      List<String> imageLabels,
                                                      double threshold) {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> comparisons = new ArrayList<>();

        // Use the first face from each image
        List<float[]> primaryEmbeddings = new ArrayList<>();
        for (int i = 0; i < allEmbeddings.size(); i++) {
            primaryEmbeddings.add(allEmbeddings.get(i).get(0));

            if (allEmbeddings.get(i).size() > 1) {
                response.put("warning_image_" + (i + 1),
                    "Multiple faces detected in " + imageLabels.get(i) + ". Using the first detected face.");
            }
        }

        // Perform pairwise comparisons
        double minSimilarity = 1.0;
        double maxSimilarity = -1.0;
        double totalSimilarity = 0.0;
        int comparisonCount = 0;

        for (int i = 0; i < primaryEmbeddings.size(); i++) {
            for (int j = i + 1; j < primaryEmbeddings.size(); j++) {
                double similarity = cosineSimilarity(primaryEmbeddings.get(i), primaryEmbeddings.get(j));

                Map<String, Object> comparison = new HashMap<>();
                comparison.put("pair", imageLabels.get(i) + " vs " + imageLabels.get(j));
                comparison.put("similarity", Math.round(similarity * 10000.0) / 10000.0);
                comparison.put("match", similarity >= threshold);
                comparison.put("confidence", getConfidenceLevel(similarity));

                comparisons.add(comparison);

                minSimilarity = Math.min(minSimilarity, similarity);
                maxSimilarity = Math.max(maxSimilarity, similarity);
                totalSimilarity += similarity;
                comparisonCount++;
            }
        }

        double averageSimilarity = totalSimilarity / comparisonCount;
        boolean allMatch = minSimilarity >= threshold;

        response.put("comparisons", comparisons);
        response.put("summary", Map.of(
            "allMatch", allMatch,
            "minSimilarity", Math.round(minSimilarity * 10000.0) / 10000.0,
            "maxSimilarity", Math.round(maxSimilarity * 10000.0) / 10000.0,
            "avgSimilarity", Math.round(averageSimilarity * 10000.0) / 10000.0,
            "threshold", threshold,
            "imagesCompared", imageLabels.size()
        ));

        response.put("verdict", allMatch ?
            "All images appear to show the same person" :
            "Images likely show different people or comparison is uncertain");

        return response;
    }

    /**
     * Get a human-readable confidence level based on similarity score.
     */
    private String getConfidenceLevel(double similarity) {
        if (similarity >= 0.8) return "Very High";
        if (similarity >= 0.7) return "High";
        if (similarity >= 0.6) return "Medium";
        if (similarity >= 0.5) return "Low";
        return "Very Low";
    }
}
