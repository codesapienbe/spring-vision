package io.github.codesapienbe.springvision.core;

import static org.assertj.core.api.Assertions.*;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Comprehensive unit tests for BoundingBox class.
 * Tests all functionality including validation, calculations, and utility methods.
 */
class BoundingBoxTest {

    @Nested
    @DisplayName("Constructor and Validation")
    class ConstructorAndValidation {

        @Test
        @DisplayName("Should create BoundingBox with valid coordinates")
        void shouldCreateBoundingBoxWithValidCoordinates() {
            // When: Creating a BoundingBox with valid coordinates
            BoundingBox bbox = new BoundingBox(0.2, 0.3, 0.4, 0.5);

            // Then: Should succeed and store correct values
            assertThat(bbox.x()).isEqualTo(0.2);
            assertThat(bbox.y()).isEqualTo(0.3);
            assertThat(bbox.width()).isEqualTo(0.4);
            assertThat(bbox.height()).isEqualTo(0.5);
        }

        @ParameterizedTest
        @CsvSource({
            "-0.1, 0.0, 0.0, 0.0, 'X coordinate must be between 0.0 and 1.0'",
            "1.1, 0.0, 0.0, 0.0, 'X coordinate must be between 0.0 and 1.0'",
            "0.0, -0.1, 0.0, 0.0, 'Y coordinate must be between 0.0 and 1.0'",
            "0.0, 1.1, 0.0, 0.0, 'Y coordinate must be between 0.0 and 1.0'",
            "0.0, 0.0, -0.1, 0.0, 'Width must be between 0.0 and 1.0'",
            "0.0, 0.0, 1.1, 0.0, 'Width must be between 0.0 and 1.0'",
            "0.0, 0.0, 0.0, -0.1, 'Height must be between 0.0 and 1.0'",
            "0.0, 0.0, 0.0, 1.1, 'Height must be between 0.0 and 1.0'"
        })
        @DisplayName("Should reject invalid coordinates and dimensions")
        void shouldRejectInvalidCoordinates(double x, double y, double width, double height, String expectedMessage) {
            // When & Then: Creating BoundingBox with invalid coordinates should throw exception
            assertThatThrownBy(() -> new BoundingBox(x, y, width, height))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(expectedMessage);
        }

        @Test
        @DisplayName("Should accept boundary values")
        void shouldAcceptBoundaryValues() {
            // When: Creating BoundingBoxes with boundary values
            BoundingBox topLeft = new BoundingBox(0.0, 0.0, 0.0, 0.0);
            BoundingBox bottomRight = new BoundingBox(1.0, 1.0, 1.0, 1.0);

            // Then: Should succeed
            assertThat(topLeft.x()).isEqualTo(0.0);
            assertThat(bottomRight.x()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {

        @Test
        @DisplayName("Should create BoundingBox from pixel coordinates")
        void shouldCreateBoundingBoxFromPixels() {
            // Given: Pixel coordinates for an 800x600 image
            int pixelX = 160, pixelY = 120, pixelWidth = 320, pixelHeight = 240;
            int imageWidth = 800, imageHeight = 600;

            // When: Creating BoundingBox from pixels
            BoundingBox bbox = BoundingBox.fromPixels(pixelX, pixelY, pixelWidth, pixelHeight, imageWidth, imageHeight);

            // Then: Should be correctly normalized
            assertThat(bbox.x()).isEqualTo(0.2); // 160/800
            assertThat(bbox.y()).isEqualTo(0.2); // 120/600
            assertThat(bbox.width()).isEqualTo(0.4); // 320/800
            assertThat(bbox.height()).isEqualTo(0.4); // 240/600
        }

        @ParameterizedTest
        @CsvSource({
            "-1, 0, 10, 10, 100, 100",
            "0, -1, 10, 10, 100, 100",
            "0, 0, -1, 10, 100, 100",
            "0, 0, 10, -1, 100, 100"
        })
        @DisplayName("Should reject negative pixel values in fromPixels")
        void shouldRejectNegativePixelValues(int pixelX, int pixelY, int pixelWidth, int pixelHeight, int imageWidth, int imageHeight) {
            assertThatThrownBy(() -> BoundingBox.fromPixels(pixelX, pixelY, pixelWidth, pixelHeight, imageWidth, imageHeight))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be non-negative");
        }

        @ParameterizedTest
        @CsvSource({
            "0, 0, 10, 10, 0, 100",
            "0, 0, 10, 10, 100, 0",
            "0, 0, 10, 10, -10, 100"
        })
        @DisplayName("Should reject invalid image dimensions in fromPixels")
        void shouldRejectInvalidImageDimensions(int pixelX, int pixelY, int pixelWidth, int pixelHeight, int imageWidth, int imageHeight) {
            assertThatThrownBy(() -> BoundingBox.fromPixels(pixelX, pixelY, pixelWidth, pixelHeight, imageWidth, imageHeight))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be positive");
        }

        @Test
        @DisplayName("Should create BoundingBox from absolute pixels")
        void shouldCreateBoundingBoxFromAbsolutePixels() {
            // When: Creating BoundingBox from absolute pixels
            BoundingBox bbox = BoundingBox.fromAbsolutePixels(100, 200, 300, 400);

            // Then: Should be normalized (using reference size of 1000)
            assertThat(bbox.x()).isEqualTo(0.1); // 100/1000
            assertThat(bbox.y()).isEqualTo(0.2); // 200/1000
            assertThat(bbox.width()).isEqualTo(0.3); // 300/1000
            assertThat(bbox.height()).isEqualTo(0.4); // 400/1000
        }

        @Test
        @DisplayName("Should reject negative values in fromAbsolutePixels")
        void shouldRejectNegativeValuesInFromAbsolutePixels() {
            assertThatThrownBy(() -> BoundingBox.fromAbsolutePixels(-10, 0, 100, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be non-negative");
        }
    }

    @Nested
    @DisplayName("Geometric Calculations")
    class GeometricCalculations {

        @Test
        @DisplayName("Should calculate right edge coordinate")
        void shouldCalculateRightEdge() {
            // Given: BoundingBox with x=0.2, width=0.4
            BoundingBox bbox = new BoundingBox(0.2, 0.3, 0.4, 0.5);

            // When: Getting right edge
            double right = bbox.getRight();

            // Then: Should be x + width
            assertThat(right).isCloseTo(0.6, Offset.offset(1e-10));
        }

        @Test
        @DisplayName("Should calculate bottom edge coordinate")
        void shouldCalculateBottomEdge() {
            // Given: BoundingBox with y=0.3, height=0.5
            BoundingBox bbox = new BoundingBox(0.2, 0.3, 0.4, 0.5);

            // When: Getting bottom edge
            double bottom = bbox.getBottom();

            // Then: Should be y + height
            assertThat(bottom).isEqualTo(0.8);
        }

        @Test
        @DisplayName("Should calculate center coordinates")
        void shouldCalculateCenterCoordinates() {
            // Given: BoundingBox
            BoundingBox bbox = new BoundingBox(0.2, 0.3, 0.4, 0.5);

            // When: Getting center coordinates
            double centerX = bbox.getCenterX();
            double centerY = bbox.getCenterY();

            // Then: Should be calculated correctly
            assertThat(centerX).isEqualTo(0.4); // 0.2 + 0.4/2
            assertThat(centerY).isEqualTo(0.55); // 0.3 + 0.5/2
        }

        @Test
        @DisplayName("Should calculate area")
        void shouldCalculateArea() {
            // Given: BoundingBox with width=0.4, height=0.5
            BoundingBox bbox = new BoundingBox(0.2, 0.3, 0.4, 0.5);

            // When: Calculating area
            double area = bbox.area();

            // Then: Should be width * height
            assertThat(area).isEqualTo(0.2); // 0.4 * 0.5
        }

        @Test
        @DisplayName("Should calculate zero area for zero dimensions")
        void shouldCalculateZeroAreaForZeroDimensions() {
            // Given: BoundingBox with zero dimensions
            BoundingBox bbox = new BoundingBox(0.2, 0.3, 0.0, 0.0);

            // When: Calculating area
            double area = bbox.area();

            // Then: Should be zero
            assertThat(area).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Point Containment")
    class PointContainment {

        @Test
        @DisplayName("Should detect point inside bounding box")
        void shouldDetectPointInsideBoundingBox() {
            // Given: BoundingBox covering (0.2,0.3) to (0.6,0.8)
            BoundingBox bbox = new BoundingBox(0.2, 0.3, 0.4, 0.5);

            // When & Then: Checking various points
            assertThat(bbox.contains(0.2, 0.3)).isTrue(); // top-left corner
            assertThat(bbox.contains(0.4, 0.55)).isTrue(); // center
            assertThat(bbox.contains(0.6, 0.8)).isTrue(); // bottom-right corner
            assertThat(bbox.contains(0.3, 0.4)).isTrue(); // inside
        }

        @Test
        @DisplayName("Should detect point outside bounding box")
        void shouldDetectPointOutsideBoundingBox() {
            // Given: BoundingBox covering (0.2,0.3) to (0.6,0.8)
            BoundingBox bbox = new BoundingBox(0.2, 0.3, 0.4, 0.5);

            // When & Then: Checking points outside
            assertThat(bbox.contains(0.1, 0.4)).isFalse(); // left of box
            assertThat(bbox.contains(0.7, 0.4)).isFalse(); // right of box
            assertThat(bbox.contains(0.4, 0.2)).isFalse(); // above box
            assertThat(bbox.contains(0.4, 0.9)).isFalse(); // below box
        }

        @Test
        @DisplayName("Should handle edge cases for point containment")
        void shouldHandleEdgeCasesForPointContainment() {
            // Given: BoundingBox
            BoundingBox bbox = new BoundingBox(0.2, 0.3, 0.4, 0.5);

            // When & Then: Edge cases (on boundaries)
            assertThat(bbox.contains(0.6, 0.4)).isTrue(); // on right edge
            assertThat(bbox.contains(0.4, 0.8)).isTrue(); // on bottom edge
            assertThat(bbox.contains(0.19, 0.4)).isFalse(); // just left of left edge
            assertThat(bbox.contains(0.4, 0.29)).isFalse(); // just above top edge
        }
    }

    @Nested
    @DisplayName("Bounding Box Intersection")
    class BoundingBoxIntersection {

        @Test
        @DisplayName("Should detect intersecting bounding boxes")
        void shouldDetectIntersectingBoundingBoxes() {
            // Given: Two overlapping bounding boxes
            BoundingBox bbox1 = new BoundingBox(0.2, 0.3, 0.4, 0.5);
            BoundingBox bbox2 = new BoundingBox(0.3, 0.4, 0.4, 0.5);

            // When & Then: Should intersect
            assertThat(bbox1.intersects(bbox2)).isTrue();
            assertThat(bbox2.intersects(bbox1)).isTrue();
        }

        @Test
        @DisplayName("Should detect non-intersecting bounding boxes")
        void shouldDetectNonIntersectingBoundingBoxes() {
            // Given: Two non-overlapping bounding boxes
            BoundingBox bbox1 = new BoundingBox(0.0, 0.0, 0.3, 0.3);
            BoundingBox bbox2 = new BoundingBox(0.7, 0.7, 0.3, 0.3);

            // When & Then: Should not intersect
            assertThat(bbox1.intersects(bbox2)).isFalse();
            assertThat(bbox2.intersects(bbox1)).isFalse();
        }

        @Test
        @DisplayName("Should handle edge-touching bounding boxes")
        void shouldHandleEdgeTouchingBoundingBoxes() {
            // Given: Two bounding boxes that touch at edges
            BoundingBox bbox1 = new BoundingBox(0.0, 0.0, 0.5, 0.5);
            BoundingBox bbox2 = new BoundingBox(0.5, 0.0, 0.5, 0.5);

            // When & Then: Should intersect (touching edges count as intersection)
            assertThat(bbox1.intersects(bbox2)).isTrue();
        }

        @Test
        @DisplayName("Should calculate intersection area")
        void shouldCalculateIntersectionArea() {
            // Given: Two overlapping bounding boxes
            // bbox1: (0.2,0.3) to (0.6,0.8)
            // bbox2: (0.4,0.5) to (0.8,0.9)
            // intersection: (0.4,0.5) to (0.6,0.8) = area 0.04
            BoundingBox bbox1 = new BoundingBox(0.2, 0.3, 0.4, 0.5);
            BoundingBox bbox2 = new BoundingBox(0.4, 0.5, 0.4, 0.4);

            // When: Calculating intersection area
            double intersectionArea = bbox1.intersectionArea(bbox2);

            // Then: Should be correct (0.2 * 0.3 = 0.06, not 0.04 as I miscalculated)
            assertThat(intersectionArea).isCloseTo(0.06, Offset.offset(1e-10)); // 0.2 * 0.3
        }

        @Test
        @DisplayName("Should return zero intersection area for non-intersecting boxes")
        void shouldReturnZeroIntersectionAreaForNonIntersectingBoxes() {
            // Given: Non-intersecting bounding boxes
            BoundingBox bbox1 = new BoundingBox(0.0, 0.0, 0.3, 0.3);
            BoundingBox bbox2 = new BoundingBox(0.7, 0.7, 0.3, 0.3);

            // When: Calculating intersection area
            double intersectionArea = bbox1.intersectionArea(bbox2);

            // Then: Should be zero
            assertThat(intersectionArea).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should calculate Intersection over Union (IoU)")
        void shouldCalculateIntersectionOverUnion() {
            // Given: Two bounding boxes with known IoU
            BoundingBox bbox1 = new BoundingBox(0.0, 0.0, 0.5, 0.5); // area = 0.25
            BoundingBox bbox2 = new BoundingBox(0.25, 0.25, 0.5, 0.5); // area = 0.25
            // Intersection area = 0.0625 (0.25*0.25), Union area = 0.4375
            // IoU = 0.0625 / 0.4375 ≈ 0.1429

            // When: Calculating IoU
            double iou = bbox1.intersectionOverUnion(bbox2);

            // Then: Should be approximately 0.143
            assertThat(iou).isCloseTo(0.142857, Offset.offset(0.001));
        }

        @Test
        @DisplayName("Should return zero IoU for non-intersecting boxes")
        void shouldReturnZeroIoUForNonIntersectingBoxes() {
            // Given: Non-intersecting bounding boxes
            BoundingBox bbox1 = new BoundingBox(0.0, 0.0, 0.3, 0.3);
            BoundingBox bbox2 = new BoundingBox(0.7, 0.7, 0.3, 0.3);

            // When: Calculating IoU
            double iou = bbox1.intersectionOverUnion(bbox2);

            // Then: Should be zero
            assertThat(iou).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return one IoU for identical boxes")
        void shouldReturnOneIoUForIdenticalBoxes() {
            // Given: Identical bounding boxes
            BoundingBox bbox1 = new BoundingBox(0.2, 0.3, 0.4, 0.5);
            BoundingBox bbox2 = new BoundingBox(0.2, 0.3, 0.4, 0.5);

            // When: Calculating IoU
            double iou = bbox1.intersectionOverUnion(bbox2);

            // Then: Should be one (perfect overlap)
            assertThat(iou).isCloseTo(1.0, Offset.offset(1e-10));
        }
    }

    @Nested
    @DisplayName("Coordinate Conversion")
    class CoordinateConversion {

        @Test
        @DisplayName("Should convert to pixel coordinates")
        void shouldConvertToPixelCoordinates() {
            // Given: Normalized bounding box and image size
            BoundingBox bbox = new BoundingBox(0.2, 0.3, 0.4, 0.5);
            int imageWidth = 800, imageHeight = 600;

            // When: Converting to pixels
            BoundingBox pixelBbox = bbox.toPixels(imageWidth, imageHeight);

            // Then: Should be correctly converted back to normalized
            assertThat(pixelBbox.x()).isEqualTo(0.2); // 160/800 = 0.2
            assertThat(pixelBbox.y()).isEqualTo(0.3); // 180/600 = 0.3
            assertThat(pixelBbox.width()).isEqualTo(0.4); // 320/800 = 0.4
            assertThat(pixelBbox.height()).isEqualTo(0.5); // 300/600 = 0.5
        }

        @Test
        @DisplayName("Should reject invalid image dimensions in toPixels")
        void shouldRejectInvalidImageDimensionsInToPixels() {
            // Given: Valid bounding box
            BoundingBox bbox = new BoundingBox(0.2, 0.3, 0.4, 0.5);

            // When & Then: Converting with invalid dimensions should throw
            assertThatThrownBy(() -> bbox.toPixels(0, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be positive");

            assertThatThrownBy(() -> bbox.toPixels(100, -50))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be positive");
        }
    }

    @Nested
    @DisplayName("String Representation")
    class StringRepresentation {

        @Test
        @DisplayName("Should provide readable string representation")
        void shouldProvideReadableStringRepresentation() {
            // Given: BoundingBox
            BoundingBox bbox = new BoundingBox(0.123, 0.456, 0.789, 0.012);

            // When: Converting to string
            String string = bbox.toString();

            // Then: Should be formatted correctly
            assertThat(string).isEqualTo("BoundingBox{x=0.123, y=0.456, width=0.789, height=0.012}");
        }

        @Test
        @DisplayName("Should round coordinates in string representation")
        void shouldRoundCoordinatesInStringRepresentation() {
            // Given: BoundingBox with more precision
            BoundingBox bbox = new BoundingBox(0.123456, 0.456789, 0.789012, 0.012345);

            // When: Converting to string
            String string = bbox.toString();

            // Then: Should be rounded to 3 decimal places
            assertThat(string).isEqualTo("BoundingBox{x=0.123, y=0.457, width=0.789, height=0.012}");
        }
    }

    @Nested
    @DisplayName("Record Properties")
    class RecordProperties {

        @Test
        @DisplayName("Should be immutable record")
        void shouldBeImmutableRecord() {
            // Given: BoundingBox instance
            BoundingBox bbox = new BoundingBox(0.2, 0.3, 0.4, 0.5);

            // When & Then: Should have proper getters
            assertThat(bbox.x()).isEqualTo(0.2);
            assertThat(bbox.y()).isEqualTo(0.3);
            assertThat(bbox.width()).isEqualTo(0.4);
            assertThat(bbox.height()).isEqualTo(0.5);
        }

        @Test
        @DisplayName("Should implement equals and hashCode correctly")
        void shouldImplementEqualsAndHashCodeCorrectly() {
            // Given: Two identical bounding boxes
            BoundingBox bbox1 = new BoundingBox(0.2, 0.3, 0.4, 0.5);
            BoundingBox bbox2 = new BoundingBox(0.2, 0.3, 0.4, 0.5);
            BoundingBox bbox3 = new BoundingBox(0.3, 0.3, 0.4, 0.5);

            // When & Then: Equals and hashCode should work correctly
            assertThat(bbox1).isEqualTo(bbox2);
            assertThat(bbox1).isNotEqualTo(bbox3);
            assertThat(bbox1.hashCode()).isEqualTo(bbox2.hashCode());
            assertThat(bbox1.hashCode()).isNotEqualTo(bbox3.hashCode());
        }
    }

    @Nested
    @DisplayName("Constants")
    class Constants {

        @Test
        @DisplayName("Should define correct coordinate constants")
        void shouldDefineCorrectCoordinateConstants() {
            assertThat(BoundingBox.MIN_COORDINATE).isEqualTo(0.0);
            assertThat(BoundingBox.MAX_COORDINATE).isEqualTo(1.0);
            assertThat(BoundingBox.MIN_DIMENSION).isEqualTo(0.0);
            assertThat(BoundingBox.MAX_DIMENSION).isEqualTo(1.0);
        }
    }
}
