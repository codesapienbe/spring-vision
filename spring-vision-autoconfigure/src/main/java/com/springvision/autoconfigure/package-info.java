/**
 * Spring Boot auto-configuration for Spring Vision framework.
 *
 * <p>This package contains Spring Boot auto-configuration classes that automatically
 * set up the Spring Vision framework when Spring Boot detects the necessary
 * dependencies. It provides seamless integration with Spring Boot applications
 * through automatic bean configuration, health monitoring, and metrics collection.</p>
 *
 * <h2>Auto-Configuration Components</h2>
 *
 * <h3>VisionAutoConfiguration</h3>
 * <p>The main auto-configuration class that sets up all Spring Vision components
 * including backends, templates, health indicators, and metrics collectors.
 * It supports multiple backend types and can be customized through application properties.</p>
 *
 * <h3>VisionProperties</h3>
 * <p>Configuration properties class that defines all configurable options for
 * the Spring Vision framework. Properties can be set in application.yml,
 * application.properties, or through environment variables.</p>
 *
 * <h3>VisionHealthIndicator</h3>
 * <p>Spring Boot Actuator health indicator that monitors the health of the
 * configured vision backend and reports its status to health endpoints.</p>
 *
 * <h3>VisionMetrics</h3>
 * <p>Metrics collection component that provides comprehensive monitoring
 * of vision operations including detection counts, processing times, and error rates.</p>
 *
 * <h2>Configuration</h2>
 *
 * <p>The auto-configuration can be customized through application properties:</p>
 *
 * <pre>{@code
 * vision:
 *   enabled: true
 *   backend: opencv
 *   opencv:
 *     enabled: true
 *     confidence-threshold: 0.8
 *   health:
 *     enabled: true
 *   metrics:
 *     enabled: true
 * }</pre>
 *
 * <h2>Usage</h2>
 *
 * <p>Simply include the Spring Vision starter in your Spring Boot application:</p>
 *
 * <pre>{@code
 * @SpringBootApplication
 * public class MyApplication {
 *     public static void main(String[] args) {
 *         SpringApplication.run(MyApplication.class, args);
 *     }
 * }
 *
 * @Component
 * public class MyService {
 *     @Autowired
 *     private VisionTemplate visionTemplate;
 *
 *     public void processImage(byte[] imageData) {
 *         VisionResult result = visionTemplate.detectFaces(imageData);
 *         // Process results...
 *     }
 * }
 * }</pre>
 *
 * <h2>Health Monitoring</h2>
 *
 * <p>Health monitoring is automatically configured when Spring Boot Actuator
 * is present. Health information is available at the `/actuator/health` endpoint:</p>
 *
 * <pre>{@code
 * {
 *   "status": "UP",
 *   "components": {
 *     "vision": {
 *       "status": "UP",
 *       "details": {
 *         "backend": "opencv",
 *         "status": "healthy",
 *         "responseTime": 45
 *       }
 *     }
 *   }
 * }
 * }</pre>
 *
 * <h2>Metrics</h2>
 *
 * <p>Metrics are automatically collected and can be exported to various
 * monitoring systems through Micrometer. Available metrics include:</p>
 *
 * <ul>
 *   <li><code>vision.detections.total</code> - Total number of detections</li>
 *   <li><code>vision.detections.face</code> - Number of face detections</li>
 *   <li><code>vision.processing.time</code> - Processing time for detections</li>
 *   <li><code>vision.errors.total</code> - Total number of errors</li>
 *   <li><code>vision.backend.health</code> - Backend health status</li>
 * </ul>
 *
 * <h2>Conditional Configuration</h2>
 *
 * <p>The auto-configuration uses Spring Boot's conditional annotations to
 * enable/disable components based on configuration:</p>
 *
 * <ul>
 *   <li><code>@ConditionalOnProperty("vision.enabled")</code> - Enables/disables the entire framework</li>
 *   <li><code>@ConditionalOnProperty("vision.health.enabled")</code> - Enables/disables health monitoring</li>
 *   <li><code>@ConditionalOnProperty("vision.metrics.enabled")</code> - Enables/disables metrics collection</li>
 *   <li><code>@ConditionalOnMissingBean</code> - Only creates beans if they don't already exist</li>
 * </ul>
 *
 * <h2>Customization</h2>
 *
 * <p>You can customize the auto-configuration by:</p>
 *
 * <ul>
 *   <li>Providing your own beans (they will be used instead of auto-configured ones)</li>
 *   <li>Using configuration properties to adjust behavior</li>
 *   <li>Excluding auto-configuration classes if needed</li>
 *   <li>Implementing custom health indicators or metrics</li>
 * </ul>
 *
 * <h2>Dependencies</h2>
 *
 * <p>The auto-configuration requires:</p>
 *
 * <ul>
 *   <li>Spring Boot 3.0+</li>
 *   <li>Spring Boot Actuator (for health monitoring)</li>
 *   <li>Micrometer (for metrics collection)</li>
 *   <li>Spring Vision Core (for core functionality)</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 * @see com.springvision.core
 * @see org.springframework.boot.autoconfigure
 * @see org.springframework.boot.actuator.health
 * @see io.micrometer.core.instrument
 */
package com.springvision.autoconfigure;
