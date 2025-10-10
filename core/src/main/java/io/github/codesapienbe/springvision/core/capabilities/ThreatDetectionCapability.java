package io.github.codesapienbe.springvision.core.capabilities;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;

import java.util.List;

/**
 * Capability interface for security threat detection.
 *
 * <p>Backends implementing this interface analyze one or more images to
 * identify potential security threats (e.g., malicious QR codes, phishing URLs,
 * signs of tampering) and return detections enriched with threat metadata.</p>
 */
public interface ThreatDetectionCapability {

    /**
     * Detects security threats from a list of images.
     *
     * <p>Each returned detection should include threat-specific attributes
     * (e.g., severity, threatType, indicators) in {@code Detection.attributes()}.</p>
     */
    List<Detection> detectThreat(List<ImageData> imageDataList);
}

