package io.github.codesapienbe.springvision.core;

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
 * @see VisionTemplate
 * @see VisionResult
 * @since 1.0.0
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
     * Body detection - identifies human bodies and body parts.
     *
     * <p>This detection type locates human bodies in images and can detect body parts,
     * used for pose estimation and human body analysis. Requires specialized body detection backends.</p>
     */
    BODY("body", "Body Detection", false, "Detects human bodies and body parts"),

    /**
     * Scene understanding - analyzes and classifies the overall scene in images.
     *
     * <p>This detection type performs scene classification and segmentation to understand
     * the context and composition of images. Requires specialized scene analysis backends.</p>
     */
    SCENE("scene", "Scene Understanding", false, "Analyzes and classifies image scenes"),

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
    CUSTOM("custom", "Custom Detection", false, "User-defined custom detection operations"),

    /**
     * Heart rate detection - analyze a temporal sequence of images to infer heart rate statistics.
     */
    HEART_RATE("heart-rate", "Heart Rate Detection", false, "Estimate heart-rate from image sequences"),

    /**
     * Fall detection - detect fall events from temporal image sequences.
     */
    FALL("fall", "Fall Detection", false, "Detect falls from image sequences"),

    /**
     * Stress analysis - infer stress level estimates from sequences of images.
     */
    STRESS("stress", "Stress Analysis", false, "Estimate stress levels from image sequences"),

    /**
     * Tumor classification - perform MRI brain tumor classification.
     */
    TUMOR("tumor", "Tumor Classification", false, "Classify brain tumors in MRI images"),

    /**
     * Threat detection - identifies security threats in images.
     *
     * <p>This detection type analyzes images for potential security threats including
     * malicious QR codes, suspicious patterns, tampering indicators, and other
     * security-related anomalies.</p>
     */
    THREAT("threat", "Threat Detection", false, "Detects security threats and malicious content in images"),

    /**
     * Eavesdropping detection - detects shoulder surfing and privacy violations.
     *
     * <p>This detection type analyzes video streams to identify potential eavesdropping
     * attempts, shoulder surfing behavior, and unauthorized viewing of sensitive information.</p>
     */
    EAVESDROPPING("eavesdropping", "Eavesdropping Detection", false, "Detects shoulder surfing and privacy violations"),

    /**
     * Access authentication - verifies authorized access using biometric data.
     *
     * <p>This detection type performs identity verification for access control systems
     * using face recognition, matching against authorized personnel databases.</p>
     */
    ACCESS_AUTH("access-auth", "Access Authentication", false, "Performs biometric access authentication"),

    /**
     * Security incident detection - identifies potential security breaches.
     *
     * <p>This detection type performs comprehensive security analysis to identify
     * unauthorized access attempts, security breaches, and suspicious activities
     * requiring immediate attention.</p>
     */
    SECURITY_INCIDENT("security-incident", "Security Incident Detection", false, "Detects security incidents and breaches"),

    /**
     * Defect detection - identifies defects in manufactured products.
     *
     * <p>This detection type analyzes images from production lines to identify
     * defects such as scratches, dents, cracks, misalignment, and other quality issues
     * in manufactured products.</p>
     */
    DEFECT("defect", "Defect Detection", false, "Detects defects in manufactured products"),

    /**
     * Robotic guidance - provides visual guidance for robotic arm operations.
     *
     * <p>This detection type analyzes images to provide position, orientation, and
     * grasp point information for guiding robotic arms in pick-and-place operations
     * and automated manipulation tasks.</p>
     */
    ROBOTIC_GUIDANCE("robotic-guidance", "Robotic Guidance", false, "Provides visual guidance for robotic operations"),

    /**
     * Component verification - verifies correct component usage during assembly.
     *
     * <p>This detection type analyzes images to verify that the correct components
     * are being used in assembly processes, checking component type, part numbers,
     * orientation, and placement.</p>
     */
    COMPONENT_VERIFICATION("component-verification", "Component Verification", false, "Verifies correct component usage in assembly"),

    /**
     * Metadata extraction - extracts metadata from images including EXIF, GPS, and camera information.
     *
     * <p>This detection type extracts various types of metadata embedded in images including
     * GPS coordinates, camera settings, timestamps, author information, and other EXIF/IPTC/XMP data.</p>
     */
    METADATA_EXTRACTION("metadata-extraction", "Metadata Extraction", true, "Extracts metadata from images including GPS and EXIF data");

    private final String code;
    private final String displayName;
    private final boolean supportedByDefault;
    private final String description;

    /**
     * Constructs a new DetectionType with the specified properties.
     *
     * @param code               the unique code identifier for this detection type
     * @param displayName        the human-readable display name
     * @param supportedByDefault whether this type is supported by default backends
     * @param description        a detailed description of what this detection type does
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
