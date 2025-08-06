/**
 * REST API components for Spring Vision framework.
 *
 * <p>This package contains the REST API components for the Spring Vision framework,
 * including controllers, DTOs, and related web components. The API provides
 * HTTP endpoints for computer vision operations with proper validation,
 * error handling, and response formatting.</p>
 *
 * <h2>API Endpoints</h2>
 *
 * <p>The REST API provides the following endpoints:</p>
 *
 * <h3>Detection Endpoints</h3>
 * <ul>
 *   <li><code>POST /api/vision/detect/faces</code> - Face detection (file upload or JSON)</li>
 *   <li><code>POST /api/vision/detect/objects</code> - Object detection (file upload or JSON)</li>
 *   <li><code>POST /api/vision/detect/multiple</code> - Multiple detection types (file upload or JSON)</li>
 * </ul>
 *
 * <h3>Information Endpoints</h3>
 * <ul>
 *   <li><code>GET /api/vision/health</code> - Backend health status</li>
 *   <li><code>GET /api/vision/info</code> - Backend information and capabilities</li>
 * </ul>
 *
 * <h2>Request/Response Formats</h2>
 *
 * <h3>File Upload</h3>
 * <p>For file uploads, use <code>multipart/form-data</code>:</p>
 * <pre>{@code
 * POST /api/vision/detect/faces
 * Content-Type: multipart/form-data
 *
 * file: [image file]
 * }</pre>
 *
 * <h3>JSON Request</h3>
 * <p>For JSON requests, use <code>application/json</code>:</p>
 * <pre>{@code
 * POST /api/vision/detect/faces
 * Content-Type: application/json
 *
 * {
 *   "imageData": "base64-encoded-image-data"
 * }
 * }</pre>
 *
 * <h3>Response Format</h3>
 * <p>All responses include correlation IDs and follow a consistent format:</p>
 * <pre>{@code
 * {
 *   "correlationId": "uuid",
 *   "detectionType": "face",
 *   "detectionCount": 2,
 *   "averageConfidence": 0.85,
 *   "processingTimeMs": 150,
 *   "detections": [...]
 * }
 * }</pre>
 *
 * <h2>Error Handling</h2>
 *
 * <p>The API provides comprehensive error handling:</p>
 * <ul>
 *   <li><strong>400 Bad Request</strong> - Invalid request format or validation errors</li>
 *   <li><strong>413 Payload Too Large</strong> - File size exceeds limits</li>
 *   <li><strong>415 Unsupported Media Type</strong> - Unsupported image format</li>
 *   <li><strong>500 Internal Server Error</strong> - Backend processing errors</li>
 * </ul>
 *
 * <h2>Validation</h2>
 *
 * <p>The API includes comprehensive validation:</p>
 * <ul>
 *   <li><strong>File Validation</strong> - Size limits, content type, format</li>
 *   <li><strong>Request Validation</strong> - Required fields, data format</li>
 *   <li><strong>Detection Type Validation</strong> - Supported detection types</li>
 * </ul>
 *
 * <h2>Security</h2>
 *
 * <p>The API includes security features:</p>
 * <ul>
 *   <li><strong>CORS Support</strong> - Cross-origin resource sharing enabled</li>
 *   <li><strong>Input Validation</strong> - Comprehensive input sanitization</li>
 *   <li><strong>Error Sanitization</strong> - Sensitive information filtered from errors</li>
 * </ul>
 *
 * <h2>Monitoring</h2>
 *
 * <p>The API includes monitoring capabilities:</p>
 * <ul>
 *   <li><strong>Correlation IDs</strong> - Request tracking and debugging</li>
 *   <li><strong>Structured Logging</strong> - JSON-formatted logs with metadata</li>
 *   <li><strong>Performance Metrics</strong> - Processing time and throughput</li>
 *   <li><strong>Health Checks</strong> - Backend status monitoring</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Face Detection with cURL</h3>
 * <pre>{@code
 * curl -X POST http://localhost:8080/api/vision/detect/faces \
 *   -F "file=@image.jpg"
 * }</pre>
 *
 * <h3>Object Detection with JavaScript</h3>
 * <pre>{@code
 * const formData = new FormData();
 * formData.append('file', imageFile);
 *
 * fetch('/api/vision/detect/objects', {
 *   method: 'POST',
 *   body: formData
 * })
 * .then(response => response.json())
 * .then(data => console.log(data));
 * }</pre>
 *
 * <h3>Health Check</h3>
 * <pre>{@code
 * curl http://localhost:8080/api/vision/health
 * }</pre>
 *
 * <h2>Configuration</h2>
 *
 * <p>The API can be configured using application properties:</p>
 * <pre>{@code
 * # File upload limits
 * spring.servlet.multipart.max-file-size=10MB
 * spring.servlet.multipart.max-request-size=10MB
 *
 * # CORS configuration
 * vision.api.cors.allowed-origins=*
 * vision.api.cors.allowed-methods=GET,POST,OPTIONS
 *
 * # Logging
 * logging.level.com.springvision.starter.web=INFO
 * }</pre>
 *
 * <h2>Testing</h2>
 *
 * <p>Comprehensive tests are included for:</p>
 * <ul>
 *   <li>Endpoint functionality and response formats</li>
 *   <li>Request validation and error handling</li>
 *   <li>File upload and processing</li>
 *   <li>JSON request/response serialization</li>
 *   <li>Health and information endpoints</li>
 * </ul>
 *
 * <h2>Dependencies</h2>
 *
 * <p>The web package requires:</p>
 * <ul>
 *   <li>Spring Boot Web Starter</li>
 *   <li>Spring Boot Validation</li>
 *   <li>Jackson for JSON processing</li>
 *   <li>Spring Vision Core and Autoconfigure</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 * @see VisionController
 * @see com.springvision.starter.web.dto
 */
package com.springvision.starter.web;
