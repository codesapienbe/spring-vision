package io.github.codesapienbe.springvision.core.plugin;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Context object passed to plugins during initialization and operation.
 *
 * <p>This class provides plugins with access to shared resources, configuration,
 * and other plugins in the system. It acts as a bridge between the plugin
 * and the Spring Vision framework.</p>
 *
 * <p>The context is thread-safe and can be shared across multiple plugin
 * instances.</p>
 *
 * @author Spring Vision Team
 * @see VisionPlugin
 * @since 1.0.0
 */
public class PluginContext {

    private final String pluginId;
    private final Map<String, Object> configuration;
    private final Map<String, Object> sharedData;
    private final PluginRegistry pluginRegistry;

    /**
     * Creates a new PluginContext with the specified parameters.
     *
     * @param pluginId       the plugin identifier
     * @param configuration  the plugin configuration
     * @param pluginRegistry the plugin registry
     */
    public PluginContext(String pluginId, Map<String, Object> configuration, PluginRegistry pluginRegistry) {
        this.pluginId = Objects.requireNonNull(pluginId, "Plugin ID must not be null");
        this.configuration = new ConcurrentHashMap<>(configuration != null ? configuration : Map.of());
        this.sharedData = new ConcurrentHashMap<>();
        this.pluginRegistry = Objects.requireNonNull(pluginRegistry, "Plugin registry must not be null");
    }

    /**
     * Gets the plugin identifier.
     *
     * @return the plugin identifier
     */
    public String getPluginId() {
        return pluginId;
    }

    /**
     * Gets a configuration value by key.
     *
     * @param key the configuration key
     * @return the configuration value, or null if not found
     */
    public Object getConfiguration(String key) {
        return configuration.get(key);
    }

    /**
     * Gets a configuration value by key with a default value.
     *
     * @param key          the configuration key
     * @param defaultValue the default value to return if key not found
     * @return the configuration value, or the default value if not found
     */
    public Object getConfiguration(String key, Object defaultValue) {
        return configuration.getOrDefault(key, defaultValue);
    }

    /**
     * Sets a configuration value.
     *
     * @param key   the configuration key
     * @param value the configuration value
     */
    public void setConfiguration(String key, Object value) {
        configuration.put(key, value);
    }

    /**
     * Gets all configuration values.
     *
     * @return a copy of the configuration map
     */
    public Map<String, Object> getAllConfiguration() {
        return Map.copyOf(configuration);
    }

    /**
     * Gets a shared data value by key.
     *
     * @param key the data key
     * @return the data value, or null if not found
     */
    public Object getSharedData(String key) {
        return sharedData.get(key);
    }

    /**
     * Gets a shared data value by key with a default value.
     *
     * @param key          the data key
     * @param defaultValue the default value to return if key not found
     * @return the data value, or the default value if not found
     */
    public Object getSharedData(String key, Object defaultValue) {
        return sharedData.getOrDefault(key, defaultValue);
    }

    /**
     * Sets a shared data value.
     *
     * @param key   the data key
     * @param value the data value
     */
    public void setSharedData(String key, Object value) {
        sharedData.put(key, value);
    }

    /**
     * Removes a shared data value.
     *
     * @param key the data key to remove
     * @return the removed value, or null if not found
     */
    public Object removeSharedData(String key) {
        return sharedData.remove(key);
    }

    /**
     * Gets all shared data values.
     *
     * @return a copy of the shared data map
     */
    public Map<String, Object> getAllSharedData() {
        return Map.copyOf(sharedData);
    }

    /**
     * Gets the plugin registry.
     *
     * @return the plugin registry
     */
    public PluginRegistry getPluginRegistry() {
        return pluginRegistry;
    }

    /**
     * Gets a plugin by ID.
     *
     * @param pluginId the plugin identifier
     * @return the plugin, or null if not found
     */
    public VisionPlugin getPlugin(String pluginId) {
        return pluginRegistry.getPlugin(pluginId);
    }

    /**
     * Gets all available plugins.
     *
     * @return all registered plugins
     */
    public Map<String, VisionPlugin> getAllPlugins() {
        return pluginRegistry.getAllPlugins();
    }

    /**
     * Gets plugins by type.
     *
     * @param pluginType the plugin type
     * @return plugins of the specified type
     */
    public Map<String, VisionPlugin> getPluginsByType(VisionPlugin.PluginType pluginType) {
        return pluginRegistry.getPluginsByType(pluginType);
    }

    /**
     * Checks if a plugin is available.
     *
     * @param pluginId the plugin identifier
     * @return true if the plugin is available, false otherwise
     */
    public boolean hasPlugin(String pluginId) {
        return pluginRegistry.hasPlugin(pluginId);
    }

    /**
     * Gets the system property value.
     *
     * @param key the property key
     * @return the property value, or null if not found
     */
    public String getSystemProperty(String key) {
        return System.getProperty(key);
    }

    /**
     * Gets the system property value with a default value.
     *
     * @param key          the property key
     * @param defaultValue the default value to return if key not found
     * @return the property value, or the default value if not found
     */
    public String getSystemProperty(String key, String defaultValue) {
        return System.getProperty(key, defaultValue);
    }

    /**
     * Gets the environment variable value.
     *
     * @param key the environment variable key
     * @return the environment variable value, or null if not found
     */
    public String getEnvironmentVariable(String key) {
        return System.getenv(key);
    }

    /**
     * Gets the current working directory.
     *
     * @return the current working directory
     */
    public String getWorkingDirectory() {
        return System.getProperty("user.dir");
    }

    /**
     * Gets the temporary directory.
     *
     * @return the temporary directory
     */
    public String getTempDirectory() {
        return System.getProperty("java.io.tmpdir");
    }

    /**
     * Gets the available memory in bytes.
     *
     * @return the available memory in bytes
     */
    public long getAvailableMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory();
    }

    /**
     * Gets the total memory in bytes.
     *
     * @return the total memory in bytes
     */
    public long getTotalMemory() {
        return Runtime.getRuntime().totalMemory();
    }

    /**
     * Gets the maximum memory in bytes.
     *
     * @return the maximum memory in bytes
     */
    public long getMaxMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    /**
     * Gets the number of available processors.
     *
     * @return the number of available processors
     */
    public int getAvailableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    @Override
    public String toString() {
        return String.format("PluginContext{pluginId='%s', configSize=%d, sharedDataSize=%d}",
            pluginId, configuration.size(), sharedData.size());
    }
}
