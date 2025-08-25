package com.springvision.core;

/**
 * High-level categories for detected entities and sub-parts in images.
 *
 * <p>This enumeration provides a taxonomy for organizing detections beyond
 * coarse detection types. It allows callers and backends to express intent
 * such as focusing on sub-parts of a face (eyes, nose, mouth) or specific
 * body parts (hand, body) without introducing breaking API changes.</p>
 *
 * <p>Categories are advisory in 1.x and may be carried via {@link Detection#attributes}
 * (e.g., key "category" with the enum name). Backends may ignore unsupported
 * categories; consumers should check backend capabilities.</p>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 */
public enum DetectionCategory {

    /** Whole human face. */
    FACE,

    /** Left/right eye regions or landmarks. */
    EYE,

    /** Nose region or landmark. */
    NOSE,

    /** Mouth or lips region or landmarks. */
    MOUTH,

    /** Ear region or landmark. */
    EAR,

    /** Hand region, palm, or finger landmarks. */
    HAND,

    /** Whole human body or torso. */
    BODY,

    /** A person as an object/category. */
    PERSON,

    /** Generic object class (non-person). */
    OBJECT,

    /** Text regions. */
    TEXT,

    /** Barcode or QR code regions. */
    BARCODE,

    /** Generic landmark points or regions. */
    LANDMARK,

    /** Pose or skeletal keypoints. */
    POSE,

    /** Custom or backend-specific category. */
    CUSTOM
} 