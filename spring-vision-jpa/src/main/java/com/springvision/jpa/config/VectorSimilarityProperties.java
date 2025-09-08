package com.springvision.jpa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for vector similarity auto-configuration.
 */
@ConfigurationProperties(prefix = "spring.vision.vector")
public class VectorSimilarityProperties {

    private VectorProvider provider = VectorProvider.AUTO;
    private PostgreSQL postgresql = new PostgreSQL();
    private Oracle oracle = new Oracle();
    private MySQL mysql = new MySQL();

    public VectorProvider getProvider() { return provider; }
    public void setProvider(VectorProvider provider) { this.provider = provider; }

    public PostgreSQL getPostgresql() { return postgresql; }
    public void setPostgresql(PostgreSQL postgresql) { this.postgresql = postgresql; }

    public Oracle getOracle() { return oracle; }
    public void setOracle(Oracle oracle) { this.oracle = oracle; }

    public MySQL getMysql() { return mysql; }
    public void setMysql(MySQL mysql) { this.mysql = mysql; }

    public static class PostgreSQL {
        private boolean enabled = true;
        private String indexType = "hnsw";
        private int hnswM = 16;
        private int hnswEfConstruction = 64;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getIndexType() { return indexType; }
        public void setIndexType(String indexType) { this.indexType = indexType; }
        public int getHnswM() { return hnswM; }
        public void setHnswM(int hnswM) { this.hnswM = hnswM; }
        public int getHnswEfConstruction() { return hnswEfConstruction; }
        public void setHnswEfConstruction(int hnswEfConstruction) { this.hnswEfConstruction = hnswEfConstruction; }
    }

    public static class Oracle {
        private boolean enabled = true;
        private String indexType = "hnsw";
        private int targetAccuracy = 95;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getIndexType() { return indexType; }
        public void setIndexType(String indexType) { this.indexType = indexType; }
        public int getTargetAccuracy() { return targetAccuracy; }
        public void setTargetAccuracy(int targetAccuracy) { this.targetAccuracy = targetAccuracy; }
    }

    public static class MySQL {
        private boolean enabled = true;
        private String indexType = "hnsw";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getIndexType() { return indexType; }
        public void setIndexType(String indexType) { this.indexType = indexType; }
    }

    public enum VectorProvider {
        AUTO, POSTGRES, ORACLE, MYSQL, JPA, H2
    }
} 