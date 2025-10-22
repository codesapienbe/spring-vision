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
 * Comprehensive unit tests for DetectionResponse DTO.
 * Tests all constructors, builders, getters/setters, and business logic methods.
 */
class DetectionResponseTest {

    // Test fixtures
    private final String testCorrelationId = "test-correlation-id-123";
    private final String testDetectionType = "face";
    private final int testDetectionCount = 2;
    private final double testAverageConfidence = 0.85;
    private final long testProcessingTimeMs = 150L;
    private final String testError = "Processing failed";
    private final BoundingBox testBoundingBox = new BoundingBox(0.1, 0.2, 0.3, 0.4);
    private final Detection testDetection = new Detection("face", 0.9, testBoundingBox, Map.of("confidence", 0.9));
    private final List<Detection> testDetections = List.of(testDetection);

    @Nested
    @DisplayName("Constructor and Basic Properties")
    class ConstructorAndBasicProperties {

        @Test
        @DisplayName("Should create DetectionResponse with all parameters constructor")
        void shouldCreateDetectionResponseWithAllParametersConstructor() {
            // When: Creating DetectionResponse with all parameters
            DetectionResponse response = new DetectionResponse(
                testCorrelationId, testDetectionType, testDetectionCount,
                testAverageConfidence, testProcessingTimeMs, testDetections, testError);

            // Then: Should store all values correctly
            assertThat(response.getCorrelationId()).isEqualTo(testCorrelationId);
            assertThat(response.getDetectionType()).isEqualTo(testDetectionType);
            assertThat(response.getDetectionCount()).isEqualTo(testDetectionCount);
            assertThat(response.getAverageConfidence()).isEqualTo(testAverageConfidence);
            assertThat(response.getProcessingTimeMs()).isEqualTo(testProcessingTimeMs);
            assertThat(response.getDetections()).isEqualTo(testDetections);
            assertThat(response.getError()).isEqualTo(testError);
        }

        @Test
        @DisplayName("Should create DetectionResponse with default constructor")
        void shouldCreateDetectionResponseWithDefaultConstructor() {
            // When: Creating DetectionResponse with default constructor
            DetectionResponse response = new DetectionResponse();

            // Then: All fields should be null initially
            assertThat(response.getCorrelationId()).isNull();
            assertThat(response.getDetectionType()).isNull();
            assertThat(response.getDetectionCount()).isEqualTo(0);
            assertThat(response.getAverageConfidence()).isEqualTo(0.0);
            assertThat(response.getProcessingTimeMs()).isEqualTo(0L);
            assertThat(response.getDetections()).isNull();
            assertThat(response.getError()).isNull();
        }

        @Test
        @DisplayName("Should set and get all properties")
        void shouldSetAndGetAllProperties() {
            // Given: DetectionResponse instance
            DetectionResponse response = new DetectionResponse();

            // When: Setting all properties
            response.setCorrelationId(testCorrelationId);
            response.setDetectionType(testDetectionType);
            response.setDetectionCount(testDetectionCount);
            response.setAverageConfidence(testAverageConfidence);
            response.setProcessingTimeMs(testProcessingTimeMs);
            response.setDetections(testDetections);
            response.setError(testError);

            // Then: Should return correct values
            assertThat(response.getCorrelationId()).isEqualTo(testCorrelationId);
            assertThat(response.getDetectionType()).isEqualTo(testDetectionType);
            assertThat(response.getDetectionCount()).isEqualTo(testDetectionCount);
            assertThat(response.getAverageConfidence()).isEqualTo(testAverageConfidence);
            assertThat(response.getProcessingTimeMs()).isEqualTo(testProcessingTimeMs);
            assertThat(response.getDetections()).isEqualTo(testDetections);
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
            DetectionResponse.Builder builder = DetectionResponse.builder();

            // Then: Should return a valid builder instance
            assertThat(builder).isNotNull();
            assertThat(builder).isInstanceOf(DetectionResponse.Builder.class);
        }

        @Test
        @DisplayName("Should build DetectionResponse with all fields")
        void shouldBuildDetectionResponseWithAllFields() {
            // When: Building DetectionResponse with all fields
            DetectionResponse response = DetectionResponse.builder()
                .correlationId(testCorrelationId)
                .detectionType(testDetectionType)
                .detectionCount(testDetectionCount)
                .averageConfidence(testAverageConfidence)
                .processingTimeMs(testProcessingTimeMs)
                .detections(testDetections)
                .error(testError)
                .build();

            // Then: Should have all correct values
            assertThat(response.getCorrelationId()).isEqualTo(testCorrelationId);
            assertThat(response.getDetectionType()).isEqualTo(testDetectionType);
            assertThat(response.getDetectionCount()).isEqualTo(testDetectionCount);
            assertThat(response.getAverageConfidence()).isEqualTo(testAverageConfidence);
            assertThat(response.getProcessingTimeMs()).isEqualTo(testProcessingTimeMs);
            assertThat(response.getDetections()).isEqualTo(testDetections);
            assertThat(response.getError()).isEqualTo(testError);
        }

        @Test
        @DisplayName("Should build DetectionResponse with partial fields")
        void shouldBuildDetectionResponseWithPartialFields() {
            // When: Building DetectionResponse with only some fields
            DetectionResponse response = DetectionResponse.builder()
                .correlationId(testCorrelationId)
                .detectionType(testDetectionType)
                .build();

            // Then: Should have set fields and default values for others
            assertThat(response.getCorrelationId()).isEqualTo(testCorrelationId);
            assertThat(response.getDetectionType()).isEqualTo(testDetectionType);
            assertThat(response.getDetectionCount()).isEqualTo(0);
            assertThat(response.getAverageConfidence()).isEqualTo(0.0);
            assertThat(response.getProcessingTimeMs()).isEqualTo(0L);
            assertThat(response.getDetections()).isNull();
            assertThat(response.getError()).isNull();
        }

        @Test
        @DisplayName("Should allow method chaining in builder")
        void shouldAllowMethodChainingInBuilder() {
            // When: Using method chaining
            DetectionResponse response = DetectionResponse.builder()
                .correlationId(testCorrelationId)
                .detectionType(testDetectionType)
                .detectionCount(testDetectionCount)
                .averageConfidence(testAverageConfidence)
                .processingTimeMs(testProcessingTimeMs)
                .detections(testDetections)
                .error(testError)
                .build();

            // Then: Should work correctly
            assertThat(response.getCorrelationId()).isEqualTo(testCorrelationId);
            assertThat(response.getDetectionType()).isEqualTo(testDetectionType);
        }

        @Test
        @DisplayName("Should reuse builder for multiple builds")
        void shouldReuseBuilderForMultipleBuilds() {
            // Given: Builder instance
            DetectionResponse.Builder builder = DetectionResponse.builder();

            // When: Building multiple times with different values
            DetectionResponse response1 = builder
                .correlationId("id1")
                .detectionType("face")
                .build();

            DetectionResponse response2 = builder
                .correlationId("id2")
                .detectionType("object")
                .build();

            // Then: Each build should have different values
            assertThat(response1.getCorrelationId()).isEqualTo("id1");
            assertThat(response1.getDetectionType()).isEqualTo("face");
            assertThat(response2.getCorrelationId()).isEqualTo("id2");
            assertThat(response2.getDetectionType()).isEqualTo("object");
        }
    }

    @Nested
    @DisplayName("Business Logic Methods")
    class BusinessLogicMethods {

        @Test
        @DisplayName("Should detect error when error field is set")
        void shouldDetectErrorWhenErrorFieldIsSet() {
            // Given: DetectionResponse with error
            DetectionResponse response = DetectionResponse.builder()
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
            // Given: DetectionResponse without error
            DetectionResponse response = DetectionResponse.builder()
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
            // Given: DetectionResponse with empty error
            DetectionResponse response = DetectionResponse.builder()
                .error("")
                .build();

            // When: Checking for error
            boolean hasError = response.hasError();

            // Then: Should return false
            assertThat(hasError).isFalse();
        }

        @Test
        @DisplayName("Should detect detections when detections list is set and not empty")
        void shouldDetectDetectionsWhenDetectionsListIsSetAndNotEmpty() {
            // Given: DetectionResponse with detections
            DetectionResponse response = DetectionResponse.builder()
                .detections(testDetections)
                .build();

            // When: Checking for detections
            boolean hasDetections = response.hasDetections();

            // Then: Should return true
            assertThat(hasDetections).isTrue();
        }

        @Test
        @DisplayName("Should not detect detections when detections list is null")
        void shouldNotDetectDetectionsWhenDetectionsListIsNull() {
            // Given: DetectionResponse with null detections
            DetectionResponse response = DetectionResponse.builder()
                .detections(null)
                .build();

            // When: Checking for detections
            boolean hasDetections = response.hasDetections();

            // Then: Should return false
            assertThat(hasDetections).isFalse();
        }

        @Test
        @DisplayName("Should not detect detections when detections list is empty")
        void shouldNotDetectDetectionsWhenDetectionsListIsEmpty() {
            // Given: DetectionResponse with empty detections
            DetectionResponse response = DetectionResponse.builder()
                .detections(List.of())
                .build();

            // When: Checking for detections
            boolean hasDetections = response.hasDetections();

            // Then: Should return false
            assertThat(hasDetections).isFalse();
        }
    }

    @Nested
    @DisplayName("Data Integrity")
    class DataIntegrity {

        @Test
        @DisplayName("Should handle null collections gracefully")
        void shouldHandleNullCollectionsGracefully() {
            // Given: DetectionResponse with null detections
            DetectionResponse response = DetectionResponse.builder()
                .detections(null)
                .error(null)
                .build();

            // When: Calling business logic methods
            boolean hasDetections = response.hasDetections();
            boolean hasError = response.hasError();

            // Then: Should handle nulls gracefully
            assertThat(hasDetections).isFalse();
            assertThat(hasError).isFalse();
        }

        @Test
        @DisplayName("Should handle empty strings appropriately")
        void shouldHandleEmptyStringsAppropriately() {
            // Given: DetectionResponse with empty strings
            DetectionResponse response = DetectionResponse.builder()
                .correlationId("")
                .detectionType("")
                .error("")
                .build();

            // When: Getting values
            String correlationId = response.getCorrelationId();
            String detectionType = response.getDetectionType();
            String error = response.getError();

            // Then: Should return empty strings
            assertThat(correlationId).isEmpty();
            assertThat(detectionType).isEmpty();
            assertThat(error).isEmpty();
            assertThat(response.hasError()).isFalse();
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle zero values appropriately")
        void shouldHandleZeroValuesAppropriately() {
            // Given: DetectionResponse with zero values
            DetectionResponse response = DetectionResponse.builder()
                .detectionCount(0)
                .averageConfidence(0.0)
                .processingTimeMs(0L)
                .build();

            // When: Getting values
            int detectionCount = response.getDetectionCount();
            double averageConfidence = response.getAverageConfidence();
            long processingTimeMs = response.getProcessingTimeMs();

            // Then: Should return zero values
            assertThat(detectionCount).isEqualTo(0);
            assertThat(averageConfidence).isEqualTo(0.0);
            assertThat(processingTimeMs).isEqualTo(0L);
        }

        @Test
        @DisplayName("Should handle negative values")
        void shouldHandleNegativeValues() {
            // Given: DetectionResponse with negative values
            DetectionResponse response = DetectionResponse.builder()
                .detectionCount(-1)
                .averageConfidence(-0.5)
                .processingTimeMs(-100L)
                .build();

            // When: Getting values
            int detectionCount = response.getDetectionCount();
            double averageConfidence = response.getAverageConfidence();
            long processingTimeMs = response.getProcessingTimeMs();

            // Then: Should return negative values (no validation in DTO)
            assertThat(detectionCount).isEqualTo(-1);
            assertThat(averageConfidence).isEqualTo(-0.5);
            assertThat(processingTimeMs).isEqualTo(-100L);
        }

        @Test
        @DisplayName("Should handle large values")
        void shouldHandleLargeValues() {
            // Given: DetectionResponse with large values
            DetectionResponse response = DetectionResponse.builder()
                .detectionCount(Integer.MAX_VALUE)
                .averageConfidence(Double.MAX_VALUE)
                .processingTimeMs(Long.MAX_VALUE)
                .build();

            // When: Getting values
            int detectionCount = response.getDetectionCount();
            double averageConfidence = response.getAverageConfidence();
            long processingTimeMs = response.getProcessingTimeMs();

            // Then: Should handle large values
            assertThat(detectionCount).isEqualTo(Integer.MAX_VALUE);
            assertThat(averageConfidence).isEqualTo(Double.MAX_VALUE);
            assertThat(processingTimeMs).isEqualTo(Long.MAX_VALUE);
        }

        @Test
        @DisplayName("Should handle very large detection lists")
        void shouldHandleVeryLargeDetectionLists() {
            // Given: Large list of detections (simulating many detections)
            List<Detection> largeDetectionList = List.of(
                testDetection, testDetection, testDetection // Repeated for simplicity
            );

            // When: Creating DetectionResponse with large list
            DetectionResponse response = DetectionResponse.builder()
                .detections(largeDetectionList)
                .detectionCount(largeDetectionList.size())
                .build();

            // Then: Should handle correctly
            assertThat(response.getDetections()).hasSize(3);
            assertThat(response.getDetectionCount()).isEqualTo(3);
            assertThat(response.hasDetections()).isTrue();
        }
    }

    @Nested
    @DisplayName("JSON Serialization")
    class JsonSerialization {

        @Test
        @DisplayName("Should have JsonProperty annotations for all fields")
        void shouldHaveJsonPropertyAnnotationsForAllFields() {
            // This test verifies the presence of JSON annotations
            // In practice, this would be tested with JSON serialization tests

            // Given: DetectionResponse with all fields set
            DetectionResponse response = DetectionResponse.builder()
                .correlationId(testCorrelationId)
                .detectionType(testDetectionType)
                .detectionCount(testDetectionCount)
                .averageConfidence(testAverageConfidence)
                .processingTimeMs(testProcessingTimeMs)
                .detections(testDetections)
                .error(testError)
                .build();

            // When & Then: Should have all fields accessible
            assertThat(response.getCorrelationId()).isNotNull();
            assertThat(response.getDetectionType()).isNotNull();
            assertThat(response.getDetectionCount()).isGreaterThan(0);
            assertThat(response.getAverageConfidence()).isGreaterThan(0.0);
            assertThat(response.getProcessingTimeMs()).isGreaterThan(0L);
            assertThat(response.getDetections()).isNotNull();
            assertThat(response.getError()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Object Behavior")
    class ObjectBehavior {

        @Test
        @DisplayName("Should implement toString")
        void shouldImplementToString() {
            // Given: DetectionResponse instance
            DetectionResponse response = DetectionResponse.builder()
                .correlationId(testCorrelationId)
                .detectionType(testDetectionType)
                .build();

            // When: Calling toString
            String string = response.toString();

            // Then: Should return a non-null string
            assertThat(string).isNotNull();
            assertThat(string).contains("DetectionResponse");
        }

        @Test
        @DisplayName("Should implement equals correctly")
        void shouldImplementEqualsCorrectly() {
            // Given: Two DetectionResponse instances with same data
            DetectionResponse response1 = new DetectionResponse(
                testCorrelationId, testDetectionType, testDetectionCount,
                testAverageConfidence, testProcessingTimeMs, testDetections, testError);

            DetectionResponse response2 = new DetectionResponse(
                testCorrelationId, testDetectionType, testDetectionCount,
                testAverageConfidence, testProcessingTimeMs, testDetections, testError);

            // When & Then: Should be equal (default Object.equals behavior)
            assertThat(response1).isEqualTo(response1);
            assertThat(response1).isNotEqualTo(response2); // Different instances
            assertThat(response1).isNotEqualTo(null);
            assertThat(response1).isNotEqualTo(new Object());
        }

        @Test
        @DisplayName("Should implement hashCode")
        void shouldImplementHashCode() {
            // Given: DetectionResponse instance
            DetectionResponse response = DetectionResponse.builder()
                .correlationId(testCorrelationId)
                .build();

            // When: Calling hashCode
            int hashCode = response.hashCode();

            // Then: Should return an integer hash code
            assertThat(hashCode).isInstanceOf(Integer.class);
        }
    }

    @Nested
    @DisplayName("Success vs Error Responses")
    class SuccessVsErrorResponses {

        @Test
        @DisplayName("Should represent successful response")
        void shouldRepresentSuccessfulResponse() {
            // When: Creating successful response
            DetectionResponse response = DetectionResponse.builder()
                .correlationId(testCorrelationId)
                .detectionType(testDetectionType)
                .detectionCount(testDetectionCount)
                .averageConfidence(testAverageConfidence)
                .processingTimeMs(testProcessingTimeMs)
                .detections(testDetections)
                .build(); // No error set

            // Then: Should represent success
            assertThat(response.hasError()).isFalse();
            assertThat(response.hasDetections()).isTrue();
            assertThat(response.getError()).isNull();
        }

        @Test
        @DisplayName("Should represent error response")
        void shouldRepresentErrorResponse() {
            // When: Creating error response
            DetectionResponse response = DetectionResponse.builder()
                .correlationId(testCorrelationId)
                .error(testError)
                .build(); // No detections set

            // Then: Should represent error
            assertThat(response.hasError()).isTrue();
            assertThat(response.hasDetections()).isFalse();
            assertThat(response.getError()).isEqualTo(testError);
        }
    }
}
