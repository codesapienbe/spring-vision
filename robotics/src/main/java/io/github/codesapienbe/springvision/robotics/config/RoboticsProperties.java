package io.github.codesapienbe.springvision.robotics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Spring Vision Robotics module.
 *
 * @param enabled               whether robotics backend is enabled
 * @param defectDetection       defect detection settings
 * @param roboticGuidance       robotic guidance settings
 * @param componentVerification component verification settings
 * @author Spring Vision Team
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "spring.vision.robotics")
public record RoboticsProperties(
    boolean enabled,
    DefectDetection defectDetection,
    RoboticGuidance roboticGuidance,
    ComponentVerification componentVerification
) {
    /**
     * Default constructor with default values.
     */
    public RoboticsProperties() {
        this(
            false,
            new DefectDetection(),
            new RoboticGuidance(),
            new ComponentVerification()
        );
    }

    /**
     * Defect detection settings.
     *
     * @param sensitivity        sensitivity level for defect detection
     * @param minDefectSize      minimum defect size in pixels
     * @param maxDefectsPerImage maximum number of defects to detect per image
     */
    public record DefectDetection(
        double sensitivity,
        int minDefectSize,
        int maxDefectsPerImage
    ) {
        /**
         * Default constructor with default values.
         */
        public DefectDetection() {
            this(0.7, 50, 100);
        }
    }

    /**
     * Robotic guidance settings.
     *
     * @param focalLength         focal length of the camera
     * @param sensorWidth         width of the camera sensor
     * @param useStereoVision     whether to use stereo vision
     * @param confidenceThreshold confidence threshold for guidance
     */
    public record RoboticGuidance(
        double focalLength,
        double sensorWidth,
        boolean useStereoVision,
        double confidenceThreshold
    ) {
        /**
         * Default constructor with default values.
         */
        public RoboticGuidance() {
            this(800.0, 6.0, false, 0.6);
        }
    }

    /**
     * Component verification settings.
     *
     * @param strictMode              whether to use strict verification mode
     * @param identificationThreshold threshold for component identification
     * @param componentDatabasePath   path to the component database
     */
    public record ComponentVerification(
        boolean strictMode,
        double identificationThreshold,
        String componentDatabasePath
    ) {
        /**
         * Default constructor with default values.
         */
        public ComponentVerification() {
            this(false, 0.7, null);
        }
    }
}
