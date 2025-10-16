package io.github.codesapienbe.springvision.core.health;

import io.github.codesapienbe.springvision.core.ImageData;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents an MRI image with medical metadata for brain tumor analysis.
 *
 * @param imageData   The raw image data
 * @param patientId   Unique identifier for the patient
 * @param studyDate   Date when the MRI study was performed
 * @param sequence    MRI sequence type (e.g., "T1", "T2", "FLAIR", "T1-Gd")
 * @param sliceNumber The slice number within the MRI series
 * @param metadata    Additional medical metadata
 * @author Spring Vision Team
 * @since 1.1.0
 */
public record MRIImage(
    ImageData imageData,
    String patientId,
    LocalDateTime studyDate,
    String sequence,
    int sliceNumber,
    Map<String, Object> metadata
) {

    /**
     * Creates an MRIImage with validation.
     */
    public MRIImage {
        if (imageData == null) {
            throw new IllegalArgumentException("Image data cannot be null");
        }
        if (patientId == null || patientId.trim().isEmpty()) {
            throw new IllegalArgumentException("Patient ID cannot be null or empty");
        }
        if (studyDate == null) {
            throw new IllegalArgumentException("Study date cannot be null");
        }
        if (sequence == null || sequence.trim().isEmpty()) {
            throw new IllegalArgumentException("Sequence cannot be null or empty");
        }
        if (sliceNumber < 0) {
            throw new IllegalArgumentException("Slice number cannot be negative");
        }
    }

    /**
     * Creates a basic MRI image with current timestamp.
     */
    public static MRIImage of(ImageData imageData, String patientId, String sequence, int sliceNumber) {
        return new MRIImage(imageData, patientId, LocalDateTime.now(), sequence, sliceNumber, Map.of());
    }

    /**
     * Creates an MRI image with additional metadata.
     */
    public static MRIImage withMetadata(ImageData imageData, String patientId, String sequence,
                                        int sliceNumber, Map<String, Object> metadata) {
        return new MRIImage(imageData, patientId, LocalDateTime.now(), sequence, sliceNumber, metadata);
    }

    /**
     * Gets the image format.
     */
    public String getFormat() {
        return imageData.format();
    }

    /**
     * Gets the image size in bytes.
     */
    public long getSize() {
        return imageData.size();
    }

    /**
     * Gets the MIME type.
     */
    public String getMimeType() {
        return imageData.mimeType();
    }
}
