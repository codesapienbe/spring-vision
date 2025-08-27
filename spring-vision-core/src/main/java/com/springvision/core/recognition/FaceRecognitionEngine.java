package com.springvision.core.recognition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.springvision.core.Detection;
import com.springvision.core.ImageData;
import com.springvision.core.VisionBackend;
import com.springvision.core.exception.BaseVisionException;
import com.springvision.core.exception.VisionProcessingException;

/**
 * High-performance face recognition engine for large-scale face lookup.
 * 
 * <p>This engine provides the complete pipeline for face recognition:</p>
 * <ul>
 *   <li>Face detection and quality assessment</li>
 *   <li>Face embedding extraction and normalization</li>
 *   <li>Similarity search against large databases (1M+ faces)</li>
 *   <li>Result ranking and confidence scoring</li>
 * </ul>
 * 
 * <p>The engine is optimized for CPU-only processing and can handle real-time
 * queries against million-face databases with sub-second response times.</p>
 * 
 * @author Spring Vision Team
 * @since 1.0.0
 */
public class FaceRecognitionEngine {

    private static final Logger logger = LoggerFactory.getLogger(FaceRecognitionEngine.class);

    private final VisionBackend visionBackend;
    private final FaceEmbeddingIndex embeddingIndex;
    private final FaceQualityAssessor qualityAssessor;
    
    // Configuration parameters
    private final double minQualityThreshold;
    private final double minSimilarityThreshold;
    private final int maxResultsPerQuery;
    
    /**
     * Create a new face recognition engine.
     * 
     * @param visionBackend backend for face detection and embedding extraction
     * @param embeddingIndex index for similarity search
     * @param qualityAssessor face quality assessment component
     * @param config engine configuration
     */
    public FaceRecognitionEngine(VisionBackend visionBackend,
                                FaceEmbeddingIndex embeddingIndex,
                                FaceQualityAssessor qualityAssessor,
                                RecognitionConfig config) {
        this.visionBackend = visionBackend;
        this.embeddingIndex = embeddingIndex;
        this.qualityAssessor = qualityAssessor;
        
        this.minQualityThreshold = config.minQualityThreshold();
        this.minSimilarityThreshold = config.minSimilarityThreshold();
        this.maxResultsPerQuery = config.maxResultsPerQuery();
        
        logger.info("Face recognition engine initialized with {} embeddings", 
                   embeddingIndex.size());
    }
    
    /**
     * Find matching faces for a query selfie.
     * 
     * <p>This is the main entry point for face recognition. It performs the complete
     * pipeline from face detection to similarity search and result ranking.</p>
     * 
     * @param selfieImage query image containing a face
     * @param topK maximum number of matches to return
     * @return list of face matches ordered by similarity (best first)
     * @throws BaseVisionException if processing fails
     */
    public List<FaceMatch> findMatches(ImageData selfieImage, int topK) throws BaseVisionException {
        if (selfieImage == null || selfieImage.isEmpty()) {
            throw new IllegalArgumentException("Selfie image must not be null or empty");
        }
        if (topK <= 0 || topK > maxResultsPerQuery) {
            throw new IllegalArgumentException("topK must be between 1 and " + maxResultsPerQuery);
        }
        
        long startTime = System.currentTimeMillis();
        String correlationId = generateCorrelationId();
        
        logger.info("Starting face recognition query [{}] - requesting {} matches", correlationId, topK);
        
        try {
            // Step 1: Detect faces and assess quality
            List<Detection> detections = detectAndValidateFaces(selfieImage, correlationId);
            if (detections.isEmpty()) {
                logger.info("No valid faces detected in query image [{}]", correlationId);
                return List.of();
            }
            
            // Step 2: Select the best face for recognition
            Detection bestFace = selectBestFace(detections, correlationId);
            
            // Step 3: Extract and normalize embedding
            float[] queryEmbedding = extractAndNormalizeEmbedding(selfieImage, bestFace, correlationId);
            if (queryEmbedding == null) {
                logger.warn("Failed to extract valid embedding [{}]", correlationId);
                return List.of();
            }
            
            // Step 4: Search for similar embeddings
            List<FaceEmbeddingIndex.SearchResult> searchResults = embeddingIndex.search(
                queryEmbedding, Math.min(topK * 3, 1000)); // Get more candidates for reranking
            
            // Step 5: Rank and filter results
            List<FaceMatch> finalMatches = rankAndFilterResults(searchResults, bestFace, correlationId);
            
            // Step 6: Limit to requested number of results
            List<FaceMatch> topMatches = finalMatches.stream()
                .limit(topK)
                .collect(Collectors.toList());
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            logger.info("Face recognition completed [{}]: {} matches in {}ms", 
                       correlationId, topMatches.size(), processingTime);
            
            return topMatches;
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            logger.error("Face recognition failed [{}] after {}ms", correlationId, processingTime, e);
            throw new VisionProcessingException(
                "Face recognition processing failed", 
                "recognition_error", 
                correlationId, 
                e
            );
        }
    }
    
    /**
     * Asynchronous face recognition for high-throughput scenarios.
     * 
     * @param selfieImage query image
     * @param topK maximum number of matches
     * @return CompletableFuture containing face matches
     */
    public CompletableFuture<List<FaceMatch>> findMatchesAsync(ImageData selfieImage, int topK) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return findMatches(selfieImage, topK);
            } catch (BaseVisionException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Detect faces and validate their quality for recognition.
     */
    private List<Detection> detectAndValidateFaces(ImageData imageData, String correlationId) 
            throws BaseVisionException {
        
        logger.debug("Detecting faces [{}]", correlationId);
        
        List<Detection> allDetections = visionBackend.detectFaces(imageData);
        List<Detection> validDetections = new ArrayList<>();
        
        for (Detection detection : allDetections) {
            double qualityScore = qualityAssessor.assessQuality(detection);
            
            if (qualityScore >= minQualityThreshold) {
                // Add quality score to detection attributes
                Map<String, Object> enhancedAttributes = new java.util.HashMap<>(detection.attributes());
                enhancedAttributes.put("quality_score", qualityScore);
                enhancedAttributes.put("recognition_ready", true);
                
                Detection enhancedDetection = new Detection(
                    detection.label(),
                    detection.confidence(),
                    detection.boundingBox(),
                    enhancedAttributes
                );
                
                validDetections.add(enhancedDetection);
                logger.debug("Valid face detected [{}]: confidence={:.3f}, quality={:.3f}", 
                           correlationId, detection.confidence(), qualityScore);
            } else {
                logger.debug("Face rejected due to low quality [{}]: quality={:.3f} < {:.3f}", 
                           correlationId, qualityScore, minQualityThreshold);
            }
        }
        
        logger.info("Face validation [{}]: {} valid faces from {} detections", 
                   correlationId, validDetections.size(), allDetections.size());
        
        return validDetections;
    }
    
    /**
     * Select the best face from multiple detections for recognition.
     */
    private Detection selectBestFace(List<Detection> detections, String correlationId) {
        if (detections.size() == 1) {
            return detections.get(0);
        }
        
        // Score faces based on multiple criteria
        Detection bestFace = null;
        double bestScore = -1.0;
        
        for (Detection detection : detections) {
            double score = calculateFaceSelectionScore(detection);
            
            if (score > bestScore) {
                bestScore = score;
                bestFace = detection;
            }
        }
        
        logger.debug("Selected best face [{}]: score={:.3f} from {} candidates", 
                    correlationId, bestScore, detections.size());
        
        return bestFace;
    }
    
    /**
     * Calculate a combined score for face selection.
     */
    private double calculateFaceSelectionScore(Detection detection) {
        double confidence = detection.confidence();
        double qualityScore = (double) detection.attributes().getOrDefault("quality_score", 0.5);
        double areaRatio = calculateFaceAreaRatio(detection);
        
        // Weighted combination favoring quality and size
        return 0.4 * qualityScore + 0.3 * confidence + 0.3 * areaRatio;
    }
    
    /**
     * Calculate face area as ratio of image area.
     */
    private double calculateFaceAreaRatio(Detection detection) {
        var bbox = detection.boundingBox();
        double faceArea = bbox.width() * bbox.height();
        
        // Optimal face area is around 10-25% of image
        if (faceArea >= 0.1 && faceArea <= 0.25) {
            return 1.0;
        } else if (faceArea >= 0.05 && faceArea <= 0.5) {
            return 0.8;
        } else {
            return Math.max(0.1, Math.min(1.0, faceArea * 4)); // Scale smaller faces
        }
    }
    
    /**
     * Extract and normalize face embedding for recognition.
     */
    private float[] extractAndNormalizeEmbedding(ImageData imageData, Detection detection, String correlationId) 
            throws BaseVisionException {
        
        logger.debug("Extracting embedding [{}]", correlationId);
        
        try {
            List<float[]> embeddings = visionBackend.extractEmbeddings(imageData);
            
            if (embeddings.isEmpty()) {
                logger.warn("No embeddings extracted [{}]", correlationId);
                return null;
            }
            
            // Use the first embedding (backends should return embeddings in quality order)
            float[] embedding = embeddings.get(0);
            
            // Normalize embedding for consistent distance calculation
            float[] normalizedEmbedding = normalizeL2(embedding);
            
            logger.debug("Embedding extracted [{}]: dimension={}, norm={:.6f}", 
                        correlationId, normalizedEmbedding.length, vectorNorm(normalizedEmbedding));
            
            return normalizedEmbedding;
            
        } catch (Exception e) {
            logger.error("Embedding extraction failed [{}]", correlationId, e);
            return null;
        }
    }
    
    /**
     * L2 normalize an embedding vector.
     */
    private float[] normalizeL2(float[] vector) {
        double norm = 0.0;
        for (float value : vector) {
            norm += value * value;
        }
        norm = Math.sqrt(norm);
        
        if (norm < 1e-8) {
            // Return zero vector if norm is too small
            return new float[vector.length];
        }
        
        float[] normalized = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = (float) (vector[i] / norm);
        }
        
        return normalized;
    }
    
    /**
     * Calculate the L2 norm of a vector.
     */
    private double vectorNorm(float[] vector) {
        double norm = 0.0;
        for (float value : vector) {
            norm += value * value;
        }
        return Math.sqrt(norm);
    }
    
    /**
     * Rank and filter search results based on multiple criteria.
     */
    private List<FaceMatch> rankAndFilterResults(List<FaceEmbeddingIndex.SearchResult> searchResults,
                                                Detection queryFace,
                                                String correlationId) {
        
        logger.debug("Ranking {} search results [{}]", searchResults.size(), correlationId);
        
        List<FaceMatch> matches = new ArrayList<>();
        
        for (FaceEmbeddingIndex.SearchResult result : searchResults) {
            if (result.similarity() >= minSimilarityThreshold) {
                FaceMatch match = new FaceMatch(
                    result.photoId(),
                    result.similarity(),
                    result.distance(),
                    calculateMatchConfidence(result, queryFace),
                    Map.of(
                        "query_correlation_id", correlationId,
                        "search_distance", result.distance(),
                        "base_similarity", result.similarity()
                    )
                );
                matches.add(match);
            }
        }
        
        // Sort by overall confidence (combination of similarity and other factors)
        matches.sort((a, b) -> Double.compare(b.confidence(), a.confidence()));
        
        logger.debug("Filtered to {} matches above similarity threshold {:.3f} [{}]", 
                    matches.size(), minSimilarityThreshold, correlationId);
        
        return matches;
    }
    
    /**
     * Calculate overall match confidence combining similarity with other factors.
     */
    private double calculateMatchConfidence(FaceEmbeddingIndex.SearchResult result, Detection queryFace) {
        double baseSimilarity = result.similarity();
        double queryQuality = (double) queryFace.attributes().getOrDefault("quality_score", 0.7);
        
        // Boost confidence for high-quality queries
        double qualityBoost = Math.min(0.1, (queryQuality - 0.7) * 0.5);
        double confidence = Math.min(1.0, baseSimilarity + qualityBoost);
        
        return confidence;
    }
    
    /**
     * Generate a correlation ID for request tracking.
     */
    private String generateCorrelationId() {
        return "rec-" + System.currentTimeMillis() % 100000 + "-" + 
               Integer.toHexString((int) (Math.random() * 0xFFFF));
    }
    
    /**
     * Get engine statistics and performance metrics.
     */
    public RecognitionEngineStats getStatistics() {
        var indexStats = embeddingIndex.getStatistics();
        
        return new RecognitionEngineStats(
            indexStats.totalEmbeddings(),
            indexStats.averageQueryTimeMillis(),
            indexStats.totalQueries(),
            minQualityThreshold,
            minSimilarityThreshold
        );
    }
    
    /**
     * Configuration for the face recognition engine.
     */
    public record RecognitionConfig(
        double minQualityThreshold,    // Minimum face quality for recognition (0.0-1.0)
        double minSimilarityThreshold, // Minimum similarity for match results (0.0-1.0) 
        int maxResultsPerQuery        // Maximum results to return per query
    ) {
        public static RecognitionConfig defaultConfig() {
            return new RecognitionConfig(0.3, 0.6, 100);
        }
        
        public static RecognitionConfig highAccuracyConfig() {
            return new RecognitionConfig(0.5, 0.75, 50);
        }
        
        public static RecognitionConfig fastSearchConfig() {
            return new RecognitionConfig(0.2, 0.5, 200);
        }
    }
    
    /**
     * Face match result with similarity and confidence information.
     */
    public record FaceMatch(
        String photoId,
        double similarity,
        double distance,
        double confidence,
        Map<String, Object> metadata
    ) implements Comparable<FaceMatch> {
        
        @Override
        public int compareTo(FaceMatch other) {
            return Double.compare(other.confidence, this.confidence);
        }
    }
    
    /**
     * Recognition engine performance statistics.
     */
    public record RecognitionEngineStats(
        long totalIndexedFaces,
        double averageQueryTimeMs,
        long totalQueries,
        double qualityThreshold,
        double similarityThreshold
    ) {}
} 