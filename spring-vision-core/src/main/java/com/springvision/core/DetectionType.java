package com.springvision.core;

/**
 * Enumeration of supported computer vision detection types.
 *
 * <p>This enum defines the various types of computer vision operations
 * that can be performed by Spring Vision backends. Each detection type
 * represents a specific computer vision task such as face detection,
 * object detection, text recognition, etc.</p>
 *
 * <p>The enum provides metadata about each detection type including
 * whether it's supported by default backends and any specific requirements
 * or limitations.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * VisionTemplate visionTemplate = // ... get template
 *
 * // Detect faces
 * VisionResult faceResult = visionTemplate.detect(ImageData.fromBytes(imageData), DetectionType.FACE);
 *
 * // Detect objects
 * VisionResult objectResult = visionTemplate.detect(ImageData.fromBytes(imageData), DetectionType.OBJECT);
 * }</pre>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 * @see VisionTemplate
 * @see VisionResult
 */
public enum DetectionType {

    /**
     * Face detection - identifies and locates human faces in images.
     *
     * <p>This detection type finds human faces in images and typically returns
     * bounding boxes around detected faces along with confidence scores.
     * Most vision backends support this operation.</p>
     */
    FACE("face", "Face Detection", true, "Detects human faces in images"),

    /**
     * Object detection - identifies and locates objects of various classes.
     *
     * <p>This detection type can identify multiple object classes (people, cars,
     * animals, etc.) and returns bounding boxes with class labels and confidence scores.
     * Requires more sophisticated backends like YOLO or MediaPipe.</p>
     */
    OBJECT("object", "Object Detection", false, "Detects various object classes in images"),

    /**
     * Text recognition (OCR) - extracts text from images.
     *
     * <p>This detection type performs Optical Character Recognition to extract
     * readable text from images. Requires specialized OCR backends.</p>
     */
    TEXT("text", "Text Recognition", false, "Extracts text from images using OCR"),

    /**
     * Barcode/QR code detection - identifies and decodes barcodes and QR codes.
     *
     * <p>This detection type locates and decodes various types of barcodes
     * and QR codes in images. Requires specialized barcode detection backends.</p>
     */
    BARCODE("barcode", "Barcode Detection", false, "Detects and decodes barcodes and QR codes"),

    /**
     * Landmark detection - identifies geographic or architectural landmarks.
     *
     * <p>This detection type can identify famous landmarks, buildings, and
     * geographic features in images. Requires specialized landmark recognition backends.</p>
     */
    LANDMARK("landmark", "Landmark Detection", false, "Identifies landmarks and geographic features"),

    /**
     * Pose estimation - detects human body poses and keypoints.
     *
     * <p>This detection type identifies human body poses, joints, and skeletal
     * structures in images. Requires specialized pose estimation backends like MediaPipe.</p>
     */
    POSE("pose", "Pose Estimation", false, "Detects human body poses and keypoints"),

    /**
     * Hand detection - identifies and tracks hand gestures and keypoints.
     *
     * <p>This detection type locates hands in images and can track hand gestures
     * and finger positions. Requires specialized hand tracking backends.</p>
     */
    HAND("hand", "Hand Detection", false, "Detects hands and hand gestures"),

    /**
     * Custom detection - user-defined detection types.
     *
     * <p>This detection type allows for custom, user-defined detection operations
     * that may not fit into the standard categories. Requires custom backend implementations.</p>
     */
    CUSTOM("custom", "Custom Detection", false, "User-defined custom detection operations");

    private final String code;
    private final String displayName;
    private final boolean supportedByDefault;
    private final String description;

    /**
     * Constructs a new DetectionType with the specified properties.
     *
     * @param code the unique code identifier for this detection type
     * @param displayName the human-readable display name
     * @param supportedByDefault whether this type is supported by default backends
     * @param description a detailed description of what this detection type does
     */
    DetectionType(String code, String displayName, boolean supportedByDefault, String description) {
        this.code = code;
        this.displayName = displayName;
        this.supportedByDefault = supportedByDefault;
        this.description = description;
    }

    /**
     * Gets the unique code identifier for this detection type.
     *
     * @return the code identifier
     */
    public String getCode() {
        return code;
    }

    /**
     * Gets the human-readable display name for this detection type.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if this detection type is supported by default backends.
     *
     * @return true if supported by default backends, false otherwise
     */
    public boolean isSupportedByDefault() {
        return supportedByDefault;
    }

    /**
     * Gets the detailed description of what this detection type does.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Finds a DetectionType by its code.
     *
     * @param code the code to search for
     * @return the DetectionType with the matching code, or null if not found
     */
    public static DetectionType fromCode(String code) {
        if (code == null) {
            return null;
        }

        for (DetectionType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Gets all detection types that are supported by default backends.
     *
     * @return an array of detection types supported by default backends
     */
    public static DetectionType[] getDefaultSupportedTypes() {
        return java.util.Arrays.stream(values())
            .filter(DetectionType::isSupportedByDefault)
            .toArray(DetectionType[]::new);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
