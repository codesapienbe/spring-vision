package io.github.codesapienbe.springvision.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for DetectionType enum.
 * Tests all enum values, methods, and utility functions.
 */
class DetectionTypeTest {

    @Nested
    @DisplayName("Enum Constants")
    class EnumConstants {

        @Test
        @DisplayName("Should have all expected detection types defined")
        void shouldHaveAllExpectedDetectionTypes() {
            // When: Getting all enum values
            DetectionType[] allTypes = DetectionType.values();

            // Then: Should contain all expected types
            assertThat(allTypes).hasSizeGreaterThanOrEqualTo(25); // At least the basic types

            // Check for key types
            assertThat(DetectionType.valueOf("FACE")).isNotNull();
            assertThat(DetectionType.valueOf("OBJECT")).isNotNull();
            assertThat(DetectionType.valueOf("TEXT")).isNotNull();
            assertThat(DetectionType.valueOf("IMAGE_CLASSIFICATION")).isNotNull();
        }

        @Test
        @DisplayName("Should have FACE as supported by default")
        void shouldHaveFaceAsSupportedByDefault() {
            // When & Then: FACE should be supported by default
            assertThat(DetectionType.FACE.isSupportedByDefault()).isTrue();
        }

        @Test
        @DisplayName("Should have most types as not supported by default")
        void shouldHaveMostTypesAsNotSupportedByDefault() {
            // When & Then: Most types should not be supported by default
            assertThat(DetectionType.OBJECT.isSupportedByDefault()).isFalse();
            assertThat(DetectionType.TEXT.isSupportedByDefault()).isFalse();
            assertThat(DetectionType.IMAGE_CLASSIFICATION.isSupportedByDefault()).isFalse();
            assertThat(DetectionType.POSE.isSupportedByDefault()).isFalse();
        }

        @Test
        @DisplayName("Should have METADATA_EXTRACTION as supported by default")
        void shouldHaveMetadataExtractionAsSupportedByDefault() {
            // When & Then: METADATA_EXTRACTION should be supported by default
            assertThat(DetectionType.METADATA_EXTRACTION.isSupportedByDefault()).isTrue();
        }
    }

    @Nested
    @DisplayName("Properties Access")
    class PropertiesAccess {

        @ParameterizedTest
        @CsvSource({
            "FACE, face, Face Detection, Detects human faces in images",
            "OBJECT, object, Object Detection, Detects various object classes in images",
            "TEXT, text, Text Recognition, Extracts text from images using OCR",
            "IMAGE_CLASSIFICATION, image-classification, Image Classification, Classifies images into categories"
        })
        @DisplayName("Should return correct properties for detection types")
        void shouldReturnCorrectProperties(DetectionType type, String expectedCode, String expectedDisplayName, String expectedDescription) {
            // When & Then: Properties should match expected values
            assertThat(type.getCode()).isEqualTo(expectedCode);
            assertThat(type.getDisplayName()).isEqualTo(expectedDisplayName);
            assertThat(type.getDescription()).isEqualTo(expectedDescription);
        }

        @Test
        @DisplayName("Should have non-empty codes for all types")
        void shouldHaveNonEmptyCodesForAllTypes() {
            // When & Then: All types should have non-empty codes
            for (DetectionType type : DetectionType.values()) {
                assertThat(type.getCode()).isNotNull();
                assertThat(type.getCode().trim()).isNotEmpty();
            }
        }

        @Test
        @DisplayName("Should have non-empty display names for all types")
        void shouldHaveNonEmptyDisplayNamesForAllTypes() {
            // When & Then: All types should have non-empty display names
            for (DetectionType type : DetectionType.values()) {
                assertThat(type.getDisplayName()).isNotNull();
                assertThat(type.getDisplayName().trim()).isNotEmpty();
            }
        }

        @Test
        @DisplayName("Should have non-empty descriptions for all types")
        void shouldHaveNonEmptyDescriptionsForAllTypes() {
            // When & Then: All types should have non-empty descriptions
            for (DetectionType type : DetectionType.values()) {
                assertThat(type.getDescription()).isNotNull();
                assertThat(type.getDescription().trim()).isNotEmpty();
            }
        }
    }

    @Nested
    @DisplayName("fromCode() Method")
    class FromCodeMethod {

        @Test
        @DisplayName("Should find detection type by code (case insensitive)")
        void shouldFindDetectionTypeByCode() {
            // When & Then: Should find types by various cases
            assertThat(DetectionType.fromCode("face")).isEqualTo(DetectionType.FACE);
            assertThat(DetectionType.fromCode("FACE")).isEqualTo(DetectionType.FACE);
            assertThat(DetectionType.fromCode("Face")).isEqualTo(DetectionType.FACE);

            assertThat(DetectionType.fromCode("object")).isEqualTo(DetectionType.OBJECT);
            assertThat(DetectionType.fromCode("text")).isEqualTo(DetectionType.TEXT);
            assertThat(DetectionType.fromCode("image-classification")).isEqualTo(DetectionType.IMAGE_CLASSIFICATION);
        }

        @Test
        @DisplayName("Should return null for unknown codes")
        void shouldReturnNullForUnknownCodes() {
            // When & Then: Unknown codes should return null
            assertThat(DetectionType.fromCode("unknown")).isNull();
            assertThat(DetectionType.fromCode("nonexistent")).isNull();
            assertThat(DetectionType.fromCode("")).isNull();
            assertThat(DetectionType.fromCode(" ")).isNull();
        }

        @Test
        @DisplayName("Should return null for null input")
        void shouldReturnNullForNullInput() {
            // When & Then: Null input should return null
            assertThat(DetectionType.fromCode(null)).isNull();
        }

        @Test
        @DisplayName("Should handle all defined codes correctly")
        void shouldHandleAllDefinedCodesCorrectly() {
            // When & Then: All defined types should be findable by their codes
            for (DetectionType type : DetectionType.values()) {
                assertThat(DetectionType.fromCode(type.getCode())).isEqualTo(type);
                assertThat(DetectionType.fromCode(type.getCode().toUpperCase())).isEqualTo(type);
                assertThat(DetectionType.fromCode(type.getCode().toLowerCase())).isEqualTo(type);
            }
        }
    }

    @Nested
    @DisplayName("getDefaultSupportedTypes() Method")
    class GetDefaultSupportedTypesMethod {

        @Test
        @DisplayName("Should return only types supported by default")
        void shouldReturnOnlyTypesSupportedByDefault() {
            // When: Getting default supported types
            DetectionType[] defaultTypes = DetectionType.getDefaultSupportedTypes();

            // Then: Should contain only types marked as supported by default
            for (DetectionType type : defaultTypes) {
                assertThat(type.isSupportedByDefault())
                    .withFailMessage("Type %s should be supported by default but is not", type)
                    .isTrue();
            }

            // And should contain all types that are supported by default
            for (DetectionType type : DetectionType.values()) {
                if (type.isSupportedByDefault()) {
                    assertThat(defaultTypes)
                        .withFailMessage("Type %s should be in default supported types", type)
                        .contains(type);
                }
            }
        }

        @Test
        @DisplayName("Should include FACE in default supported types")
        void shouldIncludeFaceInDefaultSupportedTypes() {
            // When: Getting default supported types
            DetectionType[] defaultTypes = DetectionType.getDefaultSupportedTypes();

            // Then: Should include FACE
            assertThat(defaultTypes).contains(DetectionType.FACE);
        }

        @Test
        @DisplayName("Should include METADATA_EXTRACTION in default supported types")
        void shouldIncludeMetadataExtractionInDefaultSupportedTypes() {
            // When: Getting default supported types
            DetectionType[] defaultTypes = DetectionType.getDefaultSupportedTypes();

            // Then: Should include METADATA_EXTRACTION
            assertThat(defaultTypes).contains(DetectionType.METADATA_EXTRACTION);
        }

        @Test
        @DisplayName("Should not include OBJECT in default supported types")
        void shouldNotIncludeObjectInDefaultSupportedTypes() {
            // When: Getting default supported types
            DetectionType[] defaultTypes = DetectionType.getDefaultSupportedTypes();

            // Then: Should not include OBJECT
            assertThat(defaultTypes).doesNotContain(DetectionType.OBJECT);
        }
    }

    @Nested
    @DisplayName("toString() Method")
    class ToStringMethod {

        @Test
        @DisplayName("Should return display name in toString()")
        void shouldReturnDisplayNameInToString() {
            // When & Then: toString should return display name
            assertThat(DetectionType.FACE.toString()).isEqualTo("Face Detection");
            assertThat(DetectionType.OBJECT.toString()).isEqualTo("Object Detection");
            assertThat(DetectionType.TEXT.toString()).isEqualTo("Text Recognition");
        }

        @Test
        @DisplayName("Should have consistent toString for all types")
        void shouldHaveConsistentToStringForAllTypes() {
            // When & Then: All types should have toString equal to display name
            for (DetectionType type : DetectionType.values()) {
                assertThat(type.toString()).isEqualTo(type.getDisplayName());
            }
        }
    }

    @Nested
    @DisplayName("Enum Behavior")
    class EnumBehavior {

        @Test
        @DisplayName("Should have unique codes")
        void shouldHaveUniqueCodes() {
            // Given: All detection types
            DetectionType[] types = DetectionType.values();

            // When: Collecting all codes
            String[] codes = new String[types.length];
            for (int i = 0; i < types.length; i++) {
                codes[i] = types[i].getCode();
            }

            // Then: All codes should be unique
            assertThat(codes).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("Should have unique display names")
        void shouldHaveUniqueDisplayNames() {
            // Given: All detection types
            DetectionType[] types = DetectionType.values();

            // When: Collecting all display names
            String[] displayNames = new String[types.length];
            for (int i = 0; i < types.length; i++) {
                displayNames[i] = types[i].getDisplayName();
            }

            // Then: All display names should be unique
            assertThat(displayNames).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("Should implement equals and hashCode correctly")
        void shouldImplementEqualsAndHashCodeCorrectly() {
            // When & Then: Enum constants should equal themselves
            assertThat(DetectionType.FACE).isEqualTo(DetectionType.FACE);
            assertThat(DetectionType.FACE).isNotEqualTo(DetectionType.OBJECT);
            assertThat(DetectionType.FACE.hashCode()).isEqualTo(DetectionType.FACE.hashCode());
        }

        @Test
        @DisplayName("Should be comparable")
        void shouldBeComparable() {
            // When & Then: Enum constants should be comparable
            assertThat(DetectionType.FACE.compareTo(DetectionType.FACE)).isEqualTo(0);
            assertThat(DetectionType.FACE.compareTo(DetectionType.OBJECT)).isLessThan(0);
            assertThat(DetectionType.OBJECT.compareTo(DetectionType.FACE)).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Code Patterns")
    class CodePatterns {

        @Test
        @DisplayName("Should have codes without spaces")
        void shouldHaveCodesWithoutSpaces() {
            // When & Then: All codes should not contain spaces
            for (DetectionType type : DetectionType.values()) {
                assertThat(type.getCode())
                    .withFailMessage("Code '%s' for type %s contains spaces", type.getCode(), type)
                    .doesNotContain(" ");
            }
        }

        @Test
        @DisplayName("Should have codes in lowercase or kebab-case")
        void shouldHaveCodesInLowercaseOrKebabCase() {
            // When & Then: All codes should be lowercase or kebab-case
            for (DetectionType type : DetectionType.values()) {
                String code = type.getCode();
                assertThat(code)
                    .withFailMessage("Code '%s' for type %s should be lowercase/kebab-case", code, type)
                    .matches("^[a-z0-9-]+$");
            }
        }

        @Test
        @DisplayName("Should have reasonable code lengths")
        void shouldHaveReasonableCodeLengths() {
            // When & Then: All codes should have reasonable lengths
            for (DetectionType type : DetectionType.values()) {
                String code = type.getCode();
                assertThat(code.length())
                    .withFailMessage("Code '%s' for type %s is too short", code, type)
                    .isGreaterThanOrEqualTo(3);

                assertThat(code.length())
                    .withFailMessage("Code '%s' for type %s is too long", code, type)
                    .isLessThanOrEqualTo(25);
            }
        }
    }

    @Nested
    @DisplayName("Specialized Types")
    class SpecializedTypes {

        @Test
        @DisplayName("Should have security-related detection types")
        void shouldHaveSecurityRelatedDetectionTypes() {
            // When & Then: Should have security-related types
            assertThat(DetectionType.THREAT).isNotNull();
            assertThat(DetectionType.EAVESDROPPING).isNotNull();
            assertThat(DetectionType.ACCESS_AUTH).isNotNull();
            assertThat(DetectionType.SECURITY_INCIDENT).isNotNull();
        }

        @Test
        @DisplayName("Should have manufacturing-related detection types")
        void shouldHaveManufacturingRelatedDetectionTypes() {
            // When & Then: Should have manufacturing-related types
            assertThat(DetectionType.DEFECT).isNotNull();
            assertThat(DetectionType.ROBOTIC_GUIDANCE).isNotNull();
            assertThat(DetectionType.COMPONENT_VERIFICATION).isNotNull();
        }

        @Test
        @DisplayName("Should have biometric and emotion detection types")
        void shouldHaveBiometricAndEmotionDetectionTypes() {
            // When & Then: Should have biometric/emotion types
            assertThat(DetectionType.EMOTION).isNotNull();
            assertThat(DetectionType.DEMOGRAPHICS).isNotNull();
            assertThat(DetectionType.DEEPFAKE).isNotNull();
        }

        @Test
        @DisplayName("Should have CUSTOM type for extensibility")
        void shouldHaveCustomTypeForExtensibility() {
            // When & Then: Should have CUSTOM type
            assertThat(DetectionType.CUSTOM).isNotNull();
            assertThat(DetectionType.CUSTOM.getCode()).isEqualTo("custom");
            assertThat(DetectionType.CUSTOM.isSupportedByDefault()).isFalse();
        }
    }
}
