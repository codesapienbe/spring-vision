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
 * Provides tool methods that can be exposed to MCP clients via Spring AI for performing computer vision tasks.
 * This class wraps the {@link VisionTemplate} to offer functionalities like object detection, face recognition,
 * and OCR through simple tool calls.
 *
 * <p>The methods in this class are designed to be used as tools in a Spring AI application,
 * allowing AI models to interact with the vision capabilities of the application. The methods
 * handle different input formats like byte arrays, base64 strings, and image URLs.</p>
 *
 * @see VisionTemplate
 * @see Tool
 */
@Component
public class VisionTool {

    private final VisionTemplate visionTemplate;

    // Default similarity threshold for face comparison (cosine similarity)
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.6;

    /**
     * Constructs a new VisionTool with the given VisionTemplate.
     *
     * @param visionTemplate The VisionTemplate to use for performing vision operations.
     */
    public VisionTool(VisionTemplate visionTemplate) {
        this.visionTemplate = visionTemplate;
    }

    /**
     * Downloads an image from a URL and returns its content as a byte array.
     *
     * @param imageUrl The URL of the image to download.
     * @return A byte array containing the image data.
     * @throws IOException if the image cannot be downloaded.
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
     * Calculates the cosine similarity between two embedding vectors.
     * Cosine similarity measures the cosine of the angle between two non-zero vectors,
     * with a value of 1 meaning the vectors are identical, 0 meaning they are orthogonal,
     * and -1 meaning they are diametrically opposed.
     *
     * @param embedding1 The first embedding vector.
     * @param embedding2 The second embedding vector.
     * @return The cosine similarity score between -1 and 1.
     * @throws IllegalArgumentException if the embeddings have different dimensions.
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

    /**
     * Detects objects or faces in an image provided as a byte array.
     *
     * @param imageBytes    The image data as a byte array.
     * @param detectionType The type of detection to perform, either "FACE" or "OBJECT". Defaults to "FACE".
     * @return A map containing the detection results, including a list of detections and the total count.
     */
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

    /**
     * Detects objects or faces in a base64 encoded image.
     *
     * @param base64Image   The base64 encoded string of the image.
     * @param detectionType The type of detection to perform, either "FACE" or "OBJECT".
     * @return A map containing the detection results or an error message.
     */
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

    /**
     * Detects objects or faces in an image from a given URL.
     *
     * @param imageUrl      The URL of the.
     * @param detectionType The type of detection to perform, either "FACE" or "OBJECT".
     * @return A map containing the detection results or an error message.
     */
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

    /**
     * Performs Optical Character Recognition (OCR) on an image provided as a byte array.
     *
     * @param imageBytes The image data as a byte array.
     * @return A map containing the recognized text and the raw detection results.
     */
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

    /**
     * Performs Optical Character Recognition (OCR) on an image from a given URL.
     *
     * @param imageUrl The URL of the image.
     * @return A map containing the recognized text or an error message.
     */
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

    /**
     * Recognizes faces in an image provided as a byte array.
     *
     * @param imageBytes The image data as a byte array.
     * @return A map containing the face detection results and the count of faces found.
     */
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

    /**
     * Recognizes faces in an image from a given URL.
     *
     * @param imageUrl The URL of the image.
     * @return A map containing the face detection results or an error message.
     */
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

    /**
     * Extracts face embeddings from an image provided as a byte array.
     *
     * @param imageBytes The image data as a byte array.
     * @return A map containing a list of embedding vectors for each detected face.
     */
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

    /**
     * Extracts face embeddings from a base64 encoded image.
     *
     * @param base64Image The base64 encoded string of the image.
     * @return A map containing the list of embedding vectors or an error message.
     */
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

    /**
     * Extracts face embeddings from an image at a given URL.
     *
     * @param imageUrl The URL of the image.
     * @return A map containing the list of embedding vectors or an error message.
     */
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

    /**
     * Compares faces from multiple image URLs to determine if they belong to the same person.
     *
     * @param imageUrls A list of image URLs to compare.
     * @param threshold The similarity threshold to use for the comparison. Defaults to 0.6.
     * @return A map containing the similarity scores, a match verdict, and other comparison details.
     */
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

    /**
     * Compares faces from multiple base64-encoded images to determine if they belong to the same person.
     *
     * @param base64Images A list of base64 encoded image strings.
     * @param threshold    The similarity threshold to use for the comparison. Defaults to 0.6.
     * @return A map containing the similarity scores, a match verdict, and other comparison details.
     */
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
     * Performs pairwise face comparison between all images using their embeddings.
     * It uses the first detected face from each image for the comparison.
     *
     * @param allEmbeddings A list where each element is a list of face embeddings for an image.
     * @param imageLabels   A list of labels for the images being compared.
     * @param threshold     The similarity threshold to determine if faces match.
     * @return A map containing detailed comparison results, including pairwise scores and an overall verdict.
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
     * Returns a human-readable confidence level based on a similarity score.
     *
     * @param similarity The similarity score, typically between 0 and 1.
     * @return A string representing the confidence level (e.g., "Very High", "Low").
     */
    private String getConfidenceLevel(double similarity) {
        if (similarity >= 0.8) return "Very High";
        if (similarity >= 0.7) return "High";
        if (similarity >= 0.6) return "Medium";
        if (similarity >= 0.5) return "Low";
        return "Very Low";
    }

    // ========== NEW CAPABILITIES ==========

    /**
     * Detects human body poses and keypoints in an image provided as a byte array.
     * Returns detected poses with skeletal keypoints (shoulders, elbows, knees, etc.).
     *
     * @param imageBytes The image data as a byte array.
     * @return A map containing the pose detection results, keypoints, and the count of poses found.
     */
    @Tool(description = "Detect human body poses and skeletal keypoints in an image. Returns pose estimations with joint positions.")
    public Map<String, Object> detectPoses(byte[] imageBytes) {
        try {
            ImageData imgData = ImageData.fromBytes(imageBytes);
            var poseDetections = visionTemplate.detect(imgData, io.github.codesapienbe.springvision.core.DetectionType.POSE);

            Map<String, Object> response = new HashMap<>();
            response.put("poses", poseDetections.detections());
            response.put("count", poseDetections.detections().size());
            response.put("averageConfidence", poseDetections.averageConfidence());

            return response;
        } catch (Exception e) {
            return Map.of("error", "Pose detection failed: " + e.getMessage());
        }
    }

    /**
     * Detects human body poses and keypoints from an image URL.
     *
     * @param imageUrl The URL of the image.
     * @return A map containing the pose detection results or an error message.
     */
    @Tool(description = "Detect human body poses and skeletal keypoints from an image URL. Returns pose estimations with joint positions.")
    public Map<String, Object> detectPosesUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return Map.of("error", "Missing or empty 'imageUrl' argument");
        }
        try {
            byte[] imageBytes = downloadImageFromUrl(imageUrl);
            return detectPoses(imageBytes);
        } catch (IOException e) {
            return Map.of("error", "Failed to download image from URL: " + e.getMessage());
        }
    }

    /**
     * Detects hands and hand gestures in an image provided as a byte array.
     * Returns detected hands with landmark keypoints for fingers and palm.
     *
     * @param imageBytes The image data as a byte array.
     * @return A map containing the hand detection results, landmarks, and the count of hands found.
     */
    @Tool(description = "Detect hands and hand gestures in an image. Returns hand landmarks including finger positions and palm keypoints.")
    public Map<String, Object> detectHands(byte[] imageBytes) {
        try {
            ImageData imgData = ImageData.fromBytes(imageBytes);
            var handDetections = visionTemplate.detect(imgData, io.github.codesapienbe.springvision.core.DetectionType.HAND);

            Map<String, Object> response = new HashMap<>();
            response.put("hands", handDetections.detections());
            response.put("count", handDetections.detections().size());
            response.put("averageConfidence", handDetections.averageConfidence());

            return response;
        } catch (Exception e) {
            return Map.of("error", "Hand detection failed: " + e.getMessage());
        }
    }

    /**
     * Detects hands and hand gestures from an image URL.
     *
     * @param imageUrl The URL of the image.
     * @return A map containing the hand detection results or an error message.
     */
    @Tool(description = "Detect hands and hand gestures from an image URL. Returns hand landmarks including finger positions and palm keypoints.")
    public Map<String, Object> detectHandsUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return Map.of("error", "Missing or empty 'imageUrl' argument");
        }
        try {
            byte[] imageBytes = downloadImageFromUrl(imageUrl);
            return detectHands(imageBytes);
        } catch (IOException e) {
            return Map.of("error", "Failed to download image from URL: " + e.getMessage());
        }
    }

    /**
     * Detects and decodes barcodes and QR codes in an image provided as a byte array.
     * Returns the decoded data along with barcode type and location.
     *
     * @param imageBytes The image data as a byte array.
     * @return A map containing the barcode detection results, decoded values, and the count of barcodes found.
     */
    @Tool(description = "Detect and decode barcodes and QR codes in an image. Returns decoded values, barcode types (QR, EAN, UPC, etc.), and locations.")
    public Map<String, Object> detectBarcodes(byte[] imageBytes) {
        try {
            ImageData imgData = ImageData.fromBytes(imageBytes);
            var barcodeDetections = visionTemplate.detect(imgData, io.github.codesapienbe.springvision.core.DetectionType.BARCODE);

            Map<String, Object> response = new HashMap<>();
            List<Map<String, Object>> barcodes = new ArrayList<>();

            for (var detection : barcodeDetections.detections()) {
                Map<String, Object> barcode = new HashMap<>();
                barcode.put("type", detection.label());
                barcode.put("value", detection.getAttribute("value"));
                barcode.put("confidence", detection.confidence());
                barcode.put("boundingBox", detection.boundingBox());
                barcodes.add(barcode);
            }

            response.put("barcodes", barcodes);
            response.put("count", barcodeDetections.detections().size());
            response.put("rawDetections", barcodeDetections);

            return response;
        } catch (Exception e) {
            return Map.of("error", "Barcode detection failed: " + e.getMessage());
        }
    }

    /**
     * Detects and decodes barcodes and QR codes from an image URL.
     *
     * @param imageUrl The URL of the image.
     * @return A map containing the barcode detection results or an error message.
     */
    @Tool(description = "Detect and decode barcodes and QR codes from an image URL. Returns decoded values, barcode types (QR, EAN, UPC, etc.), and locations.")
    public Map<String, Object> detectBarcodesUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return Map.of("error", "Missing or empty 'imageUrl' argument");
        }
        try {
            byte[] imageBytes = downloadImageFromUrl(imageUrl);
            return detectBarcodes(imageBytes);
        } catch (IOException e) {
            return Map.of("error", "Failed to download image from URL: " + e.getMessage());
        }
    }

    /**
     * Detects geographic landmarks and famous places in an image provided as a byte array.
     * Returns identified landmarks with confidence scores and location information.
     *
     * @param imageBytes The image data as a byte array.
     * @return A map containing the landmark detection results and the count of landmarks found.
     */
    @Tool(description = "Detect geographic landmarks and famous places in an image. Returns identified landmarks with names, confidence scores, and locations.")
    public Map<String, Object> detectLandmarks(byte[] imageBytes) {
        try {
            ImageData imgData = ImageData.fromBytes(imageBytes);
            var landmarkDetections = visionTemplate.detect(imgData, io.github.codesapienbe.springvision.core.DetectionType.LANDMARK);

            Map<String, Object> response = new HashMap<>();
            List<Map<String, Object>> landmarks = new ArrayList<>();

            for (var detection : landmarkDetections.detections()) {
                Map<String, Object> landmark = new HashMap<>();
                landmark.put("name", detection.label());
                landmark.put("confidence", detection.confidence());
                landmark.put("boundingBox", detection.boundingBox());
                landmark.put("location", detection.getAttribute("location"));
                landmark.put("description", detection.getAttribute("description"));
                landmarks.add(landmark);
            }

            response.put("landmarks", landmarks);
            response.put("count", landmarkDetections.detections().size());
            response.put("rawDetections", landmarkDetections);

            return response;
        } catch (Exception e) {
            return Map.of("error", "Landmark detection failed: " + e.getMessage());
        }
    }

    /**
     * Detects geographic landmarks and famous places from an image URL.
     *
     * @param imageUrl The URL of the image.
     * @return A map containing the landmark detection results or an error message.
     */
    @Tool(description = "Detect geographic landmarks and famous places from an image URL. Returns identified landmarks with names, confidence scores, and locations.")
    public Map<String, Object> detectLandmarksUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return Map.of("error", "Missing or empty 'imageUrl' argument");
        }
        try {
            byte[] imageBytes = downloadImageFromUrl(imageUrl);
            return detectLandmarks(imageBytes);
        } catch (IOException e) {
            return Map.of("error", "Failed to download image from URL: " + e.getMessage());
        }
    }

    /**
     * Annotates an image with detected objects by drawing bounding boxes and labels.
     * Supports multiple detection types and custom annotation styles.
     *
     * @param imageBytes    The image data as a byte array.
     * @param detectionType The type of objects to detect and annotate (FACE, OBJECT, TEXT, etc.). Defaults to "OBJECT".
     * @param drawBoxes     Whether to draw bounding boxes around detections. Defaults to true.
     * @param drawLabels    Whether to draw labels on detections. Defaults to true.
     * @return A map containing the annotated image (as base64) and detection information.
     */
    @Tool(description = "Annotate an image by detecting objects and drawing bounding boxes with labels. Returns the annotated image as base64 along with detection details.")
    public Map<String, Object> annotateImage(byte[] imageBytes, String detectionType, Boolean drawBoxes, Boolean drawLabels) {
        try {
            ImageData imgData = ImageData.fromBytes(imageBytes);

            // Determine detection type
            io.github.codesapienbe.springvision.core.DetectionType type;
            io.github.codesapienbe.springvision.core.DetectionCategory category;

            if (detectionType == null || detectionType.isEmpty()) {
                type = io.github.codesapienbe.springvision.core.DetectionType.OBJECT;
                category = io.github.codesapienbe.springvision.core.DetectionCategory.OBJECT;
            } else {
                type = switch (detectionType.toUpperCase()) {
                    case "FACE" -> io.github.codesapienbe.springvision.core.DetectionType.FACE;
                    case "OBJECT" -> io.github.codesapienbe.springvision.core.DetectionType.OBJECT;
                    case "TEXT" -> io.github.codesapienbe.springvision.core.DetectionType.TEXT;
                    case "POSE" -> io.github.codesapienbe.springvision.core.DetectionType.POSE;
                    case "HAND" -> io.github.codesapienbe.springvision.core.DetectionType.HAND;
                    case "BARCODE" -> io.github.codesapienbe.springvision.core.DetectionType.BARCODE;
                    default -> io.github.codesapienbe.springvision.core.DetectionType.OBJECT;
                };

                category = switch (detectionType.toUpperCase()) {
                    case "FACE" -> io.github.codesapienbe.springvision.core.DetectionCategory.FACE;
                    case "HAND" -> io.github.codesapienbe.springvision.core.DetectionCategory.HAND;
                    case "POSE" -> io.github.codesapienbe.springvision.core.DetectionCategory.BODY;
                    default -> io.github.codesapienbe.springvision.core.DetectionCategory.OBJECT;
                };
            }

            // Perform detection
            var detections = visionTemplate.detect(imgData, type);

            // Try to annotate the image if backend supports it
            ImageData annotatedImage = imgData;
            boolean annotationSupported = false;

            if (visionTemplate.backend() instanceof io.github.codesapienbe.springvision.core.capabilities.AnnotationCapability annotationCap) {
                try {
                    // Determine annotation action based on parameters
                    boolean shouldDrawBoxes = drawBoxes == null || drawBoxes;
                    boolean shouldDrawLabels = drawLabels == null || drawLabels;

                    if (shouldDrawBoxes) {
                        // Create an annotation request to mark (draw boxes)
                        var annotationRequest = new io.github.codesapienbe.springvision.core.AnnotationRequest.Builder()
                            .action(io.github.codesapienbe.springvision.core.AnnotationRequest.Action.MARK)
                            .categories(java.util.Set.of(category))
                            .build();

                        annotatedImage = annotationCap.annotate(imgData, annotationRequest);
                        annotationSupported = true;
                    }
                } catch (Exception e) {
                    // Annotation failed, continue with the original image
                    annotationSupported = false;
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("annotatedImage", Base64.getEncoder().encodeToString(annotatedImage.data()));
            response.put("detections", detections.detections());
            response.put("detectionCount", detections.detectionCount());
            response.put("detectionType", type.getDisplayName());
            response.put("averageConfidence", detections.averageConfidence());
            response.put("annotationApplied", annotationSupported);

            if (!annotationSupported) {
                response.put("note", "Backend does not support annotation or annotation failed. Returning original image with detection metadata.");
            }

            return response;
        } catch (Exception e) {
            return Map.of("error", "Image annotation failed: " + e.getMessage());
        }
    }

    /**
     * Annotates an image from a URL with detected objects by drawing bounding boxes and labels.
     *
     * @param imageUrl      The URL of the image.
     * @param detectionType The type of objects to detect and annotate (FACE, OBJECT, TEXT, etc.).
     * @param drawBoxes     Whether to draw bounding boxes around detections.
     * @param drawLabels    Whether to draw labels on detections.
     * @return A map containing the annotated image (as base64) and detection information.
     */
    @Tool(description = "Annotate an image from URL by detecting objects and drawing bounding boxes with labels. Returns the annotated image as base64 along with detection details.")
    public Map<String, Object> annotateImageUrl(String imageUrl, String detectionType, Boolean drawBoxes, Boolean drawLabels) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return Map.of("error", "Missing or empty 'imageUrl' argument");
        }
        try {
            byte[] imageBytes = downloadImageFromUrl(imageUrl);
            return annotateImage(imageBytes, detectionType, drawBoxes, drawLabels);
        } catch (IOException e) {
            return Map.of("error", "Failed to download image from URL: " + e.getMessage());
        }
    }
}
