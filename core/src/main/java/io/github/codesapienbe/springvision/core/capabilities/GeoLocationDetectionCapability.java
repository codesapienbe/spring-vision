package io.github.codesapienbe.springvision.core.capabilities;

import java.util.List;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;

/**
 * Capability interface for geolocation detection from images.
 *
 * <p>Backends implementing this interface can extract geographic location information
 * from images using various techniques such as:
 * <ul>
 *   <li>EXIF metadata (GPS coordinates, location tags)</li>
 *   <li>Visual landmark recognition</li>
 *   <li>Reverse geocoding</li>
 *   <li>Image-based location inference</li>
 * </ul>
 *
 * <p>This capability is useful for applications that need to determine where
 * a photo was taken, organize photos by location, or provide location-based services.</p>
 *
 * @author Spring Vision Team
 * @since 1.0.2
 */
public interface GeoLocationDetectionCapability {

    /**
     * Detects geographic location information from the provided image.
     *
     * <p>Returns a list of detections containing location information such as:
     * <ul>
     *   <li>GPS coordinates (latitude, longitude)</li>
     *   <li>Location name (city, country)</li>
     *   <li>Altitude</li>
     *   <li>Timestamp</li>
     *   <li>Address information</li>
     * </ul>
     *
     * @param imageData The image data to process.
     * @return A list of detections representing geographic locations found in or associated with the image.
     * Each detection contains location attributes in the attributes map.
     */
    List<Detection> detectGeoLocation(ImageData imageData);
}
