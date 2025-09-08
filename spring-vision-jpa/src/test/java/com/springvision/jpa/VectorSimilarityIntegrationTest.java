package com.springvision.jpa;

import com.springvision.jpa.dto.SimilaritySearchRequest;
import com.springvision.jpa.dto.SimilaritySearchResult;
import com.springvision.jpa.dto.StoreFaceEmbeddingRequest;
import com.springvision.jpa.service.VectorSimilarityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class VectorSimilarityIntegrationTest {

    @Autowired
    private VectorSimilarityService vectorService;

    @Test
    void testStoreFaceEmbedding() {
        StoreFaceEmbeddingRequest request = new StoreFaceEmbeddingRequest(
            "person-123",
            new float[]{0.1f, 0.2f, 0.3f},
            "arcface",
            "hash123",
            0.95,
            java.util.Map.of()
        );

        String embeddingId = vectorService.storeFaceEmbedding(request);
        assertNotNull(embeddingId);
    }

    @Test
    void testFindSimilarFaces() {
        storeTestEmbeddings();

        SimilaritySearchRequest searchRequest = new SimilaritySearchRequest(
            new float[]{0.1f, 0.2f, 0.3f},
            "arcface",
            null,
            com.springvision.jpa.enums.SimilarityMetric.COSINE,
            0.5,
            5,
            java.util.Set.of(),
            java.util.Set.of()
        );

        List<SimilaritySearchResult> results = vectorService.findSimilarFaces(searchRequest);
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.get(0).similarity() >= 0.5);
    }

    private void storeTestEmbeddings() {
        vectorService.storeFaceEmbedding(new StoreFaceEmbeddingRequest("person-a", new float[]{0.1f,0.2f,0.3f}, "arcface", "h1", 0.9, java.util.Map.of()));
        vectorService.storeFaceEmbedding(new StoreFaceEmbeddingRequest("person-b", new float[]{0.9f,0.1f,0.2f}, "arcface", "h2", 0.85, java.util.Map.of()));
        vectorService.storeFaceEmbedding(new StoreFaceEmbeddingRequest("person-c", new float[]{0.0f,0.1f,0.0f}, "arcface", "h3", 0.8, java.util.Map.of()));
    }
} 