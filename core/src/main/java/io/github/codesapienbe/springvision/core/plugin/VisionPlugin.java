package io.github.codesapienbe.springvision.core.plugin;

import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.VisionResult;

import java.util.Map;

/**
 * Plugin interface for extending Spring Vision functionality.
 *
 * <p>This interface defines the contract for vision plugins that can extend
 * the core framework with custom detection algorithms, preprocessing steps,
 * post-processing filters, and specialized vision operations.</p>
 *
 * <p>Plugins can be dynamically loaded and provide a way to extend the
 * framework without modifying the core codebase. They support both
 * synchronous and asynchronous operations.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @Component
 * public class CustomFaceDetectionPlugin implements VisionPlugin {
 *     @Override
 *     public String getPluginId() {
 *         return "custom-face-detection";
 *     }
 *
 *     @Override
 *     public VisionResult process(ImageData imageData, Map<String, Object> parameters) {
 *         // Custom face detection logic
 *         return new VisionResult(...);
 *     }
 * }
 * }</pre>
 *
 * @author Spring Vision Team
 * @see ImageData
 * @see VisionResult
 * @since 1.0.0
 */
public interface VisionPlugin {

    /**
     * Gets the unique identifier for this plugin.
     *
     * @return the plugin identifier
     */
    String getPluginId();

    /**
     * Gets the display name for this plugin.
     *
     * @return the display name
     */
    String getDisplayName();

    /**
     * Gets the version of this plugin.
     *
     * @return the plugin version
     */
    String getVersion();

    /**
     * Gets the description of this plugin.
     *
     * @return the plugin description
     */
    String getDescription();

    /**
     * Gets the author of this plugin.
     *
     * @return the plugin author
     */
    String getAuthor();

    /**
     * Gets the plugin type.
     *
     * @return the plugin type
     */
    PluginType getPluginType();

    /**
     * Gets the supported detection types for this plugin.
     *
     * @return array of supported detection types
     */
    String[] getSupportedDetectionTypes();

    /**
     * Checks if this plugin supports the specified detection type.
     *
     * @param detectionType the detection type to check
     * @return true if supported, false otherwise
     */
    default boolean supportsDetectionType(String detectionType) {
        if (detectionType == null) {
            return false;
        }

        for (String supportedType : getSupportedDetectionTypes()) {
            if (supportedType.equals(detectionType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the configuration parameters supported by this plugin.
     *
     * @return map of parameter names to their descriptions
     */
    Map<String, String> getSupportedParameters();

    /**
     * Gets the default parameter values for this plugin.
     *
     * @return map of parameter names to their default values
     */
    Map<String, Object> getDefaultParameters();

    /**
     * Processes the image data using this plugin.
     *
     * @param imageData  the image data to process
     * @param parameters the processing parameters
     * @return the processing result
     * @throws PluginException if processing fails
     */
    VisionResult process(ImageData imageData, Map<String, Object> parameters)
        throws PluginException;

    /**
     * Processes the image data using default parameters.
     *
     * @param imageData the image data to process
     * @return the processing result
     * @throws PluginException if processing fails
     */
    default VisionResult process(ImageData imageData) throws PluginException {
        return process(imageData, getDefaultParameters());
    }

    /**
     * Checks if this plugin is enabled.
     *
     * @return true if enabled, false otherwise
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * Gets the priority of this plugin (lower values = higher priority).
     *
     * @return the plugin priority
     */
    default int getPriority() {
        return 100;
    }

    /**
     * Gets the estimated processing time for this plugin.
     *
     * @param imageData the image data to process
     * @return estimated processing time in milliseconds
     */
    default long getEstimatedProcessingTime(ImageData imageData) {
        // Default estimation based on image size
        long imageSize = imageData.size();
        if (imageSize < 1024 * 1024) { // < 1MB
            return 100;
        } else if (imageSize < 5 * 1024 * 1024) { // < 5MB
            return 500;
        } else {
            return 1000;
        }
    }

    /**
     * Gets the memory requirements for this plugin.
     *
     * @return memory requirements in MB
     */
    default int getMemoryRequirements() {
        return 256; // Default 256MB
    }

    /**
     * Checks if this plugin requires GPU acceleration.
     *
     * @return true if GPU acceleration is required, false otherwise
     */
    default boolean requiresGpuAcceleration() {
        return false;
    }

    /**
     * Gets the plugin dependencies.
     *
     * @return array of plugin IDs this plugin depends on
     */
    default String[] getDependencies() {
        return new String[0];
    }

    /**
     * Initializes the plugin.
     *
     * @param context the plugin context
     * @throws PluginException if initialization fails
     */
    default void initialize(PluginContext context) throws PluginException {
        // Default implementation does nothing
    }

    /**
     * Shuts down the plugin.
     *
     * @throws PluginException if shutdown fails
     */
    default void shutdown() throws PluginException {
        // Default implementation does nothing
    }

    /**
     * Gets the health status of this plugin.
     *
     * @return the health status
     */
    default PluginHealthStatus getHealthStatus() {
        return PluginHealthStatus.HEALTHY;
    }

    /**
     * Plugin types enumeration.
     */
    enum PluginType {
        /**
         * Detection plugin - performs object detection, classification, etc.
         */
        DETECTION("detection"),

        /**
         * Preprocessing plugin - performs image preprocessing operations.
         */
        PREPROCESSING("preprocessing"),

        /**
         * Postprocessing plugin - performs result post-processing operations.
         */
        POSTPROCESSING("postprocessing"),

        /**
         * Filter plugin - applies filters to images or results.
         */
        FILTER("filter"),

        /**
         * Enhancement plugin - enhances image quality or detection results.
         */
        ENHANCEMENT("enhancement"),

        /**
         * Custom plugin - custom functionality not fitting other categories.
         */
        CUSTOM("custom");

        private final String code;

        PluginType(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public static PluginType fromCode(String code) {
            if (code == null) {
                return null;
            }

            for (PluginType type : values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
            return null;
        }
    }

    /**
     * Plugin health status enumeration.
     */
    enum PluginHealthStatus {
        /**
         * Plugin is healthy and ready to process requests.
         */
        HEALTHY("healthy"),

        /**
         * Plugin is unhealthy and cannot process requests.
         */
        UNHEALTHY("unhealthy"),

        /**
         * Plugin status is unknown.
         */
        UNKNOWN("unknown"),

        /**
         * Plugin is starting up.
         */
        STARTING("starting"),

        /**
         * Plugin is shutting down.
         */
        SHUTTING_DOWN("shutting_down");

        private final String value;

        PluginHealthStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
