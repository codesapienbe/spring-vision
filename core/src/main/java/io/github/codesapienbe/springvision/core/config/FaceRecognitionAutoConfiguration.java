package io.github.codesapienbe.springvision.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.codesapienbe.springvision.core.VisionBackend;
import io.github.codesapienbe.springvision.core.recognition.BasicFaceQualityAssessor;
import io.github.codesapienbe.springvision.core.recognition.FaceDatabaseBuilder;
import io.github.codesapienbe.springvision.core.recognition.FaceEmbeddingIndex;
import io.github.codesapienbe.springvision.core.recognition.FaceQualityAssessor;
import io.github.codesapienbe.springvision.core.recognition.FaceRecognitionEngine;
import io.github.codesapienbe.springvision.core.recognition.HNSWConfig;
import io.github.codesapienbe.springvision.core.recognition.HNSWFaceIndex;
import io.github.codesapienbe.springvision.core.recognition.FaceRecognitionEngine.RecognitionConfig;

/**
 * Auto-configuration for face recognition components.
 *
 * <p>This configuration automatically sets up the face recognition system when
 * enabled via properties. It configures the HNSW index, face recognition engine,
 * database builder, and quality assessor.</p>
 *
 * <p>Configuration properties:</p>
 * <ul>
 *   <li><code>spring.vision.recognition.enabled</code> - Enable face recognition (default: false)</li>
 *   <li><code>spring.vision.recognition.backend</code> - Vision backend to use (default: opencv)</li>
 *   <li><code>spring.vision.recognition.embedding-dimension</code> - Face embedding dimension (default: 512)</li>
 *   <li><code>spring.vision.recognition.max-database-size</code> - Maximum faces in database (default: 2000000)</li>
 *   <li><code>spring.vision.recognition.quality-threshold</code> - Minimum face quality (default: 0.3)</li>
 *   <li><code>spring.vision.recognition.similarity-threshold</code> - Minimum similarity for matches (default: 0.6)</li>
 *   <li><code>spring.vision.recognition.max-results</code> - Maximum results per query (default: 100)</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "spring.vision.recognition", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(FaceRecognitionAutoConfiguration.FaceRecognitionProperties.class)
public class FaceRecognitionAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(FaceRecognitionAutoConfiguration.class);

    /**
     * Default constructor for {@link FaceRecognitionAutoConfiguration}.
     */
    public FaceRecognitionAutoConfiguration() {
        // Default constructor
    }

    /**
     * Create the HNSW face embedding index.
     * @param properties The face recognition properties.
     * @return The configured face embedding index.
     */
    @Bean
    public FaceEmbeddingIndex faceEmbeddingIndex(FaceRecognitionAutoConfiguration.FaceRecognitionProperties properties) {
        logger.info("Creating HNSW face embedding index with dimension: {}",
            properties.getEmbeddingDimension());

        HNSWConfig config = createHNSWConfig(properties);
        HNSWFaceIndex index = new HNSWFaceIndex(properties.getEmbeddingDimension(), config);

        logger.info("HNSW face embedding index created successfully");
        return index;
    }

    /**
     * Create the face quality assessor.
     * @return The configured face quality assessor.
     */
    @Bean
    public FaceQualityAssessor faceQualityAssessor() {
        logger.info("Creating basic face quality assessor");
        return new BasicFaceQualityAssessor();
    }

    /**
     * Create the face recognition engine.
     * @param visionBackend The vision backend.
     * @param embeddingIndex The face embedding index.
     * @param qualityAssessor The face quality assessor.
     * @param properties The face recognition properties.
     * @return The configured face recognition engine.
     */
    @Bean
    public FaceRecognitionEngine faceRecognitionEngine(
        VisionBackend visionBackend,
        FaceEmbeddingIndex embeddingIndex,
        FaceQualityAssessor qualityAssessor,
        FaceRecognitionAutoConfiguration.FaceRecognitionProperties properties) {

        logger.info("Creating face recognition engine");

        RecognitionConfig config = new RecognitionConfig(
            properties.getQualityThreshold(),
            properties.getSimilarityThreshold(),
            properties.getMaxResults()
        );

        FaceRecognitionEngine engine = new FaceRecognitionEngine(
            visionBackend, embeddingIndex, qualityAssessor, config);

        logger.info("Face recognition engine created successfully");
        return engine;
    }

    /**
     * Create the face database builder.
     * @param visionBackend The vision backend.
     * @param embeddingIndex The face embedding index.
     * @param qualityAssessor The face quality assessor.
     * @param properties The face recognition properties.
     * @return The configured face database builder.
     */
    @Bean
    public FaceDatabaseBuilder faceDatabaseBuilder(
        VisionBackend visionBackend,
        FaceEmbeddingIndex embeddingIndex,
        FaceQualityAssessor qualityAssessor,
        FaceRecognitionAutoConfiguration.FaceRecognitionProperties properties) {

        logger.info("Creating face database builder");

        FaceDatabaseBuilder.DatabaseBuilderConfig config = new FaceDatabaseBuilder.DatabaseBuilderConfig(
            properties.getParallelThreads(),
            properties.getBatchSize(),
            properties.getQualityThreshold(),
            100,  // progressReportInterval - report every 100 files
            false // stopOnError - continue on errors
        );

        FaceDatabaseBuilder builder = new FaceDatabaseBuilder(
            visionBackend, embeddingIndex, qualityAssessor, config);

        logger.info("Face database builder created successfully");
        return builder;
    }

    /**
     * Create HNSW configuration based on properties.
     */
    private HNSWConfig createHNSWConfig(FaceRecognitionAutoConfiguration.FaceRecognitionProperties properties) {
        String accuracyMode = properties.getHnsw().getAccuracyMode();

        return switch (accuracyMode.toLowerCase()) {
            case "high" -> HNSWConfig.highAccuracyConfig();
            case "fast" -> HNSWConfig.fastSearchConfig();
            case "large" -> HNSWConfig.largeDatabaseConfig();
            case "memory" -> HNSWConfig.memoryEfficientConfig();
            case "balanced" -> HNSWConfig.defaultConfig();
            default -> HNSWConfig.defaultConfig();
        };
    }

    /**
     * Configuration properties for face recognition.
     */
    @org.springframework.boot.context.properties.ConfigurationProperties(prefix = "spring.vision.recognition")
    public static class FaceRecognitionProperties {

        private boolean enabled = false;
        private String backend = "opencv";
        private int embeddingDimension = 512;
        private int maxDatabaseSize = 2_000_000;
        private double qualityThreshold = 0.3;
        private double similarityThreshold = 0.6;
        private int maxResults = 100;
        private int parallelThreads = Runtime.getRuntime().availableProcessors();
        private int batchSize = 8;
        private HNSWProperties hnsw = new HNSWProperties();

        /**
         * Default constructor for {@link FaceRecognitionProperties}.
         */
        public FaceRecognitionProperties() {
            // Default constructor
        }

        // Getters and setters
        /**
         * Checks if face recognition is enabled.
         * @return {@code true} if enabled, {@code false} otherwise.
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets whether face recognition is enabled.
         * @param enabled {@code true} to enable, {@code false} to disable.
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Gets the vision backend to use.
         * @return The vision backend name.
         */
        public String getBackend() {
            return backend;
        }

        /**
         * Sets the vision backend to use.
         * @param backend The vision backend name.
         */
        public void setBackend(String backend) {
            this.backend = backend;
        }

        /**
         * Gets the face embedding dimension.
         * @return The embedding dimension.
         */
        public int getEmbeddingDimension() {
            return embeddingDimension;
        }

        /**
         * Sets the face embedding dimension.
         * @param embeddingDimension The embedding dimension.
         */
        public void setEmbeddingDimension(int embeddingDimension) {
            this.embeddingDimension = embeddingDimension;
        }

        /**
         * Gets the maximum number of faces in the database.
         * @return The maximum database size.
         */
        public int getMaxDatabaseSize() {
            return maxDatabaseSize;
        }

        /**
         * Sets the maximum number of faces in the database.
         * @param maxDatabaseSize The maximum database size.
         */
        public void setMaxDatabaseSize(int maxDatabaseSize) {
            this.maxDatabaseSize = maxDatabaseSize;
        }

        /**
         * Gets the minimum face quality threshold.
         * @return The quality threshold.
         */
        public double getQualityThreshold() {
            return qualityThreshold;
        }

        /**
         * Sets the minimum face quality threshold.
         * @param qualityThreshold The quality threshold.
         */
        public void setQualityThreshold(double qualityThreshold) {
            this.qualityThreshold = qualityThreshold;
        }

        /**
         * Gets the minimum similarity for matches.
         * @return The similarity threshold.
         */
        public double getSimilarityThreshold() {
            return similarityThreshold;
        }

        /**
         * Sets the minimum similarity for matches.
         * @param similarityThreshold The similarity threshold.
         */
        public void setSimilarityThreshold(double similarityThreshold) {
            this.similarityThreshold = similarityThreshold;
        }

        /**
         * Gets the maximum number of results per query.
         * @return The maximum number of results.
         */
        public int getMaxResults() {
            return maxResults;
        }

        /**
         * Sets the maximum number of results per query.
         * @param maxResults The maximum number of results.
         */
        public void setMaxResults(int maxResults) {
            this.maxResults = maxResults;
        }

        /**
         * Gets the number of parallel threads for processing.
         * @return The number of parallel threads.
         */
        public int getParallelThreads() {
            return parallelThreads;
        }

        /**
         * Sets the number of parallel threads for processing.
         * @param parallelThreads The number of parallel threads.
         */
        public void setParallelThreads(int parallelThreads) {
            this.parallelThreads = parallelThreads;
        }

        /**
         * Gets the batch size for processing.
         * @return The batch size.
         */
        public int getBatchSize() {
            return batchSize;
        }

        /**
         * Sets the batch size for processing.
         * @param batchSize The batch size.
         */
        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        /**
         * Gets the HNSW-specific configuration properties.
         * @return The HNSW properties.
         */
        public HNSWProperties getHnsw() {
            return hnsw;
        }

        /**
         * Sets the HNSW-specific configuration properties.
         * @param hnsw The HNSW properties.
         */
        public void setHnsw(HNSWProperties hnsw) {
            this.hnsw = hnsw;
        }

        /**
         * HNSW-specific configuration properties.
         */
        public static class HNSWProperties {
            private String accuracyMode = "balanced"; // balanced, high, fast, large, memory

            /**
             * Default constructor for {@link HNSWProperties}.
             */
            public HNSWProperties() {
                // Default constructor
            }

            /**
             * Gets the accuracy mode for the HNSW index.
             * @return The accuracy mode.
             */
            public String getAccuracyMode() {
                return accuracyMode;
            }

            /**
             * Sets the accuracy mode for the HNSW index.
             * @param accuracyMode The accuracy mode.
             */
            public void setAccuracyMode(String accuracyMode) {
                this.accuracyMode = accuracyMode;
            }
        }
    }
}
