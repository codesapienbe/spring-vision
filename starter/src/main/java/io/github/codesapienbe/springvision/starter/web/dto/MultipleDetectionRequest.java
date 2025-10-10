package io.github.codesapienbe.springvision.starter.web.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for multiple detection operations.
 *
 * <p>This DTO represents a request to perform multiple detection types on image data.
 * The image data should be provided as a byte array (typically base64 encoded),
 * and the detection types should be specified as a list of detection type codes.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * MultipleDetectionRequest request = MultipleDetectionRequest.builder()
 *     .imageData(imageBytes)
 *     .detectionTypes(List.of("face", "object"))
 *     .build();
 * }</pre>
 *
 * @author Spring Vision Team
 * @see MultipleDetectionResponse
 * @see DetectionRequest
 * @since 1.0.0
 */
public class MultipleDetectionRequest {

    /**
     * The image data to process (typically base64 encoded).
     */
    @NotNull(message = "Image data is required")
    @Size(min = 1, message = "Image data cannot be empty")
    @JsonProperty("imageData")
    private byte[] imageData;

    /**
     * List of detection types to perform.
     */
    @NotNull(message = "Detection types are required")
    @NotEmpty(message = "At least one detection type must be specified")
    @JsonProperty("detectionTypes")
    private List<String> detectionTypes;

    /**
     * Default constructor for JSON deserialization.
     */
    public MultipleDetectionRequest() {
    }

    /**
     * Constructs a new multiple detection request.
     *
     * @param imageData      the image data to process
     * @param detectionTypes the list of detection types
     */
    public MultipleDetectionRequest(byte[] imageData, List<String> detectionTypes) {
        this.imageData = imageData;
        this.detectionTypes = detectionTypes;
    }

    /**
     * Gets the image data.
     *
     * @return the image data
     */
    public byte[] getImageData() {
        return imageData;
    }

    /**
     * Sets the image data.
     *
     * @param imageData the image data
     */
    public void setImageData(byte[] imageData) {
        this.imageData = imageData;
    }

    /**
     * Gets the list of detection types.
     *
     * @return the list of detection types
     */
    public List<String> getDetectionTypes() {
        return detectionTypes;
    }

    /**
     * Sets the list of detection types.
     *
     * @param detectionTypes the list of detection types
     */
    public void setDetectionTypes(List<String> detectionTypes) {
        this.detectionTypes = detectionTypes;
    }

    /**
     * Builder for creating MultipleDetectionRequest instances.
     */
    public static class Builder {
        private byte[] imageData;
        private List<String> detectionTypes;

        /**
         * Sets the image data.
         *
         * @param imageData the image data
         * @return this builder
         */
        public Builder imageData(byte[] imageData) {
            this.imageData = imageData;
            return this;
        }

        /**
         * Sets the list of detection types.
         *
         * @param detectionTypes the list of detection types
         * @return this builder
         */
        public Builder detectionTypes(List<String> detectionTypes) {
            this.detectionTypes = detectionTypes;
            return this;
        }

        /**
         * Builds a new MultipleDetectionRequest.
         *
         * @return the built MultipleDetectionRequest
         */
        public MultipleDetectionRequest build() {
            return new MultipleDetectionRequest(imageData, detectionTypes);
        }
    }

    /**
     * Creates a new builder for MultipleDetectionRequest.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }
}
