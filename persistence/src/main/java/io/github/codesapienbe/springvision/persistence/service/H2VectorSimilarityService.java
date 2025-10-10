package io.github.codesapienbe.springvision.persistence.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Primary;

import io.github.codesapienbe.springvision.persistence.repository.FaceEmbeddingRepository;

/**
 * H2-backed vector similarity service.
 *
 * <p>This is a thin specialization of {@link JpaVectorSimilarityService} that
 * is enabled when the application property `spring.vision.vector.provider` is
 * set to `h2`. It reuses the JPA repository implementation and is intended
 * for tests and lightweight development environments using an in-memory H2
 * database.</p>
 */
@Service
@Primary
@ConditionalOnProperty(value = "spring.vision.vector.provider", havingValue = "h2")
public class H2VectorSimilarityService extends JpaVectorSimilarityService {

    /**
     * Constructs an H2 vector similarity service.
     *
     * @param embeddingRepository the face embedding repository
     */
    public H2VectorSimilarityService(FaceEmbeddingRepository embeddingRepository) {
        super(embeddingRepository);
    }
}
