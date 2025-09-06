package com.springvision.core.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.springvision.core.VisionBackend;
import com.springvision.core.recognition.BasicFaceQualityAssessor;
import com.springvision.core.recognition.FaceDatabaseBuilder;
import com.springvision.core.recognition.FaceEmbeddingIndex;
import com.springvision.core.recognition.FaceQualityAssessor;
import com.springvision.core.recognition.FaceRecognitionEngine;
import com.springvision.core.recognition.HNSWConfig;
import com.springvision.core.recognition.HNSWFaceIndex;
import com.springvision.core.recognition.FaceRecognitionEngine.RecognitionConfig;

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
     * Create the HNSW face embedding index.
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
     */
    @Bean
    public FaceQualityAssessor faceQualityAssessor() {
        logger.info("Creating basic face quality assessor");
        return new BasicFaceQualityAssessor();
    }

    /**
     * Create the face recognition engine.
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
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public String getBackend() { return backend; }
        public void setBackend(String backend) { this.backend = backend; }
        
        public int getEmbeddingDimension() { return embeddingDimension; }
        public void setEmbeddingDimension(int embeddingDimension) { this.embeddingDimension = embeddingDimension; }
        
        public int getMaxDatabaseSize() { return maxDatabaseSize; }
        public void setMaxDatabaseSize(int maxDatabaseSize) { this.maxDatabaseSize = maxDatabaseSize; }
        
        public double getQualityThreshold() { return qualityThreshold; }
        public void setQualityThreshold(double qualityThreshold) { this.qualityThreshold = qualityThreshold; }
        
        public double getSimilarityThreshold() { return similarityThreshold; }
        public void setSimilarityThreshold(double similarityThreshold) { this.similarityThreshold = similarityThreshold; }
        
        public int getMaxResults() { return maxResults; }
        public void setMaxResults(int maxResults) { this.maxResults = maxResults; }
        
        public int getParallelThreads() { return parallelThreads; }
        public void setParallelThreads(int parallelThreads) { this.parallelThreads = parallelThreads; }
        
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
        
        public HNSWProperties getHnsw() { return hnsw; }
        public void setHnsw(HNSWProperties hnsw) { this.hnsw = hnsw; }
        
        /**
         * HNSW-specific configuration properties.
         */
        public static class HNSWProperties {
            private String accuracyMode = "balanced"; // balanced, high, fast, large, memory
            
            public String getAccuracyMode() { return accuracyMode; }
            public void setAccuracyMode(String accuracyMode) { this.accuracyMode = accuracyMode; }
        }
    }
} 