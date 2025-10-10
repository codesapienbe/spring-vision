package io.github.codesapienbe.springvision.cyber.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Spring Vision Cyber Security module.
 *
 * @author Spring Vision Team
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = "spring.vision.cyber")
public class CyberSecurityProperties {

    /**
     * Enable/disable cyber security features.
     */
    private boolean enabled = true;

    /**
     * QR code detection settings.
     */
    private QRCodeSettings qrCode = new QRCodeSettings();

    /**
     * Shoulder surfing detection settings.
     */
    private ShoulderSurfingSettings shoulderSurfing = new ShoulderSurfingSettings();

    /**
     * Physical access monitoring settings.
     */
    private AccessMonitorSettings accessMonitor = new AccessMonitorSettings();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public QRCodeSettings getQrCode() {
        return qrCode;
    }

    public void setQrCode(QRCodeSettings qrCode) {
        this.qrCode = qrCode;
    }

    public ShoulderSurfingSettings getShoulderSurfing() {
        return shoulderSurfing;
    }

    public void setShoulderSurfing(ShoulderSurfingSettings shoulderSurfing) {
        this.shoulderSurfing = shoulderSurfing;
    }

    public AccessMonitorSettings getAccessMonitor() {
        return accessMonitor;
    }

    public void setAccessMonitor(AccessMonitorSettings accessMonitor) {
        this.accessMonitor = accessMonitor;
    }

    public static class QRCodeSettings {
        /**
         * Sensitivity for QR code threat detection (0.0 - 1.0).
         */
        private double sensitivity = 0.7;

        /**
         * Enable URL validation.
         */
        private boolean validateUrls = true;

        public double getSensitivity() {
            return sensitivity;
        }

        public void setSensitivity(double sensitivity) {
            this.sensitivity = sensitivity;
        }

        public boolean isValidateUrls() {
            return validateUrls;
        }

        public void setValidateUrls(boolean validateUrls) {
            this.validateUrls = validateUrls;
        }
    }

    public static class ShoulderSurfingSettings {
        /**
         * Enable shoulder surfing detection.
         */
        private boolean enabled = true;

        /**
         * Sensitivity threshold (0.0 - 1.0).
         */
        private double sensitivity = 0.7;

        /**
         * Minimum number of frames to consider as persistent threat.
         */
        private int persistenceFrames = 30;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public double getSensitivity() {
            return sensitivity;
        }

        public void setSensitivity(double sensitivity) {
            this.sensitivity = sensitivity;
        }

        public int getPersistenceFrames() {
            return persistenceFrames;
        }

        public void setPersistenceFrames(int persistenceFrames) {
            this.persistenceFrames = persistenceFrames;
        }
    }

    public static class AccessMonitorSettings {
        /**
         * Enable physical access monitoring.
         */
        private boolean enabled = true;

        /**
         * Recognition confidence threshold (0.0 - 1.0).
         */
        private double confidenceThreshold = 0.75;

        /**
         * Enable access event logging.
         */
        private boolean logEvents = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public double getConfidenceThreshold() {
            return confidenceThreshold;
        }

        public void setConfidenceThreshold(double confidenceThreshold) {
            this.confidenceThreshold = confidenceThreshold;
        }

        public boolean isLogEvents() {
            return logEvents;
        }

        public void setLogEvents(boolean logEvents) {
            this.logEvents = logEvents;
        }
    }
}
