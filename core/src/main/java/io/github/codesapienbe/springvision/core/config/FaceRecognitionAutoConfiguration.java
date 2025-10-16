package io.github.codesapienbe.springvision.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.codesapienbe.springvision.core.VisionBackend;

/**
 * Auto-configuration for face recognition components.
 *
 * <p>DISABLED - This configuration is temporarily disabled pending implementation
 * of the recognition package classes. Will be re-enabled in a future phase.</p>
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
        logger.warn("FaceRecognitionAutoConfiguration is currently disabled - recognition package not yet implemented");
    }

    // TODO: Re-enable these beans when recognition package is implemented
    // All bean methods commented out to avoid compilation errors

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
         *
         * @return {@code true} if enabled, {@code false} otherwise.
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets whether face recognition is enabled.
         *
         * @param enabled {@code true} to enable, {@code false} to disable.
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Gets the vision backend to use.
         *
         * @return The vision backend name.
         */
        public String getBackend() {
            return backend;
        }

        /**
         * Sets the vision backend to use.
         *
         * @param backend The vision backend name.
         */
        public void setBackend(String backend) {
            this.backend = backend;
        }

        /**
         * Gets the face embedding dimension.
         *
         * @return The embedding dimension.
         */
        public int getEmbeddingDimension() {
            return embeddingDimension;
        }

        /**
         * Sets the face embedding dimension.
         *
         * @param embeddingDimension The embedding dimension.
         */
        public void setEmbeddingDimension(int embeddingDimension) {
            this.embeddingDimension = embeddingDimension;
        }

        /**
         * Gets the maximum number of faces in the database.
         *
         * @return The maximum database size.
         */
        public int getMaxDatabaseSize() {
            return maxDatabaseSize;
        }

        /**
         * Sets the maximum number of faces in the database.
         *
         * @param maxDatabaseSize The maximum database size.
         */
        public void setMaxDatabaseSize(int maxDatabaseSize) {
            this.maxDatabaseSize = maxDatabaseSize;
        }

        /**
         * Gets the minimum face quality threshold.
         *
         * @return The quality threshold.
         */
        public double getQualityThreshold() {
            return qualityThreshold;
        }

        /**
         * Sets the minimum face quality threshold.
         *
         * @param qualityThreshold The quality threshold.
         */
        public void setQualityThreshold(double qualityThreshold) {
            this.qualityThreshold = qualityThreshold;
        }

        /**
         * Gets the minimum similarity for matches.
         *
         * @return The similarity threshold.
         */
        public double getSimilarityThreshold() {
            return similarityThreshold;
        }

        /**
         * Sets the minimum similarity for matches.
         *
         * @param similarityThreshold The similarity threshold.
         */
        public void setSimilarityThreshold(double similarityThreshold) {
            this.similarityThreshold = similarityThreshold;
        }

        /**
         * Gets the maximum number of results per query.
         *
         * @return The maximum number of results.
         */
        public int getMaxResults() {
            return maxResults;
        }

        /**
         * Sets the maximum number of results per query.
         *
         * @param maxResults The maximum number of results.
         */
        public void setMaxResults(int maxResults) {
            this.maxResults = maxResults;
        }

        /**
         * Gets the number of parallel threads for processing.
         *
         * @return The number of parallel threads.
         */
        public int getParallelThreads() {
            return parallelThreads;
        }

        /**
         * Sets the number of parallel threads for processing.
         *
         * @param parallelThreads The number of parallel threads.
         */
        public void setParallelThreads(int parallelThreads) {
            this.parallelThreads = parallelThreads;
        }

        /**
         * Gets the batch size for processing.
         *
         * @return The batch size.
         */
        public int getBatchSize() {
            return batchSize;
        }

        /**
         * Sets the batch size for processing.
         *
         * @param batchSize The batch size.
         */
        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        /**
         * Gets the HNSW-specific configuration properties.
         *
         * @return The HNSW properties.
         */
        public HNSWProperties getHnsw() {
            return hnsw;
        }

        /**
         * Sets the HNSW-specific configuration properties.
         *
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
             *
             * @return The accuracy mode.
             */
            public String getAccuracyMode() {
                return accuracyMode;
            }

            /**
             * Sets the accuracy mode for the HNSW index.
             *
             * @param accuracyMode The accuracy mode.
             */
            public void setAccuracyMode(String accuracyMode) {
                this.accuracyMode = accuracyMode;
            }
        }
    }
}
