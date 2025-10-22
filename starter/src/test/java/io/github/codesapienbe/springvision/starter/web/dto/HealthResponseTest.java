package io.github.codesapienbe.springvision.starter.web.dto;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive unit tests for HealthResponse DTO.
 * Tests all constructors, builders, getters/setters, and business logic methods.
 */
class HealthResponseTest {

    // Test fixtures
    private final String testCorrelationId = "health-check-123";
    private final String testBackendId = "djl";
    private final String testBackendName = "DJL Vision Backend";
    private final String testBackendVersion = "0.33.0";
    private final String testStatus = "HEALTHY";
    private final String testStatusMessage = "Backend is operating normally";
    private final long testResponseTimeMs = 45L;
    private final List<String> testSupportedTypes = List.of("face", "object", "text");
    private final String testError = "Connection timeout";

    @Nested
    @DisplayName("Constructor and Basic Properties")
    class ConstructorAndBasicProperties {

        @Test
        @DisplayName("Should create HealthResponse with all parameters constructor")
        void shouldCreateHealthResponseWithAllParametersConstructor() {
            // When: Creating HealthResponse with all parameters
            HealthResponse response = new HealthResponse(
                testCorrelationId, testBackendId, testBackendName, testBackendVersion,
                testStatus, testStatusMessage, testResponseTimeMs, testSupportedTypes, testError);

            // Then: Should store all values correctly
            assertThat(response.getCorrelationId()).isEqualTo(testCorrelationId);
            assertThat(response.getBackendId()).isEqualTo(testBackendId);
            assertThat(response.getBackendName()).isEqualTo(testBackendName);
            assertThat(response.getBackendVersion()).isEqualTo(testBackendVersion);
            assertThat(response.getStatus()).isEqualTo(testStatus);
            assertThat(response.getStatusMessage()).isEqualTo(testStatusMessage);
            assertThat(response.getResponseTimeMs()).isEqualTo(testResponseTimeMs);
            assertThat(response.getSupportedDetectionTypes()).isEqualTo(testSupportedTypes);
            assertThat(response.getError()).isEqualTo(testError);
        }

        @Test
        @DisplayName("Should create HealthResponse with default constructor")
        void shouldCreateHealthResponseWithDefaultConstructor() {
            // When: Creating HealthResponse with default constructor
            HealthResponse response = new HealthResponse();

            // Then: All fields should be null initially
            assertThat(response.getCorrelationId()).isNull();
            assertThat(response.getBackendId()).isNull();
            assertThat(response.getBackendName()).isNull();
            assertThat(response.getBackendVersion()).isNull();
            assertThat(response.getStatus()).isNull();
            assertThat(response.getStatusMessage()).isNull();
            assertThat(response.getResponseTimeMs()).isEqualTo(0L);
            assertThat(response.getSupportedDetectionTypes()).isNull();
            assertThat(response.getError()).isNull();
        }

        @Test
        @DisplayName("Should set and get all properties")
        void shouldSetAndGetAllProperties() {
            // Given: HealthResponse instance
            HealthResponse response = new HealthResponse();

            // When: Setting all properties
            response.setCorrelationId(testCorrelationId);
            response.setBackendId(testBackendId);
            response.setBackendName(testBackendName);
            response.setBackendVersion(testBackendVersion);
            response.setStatus(testStatus);
            response.setStatusMessage(testStatusMessage);
            response.setResponseTimeMs(testResponseTimeMs);
            response.setSupportedDetectionTypes(testSupportedTypes);
            response.setError(testError);

            // Then: Should return correct values
            assertThat(response.getCorrelationId()).isEqualTo(testCorrelationId);
            assertThat(response.getBackendId()).isEqualTo(testBackendId);
            assertThat(response.getBackendName()).isEqualTo(testBackendName);
            assertThat(response.getBackendVersion()).isEqualTo(testBackendVersion);
            assertThat(response.getStatus()).isEqualTo(testStatus);
            assertThat(response.getStatusMessage()).isEqualTo(testStatusMessage);
            assertThat(response.getResponseTimeMs()).isEqualTo(testResponseTimeMs);
            assertThat(response.getSupportedDetectionTypes()).isEqualTo(testSupportedTypes);
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
            HealthResponse.Builder builder = HealthResponse.builder();

            // Then: Should return a valid builder instance
            assertThat(builder).isNotNull();
            assertThat(builder).isInstanceOf(HealthResponse.Builder.class);
        }

        @Test
        @DisplayName("Should build HealthResponse with all fields")
        void shouldBuildHealthResponseWithAllFields() {
            // When: Building HealthResponse with all fields
            HealthResponse response = HealthResponse.builder()
                .correlationId(testCorrelationId)
                .backendId(testBackendId)
                .backendName(testBackendName)
                .backendVersion(testBackendVersion)
                .status(testStatus)
                .statusMessage(testStatusMessage)
                .responseTimeMs(testResponseTimeMs)
                .supportedDetectionTypes(testSupportedTypes)
                .error(testError)
                .build();

            // Then: Should have all correct values
            assertThat(response.getCorrelationId()).isEqualTo(testCorrelationId);
            assertThat(response.getBackendId()).isEqualTo(testBackendId);
            assertThat(response.getBackendName()).isEqualTo(testBackendName);
            assertThat(response.getBackendVersion()).isEqualTo(testBackendVersion);
            assertThat(response.getStatus()).isEqualTo(testStatus);
            assertThat(response.getStatusMessage()).isEqualTo(testStatusMessage);
            assertThat(response.getResponseTimeMs()).isEqualTo(testResponseTimeMs);
            assertThat(response.getSupportedDetectionTypes()).isEqualTo(testSupportedTypes);
            assertThat(response.getError()).isEqualTo(testError);
        }

        @Test
        @DisplayName("Should build HealthResponse with partial fields")
        void shouldBuildHealthResponseWithPartialFields() {
            // When: Building HealthResponse with only some fields
            HealthResponse response = HealthResponse.builder()
                .correlationId(testCorrelationId)
                .backendId(testBackendId)
                .status(testStatus)
                .build();

            // Then: Should have set fields and default values for others
            assertThat(response.getCorrelationId()).isEqualTo(testCorrelationId);
            assertThat(response.getBackendId()).isEqualTo(testBackendId);
            assertThat(response.getStatus()).isEqualTo(testStatus);
            assertThat(response.getBackendName()).isNull();
            assertThat(response.getResponseTimeMs()).isEqualTo(0L);
            assertThat(response.getSupportedDetectionTypes()).isNull();
            assertThat(response.getError()).isNull();
        }

        @Test
        @DisplayName("Should allow method chaining in builder")
        void shouldAllowMethodChainingInBuilder() {
            // When: Using method chaining
            HealthResponse response = HealthResponse.builder()
                .correlationId(testCorrelationId)
                .backendId(testBackendId)
                .backendName(testBackendName)
                .status(testStatus)
                .responseTimeMs(testResponseTimeMs)
                .supportedDetectionTypes(testSupportedTypes)
                .build();

            // Then: Should work correctly
            assertThat(response.getCorrelationId()).isEqualTo(testCorrelationId);
            assertThat(response.getBackendId()).isEqualTo(testBackendId);
            assertThat(response.getBackendName()).isEqualTo(testBackendName);
            assertThat(response.getStatus()).isEqualTo(testStatus);
            assertThat(response.getResponseTimeMs()).isEqualTo(testResponseTimeMs);
            assertThat(response.getSupportedDetectionTypes()).isEqualTo(testSupportedTypes);
        }

        @Test
        @DisplayName("Should create healthy response builder pattern")
        void shouldCreateHealthyResponseBuilderPattern() {
            // When: Building a healthy response
            HealthResponse response = HealthResponse.builder()
                .correlationId("health-123")
                .backendId("djl")
                .backendName("DJL Backend")
                .backendVersion("1.0.0")
                .status("HEALTHY")
                .statusMessage("All systems operational")
                .responseTimeMs(25L)
                .supportedDetectionTypes(List.of("face", "object"))
                .build();

            // Then: Should represent healthy state
            assertThat(response.getStatus()).isEqualTo("HEALTHY");
            assertThat(response.isHealthy()).isTrue();
            assertThat(response.hasError()).isFalse();
        }

        @Test
        @DisplayName("Should create unhealthy response builder pattern")
        void shouldCreateUnhealthyResponseBuilderPattern() {
            // When: Building an unhealthy response
            HealthResponse response = HealthResponse.builder()
                .correlationId("health-456")
                .backendId("djl")
                .status("UNHEALTHY")
                .statusMessage("Backend unavailable")
                .responseTimeMs(5000L)
                .error("Connection timeout")
                .build();

            // Then: Should represent unhealthy state
            assertThat(response.getStatus()).isEqualTo("UNHEALTHY");
            assertThat(response.isHealthy()).isFalse();
            assertThat(response.hasError()).isTrue();
            assertThat(response.getError()).isEqualTo("Connection timeout");
        }
    }

    @Nested
    @DisplayName("Business Logic Methods")
    class BusinessLogicMethods {

        @Test
        @DisplayName("Should detect error when error field is set")
        void shouldDetectErrorWhenErrorFieldIsSet() {
            // Given: HealthResponse with error
            HealthResponse response = HealthResponse.builder()
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
            // Given: HealthResponse without error
            HealthResponse response = HealthResponse.builder()
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
            // Given: HealthResponse with empty error
            HealthResponse response = HealthResponse.builder()
                .error("")
                .build();

            // When: Checking for error
            boolean hasError = response.hasError();

            // Then: Should return false
            assertThat(hasError).isFalse();
        }

        @Test
        @DisplayName("Should detect healthy status with HEALTHY")
        void shouldDetectHealthyStatusWithHealthy() {
            // Given: HealthResponse with HEALTHY status
            HealthResponse response = HealthResponse.builder()
                .status("HEALTHY")
                .build();

            // When: Checking if healthy
            boolean isHealthy = response.isHealthy();

            // Then: Should return true
            assertThat(isHealthy).isTrue();
        }

        @Test
        @DisplayName("Should detect healthy status with UP")
        void shouldDetectHealthyStatusWithUp() {
            // Given: HealthResponse with UP status
            HealthResponse response = HealthResponse.builder()
                .status("UP")
                .build();

            // When: Checking if healthy
            boolean isHealthy = response.isHealthy();

            // Then: Should return true
            assertThat(isHealthy).isTrue();
        }

        @Test
        @DisplayName("Should detect unhealthy status with DOWN")
        void shouldDetectUnhealthyStatusWithDown() {
            // Given: HealthResponse with DOWN status
            HealthResponse response = HealthResponse.builder()
                .status("DOWN")
                .build();

            // When: Checking if healthy
            boolean isHealthy = response.isHealthy();

            // Then: Should return false
            assertThat(isHealthy).isFalse();
        }

        @Test
        @DisplayName("Should detect unhealthy status with UNHEALTHY")
        void shouldDetectUnhealthyStatusWithUnhealthy() {
            // Given: HealthResponse with UNHEALTHY status
            HealthResponse response = HealthResponse.builder()
                .status("UNHEALTHY")
                .build();

            // When: Checking if healthy
            boolean isHealthy = response.isHealthy();

            // Then: Should return false
            assertThat(isHealthy).isFalse();
        }

        @Test
        @DisplayName("Should handle null status for health check")
        void shouldHandleNullStatusForHealthCheck() {
            // Given: HealthResponse with null status
            HealthResponse response = HealthResponse.builder()
                .status(null)
                .build();

            // When: Checking if healthy
            boolean isHealthy = response.isHealthy();

            // Then: Should return false
            assertThat(isHealthy).isFalse();
        }

        @Test
        @DisplayName("Should handle unknown status for health check")
        void shouldHandleUnknownStatusForHealthCheck() {
            // Given: HealthResponse with unknown status
            HealthResponse response = HealthResponse.builder()
                .status("UNKNOWN")
                .build();

            // When: Checking if healthy
            boolean isHealthy = response.isHealthy();

            // Then: Should return false
            assertThat(isHealthy).isFalse();
        }
    }

    @Nested
    @DisplayName("Supported Detection Types")
    class SupportedDetectionTypes {

        @Test
        @DisplayName("Should handle empty supported detection types list")
        void shouldHandleEmptySupportedDetectionTypesList() {
            // Given: HealthResponse with empty supported types
            HealthResponse response = HealthResponse.builder()
                .supportedDetectionTypes(List.of())
                .build();

            // When: Getting supported types
            List<String> types = response.getSupportedDetectionTypes();

            // Then: Should return empty list
            assertThat(types).isEmpty();
        }

        @Test
        @DisplayName("Should handle single supported detection type")
        void shouldHandleSingleSupportedDetectionType() {
            // Given: HealthResponse with single supported type
            HealthResponse response = HealthResponse.builder()
                .supportedDetectionTypes(List.of("face"))
                .build();

            // When: Getting supported types
            List<String> types = response.getSupportedDetectionTypes();

            // Then: Should return single type
            assertThat(types).hasSize(1);
            assertThat(types).containsExactly("face");
        }

        @Test
        @DisplayName("Should preserve order of supported detection types")
        void shouldPreserveOrderOfSupportedDetectionTypes() {
            // Given: HealthResponse with ordered types
            List<String> orderedTypes = List.of("face", "object", "text", "barcode");
            HealthResponse response = HealthResponse.builder()
                .supportedDetectionTypes(orderedTypes)
                .build();

            // When: Getting supported types
            List<String> types = response.getSupportedDetectionTypes();

            // Then: Should preserve order
            assertThat(types).containsExactly("face", "object", "text", "barcode");
        }
    }

    @Nested
    @DisplayName("Object Behavior")
    class ObjectBehavior {

        @Test
        @DisplayName("Should implement toString")
        void shouldImplementToString() {
            // Given: HealthResponse instance
            HealthResponse response = HealthResponse.builder()
                .correlationId(testCorrelationId)
                .backendId(testBackendId)
                .status(testStatus)
                .build();

            // When: Calling toString
            String string = response.toString();

            // Then: Should return a non-null string
            assertThat(string).isNotNull();
            assertThat(string).contains("HealthResponse");
        }

        @Test
        @DisplayName("Should implement equals correctly")
        void shouldImplementEqualsCorrectly() {
            // Given: Two HealthResponse instances with same data
            HealthResponse response1 = new HealthResponse(
                testCorrelationId, testBackendId, testBackendName, testBackendVersion,
                testStatus, testStatusMessage, testResponseTimeMs, testSupportedTypes, testError);

            HealthResponse response2 = new HealthResponse(
                testCorrelationId, testBackendId, testBackendName, testBackendVersion,
                testStatus, testStatusMessage, testResponseTimeMs, testSupportedTypes, testError);

            // When & Then: Should be equal (default Object.equals behavior)
            assertThat(response1).isEqualTo(response1);
            assertThat(response1).isNotEqualTo(response2); // Different instances
            assertThat(response1).isNotEqualTo(null);
            assertThat(response1).isNotEqualTo(new Object());
        }

        @Test
        @DisplayName("Should implement hashCode")
        void shouldImplementHashCode() {
            // Given: HealthResponse instance
            HealthResponse response = HealthResponse.builder()
                .correlationId(testCorrelationId)
                .backendId(testBackendId)
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
        @DisplayName("Should handle very long strings")
        void shouldHandleVeryLongStrings() {
            // Given: Very long strings
            String longCorrelationId = "a".repeat(1000);
            String longBackendName = "b".repeat(1000);
            String longError = "c".repeat(1000);

            // When: Creating HealthResponse with long strings
            HealthResponse response = HealthResponse.builder()
                .correlationId(longCorrelationId)
                .backendName(longBackendName)
                .error(longError)
                .build();

            // Then: Should handle correctly
            assertThat(response.getCorrelationId()).hasSize(1000);
            assertThat(response.getBackendName()).hasSize(1000);
            assertThat(response.getError()).hasSize(1000);
        }

        @Test
        @DisplayName("Should handle extreme response times")
        void shouldHandleExtremeResponseTimes() {
            // Given: Extreme response times
            HealthResponse fastResponse = HealthResponse.builder()
                .responseTimeMs(0L)
                .build();

            HealthResponse slowResponse = HealthResponse.builder()
                .responseTimeMs(Long.MAX_VALUE)
                .build();

            // When: Getting response times
            long fastTime = fastResponse.getResponseTimeMs();
            long slowTime = slowResponse.getResponseTimeMs();

            // Then: Should handle extremes
            assertThat(fastTime).isEqualTo(0L);
            assertThat(slowTime).isEqualTo(Long.MAX_VALUE);
        }

        @Test
        @DisplayName("Should handle very large supported types list")
        void shouldHandleVeryLargeSupportedTypesList() {
            // Given: Large list of supported types
            List<String> largeTypeList = List.of(
                "face", "object", "text", "barcode", "landmark", "pose",
                "hand", "body", "scene", "custom", "heart-rate", "fall",
                "stress", "tumor", "threat", "eavesdropping", "access-auth",
                "security-incident", "defect", "robotic-guidance", "component-verification",
                "metadata-extraction", "image-classification", "action-recognition",
                "nsfw", "emotion", "deepfake", "demographics"
            );

            // When: Creating HealthResponse with large list
            HealthResponse response = HealthResponse.builder()
                .supportedDetectionTypes(largeTypeList)
                .build();

            // Then: Should handle correctly
            assertThat(response.getSupportedDetectionTypes()).hasSize(largeTypeList.size());
            assertThat(response.getSupportedDetectionTypes()).contains("face", "object", "text");
        }
    }
}
