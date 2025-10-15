package io.github.codesapienbe.springvision.vllm.onnx;

import ai.djl.Application;
import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import io.github.codesapienbe.springvision.core.BackendHealthInfo;
import io.github.codesapienbe.springvision.core.BoundingBox;
import io.github.codesapienbe.springvision.core.Detection;
import io.github.codesapienbe.springvision.core.DetectionType;
import io.github.codesapienbe.springvision.core.ImageData;
import io.github.codesapienbe.springvision.core.VisionBackend;
import io.github.codesapienbe.springvision.core.capabilities.ObjectDetectionCapability;
import io.github.codesapienbe.springvision.core.exception.VisionBackendException;
import io.github.codesapienbe.springvision.core.exception.VisionProcessingException;
import io.github.codesapienbe.springvision.vllm.config.DjlOnnxProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

/**
 * Production-grade DJL-based embedded vision backend with optimized ONNX Runtime integration.
 *
 * <p>This implementation follows best practices for deploying highly optimized ONNX models
 * in Spring Boot environments, addressing the complete optimization pipeline from model
 * selection through execution provider configuration.</p>
 *
 * <h2>Key Architecture Principles:</h2>
 * <ul>
 *   <li><b>Model Selection:</b> Supports ultra-small models (SqueezeNet ~0.5MB) to efficient
 *       alternatives (MobileNetV3, EfficientNet-LITE)</li>
 *   <li><b>Quantization Support:</b> Optimized for INT8 quantized models with 4x size reduction</li>
 *   <li><b>Execution Provider Strategy:</b> Configurable EPs (OpenVINO for Intel CPUs,
 *       CUDA/TensorRT for NVIDIA GPUs) to avoid the INT8 performance paradox</li>
 *   <li><b>Data Contract Enforcement:</b> Strict pre-processing alignment with Python
 *       CalibrationDataReader used during static quantization</li>
 *   <li><b>DJL Translator Pattern:</b> Clean separation between business objects and tensors</li>
 * </ul>
 *
 * <h2>Performance Considerations:</h2>
 * <p>Without proper Execution Provider configuration, INT8 models can perform up to 10x slower
 * than FP32 versions due to QDQ overhead and lack of optimized kernels. This backend ensures
 * hardware-accelerated inference through strategic EP configuration.</p>
 *
 * <h2>Supported Model Architectures:</h2>
 * <ul>
 *   <li>SqueezeNet v1.1 (~1.2M params, 0.5MB quantized) - Absolute minimum size</li>
 *   <li>MobileNetV3-Large (~5.4M params, 6.35MB W8A16) - Balanced efficiency</li>
 *   <li>EfficientNet-LITE (~3.1M params, 12.46MB FP32) - Superior accuracy/efficiency</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @see ai.djl.inference.Predictor
 * @see VisionModelTranslator
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(prefix = "spring.vision.vllm.embedded.djl", name = "enabled", havingValue = "true")
public class OptimizedDjlVisionBackend implements VisionBackend, ObjectDetectionCapability {

    private static final Logger logger = LoggerFactory.getLogger(OptimizedDjlVisionBackend.class);

    private static final String BACKEND_ID = "djl-onnx-optimized";
    private static final String DISPLAY_NAME = "Optimized DJL Vision Backend (ONNX Runtime)";
    private static final String VERSION = "1.0.0";

    private final String modelPath;
    private final String executionProvider;
    private final double confidenceThreshold;
    private final int inputSize;

    private ZooModel<ImageData, float[]> model;
    private VisionModelTranslator translator;
    private volatile boolean initialized = false;
    private volatile BackendHealthInfo.HealthStatus healthStatus = BackendHealthInfo.HealthStatus.UNKNOWN;
    private volatile String healthErrorMessage = "Backend not initialized";
    private volatile long lastHealthCheckTime = 0L;
    private volatile long modelLoadTimeMs = 0L;

    /**
     * Creates backend with default configuration.
     */
    public OptimizedDjlVisionBackend() {
        this("models/squeezenet_int8.onnx", "OpenVINO", 0.7, 224);
    }

    /**
     * Creates backend from properties configuration.
     *
     * @param properties DJL ONNX properties
     */
    public OptimizedDjlVisionBackend(DjlOnnxProperties properties) {
        this(
            properties.modelPath(),
            properties.executionProvider(),
            properties.confidenceThreshold(),
            properties.inputSize()
        );
    }

    /**
     * Creates backend with specified configuration.
     *
     * @param modelPath           path to ONNX model file
     * @param executionProvider   execution provider (OpenVINO, CUDA, TensorRT, CPU)
     * @param confidenceThreshold confidence threshold for detections
     * @param inputSize           input image size (e.g., 224 for SqueezeNet/MobileNet)
     */
    public OptimizedDjlVisionBackend(String modelPath, String executionProvider,
                                     double confidenceThreshold, int inputSize) {
        this.modelPath = Objects.requireNonNull(modelPath, "Model path must not be null");
        this.executionProvider = Objects.requireNonNull(executionProvider, "Execution provider must not be null");
        this.confidenceThreshold = confidenceThreshold;
        this.inputSize = inputSize;

        initialize();
    }

    /**
     * Initializes the DJL model with optimized ONNX Runtime configuration.
     *
     * <p>Critical Configuration Points:</p>
     * <ul>
     *   <li>Execution Provider selection (OpenVINO for CPU, CUDA for GPU)</li>
     *   <li>Model loading from specified path</li>
     *   <li>Translator instantiation with correct pre-processing contract</li>
     * </ul>
     */
    public void initialize() {
        long startTime = System.currentTimeMillis();

        try {
            logger.info("Initializing Optimized DJL Vision Backend");
            logger.info("Model: {}", modelPath);
            logger.info("Execution Provider: {}", executionProvider);
            logger.info("Input Size: {}x{}", inputSize, inputSize);

            // Create translator with strict pre-processing contract
            this.translator = new VisionModelTranslator(
                inputSize,
                inputSize,
                true,      // Enable normalization
                255.0f,    // Normalization factor (MUST match Python calibration)
                null       // Use default ImageNet labels
            );

            // Build model criteria with execution provider configuration
            Path modelFile = Paths.get(modelPath);

            Criteria<ImageData, float[]> criteria = Criteria.builder()
                .setTypes(ImageData.class, float[].class)
                .optModelPath(modelFile)
                .optTranslator(translator)
                .optEngine("OnnxRuntime")
                // CRITICAL: Configure execution provider to avoid INT8 performance paradox
                .optOption("executionProviders", executionProvider)
                .build();

            // Load model
            this.model = criteria.loadModel();

            this.modelLoadTimeMs = System.currentTimeMillis() - startTime;
            this.initialized = true;
            this.healthStatus = BackendHealthInfo.HealthStatus.HEALTHY;
            this.healthErrorMessage = null;
            this.lastHealthCheckTime = System.currentTimeMillis();

            logger.info("Backend initialized successfully in {}ms", modelLoadTimeMs);
            logModelInfo();

        } catch (ModelNotFoundException | MalformedModelException | IOException e) {
            this.healthStatus = BackendHealthInfo.HealthStatus.UNHEALTHY;
            this.healthErrorMessage = "Initialization failed: " + e.getMessage();
            logger.error("Failed to initialize backend", e);
            throw new VisionBackendException("Failed to initialize DJL backend", e);
        }
    }

    /**
     * Logs model information and configuration details.
     */
    private void logModelInfo() {
        try {
            logger.info("Model loaded from: {}", modelPath);
            logger.info("Execution Provider: {}", executionProvider);
            logger.info("Model load time: {}ms", modelLoadTimeMs);
            logger.info("Input dimensions: {}x{}x3", inputSize, inputSize);
            logger.info("Quantization: Optimized for INT8 (if model is quantized)");

            // Log execution provider guidance
            if ("CPU".equals(executionProvider)) {
                logger.warn("Using default CPU EP - INT8 models may be slow!");
                logger.warn("Consider switching to OpenVINO EP for Intel CPUs");
            } else if ("OpenVINO".equals(executionProvider)) {
                logger.info("OpenVINO EP active - optimized for Intel CPU INT8 inference");
            } else if ("CUDA".equals(executionProvider) || "TensorRT".equals(executionProvider)) {
                logger.info("GPU acceleration enabled via {}", executionProvider);
            }

        } catch (Exception e) {
            logger.warn("Failed to log model info", e);
        }
    }

    @Override
    public String getBackendId() {
        return BACKEND_ID;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public Set<DetectionType> getSupportedDetectionTypes() {
        return Set.of(
            DetectionType.OBJECT,
            DetectionType.FACE
        );
    }

    @Override
    public boolean isHealthy() {
        return initialized && healthStatus == BackendHealthInfo.HealthStatus.HEALTHY;
    }

    @Override
    public BackendHealthInfo getHealthInfo() {
        return new BackendHealthInfo(
            BACKEND_ID,
            healthStatus,
            "Optimized DJL Vision Backend Status",
            healthErrorMessage,
            Instant.ofEpochMilli(lastHealthCheckTime),
            modelLoadTimeMs,
            Map.of(
                "modelPath", modelPath,
                "executionProvider", executionProvider,
                "inputSize", String.valueOf(inputSize),
                "initialized", String.valueOf(initialized),
                "framework", "Deep Java Library (DJL)",
                "engine", "ONNX Runtime"
            )
        );
    }

    @Override
    public List<Detection> detectObjects(ImageData imageData) {
        if (!initialized) {
            throw new VisionBackendException("Backend not initialized");
        }

        try (Predictor<ImageData, float[]> predictor = model.newPredictor()) {
            long startTime = System.nanoTime();

            // Run inference through DJL pipeline
            // The Translator handles all pre/post-processing
            float[] probabilities = predictor.predict(imageData);

            long inferenceTimeNs = System.nanoTime() - startTime;
            logger.debug("Inference completed in {}ms", inferenceTimeNs / 1_000_000.0);

            // Convert classification probabilities to Detection objects
            return parseClassificationResults(probabilities);

        } catch (Exception e) {
            logger.error("Error during object detection", e);
            throw new VisionProcessingException("Failed to detect objects: " + e.getMessage(), e);
        }
    }

    /**
     * Converts classification probabilities to Detection objects.
     *
     * @param probabilities classification probabilities from model
     * @return list of detections above confidence threshold
     */
    private List<Detection> parseClassificationResults(float[] probabilities) {
        List<Detection> detections = new ArrayList<>();

        // Get top-5 predictions
        int[] topIndices = translator.getTopK(probabilities, 5);

        for (int idx : topIndices) {
            float confidence = probabilities[idx];

            if (confidence >= confidenceThreshold) {
                String label = translator.getClassLabel(idx);

                // For classification, use full image bounding box
                Detection detection = Detection.of(
                    label,
                    confidence,
                    new BoundingBox(0, 0, inputSize, inputSize),
                    "executionProvider", executionProvider
                );

                detections.add(detection);
            }
        }

        return detections;
    }

    /**
     * Closes the model and releases resources.
     */
    public void close() {
        try {
            if (model != null) {
                model.close();
            }
            logger.info("Optimized DJL Vision Backend closed");
        } catch (Exception e) {
            logger.error("Error closing backend", e);
        }
    }
}
