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

    /**
     * Finds similar face embeddings using cosine similarity.
     * @param queryVector The query vector.
     * @param modelName The name of the model.
     * @param threshold The similarity threshold.
     * @param limit The maximum number of results to return.
     * @return A list of similar face embeddings.
     */
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

    /**
     * Finds the first face embedding by image hash.
     * @param imageHash The hash of the image.
     * @return An optional containing the face embedding if found.
     */
    java.util.Optional<FaceEmbedding> findFirstByImageHash(String imageHash);
}
