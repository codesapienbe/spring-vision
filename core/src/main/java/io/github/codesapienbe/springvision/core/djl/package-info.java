/**
 * DJL (Deep Java Library) integration for Spring Vision.
 *
 * <p>This package provides DJL-based model loading and inference capabilities,
 * using verified HuggingFace models through DJL's unified Criteria API.</p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li>Direct HuggingFace model loading with djl:// URLs</li>
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
 * <h2>Model Loading Pattern</h2>
 * <p>All models are loaded using DJL's Criteria builder with explicit HuggingFace model URLs:</p>
 * <pre>{@code
 * // Load face detection model from HuggingFace
 * Criteria<Image, DetectedObjects> criteria = Criteria.builder()
 *     .setTypes(Image.class, DetectedObjects.class)
 *     .optApplication(Application.CV.OBJECT_DETECTION)
 *     .optModelUrls("djl://ai.djl.huggingface.onnx/opencv/face_detection_yunet")
 *     .optEngine("OnnxRuntime")
 *     .optDevice(device)
 *     .optArgument("threshold", 0.6f)
 *     .optProgress(new ProgressBar())
 *     .build();
 *
 * ZooModel<Image, DetectedObjects> model = criteria.loadModel();
 * }</pre>
 *
 * <h2>Verified Models</h2>
 * <ul>
 *   <li><b>Face Detection:</b> opencv/face_detection_yunet (ONNX)</li>
 *   <li><b>Face Recognition:</b> garavv/arcface-onnx (ONNX)</li>
 *   <li><b>Pose Estimation:</b> opencv/pose_estimation_mediapipe (ONNX)</li>
 *   <li><b>Emotion Detection:</b> abhilash88/face-emotion-detection (PyTorch)</li>
 *   <li><b>NSFW Detection:</b> Falconsai/nsfw_image_detection (PyTorch)</li>
 * </ul>
 *
 * @see DjlVisionBackend
 * @see DjlProperties
 * @since 1.0.5
 */
package io.github.codesapienbe.springvision.core.djl;


