package io.github.codesapienbe.springvision.core.capabilities;

import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.ImageData;

import java.util.List;

/**
 * Capability interface for extracting metadata from images.
 *
 * <p>Backends implementing this interface can extract various types of metadata
 * from images including:</p>
 * <ul>
 *   <li>EXIF metadata (GPS coordinates, camera settings, timestamps)</li>
 *   <li>IPTC metadata (author, copyright, keywords)</li>
 *   <li>XMP metadata (extended metadata)</li>
 *   <li>Geographic location information (latitude, longitude, altitude)</li>
 *   <li>Camera and device information</li>
 *   <li>Image properties (dimensions, color space, orientation)</li>
 * </ul>
 *
 * <p>This capability is useful for applications that need to:</p>
 * <ul>
 *   <li>Organize photos by location, date, or camera</li>
 *   <li>Extract copyright and author information</li>
 *   <li>Analyze image capture settings</li>
 *   <li>Implement location-based services</li>
 *   <li>Perform forensic analysis of images</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 1.0.2
 */
public interface MetaDataExtractionCapability {

    /**
     * Extracts metadata from the provided image.
     *
     * <p>Returns a list of detections containing extracted metadata such as:</p>
     * <ul>
     *   <li>GPS coordinates (latitude, longitude, altitude)</li>
     *   <li>Timestamp information (capture date, modification date)</li>
     *   <li>Camera information (make, model, settings)</li>
     *   <li>Author and copyright information</li>
     *   <li>Image properties (width, height, color space)</li>
     *   <li>Keywords and tags</li>
     * </ul>
     *
     * <p>The metadata is returned as Detection objects where:</p>
     * <ul>
     *   <li>The label indicates the metadata category (e.g., "gps", "camera", "exif")</li>
     *   <li>The confidence is typically 1.0 for successfully extracted metadata</li>
     *   <li>The boundingBox covers the entire image (0,0,1,1)</li>
     *   <li>The attributes map contains the actual metadata key-value pairs</li>
     * </ul>
     *
     * @param imageData The image data to process.
     * @return A list of detections representing metadata extracted from the image.
     * Each detection contains metadata attributes in the attributes map.
     */
    List<Detection> extractMetaData(ImageData imageData);
}
