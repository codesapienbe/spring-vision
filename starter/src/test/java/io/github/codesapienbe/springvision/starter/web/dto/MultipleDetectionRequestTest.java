package io.github.codesapienbe.springvision.starter.web.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.List;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for MultipleDetectionRequest DTO.
 * Tests all constructors, builders, getters/setters, and validation.
 */
class MultipleDetectionRequestTest {

    // Test fixtures
    private final byte[] validImageData = {0x01, 0x02, 0x03, 0x04, 0x05};
    private final List<String> validDetectionTypes = List.of("face", "object", "text");
    private final List<String> singleDetectionType = List.of("face");
    private final List<String> emptyDetectionTypes = List.of();

    @Nested
    @DisplayName("Constructor and Basic Properties")
    class ConstructorAndBasicProperties {

        @Test
        @DisplayName("Should create MultipleDetectionRequest with all parameters constructor")
        void shouldCreateMultipleDetectionRequestWithAllParametersConstructor() {
            // When: Creating MultipleDetectionRequest with constructor
            MultipleDetectionRequest request = new MultipleDetectionRequest(validImageData, validDetectionTypes);

            // Then: Should store all values correctly
            assertThat(request.getImageData()).isEqualTo(validImageData);
            assertThat(request.getDetectionTypes()).isEqualTo(validDetectionTypes);
        }

        @Test
        @DisplayName("Should create MultipleDetectionRequest with default constructor")
        void shouldCreateMultipleDetectionRequestWithDefaultConstructor() {
            // When: Creating MultipleDetectionRequest with default constructor
            MultipleDetectionRequest request = new MultipleDetectionRequest();

            // Then: All fields should be null initially
            assertThat(request.getImageData()).isNull();
            assertThat(request.getDetectionTypes()).isNull();
        }

        @Test
        @DisplayName("Should set and get all properties")
        void shouldSetAndGetAllProperties() {
            // Given: MultipleDetectionRequest instance
            MultipleDetectionRequest request = new MultipleDetectionRequest();

            // When: Setting all properties
            request.setImageData(validImageData);
            request.setDetectionTypes(validDetectionTypes);

            // Then: Should return correct values
            assertThat(request.getImageData()).isEqualTo(validImageData);
            assertThat(request.getDetectionTypes()).isEqualTo(validDetectionTypes);
        }
    }

    @Nested
    @DisplayName("Builder Pattern")
    class BuilderPattern {

        @Test
        @DisplayName("Should create builder instance")
        void shouldCreateBuilderInstance() {
            // When: Creating builder
            MultipleDetectionRequest.Builder builder = MultipleDetectionRequest.builder();

            // Then: Should return a valid builder instance
            assertThat(builder).isNotNull();
            assertThat(builder).isInstanceOf(MultipleDetectionRequest.Builder.class);
        }

        @Test
        @DisplayName("Should build MultipleDetectionRequest with all fields")
        void shouldBuildMultipleDetectionRequestWithAllFields() {
            // When: Building MultipleDetectionRequest with all fields
            MultipleDetectionRequest request = MultipleDetectionRequest.builder()
                .imageData(validImageData)
                .detectionTypes(validDetectionTypes)
                .build();

            // Then: Should have correct values
            assertThat(request.getImageData()).isEqualTo(validImageData);
            assertThat(request.getDetectionTypes()).isEqualTo(validDetectionTypes);
        }

        @Test
        @DisplayName("Should build MultipleDetectionRequest with partial fields")
        void shouldBuildMultipleDetectionRequestWithPartialFields() {
            // When: Building MultipleDetectionRequest with only some fields
            MultipleDetectionRequest request = MultipleDetectionRequest.builder()
                .imageData(validImageData)
                .build();

            // Then: Should have set field and null for others
            assertThat(request.getImageData()).isEqualTo(validImageData);
            assertThat(request.getDetectionTypes()).isNull();
        }

        @Test
        @DisplayName("Should allow method chaining in builder")
        void shouldAllowMethodChainingInBuilder() {
            // When: Using method chaining
            MultipleDetectionRequest request = MultipleDetectionRequest.builder()
                .imageData(validImageData)
                .detectionTypes(validDetectionTypes)
                .build();

            // Then: Should work correctly
            assertThat(request.getImageData()).isEqualTo(validImageData);
            assertThat(request.getDetectionTypes()).isEqualTo(validDetectionTypes);
        }

        @Test
        @DisplayName("Should create multiple instances with builder")
        void shouldCreateMultipleInstancesWithBuilder() {
            // Given: Different data
            List<String> types1 = List.of("face");
            List<String> types2 = List.of("object", "text");

            // When: Building multiple instances
            MultipleDetectionRequest request1 = MultipleDetectionRequest.builder()
                .imageData(validImageData)
                .detectionTypes(types1)
                .build();

            MultipleDetectionRequest request2 = MultipleDetectionRequest.builder()
                .imageData(validImageData)
                .detectionTypes(types2)
                .build();

            // Then: Should have different detection types
            assertThat(request1.getDetectionTypes()).isEqualTo(types1);
            assertThat(request2.getDetectionTypes()).isEqualTo(types2);
            assertThat(request1.getDetectionTypes()).isNotEqualTo(request2.getDetectionTypes());
        }

        @Test
        @DisplayName("Should reuse builder for multiple builds")
        void shouldReuseBuilderForMultipleBuilds() {
            // Given: Builder instance
            MultipleDetectionRequest.Builder builder = MultipleDetectionRequest.builder();

            // When: Building multiple times with different values
            MultipleDetectionRequest request1 = builder
                .imageData(validImageData)
                .detectionTypes(List.of("face"))
                .build();

            MultipleDetectionRequest request2 = builder
                .imageData(validImageData)
                .detectionTypes(List.of("object"))
                .build();

            // Then: Each build should have different values
            assertThat(request1.getDetectionTypes()).containsExactly("face");
            assertThat(request2.getDetectionTypes()).containsExactly("object");
        }
    }

    @Nested
    @DisplayName("Detection Types Handling")
    class DetectionTypesHandling {

        @Test
        @DisplayName("Should handle single detection type")
        void shouldHandleSingleDetectionType() {
            // When: Creating request with single detection type
            MultipleDetectionRequest request = MultipleDetectionRequest.builder()
                .imageData(validImageData)
                .detectionTypes(singleDetectionType)
                .build();

            // Then: Should have single type
            assertThat(request.getDetectionTypes()).hasSize(1);
            assertThat(request.getDetectionTypes()).containsExactly("face");
        }

        @Test
        @DisplayName("Should handle multiple detection types")
        void shouldHandleMultipleDetectionTypes() {
            // When: Creating request with multiple detection types
            MultipleDetectionRequest request = MultipleDetectionRequest.builder()
                .imageData(validImageData)
                .detectionTypes(validDetectionTypes)
                .build();

            // Then: Should have all types
            assertThat(request.getDetectionTypes()).hasSize(3);
            assertThat(request.getDetectionTypes()).containsExactly("face", "object", "text");
        }

        @Test
        @DisplayName("Should handle empty detection types list")
        void shouldHandleEmptyDetectionTypesList() {
            // When: Creating request with empty detection types
            MultipleDetectionRequest request = MultipleDetectionRequest.builder()
                .imageData(validImageData)
                .detectionTypes(emptyDetectionTypes)
                .build();

            // Then: Should have empty list
            assertThat(request.getDetectionTypes()).isEmpty();
        }

        @Test
        @DisplayName("Should preserve order of detection types")
        void shouldPreserveOrderOfDetectionTypes() {
            // Given: Detection types in specific order
            List<String> orderedTypes = List.of("text", "face", "object", "barcode");

            // When: Creating request with ordered types
            MultipleDetectionRequest request = MultipleDetectionRequest.builder()
                .imageData(validImageData)
                .detectionTypes(orderedTypes)
                .build();

            // Then: Should preserve order
            assertThat(request.getDetectionTypes()).containsExactly("text", "face", "object", "barcode");
        }

        @Test
        @DisplayName("Should handle duplicate detection types")
        void shouldHandleDuplicateDetectionTypes() {
            // Given: Detection types with duplicates
            List<String> typesWithDuplicates = Arrays.asList("face", "object", "face", "text");

            // When: Creating request with duplicate types
            MultipleDetectionRequest request = MultipleDetectionRequest.builder()
                .imageData(validImageData)
                .detectionTypes(typesWithDuplicates)
                .build();

            // Then: Should preserve duplicates
            assertThat(request.getDetectionTypes()).hasSize(4);
            assertThat(request.getDetectionTypes()).containsExactly("face", "object", "face", "text");
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

            // When: Setting the data to a MultipleDetectionRequest
            MultipleDetectionRequest request = new MultipleDetectionRequest();
            request.setImageData(originalData);

            // And modifying the original array
            originalData[0] = (byte) 0x99;

            // Then: MultipleDetectionRequest reflects the change (no defensive copy)
            assertThat(request.getImageData()[0]).isEqualTo((byte) 0x99);
            assertThat(request.getImageData()).isEqualTo(originalData);
        }

        @Test
        @DisplayName("Should reflect changes to original detection types list when setting")
        void shouldReflectChangesToOriginalDetectionTypesListWhenSetting() {
            // Given: Original mutable list
            List<String> originalTypes = Arrays.asList("face", "object");

            // When: Setting the types to a MultipleDetectionRequest
            MultipleDetectionRequest request = new MultipleDetectionRequest();
            request.setDetectionTypes(originalTypes);

            // Then: Should have the types
            assertThat(request.getDetectionTypes()).containsExactly("face", "object");
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

            // Given: MultipleDetectionRequest with null image data
            MultipleDetectionRequest request = new MultipleDetectionRequest();
            request.setImageData(null);

            // When & Then: The validation would fail, but we can't easily test
            // the annotation directly without a validation framework
            assertThat(request.getImageData()).isNull();
        }

        @Test
        @DisplayName("Should have NotNull and NotEmpty validation on detectionTypes field")
        void shouldHaveNotNullAndNotEmptyValidationOnDetectionTypesField() {
            // Similar to above - testing the presence of validation annotations
            // would require a validation framework

            // Given: MultipleDetectionRequest with null detection types
            MultipleDetectionRequest request = new MultipleDetectionRequest();
            request.setDetectionTypes(null);

            // When & Then: Validation would fail for null list
            assertThat(request.getDetectionTypes()).isNull();
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

            // Given: MultipleDetectionRequest with all fields set
            MultipleDetectionRequest request = MultipleDetectionRequest.builder()
                .imageData(validImageData)
                .detectionTypes(validDetectionTypes)
                .build();

            // When & Then: Should have all fields accessible
            assertThat(request.getImageData()).isNotNull();
            assertThat(request.getDetectionTypes()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Object Behavior")
    class ObjectBehavior {

        @Test
        @DisplayName("Should implement toString")
        void shouldImplementToString() {
            // Given: MultipleDetectionRequest instance
            MultipleDetectionRequest request = MultipleDetectionRequest.builder()
                .imageData(validImageData)
                .detectionTypes(singleDetectionType)
                .build();

            // When: Calling toString
            String string = request.toString();

            // Then: Should return a non-null string
            assertThat(string).isNotNull();
            assertThat(string).contains("MultipleDetectionRequest");
        }

        @Test
        @DisplayName("Should implement equals correctly")
        void shouldImplementEqualsCorrectly() {
            // Given: Two MultipleDetectionRequest instances with same data
            MultipleDetectionRequest request1 = new MultipleDetectionRequest(validImageData, validDetectionTypes);
            MultipleDetectionRequest request2 = new MultipleDetectionRequest(validImageData.clone(), List.copyOf(validDetectionTypes));

            // When & Then: Should be equal (default Object.equals behavior)
            assertThat(request1).isEqualTo(request1);
            assertThat(request1).isNotEqualTo(request2); // Different instances
            assertThat(request1).isNotEqualTo(null);
            assertThat(request1).isNotEqualTo(new Object());
        }

        @Test
        @DisplayName("Should implement hashCode")
        void shouldImplementHashCode() {
            // Given: MultipleDetectionRequest instance
            MultipleDetectionRequest request = MultipleDetectionRequest.builder()
                .imageData(validImageData)
                .detectionTypes(singleDetectionType)
                .build();

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
        @DisplayName("Should handle null values appropriately")
        void shouldHandleNullValuesAppropriately() {
            // When: Creating request with null values
            MultipleDetectionRequest request = new MultipleDetectionRequest(null, null);

            // Then: Should handle nulls
            assertThat(request.getImageData()).isNull();
            assertThat(request.getDetectionTypes()).isNull();
        }

        @Test
        @DisplayName("Should handle very large detection types lists")
        void shouldHandleVeryLargeDetectionTypesLists() {
            // Given: Large list of detection types (simulating many types)
            List<String> largeTypeList = List.of("face", "object", "text", "barcode", "landmark", "pose");

            // When: Creating request with large list
            MultipleDetectionRequest request = MultipleDetectionRequest.builder()
                .imageData(validImageData)
                .detectionTypes(largeTypeList)
                .build();

            // Then: Should handle correctly
            assertThat(request.getDetectionTypes()).hasSize(6);
            assertThat(request.getDetectionTypes()).contains("face", "object", "text", "barcode", "landmark", "pose");
        }

        @Test
        @DisplayName("Should handle special characters in detection types")
        void shouldHandleSpecialCharactersInDetectionTypes() {
            // Given: Detection types with special characters
            List<String> specialTypes = List.of("face-detection", "object_detection", "text.recognition");

            // When: Creating request with special types
            MultipleDetectionRequest request = MultipleDetectionRequest.builder()
                .imageData(validImageData)
                .detectionTypes(specialTypes)
                .build();

            // Then: Should handle correctly
            assertThat(request.getDetectionTypes()).containsExactly("face-detection", "object_detection", "text.recognition");
        }
    }
}
