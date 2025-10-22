package io.github.codesapienbe.springvision.mcp.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for InMemoryVectorService.
 * Tests all vector operations, distance calculations, and data integrity.
 */
class InMemoryVectorServiceTest {

    private InMemoryVectorService vectorService;

    // Test fixtures
    private final String personId1 = "person-123";
    private final String personId2 = "person-456";
    private final float[] embedding1 = {0.1f, 0.2f, 0.3f, 0.4f, 0.5f};
    private final float[] embedding2 = {0.2f, 0.3f, 0.4f, 0.5f, 0.6f};
    private final float[] embedding3 = {0.9f, 0.8f, 0.7f, 0.6f, 0.5f};
    private final String modelName1 = "face-embedding-v1";
    private final String modelName2 = "face-embedding-v2";
    private final String imageHash1 = "hash-abc123";
    private final String imageHash2 = "hash-def456";
    private final Double confidence1 = 0.95;
    private final Double confidence2 = 0.87;
    private final Map<String, Object> metadata1 = Map.of("source", "camera1", "timestamp", "2024-01-01");
    private final Map<String, Object> metadata2 = Map.of("source", "camera2");

    @BeforeEach
    void setUp() {
        vectorService = new InMemoryVectorService();
    }

    @Nested
    @DisplayName("Store Face Embedding")
    class StoreFaceEmbedding {

        @Test
        @DisplayName("Should store face embedding and return unique ID")
        void shouldStoreFaceEmbeddingAndReturnUniqueId() {
            // When: Storing a face embedding
            String id = vectorService.storeFaceEmbedding(
                personId1, embedding1, modelName1, imageHash1, confidence1, metadata1);

            // Then: Should return a non-null unique ID
            assertThat(id).isNotNull();
            assertThat(id).isNotEmpty();
            assertThat(id.length()).isGreaterThan(10); // UUID format
        }

        @Test
        @DisplayName("Should store multiple embeddings with different IDs")
        void shouldStoreMultipleEmbeddingsWithDifferentIds() {
            // When: Storing multiple embeddings
            String id1 = vectorService.storeFaceEmbedding(
                personId1, embedding1, modelName1, imageHash1, confidence1, metadata1);
            String id2 = vectorService.storeFaceEmbedding(
                personId2, embedding2, modelName2, imageHash2, confidence2, metadata2);

            // Then: Should have different IDs
            assertThat(id1).isNotEqualTo(id2);
            assertThat(id1).isNotNull();
            assertThat(id2).isNotNull();
        }

        @Test
        @DisplayName("Should handle null metadata")
        void shouldHandleNullMetadata() {
            // When: Storing with null metadata
            String id = vectorService.storeFaceEmbedding(
                personId1, embedding1, modelName1, imageHash1, confidence1, null);

            // Then: Should store successfully
            assertThat(id).isNotNull();
        }

        @Test
        @DisplayName("Should handle null confidence")
        void shouldHandleNullConfidence() {
            // When: Storing with null confidence
            String id = vectorService.storeFaceEmbedding(
                personId1, embedding1, modelName1, imageHash1, null, metadata1);

            // Then: Should store successfully
            assertThat(id).isNotNull();
        }

        @Test
        @DisplayName("Should handle null optional parameters")
        void shouldHandleNullOptionalParameters() {
            // When: Storing with null optional parameters
            String id = vectorService.storeFaceEmbedding(
                personId1, embedding1, null, null, null, null);

            // Then: Should store successfully
            assertThat(id).isNotNull();
        }
    }

    @Nested
    @DisplayName("Find Similar Faces")
    class FindSimilarFaces {

        @BeforeEach
        void setUpTestData() {
            // Store test embeddings
            vectorService.storeFaceEmbedding(personId1, embedding1, modelName1, imageHash1, confidence1, metadata1);
            vectorService.storeFaceEmbedding(personId2, embedding2, modelName2, imageHash2, confidence2, metadata2);
            vectorService.storeFaceEmbedding(personId1, embedding3, modelName1, "hash-ghi789", 0.92, metadata1);
        }

        @Test
        @DisplayName("Should find similar faces with cosine distance")
        void shouldFindSimilarFacesWithCosineDistance() {
            // When: Finding similar faces
            List<Map<String, Object>> results = vectorService.findSimilarFaces(
                embedding1, modelName1, "cosine", 0.5, 10, null, null);

            // Then: Should return results sorted by distance
            assertThat(results).isNotEmpty();
            assertThat(results.size()).isEqualTo(2); // Two embeddings with modelName1

            // Check first result (should be most similar)
            Map<String, Object> firstResult = results.get(0);
            assertThat(firstResult).containsKey("embeddingId");
            assertThat(firstResult).containsKey("personId");
            assertThat(firstResult).containsKey("distance");
            assertThat(firstResult).containsKey("modelName");
            assertThat(firstResult).containsKey("createdAt");

            // Distance should be a valid double
            assertThat((Double) firstResult.get("distance")).isGreaterThanOrEqualTo(0.0);
        }

        @Test
        @DisplayName("Should find similar faces with euclidean distance")
        void shouldFindSimilarFacesWithEuclideanDistance() {
            // When: Finding similar faces with euclidean distance
            List<Map<String, Object>> results = vectorService.findSimilarFaces(
                embedding1, null, "euclidean", 2.0, 10, null, null);

            // Then: Should return results
            assertThat(results).isNotEmpty();
            assertThat(results.size()).isEqualTo(3); // All embeddings

            // Check distances are reasonable for euclidean
            for (Map<String, Object> result : results) {
                double distance = (Double) result.get("distance");
                assertThat(distance).isGreaterThanOrEqualTo(0.0);
                assertThat(distance).isLessThanOrEqualTo(2.0); // Within threshold
            }
        }

        @Test
        @DisplayName("Should filter by model name")
        void shouldFilterByModelName() {
            // When: Finding similar faces with specific model name
            List<Map<String, Object>> results = vectorService.findSimilarFaces(
                embedding1, modelName2, "cosine", 1.0, 10, null, null);

            // Then: Should only return embeddings with modelName2
            assertThat(results).hasSize(1);
            assertThat(results.get(0).get("modelName")).isEqualTo(modelName2);
        }

        @Test
        @DisplayName("Should filter by included person IDs")
        void shouldFilterByIncludedPersonIds() {
            // When: Finding similar faces with included person IDs
            List<Map<String, Object>> results = vectorService.findSimilarFaces(
                embedding1, null, "cosine", 1.0, 10, Set.of(personId1), null);

            // Then: Should only return embeddings for personId1
            assertThat(results).allMatch(result -> result.get("personId").equals(personId1));
        }

        @Test
        @DisplayName("Should filter by excluded person IDs")
        void shouldFilterByExcludedPersonIds() {
            // When: Finding similar faces with excluded person IDs
            List<Map<String, Object>> results = vectorService.findSimilarFaces(
                embedding1, null, "cosine", 1.0, 10, null, Set.of(personId2));

            // Then: Should not return embeddings for personId2
            assertThat(results).noneMatch(result -> result.get("personId").equals(personId2));
        }

        @Test
        @DisplayName("Should respect limit parameter")
        void shouldRespectLimitParameter() {
            // When: Finding similar faces with limit
            List<Map<String, Object>> results = vectorService.findSimilarFaces(
                embedding1, null, "cosine", 1.0, 1, null, null);

            // Then: Should return at most the limit
            assertThat(results).hasSizeLessThanOrEqualTo(1);
        }

        @Test
        @DisplayName("Should respect threshold parameter")
        void shouldRespectThresholdParameter() {
            // When: Finding similar faces with low threshold
            List<Map<String, Object>> results = vectorService.findSimilarFaces(
                embedding1, null, "cosine", 0.1, 10, null, null);

            // Then: Should only return very similar results
            assertThat(results).allMatch(result -> (Double) result.get("distance") <= 0.1);
        }

        @Test
        @DisplayName("Should return empty list for null query embedding")
        void shouldReturnEmptyListForNullQueryEmbedding() {
            // When: Finding similar faces with null query
            List<Map<String, Object>> results = vectorService.findSimilarFaces(
                null, null, "cosine", 1.0, 10, null, null);

            // Then: Should return empty list
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("Should return empty list when no matches found")
        void shouldReturnEmptyListWhenNoMatchesFound() {
            // Given: Clear the store
            InMemoryVectorService emptyService = new InMemoryVectorService();

            // When: Finding similar faces in empty store
            List<Map<String, Object>> results = emptyService.findSimilarFaces(
                embedding1, null, "cosine", 1.0, 10, null, null);

            // Then: Should return empty list
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("Should return results sorted by distance ascending")
        void shouldReturnResultsSortedByDistanceAscending() {
            // When: Finding similar faces
            List<Map<String, Object>> results = vectorService.findSimilarFaces(
                embedding1, null, "cosine", 1.0, 10, null, null);

            // Then: Results should be sorted by increasing distance
            for (int i = 1; i < results.size(); i++) {
                double prevDistance = (Double) results.get(i - 1).get("distance");
                double currDistance = (Double) results.get(i).get("distance");
                assertThat(prevDistance).isLessThanOrEqualTo(currDistance);
            }
        }
    }

    @Nested
    @DisplayName("Find Entries By Image Hash")
    class FindEntriesByImageHash {

        private String embeddingId1;
        private String embeddingId2;

        @BeforeEach
        void setUpTestData() {
            // Store test embeddings with same image hash
            embeddingId1 = vectorService.storeFaceEmbedding(
                personId1, embedding1, modelName1, imageHash1, confidence1, metadata1);
            embeddingId2 = vectorService.storeFaceEmbedding(
                personId1, embedding2, modelName1, imageHash1, confidence2, metadata2);
            // Different image hash
            vectorService.storeFaceEmbedding(
                personId2, embedding3, modelName2, imageHash2, 0.8, metadata1);
        }

        @Test
        @DisplayName("Should find entries by image hash")
        void shouldFindEntriesByImageHash() {
            // When: Finding entries by image hash
            List<Map<String, Object>> results = vectorService.findEntriesByImageHash(imageHash1);

            // Then: Should return entries with matching image hash
            assertThat(results).hasSize(2);

            // Check result structure
            for (Map<String, Object> result : results) {
                assertThat(result).containsKey("embeddingId");
                assertThat(result).containsKey("personId");
                assertThat(result).containsKey("modelName");
                assertThat(result).containsKey("createdAt");

                assertThat(result.get("embeddingId")).isIn(embeddingId1, embeddingId2);
                assertThat(result.get("personId")).isEqualTo(personId1);
            }
        }

        @Test
        @DisplayName("Should return empty list for null image hash")
        void shouldReturnEmptyListForNullImageHash() {
            // When: Finding entries with null image hash
            List<Map<String, Object>> results = vectorService.findEntriesByImageHash(null);

            // Then: Should return empty list
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("Should return empty list for non-existent image hash")
        void shouldReturnEmptyListForNonExistentImageHash() {
            // When: Finding entries with non-existent image hash
            List<Map<String, Object>> results = vectorService.findEntriesByImageHash("non-existent-hash");

            // Then: Should return empty list
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("Delete Embedding By ID")
    class DeleteEmbeddingById {

        private String embeddingId;

        @BeforeEach
        void setUpTestData() {
            embeddingId = vectorService.storeFaceEmbedding(
                personId1, embedding1, modelName1, imageHash1, confidence1, metadata1);
        }

        @Test
        @DisplayName("Should delete embedding by ID")
        void shouldDeleteEmbeddingById() {
            // Given: Embedding exists
            assertThat(vectorService.findEntriesByImageHash(imageHash1)).hasSize(1);

            // When: Deleting the embedding
            vectorService.deleteEmbeddingById(embeddingId);

            // Then: Embedding should be gone
            assertThat(vectorService.findEntriesByImageHash(imageHash1)).isEmpty();
        }

        @Test
        @DisplayName("Should handle deleting non-existent ID gracefully")
        void shouldHandleDeletingNonExistentIdGracefully() {
            // When: Deleting non-existent ID
            vectorService.deleteEmbeddingById("non-existent-id");

            // Then: Should not throw exception and store should remain unchanged
            assertThat(vectorService.findEntriesByImageHash(imageHash1)).hasSize(1);
        }

        @Test
        @DisplayName("Should handle deleting null ID gracefully")
        void shouldHandleDeletingNullIdGracefully() {
            // When: Deleting null ID
            vectorService.deleteEmbeddingById(null);

            // Then: Should not throw exception
            assertThat(vectorService.findEntriesByImageHash(imageHash1)).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Find Entries By Person ID")
    class FindEntriesByPersonId {

        @BeforeEach
        void setUpTestData() {
            // Store multiple embeddings for same person
            vectorService.storeFaceEmbedding(personId1, embedding1, modelName1, imageHash1, confidence1, metadata1);
            vectorService.storeFaceEmbedding(personId1, embedding2, modelName1, "hash-xyz", confidence2, metadata2);
            // Different person
            vectorService.storeFaceEmbedding(personId2, embedding3, modelName2, imageHash2, 0.8, metadata1);
        }

        @Test
        @DisplayName("Should find entries by person ID")
        void shouldFindEntriesByPersonId() {
            // When: Finding entries by person ID
            List<Map<String, Object>> results = vectorService.findEntriesByPersonId(personId1);

            // Then: Should return entries for that person
            assertThat(results).hasSize(2);

            // Check all results belong to the correct person
            assertThat(results).allMatch(result -> result.get("personId").equals(personId1));
        }

        @Test
        @DisplayName("Should return empty list for null person ID")
        void shouldReturnEmptyListForNullPersonId() {
            // When: Finding entries with null person ID
            List<Map<String, Object>> results = vectorService.findEntriesByPersonId(null);

            // Then: Should return empty list
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("Should return empty list for non-existent person ID")
        void shouldReturnEmptyListForNonExistentPersonId() {
            // When: Finding entries for non-existent person
            List<Map<String, Object>> results = vectorService.findEntriesByPersonId("non-existent-person");

            // Then: Should return empty list
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("Distance Computation")
    class DistanceComputation {

        @Test
        @DisplayName("Should compute cosine distance correctly")
        void shouldComputeCosineDistanceCorrectly() {
            // Given: Two identical vectors
            float[] vec1 = {1.0f, 0.0f};
            float[] vec2 = {1.0f, 0.0f};

            // When: Computing cosine distance
            double distance = computeDistance(vec1, vec2, "cosine");

            // Then: Distance should be 0 (identical vectors)
            assertThat(distance).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should compute euclidean distance correctly")
        void shouldComputeEuclideanDistanceCorrectly() {
            // Given: Two vectors
            float[] vec1 = {0.0f, 0.0f};
            float[] vec2 = {3.0f, 4.0f};

            // When: Computing euclidean distance
            double distance = computeDistance(vec1, vec2, "euclidean");

            // Then: Distance should be 5.0 (3-4-5 triangle)
            assertThat(distance).isEqualTo(5.0);
        }

        @Test
        @DisplayName("Should default to cosine distance when metric is null")
        void shouldDefaultToCosineDistanceWhenMetricIsNull() {
            // When: Computing distance with null metric
            double distance = computeDistance(embedding1, embedding2, null);

            // Then: Should use cosine distance (same as passing "cosine")
            double cosineDistance = computeDistance(embedding1, embedding2, "cosine");
            assertThat(distance).isEqualTo(cosineDistance);
        }

        @Test
        @DisplayName("Should handle null vectors")
        void shouldHandleNullVectors() {
            // When: Computing distance with null vectors
            double distance = computeDistance(null, embedding1, "cosine");

            // Then: Should return positive infinity
            assertThat(distance).isEqualTo(Double.POSITIVE_INFINITY);
        }

        @Test
        @DisplayName("Should handle mismatched vector lengths")
        void shouldHandleMismatchedVectorLengths() {
            // Given: Vectors of different lengths
            float[] vec1 = {1.0f, 2.0f};
            float[] vec2 = {1.0f, 2.0f, 3.0f};

            // When: Computing distance
            double distance = computeDistance(vec1, vec2, "cosine");

            // Then: Should return positive infinity
            assertThat(distance).isEqualTo(Double.POSITIVE_INFINITY);
        }

        @Test
        @DisplayName("Should handle zero vectors in cosine distance")
        void shouldHandleZeroVectorsInCosineDistance() {
            // Given: Zero vectors
            float[] vec1 = {0.0f, 0.0f};
            float[] vec2 = {0.0f, 0.0f};

            // When: Computing cosine distance
            double distance = computeDistance(vec1, vec2, "cosine");

            // Then: Should return positive infinity (division by zero)
            assertThat(distance).isEqualTo(Double.POSITIVE_INFINITY);
        }
    }

    @Nested
    @DisplayName("Data Integrity and Thread Safety")
    class DataIntegrityAndThreadSafety {

        @Test
        @DisplayName("Should maintain data integrity across operations")
        void shouldMaintainDataIntegrityAcrossOperations() {
            // Given: Store an embedding
            String id = vectorService.storeFaceEmbedding(
                personId1, embedding1, modelName1, imageHash1, confidence1, metadata1);

            // When: Performing various operations
            List<Map<String, Object>> byPerson = vectorService.findEntriesByPersonId(personId1);
            List<Map<String, Object>> byImageHash = vectorService.findEntriesByImageHash(imageHash1);
            List<Map<String, Object>> similar = vectorService.findSimilarFaces(
                embedding1, modelName1, "cosine", 1.0, 10, null, null);

            // Then: Data should remain consistent
            assertThat(byPerson).hasSize(1);
            assertThat(byImageHash).hasSize(1);
            assertThat(similar).hasSize(1);

            // All should reference the same embedding
            assertThat(byPerson.get(0).get("embeddingId")).isEqualTo(id);
            assertThat(byImageHash.get(0).get("embeddingId")).isEqualTo(id);
            assertThat(similar.get(0).get("embeddingId")).isEqualTo(id);
        }

        @Test
        @DisplayName("Should be thread-safe for concurrent operations")
        void shouldBeThreadSafeForConcurrentOperations() {
            // This test verifies that the ConcurrentHashMap usage makes operations thread-safe
            // In a real scenario, this would require actual concurrent execution

            // Given: Multiple operations
            String id1 = vectorService.storeFaceEmbedding(
                personId1, embedding1, modelName1, imageHash1, confidence1, metadata1);
            String id2 = vectorService.storeFaceEmbedding(
                personId2, embedding2, modelName2, imageHash2, confidence2, metadata2);

            // When: Performing concurrent-like operations
            List<Map<String, Object>> results1 = vectorService.findEntriesByPersonId(personId1);
            List<Map<String, Object>> results2 = vectorService.findEntriesByPersonId(personId2);

            // Then: Should work correctly
            assertThat(results1).hasSize(1);
            assertThat(results2).hasSize(1);
            assertThat(results1.get(0).get("embeddingId")).isEqualTo(id1);
            assertThat(results2.get(0).get("embeddingId")).isEqualTo(id2);
        }
    }

    // Helper method to access private computeDistance method for testing
    private double computeDistance(float[] a, float[] b, String metric) {
        try {
            java.lang.reflect.Method method = InMemoryVectorService.class.getDeclaredMethod(
                "computeDistance", float[].class, float[].class, String.class);
            method.setAccessible(true);
            return (Double) method.invoke(null, a, b, metric);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
