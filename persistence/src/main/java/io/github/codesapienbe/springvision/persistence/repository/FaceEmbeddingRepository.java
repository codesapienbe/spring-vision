package io.github.codesapienbe.springvision.persistence.repository;

import io.github.codesapienbe.springvision.persistence.entity.FaceEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for FaceEmbedding entities.
 */
@Repository
public interface FaceEmbeddingRepository extends JpaRepository<FaceEmbedding, UUID> {

    List<FaceEmbedding> findByPersonId(String personId);

    List<FaceEmbedding> findByModelName(String modelName);

    List<FaceEmbedding> findByModelNameAndPersonId(String modelName, String personId);

    @Query("SELECT COUNT(e) FROM FaceEmbedding e WHERE e.modelName = :modelName")
    long countByModelName(@Param("modelName") String modelName);

    java.util.Optional<FaceEmbedding> findFirstByImageHash(String imageHash);
}
