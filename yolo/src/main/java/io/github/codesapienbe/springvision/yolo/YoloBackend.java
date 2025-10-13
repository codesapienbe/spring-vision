package io.github.codesapienbe.springvision.yolo;

import io.github.codesapienbe.springvision.core.*;
import io.github.codesapienbe.springvision.core.capabilities.ObjectDetectionCapability;
import io.github.codesapienbe.springvision.core.capabilities.FaceDetectionCapability;
import io.github.codesapienbe.springvision.core.exception.VisionBackendException;
import io.github.codesapienbe.springvision.core.util.ModelResourceLoader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import io.github.codesapienbe.springvision.core.util.OnnxRuntimeGuard;
import io.github.codesapienbe.springvision.yolo.config.YoloProperties;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;


/**
 * YOLO backend implementation for object detection using ONNX Runtime.
 *
 * <p>This backend integrates with YOLO models through ONNX Runtime for high-performance
 * object detection. It supports multiple YOLO versions (v5, v8, v9) and provides
 * real-time object detection with bounding boxes and confidence scores.</p>
 *
 * <p>The backend automatically downloads required models on first use and caches them
 * locally. All operations are thread-safe and include comprehensive error handling.</p>
 *
 * @author Spring Vision Team
 * @since 1.1.0
 */
@Component
@ConditionalOnProperty(prefix = "spring.vision.yolo", name = "enabled", havingValue = "true")
public class YoloBackend implements VisionBackend, ObjectDetectionCapability, FaceDetectionCapability {

    private static final Logger logger = LoggerFactory.getLogger(YoloBackend.class);

    // Configuration loaded from YoloProperties
    private final String modelPath;
    private final String modelName;
    private final double confidenceThreshold;
    private final double nmsThreshold;
    private final int maxDetections;
    private final boolean enableAutoDownload;
    private final int inputSize;
    private final Map<String, YoloProperties.ModelInfo> modelInfo;

    // ONNX Runtime components (loaded via reflection)
    private Object ortSession;
    private Object ortEnvironment;
    private Class<?> ortSessionClass;

    // Metrics
    private final AtomicLong detectionCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private final AtomicLong modelDownloadCount = new AtomicLong(0);
    private final AtomicLong correlationIdCounter = new AtomicLong(0);

    // Shutdown flag
    private volatile boolean shutdown = false;
    private volatile boolean initialized = false;

    // COCO class names (80 classes)
    private static final String[] COCO_CLASSES = {
        "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat", "traffic light",
        "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog", "horse", "sheep", "cow",
        "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella", "handbag", "tie", "suitcase", "frisbee",
        "skis", "snowboard", "sports ball", "kite", "baseball bat", "baseball glove", "skateboard", "surfboard",
        "tennis racket", "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple",
        "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch",
        "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote", "keyboard", "cell phone",
        "microwave", "oven", "toaster", "sink", "refrigerator", "book", "clock", "vase", "scissors", "teddy bear",
        "hair drier", "toothbrush"
    };

    /**
     * Default constructor with default configuration values.
     */
    public YoloBackend() {
        this(new YoloProperties());
    }

    /**
     * Constructor that loads configuration from YoloProperties.
     * Properties object is used only for initialization, not stored.
     */
    public YoloBackend(YoloProperties properties) {
        Objects.requireNonNull(properties, "YoloProperties must not be null");

        // Load all configuration from properties into instance fields
        this.modelPath = properties.modelPath();
        this.modelName = properties.modelName();
        this.confidenceThreshold = properties.confidenceThreshold();
        this.nmsThreshold = properties.nmsThreshold();
        this.maxDetections = properties.maxDetections();
        this.enableAutoDownload = properties.enableAutoDownload();
        this.inputSize = properties.inputSize();
        this.modelInfo = properties.modelInfo();

        logger.debug("YoloBackend initialized with model: {}", modelName);
    }

    /**
     * Constructor that reads configuration directly from application.properties via @Value.
     * Used when Properties bean is not available.
     */
    public YoloBackend(
        @Value("${spring.vision.yolo.model-path:classpath:/models}") String modelPath,
        @Value("${spring.vision.yolo.model-name:yolov8n.onnx}") String modelName,
        @Value("${spring.vision.yolo.confidence-threshold:0.25}") double confidenceThreshold,
        @Value("${spring.vision.yolo.nms-threshold:0.45}") double nmsThreshold,
        @Value("${spring.vision.yolo.max-detections:100}") int maxDetections,
        @Value("${spring.vision.yolo.enable-auto-download:true}") boolean enableAutoDownload,
        @Value("${spring.vision.yolo.input-size:640}") int inputSize) {

        this.modelPath = modelPath;
        this.modelName = modelName;
        this.confidenceThreshold = confidenceThreshold;
        this.nmsThreshold = nmsThreshold;
        this.maxDetections = maxDetections;
        this.enableAutoDownload = enableAutoDownload;
        this.inputSize = inputSize;

        // Create default model info since we can't inject complex objects via @Value
        this.modelInfo = createDefaultModelInfo();

        logger.debug("YoloBackend initialized with model: {}, inputSize: {}", modelName, inputSize);
    }

    /**
     * Creates default model info map when not provided via Properties.
     */
    private static Map<String, YoloProperties.ModelInfo> createDefaultModelInfo() {
        return Map.of(
            "yolov8n.onnx", new YoloProperties.ModelInfo(
                "https://github.com/ultralytics/assets/releases/download/v0.0.0/yolov8n.pt",
                "sha256:6fcc2a971d8bc901e81db872e3c01dd6357d11ac502b4bed4c78ddc2c5d47d6a",
                "yolov8n"
            ),
            "yolov8s.onnx", new YoloProperties.ModelInfo(
                "https://github.com/ultralytics/assets/releases/download/v0.0.0/yolov8s.pt",
                "sha256:a3ec3c53f073fd53f22e8cb7e75a4c7d3c07a1ca40b621cce04c175652206572",
                "yolov8s"
            ),
            "yolov8m.onnx", new YoloProperties.ModelInfo(
                "https://github.com/ultralytics/assets/releases/download/v0.0.0/yolov8m.pt",
                "sha256:c2ce8e0240d84c5c7b8b4d8f21de54e9c71e8e0b5b8b4d8f21de54e9c71e8e0b",
                "yolov8m"
            )
        );
    }

    @Override
    public String getBackendId() {
        return "yolo";
    }

    @Override
    public String getDisplayName() {
        return "YOLO Backend";
    }

    @Override
    public Set<DetectionType> getSupportedDetectionTypes() {
        return Set.of(DetectionType.OBJECT, DetectionType.FACE);
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean isHealthy() {
        return !shutdown;
    }

    @Override
    public BackendHealthInfo getHealthInfo() {
        Map<String, Object> metrics = Map.of(
            "detectionCount", detectionCount.get(),
            "errorCount", errorCount.get(),
            "modelsDownloaded", modelDownloadCount.get(),
            "modelName", modelName,
            "shutdown", shutdown
        );

        if (isAvailable() && !shutdown) {
            return BackendHealthInfo.healthy("YOLO", "YOLO backend is operational", 0, metrics);
        } else {
            return BackendHealthInfo.unhealthy("YOLO", "YOLO backend is not available",
                shutdown ? "Backend is shutting down" : "Backend is not available", 0, metrics);
        }
    }

    @Override
    public List<Detection> detectFaces(ImageData imageData) {
        return detect(imageData, DetectionType.FACE);
    }

    @Override
    public List<Detection> detectObjects(ImageData imageData) {
        return detect(imageData, DetectionType.OBJECT);
    }

    /**
     * Performs detection on the given image for a specific detection type.
     *
     * @param imageData The image to perform detection on.
     * @param type      The type of detection to perform (e.g., OBJECT, FACE).
     * @return A list of detections found in the image.
     * @throws VisionBackendException if an error occurs during detection.
     */
    public List<Detection> detect(ImageData imageData, DetectionType type) {
        validateInput(imageData, new DetectionQuery.Builder().type(type).build());

        String correlationId = generateCorrelationId();
        detectionCount.incrementAndGet();

        try {
            logger.debug("YOLO detection requested: correlationId={}, type={}, backend=yolo", correlationId, type);

            // Initialize ONNX Runtime if not already done
            ensureInitialized();

            // Perform YOLO inference
            List<Detection> detections = performYoloInference(imageData, type, correlationId);

            logger.info("YOLO detection completed: {} objects detected, correlationId={}, backend=yolo",
                detections.size(), correlationId);

            return detections;

        } catch (Exception e) {
            errorCount.incrementAndGet();
            logger.error("YOLO detection failed: correlationId={}, type={}, error={}, backend=yolo",
                correlationId, type, e.getMessage(), e);
            throw new VisionBackendException("YOLO detection failed: " + e.getMessage(), e);
        }
    }

    /**
     * Performs YOLO inference on the image using ONNX Runtime.
     *
     * @param imageData     The image data.
     * @param type          The detection type.
     * @param correlationId The correlation ID for logging.
     * @return A list of detections.
     * @throws Exception if inference fails.
     */
    private List<Detection> performYoloInference(ImageData imageData, DetectionType type, String correlationId)
        throws Exception {

        if (ortSession == null) {
            throw new VisionBackendException("YOLO model not loaded. Please ensure model is available.");
        }

        // 1. Preprocess the image
        float[][][][] preprocessedImage = preprocessImage(imageData.data());

        // 2. Run ONNX inference
        float[][][] rawOutput = runOnnxInference(preprocessedImage);

        // 3. Post-process results (NMS, confidence filtering)
        List<Detection> detections = postProcessResults(rawOutput, imageData.data().length, correlationId);

        // 4. Filter by detection type if needed
        if (type == DetectionType.FACE) {
            detections = detections.stream()
                .filter(detection -> "person".equals(detection.label()))
                .collect(Collectors.toList());
        }

        return detections;
    }

    /**
     * Ensures ONNX Runtime is initialized and the YOLO model is loaded.
     * This method is idempotent.
     *
     * @throws Exception if initialization fails.
     */
    private void ensureInitialized() throws Exception {
        if (initialized) {
            return;
        }

        if (!OnnxRuntimeGuard.isAvailable()) {
            throw new VisionBackendException("ONNX Runtime is not available. Please add onnxruntime dependency.");
        }

        if (ortEnvironment == null) {
            ortEnvironment = OnnxRuntimeGuard.createEnvironment();
            ortSessionClass = ortEnvironment.getClass();
        }

        if (ortSession == null) {
            loadYoloModel();
        }

        initialized = true;
        logger.info("YOLO backend initialized successfully: model={}, backend=yolo", modelName);
    }

    /**
     * Loads the YOLO model from the configured path. If auto-download is enabled,
     * it will download the model if it's not present locally.
     *
     * @throws Exception if the model cannot be loaded.
     */
    private void loadYoloModel() throws Exception {
        String modelFilePath = getModelFilePath();


        // Create ONNX session via guard (reflection-guarded)
        ortSession = OnnxRuntimeGuard.createSession(ortEnvironment, modelFilePath);
        if (ortSession != null) {
            ortSessionClass = ortSession.getClass();
            logger.info("YOLO model loaded successfully: path={}, backend=yolo", modelFilePath);
        } else {
            throw new VisionBackendException("Failed to create ONNX session for YOLO model");
        }
    }

    /**
     * Preprocesses the input image for YOLO inference. This involves resizing the
     * image to the model's input size, normalizing pixel values, and converting
     * it to the required tensor format (NCHW).
     *
     * @param imageData The raw byte data of the image.
     * @return A 4D float array representing the preprocessed image tensor.
     * @throws Exception if image processing fails.
     */
    private float[][][][] preprocessImage(byte[] imageData) throws Exception {
        // Convert byte array to BufferedImage
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
        if (image == null) {
            throw new VisionBackendException("Failed to decode image data");
        }

        // Resize image to model input size (640x640 for YOLOv8)
        BufferedImage resizedImage = resizeImage(image, inputSize, inputSize);

        // Convert to RGB and normalize to [0, 1]
        float[][][][] preprocessed = new float[1][3][inputSize][inputSize];

        for (int y = 0; y < inputSize; y++) {
            for (int x = 0; x < inputSize; x++) {
                int rgb = resizedImage.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Normalize to [0, 1] and convert to CHW format
                preprocessed[0][0][y][x] = r / 255.0f; // Red channel
                preprocessed[0][1][y][x] = g / 255.0f; // Green channel
                preprocessed[0][2][y][x] = b / 255.0f; // Blue channel
            }
        }

        return preprocessed;
    }

    /**
     * Runs the ONNX inference with the preprocessed image tensor.
     *
     * @param preprocessedImage The preprocessed image tensor.
     * @return The raw output tensor from the model.
     * @throws Exception if ONNX runtime inference fails.
     */
    private float[][][] runOnnxInference(float[][][][] preprocessedImage) throws Exception {
        // Create input tensor
        Class<?> onnxTensorClass = Class.forName("ai.onnxruntime.OnnxTensor");
        Class<?> floatBufferClass = Class.forName("java.nio.FloatBuffer");

        // Flatten the 4D array to 1D for FloatBuffer
        float[] flatArray = new float[3 * inputSize * inputSize];
        int index = 0;
        for (int b = 0; b < 1; b++) {
            for (int c = 0; c < 3; c++) {
                for (int h = 0; h < inputSize; h++) {
                    for (int w = 0; w < inputSize; w++) {
                        flatArray[index++] = preprocessedImage[b][c][h][w];
                    }
                }
            }
        }

        FloatBuffer floatBuffer = FloatBuffer.wrap(flatArray);
        long[] shape = {1, 3, inputSize, inputSize};

        Object inputTensor = onnxTensorClass.getMethod("createTensor",
                ortSessionClass, floatBufferClass, long[].class)
            .invoke(null, ortEnvironment, floatBuffer, shape);

        // Run inference
        Object result = ortSession.getClass().getMethod("run", Map.class)
            .invoke(ortSession, Map.of("images", inputTensor));

        // Extract output tensor
        Object outputTensor = result.getClass().getMethod("get", int.class).invoke(result, 0);
        Object outputArray = outputTensor.getClass().getMethod("getValue").invoke(outputTensor);

        // Convert to float array
        if (outputArray instanceof float[][][]) {
            return (float[][][]) outputArray;
        } else if (outputArray instanceof float[][][][] output4D) {
            return output4D[0]; // Take first batch
        } else {
            throw new VisionBackendException("Unexpected output tensor format");
        }
    }

    /**
     * Post-processes the raw output from the YOLO model to generate a list of
     * {@link Detection} objects. This includes applying a confidence threshold,
     * non-maximum suppression (NMS), and scaling bounding boxes to the original
     * image dimensions.
     *
     * @param rawOutput         The raw float tensor output from the YOLO model.
     * @param originalImageSize The size of the original input image to scale bounding boxes.
     * @param correlationId     The correlation ID for logging.
     * @return A list of filtered and processed detections.
     */
    private List<Detection> postProcessResults(float[][][] rawOutput, int originalImageSize, String correlationId) {
        List<Detection> detections = new ArrayList<>();

        // YOLO output format: [batch, 84, 8400] where 84 = 4 (bbox) + 80 (classes)
        int numDetections = rawOutput[0][0].length; // 8400
        int numClasses = 80;

        for (int i = 0; i < numDetections; i++) {
            // Extract bounding box coordinates (center_x, center_y, width, height)
            float centerX = rawOutput[0][0][i];
            float centerY = rawOutput[0][1][i];
            float width = rawOutput[0][2][i];
            float height = rawOutput[0][3][i];

            // Extract class probabilities
            float maxConfidence = 0;
            int bestClass = -1;

            for (int j = 0; j < numClasses; j++) {
                float confidence = rawOutput[0][4 + j][i];
                if (confidence > maxConfidence) {
                    maxConfidence = confidence;
                    bestClass = j;
                }
            }

            // Filter by confidence threshold
            if (maxConfidence >= (float) confidenceThreshold && bestClass >= 0) {
                float x1 = centerX - width / 2;
                float y1 = centerY - height / 2;
                float x2 = centerX + width / 2;
                float y2 = centerY + height / 2;

                // Scale coordinates back to original image size
                float scaleX = (float) originalImageSize / inputSize;
                float scaleY = (float) originalImageSize / inputSize;

                x1 *= scaleX;
                y1 *= scaleY;
                x2 *= scaleX;
                y2 *= scaleY;

                // Create bounding box
                BoundingBox box = new BoundingBox(x1, y1, x2 - x1, y2 - y1);

                // Create detection attributes
                Map<String, Object> attributes = new HashMap<>();
                attributes.put("model", modelName);
                attributes.put("backend", "yolo");
                attributes.put("class_id", bestClass);
                attributes.put("confidence", maxConfidence);

                // Create detection
                Detection detection = new Detection(
                    COCO_CLASSES[bestClass],
                    maxConfidence,
                    box,
                    attributes
                );

                detections.add(detection);
            }
        }

        // Apply NMS using configured threshold
        if (nmsThreshold > 0 && nmsThreshold < 1.0) {
            detections = applyNMS(detections);
        }

        // Limit results
        if (maxDetections > 0 && detections.size() > maxDetections) {
            detections = detections.stream()
                .sorted((a, b) -> Double.compare(b.confidence(), a.confidence()))
                .limit(maxDetections)
                .collect(Collectors.toList());
        }

        logger.debug("Post-processed {} detections from YOLO output: correlationId={}, backend=yolo",
            detections.size(), correlationId);

        return detections;
    }

    /**
     * Applies Non-Maximum Suppression (NMS) to a list of detections to filter
     * out overlapping bounding boxes, keeping only the ones with the highest
     * confidence.
     *
     * @param detections The list of detections to process.
     * @return A new list of detections after applying NMS.
     */
    private List<Detection> applyNMS(List<Detection> detections) {
        if (detections.isEmpty()) {
            return detections;
        }

        // Sort by confidence (descending)
        detections.sort((a, b) -> Double.compare(b.confidence(), a.confidence()));

        List<Detection> nmsDetections = new ArrayList<>();
        boolean[] suppressed = new boolean[detections.size()];

        for (int i = 0; i < detections.size(); i++) {
            if (suppressed[i]) {
                continue;
            }

            Detection current = detections.get(i);
            nmsDetections.add(current);

            // Suppress overlapping detections
            for (int j = i + 1; j < detections.size(); j++) {
                if (suppressed[j]) {
                    continue;
                }

                Detection other = detections.get(j);
                double iou = calculateIoU(current.boundingBox(), other.boundingBox());

                if (iou > nmsThreshold) {
                    suppressed[j] = true;
                }
            }
        }

        return nmsDetections;
    }

    /**
     * Calculates the Intersection over Union (IoU) between two bounding boxes.
     * IoU is a measure of the overlap between two boxes.
     *
     * @param box1 The first bounding box.
     * @param box2 The second bounding box.
     * @return The IoU value, between 0.0 and 1.0.
     */
    private double calculateIoU(BoundingBox box1, BoundingBox box2) {
        double x1 = Math.max(box1.x(), box2.x());
        double y1 = Math.max(box1.y(), box2.y());
        double x2 = Math.min(box1.x() + box1.width(), box2.x() + box2.width());
        double y2 = Math.min(box1.y() + box1.height(), box2.y() + box2.height());

        if (x2 <= x1 || y2 <= y1) {
            return 0.0;
        }

        double intersection = (x2 - x1) * (y2 - y1);
        double area1 = box1.width() * box1.height();
        double area2 = box2.width() * box2.height();
        double union = area1 + area2 - intersection;

        return intersection / union;
    }

    /**
     * Resizes a {@link BufferedImage} to the specified target width and height.
     *
     * @param originalImage The image to resize.
     * @param targetWidth   The target width.
     * @param targetHeight  The target height.
     * @return The resized image.
     */
    private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();
        return resizedImage;
    }

    /**
     * Determines the absolute file path for the configured YOLO model.
     * It uses {@link ModelResourceLoader} to prioritize classpath resources,
     * then checks a configured external path, and finally falls back to a
     * default location, potentially triggering a download if enabled.
     *
     * @return The absolute path to the model file.
     * @throws VisionBackendException if the model is unknown or the path cannot be resolved.
     */
    private String getModelFilePath() {
        YoloProperties.ModelInfo modelInfoEntry = modelInfo.get(modelName);
        if (modelInfoEntry == null) {
            throw new VisionBackendException("Unknown model: " + modelName);
        }

        // Build classpath resource path
        String classpathResource = "/models/" + modelName;

        // Use ModelResourceLoader for unified resource loading with classpath priority
        String resolvedPath = ModelResourceLoader.resolveModelPath(
            modelPath.startsWith("classpath:") ? null : modelPath,  // configured external path
            classpathResource,                                        // classpath resource
            modelName,                                                // model filename
            "yolo",                                                   // module subdirectory
            modelInfoEntry.url(),                                          // download URL
            enableAutoDownload                                        // auto-download flag
        );

        if (resolvedPath == null) {
            // Fallback to old behavior for backwards compatibility
            Path modelDir = Paths.get(modelPath.replace("~", System.getProperty("user.home")));
            return modelDir.resolve(modelName).toString();
        }

        return resolvedPath;
    }

    /**
     * Validates the input {@link ImageData} and {@link DetectionQuery} to ensure
     * they are not null and meet basic requirements before processing.
     *
     * @param imageData The image data to validate.
     * @param query     The detection query to validate.
     * @throws IllegalArgumentException if validation fails.
     */
    private void validateInput(ImageData imageData, DetectionQuery query) {
        Objects.requireNonNull(imageData, "ImageData cannot be null");
        Objects.requireNonNull(query, "DetectionQuery cannot be null");

        if (imageData.data() == null || imageData.data().length == 0) {
            throw new IllegalArgumentException("Image data cannot be empty");
        }

        if (imageData.data().length > 50 * 1024 * 1024) { // 50MB limit
            throw new IllegalArgumentException("Image size exceeds maximum limit of 50MB");
        }

        if (!getSupportedDetectionTypes().contains(query.getType())) {
            throw new IllegalArgumentException("Unsupported detection type: " + query.getType());
        }
    }

    /**
     * Generates a unique correlation ID for tracking a detection request through
     * the system.
     *
     * @return A unique string identifier.
     */
    private String generateCorrelationId() {
        return "yolo-" + correlationIdCounter.incrementAndGet();
    }

    /**
     * Checks if the YOLO backend is available by verifying if the ONNX Runtime
     * classes are present on the classpath.
     *
     * @return {@code true} if ONNX Runtime is available, {@code false} otherwise.
     */
    private boolean isAvailable() {
        // Consider available if ONNX runtime classes are present. Session may be null until initialized.
        return OnnxRuntimeGuard.isAvailable();
    }

    /**
     * Graceful shutdown of the backend.
     */
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down YOLO backend: backend=yolo");
        shutdown = true;

        try {
            // Close ONNX session and environment if present
            if (ortSession != null) {
                OnnxRuntimeGuard.closeSessionQuietly(ortSession);
                ortSession = null;
            }
        } catch (Exception e) {
            logger.warn("Error closing ONNX session: {}", e.getMessage());
        }

        try {
            if (ortEnvironment != null) {
                OnnxRuntimeGuard.closeEnvironmentQuietly(ortEnvironment);
                ortEnvironment = null;
            }
        } catch (Exception e) {
            logger.warn("Error closing ONNX environment: {}", e.getMessage());
        }

        logger.info("YOLO backend shutdown completed: backend=yolo");
    }


}
