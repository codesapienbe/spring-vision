package io.github.codesapienbe.springvision.cyber;

import io.github.codesapienbe.springvision.core.*;
import io.github.codesapienbe.springvision.core.DetectionType.*;
import io.github.codesapienbe.springvision.core.capabilities.AccessAuthenticationCapability;
import io.github.codesapienbe.springvision.core.capabilities.EavesdroppingDetectionCapability;
import io.github.codesapienbe.springvision.core.capabilities.ThreatDetectionCapability;
import io.github.codesapienbe.springvision.cyber.detectors.QRCodeSecurityDetector;
import io.github.codesapienbe.springvision.cyber.detectors.ShoulderSurfingDetector;
import io.github.codesapienbe.springvision.cyber.detectors.PhysicalAccessMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * VisionBackend implementation for cybersecurity-focused computer vision tasks.
 *
 * <p>This backend provides specialized detection capabilities for security scenarios including:
 * <ul>
 *   <li>Threat Detection - Identifies security threats like malicious QR codes</li>
 *   <li>Eavesdropping Detection - Detects shoulder surfing attempts from video streams</li>
 *   <li>Access Authentication - Verifies authorized access using face recognition</li>
 * </ul>
 *
 * <p>Unlike generic vision backends that focus on object/face detection, this backend
 * provides security-specific analysis and threat intelligence. It can use underlying
 * face detection or QR scanning capabilities but adds security context and threat assessment.</p>
 *
 * @author Spring Vision Team
 * @since 1.1.0
 */
@Component
@ConditionalOnProperty(prefix = "spring.vision.cyber", name = "enabled", havingValue = "true")
public class CyberSecurityBackend implements VisionBackend, ThreatDetectionCapability, EavesdroppingDetectionCapability, AccessAuthenticationCapability {

    private static final Logger logger = LoggerFactory.getLogger(CyberSecurityBackend.class);

    private static final String BACKEND_ID = "cyber-security";
    private static final String DISPLAY_NAME = "Cyber Security Backend";
    private static final String VERSION = "1.0.0";

    private final QRCodeSecurityDetector qrCodeDetector;
    private final ShoulderSurfingDetector shoulderSurfingDetector;
    private final PhysicalAccessMonitor physicalAccessMonitor;

    /**
     * Default constructor with default configuration values.
     */
    public CyberSecurityBackend() {
        this(new io.github.codesapienbe.springvision.cyber.config.CyberSecurityProperties());
    }

    /**
     * Constructs a new CyberSecurityBackend with all security detectors initialized.
     */
    public CyberSecurityBackend(io.github.codesapienbe.springvision.cyber.config.CyberSecurityProperties properties) {
        this.qrCodeDetector = new QRCodeSecurityDetector();
        this.shoulderSurfingDetector = new ShoulderSurfingDetector();
        this.physicalAccessMonitor = new PhysicalAccessMonitor();
        logger.info("Initialized CyberSecurityBackend with all security detectors");
    }

    @Override
    public String getBackendId() {
        return BACKEND_ID;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public Set<DetectionType> getSupportedDetectionTypes() {
        // This backend supports cyber security specific detection types
        return Set.of(
            DetectionType.THREAT,
            DetectionType.EAVESDROPPING,
            DetectionType.ACCESS_AUTH,
            DetectionType.SECURITY_INCIDENT,
            DetectionType.FACE,      // Still support face detection for access control
            DetectionType.BARCODE    // Still support barcode detection for QR code analysis
        );
    }

    @Override
    public boolean isHealthy() {
        try {
            // Verify all critical components are available
            Class.forName("com.google.zxing.BarcodeFormat");
            return qrCodeDetector != null &&
                shoulderSurfingDetector != null &&
                physicalAccessMonitor != null;
        } catch (Throwable t) {
            logger.error("Health check failed", t);
            return false;
        }
    }

    @Override
    public BackendHealthInfo getHealthInfo() {
        long start = System.currentTimeMillis();
        boolean healthy = isHealthy();
        long duration = System.currentTimeMillis() - start;

        Map<String, Object> details = new HashMap<>();
        details.put("displayName", DISPLAY_NAME);
        details.put("version", VERSION);
        details.put("qrCodeDetector", qrCodeDetector != null ? "available" : "unavailable");
        details.put("shoulderSurfingDetector", shoulderSurfingDetector != null ? "available" : "unavailable");
        details.put("physicalAccessMonitor", physicalAccessMonitor != null ? "available" : "unavailable");

        if (healthy) {
            return BackendHealthInfo.healthy(BACKEND_ID, "All security detectors operational", duration, details);
        } else {
            return BackendHealthInfo.unhealthy(BACKEND_ID, "One or more security detectors unavailable",
                "Some cyber security components are not available", duration, details);
        }
    }

    // ==================== Security-Specific Capabilities ====================

    /**
     * Detects security threats in images, such as malicious QR codes or suspicious content.
     *
     * <p>This method analyzes images for potential security threats including:
     * <ul>
     *   <li>Suspicious QR codes that may lead to phishing sites</li>
     *   <li>Malicious URLs embedded in visual content</li>
     *   <li>Security indicators that suggest tampering or forgery</li>
     * </ul>
     *
     * @param imageDataList list of images to analyze for threats
     * @return list of detections representing identified threats with severity ratings
     */
    public List<Detection> detectThreat(List<ImageData> imageDataList) {
        if (imageDataList == null || imageDataList.isEmpty()) {
            logger.warn("Received null or empty image data list for threat detection");
            return List.of();
        }

        logger.debug("Analyzing {} images for security threats", imageDataList.size());
        List<Detection> allThreats = new ArrayList<>();

        for (ImageData imageData : imageDataList) {
            // Scan for malicious QR codes
            List<Detection> qrThreats = qrCodeDetector.detectSuspiciousQRCodes(imageData);
            allThreats.addAll(qrThreats);

            // Additional threat detection logic can be added here
            // e.g., suspicious patterns, tampered images, etc.
        }

        logger.info("Detected {} potential security threats", allThreats.size());
        return allThreats;
    }

    /**
     * Detects eavesdropping attempts (shoulder surfing) from video stream frames.
     *
     * <p>Analyzes temporal sequences of video frames to identify suspicious individuals
     * attempting to view sensitive information over someone's shoulder. This method:
     * <ul>
     *   <li>Tracks face positions across multiple frames</li>
     *   <li>Identifies suspicious viewing angles and persistence</li>
     *   <li>Calculates threat levels based on position and duration</li>
     *   <li>Filters false positives using temporal analysis</li>
     * </ul>
     *
     * @param imageDataList sequential video frames to analyze
     * @return list of detections indicating potential eavesdropping attempts
     */
    public List<Detection> detectEavesdropping(List<ImageData> imageDataList) {
        if (imageDataList == null || imageDataList.isEmpty()) {
            logger.warn("Received null or empty image data list for eavesdropping detection");
            return List.of();
        }

        logger.debug("Analyzing {} video frames for eavesdropping attempts", imageDataList.size());
        List<Detection> threats = shoulderSurfingDetector.analyzeVideoStream(imageDataList);

        logger.info("Detected {} potential eavesdropping attempts", threats.size());
        return threats;
    }

    /**
     * Authenticates access by verifying identity through face recognition.
     *
     * <p>This method performs face-based authentication for physical access control:
     * <ul>
     *   <li>Detects faces in the access control image</li>
     *   <li>Matches detected faces against authorized personnel database</li>
     *   <li>Logs access attempts (both authorized and unauthorized)</li>
     *   <li>Returns authentication result with confidence score</li>
     * </ul>
     *
     * @param imageData image from access control camera
     * @return list of detections indicating authentication status (AUTHORIZED or UNAUTHORIZED)
     */
    public List<Detection> authenticateAccess(ImageData imageData) {
        if (imageData == null || imageData.data() == null) {
            logger.warn("Received null image data for access authentication");
            return List.of();
        }

        logger.debug("Authenticating access from camera image");
        List<Detection> result = physicalAccessMonitor.monitorAccess(imageData);

        boolean authorized = result.stream()
            .anyMatch(d -> "AUTHORIZED_ACCESS".equals(d.label()));

        logger.info("Access authentication completed: {}", authorized ? "AUTHORIZED" : "UNAUTHORIZED");
        return result;
    }

    /**
     * Detects potential security incidents from a batch of images.
     *
     * <p>Performs comprehensive security analysis across multiple images to identify:
     * <ul>
     *   <li>Unauthorized access attempts</li>
     *   <li>Security breaches or anomalies</li>
     *   <li>Suspicious activities requiring immediate attention</li>
     * </ul>
     *
     * @param imageDataList batch of images to analyze
     * @return list of detections representing security incidents
     */
    public List<Detection> detectSecurityIncident(List<ImageData> imageDataList) {
        if (imageDataList == null || imageDataList.isEmpty()) {
            logger.warn("Received null or empty image data list for security incident detection");
            return List.of();
        }

        logger.debug("Analyzing {} images for security incidents", imageDataList.size());
        List<Detection> incidents = new ArrayList<>();

        for (ImageData imageData : imageDataList) {
            List<Detection> accessDetections = physicalAccessMonitor.monitorAccess(imageData);

            // Filter for unauthorized access incidents
            List<Detection> unauthorizedAccess = accessDetections.stream()
                .filter(d -> "UNAUTHORIZED_ACCESS".equals(d.label()))
                .toList();

            incidents.addAll(unauthorizedAccess);
        }

        logger.info("Detected {} security incidents", incidents.size());
        return incidents;
    }

    /**
     * Generic detection method that routes to appropriate security detection capabilities.
     *
     * <p>This method provides a unified interface for all detection types supported by this backend.
     * It routes requests to the appropriate security-specific detection methods based on the type.</p>
     *
     * @param imageData     the image data to analyze (can be null for defensive handling)
     * @param detectionType the type of detection to perform (can be null for defensive handling)
     * @return list of detections, empty if input is null or invalid
     */
    public List<Detection> detect(ImageData imageData, DetectionType detectionType) {
        if (imageData == null) {
            logger.warn("Received null image data for detection");
            return List.of();
        }

        if (detectionType == null) {
            logger.warn("Received null detection type");
            return List.of();
        }

        logger.debug("Performing {} detection with cyber security backend", detectionType);

        try {
            switch (detectionType) {
                case THREAT -> {
                    // Use QR code detector for threat analysis
                    return qrCodeDetector.detectSuspiciousQRCodes(imageData);
                }
                case EAVESDROPPING -> {
                    // Use shoulder surfing detector for eavesdropping detection
                    return shoulderSurfingDetector.analyzeVideoStream(List.of(imageData));
                }
                case ACCESS_AUTH -> {
                    // Use physical access monitor for access authentication
                    return physicalAccessMonitor.monitorAccess(imageData);
                }
                case SECURITY_INCIDENT -> {
                    // Use physical access monitor to detect security incidents
                    List<Detection> accessDetections = physicalAccessMonitor.monitorAccess(imageData);
                    // Filter for unauthorized access incidents
                    return accessDetections.stream()
                        .filter(d -> "UNAUTHORIZED_ACCESS".equals(d.label()))
                        .toList();
                }
                case FACE -> {
                    // Legacy support: Use physical access monitor for face-based security detection
                    return physicalAccessMonitor.monitorAccess(imageData);
                }
                case BARCODE -> {
                    // Legacy support: Use QR code detector for barcode/QR security analysis
                    return qrCodeDetector.detectSuspiciousQRCodes(imageData);
                }
                default -> {
                    logger.warn("Unsupported detection type: {} for cyber security backend", detectionType);
                    return List.of();
                }
            }
        } catch (Exception e) {
            logger.error("Error during {} detection", detectionType, e);
            return List.of();
        }
    }

    // ==================== Component Access Methods ====================

    /**
     * Gets the QR code security detector for advanced configuration.
     *
     * @return the QR code security detector instance
     */
    public QRCodeSecurityDetector getQrCodeDetector() {
        return qrCodeDetector;
    }

    /**
     * Gets the shoulder surfing detector for advanced configuration.
     *
     * @return the shoulder surfing detector instance
     */
    public ShoulderSurfingDetector getShoulderSurfingDetector() {
        return shoulderSurfingDetector;
    }

    /**
     * Gets the physical access monitor for advanced configuration.
     *
     * @return the physical access monitor instance
     */
    public PhysicalAccessMonitor getPhysicalAccessMonitor() {
        return physicalAccessMonitor;
    }
}
