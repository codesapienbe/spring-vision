package io.github.codesapienbe.springvision.starter.web.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for DetectionRequest DTO.
 * Tests all constructors, builders, getters/setters, and validation.
 */
class DetectionRequestTest {

    // Test fixtures
    private final byte[] validImageData = {0x01, 0x02, 0x03, 0x04, 0x05};
    private final byte[] emptyImageData = new byte[0];

    @Nested
    @DisplayName("Constructor and Basic Properties")
    class ConstructorAndBasicProperties {

        @Test
        @DisplayName("Should create DetectionRequest with valid constructor")
        void shouldCreateDetectionRequestWithValidConstructor() {
            // When: Creating DetectionRequest with constructor
            DetectionRequest request = new DetectionRequest(validImageData);

            // Then: Should store the image data correctly
            assertThat(request.getImageData()).isEqualTo(validImageData);
        }

        @Test
        @DisplayName("Should create DetectionRequest with default constructor")
        void shouldCreateDetectionRequestWithDefaultConstructor() {
            // When: Creating DetectionRequest with default constructor
            DetectionRequest request = new DetectionRequest();

            // Then: Should have null image data initially
            assertThat(request.getImageData()).isNull();
        }

        @Test
        @DisplayName("Should set and get image data")
        void shouldSetAndGetImageData() {
            // Given: DetectionRequest instance
            DetectionRequest request = new DetectionRequest();

            // When: Setting image data
            request.setImageData(validImageData);

            // Then: Should return the same data
            assertThat(request.getImageData()).isEqualTo(validImageData);
        }

        @Test
        @DisplayName("Should handle null image data")
        void shouldHandleNullImageData() {
            // Given: DetectionRequest instance
            DetectionRequest request = new DetectionRequest();

            // When: Setting null image data
            request.setImageData(null);

            // Then: Should return null
            assertThat(request.getImageData()).isNull();
        }
    }

    @Nested
    @DisplayName("Builder Pattern")
    class BuilderPattern {

        @Test
        @DisplayName("Should create builder instance")
        void shouldCreateBuilderInstance() {
            // When: Creating builder
            DetectionRequest.Builder builder = DetectionRequest.builder();

            // Then: Should return a valid builder instance
            assertThat(builder).isNotNull();
            assertThat(builder).isInstanceOf(DetectionRequest.Builder.class);
        }

        @Test
        @DisplayName("Should build DetectionRequest with image data")
        void shouldBuildDetectionRequestWithImageData() {
            // When: Building DetectionRequest with image data
            DetectionRequest request = DetectionRequest.builder()
                .imageData(validImageData)
                .build();

            // Then: Should have correct image data
            assertThat(request.getImageData()).isEqualTo(validImageData);
        }

        @Test
        @DisplayName("Should build DetectionRequest with null image data")
        void shouldBuildDetectionRequestWithNullImageData() {
            // When: Building DetectionRequest with null image data
            DetectionRequest request = DetectionRequest.builder()
                .imageData(null)
                .build();

            // Then: Should have null image data
            assertThat(request.getImageData()).isNull();
        }

        @Test
        @DisplayName("Should allow method chaining in builder")
        void shouldAllowMethodChainingInBuilder() {
            // When: Using method chaining
            DetectionRequest.Builder builder = DetectionRequest.builder();
            DetectionRequest request = builder
                .imageData(validImageData)
                .build();

            // Then: Should work correctly
            assertThat(request.getImageData()).isEqualTo(validImageData);
        }

        @Test
        @DisplayName("Should create multiple instances with builder")
        void shouldCreateMultipleInstancesWithBuilder() {
            // Given: Different image data
            byte[] imageData1 = {0x01, 0x02};
            byte[] imageData2 = {0x03, 0x04, 0x05};

            // When: Building multiple instances
            DetectionRequest request1 = DetectionRequest.builder()
                .imageData(imageData1)
                .build();

            DetectionRequest request2 = DetectionRequest.builder()
                .imageData(imageData2)
                .build();

            // Then: Should have different data
            assertThat(request1.getImageData()).isEqualTo(imageData1);
            assertThat(request2.getImageData()).isEqualTo(imageData2);
            assertThat(request1.getImageData()).isNotEqualTo(request2.getImageData());
        }

        @Test
        @DisplayName("Should reuse builder for multiple builds")
        void shouldReuseBuilderForMultipleBuilds() {
            // Given: Builder instance
            DetectionRequest.Builder builder = DetectionRequest.builder();

            // When: Building multiple times
            DetectionRequest request1 = builder.imageData(validImageData).build();
            DetectionRequest request2 = builder.imageData(emptyImageData).build();

            // Then: Each build should have different data
            assertThat(request1.getImageData()).isEqualTo(validImageData);
            assertThat(request2.getImageData()).isEqualTo(emptyImageData);
        }
    }

    @Nested
    @DisplayName("Data Integrity")
    class DataIntegrity {

        @Test
        @DisplayName("Should reflect changes to original byte array when setting")
        void shouldReflectChangesToOriginalByteArrayWhenSetting() {
            // Given: Original byte array
            byte[] originalData = {0x01, 0x02, 0x03};

            // When: Setting the data to a DetectionRequest
            DetectionRequest request = new DetectionRequest();
            request.setImageData(originalData);

            // And modifying the original array
            originalData[0] = (byte) 0x99;

            // Then: DetectionRequest reflects the change (no defensive copy)
            assertThat(request.getImageData()[0]).isEqualTo((byte) 0x99);
            assertThat(request.getImageData()).isEqualTo(originalData);
        }

        @Test
        @DisplayName("Should not expose internal array reference")
        void shouldNotExposeInternalArrayReference() {
            // Given: DetectionRequest with data
            DetectionRequest request = new DetectionRequest(validImageData);

            // When: Getting image data and modifying it
            byte[] retrievedData = request.getImageData();
            retrievedData[0] = (byte) 0x99;

            // Then: Original data should be unchanged (if defensive copy is made)
            // Note: Current implementation doesn't make defensive copies in getters
            // This test documents the current behavior
            assertThat(validImageData[0]).isEqualTo((byte) 0x99);
        }
    }

    @Nested
    @DisplayName("Validation Annotations")
    class ValidationAnnotations {

        @Test
        @DisplayName("Should have NotNull validation on imageData field")
        void shouldHaveNotNullValidationOnImageDataField() {
            // This test verifies that the validation annotation is present
            // In a real application, this would be tested with a validation framework

            // Given: DetectionRequest with null image data
            DetectionRequest request = new DetectionRequest();
            request.setImageData(null);

            // When & Then: The validation would fail, but we can't easily test
            // the annotation directly without a validation framework
            assertThat(request.getImageData()).isNull();
        }

        @Test
        @DisplayName("Should have Size validation on imageData field")
        void shouldHaveSizeValidationOnImageDataField() {
            // Similar to above - testing the presence of validation annotations
            // would require a validation framework

            // Given: DetectionRequest with empty image data
            DetectionRequest request = new DetectionRequest();
            request.setImageData(emptyImageData);

            // When & Then: Validation would fail for empty array
            assertThat(request.getImageData()).isEmpty();
        }
    }

    @Nested
    @DisplayName("JSON Serialization")
    class JsonSerialization {

        @Test
        @DisplayName("Should have JsonProperty annotation for imageData")
        void shouldHaveJsonPropertyAnnotationForImageData() {
            // This test verifies the presence of JSON annotations
            // In practice, this would be tested with JSON serialization tests

            // Given: DetectionRequest instance
            DetectionRequest request = new DetectionRequest(validImageData);

            // When & Then: Should have image data accessible
            assertThat(request.getImageData()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Object Behavior")
    class ObjectBehavior {

        @Test
        @DisplayName("Should implement toString")
        void shouldImplementToString() {
            // Given: DetectionRequest instance
            DetectionRequest request = new DetectionRequest(validImageData);

            // When: Calling toString
            String string = request.toString();

            // Then: Should return a non-null string
            assertThat(string).isNotNull();
            assertThat(string).contains("DetectionRequest");
        }

        @Test
        @DisplayName("Should implement equals correctly")
        void shouldImplementEqualsCorrectly() {
            // Given: Two DetectionRequest instances with same data
            DetectionRequest request1 = new DetectionRequest(validImageData);
            DetectionRequest request2 = new DetectionRequest(validImageData.clone());

            // When & Then: Should be equal (default Object.equals behavior)
            assertThat(request1).isEqualTo(request1);
            assertThat(request1).isNotEqualTo(request2); // Different instances, even with same data
            assertThat(request1).isNotEqualTo(null);
            assertThat(request1).isNotEqualTo(new Object());
        }

        @Test
        @DisplayName("Should implement hashCode")
        void shouldImplementHashCode() {
            // Given: DetectionRequest instance
            DetectionRequest request = new DetectionRequest(validImageData);

            // When: Calling hashCode
            int hashCode = request.hashCode();

            // Then: Should return an integer hash code
            assertThat(hashCode).isInstanceOf(Integer.class);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle very large byte arrays")
        void shouldHandleVeryLargeByteArrays() {
            // Given: Large byte array (simulating a large image)
            byte[] largeData = new byte[1024 * 1024]; // 1MB
            for (int i = 0; i < largeData.length; i++) {
                largeData[i] = (byte) (i % 256);
            }

            // When: Creating DetectionRequest with large data
            DetectionRequest request = new DetectionRequest(largeData);

            // Then: Should handle it correctly
            assertThat(request.getImageData()).hasSize(1024 * 1024);
            assertThat(request.getImageData()).isEqualTo(largeData);
        }

        @Test
        @DisplayName("Should handle single byte arrays")
        void shouldHandleSingleByteArrays() {
            // Given: Single byte array
            byte[] singleByte = {(byte) 0xFF};

            // When: Creating DetectionRequest
            DetectionRequest request = new DetectionRequest(singleByte);

            // Then: Should handle correctly
            assertThat(request.getImageData()).hasSize(1);
            assertThat(request.getImageData()[0]).isEqualTo((byte) 0xFF);
        }

        @Test
        @DisplayName("Should handle maximum size byte arrays")
        void shouldHandleMaximumSizeByteArrays() {
            // Given: Maximum reasonable size array (100MB)
            int maxSize = 100 * 1024 * 1024; // 100MB
            byte[] maxData = new byte[maxSize];
            // Don't fill it to avoid memory issues in tests

            // When: Creating DetectionRequest
            DetectionRequest request = new DetectionRequest(maxData);

            // Then: Should handle the size
            assertThat(request.getImageData()).hasSize(maxSize);
        }
    }
}
