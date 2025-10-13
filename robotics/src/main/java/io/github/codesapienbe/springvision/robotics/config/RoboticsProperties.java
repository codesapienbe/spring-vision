package io.github.codesapienbe.springvision.robotics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the Spring Vision Robotics module.
 *
 * @author Spring Vision Team
 * @since 2.0.0
 */
@Component
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

    public record DefectDetection(
        double sensitivity,
        int minDefectSize,
        int maxDefectsPerImage
    ) {
        public DefectDetection() {
            this(0.7, 50, 100);
        }
    }

    public record RoboticGuidance(
        double focalLength,
        double sensorWidth,
        boolean useStereoVision,
        double confidenceThreshold
    ) {
        public RoboticGuidance() {
            this(800.0, 6.0, false, 0.6);
        }
    }

    public record ComponentVerification(
        boolean strictMode,
        double identificationThreshold,
        String componentDatabasePath
    ) {
        public ComponentVerification() {
            this(false, 0.7, null);
        }
    }
}
