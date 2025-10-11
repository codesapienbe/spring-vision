package io.github.codesapienbe.springvision.robotics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Spring Vision Robotics module.
 *
 * @author Spring Vision Team
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "spring.vision.robotics")
public class RoboticsProperties {

    private boolean enabled = false;
    private DefectDetection defectDetection = new DefectDetection();
    private RoboticGuidance roboticGuidance = new RoboticGuidance();
    private ComponentVerification componentVerification = new ComponentVerification();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public DefectDetection getDefectDetection() {
        return defectDetection;
    }

    public void setDefectDetection(DefectDetection defectDetection) {
        this.defectDetection = defectDetection;
    }

    public RoboticGuidance getRoboticGuidance() {
        return roboticGuidance;
    }

    public void setRoboticGuidance(RoboticGuidance roboticGuidance) {
        this.roboticGuidance = roboticGuidance;
    }

    public ComponentVerification getComponentVerification() {
        return componentVerification;
    }

    public void setComponentVerification(ComponentVerification componentVerification) {
        this.componentVerification = componentVerification;
    }

    public static class DefectDetection {
        private double sensitivity = 0.7;
        private int minDefectSize = 50;
        private int maxDefectsPerImage = 100;

        public double getSensitivity() {
            return sensitivity;
        }

        public void setSensitivity(double sensitivity) {
            this.sensitivity = sensitivity;
        }

        public int getMinDefectSize() {
            return minDefectSize;
        }

        public void setMinDefectSize(int minDefectSize) {
            this.minDefectSize = minDefectSize;
        }

        public int getMaxDefectsPerImage() {
            return maxDefectsPerImage;
        }

        public void setMaxDefectsPerImage(int maxDefectsPerImage) {
            this.maxDefectsPerImage = maxDefectsPerImage;
        }
    }

    public static class RoboticGuidance {
        private double focalLength = 800.0;
        private double sensorWidth = 6.0;
        private boolean useStereoVision = false;
        private double confidenceThreshold = 0.6;

        public double getFocalLength() {
            return focalLength;
        }

        public void setFocalLength(double focalLength) {
            this.focalLength = focalLength;
        }

        public double getSensorWidth() {
            return sensorWidth;
        }

        public void setSensorWidth(double sensorWidth) {
            this.sensorWidth = sensorWidth;
        }

        public boolean isUseStereoVision() {
            return useStereoVision;
        }

        public void setUseStereoVision(boolean useStereoVision) {
            this.useStereoVision = useStereoVision;
        }

        public double getConfidenceThreshold() {
            return confidenceThreshold;
        }

        public void setConfidenceThreshold(double confidenceThreshold) {
            this.confidenceThreshold = confidenceThreshold;
        }
    }

    public static class ComponentVerification {
        private boolean strictMode = false;
        private double identificationThreshold = 0.7;
        private String componentDatabasePath;

        public boolean isStrictMode() {
            return strictMode;
        }

        public void setStrictMode(boolean strictMode) {
            this.strictMode = strictMode;
        }

        public double getIdentificationThreshold() {
            return identificationThreshold;
        }

        public void setIdentificationThreshold(double identificationThreshold) {
            this.identificationThreshold = identificationThreshold;
        }

        public String getComponentDatabasePath() {
            return componentDatabasePath;
        }

        public void setComponentDatabasePath(String componentDatabasePath) {
            this.componentDatabasePath = componentDatabasePath;
        }
    }
}

