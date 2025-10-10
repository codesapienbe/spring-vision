package io.github.codesapienbe.springvision.core.recognition;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.VisionBackend;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;
import io.github.codesapienbe.springvision.core.exception.VisionProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
 * <p>Supports various face detection backends including lightweight models like
 * BlazeFace optimized for short-range selfie-like images from smartphone cameras
 * or webcams, using SSD convolutional networks with custom encoders.</p>
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
     * @param visionBackend   backend for face detection and embedding extraction
     * @param embeddingIndex  index for similarity search
     * @param qualityAssessor face quality assessment component
     * @param config          engine configuration
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
     * @param topK        maximum number of matches to return
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
                .limit(topK).toList();

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
     * @param topK        maximum number of matches
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
                // Additional validation: check for facial landmarks
                boolean hasLandmarks = validateWithLandmarks(imageData, detection, correlationId);

                if (hasLandmarks) {
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
                    logger.debug("Valid face detected [{}]: confidence={}, quality={}",
                        correlationId, detection.confidence(), qualityScore);
                } else {
                    logger.debug("Face rejected due to missing landmarks [{}]: confidence={}, quality={}",
                        correlationId, detection.confidence(), qualityScore);
                }
            } else {
                logger.debug("Face rejected due to low quality [{}]: quality={} < {}",
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

        logger.debug("Selected best face [{}]: score={} from {} candidates",
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

            logger.debug("Embedding extracted [{}]: dimension={}, norm={}",
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

        logger.debug("Filtered to {} matches above similarity threshold {} [{}]",
            matches.size(), minSimilarityThreshold, correlationId);

        return matches;
    }

    /**
     * Calculate overall match confidence combining similarity with other factors.
     */
    private double calculateMatchConfidence(FaceEmbeddingIndex.SearchResult result, Detection queryFace) {
        double baseSimilarity = result.similarity();
        double detectionConfidence = queryFace.confidence();
        double queryQuality = (double) queryFace.attributes().getOrDefault("quality_score", 0.7);

        // Boost confidence for high-quality queries
        double qualityBoost = Math.min(0.1, (queryQuality - 0.7) * 0.5);

        // Combine similarity with detection confidence and quality boost
        return Math.min(1.0, baseSimilarity * detectionConfidence + qualityBoost);
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
     *
     * @param minQualityThreshold    the minimum face quality threshold for recognition (0.0-1.0)
     * @param minSimilarityThreshold the minimum similarity threshold for match results (0.0-1.0)
     * @param maxResultsPerQuery     the maximum number of results to return per query
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
     *
     * @param photoId    the identifier of the matched photo
     * @param similarity the similarity score (0.0 to 1.0)
     * @param distance   the distance score
     * @param confidence the overall confidence score
     * @param metadata   additional metadata about the match
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
     *
     * @param totalIndexedFaces   the total number of faces indexed
     * @param averageQueryTimeMs  the average query time in milliseconds
     * @param totalQueries        the total number of queries performed
     * @param qualityThreshold    the quality threshold used
     * @param similarityThreshold the similarity threshold used
     */
    public record RecognitionEngineStats(
        long totalIndexedFaces,
        double averageQueryTimeMs,
        long totalQueries,
        double qualityThreshold,
        double similarityThreshold
    ) {
    }

    /**
     * Validate face detection by checking for facial landmarks.
     */
    private boolean validateWithLandmarks(ImageData imageData, Detection detection, String correlationId) {
        try {
            // Check if backend supports landmark detection
            if (!visionBackend.supportsDetectionType(io.github.codesapienbe.springvision.core.DetectionType.LANDMARK)) {
                logger.debug("Landmark detection not supported by backend [{}], using confidence fallback", correlationId);
                // Fallback: reject low-confidence detections
                return detection.confidence() >= 0.8;
            }

            // Detect landmarks in the image
            List<Detection> landmarks = visionBackend.detectLandmarks(imageData);

            // Check if any landmarks are within the face bounding box and their distribution
            var bbox = detection.boundingBox();
            int landmarkCount = 0;
            int topCount = 0, bottomCount = 0, leftCount = 0, rightCount = 0;
            double midX = bbox.x() + bbox.width() / 2;
            double midY = bbox.y() + bbox.height() / 2;

            for (Detection landmark : landmarks) {
                var landmarkBbox = landmark.boundingBox();
                double lx = landmarkBbox.x() + landmarkBbox.width() / 2;
                double ly = landmarkBbox.y() + landmarkBbox.height() / 2;

                if (bbox.contains(lx, ly)) {
                    landmarkCount++;

                    // Check distribution
                    if (ly < midY) topCount++;
                    else bottomCount++;

                    if (lx < midX) leftCount++;
                    else rightCount++;
                }
            }

            // Require at least a minimum number of landmarks and proper distribution
            int minLandmarks = 50; // Adjusted for face mesh models with 478 points
            int minPerRegion = 10; // Minimum landmarks in each quadrant
            boolean hasEnoughLandmarks = landmarkCount >= minLandmarks;
            boolean wellDistributed = topCount >= minPerRegion && bottomCount >= minPerRegion &&
                leftCount >= minPerRegion && rightCount >= minPerRegion;

            logger.debug("Landmark validation [{}]: {} landmarks found (min: {}), distribution: T{}/B{}/L{}/R{} (min per region: {})",
                correlationId, landmarkCount, minLandmarks, topCount, bottomCount, leftCount, rightCount, minPerRegion);

            return hasEnoughLandmarks && wellDistributed;

        } catch (Exception e) {
            logger.warn("Landmark validation failed [{}]: {}", correlationId, e.getMessage());
            // If landmark detection fails, use confidence fallback
            return detection.confidence() >= 0.8;
        }
    }
}
