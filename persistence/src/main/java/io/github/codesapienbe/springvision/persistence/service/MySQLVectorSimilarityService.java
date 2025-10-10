package io.github.codesapienbe.springvision.persistence.service;

import io.github.codesapienbe.springvision.persistence.dto.SimilaritySearchRequest;
import io.github.codesapienbe.springvision.persistence.dto.SimilaritySearchResult;
import io.github.codesapienbe.springvision.persistence.entity.FaceEmbedding;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A {@link VectorSimilarityService} implementation for MySQL.
 * This service leverages native SQL queries for vector operations when possible, using a JSON representation for vectors.
 * It is activated when the `spring.vision.vector.provider` property is set to `mysql`.
 */
@Service
@ConditionalOnProperty(value = "spring.vision.vector.provider", havingValue = "mysql")
@ConditionalOnClass(name = "com.mysql.cj.jdbc.Driver")
public class MySQLVectorSimilarityService implements VectorSimilarityService {

    private final io.github.codesapienbe.springvision.persistence.repository.MySQLFaceEmbeddingRepository repository;
    private final JdbcTemplate jdbcTemplate;
    private NativeVectorAdapter nativeVectorAdapter;

    /**
     * Constructs a new MySQLVectorSimilarityService.
     *
     * @param repository   The repository for face embedding data.
     * @param jdbcTemplate The JDBC template for executing native SQL queries.
     * @param registry     The registry for obtaining the appropriate native vector adapter.
     */
    @Autowired
    public MySQLVectorSimilarityService(io.github.codesapienbe.springvision.persistence.repository.MySQLFaceEmbeddingRepository repository, JdbcTemplate jdbcTemplate, io.github.codesapienbe.springvision.persistence.service.NativeVectorAdapterRegistry registry) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
        this.nativeVectorAdapter = registry.getAdapter("mysql");
    }

    @Override
    public String storeFaceEmbedding(io.github.codesapienbe.springvision.persistence.dto.StoreFaceEmbeddingRequest request) {
        // prefer existing native vector for same image hash
        float[] embeddingForInsert = request.embedding();
        if (request.imageHash() != null) {
            java.util.Optional<io.github.codesapienbe.springvision.persistence.entity.FaceEmbedding> existing = repository.findFirstByImageHash(request.imageHash());
            if (existing.isPresent() && existing.get().getNativeVector() != null && existing.get().getNativeVector().length > 0) {
                embeddingForInsert = NativeVectorMapper.bytesToFloatArray(existing.get().getNativeVector());
            }
        }

        FaceEmbedding embedding = new FaceEmbedding();
        embedding.setPersonId(request.personId());
        embedding.setModelName(request.modelName());
        embedding.setDimension(embeddingForInsert == null ? 0 : embeddingForInsert.length);
        final float[] insertEmbedding = embeddingForInsert == null ? new float[0] : embeddingForInsert;
        final byte[] blob = io.github.codesapienbe.springvision.persistence.util.VectorUtils.serializeFloatArray(insertEmbedding);
        embedding.setEmbeddingBlob(blob);
        // store JSON-like representation for MySQL vector column
        final Object jsonParam = nativeVectorAdapter != null ? nativeVectorAdapter.toInsertValue(io.github.codesapienbe.springvision.persistence.service.VectorConversionHelpers.serializeFloatArrayToBytes(insertEmbedding)) : formatMySqlVectorJson(insertEmbedding);

        String sql = "INSERT INTO face_embeddings (person_id, model_name, dimension, embedding_blob, native_vector, image_hash, confidence, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, now(), now())";
        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                    PreparedStatement ps = con.prepareStatement(sql, new String[]{"id"});
                    ps.setString(1, request.personId());
                    ps.setString(2, request.modelName());
                    ps.setInt(3, insertEmbedding == null ? 0 : insertEmbedding.length);
                    ps.setBytes(4, blob);
                    ps.setString(5, String.valueOf(jsonParam));
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

        // fallback save: store native vector only; vendor-specific columns removed from entity
        embedding.setNativeVector(io.github.codesapienbe.springvision.persistence.service.VectorConversionHelpers.serializeFloatArrayToBytes(embeddingForInsert));
        io.github.codesapienbe.springvision.persistence.entity.FaceEmbedding saved = repository.save(embedding);
        return saved.getId() == null ? null : saved.getId().toString();
    }

    @Override
    public List<SimilaritySearchResult> findSimilarFaces(SimilaritySearchRequest request) {
        // Placeholder: delegate to repository when implemented
        Object queryParam = nativeVectorAdapter != null && request.queryEmbedding() != null ? nativeVectorAdapter.toQueryParam(io.github.codesapienbe.springvision.persistence.service.VectorConversionHelpers.serializeFloatArrayToBytes(request.queryEmbedding())) : formatMySqlVector(request.queryEmbedding());
        List<Object[]> results = repository.findSimilarByCosineSimilarity(String.valueOf(queryParam), request.modelName(), request.threshold(), request.limit());
        return results.stream().map(this::mapToSimilarityResult).collect(Collectors.toList());
    }

    private String formatMySqlVector(float[] embedding) {
        // For MySQL native queries we may use JSON-style vector, reuse NativeVectorMapper
        return NativeVectorMapper.toMySqlJson(embedding);
    }

    private String formatMySqlVectorJson(float[] embedding) {
        return NativeVectorMapper.toMySqlJson(embedding);
    }

    // setter removed; adapter resolved via registry at construction

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
        double sim = io.github.codesapienbe.springvision.persistence.util.VectorUtils.cosineSimilarity(request.getEmbeddingA(), request.getEmbeddingB());
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
