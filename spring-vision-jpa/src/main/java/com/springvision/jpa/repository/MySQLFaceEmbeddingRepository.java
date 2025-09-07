package com.springvision.jpa.repository;

import com.springvision.jpa.entity.FaceEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * MySQL specific repository for vector operations (placeholder).
 */
@Repository
public interface MySQLFaceEmbeddingRepository extends JpaRepository<FaceEmbedding, UUID> {

    @Query(value = """
        SELECT e.id, e.person_id, e.model_name, e.created_at, e.confidence,
               VECTOR_DISTANCE(e.mysql_embedding, CAST(?1 AS JSON), COSINE) as distance
        FROM face_embeddings e
        WHERE e.model_name = ?2
        AND VECTOR_DISTANCE(e.mysql_embedding, CAST(?1 AS JSON), COSINE) < ?3
        ORDER BY distance ASC
        LIMIT ?4
        """, nativeQuery = true)
    List<Object[]> findSimilarByCosineSimilarity(String queryVectorJson, String modelName, Double threshold, Integer limit);
} 