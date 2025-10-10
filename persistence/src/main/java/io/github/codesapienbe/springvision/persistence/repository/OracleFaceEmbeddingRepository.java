package io.github.codesapienbe.springvision.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import io.github.codesapienbe.springvision.persistence.entity.FaceEmbedding;

/**
 * Oracle-specific repository for vector operations.
 */
@Repository
public interface OracleFaceEmbeddingRepository extends JpaRepository<FaceEmbedding, UUID> {

    @Query(value = """
        SELECT e.id, e.person_id, e.model_name, e.created_at, e.confidence,
               VECTOR_DISTANCE(e.native_vector, :queryVector, COSINE) as distance
        FROM face_embeddings e
        WHERE e.model_name = :modelName
        AND VECTOR_DISTANCE(e.native_vector, :queryVector, COSINE) < :threshold
        ORDER BY distance ASC
        FETCH FIRST :limit ROWS ONLY
        """, nativeQuery = true)
    List<Object[]> findSimilarByCosineSimilarity(@Param("queryVector") byte[] queryVector,
                                                 @Param("modelName") String modelName,
                                                 @Param("threshold") Double threshold,
                                                 @Param("limit") Integer limit);

    java.util.Optional<FaceEmbedding> findFirstByImageHash(String imageHash);
}
