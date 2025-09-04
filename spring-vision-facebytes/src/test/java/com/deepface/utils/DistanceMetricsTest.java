package com.deepface.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DistanceMetrics utility class.
 * Tests all distance calculation methods: cosine, euclidean, and euclidean L2.
 */
@DisplayName("Distance Metrics Tests")
class DistanceMetricsTest {

    @Test
    @DisplayName("Cosine distance should return 0 for identical vectors")
    void cosineDistance_IdenticalVectors_ReturnsZero() {
        double[] vec1 = {1.0, 2.0, 3.0, 4.0, 5.0};
        double[] vec2 = {1.0, 2.0, 3.0, 4.0, 5.0};
        
        double distance = DistanceMetrics.cosineDistance(vec1, vec2);
        
        assertEquals(0.0, distance, 1e-10, "Cosine distance should be 0 for identical vectors");
    }

    @Test
    @DisplayName("Cosine distance should return 1 for orthogonal vectors")
    void cosineDistance_OrthogonalVectors_ReturnsOne() {
        double[] vec1 = {1.0, 0.0, 0.0};
        double[] vec2 = {0.0, 1.0, 0.0};
        
        double distance = DistanceMetrics.cosineDistance(vec1, vec2);
        
        assertEquals(1.0, distance, 1e-10, "Cosine distance should be 1 for orthogonal vectors");
    }

    @Test
    @DisplayName("Cosine distance should return 1 for opposite vectors")
    void cosineDistance_OppositeVectors_ReturnsOne() {
        double[] vec1 = {1.0, 2.0, 3.0};
        double[] vec2 = {-1.0, -2.0, -3.0};
        
        double distance = DistanceMetrics.cosineDistance(vec1, vec2);
        
        // For opposite vectors, cosine similarity is -1, so cosine distance is 1 - (-1) = 2
        assertEquals(2.0, distance, 1e-10, "Cosine distance should be 2 for opposite vectors");
    }

    @Test
    @DisplayName("Cosine distance should handle unit vectors correctly")
    void cosineDistance_UnitVectors_ReturnsExpectedValue() {
        double[] vec1 = {1.0, 0.0, 0.0};
        double[] vec2 = {0.5, 0.5, 0.7071067811865476}; // Unit vector at 45 degrees
        
        double distance = DistanceMetrics.cosineDistance(vec1, vec2);
        
        // Expected: 1 - cos(45°) = 1 - 0.7071067811865476 ≈ 0.2928932188134524
        // But the second vector is not actually a unit vector, let me recalculate
        // Magnitude of vec2: sqrt(0.5² + 0.5² + 0.7071067811865476²) = sqrt(0.25 + 0.25 + 0.5) = sqrt(1) = 1
        // Dot product: 1.0 * 0.5 + 0.0 * 0.5 + 0.0 * 0.7071067811865476 = 0.5
        // Cosine similarity: 0.5 / (1 * 1) = 0.5
        // Cosine distance: 1 - 0.5 = 0.5
        assertEquals(0.5, distance, 1e-10, "Cosine distance should match expected value");
    }

    @Test
    @DisplayName("Euclidean distance should return 0 for identical vectors")
    void euclideanDistance_IdenticalVectors_ReturnsZero() {
        double[] vec1 = {1.0, 2.0, 3.0, 4.0, 5.0};
        double[] vec2 = {1.0, 2.0, 3.0, 4.0, 5.0};
        
        double distance = DistanceMetrics.euclideanDistance(vec1, vec2);
        
        assertEquals(0.0, distance, 1e-10, "Euclidean distance should be 0 for identical vectors");
    }

    @Test
    @DisplayName("Euclidean distance should return correct value for simple case")
    void euclideanDistance_SimpleCase_ReturnsCorrectValue() {
        double[] vec1 = {0.0, 0.0, 0.0};
        double[] vec2 = {3.0, 4.0, 0.0};
        
        double distance = DistanceMetrics.euclideanDistance(vec1, vec2);
        
        assertEquals(5.0, distance, 1e-10, "Euclidean distance should be 5 for 3-4-5 triangle");
    }

    @Test
    @DisplayName("Euclidean distance should handle negative values")
    void euclideanDistance_NegativeValues_ReturnsCorrectValue() {
        double[] vec1 = {1.0, 2.0, 3.0};
        double[] vec2 = {-1.0, -2.0, -3.0};
        
        double distance = DistanceMetrics.euclideanDistance(vec1, vec2);
        
        // Expected: sqrt((1-(-1))² + (2-(-2))² + (3-(-3))²) = sqrt(4 + 16 + 36) = sqrt(56) ≈ 7.483314773547883
        assertEquals(7.483314773547883, distance, 1e-10, "Euclidean distance should handle negative values correctly");
    }

    @Test
    @DisplayName("Euclidean L2 distance should be same as regular euclidean distance")
    void euclideanL2Distance_ShouldMatchEuclideanDistance() {
        double[] vec1 = {1.0, 2.0, 3.0, 4.0};
        double[] vec2 = {5.0, 6.0, 7.0, 8.0};
        
        double euclidean = DistanceMetrics.euclideanDistance(vec1, vec2);
        double euclideanL2 = DistanceMetrics.euclideanL2Distance(vec1, vec2);
        
        assertEquals(euclidean, euclideanL2, 1e-10, "Euclidean L2 distance should match regular euclidean distance");
    }

    @Test
    @DisplayName("Distance metrics should handle single-element vectors")
    void distanceMetrics_SingleElementVectors_WorkCorrectly() {
        double[] vec1 = {5.0};
        double[] vec2 = {8.0};
        
        double cosine = DistanceMetrics.cosineDistance(vec1, vec2);
        double euclidean = DistanceMetrics.euclideanDistance(vec1, vec2);
        
        assertEquals(0.0, cosine, 1e-10, "Cosine distance should be 0 for single-element vectors");
        assertEquals(3.0, euclidean, 1e-10, "Euclidean distance should be 3 for single-element vectors");
    }

    @Test
    @DisplayName("Distance metrics should handle large vectors")
    void distanceMetrics_LargeVectors_WorkCorrectly() {
        int size = 1000;
        double[] vec1 = new double[size];
        double[] vec2 = new double[size];
        
        for (int i = 0; i < size; i++) {
            vec1[i] = i;
            vec2[i] = i + 1;
        }
        
        double cosine = DistanceMetrics.cosineDistance(vec1, vec2);
        double euclidean = DistanceMetrics.euclideanDistance(vec1, vec2);
        
        assertTrue(cosine >= 0.0 && cosine <= 1.0, "Cosine distance should be in [0,1] range");
        assertTrue(euclidean >= 0.0, "Euclidean distance should be non-negative");
        assertEquals(Math.sqrt(size), euclidean, 1e-10, "Euclidean distance should be sqrt(n) for unit difference vectors");
    }

    @Test
    @DisplayName("Distance metrics should throw exception for null vectors")
    void distanceMetrics_NullVectors_ThrowsException() {
        double[] vec1 = {1.0, 2.0, 3.0};
        double[] vec2 = null;
        
        assertThrows(IllegalArgumentException.class, () -> 
            DistanceMetrics.cosineDistance(vec1, vec2), 
            "Should throw exception for null second vector"
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            DistanceMetrics.euclideanDistance(vec1, vec2), 
            "Should throw exception for null second vector"
        );
    }

    @Test
    @DisplayName("Distance metrics should throw exception for different length vectors")
    void distanceMetrics_DifferentLengthVectors_ThrowsException() {
        double[] vec1 = {1.0, 2.0, 3.0};
        double[] vec2 = {1.0, 2.0};
        
        assertThrows(IllegalArgumentException.class, () -> 
            DistanceMetrics.cosineDistance(vec1, vec2), 
            "Should throw exception for different length vectors"
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            DistanceMetrics.euclideanDistance(vec1, vec2), 
            "Should throw exception for different length vectors"
        );
    }

    @Test
    @DisplayName("Distance metrics should handle zero vectors correctly")
    void distanceMetrics_ZeroVectors_HandleCorrectly() {
        double[] vec1 = {0.0, 0.0, 0.0};
        double[] vec2 = {0.0, 0.0, 0.0};
        
        double cosine = DistanceMetrics.cosineDistance(vec1, vec2);
        double euclidean = DistanceMetrics.euclideanDistance(vec1, vec2);
        
        // For zero vectors, cosine distance should return 1 (maximum distance)
        // as they have no direction to compare
        assertEquals(1.0, cosine, 1e-10, "Cosine distance should be 1 for zero vectors");
        assertEquals(0.0, euclidean, 1e-10, "Euclidean distance should be 0 for zero vectors");
    }
} 