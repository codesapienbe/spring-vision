package io.github.codesapienbe.springvision.core.capabilities;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

import java.util.List;

/**
 * Capability for detecting falls from human pose analysis.
 *
 * <p>This capability analyzes body orientation, pose keypoints, and movement patterns
 * to detect potential falls, particularly useful for elderly care and safety monitoring.</p>
 *
 * <h2>Use Cases</h2>
 * <ul>
 *   <li>Elderly care and assisted living facilities</li>
 *   <li>Hospital patient monitoring</li>
 *   <li>Home safety systems</li>
 *   <li>Workplace safety monitoring</li>
 *   <li>Rehabilitation centers</li>
 * </ul>
 *
 * <h2>Detection Strategy</h2>
 * <p>Fall detection analyzes:</p>
 * <ul>
 *   <li><b>Body Orientation</b> - Horizontal vs vertical body alignment</li>
 *   <li><b>Keypoint Positions</b> - Head, torso, hip relative positions</li>
 *   <li><b>Aspect Ratio</b> - Body bounding box width/height ratio</li>
 *   <li><b>Center of Mass</b> - Body center position changes</li>
 *   <li><b>Velocity</b> - Rapid downward movement (if temporal sequence)</li>
 * </ul>
 *
 * <h2>Detection Attributes</h2>
 * <p>Each Detection contains:</p>
 * <ul>
 *   <li><b>fallDetected</b> (Boolean) - Whether a fall is detected</li>
 *   <li><b>bodyOrientation</b> (String) - "standing", "sitting", "lying", "falling"</li>
 *   <li><b>confidence</b> (Double) - Fall detection confidence (0.0-1.0)</li>
 *   <li><b>riskLevel</b> (String) - "low", "medium", "high"</li>
 *   <li><b>aspectRatio</b> (Double) - Body bounding box aspect ratio</li>
 *   <li><b>headHeight</b> (Double) - Normalized head height (0.0-1.0)</li>
 *   <li><b>frameIndex</b> (Integer) - Frame index in sequence (if applicable)</li>
 * </ul>
 *
 * <h2>Implementation Notes</h2>
 * <ul>
 *   <li>Uses existing {@link PoseEstimationCapability} for keypoint detection</li>
 *   <li>Single image: Analyzes body orientation and position</li>
 *   <li>Image sequence: Can detect motion and velocity</li>
 *   <li>Threshold-based heuristics for fall classification</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @see PoseEstimationCapability
 * @since 1.0.8
 */
public interface FallDetectionCapability {

    /**
     * Detect falls from a list of images representing a temporal sequence or single frame.
     *
     * <p>For single image: Analyzes body pose and orientation.</p>
     * <p>For sequence: Analyzes pose changes and movement velocity.</p>
     *
     * <p>Returns one Detection per frame/image with label indicating body state
     * and attributes containing fall analysis metrics.</p>
     *
     * @param imageDataList list of images (single or sequence)
     * @return list of detections with fall analysis for each frame
     * @throws BaseVisionException if detection fails
     */
    List<Detection> detectFall(List<ImageData> imageDataList) throws BaseVisionException;
}
