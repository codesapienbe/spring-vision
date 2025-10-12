package io.github.codesapienbe.springvision.starter.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for detection operations.
 *
 * <p>This DTO represents a request to perform detection operations on image data.
 * The image data should be provided as a byte array (typically base64 encoded).</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * DetectionRequest request = DetectionRequest.builder()
 *     .imageData(imageBytes)
 *     .build();
 * }</pre>
 *
 * @author Spring Vision Team
 * @see DetectionResponse
 * @since 1.0.0
 */
public class DetectionRequest {

    /**
     * The image data to process (typically base64 encoded).
     */
    @NotNull(message = "Image data is required")
    @Size(min = 1, message = "Image data cannot be empty")
    @JsonProperty("imageData")
    private byte[] imageData;

    /**
     * Default constructor for JSON deserialization.
     */
    public DetectionRequest() {
    }

    /**
     * Constructs a new detection request.
     *
     * @param imageData the image data to process
     */
    public DetectionRequest(byte[] imageData) {
        this.imageData = imageData;
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
     * Builder for creating DetectionRequest instances.
     */
    public static class Builder {
        private byte[] imageData;

        /**
         * Default constructor for the builder.
         */
        public Builder() {
        }

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
         * Builds a new DetectionRequest.
         *
         * @return the built DetectionRequest
         */
        public DetectionRequest build() {
            return new DetectionRequest(imageData);
        }
    }

    /**
     * Creates a new builder for DetectionRequest.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }
}
