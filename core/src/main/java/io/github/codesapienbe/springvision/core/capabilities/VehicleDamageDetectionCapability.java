package io.github.codesapienbe.springvision.core.capabilities;

import java.util.List;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;

/**
 * Capability interface for vehicle damage detection.
 *
 * <p>Backends implementing this interface first locate vehicles in the image and then
 * classify each vehicle region using a dedicated damage-detection ONNX model. The model
 * must be bundled at {@code /models/vehicle-damage/vehicle-damage-classifier.onnx}; when
 * absent the backend throws {@link io.github.codesapienbe.springvision.core.exception.VisionUnsupportedException}
 * rather than returning fabricated results.</p>
 *
 * <p><b>Returned Detection Attributes:</b></p>
 * <ul>
 *   <li>{@code vehicleType} (String): COCO class of the vehicle, e.g. "car"</li>
 *   <li>{@code damageType} (String): damage class predicted by the model</li>
 *   <li>{@code severity} (String): "NONE", "MINOR", "MODERATE", or "SEVERE"</li>
 *   <li>{@code vehicleLabel} (String): original vehicle detection label</li>
 *   <li>{@code vehicleConfidence} (double): confidence of the vehicle detection</li>
 * </ul>
 */
public interface VehicleDamageDetectionCapability {

    /**
     * Detects damage on vehicles found in the provided image.
     *
     * @param imageData the image to analyse
     * @return list of damage detections; empty if no vehicles or damage found
     * @throws io.github.codesapienbe.springvision.core.exception.BaseVisionException on failure
     * @throws io.github.codesapienbe.springvision.core.exception.VisionUnsupportedException
     *         when the damage detection model is not available
     */
    List<Detection> detectVehicleDamages(ImageData imageData);

    /**
     * Returns {@code true} when the damage-detection model is loaded and ready.
     */
    boolean isVehicleDamageDetectionModelAvailable();
}
