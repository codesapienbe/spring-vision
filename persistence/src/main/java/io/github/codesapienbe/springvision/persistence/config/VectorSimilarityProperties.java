package io.github.codesapienbe.springvision.persistence.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for vector similarity features within the persistence module.
 * These properties allow for fine-tuning the behavior of vector indexes and similarity search providers.
 */
@ConfigurationProperties(prefix = "spring.vision.vector")
public class VectorSimilarityProperties {

    /** The desired vector provider. AUTO will attempt to detect the best one based on the database vendor. */
    private VectorProvider provider = VectorProvider.AUTO;
    /** PostgreSQL-specific vector settings. */
    private PostgreSQL postgresql = new PostgreSQL();
    /** Oracle-specific vector settings. */
    private Oracle oracle = new Oracle();
    /** MySQL-specific vector settings. */
    private MySQL mysql = new MySQL();

    public VectorProvider getProvider() {
        return provider;
    }

    public void setProvider(VectorProvider provider) {
        this.provider = provider;
    }

    public PostgreSQL getPostgresql() {
        return postgresql;
    }

    public void setPostgresql(PostgreSQL postgresql) {
        this.postgresql = postgresql;
    }

    public Oracle getOracle() {
        return oracle;
    }

    public void setOracle(Oracle oracle) {
        this.oracle = oracle;
    }

    public MySQL getMysql() {
        return mysql;
    }

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

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getIndexType() {
            return indexType;
        }

        public void setIndexType(String indexType) {
            this.indexType = indexType;
        }

        public int getHnswM() {
            return hnswM;
        }

        public void setHnswM(int hnswM) {
            this.hnswM = hnswM;
        }

        public int getHnswEfConstruction() {
            return hnswEfConstruction;
        }

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

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getIndexType() {
            return indexType;
        }

        public void setIndexType(String indexType) {
            this.indexType = indexType;
        }

        public int getTargetAccuracy() {
            return targetAccuracy;
        }

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

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getIndexType() {
            return indexType;
        }

        public void setIndexType(String indexType) {
            this.indexType = indexType;
        }
    }

    /**
     * Enumerates the supported vector providers for similarity search.
     */
    public enum VectorProvider {
        /** Automatically detect the provider based on the database vendor. */
        AUTO,
        /** Use PostgreSQL with the pgvector extension. */
        POSTGRES,
        /** Use Oracle's native vector capabilities. */
        ORACLE,
        /** Use MySQL's native vector capabilities. */
        MYSQL,
        /** Use a generic JPA-based implementation (fallback). */
        JPA,
        /** Use an H2-specific implementation for in-memory testing. */
        H2
    }
}
