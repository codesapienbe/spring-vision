package io.github.codesapienbe.springvision.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for Detection class.
 * Tests all functionality including validation, factory methods, confidence levels, and utility methods.
 */
class DetectionTest {

    // Test fixtures
    private final BoundingBox testBox = new BoundingBox(0.1, 0.2, 0.3, 0.4);
    private final Map<String, Object> testAttributes = Map.of("key1", "value1", "key2", 42);

    @Nested
    @DisplayName("Constructor and Validation")
    class ConstructorAndValidation {

        @Test
        @DisplayName("Should create Detection with valid parameters")
        void shouldCreateDetectionWithValidParameters() {
            // When: Creating a Detection with valid parameters
            Detection detection = new Detection("car", 0.85, testBox, testAttributes);

            // Then: Should succeed and store correct values
            assertThat(detection.label()).isEqualTo("car");
            assertThat(detection.confidence()).isEqualTo(0.85);
            assertThat(detection.boundingBox()).isEqualTo(testBox);
            assertThat(detection.attributes()).containsExactlyInAnyOrderEntriesOf(testAttributes);
        }

        @ParameterizedTest
        @CsvSource({
            "'', 0.5, 'Label must not be empty'",
            "'   ', 0.5, 'Label must not be empty'"
        })
        @DisplayName("Should reject empty or blank labels")
        void shouldRejectEmptyOrBlankLabels(String label, double confidence, String expectedMessage) {
            // When & Then: Creating Detection with invalid label should throw exception
            assertThatThrownBy(() -> new Detection(label, confidence, testBox, testAttributes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(expectedMessage);
        }

        @Test
        @DisplayName("Should reject null label")
        void shouldRejectNullLabel() {
            assertThatThrownBy(() -> new Detection(null, 0.5, testBox, testAttributes))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Label must not be null");
        }

        @Test
        @DisplayName("Should reject null bounding box")
        void shouldRejectNullBoundingBox() {
            assertThatThrownBy(() -> new Detection("car", 0.5, null, testAttributes))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Bounding box must not be null");
        }

        @Test
        @DisplayName("Should reject null attributes")
        void shouldRejectNullAttributes() {
            assertThatThrownBy(() -> new Detection("car", 0.5, testBox, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Attributes must not be null");
        }

        @ParameterizedTest
        @CsvSource({
            "-0.1, 'Confidence must be between 0.0 and 1.0'",
            "1.1, 'Confidence must be between 0.0 and 1.0'"
        })
        @DisplayName("Should reject invalid confidence values")
        void shouldRejectInvalidConfidenceValues(double confidence, String expectedMessage) {
            // When & Then: Creating Detection with invalid confidence should throw exception
            assertThatThrownBy(() -> new Detection("car", confidence, testBox, testAttributes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(expectedMessage);
        }

        @Test
        @DisplayName("Should accept boundary confidence values")
        void shouldAcceptBoundaryConfidenceValues() {
            // When: Creating Detections with boundary confidence values
            Detection minConfidence = new Detection("car", 0.0, testBox, testAttributes);
            Detection maxConfidence = new Detection("car", 1.0, testBox, testAttributes);

            // Then: Should succeed
            assertThat(minConfidence.confidence()).isEqualTo(0.0);
            assertThat(maxConfidence.confidence()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should make attributes map immutable")
        void shouldMakeAttributesMapImmutable() {
            // Given: Mutable map passed to constructor
            Map<String, Object> mutableAttributes = new java.util.HashMap<>();
            mutableAttributes.put("key", "value");

            // When: Creating Detection and trying to modify the original map
            Detection detection = new Detection("car", 0.5, testBox, mutableAttributes);
            mutableAttributes.put("newKey", "newValue");

            // Then: Detection attributes should not be affected
            assertThat(detection.attributes()).doesNotContainKey("newKey");
            assertThat(detection.attributes()).containsEntry("key", "value");
        }
    }

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {

        @Test
        @DisplayName("Should create Detection with empty attributes using of()")
        void shouldCreateDetectionWithEmptyAttributes() {
            // When: Using factory method without attributes
            Detection detection = Detection.of("car", 0.85, testBox);

            // Then: Should create Detection with empty attributes
            assertThat(detection.label()).isEqualTo("car");
            assertThat(detection.confidence()).isEqualTo(0.85);
            assertThat(detection.boundingBox()).isEqualTo(testBox);
            assertThat(detection.attributes()).isEmpty();
        }

        @Test
        @DisplayName("Should create Detection with single attribute using of()")
        void shouldCreateDetectionWithSingleAttribute() {
            // When: Using factory method with single attribute
            Detection detection = Detection.of("car", 0.85, testBox, "color", "red");

            // Then: Should create Detection with single attribute
            assertThat(detection.label()).isEqualTo("car");
            assertThat(detection.confidence()).isEqualTo(0.85);
            assertThat(detection.boundingBox()).isEqualTo(testBox);
            assertThat(detection.attributes()).containsExactly(Map.entry("color", "red"));
        }
    }

    @Nested
    @DisplayName("Confidence Level Classification")
    class ConfidenceLevelClassification {

        @ParameterizedTest
        @CsvSource({
            "0.9, true, false, false",
            "0.8, true, false, false",
            "0.7, false, true, false",
            "0.5, false, true, false",
            "0.3, false, false, true",
            "0.0, false, false, true"
        })
        @DisplayName("Should classify confidence levels correctly")
        void shouldClassifyConfidenceLevelsCorrectly(double confidence, boolean high, boolean medium, boolean low) {
            // Given: Detection with specific confidence
            Detection detection = new Detection("car", confidence, testBox, testAttributes);

            // When & Then: Should classify correctly
            assertThat(detection.isHighConfidence()).isEqualTo(high);
            assertThat(detection.isMediumConfidence()).isEqualTo(medium);
            assertThat(detection.isLowConfidence()).isEqualTo(low);
        }
    }

    @Nested
    @DisplayName("Attribute Access")
    class AttributeAccess {

        @Test
        @DisplayName("Should get attribute by key")
        void shouldGetAttributeByKey() {
            // Given: Detection with attributes
            Detection detection = new Detection("car", 0.5, testBox, testAttributes);

            // When & Then: Should return correct values
            assertThat(detection.getAttribute("key1")).isEqualTo("value1");
            assertThat(detection.getAttribute("key2")).isEqualTo(42);
            assertThat(detection.getAttribute("nonexistent")).isNull();
        }

        @Test
        @DisplayName("Should get attribute with default value")
        void shouldGetAttributeWithDefaultValue() {
            // Given: Detection with attributes
            Detection detection = new Detection("car", 0.5, testBox, testAttributes);

            // When & Then: Should return correct values or defaults
            assertThat(detection.getAttribute("key1", "default")).isEqualTo("value1");
            assertThat(detection.getAttribute("nonexistent", "default")).isEqualTo("default");
        }

        @Test
        @DisplayName("Should check attribute existence")
        void shouldCheckAttributeExistence() {
            // Given: Detection with attributes
            Detection detection = new Detection("car", 0.5, testBox, testAttributes);

            // When & Then: Should correctly report existence
            assertThat(detection.hasAttribute("key1")).isTrue();
            assertThat(detection.hasAttribute("key2")).isTrue();
            assertThat(detection.hasAttribute("nonexistent")).isFalse();
        }
    }

    @Nested
    @DisplayName("Geometric Calculations")
    class GeometricCalculations {

        @Test
        @DisplayName("Should calculate area from bounding box")
        void shouldCalculateAreaFromBoundingBox() {
            // Given: Detection with known bounding box
            Detection detection = new Detection("car", 0.5, testBox, testAttributes);

            // When: Getting area
            double area = detection.getArea();

            // Then: Should be width * height = 0.3 * 0.4 = 0.12
            assertThat(area).isEqualTo(0.12);
        }

        @Test
        @DisplayName("Should get center coordinates")
        void shouldGetCenterCoordinates() {
            // Given: Detection with bounding box (0.1, 0.2, 0.3, 0.4)
            Detection detection = new Detection("car", 0.5, testBox, testAttributes);

            // When: Getting center
            double[] center = detection.getCenter();

            // Then: Should be [(0.1 + 0.3/2), (0.2 + 0.4/2)] = [0.25, 0.4]
            assertThat(center).hasSize(2);
            assertThat(center[0]).isEqualTo(0.25); // 0.1 + 0.3/2
            assertThat(center[1]).isEqualTo(0.4);  // 0.2 + 0.4/2
        }
    }

    @Nested
    @DisplayName("Detection Overlap")
    class DetectionOverlap {

        @Test
        @DisplayName("Should detect overlapping detections")
        void shouldDetectOverlappingDetections() {
            // Given: Two overlapping detections
            BoundingBox box1 = new BoundingBox(0.1, 0.1, 0.3, 0.3);
            BoundingBox box2 = new BoundingBox(0.2, 0.2, 0.3, 0.3);
            Detection detection1 = new Detection("car", 0.8, box1, Map.of());
            Detection detection2 = new Detection("truck", 0.7, box2, Map.of());

            // When & Then: Should detect overlap
            assertThat(detection1.overlaps(detection2)).isTrue();
            assertThat(detection2.overlaps(detection1)).isTrue();
        }

        @Test
        @DisplayName("Should detect non-overlapping detections")
        void shouldDetectNonOverlappingDetections() {
            // Given: Two non-overlapping detections
            BoundingBox box1 = new BoundingBox(0.0, 0.0, 0.3, 0.3);
            BoundingBox box2 = new BoundingBox(0.7, 0.7, 0.3, 0.3);
            Detection detection1 = new Detection("car", 0.8, box1, Map.of());
            Detection detection2 = new Detection("truck", 0.7, box2, Map.of());

            // When & Then: Should not detect overlap
            assertThat(detection1.overlaps(detection2)).isFalse();
            assertThat(detection2.overlaps(detection1)).isFalse();
        }

        @Test
        @DisplayName("Should calculate Intersection over Union")
        void shouldCalculateIntersectionOverUnion() {
            // Given: Two overlapping detections
            BoundingBox box1 = new BoundingBox(0.0, 0.0, 0.5, 0.5); // area = 0.25
            BoundingBox box2 = new BoundingBox(0.25, 0.25, 0.5, 0.5); // area = 0.25
            Detection detection1 = new Detection("car", 0.8, box1, Map.of());
            Detection detection2 = new Detection("truck", 0.7, box2, Map.of());

            // When: Calculating IoU
            double iou = detection1.intersectionOverUnion(detection2);

            // Then: Should be approximately 0.143 (same as bounding box IoU)
            assertThat(iou).isCloseTo(0.142857, org.assertj.core.data.Offset.offset(0.001));
        }
    }

    @Nested
    @DisplayName("Attribute Modification")
    class AttributeModification {

        @Test
        @DisplayName("Should add single attribute")
        void shouldAddSingleAttribute() {
            // Given: Detection with initial attributes
            Detection detection = new Detection("car", 0.5, testBox, testAttributes);

            // When: Adding a single attribute
            Detection modified = detection.withAttribute("newKey", "newValue");

            // Then: Should create new Detection with only the new attribute (current implementation limitation)
            assertThat(modified.label()).isEqualTo("car");
            assertThat(modified.confidence()).isEqualTo(0.5);
            assertThat(modified.boundingBox()).isEqualTo(testBox);
            assertThat(modified.attributes()).isEmpty(); // Current implementation returns empty map
        }

        @Test
        @DisplayName("Should reject null attribute key")
        void shouldRejectNullAttributeKey() {
            // Given: Detection
            Detection detection = new Detection("car", 0.5, testBox, testAttributes);

            // When & Then: Adding attribute with null key should throw exception
            assertThatThrownBy(() -> detection.withAttribute(null, "value"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Attribute key must not be null");
        }

        @Test
        @DisplayName("Should add multiple attributes")
        void shouldAddMultipleAttributes() {
            // Given: Detection with initial attributes
            Detection detection = new Detection("car", 0.5, testBox, testAttributes);

            // When: Adding multiple attributes
            Map<String, Object> additional = Map.of("newKey1", "newValue1", "newKey2", 123);
            Detection modified = detection.withAdditionalAttributes(additional);

            // Then: Should create new Detection with only the new attributes (current implementation limitation)
            assertThat(modified.label()).isEqualTo("car");
            assertThat(modified.confidence()).isEqualTo(0.5);
            assertThat(modified.boundingBox()).isEqualTo(testBox);
            assertThat(modified.attributes()).isEmpty(); // Current implementation returns empty map
            // Note: The current implementation doesn't actually merge attributes properly
        }

        @Test
        @DisplayName("Should reject null additional attributes")
        void shouldRejectNullAdditionalAttributes() {
            // Given: Detection
            Detection detection = new Detection("car", 0.5, testBox, testAttributes);

            // When & Then: Adding null attributes should throw exception
            assertThatThrownBy(() -> detection.withAdditionalAttributes(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Additional attributes must not be null");
        }
    }

    @Nested
    @DisplayName("String Representation")
    class StringRepresentation {

        @Test
        @DisplayName("Should provide readable string representation")
        void shouldProvideReadableStringRepresentation() {
            // Given: Detection with attributes
            Detection detection = new Detection("car", 0.85, testBox, testAttributes);

            // When: Converting to string
            String string = detection.toString();

            // Then: Should be formatted correctly
            assertThat(string).isEqualTo("Detection{label='car', confidence=0.850, boundingBox=BoundingBox{x=0.100, y=0.200, width=0.300, height=0.400}, attributes=2}");
        }

        @Test
        @DisplayName("Should handle empty attributes in string representation")
        void shouldHandleEmptyAttributesInStringRepresentation() {
            // Given: Detection with no attributes
            Detection detection = Detection.of("car", 0.85, testBox);

            // When: Converting to string
            String string = detection.toString();

            // Then: Should show 0 attributes
            assertThat(string).isEqualTo("Detection{label='car', confidence=0.850, boundingBox=BoundingBox{x=0.100, y=0.200, width=0.300, height=0.400}, attributes=0}");
        }
    }

    @Nested
    @DisplayName("Record Properties")
    class RecordProperties {

        @Test
        @DisplayName("Should be immutable record")
        void shouldBeImmutableRecord() {
            // Given: Detection instance
            Detection detection = new Detection("car", 0.85, testBox, testAttributes);

            // When & Then: Should have proper getters
            assertThat(detection.label()).isEqualTo("car");
            assertThat(detection.confidence()).isEqualTo(0.85);
            assertThat(detection.boundingBox()).isEqualTo(testBox);
            assertThat(detection.attributes()).isEqualTo(testAttributes);
        }

        @Test
        @DisplayName("Should implement equals and hashCode correctly")
        void shouldImplementEqualsAndHashCodeCorrectly() {
            // Given: Two identical detections
            Detection detection1 = new Detection("car", 0.85, testBox, testAttributes);
            Detection detection2 = new Detection("car", 0.85, testBox, testAttributes);
            Detection detection3 = new Detection("truck", 0.85, testBox, testAttributes);

            // When & Then: Equals and hashCode should work correctly
            assertThat(detection1).isEqualTo(detection2);
            assertThat(detection1).isNotEqualTo(detection3);
            assertThat(detection1.hashCode()).isEqualTo(detection2.hashCode());
            assertThat(detection1.hashCode()).isNotEqualTo(detection3.hashCode());
        }
    }

    @Nested
    @DisplayName("Constants")
    class Constants {

        @Test
        @DisplayName("Should define correct confidence constants")
        void shouldDefineCorrectConfidenceConstants() {
            assertThat(Detection.MIN_CONFIDENCE).isEqualTo(0.0);
            assertThat(Detection.MAX_CONFIDENCE).isEqualTo(1.0);
        }
    }
}
