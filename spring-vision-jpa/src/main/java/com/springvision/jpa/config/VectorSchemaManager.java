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
            case H2 -> createH2Schema();
            default -> log.info("No vector schema actions for vendor: {}", vendor);
        }
    }

    private void createH2Schema() {
        try {
            // Minimal table compatible with the JPA entity for H2 in-memory tests.
            // We store a portable `embedding_blob` and a single `native_vector` byte[] used by
            // provider adapters to convert into DB-native formats when performing native operations.
            executeSQL("CREATE TABLE IF NOT EXISTS face_embeddings ("
                + "id UUID PRIMARY KEY,"
                + "person_id VARCHAR(255) NOT NULL,"
                + "model_name VARCHAR(255) NOT NULL,"
                + "dimension INTEGER,"
                + "embedding_blob BLOB NOT NULL,"
                + "native_vector BLOB,"
                + "image_hash VARCHAR(255),"
                + "confidence DOUBLE,"
                + "created_at TIMESTAMP,"
                + "updated_at TIMESTAMP,"
                + "version BIGINT)"
            );
        } catch (Exception e) {
            log.warn("Failed to create H2 test schema: {}", e.getMessage());
        }
    }

    private void createPostgreSQLSchema() {
        if (properties.getPostgresql().isEnabled()) {
            executeSQL("CREATE EXTENSION IF NOT EXISTS vector");
            // Create index on `native_vector` which will be populated with a provider-native
            // representation (pgvector) when the adapter is used. The SQL below assumes
            // pgvector extension is present and that native_vector can be cast/used as vector.
            executeSQL(String.format("CREATE INDEX IF NOT EXISTS idx_face_embeddings_native_vector ON face_embeddings USING hnsw (native_vector vector_cosine_ops) WITH (m = %d, ef_construction = %d)",
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