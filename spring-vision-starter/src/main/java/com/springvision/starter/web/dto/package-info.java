/**
 * Data Transfer Objects (DTOs) for Spring Vision REST API.
 *
 * <p>This package contains the DTOs used for request and response serialization
 * in the Spring Vision REST API. These DTOs provide a clean separation between
 * the API contract and internal domain models.</p>
 *
 * <h2>Request DTOs</h2>
 *
 * <h3>DetectionRequest</h3>
 * <p>Represents a request for single detection operations:</p>
 * <ul>
 *   <li>Contains image data (typically base64 encoded)</li>
 *   <li>Includes validation annotations for data integrity</li>
 *   <li>Supports both face and object detection</li>
 * </ul>
 *
 * <h3>MultipleDetectionRequest</h3>
 * <p>Represents a request for multiple detection operations:</p>
 * <ul>
 *   <li>Contains image data and list of detection types</li>
 *   <li>Supports batch processing of multiple detection types</li>
 *   <li>Validates detection type specifications</li>
 * </ul>
 *
 * <h2>Response DTOs</h2>
 *
 * <h3>DetectionResponse</h3>
 * <p>Represents the response from detection operations:</p>
 * <ul>
 *   <li>Includes correlation ID for request tracking</li>
 *   <li>Contains detection results and metadata</li>
 *   <li>Provides error information if processing failed</li>
 *   <li>Includes performance metrics (processing time, confidence)</li>
 * </ul>
 *
 * <h3>MultipleDetectionResponse</h3>
 * <p>Represents the response from multiple detection operations:</p>
 * <ul>
 *   <li>Contains results for each requested detection type</li>
 *   <li>Provides aggregated metrics across all detections</li>
 *   <li>Includes correlation ID and error handling</li>
 * </ul>
 *
 * <h3>HealthResponse</h3>
 * <p>Represents the response from health check operations:</p>
 * <ul>
 *   <li>Contains backend health status and information</li>
 *   <li>Includes performance metrics and capabilities</li>
 *   <li>Provides error information if health check failed</li>
 * </ul>
 *
 * <h2>Features</h2>
 *
 * <h3>JSON Serialization</h3>
 * <p>All DTOs include proper JSON annotations:</p>
 * <ul>
 *   <li><code>@JsonProperty</code> for field mapping</li>
 *   <li>Consistent naming conventions</li>
 *   <li>Proper type handling for dates, numbers, etc.</li>
 * </ul>
 *
 * <h3>Validation</h3>
 * <p>Request DTOs include comprehensive validation:</p>
 * <ul>
 *   <li><code>@NotNull</code> for required fields</li>
 *   <li><code>@Size</code> for array/string length validation</li>
 *   <li><code>@NotEmpty</code> for collection validation</li>
 *   <li>Custom validation messages for user-friendly errors</li>
 * </ul>
 *
 * <h3>Builder Pattern</h3>
 * <p>All DTOs support the builder pattern for easy construction:</p>
 * <pre>{@code
 * DetectionRequest request = DetectionRequest.builder()
 *     .imageData(imageBytes)
 *     .build();
 *
 * DetectionResponse response = DetectionResponse.builder()
 *     .correlationId("uuid")
 *     .detectionType("face")
 *     .detectionCount(2)
 *     .averageConfidence(0.85)
 *     .processingTimeMs(150)
 *     .detections(detections)
 *     .build();
 * }</pre>
 *
 * <h3>Error Handling</h3>
 * <p>Response DTOs include error handling capabilities:</p>
 * <ul>
 *   <li>Optional error message fields</li>
 *   <li>Error state checking methods</li>
 *   <li>Consistent error response format</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Creating a Detection Request</h3>
 * <pre>{@code
 * byte[] imageData = Files.readAllBytes(Paths.get("image.jpg"));
 * DetectionRequest request = DetectionRequest.builder()
 *     .imageData(imageData)
 *     .build();
 * }</pre>
 *
 * <h3>Creating a Multiple Detection Request</h3>
 * <pre>{@code
 * MultipleDetectionRequest request = MultipleDetectionRequest.builder()
 *     .imageData(imageData)
 *     .detectionTypes(List.of("face", "object"))
 *     .build();
 * }</pre>
 *
 * <h3>Processing a Detection Response</h3>
 * <pre>{@code
 * if (response.hasError()) {
 *     logger.error("Detection failed: {}", response.getError());
 * } else if (response.hasDetections()) {
 *     response.getDetections().forEach(detection -> {
 *         logger.info("Detection: {} (confidence: {})",
 *             detection.label(), detection.confidence());
 *     });
 * }
 * }</pre>
 *
 * <h3>Processing a Multiple Detection Response</h3>
 * <pre>{@code
 * if (response.hasResults()) {
 *     logger.info("Total detections: {}", response.getTotalDetectionCount());
 *     logger.info("Average confidence: {}", response.getAverageConfidence());
 *     logger.info("Total processing time: {}ms", response.getTotalProcessingTimeMs());
 * }
 * }</pre>
 *
 * <h2>Serialization</h2>
 *
 * <p>DTOs are designed for efficient JSON serialization:</p>
 * <ul>
 *   <li>Minimal memory footprint</li>
 *   <li>Fast serialization/deserialization</li>
 *   <li>Compatible with Jackson ObjectMapper</li>
 *   <li>Support for custom serializers if needed</li>
 * </ul>
 *
 * <h2>Validation</h2>
 *
 * <p>Request validation is handled automatically by Spring Boot:</p>
 * <ul>
 *   <li>Automatic validation on controller methods</li>
 *   <li>Custom error messages for validation failures</li>
 *   <li>Integration with Spring Boot's error handling</li>
 * </ul>
 *
 * <h2>Testing</h2>
 *
 * <p>DTOs are thoroughly tested for:</p>
 * <ul>
 *   <li>JSON serialization/deserialization</li>
 *   <li>Builder pattern functionality</li>
 *   <li>Validation annotations</li>
 *   <li>Error handling methods</li>
 *   <li>Edge cases and null handling</li>
 * </ul>
 *
 * <h2>Dependencies</h2>
 *
 * <p>The DTO package requires:</p>
 * <ul>
 *   <li>Jackson for JSON processing</li>
 *   <li>Jakarta Validation for input validation</li>
 *   <li>Spring Vision Core for domain models</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 * @see DetectionRequest
 * @see DetectionResponse
 * @see MultipleDetectionRequest
 * @see MultipleDetectionResponse
 * @see HealthResponse
 */
package com.springvision.starter.web.dto;
