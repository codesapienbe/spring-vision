package com.springvision.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Spring Vision framework.
 *
 * <p>This class defines all configurable properties for the Spring Vision
 * framework, including backend selection, OpenCV settings, health monitoring,
 * and metrics configuration.</p>
 *
 * <p>Properties can be configured in application.yml, application.properties,
 * or through environment variables.</p>
 *
 * <p>Example configuration:</p>
 * <pre>{@code
 * vision:
 *   enabled: true
 *   backend: opencv
 *   opencv:
 *     enabled: true
 *     face-cascade-path: /haarcascade_frontalface_default.xml
 *     confidence-threshold: 0.8
 *   health:
 *     enabled: true
 *   metrics:
 *     enabled: true
 * }</pre>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 * @see VisionAutoConfiguration
 */
@ConfigurationProperties(prefix = "vision")
public class VisionProperties {

    /**
     * Whether Spring Vision is enabled.
     */
    private boolean enabled = true;

    /**
     * The vision backend to use (opencv, mediapipe, yolo).
     */
    private String backend = "opencv";

    /**
     * OpenCV-specific configuration properties.
     */
    private OpenCv opencv = new OpenCv();

    /**
     * Health monitoring configuration.
     */
    private Health health = new Health();

    /**
     * Metrics configuration.
     */
    private Metrics metrics = new Metrics();

    /**
     * Gets whether Spring Vision is enabled.
     *
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether Spring Vision is enabled.
     *
     * @param enabled whether to enable Spring Vision
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets the vision backend to use.
     *
     * @return the backend name
     */
    public String getBackend() {
        return backend;
    }

    /**
     * Sets the vision backend to use.
     *
     * @param backend the backend name
     */
    public void setBackend(String backend) {
        this.backend = backend;
    }

    /**
     * Gets the OpenCV configuration.
     *
     * @return the OpenCV configuration
     */
    public OpenCv getOpencv() {
        return opencv;
    }

    /**
     * Sets the OpenCV configuration.
     *
     * @param opencv the OpenCV configuration
     */
    public void setOpencv(OpenCv opencv) {
        this.opencv = opencv;
    }

    /**
     * Gets the health monitoring configuration.
     *
     * @return the health configuration
     */
    public Health getHealth() {
        return health;
    }

    /**
     * Sets the health monitoring configuration.
     *
     * @param health the health configuration
     */
    public void setHealth(Health health) {
        this.health = health;
    }

    /**
     * Gets the metrics configuration.
     *
     * @return the metrics configuration
     */
    public Metrics getMetrics() {
        return metrics;
    }

    /**
     * Sets the metrics configuration.
     *
     * @param metrics the metrics configuration
     */
    public void setMetrics(Metrics metrics) {
        this.metrics = metrics;
    }

    /**
     * OpenCV-specific configuration properties.
     */
    public static class OpenCv {

        /**
         * Whether OpenCV backend is enabled.
         */
        private boolean enabled = true;

        /**
         * Path to the face detection cascade classifier.
         */
        private String faceCascadePath = "/haarcascade_frontalface_default.xml";

        /**
         * Confidence threshold for detections (0.0 to 1.0).
         */
        private double confidenceThreshold = 0.8;

        /**
         * Minimum face size ratio relative to image size.
         */
        private double minFaceSizeRatio = 0.1;

        /**
         * Maximum face size ratio relative to image size.
         */
        private double maxFaceSizeRatio = 0.8;

        /**
         * Whether to enable GPU acceleration (if available).
         */
        private boolean gpuAcceleration = false;

        /**
         * Maximum image size in bytes for processing.
         */
        private long maxImageSize = 10 * 1024 * 1024; // 10MB

        /**
         * Gets whether OpenCV backend is enabled.
         *
         * @return true if enabled, false otherwise
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets whether OpenCV backend is enabled.
         *
         * @param enabled whether to enable OpenCV backend
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Gets the face cascade path.
         *
         * @return the face cascade path
         */
        public String getFaceCascadePath() {
            return faceCascadePath;
        }

        /**
         * Sets the face cascade path.
         *
         * @param faceCascadePath the face cascade path
         */
        public void setFaceCascadePath(String faceCascadePath) {
            this.faceCascadePath = faceCascadePath;
        }

        /**
         * Gets the confidence threshold.
         *
         * @return the confidence threshold
         */
        public double getConfidenceThreshold() {
            return confidenceThreshold;
        }

        /**
         * Sets the confidence threshold.
         *
         * @param confidenceThreshold the confidence threshold
         */
        public void setConfidenceThreshold(double confidenceThreshold) {
            this.confidenceThreshold = confidenceThreshold;
        }

        /**
         * Gets the minimum face size ratio.
         *
         * @return the minimum face size ratio
         */
        public double getMinFaceSizeRatio() {
            return minFaceSizeRatio;
        }

        /**
         * Sets the minimum face size ratio.
         *
         * @param minFaceSizeRatio the minimum face size ratio
         */
        public void setMinFaceSizeRatio(double minFaceSizeRatio) {
            this.minFaceSizeRatio = minFaceSizeRatio;
        }

        /**
         * Gets the maximum face size ratio.
         *
         * @return the maximum face size ratio
         */
        public double getMaxFaceSizeRatio() {
            return maxFaceSizeRatio;
        }

        /**
         * Sets the maximum face size ratio.
         *
         * @param maxFaceSizeRatio the maximum face size ratio
         */
        public void setMaxFaceSizeRatio(double maxFaceSizeRatio) {
            this.maxFaceSizeRatio = maxFaceSizeRatio;
        }

        /**
         * Gets whether GPU acceleration is enabled.
         *
         * @return true if GPU acceleration is enabled, false otherwise
         */
        public boolean isGpuAcceleration() {
            return gpuAcceleration;
        }

        /**
         * Sets whether GPU acceleration is enabled.
         *
         * @param gpuAcceleration whether to enable GPU acceleration
         */
        public void setGpuAcceleration(boolean gpuAcceleration) {
            this.gpuAcceleration = gpuAcceleration;
        }

        /**
         * Gets the maximum image size.
         *
         * @return the maximum image size in bytes
         */
        public long getMaxImageSize() {
            return maxImageSize;
        }

        /**
         * Sets the maximum image size.
         *
         * @param maxImageSize the maximum image size in bytes
         */
        public void setMaxImageSize(long maxImageSize) {
            this.maxImageSize = maxImageSize;
        }
    }

    /**
     * Health monitoring configuration properties.
     */
    public static class Health {

        /**
         * Whether health monitoring is enabled.
         */
        private boolean enabled = true;

        /**
         * Health check interval in milliseconds.
         */
        private long checkInterval = 30000; // 30 seconds

        /**
         * Maximum response time for health checks in milliseconds.
         */
        private long maxResponseTime = 5000; // 5 seconds

        /**
         * Gets whether health monitoring is enabled.
         *
         * @return true if enabled, false otherwise
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets whether health monitoring is enabled.
         *
         * @param enabled whether to enable health monitoring
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Gets the health check interval.
         *
         * @return the health check interval in milliseconds
         */
        public long getCheckInterval() {
            return checkInterval;
        }

        /**
         * Sets the health check interval.
         *
         * @param checkInterval the health check interval in milliseconds
         */
        public void setCheckInterval(long checkInterval) {
            this.checkInterval = checkInterval;
        }

        /**
         * Gets the maximum response time for health checks.
         *
         * @return the maximum response time in milliseconds
         */
        public long getMaxResponseTime() {
            return maxResponseTime;
        }

        /**
         * Sets the maximum response time for health checks.
         *
         * @param maxResponseTime the maximum response time in milliseconds
         */
        public void setMaxResponseTime(long maxResponseTime) {
            this.maxResponseTime = maxResponseTime;
        }
    }

    /**
     * Metrics configuration properties.
     */
    public static class Metrics {

        /**
         * Whether metrics collection is enabled.
         */
        private boolean enabled = true;

        /**
         * Metrics collection interval in milliseconds.
         */
        private long collectionInterval = 60000; // 60 seconds

        /**
         * Whether to include detailed metrics.
         */
        private boolean detailed = false;

        /**
         * Gets whether metrics collection is enabled.
         *
         * @return true if enabled, false otherwise
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets whether metrics collection is enabled.
         *
         * @param enabled whether to enable metrics collection
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Gets the metrics collection interval.
         *
         * @return the collection interval in milliseconds
         */
        public long getCollectionInterval() {
            return collectionInterval;
        }

        /**
         * Sets the metrics collection interval.
         *
         * @param collectionInterval the collection interval in milliseconds
         */
        public void setCollectionInterval(long collectionInterval) {
            this.collectionInterval = collectionInterval;
        }

        /**
         * Gets whether detailed metrics are included.
         *
         * @return true if detailed metrics are included, false otherwise
         */
        public boolean isDetailed() {
            return detailed;
        }

        /**
         * Sets whether detailed metrics are included.
         *
         * @param detailed whether to include detailed metrics
         */
        public void setDetailed(boolean detailed) {
            this.detailed = detailed;
        }
    }
}
