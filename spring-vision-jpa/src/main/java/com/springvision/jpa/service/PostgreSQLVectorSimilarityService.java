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
import org.springframework.beans.factory.annotation.Autowired;

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
@ConditionalOnProperty(value = "spring.vision.vector.provider", havingValue = "postgres")
@ConditionalOnClass(name = "org.postgresql.util.PGobject")
public class PostgreSQLVectorSimilarityService implements VectorSimilarityService {

    private final PostgreSQLFaceEmbeddingRepository repository;
    private final JdbcTemplate jdbcTemplate;
    private NativeVectorAdapter nativeVectorAdapter;

    @Autowired
    public PostgreSQLVectorSimilarityService(PostgreSQLFaceEmbeddingRepository repository, JdbcTemplate jdbcTemplate, com.springvision.jpa.service.NativeVectorAdapterRegistry registry) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
        // Constructor-injected adapter via registry for safety and immutability
        this.nativeVectorAdapter = registry.getAdapter("postgres");
    }

    @Override
    public String storeFaceEmbedding(com.springvision.jpa.dto.StoreFaceEmbeddingRequest request) {
        // Prefer existing native vector for the same image hash if available
        float[] embeddingForInsert = request.embedding();
        if (request.imageHash() != null) {
            java.util.Optional<FaceEmbedding> existing = repository.findFirstByImageHash(request.imageHash());
            if (existing.isPresent() && existing.get().getNativeVector() != null && existing.get().getNativeVector().length > 0) {
                embeddingForInsert = NativeVectorMapper.bytesToFloatArray(existing.get().getNativeVector());
            }
        }

        // Try native insert with correct pgvector type handling; fall back to JPA save
        final float[] insertEmbedding = embeddingForInsert == null ? new float[0] : embeddingForInsert;
        final Object vectorParam = nativeVectorAdapter != null ? nativeVectorAdapter.toInsertValue(com.springvision.jpa.service.VectorConversionHelpers.serializeFloatArrayToBytes(insertEmbedding)) : formatPgVector(insertEmbedding);
        final byte[] blob = com.springvision.jpa.util.VectorUtils.serializeFloatArray(insertEmbedding);

        String sql = "INSERT INTO face_embeddings (person_id, model_name, dimension, embedding_blob, native_vector, image_hash, confidence, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, now(), now()) RETURNING id";

        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                    PreparedStatement ps = con.prepareStatement(sql);
                    ps.setString(1, request.personId());
                    ps.setString(2, request.modelName());
                    ps.setInt(3, insertEmbedding == null ? 0 : insertEmbedding.length);
                    ps.setBytes(4, blob);

                    // attempt to create PGobject via reflection to set type 'vector'
                    try {
                        if (nativeVectorAdapter != null) {
                            ps.setObject(5, vectorParam, Types.OTHER);
                        } else {
                            Class<?> pgObjectClass = Class.forName("org.postgresql.util.PGobject");
                            Object pgObject = pgObjectClass.getDeclaredConstructor().newInstance();
                            pgObjectClass.getMethod("setType", String.class).invoke(pgObject, "vector");
                            pgObjectClass.getMethod("setValue", String.class).invoke(pgObject, formatPgVector(insertEmbedding));
                            ps.setObject(5, pgObject, Types.OTHER);
                        }
                    } catch (Exception e) {
                        // fallback: set string value
                        ps.setString(5, formatPgVector(insertEmbedding));
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
        // store portable native bytes
        embedding.setNativeVector(com.springvision.jpa.service.VectorConversionHelpers.serializeFloatArrayToBytes(embeddingForInsert));
        embedding.setImageHash(request.imageHash());
        embedding.setConfidence(request.confidence());

        FaceEmbedding saved = repository.save(embedding);
        return saved.getId() == null ? null : saved.getId().toString();
    }

    @Override
    public List<SimilaritySearchResult> findSimilarFaces(SimilaritySearchRequest request) {
        float[] effectiveEmbedding = request.queryEmbedding();

        // If caller didn't provide a query embedding but provided an imageHash, try to reuse stored native vector
        if ((effectiveEmbedding == null || effectiveEmbedding.length == 0) && request instanceof com.springvision.jpa.dto.SimilaritySearchRequest) {
            // nothing: keep as-is (we may enhance DTO later). Attempt to use repository lookup by imageHash if present in request metadata (not present currently).
        }

        Object queryParam = nativeVectorAdapter != null && effectiveEmbedding != null ? nativeVectorAdapter.toQueryParam(com.springvision.jpa.service.VectorConversionHelpers.serializeFloatArrayToBytes(effectiveEmbedding)) : formatPgVector(effectiveEmbedding == null ? new float[0] : effectiveEmbedding);

        List<Object[]> results = repository.findSimilarByCosineSimilarity(String.valueOf(queryParam), request.modelName(), request.threshold(), request.limit());

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
        return NativeVectorMapper.toPgVectorString(embedding);
    }

    // setter removed; adapter is resolved at construction time via registry

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