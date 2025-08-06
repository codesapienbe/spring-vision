package com.springvision.core;

import java.util.Objects;

/**
 * Advanced detection types for computer vision operations.
 *
 * <p>This enum extends the basic detection types with more sophisticated
 * computer vision capabilities including pose estimation, hand tracking,
 * landmark detection, and custom model support.</p>
 *
 * <p>Advanced detection types provide more granular control over vision
 * operations and enable specialized use cases that go beyond basic
 * face and object detection.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * VisionTemplate visionTemplate = // ... get template
 * ImageData imageData = ImageData.fromBytes(imageBytes);
 *
 * // Perform pose estimation
 * VisionResult poseResult = visionTemplate.detect(imageData, AdvancedDetectionType.POSE);
 *
 * // Perform hand tracking
 * VisionResult handResult = visionTemplate.detect(imageData, AdvancedDetectionType.HAND);
 * }</pre>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 * @see DetectionType
 * @see VisionTemplate
 */
public enum AdvancedDetectionType {

    /**
     * Human pose estimation and keypoint detection.
     * Detects body landmarks, joints, and skeletal structure.
     */
    POSE("pose", "Pose Estimation",
         "Detects human body pose, keypoints, and skeletal structure"),

    /**
     * Hand tracking and gesture recognition.
     * Detects hand landmarks, finger positions, and gestures.
     */
    HAND("hand", "Hand Tracking",
         "Detects hand landmarks, finger positions, and gestures"),

    /**
     * Facial landmark detection.
     * Detects detailed facial features like eyes, nose, mouth, etc.
     */
    FACE_LANDMARKS("face_landmarks", "Face Landmarks",
                   "Detects detailed facial landmarks and features"),

    /**
     * Text detection and OCR (Optical Character Recognition).
     * Detects and recognizes text in images.
     */
    TEXT_OCR("text_ocr", "Text OCR",
             "Detects and recognizes text in images"),

    /**
     * Barcode and QR code detection.
     * Detects and decodes various barcode formats.
     */
    BARCODE_QR("barcode_qr", "Barcode/QR Code",
               "Detects and decodes barcodes and QR codes"),

    /**
     * Scene understanding and classification.
     * Classifies the overall scene or environment in the image.
     */
    SCENE("scene", "Scene Classification",
          "Classifies the overall scene or environment"),

    /**
     * Image segmentation.
     * Performs pixel-level segmentation of objects and regions.
     */
    SEGMENTATION("segmentation", "Image Segmentation",
                 "Performs pixel-level segmentation of objects"),

    /**
     * Depth estimation.
     * Estimates depth information from 2D images.
     */
    DEPTH("depth", "Depth Estimation",
          "Estimates depth information from 2D images"),

    /**
     * Motion detection and tracking.
     * Detects and tracks moving objects in video sequences.
     */
    MOTION("motion", "Motion Detection",
           "Detects and tracks moving objects"),

    /**
     * Custom model inference.
     * Supports custom trained models for specialized detection tasks.
     */
    CUSTOM("custom", "Custom Model",
           "Supports custom trained models for specialized tasks");

    private final String code;
    private final String displayName;
    private final String description;

    /**
     * Creates a new AdvancedDetectionType with the specified parameters.
     *
     * @param code the unique code identifier
     * @param displayName the human-readable display name
     * @param description the detailed description
     */
    AdvancedDetectionType(String code, String displayName, String description) {
        this.code = Objects.requireNonNull(code, "Code must not be null");
        this.displayName = Objects.requireNonNull(displayName, "Display name must not be null");
        this.description = Objects.requireNonNull(description, "Description must not be null");
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
     * Gets the detailed description of this detection type.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Finds an AdvancedDetectionType by its code.
     *
     * @param code the code to search for
     * @return the matching AdvancedDetectionType, or null if not found
     */
    public static AdvancedDetectionType fromCode(String code) {
        if (code == null) {
            return null;
        }

        for (AdvancedDetectionType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Checks if this detection type requires specialized hardware or models.
     *
     * @return true if specialized resources are required, false otherwise
     */
    public boolean requiresSpecializedResources() {
        return this == DEPTH || this == SEGMENTATION || this == CUSTOM;
    }

    /**
     * Checks if this detection type is suitable for real-time processing.
     *
     * @return true if suitable for real-time processing, false otherwise
     */
    public boolean isRealTimeCapable() {
        return this == POSE || this == HAND || this == FACE_LANDMARKS ||
               this == BARCODE_QR || this == MOTION;
    }

    /**
     * Gets the estimated processing complexity for this detection type.
     *
     * @return the complexity level (1-10, where 10 is most complex)
     */
    public int getProcessingComplexity() {
        return switch (this) {
            case POSE -> 8;
            case HAND -> 7;
            case FACE_LANDMARKS -> 6;
            case TEXT_OCR -> 9;
            case BARCODE_QR -> 4;
            case SCENE -> 5;
            case SEGMENTATION -> 10;
            case DEPTH -> 9;
            case MOTION -> 6;
            case CUSTOM -> 10;
        };
    }

    /**
     * Gets the recommended minimum image resolution for this detection type.
     *
     * @return the minimum resolution in pixels (width x height)
     */
    public String getRecommendedMinResolution() {
        return switch (this) {
            case POSE -> "640x480";
            case HAND -> "320x240";
            case FACE_LANDMARKS -> "640x480";
            case TEXT_OCR -> "1024x768";
            case BARCODE_QR -> "320x240";
            case SCENE -> "512x512";
            case SEGMENTATION -> "1024x1024";
            case DEPTH -> "640x480";
            case MOTION -> "640x480";
            case CUSTOM -> "512x512";
        };
    }

    /**
     * Gets the estimated memory requirements for this detection type.
     *
     * @return the memory requirements in MB
     */
    public int getEstimatedMemoryRequirements() {
        return switch (this) {
            case POSE -> 512;
            case HAND -> 256;
            case FACE_LANDMARKS -> 256;
            case TEXT_OCR -> 1024;
            case BARCODE_QR -> 128;
            case SCENE -> 512;
            case SEGMENTATION -> 2048;
            case DEPTH -> 1024;
            case MOTION -> 512;
            case CUSTOM -> 2048;
        };
    }

    /**
     * Checks if this detection type supports batch processing.
     *
     * @return true if batch processing is supported, false otherwise
     */
    public boolean supportsBatchProcessing() {
        return this != MOTION && this != CUSTOM;
    }

    /**
     * Gets the confidence threshold recommendations for this detection type.
     *
     * @return the recommended confidence threshold (0.0 to 1.0)
     */
    public double getRecommendedConfidenceThreshold() {
        return switch (this) {
            case POSE -> 0.7;
            case HAND -> 0.6;
            case FACE_LANDMARKS -> 0.8;
            case TEXT_OCR -> 0.9;
            case BARCODE_QR -> 0.8;
            case SCENE -> 0.7;
            case SEGMENTATION -> 0.8;
            case DEPTH -> 0.7;
            case MOTION -> 0.6;
            case CUSTOM -> 0.7;
        };
    }

    @Override
    public String toString() {
        return String.format("AdvancedDetectionType{code='%s', displayName='%s'}",
                           code, displayName);
    }
}
