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
 * Oracle-backed vector similarity service (placeholder implementation).
 */
@Service
@ConditionalOnProperty(value = "spring.vision.vector.provider", havingValue = "oracle")
@ConditionalOnClass(name = "oracle.jdbc.OracleConnection")
public class OracleVectorSimilarityService implements VectorSimilarityService {

    private final io.github.codesapienbe.springvision.persistence.repository.OracleFaceEmbeddingRepository repository;
    private final JdbcTemplate jdbcTemplate;
    private NativeVectorAdapter nativeVectorAdapter;

    @Autowired
    public OracleVectorSimilarityService(io.github.codesapienbe.springvision.persistence.repository.OracleFaceEmbeddingRepository repository, JdbcTemplate jdbcTemplate, io.github.codesapienbe.springvision.persistence.service.NativeVectorAdapterRegistry registry) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
        this.nativeVectorAdapter = registry.getAdapter("oracle");
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

        byte[] blob = io.github.codesapienbe.springvision.persistence.util.VectorUtils.serializeFloatArray(embeddingForInsert);
        Object oracleInsertVal = nativeVectorAdapter != null ? nativeVectorAdapter.toInsertValue(VectorConversionHelpers.serializeFloatArrayToBytes(embeddingForInsert)) : VectorConversionHelpers.serializeFloatArrayToBytes(embeddingForInsert);

        String sql = "INSERT INTO face_embeddings (person_id, model_name, dimension, embedding_blob, native_vector, image_hash, confidence, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, SYSDATE, SYSDATE) RETURNING id INTO ?";

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
                    if (oracleInsertVal instanceof byte[]) ps.setBytes(5, (byte[]) oracleInsertVal);
                    else ps.setObject(5, oracleInsertVal);
                    ps.setString(6, request.imageHash());
                    if (request.confidence() == null) ps.setNull(7, Types.DOUBLE);
                    else ps.setDouble(7, request.confidence());
                    return ps;
                }
            }, keyHolder);

            Object key = keyHolder.getKeys() != null ? keyHolder.getKeys().get("id") : keyHolder.getKey();
            if (key != null) return key.toString();
        } catch (Exception e) {
            // fallback
        }

        FaceEmbedding embedding = new FaceEmbedding();
        embedding.setPersonId(request.personId());
        embedding.setModelName(request.modelName());
        embedding.setDimension(embeddingForInsert == null ? 0 : embeddingForInsert.length);
        embedding.setEmbeddingBlob(blob);
        embedding.setNativeVector(VectorConversionHelpers.serializeFloatArrayToBytes(embeddingForInsert));
        embedding.setImageHash(request.imageHash());
        embedding.setConfidence(request.confidence());

        FaceEmbedding saved = repository.save(embedding);
        return saved.getId() == null ? null : saved.getId().toString();
    }

    @Override
    public List<SimilaritySearchResult> findSimilarFaces(SimilaritySearchRequest request) {
        float[] effectiveEmbedding = request.queryEmbedding();
        if ((effectiveEmbedding == null || effectiveEmbedding.length == 0) && request instanceof io.github.codesapienbe.springvision.persistence.dto.SimilaritySearchRequest) {
            // no query embedding - cannot lookup by imageHash here because DTO lacks imageHash; future enhancement
        }
        Object queryParam = nativeVectorAdapter != null && effectiveEmbedding != null ? nativeVectorAdapter.toQueryParam(VectorConversionHelpers.serializeFloatArrayToBytes(effectiveEmbedding)) : floatArrayToOracleVector(effectiveEmbedding);
        byte[] vectorBytes = queryParam instanceof byte[] ? (byte[]) queryParam : floatArrayToOracleVector(effectiveEmbedding);
        List<Object[]> results = repository.findSimilarByCosineSimilarity(vectorBytes, request.modelName(), request.threshold(), request.limit());
        return results.stream().map(this::mapToSimilarityResult).collect(Collectors.toList());
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

    /**
     * Placeholder conversion; Oracle VECTOR format will be handled in later batch
     * For now we serialize float[] to bytes using VectorConversionHelpers as a portable fallback.
     */
    private byte[] floatArrayToOracleVector(float[] embedding) {
        if (embedding == null) return new byte[0];
        return VectorConversionHelpers.serializeFloatArrayToBytes(embedding);
    }

    // setter removed; adapter injected via registry at construction

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
