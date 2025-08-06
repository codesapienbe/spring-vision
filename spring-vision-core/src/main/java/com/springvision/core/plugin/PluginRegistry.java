package com.springvision.core.plugin;

import java.util.Map;

/**
 * Registry for managing vision plugins.
 *
 * <p>This interface defines the contract for a plugin registry that manages
 * the registration, discovery, and lifecycle of vision plugins. It provides
 * methods for adding, removing, and querying plugins.</p>
 *
 * <p>The registry is responsible for ensuring plugin uniqueness, managing
 * dependencies, and providing access to plugins by various criteria.</p>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 * @see VisionPlugin
 * @see PluginManager
 */
public interface PluginRegistry {

    /**
     * Registers a plugin with the registry.
     *
     * @param plugin the plugin to register
     * @throws PluginException if registration fails
     */
    void registerPlugin(VisionPlugin plugin) throws PluginException;

    /**
     * Unregisters a plugin from the registry.
     *
     * @param pluginId the plugin identifier
     * @return the unregistered plugin, or null if not found
     * @throws PluginException if unregistration fails
     */
    VisionPlugin unregisterPlugin(String pluginId) throws PluginException;

    /**
     * Gets a plugin by its identifier.
     *
     * @param pluginId the plugin identifier
     * @return the plugin, or null if not found
     */
    VisionPlugin getPlugin(String pluginId);

    /**
     * Gets all registered plugins.
     *
     * @return a map of plugin IDs to plugins
     */
    Map<String, VisionPlugin> getAllPlugins();

    /**
     * Gets plugins by type.
     *
     * @param pluginType the plugin type
     * @return a map of plugin IDs to plugins of the specified type
     */
    Map<String, VisionPlugin> getPluginsByType(VisionPlugin.PluginType pluginType);

    /**
     * Gets plugins that support a specific detection type.
     *
     * @param detectionType the detection type
     * @return a map of plugin IDs to plugins that support the detection type
     */
    Map<String, VisionPlugin> getPluginsByDetectionType(String detectionType);

    /**
     * Gets enabled plugins.
     *
     * @return a map of plugin IDs to enabled plugins
     */
    Map<String, VisionPlugin> getEnabledPlugins();

    /**
     * Gets disabled plugins.
     *
     * @return a map of plugin IDs to disabled plugins
     */
    Map<String, VisionPlugin> getDisabledPlugins();

    /**
     * Checks if a plugin is registered.
     *
     * @param pluginId the plugin identifier
     * @return true if the plugin is registered, false otherwise
     */
    boolean hasPlugin(String pluginId);

    /**
     * Gets the number of registered plugins.
     *
     * @return the number of registered plugins
     */
    int getPluginCount();

    /**
     * Gets the number of plugins by type.
     *
     * @param pluginType the plugin type
     * @return the number of plugins of the specified type
     */
    int getPluginCountByType(VisionPlugin.PluginType pluginType);

    /**
     * Gets plugin metadata for all registered plugins.
     *
     * @return a map of plugin IDs to plugin metadata
     */
    Map<String, PluginMetadata> getAllPluginMetadata();

    /**
     * Gets plugin metadata by ID.
     *
     * @param pluginId the plugin identifier
     * @return the plugin metadata, or null if not found
     */
    PluginMetadata getPluginMetadata(String pluginId);

    /**
     * Validates plugin dependencies.
     *
     * @param pluginId the plugin identifier
     * @return validation result
     */
    PluginValidationResult validatePluginDependencies(String pluginId);

    /**
     * Gets plugins that depend on the specified plugin.
     *
     * @param pluginId the plugin identifier
     * @return a map of plugin IDs to dependent plugins
     */
    Map<String, VisionPlugin> getDependentPlugins(String pluginId);

    /**
     * Checks if a plugin can be safely unregistered.
     *
     * @param pluginId the plugin identifier
     * @return true if the plugin can be safely unregistered, false otherwise
     */
    boolean canUnregisterPlugin(String pluginId);

    /**
     * Gets the plugin load order based on dependencies.
     *
     * @return an array of plugin IDs in load order
     */
    String[] getPluginLoadOrder();

    /**
     * Refreshes the plugin registry.
     *
     * @throws PluginException if refresh fails
     */
    void refresh() throws PluginException;

    /**
     * Clears all registered plugins.
     *
     * @throws PluginException if clearing fails
     */
    void clear() throws PluginException;
}
