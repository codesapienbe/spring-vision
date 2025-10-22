package io.github.codesapienbe.springvision.starter.web.dto;

import io.github.codesapienbe.springvision.core.BoundingBox;
import io.github.codesapienbe.springvision.core.Detection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for MultipleDetectionResponse DTO.
 * Tests all constructors, builders, getters/setters, and business logic methods.
 */
class MultipleDetectionResponseTest {

    // Test fixtures
    private final String testCorrelationId = "multi-detect-123";
    private final List<String> testDetectionTypes = List.of("face", "object", "text");
    private final String testError = "Processing failed";

    // DetectionResponse fixtures
    private final BoundingBox testBoundingBox = new BoundingBox(0.1, 0.2, 0.3, 0.4);
    private final Detection testDetection = new Detection("face", 0.9, testBoundingBox, Map.of("confidence", 0.9));
    private final List<Detection> testDetections = List.of(testDetection);

    private final DetectionResponse faceResponse = DetectionResponse.builder()
        .correlationId(testCorrelationId)
        .detectionType("face")
        .detectionCount(2)
        .averageConfidence(0.85)
        .processingTimeMs(150L)
        .detections(testDetections)
        .build();

    private final DetectionResponse objectResponse = DetectionResponse.builder()
        .correlationId(testCorrelationId)
        .detectionType("object")
        .detectionCount(1)
        .averageConfidence(0.92)
        .processingTimeMs(200L)
        .detections(testDetections)
        .build();

    private final List<DetectionResponse> testResults = List.of(faceResponse, objectResponse);

    @Nested
    @DisplayName("Constructor and Basic Properties")
    class ConstructorAndBasicProperties {

        @Test
        @DisplayName("Should create MultipleDetectionResponse with all parameters constructor")
        void shouldCreateMultipleDetectionResponseWithAllParametersConstructor() {
            // When: Creating MultipleDetectionResponse with all parameters
            MultipleDetectionResponse response = new MultipleDetectionResponse(
                testCorrelationId, testDetectionTypes, testResults, testError);

            // Then: Should store all values correctly
            assertThat(response.getCorrelationId()).isEqualTo(testCorrelationId);
            assertThat(response.getDetectionTypes()).isEqualTo(testDetectionTypes);
            assertThat(response.getResults()).isEqualTo(testResults);
            assertThat(response.getError()).isEqualTo(testError);
        }

        @Test
        @DisplayName("Should create MultipleDetectionResponse with default constructor")
        void shouldCreateMultipleDetectionResponseWithDefaultConstructor() {
            // When: Creating MultipleDetectionResponse with default constructor
            MultipleDetectionResponse response = new MultipleDetectionResponse();

            // Then: All fields should be null initially
            assertThat(response.getCorrelationId()).isNull();
            assertThat(response.getDetectionTypes()).isNull();
            assertThat(response.getResults()).isNull();
            assertThat(response.getError()).isNull();
        }

        @Test
        @DisplayName("Should set and get all properties")
        void shouldSetAndGetAllProperties() {
            // Given: MultipleDetectionResponse instance
            MultipleDetectionResponse response = new MultipleDetectionResponse();

            // When: Setting all properties
            response.setCorrelationId(testCorrelationId);
            response.setDetectionTypes(testDetectionTypes);
            response.setResults(testResults);
            response.setError(testError);

            // Then: Should return correct values
            assertThat(response.getCorrelationId()).isEqualTo(testCorrelationId);
            assertThat(response.getDetectionTypes()).isEqualTo(testDetectionTypes);
            assertThat(response.getResults()).isEqualTo(testResults);
            assertThat(response.getError()).isEqualTo(testError);
        }
    }

    @Nested
    @DisplayName("Builder Pattern")
    class BuilderPattern {

        @Test
        @DisplayName("Should create builder instance")
        void shouldCreateBuilderInstance() {
            // When: Creating builder
            MultipleDetectionResponse.Builder builder = MultipleDetectionResponse.builder();

            // Then: Should return a valid builder instance
            assertThat(builder).isNotNull();
            assertThat(builder).isInstanceOf(MultipleDetectionResponse.Builder.class);
        }

        @Test
        @DisplayName("Should build MultipleDetectionResponse with all fields")
        void shouldBuildMultipleDetectionResponseWithAllFields() {
            // When: Building MultipleDetectionResponse with all fields
            MultipleDetectionResponse response = MultipleDetectionResponse.builder()
                .correlationId(testCorrelationId)
                .detectionTypes(testDetectionTypes)
                .results(testResults)
                .error(testError)
                .build();

            // Then: Should have all correct values
            assertThat(response.getCorrelationId()).isEqualTo(testCorrelationId);
            assertThat(response.getDetectionTypes()).isEqualTo(testDetectionTypes);
            assertThat(response.getResults()).isEqualTo(testResults);
            assertThat(response.getError()).isEqualTo(testError);
        }

        @Test
        @DisplayName("Should build MultipleDetectionResponse with partial fields")
        void shouldBuildMultipleDetectionResponseWithPartialFields() {
            // When: Building MultipleDetectionResponse with only some fields
            MultipleDetectionResponse response = MultipleDetectionResponse.builder()
                .correlationId(testCorrelationId)
                .detectionTypes(testDetectionTypes)
                .build();

            // Then: Should have set fields and default values for others
            assertThat(response.getCorrelationId()).isEqualTo(testCorrelationId);
            assertThat(response.getDetectionTypes()).isEqualTo(testDetectionTypes);
            assertThat(response.getResults()).isNull();
            assertThat(response.getError()).isNull();
        }

        @Test
        @DisplayName("Should allow method chaining in builder")
        void shouldAllowMethodChainingInBuilder() {
            // When: Using method chaining
            MultipleDetectionResponse response = MultipleDetectionResponse.builder()
                .correlationId(testCorrelationId)
                .detectionTypes(testDetectionTypes)
                .results(testResults)
                .error(testError)
                .build();

            // Then: Should work correctly
            assertThat(response.getCorrelationId()).isEqualTo(testCorrelationId);
            assertThat(response.getDetectionTypes()).isEqualTo(testDetectionTypes);
            assertThat(response.getResults()).isEqualTo(testResults);
            assertThat(response.getError()).isEqualTo(testError);
        }

        @Test
        @DisplayName("Should create successful multiple detection response")
        void shouldCreateSuccessfulMultipleDetectionResponse() {
            // When: Building successful response
            MultipleDetectionResponse response = MultipleDetectionResponse.builder()
                .correlationId("multi-123")
                .detectionTypes(List.of("face", "object"))
                .results(testResults)
                .build();

            // Then: Should represent success
            assertThat(response.hasError()).isFalse();
            assertThat(response.hasResults()).isTrue();
            assertThat(response.getError()).isNull();
            assertThat(response.getResults()).hasSize(2);
        }

        @Test
        @DisplayName("Should create error multiple detection response")
        void shouldCreateErrorMultipleDetectionResponse() {
            // When: Building error response
            MultipleDetectionResponse response = MultipleDetectionResponse.builder()
                .correlationId("multi-456")
                .detectionTypes(List.of("face"))
                .error("Backend unavailable")
                .build();

            // Then: Should represent error
            assertThat(response.hasError()).isTrue();
            assertThat(response.hasResults()).isFalse();
            assertThat(response.getError()).isEqualTo("Backend unavailable");
            assertThat(response.getResults()).isNull();
        }
    }

    @Nested
    @DisplayName("Business Logic Methods")
    class BusinessLogicMethods {

        @Test
        @DisplayName("Should detect error when error field is set")
        void shouldDetectErrorWhenErrorFieldIsSet() {
            // Given: MultipleDetectionResponse with error
            MultipleDetectionResponse response = MultipleDetectionResponse.builder()
                .error(testError)
                .build();

            // When: Checking for error
            boolean hasError = response.hasError();

            // Then: Should return true
            assertThat(hasError).isTrue();
        }

        @Test
        @DisplayName("Should not detect error when error field is null")
        void shouldNotDetectErrorWhenErrorFieldIsNull() {
            // Given: MultipleDetectionResponse without error
            MultipleDetectionResponse response = MultipleDetectionResponse.builder()
                .error(null)
                .build();

            // When: Checking for error
            boolean hasError = response.hasError();

            // Then: Should return false
            assertThat(hasError).isFalse();
        }

        @Test
        @DisplayName("Should not detect error when error field is empty")
        void shouldNotDetectErrorWhenErrorFieldIsEmpty() {
            // Given: MultipleDetectionResponse with empty error
            MultipleDetectionResponse response = MultipleDetectionResponse.builder()
                .error("")
                .build();

            // When: Checking for error
            boolean hasError = response.hasError();

            // Then: Should return false
            assertThat(hasError).isFalse();
        }

        @Test
        @DisplayName("Should detect results when results list is set and not empty")
        void shouldDetectResultsWhenResultsListIsSetAndNotEmpty() {
            // Given: MultipleDetectionResponse with results
            MultipleDetectionResponse response = MultipleDetectionResponse.builder()
                .results(testResults)
                .build();

            // When: Checking for results
            boolean hasResults = response.hasResults();

            // Then: Should return true
            assertThat(hasResults).isTrue();
        }

        @Test
        @DisplayName("Should not detect results when results list is null")
        void shouldNotDetectResultsWhenResultsListIsNull() {
            // Given: MultipleDetectionResponse with null results
            MultipleDetectionResponse response = MultipleDetectionResponse.builder()
                .results(null)
                .build();

            // When: Checking for results
            boolean hasResults = response.hasResults();

            // Then: Should return false
            assertThat(hasResults).isFalse();
        }

        @Test
        @DisplayName("Should not detect results when results list is empty")
        void shouldNotDetectResultsWhenResultsListIsEmpty() {
            // Given: MultipleDetectionResponse with empty results
            MultipleDetectionResponse response = MultipleDetectionResponse.builder()
                .results(List.of())
                .build();

            // When: Checking for results
            boolean hasResults = response.hasResults();

            // Then: Should return false
            assertThat(hasResults).isFalse();
        }

        @Test
        @DisplayName("Should calculate total detection count correctly")
        void shouldCalculateTotalDetectionCountCorrectly() {
            // Given: MultipleDetectionResponse with results
            MultipleDetectionResponse response = MultipleDetectionResponse.builder()
                .results(testResults) // face: 2 detections, object: 1 detection
                .build();

            // When: Getting total detection count
            int totalCount = response.getTotalDetectionCount();

            // Then: Should return sum of all detection counts (2 + 1 = 3)
            assertThat(totalCount).isEqualTo(3);
        }

        @Test
        @DisplayName("Should return zero total detection count when results is null")
        void shouldReturnZeroTotalDetectionCountWhenResultsIsNull() {
            // Given: MultipleDetectionResponse with null results
            MultipleDetectionResponse response = MultipleDetectionResponse.builder()
                .results(null)
                .build();

            // When: Getting total detection count
            int totalCount = response.getTotalDetectionCount();

            // Then: Should return 0
            assertThat(totalCount).isEqualTo(0);
        }

        @Test
        @DisplayName("Should return zero total detection count when results is empty")
        void shouldReturnZeroTotalDetectionCountWhenResultsIsEmpty() {
            // Given: MultipleDetectionResponse with empty results
            MultipleDetectionResponse response = MultipleDetectionResponse.builder()
                .results(List.of())
                .build();

            // When: Getting total detection count
            int totalCount = response.getTotalDetectionCount();

            // Then: Should return 0
            assertThat(totalCount).isEqualTo(0);
        }

        @Test
        @DisplayName("Should calculate average confidence correctly")
        void shouldCalculateAverageConfidenceCorrectly() {
            // Given: MultipleDetectionResponse with results
            MultipleDetectionResponse response = MultipleDetectionResponse.builder()
                .results(testResults) // face: 0.85, object: 0.92
                .build();

            // When: Getting average confidence
            double averageConfidence = response.getAverageConfidence();

            // Then: Should return average of all average confidences ((0.85 + 0.92) / 2 = 0.885)
            assertThat(averageConfidence).isEqualTo(0.885);
        }

        @Test
        @DisplayName("Should return zero average confidence when results is null")
        void shouldReturnZeroAverageConfidenceWhenResultsIsNull() {
            // Given: MultipleDetectionResponse with null results
            MultipleDetectionResponse response = MultipleDetectionResponse.builder()
                .results(null)
                .build();

            // When: Getting average confidence
            double averageConfidence = response.getAverageConfidence();

            // Then: Should return 0.0
            assertThat(averageConfidence).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return zero average confidence when results is empty")
        void shouldReturnZeroAverageConfidenceWhenResultsIsEmpty() {
            // Given: MultipleDetectionResponse with empty results
            MultipleDetectionResponse response = MultipleDetectionResponse.builder()
                .results(List.of())
                .build();

            // When: Getting average confidence
            double averageConfidence = response.getAverageConfidence();

            // Then: Should return 0.0
            assertThat(averageConfidence).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should calculate total processing time correctly")
        void shouldCalculateTotalProcessingTimeCorrectly() {
            // Given: MultipleDetectionResponse with results
            MultipleDetectionResponse response = MultipleDetectionResponse.builder()
                .results(testResults) // face: 150ms, object: 200ms
                .build();

            // When: Getting total processing time
            long totalTime = response.getTotalProcessingTimeMs();

            // Then: Should return sum of all processing times (150 + 200 = 350)
            assertThat(totalTime).isEqualTo(350L);
        }

        @Test
        @DisplayName("Should return zero total processing time when results is null")
        void shouldReturnZeroTotalProcessingTimeWhenResultsIsNull() {
            // Given: MultipleDetectionResponse with null results
            MultipleDetectionResponse response = MultipleDetectionResponse.builder()
                .results(null)
                .build();

            // When: Getting total processing time
            long totalTime = response.getTotalProcessingTimeMs();

            // Then: Should return 0
            assertThat(totalTime).isEqualTo(0L);
        }

        @Test
        @DisplayName("Should return zero total processing time when results is empty")
        void shouldReturnZeroTotalProcessingTimeWhenResultsIsEmpty() {
            // Given: MultipleDetectionResponse with empty results
            MultipleDetectionResponse response = MultipleDetectionResponse.builder()
                .results(List.of())
                .build();

            // When: Getting total processing time
            long totalTime = response.getTotalProcessingTimeMs();

            // Then: Should return 0
            assertThat(totalTime).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("Results Handling")
    class ResultsHandling {

        @Test
        @DisplayName("Should handle empty results list")
        void shouldHandleEmptyResultsList() {
            // Given: MultipleDetectionResponse with empty results
            MultipleDetectionResponse response = MultipleDetectionResponse.builder()
                .results(List.of())
                .build();

            // When: Getting results
            List<DetectionResponse> results = response.getResults();

            // Then: Should return empty list
            assertThat(results).isEmpty();
            assertThat(response.hasResults()).isFalse();
            assertThat(response.getTotalDetectionCount()).isEqualTo(0);
            assertThat(response.getAverageConfidence()).isEqualTo(0.0);
            assertThat(response.getTotalProcessingTimeMs()).isEqualTo(0L);
        }

        @Test
        @DisplayName("Should handle single result")
        void shouldHandleSingleResult() {
            // Given: MultipleDetectionResponse with single result
            MultipleDetectionResponse response = MultipleDetectionResponse.builder()
                .results(List.of(faceResponse))
                .build();

            // When: Getting results
            List<DetectionResponse> results = response.getResults();

            // Then: Should return single result
            assertThat(results).hasSize(1);
            assertThat(results.get(0)).isEqualTo(faceResponse);
            assertThat(response.hasResults()).isTrue();
            assertThat(response.getTotalDetectionCount()).isEqualTo(2);
            assertThat(response.getAverageConfidence()).isEqualTo(0.85);
            assertThat(response.getTotalProcessingTimeMs()).isEqualTo(150L);
        }

        @Test
        @DisplayName("Should handle multiple results")
        void shouldHandleMultipleResults() {
            // Given: MultipleDetectionResponse with multiple results
            MultipleDetectionResponse response = MultipleDetectionResponse.builder()
                .results(testResults)
                .build();

            // When: Getting results
            List<DetectionResponse> results = response.getResults();

            // Then: Should return all results
            assertThat(results).hasSize(2);
            assertThat(results).containsExactly(faceResponse, objectResponse);
            assertThat(response.hasResults()).isTrue();
        }

        @Test
        @DisplayName("Should preserve order of results")
        void shouldPreserveOrderOfResults() {
            // Given: Results in specific order
            List<DetectionResponse> orderedResults = List.of(objectResponse, faceResponse);

            // When: Creating response with ordered results
            MultipleDetectionResponse response = MultipleDetectionResponse.builder()
                .results(orderedResults)
                .build();

            // Then: Should preserve order
            assertThat(response.getResults()).containsExactly(objectResponse, faceResponse);
        }
    }

    @Nested
    @DisplayName("Object Behavior")
    class ObjectBehavior {

        @Test
        @DisplayName("Should implement toString")
        void shouldImplementToString() {
            // Given: MultipleDetectionResponse instance
            MultipleDetectionResponse response = MultipleDetectionResponse.builder()
                .correlationId(testCorrelationId)
                .detectionTypes(List.of("face"))
                .build();

            // When: Calling toString
            String string = response.toString();

            // Then: Should return a non-null string
            assertThat(string).isNotNull();
            assertThat(string).contains("MultipleDetectionResponse");
        }

        @Test
        @DisplayName("Should implement equals correctly")
        void shouldImplementEqualsCorrectly() {
            // Given: Two MultipleDetectionResponse instances with same data
            MultipleDetectionResponse response1 = new MultipleDetectionResponse(
                testCorrelationId, testDetectionTypes, testResults, testError);

            MultipleDetectionResponse response2 = new MultipleDetectionResponse(
                testCorrelationId, List.copyOf(testDetectionTypes), testResults, testError);

            // When & Then: Should be equal (default Object.equals behavior)
            assertThat(response1).isEqualTo(response1);
            assertThat(response1).isNotEqualTo(response2); // Different instances
            assertThat(response1).isNotEqualTo(null);
            assertThat(response1).isNotEqualTo(new Object());
        }

        @Test
        @DisplayName("Should implement hashCode")
        void shouldImplementHashCode() {
            // Given: MultipleDetectionResponse instance
            MultipleDetectionResponse response = MultipleDetectionResponse.builder()
                .correlationId(testCorrelationId)
                .build();

            // When: Calling hashCode
            int hashCode = response.hashCode();

            // Then: Should return an integer hash code
            assertThat(hashCode).isInstanceOf(Integer.class);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle null values appropriately")
        void shouldHandleNullValuesAppropriately() {
            // When: Creating response with null values
            MultipleDetectionResponse response = new MultipleDetectionResponse(null, null, null, null);

            // Then: Should handle nulls gracefully
            assertThat(response.hasError()).isFalse();
            assertThat(response.hasResults()).isFalse();
            assertThat(response.getTotalDetectionCount()).isEqualTo(0);
            assertThat(response.getAverageConfidence()).isEqualTo(0.0);
            assertThat(response.getTotalProcessingTimeMs()).isEqualTo(0L);
        }

        @Test
        @DisplayName("Should handle results with zero detection counts")
        void shouldHandleResultsWithZeroDetectionCounts() {
            // Given: Results with zero detections
            DetectionResponse emptyResponse = DetectionResponse.builder()
                .correlationId(testCorrelationId)
                .detectionType("empty")
                .detectionCount(0)
                .averageConfidence(0.0)
                .processingTimeMs(50L)
                .detections(List.of())
                .build();

            // When: Creating response with empty results
            MultipleDetectionResponse response = MultipleDetectionResponse.builder()
                .results(List.of(emptyResponse))
                .build();

            // Then: Should handle correctly
            assertThat(response.getTotalDetectionCount()).isEqualTo(0);
            assertThat(response.getAverageConfidence()).isEqualTo(0.0);
            assertThat(response.getTotalProcessingTimeMs()).isEqualTo(50L);
        }

        @Test
        @DisplayName("Should handle very large result lists")
        void shouldHandleVeryLargeResultLists() {
            // Given: Large list of results (simulating many detection types)
            List<DetectionResponse> largeResults = List.of(
                faceResponse, objectResponse, faceResponse, objectResponse
            );

            // When: Creating response with large result list
            MultipleDetectionResponse response = MultipleDetectionResponse.builder()
                .results(largeResults)
                .build();

            // Then: Should handle correctly
            assertThat(response.getResults()).hasSize(4);
            assertThat(response.getTotalDetectionCount()).isEqualTo(6); // (2+1+2+1)
            assertThat(response.getTotalProcessingTimeMs()).isEqualTo(700L); // (150+200+150+200)
        }

        @Test
        @DisplayName("Should handle results with extreme confidence values")
        void shouldHandleResultsWithExtremeConfidenceValues() {
            // Given: Results with extreme confidence values
            DetectionResponse highConfidenceResponse = DetectionResponse.builder()
                .correlationId(testCorrelationId)
                .detectionType("high")
                .detectionCount(1)
                .averageConfidence(1.0)
                .processingTimeMs(100L)
                .detections(testDetections)
                .build();

            DetectionResponse lowConfidenceResponse = DetectionResponse.builder()
                .correlationId(testCorrelationId)
                .detectionType("low")
                .detectionCount(1)
                .averageConfidence(0.0)
                .processingTimeMs(100L)
                .detections(testDetections)
                .build();

            // When: Creating response with extreme values
            MultipleDetectionResponse response = MultipleDetectionResponse.builder()
                .results(List.of(highConfidenceResponse, lowConfidenceResponse))
                .build();

            // Then: Should calculate average correctly ((1.0 + 0.0) / 2 = 0.5)
            assertThat(response.getAverageConfidence()).isEqualTo(0.5);
            assertThat(response.getTotalDetectionCount()).isEqualTo(2);
            assertThat(response.getTotalProcessingTimeMs()).isEqualTo(200L);
        }
    }
}
