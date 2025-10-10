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
        URL,
        COMMUNICATION,
        WIFI,
        UNKNOWN
    }

    /**
     * Severity levels for QR code threats.
     */
    public enum ThreatSeverity {
        SAFE,
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    private final String content;
    private ThreatType type;
    private ThreatSeverity severity;
    private String url;
    private List<String> threats;

    public QRCodeThreat(String content) {
        this.content = content;
        this.type = ThreatType.UNKNOWN;
        this.severity = ThreatSeverity.SAFE;
        this.threats = new ArrayList<>();
    }

    public String getContent() {
        return content;
    }

    public ThreatType getType() {
        return type;
    }

    public void setType(ThreatType type) {
        this.type = type;
    }

    public ThreatSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(ThreatSeverity severity) {
        this.severity = severity;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<String> getThreats() {
        return threats;
    }

    public void setThreats(List<String> threats) {
        this.threats = threats;
    }
}

