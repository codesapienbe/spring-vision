package io.github.codesapienbe.springvision.persistence;

import io.github.codesapienbe.springvision.persistence.dto.StoreFaceEmbeddingRequest;
import io.github.codesapienbe.springvision.persistence.dto.SimilaritySearchRequest;
import io.github.codesapienbe.springvision.persistence.dto.SimilaritySearchResult;
import io.github.codesapienbe.springvision.persistence.service.VectorSimilarityService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for PostgreSQL (native postgres provider). Disabled by default; remove
 * the {@code @Disabled} annotation to run against a real Postgres instance
 * with the pgvector extension installed.
 */
@SpringBootTest
@Disabled("Postgres integration tests are disabled by default; enable when Postgres+pgvector is available")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/springvision_test",
    "spring.datasource.username=postgres",
    "spring.datasource.password=postgres",
    "spring.jpa.hibernate.ddl-auto=update",
    "spring.vision.vector.provider=postgres"
})
class PostgresPgVectorIntegrationTest {

    @Autowired
    private VectorSimilarityService vectorService;

    @Test
    void testStoreAndFindSimilar() {
        StoreFaceEmbeddingRequest req = new StoreFaceEmbeddingRequest(
            "person-pg",
            new float[]{0.1f, 0.2f, 0.3f},
            "arcface",
            "hash-pg",
            0.95,
            java.util.Map.of()
        );

        String id = vectorService.storeFaceEmbedding(req);
        assertNotNull(id, "Embedding id should not be null after store");

        SimilaritySearchRequest search = new SimilaritySearchRequest(
            new float[]{0.1f, 0.2f, 0.3f},
            "arcface",
            null,
            io.github.codesapienbe.springvision.persistence.enums.SimilarityMetric.COSINE,
            0.5,
            10,
            Set.of(),
            Set.of()
        );

        List<SimilaritySearchResult> results = vectorService.findSimilarFaces(search);
        assertNotNull(results, "Results should not be null");
        assertFalse(results.isEmpty(), "Expected at least one similar result");
    }
}
