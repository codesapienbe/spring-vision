package io.github.codesapienbe.springvision.persistence.repository;

import io.github.codesapienbe.springvision.persistence.entity.FaceEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * MySQL specific repository for vector operations.
 * <p>
 * Note: This uses native queries that assume MySQL 8+/vector support or a JSON-based vector representation.
 */
@Repository
public interface MySQLFaceEmbeddingRepository extends JpaRepository<FaceEmbedding, UUID> {

    /**
     * Finds similar embeddings using cosine similarity on MySQL vector columns.
     *
     * @param queryVectorJson the query vector as JSON string
     * @param modelName       the model name to filter by
     * @param threshold       the maximum distance threshold
     * @param limit           the maximum number of results
     * @return list of result arrays containing embedding data and distance
     */
    @Query(value = """
        SELECT e.id, e.person_id, e.model_name, e.created_at, e.confidence,
               VECTOR_DISTANCE(e.native_vector, CAST(?1 AS JSON), COSINE) as distance
        FROM face_embeddings e
        WHERE e.model_name = ?2
        AND VECTOR_DISTANCE(e.native_vector, CAST(?1 AS JSON), COSINE) < ?3
        ORDER BY distance ASC
        LIMIT ?4
        """, nativeQuery = true)
    List<Object[]> findSimilarByCosineSimilarity(String queryVectorJson, String modelName, Double threshold, Integer limit);

    /**
     * Finds the first embedding with a specific image hash.
     *
     * @param imageHash the image hash
     * @return optional face embedding
     */
    java.util.Optional<FaceEmbedding> findFirstByImageHash(String imageHash);
}
