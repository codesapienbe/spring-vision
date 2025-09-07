package com.springvision.jpa.service;

import com.springvision.jpa.dto.SimilaritySearchRequest;
import com.springvision.jpa.dto.SimilaritySearchResult;
import com.springvision.jpa.entity.FaceEmbedding;
import com.springvision.jpa.repository.PostgreSQLFaceEmbeddingRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PostgreSQL-backed vector service using pgvector extension.
 */
@Service
@ConditionalOnProperty(value = "spring.vision.vector.provider", havingValue = "pgvector")
@ConditionalOnClass(name = "org.postgresql.util.PGobject")
public class PostgreSQLVectorSimilarityService implements VectorSimilarityService {

    private final PostgreSQLFaceEmbeddingRepository repository;
    private final JdbcTemplate jdbcTemplate;

    public PostgreSQLVectorSimilarityService(PostgreSQLFaceEmbeddingRepository repository, JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String storeFaceEmbedding(com.springvision.jpa.dto.StoreFaceEmbeddingRequest request) {
        // Try native insert with correct pgvector type handling; fall back to JPA save
        String vectorString = formatPgVector(request.embedding());
        byte[] blob = com.springvision.jpa.util.VectorUtils.serializeFloatArray(request.embedding());

        String sql = "INSERT INTO face_embeddings (person_id, model_name, dimension, embedding_blob, pgvector_embedding, image_hash, confidence, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, now(), now()) RETURNING id";

        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                    PreparedStatement ps = con.prepareStatement(sql);
                    ps.setString(1, request.personId());
                    ps.setString(2, request.modelName());
                    ps.setInt(3, request.embedding() == null ? 0 : request.embedding().length);
                    ps.setBytes(4, blob);

                    // attempt to create PGobject via reflection to set type 'vector'
                    try {
                        Class<?> pgObjectClass = Class.forName("org.postgresql.util.PGobject");
                        Object pgObject = pgObjectClass.getDeclaredConstructor().newInstance();
                        pgObjectClass.getMethod("setType", String.class).invoke(pgObject, "vector");
                        pgObjectClass.getMethod("setValue", String.class).invoke(pgObject, vectorString);
                        ps.setObject(5, pgObject, Types.OTHER);
                    } catch (Exception e) {
                        // fallback: set string value
                        ps.setString(5, vectorString);
                    }

                    ps.setString(6, request.imageHash());
                    if (request.confidence() == null) ps.setNull(7, Types.DOUBLE);
                    else ps.setDouble(7, request.confidence());
                    return ps;
                }
            }, keyHolder);

            Object key = keyHolder.getKeys() != null ? keyHolder.getKeys().get("id") : keyHolder.getKey();
            if (key != null) return key.toString();
        } catch (Exception e) {
            // fallback to JPA repository save
        }

        FaceEmbedding embedding = new FaceEmbedding();
        embedding.setPersonId(request.personId());
        embedding.setModelName(request.modelName());
        embedding.setDimension(request.embedding() == null ? 0 : request.embedding().length);
        embedding.setEmbeddingBlob(blob);
        embedding.setPgVectorEmbedding(request.embedding());
        embedding.setImageHash(request.imageHash());
        embedding.setConfidence(request.confidence());

        FaceEmbedding saved = repository.save(embedding);
        return saved.getId() == null ? null : saved.getId().toString();
    }

    @Override
    public List<SimilaritySearchResult> findSimilarFaces(SimilaritySearchRequest request) {
        String vectorString = formatPgVector(request.queryEmbedding());

        List<Object[]> results = repository.findSimilarByCosineSimilarity(vectorString, request.modelName(), request.threshold(), request.limit());

        return results.stream()
            .map(this::mapToSimilarityResult)
            .collect(Collectors.toList());
    }

    private SimilaritySearchResult mapToSimilarityResult(Object[] row) {
        // Expecting: id, person_id, model_name, created_at, confidence, distance
        String id = row[0] == null ? null : row[0].toString();
        String personId = row[1] == null ? null : row[1].toString();
        String modelName = row[2] == null ? null : row[2].toString();
        java.time.LocalDateTime createdAt = row[3] == null ? null : (java.time.LocalDateTime) row[3];
        Double confidence = row[4] == null ? null : ((Number) row[4]).doubleValue();
        Double distance = row[5] == null ? null : ((Number) row[5]).doubleValue();

        double similarity = distance == null ? 0.0 : 1.0 - distance;

        return new SimilaritySearchResult(id, personId, similarity, distance, modelName, createdAt, java.util.Map.of("confidence", confidence));
    }

    private String formatPgVector(float[] embedding) {
        if (embedding == null) return "[]";
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(String.valueOf(embedding[i]));
        }
        sb.append(']');
        return sb.toString();
    }

    @Override
    public VerificationResult verifyFaces(VerificationRequest request) {
        double sim = com.springvision.jpa.util.VectorUtils.cosineSimilarity(request.getEmbeddingA(), request.getEmbeddingB());
        boolean match = sim >= 0.7;
        return new VerificationResult(match, 1.0 - sim, sim);
    }

    @Override
    public void deleteFaceEmbedding(String embeddingId) {
        if (embeddingId == null) return;
        try {
            repository.deleteById(java.util.UUID.fromString(embeddingId));
        } catch (Exception ignored) {
        }
    }

    @Override
    public VectorServiceHealth getHealth() {
        return new VectorServiceHealth(true, "ok");
    }

    @Override
    public java.util.Set<String> getSupportedMetrics() {
        return java.util.Set.of("COSINE");
    }
} 