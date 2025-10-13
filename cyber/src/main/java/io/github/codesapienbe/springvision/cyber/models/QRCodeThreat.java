package io.github.codesapienbe.springvision.cyber.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a potential security threat found in a QR code.
 *
 * @author Spring Vision Team
 * @since 1.1.0
 */
public class QRCodeThreat {

    /**
     * Types of threats that can be found in QR codes.
     */
    public enum ThreatType {
        /**
         * URL-based threat.
         */
        URL,
        /**
         * Communication-related threat.
         */
        COMMUNICATION,
        /**
         * WiFi configuration threat.
         */
        WIFI,
        /**
         * Unknown threat type.
         */
        UNKNOWN
    }

    /**
     * Severity levels for QR code threats.
     */
    public enum ThreatSeverity {
        /**
         * Safe, no threat detected.
         */
        SAFE,
        /**
         * Low severity threat.
         */
        LOW,
        /**
         * Medium severity threat.
         */
        MEDIUM,
        /**
         * High severity threat.
         */
        HIGH,
        /**
         * Critical severity threat.
         */
        CRITICAL
    }

    private final String content;
    private ThreatType type;
    private ThreatSeverity severity;
    private String url;
    private List<String> threats;

    /**
     * Constructs a new QRCodeThreat with the given content.
     *
     * @param content The raw content decoded from the QR code.
     */
    public QRCodeThreat(String content) {
        this.content = content;
        this.type = ThreatType.UNKNOWN;
        this.severity = ThreatSeverity.SAFE;
        this.threats = new ArrayList<>();
    }

    /**
     * Gets the raw content decoded from the QR code.
     *
     * @return The raw content.
     */
    public String getContent() {
        return content;
    }

    /**
     * Gets the type of threat identified.
     *
     * @return The threat type.
     */
    public ThreatType getType() {
        return type;
    }

    /**
     * Sets the type of threat identified.
     *
     * @param type The threat type to set.
     */
    public void setType(ThreatType type) {
        this.type = type;
    }

    /**
     * Gets the severity level of the identified threat.
     *
     * @return The threat severity.
     */
    public ThreatSeverity getSeverity() {
        return severity;
    }

    /**
     * Sets the severity level of the identified threat.
     *
     * @param severity The threat severity to set.
     */
    public void setSeverity(ThreatSeverity severity) {
        this.severity = severity;
    }

    /**
     * Gets the URL associated with the threat, if applicable.
     *
     * @return The URL, or null if the threat is not URL-based.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the URL associated with the threat.
     *
     * @param url The URL to set.
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Gets the list of specific threats or reasons for the severity rating.
     *
     * @return A list of threat descriptions.
     */
    public List<String> getThreats() {
        return threats;
    }

    /**
     * Sets the list of specific threats or reasons for the severity rating.
     *
     * @param threats The list of threats to set.
     */
    public void setThreats(List<String> threats) {
        this.threats = threats;
    }
}
