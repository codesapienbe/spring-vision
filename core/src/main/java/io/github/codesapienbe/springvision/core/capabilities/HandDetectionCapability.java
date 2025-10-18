package io.github.codesapienbe.springvision.core.capabilities;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;

import java.util.List;

/**
 * Capability interface for hand detection and gesture recognition.
 *
 * <p>This capability provides detection of hands in images, including bounding
 * boxes and optionally gesture classification.</p>
 *
 * <h2>Use Cases</h2>
 * <ul>
 *   <li>Gesture-based interfaces and controls</li>
 *   <li>Sign language recognition</li>
 *   <li>Virtual reality and augmented reality</li>
 *   <li>Touchless interaction systems</li>
 *   <li>Safety monitoring (proper PPE usage)</li>
 * </ul>
 *
 * <h2>Verified Models</h2>
 * <ul>
 *   <li><b>DamarJati/face-hand-YOLOv5</b> - Detects both faces and hands with YOLO</li>
 *   <li><b>lewiswatson/yolov8x-tuned-hand-gestures</b> - Hand gesture recognition</li>
 *   <li><b>dima806/hand_gestures_image_detection</b> - 18 gesture classes with 96% accuracy</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @see ObjectDetectionCapability
 * @since 1.0.0
 */
public interface HandDetectionCapability {

    /**
     * Detects hands in the provided image.
     *
     * @param imageData the image to analyze
     * @return list of detected hands with bounding boxes and attributes
     * @throws BaseVisionException if detection fails
     */
    List<Detection> detectHands(ImageData imageData) throws BaseVisionException;
}
