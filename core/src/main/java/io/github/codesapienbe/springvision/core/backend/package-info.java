/**
 * Default backend implementation for the Spring Vision framework.
 *
 * <p>This package contains the default OpenCV backend implementation of the
 * {@link io.github.codesapienbe.springvision.core.VisionBackend} interface. Other backend implementations
 * are available as separate modules (spring-vision-mediapipe, spring-vision-deepface, etc.).</p>
 *
 * <h2>Default Backend</h2>
 *
 * <h3>OpenCV Backend</h3>
 * <p>The {@link io.github.codesapienbe.springvision.core.backend.OpenCvVisionBackend} provides computer vision
 * capabilities using the OpenCV library through JavaCV bindings. It supports:</p>
 * <ul>
 *   <li>Face detection using Haar cascade classifiers</li>
 *   <li>Object detection using various OpenCV algorithms</li>
 *   <li>Basic image processing operations</li>
 *   <li>Real-time performance for most applications</li>
 * </ul>
 *
 * <h2>Additional Backends</h2>
 * <p>Additional backend implementations are available as separate modules:</p>
 * <ul>
 *   <li><code>spring-vision-mediapipe</code> - MediaPipe backend for advanced face/hand/pose detection</li>
 *   <li><code>spring-vision-deepface</code> - DeepFace backend for face recognition and analysis</li>
 *   <li><code>spring-vision-compreface</code> - CompreFace backend for face recognition</li>
 *   <li><code>spring-vision-insightface</code> - InsightFace backend for high-accuracy face recognition</li>
 *   <li><code>spring-vision-yolo</code> - YOLO backend for object detection</li>
 * </ul>
 *
 * <h3>Testing</h3>
 * <p>Integration tests are provided to verify backend functionality.
 * Tests are conditionally enabled based on backend availability.</p>
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
 * <p>All backend operations throw {@link io.github.codesapienbe.springvision.core.exception.BaseVisionException}
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
 * <p>Integration tests are provided to verify backend functionality.
 * Tests are conditionally enabled based on backend availability.</p>
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
 * @see io.github.codesapienbe.springvision.core.VisionBackend
 * @see io.github.codesapienbe.springvision.core.VisionTemplate
 * @see io.github.codesapienbe.springvision.core.ImageData
 * @see io.github.codesapienbe.springvision.core.VisionResult
 * @since 1.0.0
 */
package io.github.codesapienbe.springvision.core.backend;
