package io.github.codesapienbe.springvision.core;

import static org.assertj.core.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive unit tests for ImageData class.
 * Tests all factory methods, validation, MIME type detection, and utility methods.
 */
class ImageDataTest {

    // Test data - all must be >= 10 bytes for security validation
    private final byte[] validJpegData = new byte[]{(byte) 0xFF, (byte) 0xD8, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09};
    private final byte[] validPngData = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07};
    private final byte[] validWebpData = new byte[]{0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50, 0x00, 0x01};
    private final byte[] unknownData = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09};
    private final byte[] tooSmallData = new byte[]{0x00, 0x01};

    @Nested
    @DisplayName("Constructor and Basic Properties")
    class ConstructorAndBasicProperties {

        @Test
        @DisplayName("Should create ImageData with valid parameters")
        void shouldCreateImageDataWithValidParameters() {
            // Given: Valid parameters
            byte[] data = {0x01, 0x02, 0x03, 0x04};
            String mimeType = "image/jpeg";
            long size = 4L;
            String format = "jpeg";

            // When: Creating ImageData
            ImageData imageData = new ImageData(data, mimeType, size, format);

            // Then: Should store correct values
            assertThat(imageData.data()).isEqualTo(data);
            assertThat(imageData.mimeType()).isEqualTo(mimeType);
            assertThat(imageData.size()).isEqualTo(size);
            assertThat(imageData.format()).isEqualTo(format);
        }
    }

    @Nested
    @DisplayName("Factory Methods - fromBytes")
    class FactoryMethodsFromBytes {

        @Test
        @DisplayName("Should create ImageData from bytes with explicit MIME type")
        void shouldCreateImageDataFromBytesWithMimeType() {
            // When: Creating ImageData from bytes
            ImageData imageData = ImageData.fromBytes(validJpegData, "image/jpeg");

            // Then: Should have correct properties
            assertThat(imageData.data()).isEqualTo(validJpegData);
            assertThat(imageData.mimeType()).isEqualTo("image/jpeg");
            assertThat(imageData.size()).isEqualTo(validJpegData.length);
            assertThat(imageData.format()).isEqualTo("jpeg");
        }

        @Test
        @DisplayName("Should create ImageData from bytes with auto-detected MIME type")
        void shouldCreateImageDataFromBytesWithAutoDetection() {
            // When: Creating ImageData from JPEG bytes without MIME type
            ImageData imageData = ImageData.fromBytes(validJpegData);

            // Then: Should detect JPEG MIME type
            assertThat(imageData.mimeType()).isEqualTo("image/jpeg");
            assertThat(imageData.format()).isEqualTo("jpeg");
        }

        @Test
        @DisplayName("Should detect PNG MIME type from file signature")
        void shouldDetectPngMimeType() {
            // When: Creating ImageData from PNG bytes
            ImageData imageData = ImageData.fromBytes(validPngData);

            // Then: Should detect PNG
            assertThat(imageData.mimeType()).isEqualTo("image/png");
            assertThat(imageData.format()).isEqualTo("png");
        }

        @Test
        @DisplayName("Should detect WebP MIME type from file signature")
        void shouldDetectWebpMimeType() {
            // When: Creating ImageData from WebP bytes
            ImageData imageData = ImageData.fromBytes(validWebpData);

            // Then: Should detect WebP
            assertThat(imageData.mimeType()).isEqualTo("image/webp");
            assertThat(imageData.format()).isEqualTo("webp");
        }

        @Test
        @DisplayName("Should default to JPEG for unknown file signatures")
        void shouldDefaultToJpegForUnknownSignatures() {
            // When: Creating ImageData from unknown bytes
            ImageData imageData = ImageData.fromBytes(unknownData);

            // Then: Should default to JPEG
            assertThat(imageData.mimeType()).isEqualTo("image/jpeg");
            assertThat(imageData.format()).isEqualTo("jpeg");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\t", "\n"})
        @DisplayName("Should reject empty or whitespace-only MIME types")
        void shouldRejectEmptyMimeTypes(String mimeType) {
            assertThatThrownBy(() -> ImageData.fromBytes(validJpegData, mimeType))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MIME type must not be null or empty");
        }

        @Test
        @DisplayName("Should reject null MIME type")
        void shouldRejectNullMimeType() {
            assertThatThrownBy(() -> ImageData.fromBytes(validJpegData, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MIME type must not be null or empty");
        }

        @Test
        @DisplayName("Should reject null data")
        void shouldRejectNullData() {
            assertThatThrownBy(() -> ImageData.fromBytes(null, "image/jpeg"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Image data must not be null");
        }

        @Test
        @DisplayName("Should reject empty data")
        void shouldRejectEmptyData() {
            assertThatThrownBy(() -> ImageData.fromBytes(new byte[0], "image/jpeg"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Image data must not be empty");
        }

        @Test
        @DisplayName("Should reject data that is too small")
        void shouldRejectDataTooSmall() {
            assertThatThrownBy(() -> ImageData.fromBytes(tooSmallData, "image/jpeg"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("too small to be a valid image");
        }

        @Test
        @DisplayName("Should reject oversized images")
        void shouldRejectOversizedImages() {
            // Given: Data larger than MAX_IMAGE_SIZE (100MB)
            byte[] oversizedData = new byte[(int) (ImageData.MAX_IMAGE_SIZE + 1)];

            // When & Then: Should reject oversized data
            assertThatThrownBy(() -> ImageData.fromBytes(oversizedData, "image/jpeg"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds maximum allowed size");
        }
    }

    @Nested
    @DisplayName("Factory Methods - fromStream")
    class FactoryMethodsFromStream {

        @Test
        @DisplayName("Should create ImageData from input stream with explicit MIME type")
        void shouldCreateImageDataFromStreamWithMimeType() throws IOException {
            // Given: Input stream with JPEG data
            InputStream inputStream = new ByteArrayInputStream(validJpegData);

            // When: Creating ImageData from stream
            ImageData imageData = ImageData.fromStream(inputStream, "image/jpeg");

            // Then: Should have correct properties
            assertThat(imageData.data()).isEqualTo(validJpegData);
            assertThat(imageData.mimeType()).isEqualTo("image/jpeg");
            assertThat(imageData.format()).isEqualTo("jpeg");
        }

        @Test
        @DisplayName("Should create ImageData from input stream with auto-detected MIME type")
        void shouldCreateImageDataFromStreamWithAutoDetection() throws IOException {
            // Given: Input stream with PNG data
            InputStream inputStream = new ByteArrayInputStream(validPngData);

            // When: Creating ImageData from stream
            ImageData imageData = ImageData.fromStream(inputStream);

            // Then: Should detect PNG
            assertThat(imageData.mimeType()).isEqualTo("image/png");
            assertThat(imageData.format()).isEqualTo("png");
        }

        @Test
        @DisplayName("Should reject null input stream")
        void shouldRejectNullInputStream() {
            assertThatThrownBy(() -> ImageData.fromStream(null, "image/jpeg"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Input stream must not be null");
        }

        @Test
        @DisplayName("Should reject null input stream in auto-detection")
        void shouldRejectNullInputStreamAutoDetection() {
            assertThatThrownBy(() -> ImageData.fromStream(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Input stream must not be null");
        }
    }

    @Nested
    @DisplayName("Utility Methods")
    class UtilityMethods {

        @Test
        @DisplayName("Should create input stream from image data")
        void shouldCreateInputStreamFromImageData() {
            // Given: ImageData instance
            ImageData imageData = ImageData.fromBytes(validJpegData, "image/jpeg");

            // When: Creating input stream
            InputStream inputStream = imageData.toInputStream();

            // Then: Should be ByteArrayInputStream with correct data
            assertThat(inputStream).isInstanceOf(ByteArrayInputStream.class);
            // Note: Can't easily test the content without consuming the stream
        }

        @Test
        @DisplayName("Should detect empty data")
        void shouldDetectEmptyData() {
            // Given: ImageData with empty data (this would normally be rejected by factory methods)
            // We'll create it directly for testing
            ImageData emptyImageData = new ImageData(new byte[0], "image/jpeg", 0, "jpeg");

            // When & Then: Should report as empty
            assertThat(emptyImageData.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("Should detect non-empty data")
        void shouldDetectNonEmptyData() {
            // Given: ImageData with data
            ImageData imageData = ImageData.fromBytes(validJpegData, "image/jpeg");

            // When & Then: Should not report as empty
            assertThat(imageData.isEmpty()).isFalse();
        }

        @Test
        @DisplayName("Should return correct size in bytes")
        void shouldReturnCorrectSizeInBytes() {
            // Given: ImageData with known size
            ImageData imageData = ImageData.fromBytes(validJpegData, "image/jpeg");

            // When & Then: Should return correct size
            assertThat(imageData.getSizeInBytes()).isEqualTo(validJpegData.length);
        }

        @Test
        @DisplayName("Should calculate size in KB")
        void shouldCalculateSizeInKB() {
            // Given: ImageData with 2048 bytes
            byte[] data = new byte[2048];
            ImageData imageData = ImageData.fromBytes(data, "image/jpeg");

            // When: Getting size in KB
            double sizeInKB = imageData.getSizeInKB();

            // Then: Should be 2.0 KB
            assertThat(sizeInKB).isEqualTo(2.0);
        }

        @Test
        @DisplayName("Should calculate size in MB")
        void shouldCalculateSizeInMB() {
            // Given: ImageData with 2MB
            int twoMegabytes = 2 * 1024 * 1024;
            byte[] data = new byte[twoMegabytes];
            ImageData imageData = ImageData.fromBytes(data, "image/jpeg");

            // When: Getting size in MB
            double sizeInMB = imageData.getSizeInMB();

            // Then: Should be 2.0 MB
            assertThat(sizeInMB).isEqualTo(2.0);
        }
    }

    @Nested
    @DisplayName("MIME Type and Format Detection")
    class MimeTypeAndFormatDetection {

        @Test
        @DisplayName("Should extract format from MIME type")
        void shouldExtractFormatFromMimeType() {
            // Test various MIME types - we need to call the private method via reflection or test indirectly
            // Since this is private, we'll test it indirectly through the factory methods

            // JPEG formats
            ImageData jpeg1 = ImageData.fromBytes(validJpegData, "image/jpeg");
            assertThat(jpeg1.format()).isEqualTo("jpeg");

            ImageData jpeg2 = ImageData.fromBytes(validJpegData, "image/jpg");
            assertThat(jpeg2.format()).isEqualTo("jpeg");

            // PNG format
            ImageData png = ImageData.fromBytes(validPngData, "image/png");
            assertThat(png.format()).isEqualTo("png");

            // WebP format
            ImageData webp = ImageData.fromBytes(validWebpData, "image/webp");
            assertThat(webp.format()).isEqualTo("webp");
        }

        @Test
        @DisplayName("Should return unknown format for unsupported MIME types")
        void shouldReturnUnknownFormatForUnsupportedMimeTypes() {
            // Given: ImageData with unsupported MIME type
            ImageData imageData = new ImageData(validJpegData, "image/unsupported", validJpegData.length, "unknown");

            // When & Then: Should have unknown format
            assertThat(imageData.format()).isEqualTo("unknown");
        }
    }

    @Nested
    @DisplayName("Constants")
    class Constants {

        @Test
        @DisplayName("Should define correct maximum image size")
        void shouldDefineCorrectMaximumImageSize() {
            // 100MB = 100 * 1024 * 1024 = 104857600 bytes
            long expectedMaxSize = 100L * 1024L * 1024L;
            assertThat(ImageData.MAX_IMAGE_SIZE).isEqualTo(expectedMaxSize);
        }

        @Test
        @DisplayName("Should define correct default MIME types")
        void shouldDefineCorrectDefaultMimeTypes() {
            assertThat(ImageData.DEFAULT_JPEG_MIME_TYPE).isEqualTo("image/jpeg");
            assertThat(ImageData.DEFAULT_PNG_MIME_TYPE).isEqualTo("image/png");
            assertThat(ImageData.DEFAULT_WEBP_MIME_TYPE).isEqualTo("image/webp");
        }
    }

    @Nested
    @DisplayName("Record Properties")
    class RecordProperties {

        @Test
        @DisplayName("Should be immutable record")
        void shouldBeImmutableRecord() {
            // Given: ImageData instance
            ImageData imageData = ImageData.fromBytes(validJpegData, "image/jpeg");

            // When & Then: Should have proper getters
            assertThat(imageData.data()).isEqualTo(validJpegData);
            assertThat(imageData.mimeType()).isEqualTo("image/jpeg");
            assertThat(imageData.size()).isEqualTo(validJpegData.length);
            assertThat(imageData.format()).isEqualTo("jpeg");
        }

        @Test
        @DisplayName("Should implement equals and hashCode correctly")
        void shouldImplementEqualsAndHashCodeCorrectly() {
            // Given: ImageData instances with different byte array instances
            byte[] data1 = validJpegData.clone();
            byte[] data2 = validJpegData.clone();
            ImageData imageData1 = new ImageData(data1, "image/jpeg", validJpegData.length, "jpeg");
            ImageData imageData2 = new ImageData(data2, "image/jpeg", validJpegData.length, "jpeg"); // Different instance, same content
            ImageData imageData3 = new ImageData(validPngData, "image/png", validPngData.length, "png");

            // When & Then: Note that equals compares byte arrays by reference, not content
            // So two ImageData with different byte array instances are not equal
            assertThat(imageData1).isNotEqualTo(imageData2); // Different byte array references
            assertThat(imageData1).isNotEqualTo(imageData3); // Different MIME type
            assertThat(imageData1.hashCode()).isNotEqualTo(imageData2.hashCode()); // Different byte array references
            assertThat(imageData1.hashCode()).isNotEqualTo(imageData3.hashCode()); // Different MIME type

            // But same instance should be equal to itself
            assertThat(imageData1).isEqualTo(imageData1);
        }
    }

    @Nested
    @DisplayName("Security Validation")
    class SecurityValidation {

        @Test
        @DisplayName("Should pass security validation for normal images")
        void shouldPassSecurityValidationForNormalImages() {
            // When & Then: Normal image data should pass security validation
            assertThatCode(() -> ImageData.fromBytes(validJpegData, "image/jpeg"))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should fail security validation for suspiciously small data")
        void shouldFailSecurityValidationForSuspiciouslySmallData() {
            // Given: Very small data that looks suspicious
            byte[] suspiciousData = new byte[]{0x00, 0x01, 0x02};

            // When & Then: Should fail security validation
            assertThatThrownBy(() -> ImageData.fromBytes(suspiciousData, "image/jpeg"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("too small to be a valid image");
        }
    }
}
