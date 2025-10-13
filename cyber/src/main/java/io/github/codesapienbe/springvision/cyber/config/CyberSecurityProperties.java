package io.github.codesapienbe.springvision.cyber.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Spring Vision Cyber Security module.
 *
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

    public record QRCodeSettings(
        double sensitivity,
        boolean validateUrls
    ) {
        public QRCodeSettings() {
            this(0.7, true);
        }
    }

    public record ShoulderSurfingSettings(
        boolean enabled,
        double sensitivity,
        int alertThreshold
    ) {
        public ShoulderSurfingSettings() {
            this(true, 0.7, 3);
        }
    }

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
