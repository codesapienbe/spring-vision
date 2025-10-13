package io.github.codesapienbe.springvision.cyber.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Spring Vision Cyber Security module.
 *
 * @param enabled         whether cyber security backend is enabled
 * @param qrCode          QR code security settings
 * @param shoulderSurfing shoulder surfing detection settings
 * @param accessMonitor   access monitoring settings
 * @author Spring Vision Team
 * @since 1.1.0
 */
@Component
@ConfigurationProperties(prefix = "spring.vision.cyber")
public record CyberSecurityProperties(
    boolean enabled,
    QRCodeSettings qrCode,
    ShoulderSurfingSettings shoulderSurfing,
    AccessMonitorSettings accessMonitor
) {
    /**
     * Default constructor with default values.
     */
    public CyberSecurityProperties() {
        this(
            true,
            new QRCodeSettings(),
            new ShoulderSurfingSettings(),
            new AccessMonitorSettings()
        );
    }

    /**
     * QR code security settings.
     *
     * @param sensitivity  sensitivity level for QR code detection
     * @param validateUrls whether to validate URLs found in QR codes
     */
    public record QRCodeSettings(
        double sensitivity,
        boolean validateUrls
    ) {
        public QRCodeSettings() {
            this(0.7, true);
        }
    }

    /**
     * Shoulder surfing detection settings.
     *
     * @param enabled        whether shoulder surfing detection is enabled
     * @param sensitivity    sensitivity level for detection
     * @param alertThreshold threshold for triggering alerts
     */
    public record ShoulderSurfingSettings(
        boolean enabled,
        double sensitivity,
        int alertThreshold
    ) {
        public ShoulderSurfingSettings() {
            this(true, 0.7, 3);
        }
    }

    /**
     * Access monitoring settings.
     *
     * @param enabled               whether access monitoring is enabled
     * @param confidenceThreshold   confidence threshold for access validation
     * @param logUnauthorizedAccess whether to log unauthorized access attempts
     */
    public record AccessMonitorSettings(
        boolean enabled,
        double confidenceThreshold,
        boolean logUnauthorizedAccess
    ) {
        public AccessMonitorSettings() {
            this(true, 0.8, true);
        }
    }
}
