package com.springvision.core.backend;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.deepface.core.DeepFace;
import com.deepface.core.EmbeddingResult;
import com.deepface.core.FaceRegion;
import com.springvision.core.BackendHealthInfo;
import com.springvision.core.Detection;
import com.springvision.core.DetectionType;
import com.springvision.core.ImageData;
import com.springvision.core.VisionBackend;
import com.springvision.core.VisionResult;
import com.springvision.core.exception.BaseVisionException;
import com.springvision.core.DetectionQuery;

/**
 * VisionBackend implementation backed by the FaceBytes (Similar to DeepFace for python by serengil) module.
 *
 * <p>This backend provides face detection by delegating to FaceBytes' OpenCV Haar cascade
 * face detector and mapping results into Spring Vision core domain objects.</p>
 */
public final class FaceBytesBackend implements VisionBackend, com.springvision.core.capabilities.FaceDetectionCapability, com.springvision.core.capabilities.EmbeddingCapability {

    private static final Logger logger = LoggerFactory.getLogger(FaceBytesBackend.class);

    private static final String BACKEND_ID = "facebytes";
    private static final String DISPLAY_NAME = "FaceBytes Backend";
    private static final String VERSION = "1.0.0";

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
            // Lightweight health check: attempt to load FaceBytes classes
            Class.forName("com.deepface.core.DeepFace");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public BackendHealthInfo getHealthInfo() {
        long start = System.currentTimeMillis();
        boolean ok = isHealthy();
        long took = System.currentTimeMillis() - start;
        if (ok) {
            return BackendHealthInfo.healthy(BACKEND_ID, "FaceBytes available", took, Map.of(
                "displayName", DISPLAY_NAME,
                "version", VERSION
            ));
        }
        return BackendHealthInfo.unhealthy(BACKEND_ID, "FaceBytes unavailable",
            "Required classes not found", took);
    }

    @Override
    public List<Detection> detectFaces(ImageData imageData) {
        DetectionQuery query = new DetectionQuery.Builder()
            .type(DetectionType.FACE)
            .build();
        return detectWithEnhancements(imageData, query);
    }
    
    @Override
    public List<Detection> detectObjects(ImageData imageData) {
        DetectionQuery query = new DetectionQuery.Builder()
            .type(DetectionType.OBJECT)
            .build();
        return detectWithEnhancements(imageData, query);
    }

    /**
     * Enhanced detection with CPU optimizations and quality assessment.
     */
    private List<Detection> detectWithEnhancements(ImageData imageData, DetectionQuery query) {
        if (imageData == null || imageData.isEmpty()) {
            throw new IllegalArgumentException("ImageData must not be null or empty");
        }
        
        long start = System.currentTimeMillis();
        String correlationId = generateCorrelationId();
        
        logger.debug("Starting enhanced face detection [{}]", correlationId);
        
        try {
            // Optimized image preprocessing
            BufferedImage img = preprocessImageForRecognition(imageData);
            if (img == null) {
                throw new IllegalArgumentException("Unsupported or corrupt image data");
            }
            
            // CPU-optimized embedding extraction with quality assessment
            List<EmbeddingResult> embeddings = extractEmbeddingsWithQualityCheck(img, correlationId);
            List<Detection> detections = convertToDetectionsWithEnhancements(embeddings, img);
            
            long took = System.currentTimeMillis() - start;
            
            logger.info("Enhanced face detection completed: {} faces in {}ms [{}]", 
                       detections.size(), took, correlationId);
            
            return detections;
            
        } catch (Exception e) {
            long took = System.currentTimeMillis() - start;
            
            // Gracefully handle expected "no faces/embeddings" cases
            String msg = e.getMessage() == null ? "" : e.getMessage();
            if (e instanceof IllegalArgumentException && 
                (msg.contains("No faces detected") || msg.contains("No valid face embeddings produced"))) {
                logger.info("No faces detected in image [{}] - processing time: {}ms", correlationId, took);
                return List.of();
            }
            
            logger.error("Enhanced face detection failed [{}] - processing time: {}ms", correlationId, took, e);
            return List.of();
        }
    }

    /**
     * Optimized image preprocessing for better recognition accuracy.
     */
    private BufferedImage preprocessImageForRecognition(ImageData imageData) throws Exception {
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageData.data()));
        if (originalImage == null) {
            return null;
        }
        
        // Apply preprocessing optimizations for CPU-based processing
        BufferedImage processedImage = applyRecognitionOptimizations(originalImage);
        
        return processedImage;
    }

    /**
     * Apply CPU-optimized preprocessing for better face recognition.
     */
    private BufferedImage applyRecognitionOptimizations(BufferedImage image) {
        try {
            int width = image.getWidth();
            int height = image.getHeight();
            
            // 1. Resize if too large for efficient processing
            BufferedImage optimizedImage = image;
            if (width > 1920 || height > 1080) {
                double scale = Math.min(1920.0 / width, 1080.0 / height);
                int newWidth = (int) (width * scale);
                int newHeight = (int) (height * scale);
                
                optimizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
                java.awt.Graphics2D g2d = optimizedImage.createGraphics();
                g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, 
                                    java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.drawImage(image, 0, 0, newWidth, newHeight, null);
                g2d.dispose();
                
                logger.debug("Resized image from {}x{} to {}x{} for optimization", 
                           width, height, newWidth, newHeight);
            }
            
            // 2. Enhance contrast if needed (CPU-efficient approach)
            optimizedImage = enhanceContrastForRecognition(optimizedImage);
            
            return optimizedImage;
            
        } catch (Exception e) {
            logger.debug("Image optimization failed, using original: {}", e.getMessage());
            return image;
        }
    }

    /**
     * CPU-efficient contrast enhancement for better face detection.
     */
    private BufferedImage enhanceContrastForRecognition(BufferedImage image) {
        try {
            // Simple histogram-based enhancement
            int width = image.getWidth();
            int height = image.getHeight();
            BufferedImage enhanced = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            
            // Calculate histogram
            int[] histogram = new int[256];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = image.getRGB(x, y);
                    int gray = (int) (0.299 * ((rgb >> 16) & 0xFF) + 
                                     0.587 * ((rgb >> 8) & 0xFF) + 
                                     0.114 * (rgb & 0xFF));
                    histogram[gray]++;
                }
            }
            
            // Calculate cumulative histogram for equalization
            int[] cumHistogram = new int[256];
            cumHistogram[0] = histogram[0];
            for (int i = 1; i < 256; i++) {
                cumHistogram[i] = cumHistogram[i - 1] + histogram[i];
            }
            
            // Apply mild equalization (less aggressive than full histogram equalization)
            int totalPixels = width * height;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = image.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    
                    int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                    int newGray = (int) (255.0 * cumHistogram[gray] / totalPixels);
                    
                    // Blend original and equalized (mild enhancement)
                    double alpha = 0.3; // 30% enhancement, 70% original
                    int finalGray = (int) (alpha * newGray + (1 - alpha) * gray);
                    
                    // Maintain color ratios
                    double ratio = finalGray / Math.max(1.0, gray);
                    int newR = Math.min(255, (int) (r * ratio));
                    int newG = Math.min(255, (int) (g * ratio));
                    int newB = Math.min(255, (int) (b * ratio));
                    
                    enhanced.setRGB(x, y, (newR << 16) | (newG << 8) | newB);
                }
            }
            
            return enhanced;
            
        } catch (Exception e) {
            logger.debug("Contrast enhancement failed: {}", e.getMessage());
            return image;
        }
    }

    /**
     * CPU-optimized embedding extraction with quality validation.
     */
    private List<EmbeddingResult> extractEmbeddingsWithQualityCheck(BufferedImage image, String correlationId) 
            throws Exception {
        
        logger.debug("Extracting embeddings with quality check [{}]", correlationId);
        
        // Configure CPU-optimized settings for DeepFace
        configureCpuOptimizations();
        
        // Extract embeddings using optimized DeepFace processing
        List<EmbeddingResult> embeddings = DeepFace.represent(image);
        
        // Filter embeddings based on quality assessment
        List<EmbeddingResult> qualityFilteredEmbeddings = new ArrayList<>();
        
        for (EmbeddingResult embedding : embeddings) {
            if (validateEmbeddingQuality(embedding, image)) {
                qualityFilteredEmbeddings.add(embedding);
            } else {
                logger.debug("Filtered out low-quality embedding [{}]", correlationId);
            }
        }
        
        logger.debug("Quality filtering: {} -> {} embeddings [{}]", 
                    embeddings.size(), qualityFilteredEmbeddings.size(), correlationId);
        
        return qualityFilteredEmbeddings;
    }

    /**
     * Configure CPU optimizations for DeepFace processing.
     */
    private void configureCpuOptimizations() {
        // Set system properties for CPU-optimized processing
        System.setProperty("deepface.cpu.threads", String.valueOf(
            Math.max(1, Runtime.getRuntime().availableProcessors() / 2)));
        System.setProperty("deepface.batch.size", "1"); // Process one at a time for memory efficiency
        System.setProperty("deepface.cache.models", "true"); // Cache models for faster subsequent calls
    }

    /**
     * Validate embedding quality for recognition accuracy.
     */
    private boolean validateEmbeddingQuality(EmbeddingResult embeddingResult, BufferedImage image) {
        try {
            FaceRegion faceRegion = embeddingResult.faceRegion();
            
            // 1. Face size validation
            double faceArea = faceRegion.width() * faceRegion.height();
            double imageArea = image.getWidth() * image.getHeight();
            double sizeRatio = faceArea / imageArea;
            
            if (sizeRatio < 0.001 || sizeRatio > 0.8) {
                logger.debug("Face size invalid: {}% of image area", sizeRatio * 100);
                return false;
            }
            
            // 2. Face confidence validation
            if (faceRegion.confidence() < 0.5) {
                logger.debug("Face confidence too low: {}", faceRegion.confidence());
                return false;
            }
            
            // 3. Landmarks validation
            float[] landmarks = faceRegion.landmarks();
            if (landmarks != null && landmarks.length >= 10) {
                // Check eye distance for plausible face
                double eyeDx = Math.abs(landmarks[0] - landmarks[2]);
                double eyeDy = Math.abs(landmarks[1] - landmarks[3]);
                double eyeDistance = Math.hypot(eyeDx, eyeDy);
                
                // Eye distance should be reasonable relative to face size
                double expectedEyeDistance = Math.min(faceRegion.width(), faceRegion.height()) * 0.2;
                if (eyeDistance < expectedEyeDistance * 0.3 || eyeDistance > expectedEyeDistance * 3.0) {
                    logger.debug("Eye distance implausible: {} (expected ~{})", eyeDistance, expectedEyeDistance);
                    return false;
                }
            }
            
            // 4. Aspect ratio validation
            double aspectRatio = (double) faceRegion.width() / faceRegion.height();
            if (aspectRatio < 0.5 || aspectRatio > 2.0) {
                logger.debug("Face aspect ratio invalid: {}", aspectRatio);
                return false;
            }
            
            // 5. Embedding vector validation
            float[] embedding = embeddingResult.embedding();
            if (embedding == null || embedding.length == 0) {
                logger.debug("Invalid embedding vector");
                return false;
            }
            
            // Check for zero or constant embeddings (indicates processing failure)
            boolean allZero = true;
            boolean allConstant = true;
            float firstValue = embedding[0];
            for (float value : embedding) {
                if (Math.abs(value) > 1e-6) {
                    allZero = false;
                }
                if (Math.abs(value - firstValue) > 1e-6) {
                    allConstant = false;
                }
            }
            
            if (allZero || allConstant) {
                logger.debug("Invalid embedding: all zero={}, all constant={}", allZero, allConstant);
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            logger.debug("Embedding validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Convert embeddings to detections with enhanced attributes.
     */
    private List<Detection> convertToDetectionsWithEnhancements(List<EmbeddingResult> embeddings, BufferedImage image) {
        List<Detection> detections = new ArrayList<>(embeddings.size());
        
        for (int i = 0; i < embeddings.size(); i++) {
            EmbeddingResult embeddingResult = embeddings.get(i);
            FaceRegion faceRegion = embeddingResult.faceRegion();
            
            // Normalize bounding box coordinates
            double nx = clamp01((double) faceRegion.x() / image.getWidth());
            double ny = clamp01((double) faceRegion.y() / image.getHeight());
            double nw = clamp01((double) faceRegion.width() / image.getWidth());
            double nh = clamp01((double) faceRegion.height() / image.getHeight());
            
            var bbox = new com.springvision.core.BoundingBox(nx, ny, nw, nh);
            double confidence = clamp01((double) faceRegion.confidence());
            
            // Enhanced attributes for recognition
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("category", com.springvision.core.DetectionCategory.FACE.name());
            attributes.put("backend", "facebytes");
            attributes.put("face_index", i);
            attributes.put("embedding_dimension", embeddingResult.embedding().length);
            attributes.put("face_area_ratio", nw * nh);
            
            // Add quality metrics
            attributes.put("resolution_score", computeFaceResolutionScore(faceRegion));
            attributes.put("pose_quality", estimatePoseQuality(faceRegion));
            
            // Add landmark information if available
            float[] landmarks = faceRegion.landmarks();
            if (landmarks != null && landmarks.length >= 10) {
                attributes.put("has_landmarks", true);
                attributes.put("landmark_count", landmarks.length / 2); // x,y pairs
                
                // Calculate eye distance for quality assessment
                double eyeDx = Math.abs(landmarks[0] - landmarks[2]);
                double eyeDy = Math.abs(landmarks[1] - landmarks[3]);
                double eyeDistance = Math.hypot(eyeDx, eyeDy);
                attributes.put("eye_distance", eyeDistance);
            } else {
                attributes.put("has_landmarks", false);
            }
            
            detections.add(new Detection("face", confidence, bbox, attributes));
        }
        
        // Sort by confidence (best first)
        detections.sort((a, b) -> Double.compare(b.confidence(), a.confidence()));
        
        return detections;
    }

    /**
     * Compute face resolution score for recognition quality.
     */
    private double computeFaceResolutionScore(FaceRegion faceRegion) {
        int minDimension = Math.min(faceRegion.width(), faceRegion.height());
        
        // Scoring based on face size for recognition accuracy
        if (minDimension >= 112) return 1.0;      // Optimal for recognition
        if (minDimension >= 96) return 0.9;       // Very good
        if (minDimension >= 80) return 0.8;       // Good
        if (minDimension >= 64) return 0.6;       // Acceptable
        if (minDimension >= 48) return 0.4;       // Poor but usable
        if (minDimension >= 32) return 0.2;       // Very poor
        return 0.1;                               // Unusable for recognition
    }

    /**
     * Estimate pose quality based on face region characteristics.
     */
    private double estimatePoseQuality(FaceRegion faceRegion) {
        try {
            // Use aspect ratio as a proxy for pose quality
            double aspectRatio = (double) faceRegion.width() / faceRegion.height();
            
            // Frontal faces typically have aspect ratios between 0.7 and 1.3
            if (aspectRatio >= 0.8 && aspectRatio <= 1.2) {
                return 1.0; // Likely frontal
            } else if (aspectRatio >= 0.7 && aspectRatio <= 1.4) {
                return 0.8; // Slightly off-angle
            } else if (aspectRatio >= 0.6 && aspectRatio <= 1.6) {
                return 0.6; // Moderate angle
            } else {
                return 0.3; // Likely profile or extreme angle
            }
        } catch (Exception e) {
            return 0.7; // Default moderate quality
        }
    }

    /**
     * Generate correlation ID for request tracking.
     */
    private String generateCorrelationId() {
        return "fb-" + System.currentTimeMillis() % 100000 + "-" + 
               Integer.toHexString((int) (Math.random() * 0xFFFF));
    }

    @Override
    public java.util.List<float[]> extractEmbeddings(com.springvision.core.ImageData imageData,
                                                     com.springvision.core.DetectionCategory subject)
            throws com.springvision.core.exception.BaseVisionException {
        // For now, ignore subject and always extract face embeddings via FaceBytes
        return com.springvision.core.util.EmbeddingSupport.defaultExtractEmbeddings(imageData);
    }

    private static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }
}
