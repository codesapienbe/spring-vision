package io.github.codesapienbe.springvision.persistence.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for vector similarity features within the persistence module.
 * These properties allow for fine-tuning the behavior of vector indexes and similarity search providers.
 */
@Component
@ConfigurationProperties(prefix = "spring.vision.vector")
public class VectorSimilarityProperties {

    /**
     * The desired vector provider. AUTO will attempt to detect the best one based on the database vendor.
     */
    private VectorProvider provider = VectorProvider.AUTO;
    /**
     * PostgreSQL-specific vector settings.
     */
    private PostgreSQL postgresql = new PostgreSQL();
    /**
     * Oracle-specific vector settings.
     */
    private Oracle oracle = new Oracle();
    /**
     * MySQL-specific vector settings.
     */
    private MySQL mysql = new MySQL();

    /**
     * Default constructor for VectorSimilarityProperties.
     */
    public VectorSimilarityProperties() {
        // Default constructor
    }

    /**
     * Gets the desired vector provider.
     * @return The desired vector provider.
     */
    public VectorProvider getProvider() {
        return provider;
    }

    /**
     * Sets the desired vector provider.
     * @param provider The desired vector provider.
     */
    public void setProvider(VectorProvider provider) {
        this.provider = provider;
    }

    /**
     * Gets the PostgreSQL-specific vector settings.
     * @return The PostgreSQL-specific vector settings.
     */
    public PostgreSQL getPostgresql() {
        return postgresql;
    }

    /**
     * Sets the PostgreSQL-specific vector settings.
     * @param postgresql The PostgreSQL-specific vector settings.
     */
    public void setPostgresql(PostgreSQL postgresql) {
        this.postgresql = postgresql;
    }

    /**
     * Gets the Oracle-specific vector settings.
     * @return The Oracle-specific vector settings.
     */
    public Oracle getOracle() {
        return oracle;
    }

    /**
     * Sets the Oracle-specific vector settings.
     * @param oracle The Oracle-specific vector settings.
     */
    public void setOracle(Oracle oracle) {
        this.oracle = oracle;
    }

    /**
     * Gets the MySQL-specific vector settings.
     * @return The MySQL-specific vector settings.
     */
    public MySQL getMysql() {
        return mysql;
    }

    /**
     * Sets the MySQL-specific vector settings.
     * @param mysql The MySQL-specific vector settings.
     */
    public void setMysql(MySQL mysql) {
        this.mysql = mysql;
    }

    /**
     * Configuration for PostgreSQL vector similarity using the pgvector extension.
     */
    public static class PostgreSQL {
        private boolean enabled = true;
        private String indexType = "hnsw";
        private int hnswM = 16;
        private int hnswEfConstruction = 64;

        /**
         * Default constructor for PostgreSQL settings.
         */
        public PostgreSQL() {
            // Default constructor
        }

        /**
         * Checks if PostgreSQL vector features are enabled.
         * @return Whether PostgreSQL vector features are enabled.
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets whether PostgreSQL vector features are enabled.
         * @param enabled Whether PostgreSQL vector features are enabled.
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Gets the type of index to use for vectors.
         * @return The type of index to use for vectors.
         */
        public String getIndexType() {
            return indexType;
        }

        /**
         * Sets the type of index to use for vectors.
         * @param indexType The type of index to use for vectors.
         */
        public void setIndexType(String indexType) {
            this.indexType = indexType;
        }

        /**
         * Gets the M value for HNSW indexes.
         * @return The M value for HNSW indexes.
         */
        public int getHnswM() {
            return hnswM;
        }

        /**
         * Sets the M value for HNSW indexes.
         * @param hnswM The M value for HNSW indexes.
         */
        public void setHnswM(int hnswM) {
            this.hnswM = hnswM;
        }

        /**
         * Gets the ef_construction value for HNSW indexes.
         * @return The ef_construction value for HNSW indexes.
         */
        public int getHnswEfConstruction() {
            return hnswEfConstruction;
        }

        /**
         * Sets the ef_construction value for HNSW indexes.
         * @param hnswEfConstruction The ef_construction value for HNSW indexes.
         */
        public void setHnswEfConstruction(int hnswEfConstruction) {
            this.hnswEfConstruction = hnswEfConstruction;
        }
    }

    /**
     * Configuration for Oracle vector similarity features.
     */
    public static class Oracle {
        private boolean enabled = true;
        private String indexType = "hnsw";
        private int targetAccuracy = 95;

        /**
         * Default constructor for Oracle settings.
         */
        public Oracle() {
            // Default constructor
        }

        /**
         * Checks if Oracle vector features are enabled.
         * @return Whether Oracle vector features are enabled.
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets whether Oracle vector features are enabled.
         * @param enabled Whether Oracle vector features are enabled.
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Gets the type of index to use for vectors.
         * @return The type of index to use for vectors.
         */
        public String getIndexType() {
            return indexType;
        }

        /**
         * Sets the type of index to use for vectors.
         * @param indexType The type of index to use for vectors.
         */
        public void setIndexType(String indexType) {
            this.indexType = indexType;
        }

        /**
         * Gets the target accuracy for vector searches.
         * @return The target accuracy for vector searches.
         */
        public int getTargetAccuracy() {
            return targetAccuracy;
        }

        /**
         * Sets the target accuracy for vector searches.
         * @param targetAccuracy The target accuracy for vector searches.
         */
        public void setTargetAccuracy(int targetAccuracy) {
            this.targetAccuracy = targetAccuracy;
        }
    }

    /**
     * Configuration for MySQL vector similarity features.
     */
    public static class MySQL {
        private boolean enabled = true;
        private String indexType = "hnsw";

        /**
         * Default constructor for MySQL settings.
         */
        public MySQL() {
            // Default constructor
        }

        /**
         * Checks if MySQL vector features are enabled.
         * @return Whether MySQL vector features are enabled.
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets whether MySQL vector features are enabled.
         * @param enabled Whether MySQL vector features are enabled.
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Gets the type of index to use for vectors.
         * @return The type of index to use for vectors.
         */
        public String getIndexType() {
            return indexType;
        }

        /**
         * Sets the type of index to use for vectors.
         * @param indexType The type of index to use for vectors.
         */
        public void setIndexType(String indexType) {
            this.indexType = indexType;
        }
    }

    /**
     * Enumerates the supported vector providers for similarity search.
     */
    public enum VectorProvider {
        /**
         * Automatically detect the provider based on the database vendor.
         */
        AUTO,
        /**
         * Use PostgreSQL with the pgvector extension.
         */
        POSTGRES,
        /**
         * Use Oracle's native vector capabilities.
         */
        ORACLE,
        /**
         * Use MySQL's native vector capabilities.
         */
        MYSQL,
        /**
         * Use a generic JPA-based implementation (fallback).
         */
        JPA,
        /**
         * Use an H2-specific implementation for in-memory testing.
         */
        H2
    }
}
