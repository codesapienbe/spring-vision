package io.github.codesapienbe.springvision.core.plugin;

/**
 * Exception thrown when a plugin operation fails.
 *
 * <p>This exception is used to indicate errors that occur during plugin
 * operations such as initialization, processing, or shutdown.</p>
 *
 * @author Spring Vision Team
 * @see VisionPlugin
 * @since 1.0.0
 */
public class PluginException extends RuntimeException {

    private final String pluginId;
    private final String operation;

    /**
     * Creates a new PluginException with the specified message.
     *
     * @param message the error message
     */
    public PluginException(String message) {
        super(message);
        this.pluginId = null;
        this.operation = null;
    }

    /**
     * Creates a new PluginException with the specified message and cause.
     *
     * @param message the error message
     * @param cause   the cause of the exception
     */
    public PluginException(String message, Throwable cause) {
        super(message, cause);
        this.pluginId = null;
        this.operation = null;
    }

    /**
     * Creates a new PluginException with the specified plugin ID, operation, and message.
     *
     * @param pluginId  the plugin identifier
     * @param operation the operation that failed
     * @param message   the error message
     */
    public PluginException(String pluginId, String operation, String message) {
        super(String.format("Plugin '%s' failed during '%s': %s", pluginId, operation, message));
        this.pluginId = pluginId;
        this.operation = operation;
    }

    /**
     * Creates a new PluginException with the specified plugin ID, operation, message, and cause.
     *
     * @param pluginId  the plugin identifier
     * @param operation the operation that failed
     * @param message   the error message
     * @param cause     the cause of the exception
     */
    public PluginException(String pluginId, String operation, String message, Throwable cause) {
        super(String.format("Plugin '%s' failed during '%s': %s", pluginId, operation, message), cause);
        this.pluginId = pluginId;
        this.operation = operation;
    }

    /**
     * Gets the plugin identifier associated with this exception.
     *
     * @return the plugin identifier, or null if not specified
     */
    public String getPluginId() {
        return pluginId;
    }

    /**
     * Gets the operation that failed.
     *
     * @return the operation name, or null if not specified
     */
    public String getOperation() {
        return operation;
    }
}
