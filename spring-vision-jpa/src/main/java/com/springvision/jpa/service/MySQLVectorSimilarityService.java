package com.springvision.jpa.service;

import com.springvision.jpa.dto.SimilaritySearchRequest;
import com.springvision.jpa.dto.SimilaritySearchResult;
import com.springvision.jpa.entity.FaceEmbedding;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MySQL-backed vector similarity service (placeholder).
 */
@Service
@ConditionalOnProperty(value = "spring.vision.vector.provider", havingValue = "mysql")
@ConditionalOnClass(name = "com.mysql.cj.jdbc.Driver")
public class MySQLVectorSimilarityService implements VectorSimilarityService {

    private final com.springvision.jpa.repository.MySQLFaceEmbeddingRepository repository;
    private final JdbcTemplate jdbcTemplate;

    public MySQLVectorSimilarityService(com.springvision.jpa.repository.MySQLFaceEmbeddingRepository repository, JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String storeFaceEmbedding(com.springvision.jpa.dto.StoreFaceEmbeddingRequest request) {
        FaceEmbedding embedding = new FaceEmbedding();
        embedding.setPersonId(request.personId());
        embedding.setModelName(request.modelName());
        embedding.setDimension(request.embedding() == null ? 0 : request.embedding().length);
        byte[] blob = com.springvision.jpa.util.VectorUtils.serializeFloatArray(request.embedding());
        embedding.setEmbeddingBlob(blob);
        // store JSON-like representation for MySQL vector column
        String json = formatMySqlVectorJson(request.embedding());

        String sql = "INSERT INTO face_embeddings (person_id, model_name, dimension, embedding_blob, mysql_embedding, image_hash, confidence, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, now(), now())";
        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                    PreparedStatement ps = con.prepareStatement(sql, new String[]{"id"});
                    ps.setString(1, request.personId());
                    ps.setString(2, request.modelName());
                    ps.setInt(3, request.embedding() == null ? 0 : request.embedding().length);
                    ps.setBytes(4, blob);
                    ps.setString(5, json);
                    ps.setString(6, request.imageHash());
                    if (request.confidence() == null) ps.setNull(7, Types.DOUBLE);
                    else ps.setDouble(7, request.confidence());
                    return ps;
                }
            }, keyHolder);

            Number key = keyHolder.getKey();
            if (key != null) return key.toString();
        } catch (Exception e) {
            // fallback to repository
        }

        // fallback save
        embedding.setMysqlEmbedding(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        com.springvision.jpa.entity.FaceEmbedding saved = repository.save(embedding);
        return saved.getId() == null ? null : saved.getId().toString();
    }

    @Override
    public List<SimilaritySearchResult> findSimilarFaces(SimilaritySearchRequest request) {
        // Placeholder: delegate to repository when implemented
        List<Object[]> results = repository.findSimilarByCosineSimilarity(formatMySqlVector(request.queryEmbedding()), request.modelName(), request.threshold(), request.limit());
        return results.stream().map(this::mapToSimilarityResult).collect(Collectors.toList());
    }

    private String formatMySqlVector(float[] embedding) {
        if (embedding == null) return "";
        return "(" + java.util.Arrays.stream(embedding).mapToObj(String::valueOf).collect(java.util.stream.Collectors.joining(",")) + ")";
    }

    private String formatMySqlVectorJson(float[] embedding) {
        if (embedding == null) return "[]";
        return "[" + java.util.Arrays.stream(embedding).mapToObj(String::valueOf).collect(java.util.stream.Collectors.joining(",")) + "]";
    }

    private SimilaritySearchResult mapToSimilarityResult(Object[] row) {
        String id = row[0] == null ? null : row[0].toString();
        String personId = row[1] == null ? null : row[1].toString();
        String modelName = row[2] == null ? null : row[2].toString();
        java.time.LocalDateTime createdAt = row[3] == null ? null : (java.time.LocalDateTime) row[3];
        Double confidence = row[4] == null ? null : ((Number) row[4]).doubleValue();
        Double distance = row[5] == null ? null : ((Number) row[5]).doubleValue();
        double similarity = distance == null ? 0.0 : 1.0 - distance;
        return new SimilaritySearchResult(id, personId, similarity, distance, modelName, createdAt, java.util.Map.of("confidence", confidence));
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