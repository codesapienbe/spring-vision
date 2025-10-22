package io.github.codesapienbe.springvision.core.capabilities;

import java.util.List;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;

/**
 * Capability interface for security threat detection.
 *
 * <p>Backends implementing this interface analyze one or more images to
 * identify potential security threats including:</p>
 * <ul>
 *   <li><b>Weapons:</b> Firearms (handguns, rifles, shotguns), knives, bladed weapons</li>
 *   <li><b>Violence:</b> Aggressive behavior, fighting, physical altercations</li>
 *   <li><b>Suspicious Objects:</b> Unattended packages, suspicious items</li>
 *   <li><b>Digital Threats:</b> Malicious QR codes, phishing URLs, tampering signs</li>
 * </ul>
 *
 * <p>The returned detections include rich metadata about detected threats:</p>
 * <ul>
 *   <li><b>threatType:</b> Category of threat (weapon, violence, suspicious_object)</li>
 *   <li><b>severity:</b> Threat severity level (LOW, MEDIUM, HIGH, CRITICAL)</li>
 *   <li><b>weaponClass:</b> Specific weapon type if applicable (handgun, knife, rifle)</li>
 *   <li><b>confidence:</b> Detection confidence score (0.0 - 1.0)</li>
 *   <li><b>description:</b> Human-readable threat description</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * List<ImageData> securityFrames = Arrays.asList(imageData);
 * List<Detection> threats = threatDetection.detectThreat(securityFrames);
 * 
 * for (Detection threat : threats) {
 *     String severity = (String) threat.attributes().get("severity");
 *     String threatType = (String) threat.attributes().get("threatType");
 *     
 *     if ("HIGH".equals(severity) || "CRITICAL".equals(severity)) {
 *         // Alert security personnel
 *         alertSecurity(threat);
 *     }
 * }
 * }</pre>
 *
 * <h2>Ethical and Legal Considerations</h2>
 * <p><b>⚠️ IMPORTANT:</b> This capability is designed for legitimate security and safety applications.
 * Users must ensure compliance with:</p>
 * <ul>
 *   <li>Local surveillance and privacy laws</li>
 *   <li>Data protection regulations (GDPR, CCPA, etc.)</li>
 *   <li>Ethical AI principles and bias mitigation</li>
 *   <li>Appropriate notice and consent requirements</li>
 * </ul>
 *
 * <p><b>Recommended Use Cases:</b></p>
 * <ul>
 *   <li>School and campus security monitoring</li>
 *   <li>Public venue safety (stadiums, transportation hubs)</li>
 *   <li>Corporate facility security</li>
 *   <li>Critical infrastructure protection</li>
 *   <li>Retail loss prevention</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 2.0.0
 * @see Detection
 * @see io.github.codesapienbe.springvision.core.DetectionType#THREAT
 */
public interface ThreatDetectionCapability {

    /**
     * Detects security threats from a list of images.
     *
     * <p>Analyzes each image for potential security threats including weapons,
     * violence indicators, and suspicious objects. Returns a unified list of
     * detections across all images with rich threat metadata.</p>
     *
     * <p><b>Returned Detection Attributes:</b></p>
     * <ul>
     *   <li>{@code threatType} (String): "weapon", "violence", "suspicious_object"</li>
     *   <li>{@code severity} (String): "LOW", "MEDIUM", "HIGH", "CRITICAL"</li>
     *   <li>{@code weaponClass} (String): Specific weapon type (e.g., "handgun", "knife")</li>
     *   <li>{@code description} (String): Human-readable threat description</li>
     * </ul>
     *
     * @param imageDataList list of images to analyze for threats
     * @return list of detections representing identified threats, empty list if no threats detected
     * @throws io.github.codesapienbe.springvision.core.exception.BaseVisionException if analysis fails
     */
    List<Detection> detectThreat(List<ImageData> imageDataList);
}
