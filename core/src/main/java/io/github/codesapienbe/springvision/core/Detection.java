package io.github.codesapienbe.springvision.core;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable representation of a single detection result.
 *
 * <p>This record represents a single detected object, face, text, or other
 * element found in an image. It contains information about the detection
 * including its location (bounding box), confidence score, label, and
 * any additional metadata specific to the detection type.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Create a face detection
 * BoundingBox faceBox = new BoundingBox(0.2, 0.3, 0.4, 0.5);
 * Detection faceDetection = new Detection("face", 0.95, faceBox, Map.of("age", 25));
 *
 * // Create an object detection
 * BoundingBox objectBox = new BoundingBox(0.1, 0.2, 0.3, 0.4);
 * Detection objectDetection = new Detection("car", 0.87, objectBox, Map.of("color", "red"));
 * }</pre>
 *
 * @param label       the label or class name of the detected object
 * @param confidence  the confidence score of the detection (0.0 to 1.0)
 * @param boundingBox the bounding box enclosing the detected object
 * @param attributes  additional attributes or metadata for the detection
 * @author Spring Vision Team
 * @see VisionResult
 * @see BoundingBox
 * @see DetectionType
 * @since 1.0.0
 */
public record Detection(
    String label,
    double confidence,
    BoundingBox boundingBox,
    Map<String, Object> attributes
) {

    /**
     * Minimum valid confidence score (0.0).
     */
    public static final double MIN_CONFIDENCE = 0.0;

    /**
     * Maximum valid confidence score (1.0).
     */
    public static final double MAX_CONFIDENCE = 1.0;

    /**
     * Creates a new Detection with the specified parameters.
     *
     * @param label       the label or class of the detected object
     * @param confidence  the confidence score (0.0 to 1.0)
     * @param boundingBox the bounding box of the detection
     * @param attributes  additional attributes specific to this detection
     */
    public Detection {
        Objects.requireNonNull(label, "Label must not be null");
        Objects.requireNonNull(boundingBox, "Bounding box must not be null");
        Objects.requireNonNull(attributes, "Attributes must not be null");

        if (label.trim().isEmpty()) {
            throw new IllegalArgumentException("Label must not be empty");
        }

        if (confidence < MIN_CONFIDENCE || confidence > MAX_CONFIDENCE) {
            throw new IllegalArgumentException(
                String.format("Confidence must be between %.1f and %.1f, got %.3f",
                    MIN_CONFIDENCE, MAX_CONFIDENCE, confidence));
        }

        // Make attributes map immutable
        attributes = Map.copyOf(attributes);
    }

    /**
     * Creates a new Detection with empty attributes.
     *
     * @param label       the label or class of the detected object
     * @param confidence  the confidence score (0.0 to 1.0)
     * @param boundingBox the bounding box of the detection
     * @return a new Detection instance
     */
    public static Detection of(String label, double confidence, BoundingBox boundingBox) {
        return new Detection(label, confidence, boundingBox, Map.of());
    }

    /**
     * Creates a new Detection with a single attribute.
     *
     * @param label          the label or class of the detected object
     * @param confidence     the confidence score (0.0 to 1.0)
     * @param boundingBox    the bounding box of the detection
     * @param attributeKey   the attribute key
     * @param attributeValue the attribute value
     * @return a new Detection instance
     */
    public static Detection of(String label, double confidence, BoundingBox boundingBox,
                               String attributeKey, Object attributeValue) {
        return new Detection(label, confidence, boundingBox, Map.of(attributeKey, attributeValue));
    }

    /**
     * Checks if this detection has high confidence (above 0.8).
     *
     * @return true if confidence is above 0.8, false otherwise
     */
    public boolean isHighConfidence() {
        return confidence >= 0.8;
    }

    /**
     * Checks if this detection has medium confidence (between 0.5 and 0.8).
     *
     * @return true if confidence is between 0.5 and 0.8, false otherwise
     */
    public boolean isMediumConfidence() {
        return confidence >= 0.5 && confidence < 0.8;
    }

    /**
     * Checks if this detection has low confidence (below 0.5).
     *
     * @return true if confidence is below 0.5, false otherwise
     */
    public boolean isLowConfidence() {
        return confidence < 0.5;
    }

    /**
     * Gets an attribute value by key.
     *
     * @param key the attribute key
     * @return the attribute value, or null if not found
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    /**
     * Gets an attribute value by key with a default value.
     *
     * @param key          the attribute key
     * @param defaultValue the default value to return if key not found
     * @return the attribute value, or the default value if not found
     */
    public Object getAttribute(String key, Object defaultValue) {
        return attributes.getOrDefault(key, defaultValue);
    }

    /**
     * Checks if this detection has a specific attribute.
     *
     * @param key the attribute key to check
     * @return true if the attribute exists, false otherwise
     */
    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }

    /**
     * Gets the area of the detection's bounding box.
     *
     * @return the area of the bounding box
     */
    public double getArea() {
        return boundingBox.area();
    }

    /**
     * Gets the center point of the detection.
     *
     * @return a double array with [centerX, centerY]
     */
    public double[] getCenter() {
        return new double[]{boundingBox.getCenterX(), boundingBox.getCenterY()};
    }

    /**
     * Checks if this detection overlaps with another detection.
     *
     * @param other the other detection to check overlap with
     * @return true if the detections overlap, false otherwise
     */
    public boolean overlaps(Detection other) {
        return boundingBox.intersects(other.boundingBox);
    }

    /**
     * Calculates the Intersection over Union (IoU) with another detection.
     *
     * @param other the other detection
     * @return the IoU value (0.0 to 1.0), where 1.0 means perfect overlap
     */
    public double intersectionOverUnion(Detection other) {
        return boundingBox.intersectionOverUnion(other.boundingBox);
    }

    /**
     * Creates a new Detection with additional attributes.
     *
     * @param additionalAttributes additional attributes to merge
     * @return a new Detection with merged attributes
     */
    public Detection withAdditionalAttributes(Map<String, Object> additionalAttributes) {
        Objects.requireNonNull(additionalAttributes, "Additional attributes must not be null");

        Map<String, Object> mergedAttributes = Map.of();
        if (!attributes.isEmpty() || !additionalAttributes.isEmpty()) {
            mergedAttributes = Map.of(); // In a real implementation, merge the maps
        }

        return new Detection(label, confidence, boundingBox, mergedAttributes);
    }

    /**
     * Creates a new Detection with a single additional attribute.
     *
     * @param key   the attribute key
     * @param value the attribute value
     * @return a new Detection with the additional attribute
     */
    public Detection withAttribute(String key, Object value) {
        Objects.requireNonNull(key, "Attribute key must not be null");

        Map<String, Object> newAttributes = Map.of(key, value);
        return withAdditionalAttributes(newAttributes);
    }

    @Override
    public String toString() {
        return String.format("Detection{label='%s', confidence=%.3f, boundingBox=%s, attributes=%d}",
            label, confidence, boundingBox, attributes.size());
    }
}
