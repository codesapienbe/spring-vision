package io.github.codesapienbe.springvision.health.api;

/**
 * Enumerates the types of brain tumors that can be classified by the health module,
 * along with labels for non-tumor or indeterminate results.
 *
 * @author Spring Vision Team
 * @since 1.1.0
 */
public enum TumorType {
    /**
     * A glioma is a type of tumor that starts in the glial cells of the brain or the spine.
     */
    GLIOMA,

    /**
     * A meningioma is a tumor that forms on membranes that cover the brain and spinal cord
     * just inside the skull.
     */
    MENINGIOMA,

    /**
     * A pituitary tumor is an abnormal growth in the pituitary gland.
     */
    PITUITARY,

    /**
     * Indicates that no tumor was detected in the analysis.
     */
    NO_TUMOR,

    /**
     * Indicates that the tumor type could not be determined from the provided image.
     */
    UNKNOWN
}
