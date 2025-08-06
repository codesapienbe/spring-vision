/**
 * Core domain classes and interfaces for Spring Vision.
 *
 * <p>This package contains the fundamental abstractions and domain models
 * for computer vision operations in Spring Boot applications. It provides
 * the core interfaces, data structures, and exception classes that form
 * the foundation of the Spring Vision framework.</p>
 *
 * <p>Key components in this package include:</p>
 * <ul>
 *   <li><strong>VisionBackend</strong>: Service Provider Interface for vision backends</li>
 *   <li><strong>VisionTemplate</strong>: Template class for vision operations</li>
 *   <li><strong>ImageData</strong>: Immutable data transfer object for image data</li>
 *   <li><strong>VisionResult</strong>: Result container for vision operations</li>
 *   <li><strong>DetectionType</strong>: Enumeration of supported detection types</li>
 *   <li><strong>BaseVisionException</strong>: Base exception class for vision operations</li>
 * </ul>
 *
 * <p>This package follows the Spring template pattern and provides a
 * consistent, extensible API for computer vision operations regardless
 * of the underlying implementation (OpenCV, MediaPipe, YOLO, etc.).</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @Autowired
 * private VisionTemplate visionTemplate;
 *
 * public void detectFaces(byte[] imageData) {
 *     ImageData data = ImageData.fromBytes(imageData);
 *     VisionResult result = visionTemplate.detectFaces(data);
 *     // Process results
 * }
 * }</pre>
 *
 * @author Spring Vision Team
 * @since 1.0.0
 * @see com.springvision.core.VisionBackend
 * @see com.springvision.core.VisionTemplate
 * @see com.springvision.core.ImageData
 * @see com.springvision.core.VisionResult
 */
package com.springvision.core;
