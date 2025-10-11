package io.github.codesapienbe.springvision.core.capabilities;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;

import java.util.List;

/**
 * Capability interface for robotic arm guidance using computer vision.
 *
 * <p>Backends implementing this interface analyze images to provide visual input
 * for guiding robotic arms in pick-and-place operations, assembly tasks,
 * and other automated manipulation operations.</p>
 *
 * <p>The returned detections should include guidance-specific attributes such as:
 * <ul>
 *   <li>targetPosition - 3D coordinates of the target object</li>
 *   <li>orientation - orientation angles (roll, pitch, yaw)</li>
 *   <li>graspPoints - recommended grasp points for the robotic gripper</li>
 *   <li>confidence - confidence score of the position estimate</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 2.0.0
 */
public interface RoboticGuidanceCapability {

    /**
     * Provides guidance coordinates for robotic arm operations.
     *
     * <p>Each returned detection represents a target object with its position,
     * orientation, and recommended grasp points for the robotic arm.</p>
     *
     * @param imageDataList list of images from robotic workspace cameras
     * @return list of detections with guidance information for robotic arms
     */
    List<Detection> provideGuidance(List<ImageData> imageDataList);
}

