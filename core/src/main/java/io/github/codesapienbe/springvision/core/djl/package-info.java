/**
 * DJL (Deep Java Library) integration for Spring Vision.
 *
 * <p>This package provides DJL-based model loading and inference capabilities,
 * replacing custom model loading code with DJL's unified ModelZoo API.</p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li>Unified model loading from multiple sources (local, URL, S3, HDFS)</li>
 *   <li>Support for multiple frameworks (PyTorch, ONNX, TensorFlow, MXNet)</li>
 *   <li>Automatic model versioning and caching</li>
 *   <li>Thread-safe inference with built-in pooling</li>
 *   <li>GPU acceleration support</li>
 *   <li>Progress tracking for model downloads</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Enable DJL backend in application.properties
 * spring.vision.djl.enabled=true
 * spring.vision.djl.engine=PyTorch
 *
 * // Use the backend
 * @Autowired
 * private DjlVisionBackend djlBackend;
 *
 * List<Detection> faces = djlBackend.detectFaces(imageData);
 * }</pre>
 *
 * <h2>Model Loading</h2>
 * <pre>{@code
 * // Load from local path
 * ZooModel<Image, DetectedObjects> model = DjlModelLoader.loadFromPath(
 *     "/path/to/model",
 *     "model_name",
 *     Image.class,
 *     DetectedObjects.class
 * );
 *
 * // Load from URL
 * ZooModel<Image, DetectedObjects> model = DjlModelLoader.loadFromUrl(
 *     "https://example.com/model.zip",
 *     Image.class,
 *     DetectedObjects.class
 * );
 *
 * // Load from ModelZoo
 * ZooModel<Image, DetectedObjects> model = DjlModelLoader.loadFromModelZoo(
 *     "ai.djl.pytorch:resnet",
 *     Image.class,
 *     DetectedObjects.class
 * );
 * }</pre>
 *
 * @see DjlModelLoader
 * @see DjlVisionBackend
 * @see DjlProperties
 * @since 1.0.5
 */
package io.github.codesapienbe.springvision.core.djl;

