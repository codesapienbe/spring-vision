package com.springvision.jpa.repository;

import com.springvision.jpa.entity.FaceEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * PostgreSQL-specific repository for vector operations. ConditionalOnClass
 * will be handled by auto-configuration; keep interface minimal to compile.
 */
@Repository
public interface PostgreSQLFaceEmbeddingRepository extends JpaRepository<FaceEmbedding, UUID> {

    @Query(value = """
        SELECT e.id, e.person_id, e.model_name, e.created_at, e.confidence,
               ( (CAST(?1 AS vector) <=> e.native_vector) ) as distance 
        FROM face_embeddings e 
        WHERE e.model_name = ?2 
        AND ( (CAST(?1 AS vector) <=> e.native_vector) ) < ?3
        ORDER BY distance ASC
        LIMIT ?4
        """, nativeQuery = true)
    List<Object[]> findSimilarByCosineSimilarity(String queryVector, String modelName, Double threshold, Integer limit);

    java.util.Optional<FaceEmbedding> findFirstByImageHash(String imageHash);
} 