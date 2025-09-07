package com.springvision.jpa.config;

import com.springvision.jpa.service.DatabaseVendorDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import com.springvision.jpa.enums.DatabaseVendor;

/**
 * Creates vector-related schema (indexes/extensions) depending on database vendor.
 */
@Component
@ConditionalOnBean(DatabaseVendorDetector.class)
public class VectorSchemaManager {

    private static final Logger log = LoggerFactory.getLogger(VectorSchemaManager.class);

    private final DatabaseVendorDetector vendorDetector;
    private final JdbcTemplate jdbcTemplate;
    private final VectorSimilarityProperties properties;

    @Autowired
    public VectorSchemaManager(DatabaseVendorDetector vendorDetector, JdbcTemplate jdbcTemplate, VectorSimilarityProperties properties) {
        this.vendorDetector = vendorDetector;
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        DatabaseVendor vendor = vendorDetector.detectVendor();
        createVectorSchema(vendor);
    }

    private void createVectorSchema(DatabaseVendor vendor) {
        switch (vendor) {
            case POSTGRESQL -> createPostgreSQLSchema();
            case ORACLE -> createOracleSchema();
            case MYSQL -> createMySQLSchema();
            default -> log.info("No vector schema actions for vendor: {}", vendor);
        }
    }

    private void createPostgreSQLSchema() {
        if (properties.getPostgresql().isEnabled()) {
            executeSQL("CREATE EXTENSION IF NOT EXISTS vector");
            executeSQL(String.format("CREATE INDEX IF NOT EXISTS idx_face_embeddings_pgvector ON face_embeddings USING hnsw (pgvector_embedding vector_cosine_ops) WITH (m = %d, ef_construction = %d)",
                    properties.getPostgresql().getHnswM(), properties.getPostgresql().getHnswEfConstruction()));
        }
    }

    private void createOracleSchema() {
        if (properties.getOracle().isEnabled()) {
            // Placeholder: Oracle vector index creation; vendor-specific SQL will vary
            log.info("Oracle vector schema creation is enabled but requires manual SQL for Oracle 23ai");
        }
    }

    private void createMySQLSchema() {
        if (properties.getMysql().isEnabled()) {
            log.info("MySQL vector schema creation is enabled but requires MySQL 8+ vector features and possibly manual setup");
        }
    }

    private void executeSQL(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            log.warn("Failed to execute SQL (non-fatal): {}: {}", sql, e.getMessage());
        }
    }
} 