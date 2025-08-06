package com.springvision.core.plugin;

import java.util.Map;
import java.util.Objects;

/**
 * Metadata information about a vision plugin.
 *
 * <p>This record encapsulates metadata about a plugin including its basic
 * information, capabilities, requirements, and configuration details.</p>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 * @see VisionPlugin
 */
public record PluginMetadata(
    String pluginId,
    String displayName,
    String version,
    String description,
    String author,
    VisionPlugin.PluginType pluginType,
    String[] supportedDetectionTypes,
    Map<String, String> supportedParameters,
    Map<String, Object> defaultParameters,
    int priority,
    int memoryRequirements,
    boolean requiresGpuAcceleration,
    String[] dependencies
) {

    /**
     * Creates a new PluginMetadata with the specified parameters.
     *
     * @param pluginId the plugin identifier
     * @param displayName the display name
     * @param version the version
     * @param description the description
     * @param author the author
     * @param pluginType the plugin type
     * @param supportedDetectionTypes the supported detection types
     * @param supportedParameters the supported parameters
     * @param defaultParameters the default parameters
     * @param priority the priority
     * @param memoryRequirements the memory requirements in MB
     * @param requiresGpuAcceleration whether GPU acceleration is required
     * @param dependencies the plugin dependencies
     */
    public PluginMetadata {
        Objects.requireNonNull(pluginId, "Plugin ID must not be null");
        Objects.requireNonNull(displayName, "Display name must not be null");
        Objects.requireNonNull(version, "Version must not be null");
        Objects.requireNonNull(description, "Description must not be null");
        Objects.requireNonNull(author, "Author must not be null");
        Objects.requireNonNull(pluginType, "Plugin type must not be null");
        Objects.requireNonNull(supportedDetectionTypes, "Supported detection types must not be null");
        Objects.requireNonNull(supportedParameters, "Supported parameters must not be null");
        Objects.requireNonNull(defaultParameters, "Default parameters must not be null");
        Objects.requireNonNull(dependencies, "Dependencies must not be null");

        if (pluginId.trim().isEmpty()) {
            throw new IllegalArgumentException("Plugin ID must not be empty");
        }
        if (displayName.trim().isEmpty()) {
            throw new IllegalArgumentException("Display name must not be empty");
        }
        if (version.trim().isEmpty()) {
            throw new IllegalArgumentException("Version must not be empty");
        }
        if (description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description must not be empty");
        }
        if (author.trim().isEmpty()) {
            throw new IllegalArgumentException("Author must not be empty");
        }
        if (priority < 0) {
            throw new IllegalArgumentException("Priority must be non-negative");
        }
        if (memoryRequirements < 0) {
            throw new IllegalArgumentException("Memory requirements must be non-negative");
        }
    }

    /**
     * Checks if this plugin supports a specific detection type.
     *
     * @param detectionType the detection type to check
     * @return true if supported, false otherwise
     */
    public boolean supportsDetectionType(String detectionType) {
        if (detectionType == null) {
            return false;
        }

        for (String supportedType : supportedDetectionTypes) {
            if (supportedType.equals(detectionType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if this plugin supports a specific parameter.
     *
     * @param parameterName the parameter name to check
     * @return true if supported, false otherwise
     */
    public boolean supportsParameter(String parameterName) {
        return supportedParameters.containsKey(parameterName);
    }

    /**
     * Gets the default value for a parameter.
     *
     * @param parameterName the parameter name
     * @return the default value, or null if not found
     */
    public Object getDefaultParameterValue(String parameterName) {
        return defaultParameters.get(parameterName);
    }

    /**
     * Gets the description for a parameter.
     *
     * @param parameterName the parameter name
     * @return the parameter description, or null if not found
     */
    public String getParameterDescription(String parameterName) {
        return supportedParameters.get(parameterName);
    }

    /**
     * Checks if this plugin has dependencies.
     *
     * @return true if the plugin has dependencies, false otherwise
     */
    public boolean hasDependencies() {
        return dependencies.length > 0;
    }

    /**
     * Checks if this plugin depends on the specified plugin.
     *
     * @param pluginId the plugin identifier to check
     * @return true if this plugin depends on the specified plugin, false otherwise
     */
    public boolean dependsOn(String pluginId) {
        if (pluginId == null) {
            return false;
        }

        for (String dependency : dependencies) {
            if (dependency.equals(pluginId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the number of supported detection types.
     *
     * @return the number of supported detection types
     */
    public int getSupportedDetectionTypeCount() {
        return supportedDetectionTypes.length;
    }

    /**
     * Gets the number of supported parameters.
     *
     * @return the number of supported parameters
     */
    public int getSupportedParameterCount() {
        return supportedParameters.size();
    }

    /**
     * Gets the number of dependencies.
     *
     * @return the number of dependencies
     */
    public int getDependencyCount() {
        return dependencies.length;
    }

    /**
     * Checks if this plugin is a detection plugin.
     *
     * @return true if this is a detection plugin, false otherwise
     */
    public boolean isDetectionPlugin() {
        return pluginType == VisionPlugin.PluginType.DETECTION;
    }

    /**
     * Checks if this plugin is a preprocessing plugin.
     *
     * @return true if this is a preprocessing plugin, false otherwise
     */
    public boolean isPreprocessingPlugin() {
        return pluginType == VisionPlugin.PluginType.PREPROCESSING;
    }

    /**
     * Checks if this plugin is a postprocessing plugin.
     *
     * @return true if this is a postprocessing plugin, false otherwise
     */
    public boolean isPostprocessingPlugin() {
        return pluginType == VisionPlugin.PluginType.POSTPROCESSING;
    }

    /**
     * Checks if this plugin is a filter plugin.
     *
     * @return true if this is a filter plugin, false otherwise
     */
    public boolean isFilterPlugin() {
        return pluginType == VisionPlugin.PluginType.FILTER;
    }

    /**
     * Checks if this plugin is an enhancement plugin.
     *
     * @return true if this is an enhancement plugin, false otherwise
     */
    public boolean isEnhancementPlugin() {
        return pluginType == VisionPlugin.PluginType.ENHANCEMENT;
    }

    /**
     * Checks if this plugin is a custom plugin.
     *
     * @return true if this is a custom plugin, false otherwise
     */
    public boolean isCustomPlugin() {
        return pluginType == VisionPlugin.PluginType.CUSTOM;
    }

    @Override
    public String toString() {
        return String.format("PluginMetadata{id='%s', name='%s', version='%s', type=%s}",
                           pluginId, displayName, version, pluginType);
    }
}
