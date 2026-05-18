package io.github.codesapienbe.springvision.core.capabilities;

import java.util.List;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;

/**
 * Capability interface for vehicle detection.
 *
 * <p>Backends implementing this interface detect vehicles in images using the COCO object
 * detection model's vehicle classes (car, truck, bus, motorcycle, bicycle, train, boat,
 * airplane). Each returned detection includes bounding-box coordinates plus metadata such
 * as {@code vehicleType} and {@code vehicleCategory}.</p>
 *
 * <p><b>Returned Detection Attributes:</b></p>
 * <ul>
 *   <li>{@code vehicleType} (String): exact COCO class name, e.g. "car", "truck"</li>
 *   <li>{@code vehicleCategory} (String): coarse grouping – "passenger_vehicle",
 *       "commercial_vehicle", "public_transport", "two_wheeler", "aircraft",
 *       "watercraft", "rail_vehicle"</li>
 * </ul>
 */
public interface VehicleDetectionCapability {

    /**
     * Detects vehicles in the provided image.
     *
     * @param imageData the image to analyse
     * @return list of detections; empty if no vehicles found
     * @throws io.github.codesapienbe.springvision.core.exception.BaseVisionException on failure
     */
    List<Detection> detectVehicles(ImageData imageData);

    /**
     * Returns {@code true} when the underlying object-detection model is loaded
     * and vehicle detection can proceed.
     */
    boolean isVehicleDetectionModelAvailable();
}
