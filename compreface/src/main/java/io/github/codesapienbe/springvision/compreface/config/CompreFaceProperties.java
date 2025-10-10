package io.github.codesapienbe.springvision.compreface.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Spring Vision CompreFace module.
 *
 * @author Spring Vision Team
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = "spring.vision.compreface")
public class CompreFaceProperties {

    /**
     * Whether CompreFace backend is enabled.
     */
    private boolean enabled = true;

    /**
     * CompreFace API server URL.
     */
    private String apiUrl = "http://localhost:8000";

    /**
     * CompreFace base URL (alias for apiUrl).
     */
    private String baseUrl = "http://localhost:8000";

    /**
     * CompreFace API key for authentication.
     */
    private String apiKey;

    /**
     * Connection timeout in milliseconds.
     */
    private int connectTimeout = 5000;

    /**
     * Read timeout in milliseconds.
     */
    private int readTimeout = 30000;

    /**
     * General timeout in seconds.
     */
    private int timeout = 30;

    /**
     * Detection probability threshold (0.0 to 1.0).
     */
    private double detectionThreshold = 0.7;

    /**
     * Face-verification threshold (0.0 to 1.0).
     */
    private double verificationThreshold = 0.7;

    /**
     * Gets whether CompreFace backend is enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether CompreFace backend is enabled.
     *
     * @param enabled true to enable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets the CompreFace API URL.
     *
     * @return API URL
     */
    public String getApiUrl() {
        return apiUrl;
    }

    /**
     * Sets the CompreFace API URL.
     *
     * @param apiUrl API URL
     */
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
        this.baseUrl = apiUrl; // Keep baseUrl in sync
    }

    /**
     * Gets the CompreFace base URL.
     *
     * @return base URL
     */
    public String getBaseUrl() {
        return baseUrl != null ? baseUrl : apiUrl;
    }

    /**
     * Sets the CompreFace base URL.
     *
     * @param baseUrl base URL
     */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        this.apiUrl = baseUrl; // Keep apiUrl in sync
    }

    /**
     * Gets the CompreFace API key.
     *
     * @return API key
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Sets the CompreFace API key.
     *
     * @param apiKey API key
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Gets the connection timeout.
     *
     * @return connection timeout in milliseconds
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Sets the connection timeout.
     *
     * @param connectTimeout connection timeout in milliseconds
     */
    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    /**
     * Gets the read timeout.
     *
     * @return read timeout in milliseconds
     */
    public int getReadTimeout() {
        return readTimeout;
    }

    /**
     * Sets the read timeout.
     *
     * @param readTimeout read timeout in milliseconds
     */
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    /**
     * Gets the general timeout.
     *
     * @return timeout in seconds
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Sets the general timeout.
     *
     * @param timeout timeout in seconds
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * Gets the detection threshold.
     *
     * @return detection threshold
     */
    public double getDetectionThreshold() {
        return detectionThreshold;
    }

    /**
     * Sets the detection threshold.
     *
     * @param detectionThreshold detection threshold
     */
    public void setDetectionThreshold(double detectionThreshold) {
        this.detectionThreshold = detectionThreshold;
    }

    /**
     * Gets the verification threshold.
     *
     * @return verification threshold
     */
    public double getVerificationThreshold() {
        return verificationThreshold;
    }

    /**
     * Sets the verification threshold.
     *
     * @param verificationThreshold verification threshold
     */
    public void setVerificationThreshold(double verificationThreshold) {
        this.verificationThreshold = verificationThreshold;
    }
}
