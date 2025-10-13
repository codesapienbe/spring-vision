package io.github.codesapienbe.springvision.robotics;

import io.github.codesapienbe.springvision.core.*;
import io.github.codesapienbe.springvision.core.capabilities.ComponentVerificationCapability;
import io.github.codesapienbe.springvision.core.capabilities.DefectDetectionCapability;
import io.github.codesapienbe.springvision.core.capabilities.RoboticGuidanceCapability;
import io.github.codesapienbe.springvision.robotics.detectors.DefectDetector;
import io.github.codesapienbe.springvision.robotics.detectors.RoboticArmGuidance;
import io.github.codesapienbe.springvision.robotics.detectors.ComponentVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * VisionBackend implementation for industrial automation and robotics-focused computer vision tasks.
 *
 * <p>This backend provides specialized detection capabilities for robotics and automation scenarios including:
 * <ul>
 *   <li>Defect Detection - Identifies defects in products on production lines from video feeds</li>
 *   <li>Robotic Arm Guidance - Provides visual input to guide robotic arms for pick-and-place operations</li>
 *   <li>Component Verification - Verifies correct component usage during assembly processes</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 2.0.0
 */
@Component
@ConditionalOnProperty(prefix = "spring.vision.robotics", name = "enabled", havingValue = "true")
public class RoboticsBackend implements VisionBackend,
    DefectDetectionCapability,
    RoboticGuidanceCapability,
    ComponentVerificationCapability {

    private static final Logger logger = LoggerFactory.getLogger(RoboticsBackend.class);

    private static final String BACKEND_ID = "robotics";
    private static final String DISPLAY_NAME = "Robotics & Industrial Automation Backend";
    private static final String VERSION = "2.0.0";

    private final DefectDetector defectDetector;
    private final RoboticArmGuidance roboticArmGuidance;
    private final ComponentVerifier componentVerifier;

    /**
     * Default constructor with default configuration values.
     */
    public RoboticsBackend() {
        this(new io.github.codesapienbe.springvision.robotics.config.RoboticsProperties());
    }

    /**
     * Constructs a new RoboticsBackend with all industrial automation detectors initialized.
     */
    public RoboticsBackend(io.github.codesapienbe.springvision.robotics.config.RoboticsProperties properties) {
        this.defectDetector = new DefectDetector();
        this.roboticArmGuidance = new RoboticArmGuidance();
        this.componentVerifier = new ComponentVerifier();
        logger.info("Initialized RoboticsBackend with all industrial automation detectors");
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
        return Set.of(
            DetectionType.DEFECT,
            DetectionType.ROBOTIC_GUIDANCE,
            DetectionType.COMPONENT_VERIFICATION,
            DetectionType.OBJECT
        );
    }

    @Override
    public boolean isHealthy() {
        try {
            Class.forName("org.bytedeco.opencv.opencv_core.Mat");
            return defectDetector != null &&
                roboticArmGuidance != null &&
                componentVerifier != null;
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
        details.put("defectDetector", defectDetector != null ? "available" : "unavailable");
        details.put("roboticArmGuidance", roboticArmGuidance != null ? "available" : "unavailable");
        details.put("componentVerifier", componentVerifier != null ? "available" : "unavailable");

        if (healthy) {
            return BackendHealthInfo.healthy(BACKEND_ID, "All industrial automation detectors operational", duration, details);
        } else {
            return BackendHealthInfo.unhealthy(BACKEND_ID, "One or more automation detectors unavailable",
                "Some robotics components are not available", duration, details);
        }
    }

    // ==================== Capability Implementations ====================

    /**
     * Detects defects in manufactured products from production line images.
     *
     * <p>This method analyzes images for product defects including:
     * <ul>
     *   <li>Surface defects (scratches, dents, cracks)</li>
     *   <li>Dimensional defects (misalignments, incorrect sizes)</li>
     *   <li>Color variations and finish issues</li>
     *   <li>Missing components or features</li>
     * </ul>
     *
     * @param imageDataList list of product images to analyze for defects
     * @return list of detections representing identified defects with severity ratings
     */
    @Override
    public List<Detection> detectDefects(List<ImageData> imageDataList) {
        if (imageDataList == null || imageDataList.isEmpty()) {
            logger.warn("Received null or empty image data list for defect detection");
            return List.of();
        }

        logger.debug("Analyzing {} images for product defects", imageDataList.size());
        List<Detection> allDefects = new java.util.ArrayList<>();

        for (ImageData imageData : imageDataList) {
            try {
                List<Detection> defects = defectDetector.detect(imageData);
                allDefects.addAll(defects);
                logger.debug("Found {} defects in image", defects.size());
            } catch (Exception e) {
                logger.error("Error detecting defects in image", e);
            }
        }

        logger.info("Total defects detected: {}", allDefects.size());
        return allDefects;
    }

    /**
     * Provides visual guidance for robotic arm pick-and-place operations.
     *
     * <p>This method analyzes images to provide guidance information including:
     * <ul>
     *   <li>3D position coordinates of target objects</li>
     *   <li>Orientation angles (roll, pitch, yaw)</li>
     *   <li>Recommended grasp points for robotic grippers</li>
     *   <li>Confidence scores for position estimates</li>
     * </ul>
     *
     * @param imageDataList list of images from robotic workspace cameras
     * @return list of detections with guidance information for robotic arms
     */
    @Override
    public List<Detection> provideGuidance(List<ImageData> imageDataList) {
        if (imageDataList == null || imageDataList.isEmpty()) {
            logger.warn("Received null or empty image data list for robotic guidance");
            return List.of();
        }

        logger.debug("Analyzing {} images for robotic arm guidance", imageDataList.size());
        List<Detection> allGuidance = new java.util.ArrayList<>();

        for (ImageData imageData : imageDataList) {
            try {
                List<Detection> guidance = roboticArmGuidance.detect(imageData);
                allGuidance.addAll(guidance);
                logger.debug("Generated {} guidance detections", guidance.size());
            } catch (Exception e) {
                logger.error("Error generating robotic guidance", e);
            }
        }

        logger.info("Total guidance detections: {}", allGuidance.size());
        return allGuidance;
    }

    /**
     * Verifies correct component usage during assembly processes.
     *
     * <p>This method analyzes images to verify components including:
     * <ul>
     *   <li>Component type and model identification</li>
     *   <li>Part number verification</li>
     *   <li>Component orientation checks</li>
     *   <li>Placement correctness validation</li>
     * </ul>
     *
     * @param imageDataList list of images showing components to verify
     * @return list of detections with component verification results
     */
    @Override
    public List<Detection> verifyComponents(List<ImageData> imageDataList) {
        if (imageDataList == null || imageDataList.isEmpty()) {
            logger.warn("Received null or empty image data list for component verification");
            return List.of();
        }

        logger.debug("Analyzing {} images for component verification", imageDataList.size());
        List<Detection> allVerifications = new java.util.ArrayList<>();

        for (ImageData imageData : imageDataList) {
            try {
                List<Detection> verifications = componentVerifier.detect(imageData);
                allVerifications.addAll(verifications);
                logger.debug("Verified {} components", verifications.size());
            } catch (Exception e) {
                logger.error("Error verifying components", e);
            }
        }

        logger.info("Total component verifications: {}", allVerifications.size());
        return allVerifications;
    }

    // ==================== Detector Getters (for direct access if needed) ====================

    public DefectDetector getDefectDetector() {
        return defectDetector;
    }

    public RoboticArmGuidance getRoboticArmGuidance() {
        return roboticArmGuidance;
    }

    public ComponentVerifier getComponentVerifier() {
        return componentVerifier;
    }
}
