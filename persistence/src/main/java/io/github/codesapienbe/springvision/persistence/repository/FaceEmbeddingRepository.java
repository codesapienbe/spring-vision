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

    /**
     * Finds all embeddings for a specific person.
     *
     * @param personId the person identifier
     * @return list of face embeddings
     */
    List<FaceEmbedding> findByPersonId(String personId);

    /**
     * Finds all embeddings created by a specific model.
     *
     * @param modelName the model name
     * @return list of face embeddings
     */
    List<FaceEmbedding> findByModelName(String modelName);

    /**
     * Finds embeddings for a specific person and model combination.
     *
     * @param modelName the model name
     * @param personId  the person identifier
     * @return list of face embeddings
     */
    List<FaceEmbedding> findByModelNameAndPersonId(String modelName, String personId);

    /**
     * Counts embeddings created by a specific model.
     *
     * @param modelName the model name
     * @return count of embeddings
     */
    @Query("SELECT COUNT(e) FROM FaceEmbedding e WHERE e.modelName = :modelName")
    long countByModelName(@Param("modelName") String modelName);

    /**
     * Finds the first embedding with a specific image hash.
     *
     * @param imageHash the image hash
     * @return optional face embedding
     */
    java.util.Optional<FaceEmbedding> findFirstByImageHash(String imageHash);
}
