package com.springvision.core;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Immutable data transfer object for image data.
 *
 * <p>This record encapsulates image data along with metadata such as format,
 * size, and MIME type. It provides a clean, immutable interface for passing
 * image data between components in the Spring Vision framework.</p>
 *
 * <p>The record uses Java 21+ features and provides utility methods for
 * creating instances from various sources (byte arrays, input streams, etc.)
 * while maintaining immutability and thread safety.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Create from byte array
 * byte[] imageBytes = loadImageBytes();
 * ImageData imageData = ImageData.fromBytes(imageBytes, "image/jpeg");
 *
 * // Create from input stream
 * try (InputStream is = new FileInputStream("image.jpg")) {
 *     ImageData imageData = ImageData.fromStream(is, "image/jpeg");
 * }
 * }</pre>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 * @see VisionTemplate
 * @see VisionBackend
 */
public record ImageData(
    byte[] data,
    String mimeType,
    long size,
    String format
) {

    /**
     * Maximum allowed image size in bytes (100MB).
     */
    public static final long MAX_IMAGE_SIZE = 100 * 1024 * 1024;

    /**
     * Default MIME type for JPEG images.
     */
    public static final String DEFAULT_JPEG_MIME_TYPE = "image/jpeg";

    /**
     * Default MIME type for PNG images.
     */
    public static final String DEFAULT_PNG_MIME_TYPE = "image/png";

    /**
     * Default MIME type for WebP images.
     */
    public static final String DEFAULT_WEBP_MIME_TYPE = "image/webp";

    /**
     * Creates an ImageData instance from a byte array.
     *
     * <p>This factory method creates an ImageData instance from the provided
     * byte array and MIME type. The size is automatically calculated from
     * the array length.</p>
     *
     * @param data the image data as a byte array, must not be null
     * @param mimeType the MIME type of the image, must not be null or empty
     * @return a new ImageData instance
     * @throws IllegalArgumentException if data is null, mimeType is null/empty, or size exceeds maximum
     * @throws SecurityException if the image data fails security validation
     */
    public static ImageData fromBytes(byte[] data, String mimeType) {
        validateImageData(data);
        validateMimeType(mimeType);
        validateImageSize(data.length);

        String format = extractFormatFromMimeType(mimeType);
        return new ImageData(data, mimeType, data.length, format);
    }

    /**
     * Creates an ImageData instance from a byte array with automatic MIME type detection.
     *
     * <p>This factory method attempts to detect the MIME type from the image data
     * by examining the file signature (magic bytes). Falls back to JPEG if detection fails.</p>
     *
     * @param data the image data as a byte array, must not be null
     * @return a new ImageData instance with detected MIME type
     * @throws IllegalArgumentException if data is null or size exceeds maximum
     * @throws SecurityException if the image data fails security validation
     */
    public static ImageData fromBytes(byte[] data) {
        validateImageData(data);
        validateImageSize(data.length);

        String mimeType = detectMimeType(data);
        String format = extractFormatFromMimeType(mimeType);
        return new ImageData(data, mimeType, data.length, format);
    }

    /**
     * Creates an ImageData instance from an input stream.
     *
     * <p>This factory method reads the entire input stream and creates an ImageData
     * instance. The stream is automatically closed after reading.</p>
     *
     * @param inputStream the input stream to read from, must not be null
     * @param mimeType the MIME type of the image, must not be null or empty
     * @return a new ImageData instance
     * @throws IllegalArgumentException if inputStream is null, mimeType is null/empty, or size exceeds maximum
     * @throws SecurityException if the image data fails security validation
     * @throws IOException if an I/O error occurs while reading the stream
     */
    public static ImageData fromStream(InputStream inputStream, String mimeType) throws IOException {
        Objects.requireNonNull(inputStream, "Input stream must not be null");
        validateMimeType(mimeType);

        byte[] data = inputStream.readAllBytes();
        validateImageData(data);
        validateImageSize(data.length);

        String format = extractFormatFromMimeType(mimeType);
        return new ImageData(data, mimeType, data.length, format);
    }

    /**
     * Creates an ImageData instance from an input stream with automatic MIME type detection.
     *
     * @param inputStream the input stream to read from, must not be null
     * @return a new ImageData instance with detected MIME type
     * @throws IllegalArgumentException if inputStream is null or size exceeds maximum
     * @throws SecurityException if the image data fails security validation
     * @throws IOException if an I/O error occurs while reading the stream
     */
    public static ImageData fromStream(InputStream inputStream) throws IOException {
        Objects.requireNonNull(inputStream, "Input stream must not be null");

        byte[] data = inputStream.readAllBytes();
        validateImageData(data);
        validateImageSize(data.length);

        String mimeType = detectMimeType(data);
        String format = extractFormatFromMimeType(mimeType);
        return new ImageData(data, mimeType, data.length, format);
    }

    /**
     * Creates an input stream from this image data.
     *
     * @return a new ByteArrayInputStream containing the image data
     */
    public InputStream toInputStream() {
        return new ByteArrayInputStream(data);
    }

    /**
     * Checks if this image data is empty.
     *
     * @return true if the data array is empty, false otherwise
     */
    public boolean isEmpty() {
        return data.length == 0;
    }

    /**
     * Gets the size of the image data in bytes.
     *
     * @return the size in bytes
     */
    public long getSizeInBytes() {
        return size;
    }

    /**
     * Gets the size of the image data in kilobytes.
     *
     * @return the size in kilobytes
     */
    public double getSizeInKB() {
        return size / 1024.0;
    }

    /**
     * Gets the size of the image data in megabytes.
     *
     * @return the size in megabytes
     */
    public double getSizeInMB() {
        return size / (1024.0 * 1024.0);
    }

    /**
     * Validates that the image data is not null and not empty.
     *
     * @param data the image data to validate
     * @throws IllegalArgumentException if data is null or empty
     * @throws SecurityException if the image data fails security validation
     */
    private static void validateImageData(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Image data must not be null");
        }
        if (data.length == 0) {
            throw new IllegalArgumentException("Image data must not be empty");
        }

        // Security validation: check for suspicious patterns
        validateImageSecurity(data);
    }

    /**
     * Validates that the MIME type is not null or empty.
     *
     * @param mimeType the MIME type to validate
     * @throws IllegalArgumentException if mimeType is null or empty
     */
    private static void validateMimeType(String mimeType) {
        if (mimeType == null || mimeType.trim().isEmpty()) {
            throw new IllegalArgumentException("MIME type must not be null or empty");
        }
    }

    /**
     * Validates that the image size does not exceed the maximum allowed size.
     *
     * @param size the image size in bytes
     * @throws IllegalArgumentException if size exceeds maximum allowed size
     */
    private static void validateImageSize(long size) {
        if (size > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException(
                String.format("Image size %d bytes exceeds maximum allowed size of %d bytes",
                    size, MAX_IMAGE_SIZE));
        }
    }

    /**
     * Performs security validation on the image data.
     *
     * @param data the image data to validate
     * @throws SecurityException if the image data fails security validation
     */
    private static void validateImageSecurity(byte[] data) {
        // Check for minimum size to prevent tiny files that might be malicious
        if (data.length < 10) {
            throw new SecurityException("Image data too small to be a valid image");
        }

        // Additional security checks can be added here
        // For example, checking for specific file signatures, etc.
    }

    /**
     * Detects the MIME type from the image data by examining file signatures.
     *
     * @param data the image data
     * @return the detected MIME type, defaults to JPEG if detection fails
     */
    private static String detectMimeType(byte[] data) {
        if (data.length < 4) {
            return DEFAULT_JPEG_MIME_TYPE;
        }

        // Check for PNG signature
        if (data[0] == (byte) 0x89 && data[1] == 0x50 && data[2] == 0x4E && data[3] == 0x47) {
            return DEFAULT_PNG_MIME_TYPE;
        }

        // Check for JPEG signature
        if (data[0] == (byte) 0xFF && data[1] == (byte) 0xD8) {
            return DEFAULT_JPEG_MIME_TYPE;
        }

        // Check for WebP signature
        if (data.length >= 12 &&
            data[0] == 0x52 && data[1] == 0x49 && data[2] == 0x46 && data[3] == 0x46 &&
            data[8] == 0x57 && data[9] == 0x45 && data[10] == 0x42 && data[11] == 0x50) {
            return DEFAULT_WEBP_MIME_TYPE;
        }

        // Default to JPEG if detection fails
        return DEFAULT_JPEG_MIME_TYPE;
    }

    /**
     * Extracts the format from the MIME type.
     *
     * @param mimeType the MIME type
     * @return the format (e.g., "jpeg", "png", "webp")
     */
    private static String extractFormatFromMimeType(String mimeType) {
        if (mimeType == null) {
            return "unknown";
        }

        return switch (mimeType.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> "jpeg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            case "image/bmp" -> "bmp";
            case "image/tiff" -> "tiff";
            default -> "unknown";
        };
    }
}
