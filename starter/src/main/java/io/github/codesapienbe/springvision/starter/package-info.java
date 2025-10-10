/**
 * Spring Boot starter for Spring Vision framework.
 *
 * <p>This package contains the Spring Boot starter module that provides
 * easy integration of the Spring Vision framework into Spring Boot applications.
 * The starter automatically includes all necessary dependencies and configures
 * the framework components.</p>
 *
 * <h2>Quick Start</h2>
 *
 * <p>To use Spring Vision in your Spring Boot application, simply add the
 * starter dependency to your project:</p>
 *
 * <pre>{@code
 * <dependency>
 *     <groupId>com.springvision</groupId>
 *     <artifactId>spring-vision-starter</artifactId>
 *     <version>1.0</version>
 * </dependency>
 * }</pre>
 *
 * <p>Then inject the VisionTemplate into your components:</p>
 *
 * <pre>{@code
 * @Component
 * public class ImageService {
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
 * <h2>Included Dependencies</h2>
 *
 * <p>The starter automatically includes:</p>
 * <ul>
 *   <li>Spring Vision Core - Core framework components</li>
 *   <li>Spring Vision Autoconfigure - Auto-configuration classes</li>
 *   <li>Spring Boot Starter - Spring Boot integration</li>
 *   <li>Spring Boot Actuator - Health monitoring and metrics</li>
 *   <li>Micrometer Core - Metrics collection</li>
 *   <li>OpenCV Dependencies - Computer vision library</li>
 *   <li>JavaCV - Java bindings for OpenCV</li>
 * </ul>
 *
 * <h2>Auto-Configuration</h2>
 *
 * <p>The starter automatically configures:</p>
 * <ul>
 *   <li>Vision backend (OpenCV by default)</li>
 *   <li>Vision template for easy access</li>
 *   <li>Health indicators for monitoring</li>
 *   <li>Metrics collection for observability</li>
 *   <li>Proper lifecycle management</li>
 * </ul>
 *
 * <h2>Configuration</h2>
 *
 * <p>Configure the framework using application properties:</p>
 *
 * <pre>{@code
 * vision:
 *   enabled: true
 *   backend: opencv
 *   opencv:
 *     confidence-threshold: 0.8
 *   health:
 *     enabled: true
 *   metrics:
 *     enabled: true
 * }</pre>
 *
 * <h2>Sample Application</h2>
 *
 * <p>A sample application demonstrating usage is included in the test package:</p>
 *
 * <pre>{@code
 * @SpringBootApplication
 * public class SampleVisionApplication {
 *     public static void main(String[] args) {
 *         SpringApplication.run(SampleVisionApplication.class, args);
 *     }
 *
 *     @Bean
 *     public CommandLineRunner visionDemo(VisionTemplate visionTemplate) {
 *         return args -> {
 *             // Demonstrate vision capabilities
 *             VisionResult result = visionTemplate.detectFaces(imageData);
 *             // Process results...
 *         };
 *     }
 * }
 * }</pre>
 *
 * <h2>Health Monitoring</h2>
 *
 * <p>Health monitoring is automatically configured and available at:</p>
 * <ul>
 *   <li><code>/actuator/health</code> - Overall application health</li>
 *   <li><code>/actuator/health/vision</code> - Vision backend health</li>
 * </ul>
 *
 * <h2>Metrics</h2>
 *
 * <p>Metrics are automatically collected and available at:</p>
 * <ul>
 *   <li><code>/actuator/metrics</code> - All available metrics</li>
 *   <li><code>/actuator/metrics/vision.detections.total</code> - Detection counts</li>
 *   <li><code>/actuator/metrics/vision.processing.time</code> - Processing times</li>
 * </ul>
 *
 * <h2>Testing</h2>
 *
 * <p>Comprehensive tests are included to verify:</p>
 * <ul>
 *   <li>Auto-configuration works correctly</li>
 *   <li>Dependencies are properly included</li>
 *   <li>Configuration properties are respected</li>
 *   <li>Health monitoring and metrics work</li>
 * </ul>
 *
 * <h2>Customization</h2>
 *
 * <p>You can customize the starter by:</p>
 * <ul>
 *   <li>Providing your own beans (they will override auto-configured ones)</li>
 *   <li>Using configuration properties to adjust behavior</li>
 *   <li>Excluding specific auto-configuration classes if needed</li>
 *   <li>Implementing custom health indicators or metrics</li>
 * </ul>
 *
 * <h2>Dependencies</h2>
 *
 * <p>The starter requires:</p>
 * <ul>
 *   <li>Spring Boot 3.0+</li>
 *   <li>Java 21+</li>
 *   <li>Maven or Gradle</li>
 * </ul>
 *
 * <h2>Examples</h2>
 *
 * <p>See the test package for complete examples of:</p>
 * <ul>
 *   <li>Face detection</li>
 *   <li>Object detection</li>
 *   <li>Multiple detection types</li>
 *   <li>Health monitoring</li>
 *   <li>Metrics collection</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @see io.github.codesapienbe.springvision.core
 * @see io.github.codesapienbe.springvision.core.config
 * @see org.springframework.boot.autoconfigure
 * @since 1.0.0
 */
package io.github.codesapienbe.springvision.starter;
