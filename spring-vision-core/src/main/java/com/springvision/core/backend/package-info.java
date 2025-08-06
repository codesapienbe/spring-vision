/**
 * Backend implementations for the Spring Vision framework.
 *
 * <p>This package contains concrete implementations of the {@link com.springvision.core.VisionBackend}
 * interface, providing computer vision capabilities through various underlying technologies.</p>
 *
 * <h2>Available Backends</h2>
 *
 * <h3>OpenCV Backend</h3>
 * <p>The {@link com.springvision.core.backend.OpenCvVisionBackend} provides computer vision
 * capabilities using the OpenCV library through JavaCV bindings. It supports:</p>
 * <ul>
 *   <li>Face detection using Haar cascade classifiers</li>
 *   <li>Object detection using various OpenCV algorithms</li>
 *   <li>Basic image processing operations</li>
 *   <li>Real-time performance for most applications</li>
 * </ul>
 *
 * <h3>Demo and Testing</h3>
 * <p>The {@link com.springvision.core.backend.OpenCvDemo} class provides examples of how to
 * use the OpenCV backend for basic image operations, face detection, and result visualization.</p>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Face Detection</h3>
 * <pre>{@code
 * OpenCvVisionBackend backend = new OpenCvVisionBackend();
 * backend.initialize();
 *
 * ImageData imageData = ImageData.fromBytes(imageBytes);
 * VisionResult result = backend.detectFaces(imageData);
 *
 * if (result.hasDetections()) {
 *     result.detections().forEach(detection -> {
 *         System.out.println("Face detected with confidence: " + detection.confidence());
 *     });
 * }
 *
 * backend.shutdown();
 * }</pre>
 *
 * <h3>Multiple Detection Types</h3>
 * <pre>{@code
 * List<DetectionType> detectionTypes = List.of(DetectionType.FACE, DetectionType.OBJECT);
 * List<VisionResult> results = backend.detectMultiple(imageData, detectionTypes);
 * }</pre>
 *
 * <h3>Health Monitoring</h3>
 * <pre>{@code
 * if (backend.isHealthy()) {
 *     BackendHealthInfo healthInfo = backend.getHealthInfo();
 *     System.out.println("Backend is healthy: " + healthInfo.statusMessage());
 * } else {
 *     System.out.println("Backend is unhealthy: " + healthInfo.errorMessage());
 * }
 * }</pre>
 *
 * <h2>Configuration</h2>
 *
 * <p>Backends can be configured through Spring Boot application properties:</p>
 *
 * <pre>{@code
 * vision:
 *   backend: opencv
 *   opencv:
 *     enabled: true
 *     face-cascade-path: /haarcascade_frontalface_default.xml
 *     confidence-threshold: 0.8
 * }</pre>
 *
 * <h2>Dependencies</h2>
 *
 * <p>The OpenCV backend requires the following dependencies:</p>
 * <ul>
 *   <li>OpenCV 4.8.1+</li>
 *   <li>JavaCV 1.5.9+</li>
 *   <li>Java 21+</li>
 * </ul>
 *
 * <p>These dependencies are automatically included when using the Spring Vision starter.</p>
 *
 * <h2>Performance Considerations</h2>
 *
 * <ul>
 *   <li>Face detection performance depends on image size and quality</li>
 *   <li>Large images (>1MB) should be resized before processing</li>
 *   <li>GPU acceleration is available for supported hardware</li>
 *   <li>Memory usage scales with image size and processing complexity</li>
 * </ul>
 *
 * <h2>Error Handling</h2>
 *
 * <p>All backend operations throw {@link com.springvision.core.exception.BaseVisionException}
 * when errors occur. Common error scenarios include:</p>
 * <ul>
 *   <li>OpenCV not available on the system</li>
 *   <li>Face cascade classifier not found</li>
 *   <li>Invalid or corrupted image data</li>
 *   <li>Memory allocation failures</li>
 * </ul>
 *
 * <h2>Testing</h2>
 *
 * <p>Integration tests are provided in {@link com.springvision.core.backend.OpenCvVisionBackendIntegrationTest}
 * to verify backend functionality. Tests are conditionally enabled based on OpenCV availability.</p>
 *
 * <h2>Future Backends</h2>
 *
 * <p>Additional backend implementations are planned:</p>
 * <ul>
 *   <li>MediaPipe backend for advanced ML-based detection</li>
 *   <li>YOLO backend for real-time object detection</li>
 *   <li>TensorFlow Lite backend for mobile deployment</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 * @see com.springvision.core.VisionBackend
 * @see com.springvision.core.VisionTemplate
 * @see com.springvision.core.ImageData
 * @see com.springvision.core.VisionResult
 */
package com.springvision.core.backend;