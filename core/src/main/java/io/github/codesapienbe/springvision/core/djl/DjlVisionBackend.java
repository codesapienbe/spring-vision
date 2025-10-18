package io.github.codesapienbe.springvision.core.djl;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Joints;
import ai.djl.modality.cv.output.CategoryMask;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import ai.djl.Application;
import ai.djl.Device;
import ai.djl.training.util.ProgressBar;
import io.github.codesapienbe.springvision.core.*;
import io.github.codesapienbe.springvision.core.capabilities.*;
import io.github.codesapienbe.springvision.core.exception.BaseVisionException;
import io.github.codesapienbe.springvision.core.exception.VisionBackendException;
import io.github.codesapienbe.springvision.core.exception.VisionProcessingException;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.types.DataType;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import ai.djl.ndarray.NDManager;
import io.github.codesapienbe.springvision.core.djl.translator.YuNetFaceDetectionTranslator;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.GenericMultipleBarcodeReader;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.Directory;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.exif.ExifSubIFDDirectory;

/**
 * Enhanced DJL-based vision backend with comprehensive computer vision capabilities.
 *
 * <p>This backend uses Deep Java Library (DJL) to provide:</p>
 * <ul>
 *   <li>Face detection and recognition</li>
 *   <li>Object detection (80+ COCO classes)</li>
 *   <li>Pose estimation (17-joint human pose)</li>
 *   <li>Action recognition</li>
 *   <li>Instance and semantic segmentation</li>
 *   <li>Embedding extraction</li>
 *   <li>Support for PyTorch, ONNX, TensorFlow, MXNet</li>
 *   <li>Automatic GPU acceleration when available</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 1.0.5
 */
@Component
@ConditionalOnProperty(prefix = "spring.vision.djl", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DjlVisionBackend implements VisionBackend,
    FaceDetectionCapability,
    ObjectDetectionCapability,
    PoseEstimationCapability,
    ActionRecognitionCapability,
    SegmentationCapability,
    EmbeddingCapability,
    OcrCapability,
    ImageClassificationCapability,
    BarcodeCapability,
    MetaDataExtractionCapability,
    AnnotationCapability {

    private static final Logger logger = LoggerFactory.getLogger(DjlVisionBackend.class);

    public static final String BACKEND_ID = "djl";
    public static final String DISPLAY_NAME = "DJL Vision Backend";
    public static final String VERSION = "1.0.5-DJL-0.33.0";

    // Models
    private ZooModel<Image, DetectedObjects> faceDetectionModel;
    private ZooModel<Image, float[]> faceRecognitionModel;
    private ZooModel<Image, DetectedObjects> objectDetectionModel;
    private ZooModel<Image, Joints> poseEstimationModel;
    private ZooModel<Image, float[]> actionRecognitionModel;
    private ZooModel<Image, CategoryMask> semanticSegmentationModel;
    private ZooModel<Image, DetectedObjects> instanceSegmentationModel;

    private final DjlProperties properties;
    private final Device device;

    // Lifecycle and health tracking
    private boolean initialized = false;
    private BackendHealthInfo.HealthStatus healthStatus = BackendHealthInfo.HealthStatus.UNKNOWN;
    private String healthErrorMessage = "Backend not initialized";
    private long lastHealthCheckTime;

    // Model loading cache
    private final Map<String, Object> modelCache = new ConcurrentHashMap<>();

    // Concurrency control
    private final Semaphore inferenceSemaphore;
    private final int maxConcurrentInferences;

    // Functional callback for predictor usage
    @FunctionalInterface
    private interface PredictorCallback<I, O, R> {
        R apply(Predictor<I, O> predictor) throws Exception;
    }

    // Constructors
    public DjlVisionBackend() {
        this.properties = new DjlProperties();
        this.device = safeCreateDevice(this.properties.getDevice());

        // Align concurrency with available hardware unless the user explicitly set a different value.
        int cores = Math.max(1, Runtime.getRuntime().availableProcessors());
        int configured = this.properties.getMaxConcurrentInferences();
        if (configured == 4) { // heuristic: 4 is the default - treat as unspecified
            this.maxConcurrentInferences = cores;
        } else {
            // respect user configuration but don't exceed cores
            this.maxConcurrentInferences = Math.max(1, Math.min(configured, cores));
        }
        this.inferenceSemaphore = new Semaphore(this.maxConcurrentInferences);

        logBackendInfo();
    }

    public DjlVisionBackend(DjlProperties properties) {
        this.properties = properties != null ? properties : new DjlProperties();
        this.device = safeCreateDevice(this.properties.getDevice());

        int cores = Math.max(1, Runtime.getRuntime().availableProcessors());
        int configured = this.properties.getMaxConcurrentInferences();
        if (configured == 4) {
            this.maxConcurrentInferences = cores;
        } else {
            this.maxConcurrentInferences = Math.max(1, Math.min(configured, cores));
        }
        this.inferenceSemaphore = new Semaphore(this.maxConcurrentInferences);

        logBackendInfo();
    }

    private Device safeCreateDevice(String deviceName) {
        try {
            // Device.fromName may trigger engine/native initialization in some DJL versions.
            // Wrap in try/catch and fall back to CPU to avoid JNI downloads during tests or in constrained environments.
            return Device.fromName(deviceName);
        } catch (Throwable t) {
            logger.warn("Failed to initialize DJL device '{}' - falling back to CPU. Reason: {}", deviceName, t.toString());
            try {
                return Device.cpu();
            } catch (Throwable t2) {
                // Extremely defensive: if even Device.cpu() fails, return a best-effort null and let callers handle it.
                logger.error("Failed to create fallback CPU Device for DJL: {}", t2.toString());
                return null;
            }
        }
    }

    private void logBackendInfo() {
        // Avoid calling Engine.getEngine(...) here since that can trigger native/JNI downloads in some environments.
        String configuredEngine = properties.getEngine();
        String deviceName = properties.getDevice();
        boolean gpuAvailable = false;
        try {
            gpuAvailable = (device != null) && device.isGpu();
        } catch (Throwable ignored) {
            // ignore - some DJL versions may throw when querying device properties
        }

        logger.info("Initializing DJL Vision Backend - Engine: {}, Device: {}, Version: {}",
            configuredEngine, deviceName, VERSION);
        logger.info("DJL Engine (configured): {}, GPU Available: {}", configuredEngine, gpuAvailable);
        logger.info("DJL concurrency: maxConcurrentInferences={}", this.maxConcurrentInferences);
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
            DetectionType.FACE,
            DetectionType.OBJECT,
            DetectionType.BODY,      // For pose estimation
            DetectionType.SCENE      // For segmentation
        );
    }

    @Override
    public boolean isHealthy() {
        return healthStatus == BackendHealthInfo.HealthStatus.HEALTHY && initialized;
    }

    @Override
    public BackendHealthInfo getHealthInfo() {
        long responseTime = System.currentTimeMillis() - lastHealthCheckTime;

        Map<String, Object> details = new HashMap<>();
        details.put("engine", properties.getEngine());
        details.put("device", properties.getDevice());
        boolean gpuAvailable = false;
        try {
            gpuAvailable = (device != null) && device.isGpu();
        } catch (Exception e) {
            // GPU detection not available in this DJL version
        }
        details.put("gpuAvailable", gpuAvailable);
        details.put("modelsLoaded", modelCache.size());
        details.put("maxConcurrentInferences", this.maxConcurrentInferences);

        if (healthStatus == BackendHealthInfo.HealthStatus.HEALTHY) {
            // Use the overload that accepts metrics so the details map is included
            return BackendHealthInfo.healthy(getBackendId(),
                "DJL backend is operational", responseTime, details);
        } else {
            // Include metrics even for unhealthy responses to aid debugging
            return BackendHealthInfo.unhealthy(getBackendId(),
                "DJL backend is not operational", healthErrorMessage, responseTime, details);
        }
    }

    @Override
    public void initialize() throws BaseVisionException {
        logger.info("Initializing DJL vision backend with all capabilities");

        try {
            // Load face detector first for accurate face counts
            try {
                loadFaceDetectionModel();
            } catch (Exception e) {
                logger.warn("Failed to load dedicated face detection model: {}. Falling back to generic object detection.", e.getMessage());
            }

            // Load core models
            loadObjectDetectionModel();

            // Only load optional models if configured
            String poseModel = properties.getPoseEstimation() != null ? properties.getPoseEstimation().getModel() : null;
            if (poseModel != null && !poseModel.isBlank()) {
                try {
                    loadPoseEstimationModel();
                } catch (Exception e) {
                    logger.warn("Failed to load pose estimation model: {}", e.getMessage());
                }
            } else {
                logger.info("Pose estimation model not configured or blank; skipping pose model load.");
            }

            String actionModel = properties.getActionRecognition() != null ? properties.getActionRecognition().getModel() : null;
            if (actionModel != null && !actionModel.isBlank()) {
                try {
                    loadActionRecognitionModel();
                } catch (Exception e) {
                    logger.warn("Failed to load action recognition model: {}", e.getMessage());
                }
            } else {
                logger.info("Action recognition model not configured or blank; skipping action recognition model load.");
            }

            if (properties.getSegmentation() != null) {
                try {
                    loadSegmentationModels();
                } catch (Exception e) {
                    logger.warn("Failed to load segmentation models: {}", e.getMessage());
                }
            }

            if (properties.getFaceRecognition() != null) {
                try {
                    loadFaceRecognitionModel();
                } catch (Exception e) {
                    logger.warn("Failed to load face recognition model: {}", e.getMessage());
                }
            }

            initialized = true;
            healthStatus = BackendHealthInfo.HealthStatus.HEALTHY;
            healthErrorMessage = null;
            lastHealthCheckTime = System.currentTimeMillis();

            logger.info("DJL vision backend initialized successfully with {} models loaded", modelCache.size());

        } catch (Exception e) {
            logger.error("Failed to initialize DJL backend", e);
            initialized = false;
            healthStatus = BackendHealthInfo.HealthStatus.UNHEALTHY;
            healthErrorMessage = "Initialization failed: " + e.getMessage();
            throw new VisionBackendException(
                "Failed to initialize DJL backend",
                "initialization_failed",
                null,
                e
            );
        }
    }

    // New helper that centralizes predictor usage and enforces concurrency limits
    private <I, O, R> R withPredictor(ZooModel<I, O> model, PredictorCallback<I, O, R> callback) throws Exception {
        if (model == null) {
            throw new VisionBackendException("Model not initialized", "model_not_initialized", null);
        }
        try {
            inferenceSemaphore.acquire();
            try (Predictor<I, O> predictor = model.newPredictor()) {
                return callback.apply(predictor);
            } finally {
                inferenceSemaphore.release();
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new VisionBackendException("Inference interrupted", "interrupted", null, ie);
        }
    }

    // Overload to support creating a Predictor with a custom Translator
    private <I, O, R> R withPredictor(ZooModel<I, O> model, Translator<I, O> translator, PredictorCallback<I, O, R> callback) throws Exception {
        if (model == null) {
            throw new VisionBackendException("Model not initialized", "model_not_initialized", null);
        }
        try {
            inferenceSemaphore.acquire();
            try (Predictor<I, O> predictor = model.newPredictor(translator)) {
                return callback.apply(predictor);
            } finally {
                inferenceSemaphore.release();
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new VisionBackendException("Inference interrupted", "interrupted", null, ie);
        }
    }

    private void loadFaceRecognitionModel() throws ModelNotFoundException, MalformedModelException, IOException {
        logger.info("Loading face recognition model with DJL - using HuggingFace ArcFace model");

        // Use verified ArcFace model from HuggingFace (garavv/arcface-onnx)
        // This model generates 512-dimensional embeddings for face recognition
        try {
            Criteria<Image, NDArray> criteria = Criteria.builder()
                .setTypes(Image.class, NDArray.class)
                .optApplication(Application.CV.IMAGE_CLASSIFICATION)
                .optModelUrls("djl://ai.djl.huggingface.onnx/garavv/arcface-onnx")
                .optEngine("OnnxRuntime")
                .optDevice(device)
                .optArgument("inputShape", new int[]{1, 3, 112, 112})
                .optArgument("normalize", true)
                .optProgress(properties.isShowProgress() ? new ProgressBar() : null)
                .build();

            @SuppressWarnings("unchecked")
            ZooModel<Image, float[]> model = (ZooModel<Image, float[]>) (Object) criteria.loadModel();
            faceRecognitionModel = model;
            
            modelCache.put("face_recognition", faceRecognitionModel);
            logger.info("Face recognition model loaded: ArcFace (garavv/arcface-onnx) - 512-dim embeddings");
        } catch (Exception e) {
            logger.error("Failed to load ArcFace model from HuggingFace: {}", e.getMessage());
            throw new ModelNotFoundException("ArcFace face recognition model not available", e);
        }
    }

    private void loadFaceDetectionModel() throws ModelNotFoundException, MalformedModelException, IOException {
        logger.info("Loading dedicated face detection model with DJL - using HuggingFace YuNet model");

        // Use verified YuNet face detection model from HuggingFace (opencv/face_detection_yunet)
        // This is optimized for speed with millisecond-level inference
        try {
            Criteria<Image, DetectedObjects> criteria = Criteria.builder()
                .setTypes(Image.class, DetectedObjects.class)
                .optApplication(Application.CV.OBJECT_DETECTION)
                .optModelUrls("djl://ai.djl.huggingface.onnx/opencv/face_detection_yunet")
                .optEngine("OnnxRuntime")
                .optDevice(device)
                .optArgument("threshold", 0.6f)
                .optArgument("nms_threshold", 0.3f)
                .optTranslator(new YuNetFaceDetectionTranslator())
                .optProgress(properties.isShowProgress() ? new ProgressBar() : null)
                .build();

            faceDetectionModel = criteria.loadModel();
            modelCache.put("face_detection", faceDetectionModel);
            logger.info("Face detection model loaded: YuNet (opencv/face_detection_yunet) - millisecond-level inference");
        } catch (Exception e) {
            logger.warn("Failed to load YuNet model from HuggingFace: {}. Attempting fallback to YOLO face detection.", e.getMessage());
            
            // Fallback to YOLOv11 face detection
            try {
                Criteria<Image, DetectedObjects> criteria = Criteria.builder()
                    .setTypes(Image.class, DetectedObjects.class)
                    .optApplication(Application.CV.OBJECT_DETECTION)
                    .optModelUrls("djl://ai.djl.huggingface.pytorch/AdamCodd/YOLOv11n-face-detection")
                    .optEngine("PyTorch")
                    .optDevice(device)
                    .optArgument("threshold", 0.5f)
                    .optArgument("size", 640)
                    .optProgress(properties.isShowProgress() ? new ProgressBar() : null)
                    .build();

                faceDetectionModel = criteria.loadModel();
                modelCache.put("face_detection", faceDetectionModel);
                logger.info("Face detection model loaded: YOLOv11n (AdamCodd/YOLOv11n-face-detection)");
            } catch (Exception fallbackEx) {
                logger.error("Failed to load face detection model from HuggingFace: {}", fallbackEx.getMessage());
                throw new ModelNotFoundException("Face detection model not available", fallbackEx);
            }
        }
    }

    private void loadObjectDetectionModel() throws ModelNotFoundException, MalformedModelException, IOException {
        logger.info("Loading object detection model with DJL");

        // Use a standard SSD model without overly specific filters
        Criteria<Image, DetectedObjects> criteria = Criteria.builder()
            .optApplication(Application.CV.OBJECT_DETECTION)
            .setTypes(Image.class, DetectedObjects.class)
            .optEngine(properties.getEngine())
            .optDevice(device)
            .optProgress(properties.isShowProgress() ? new ProgressBar() : null)
            .build();

        objectDetectionModel = criteria.loadModel();
        modelCache.put("object_detection", objectDetectionModel);
        logger.info("Object detection model loaded: {}", objectDetectionModel.getName());
    }

    private void loadPoseEstimationModel() throws ModelNotFoundException, MalformedModelException, IOException {
        logger.info("Loading pose estimation model with DJL - using HuggingFace MediaPipe Pose model");

        // Use verified MediaPipe Pose model from HuggingFace (opencv/pose_estimation_mediapipe)
        // This model detects 33 body keypoints including face, hands, and torso landmarks
        try {
            Criteria<Image, NDList> criteria = Criteria.builder()
                .setTypes(Image.class, NDList.class)
                .optApplication(Application.CV.POSE_ESTIMATION)
                .optModelUrls("djl://ai.djl.huggingface.onnx/opencv/pose_estimation_mediapipe")
                .optEngine("OnnxRuntime")
                .optDevice(device)
                .optArgument("inputShape", new int[]{1, 3, 256, 256})
                .optArgument("normalize", true)
                .optProgress(properties.isShowProgress() ? new ProgressBar() : null)
                .build();

            @SuppressWarnings("unchecked")
            ZooModel<Image, Joints> model = (ZooModel<Image, Joints>) (Object) criteria.loadModel();
            poseEstimationModel = model;
            
            modelCache.put("pose_estimation", poseEstimationModel);
            logger.info("Pose estimation model loaded: MediaPipe Pose (opencv/pose_estimation_mediapipe) - 33 keypoints");
        } catch (Exception e) {
            logger.error("Failed to load MediaPipe Pose model from HuggingFace: {}", e.getMessage());
            throw new ModelNotFoundException("Pose estimation model not available", e);
        }
    }

    private void loadActionRecognitionModel() throws ModelNotFoundException, MalformedModelException, IOException {
        logger.info("Loading action recognition model with DJL");

        Criteria<Image, float[]> criteria = Criteria.builder()
            .optApplication(Application.CV.ACTION_RECOGNITION)
            .setTypes(Image.class, float[].class)
            .optEngine(properties.getEngine())
            .optDevice(device)
            .optProgress(properties.isShowProgress() ? new ProgressBar() : null)
            .build();

        actionRecognitionModel = criteria.loadModel();
        modelCache.put("action_recognition", actionRecognitionModel);
        logger.info("Action recognition model loaded: {}", actionRecognitionModel.getName());
    }

    private void loadSegmentationModels() throws ModelNotFoundException, MalformedModelException, IOException {
        logger.info("Loading segmentation models with DJL");

        // Semantic segmentation
        Criteria<Image, CategoryMask> semanticCriteria = Criteria.builder()
            .optApplication(Application.CV.SEMANTIC_SEGMENTATION)
            .setTypes(Image.class, CategoryMask.class)
            .optEngine(properties.getEngine())
            .optDevice(device)
            .optProgress(properties.isShowProgress() ? new ProgressBar() : null)
            .build();

        semanticSegmentationModel = semanticCriteria.loadModel();
        modelCache.put("semantic_segmentation", semanticSegmentationModel);

        // Instance segmentation
        Criteria<Image, DetectedObjects> instanceCriteria = Criteria.builder()
            .optApplication(Application.CV.INSTANCE_SEGMENTATION)
            .setTypes(Image.class, DetectedObjects.class)
            .optEngine(properties.getEngine())
            .optDevice(device)
            .optProgress(properties.isShowProgress() ? new ProgressBar() : null)
            .build();

        instanceSegmentationModel = instanceCriteria.loadModel();
        modelCache.put("instance_segmentation", instanceSegmentationModel);

        logger.info("Segmentation models loaded: semantic={}, instance={}",
            semanticSegmentationModel.getName(), instanceSegmentationModel.getName());
    }

    // ==========================
    // Face Detection Implementation
    // ==========================

    @Override
    public List<Detection> detectFaces(ImageData imageData) throws BaseVisionException {
        // Use configured thresholds from properties by default
        DetectionQuery query = new DetectionQuery.Builder()
            .type(DetectionType.FACE)
            .minConfidence(properties.getFaceDetection().getConfidenceThreshold())
            .maxDetections(properties.getFaceDetection().getMaxFaces())
            .build();
        return detectFaces(imageData, query);
    }

    /**
     * Detects faces with query parameters for filtering.
     */
    public List<Detection> detectFaces(ImageData imageData, DetectionQuery query) throws BaseVisionException {
        if (!initialized) {
            throw new VisionBackendException(
                "Backend not initialized",
                "not_initialized",
                null
            );
        }

        validateInput(imageData, query);

        String correlationId = generateCorrelationId();

        try {
            logger.debug("DJL face detection requested: correlationId={}, backend=djl", correlationId);

            // Convert ImageData to DJL Image
            Image djlImage = ImageFactory.getInstance()
                .fromInputStream(new ByteArrayInputStream(imageData.data()));

            // Prefer specialized face detection model when available, fall back to object detection
            ZooModel<Image, DetectedObjects> modelToUse = (faceDetectionModel != null) ? faceDetectionModel : objectDetectionModel;

            List<Detection> faceDetections = withPredictor(modelToUse, predictor -> {
                DetectedObjects detections = predictor.predict(djlImage);

                // If using object detector as fallback, filter 'person' or 'face' class labels; otherwise use all classes as faces
                List<Detection> allDetections = convertDetections(detections, query);
                if (modelToUse == objectDetectionModel) {
                    return allDetections.stream()
                        .filter(d -> {
                            String lbl = d.label() == null ? "" : d.label().toLowerCase();
                            return lbl.contains("person") || lbl.contains("face") || lbl.contains("head") || lbl.contains("human");
                        })
                        .toList();
                } else {
                    // face detector returns face classes - return as-is
                    return allDetections;
                }
            });

            logger.info("DJL face detection completed: {} faces detected, correlationId={}, backend=djl",
                faceDetections.size(), correlationId);

            return faceDetections;

        } catch (IOException e) {
            throw new VisionProcessingException(
                "Failed to load image",
                "image_load_failed",
                DetectionType.FACE.name(),
                e
            );
        } catch (TranslateException e) {
            throw new VisionProcessingException(
                "Failed to process image",
                "inference_failed",
                DetectionType.FACE.name(),
                e
            );
        } catch (VisionBackendException e) {
            throw e;
        } catch (Exception e) {
            throw new VisionBackendException(
                "Face detection failed",
                "detection_failed",
                null,
                e
            );
        }
    }

    // ==========================
    // Object Detection Implementation
    // ==========================

    @Override
    public List<Detection> detectObjects(ImageData imageData) {
        DetectionQuery query = new DetectionQuery.Builder()
            .type(DetectionType.OBJECT)
            .minConfidence(properties.getObjectDetection().getConfidenceThreshold())
            .build();
        try {
            return detectObjects(imageData, query);
        } catch (BaseVisionException e) {
            throw new io.github.codesapienbe.springvision.core.exception.VisionBackendException(
                "Object detection failed",
                "detect_objects",
                null,
                e
            );
        }
    }

    /**
     * Detects objects with query parameters for filtering.
     */
    public List<Detection> detectObjects(ImageData imageData, DetectionQuery query) throws BaseVisionException {
        if (!initialized) {
            throw new VisionBackendException(
                "Backend not initialized",
                "not_initialized",
                null
            );
        }

        validateInput(imageData, query);

        String correlationId = generateCorrelationId();
        try {
            logger.debug("DJL object detection requested: correlationId={}, backend=djl", correlationId);

            Image djlImage = ImageFactory.getInstance()
                .fromInputStream(new ByteArrayInputStream(imageData.data()));

            List<Detection> results = withPredictor(objectDetectionModel, predictor -> {
                DetectedObjects detections = predictor.predict(djlImage);
                return convertDetections(detections, query);
            });

            logger.info("DJL object detection completed: {} objects detected, correlationId={}, backend=djl",
                results.size(), correlationId);

            return results;
        } catch (IOException e) {
            throw new VisionProcessingException(
                "Failed to load image",
                "image_load_failed",
                DetectionType.OBJECT.name(),
                e
            );
        } catch (TranslateException e) {
            throw new VisionProcessingException(
                "Failed to process image",
                "inference_failed",
                DetectionType.OBJECT.name(),
                e
            );
        } catch (Exception e) {
            throw new VisionBackendException(
                "Object detection failed",
                "detection_failed",
                null,
                e
            );
        }
    }

    // ==========================
    // Pose Estimation Implementation
    // ==========================

    @Override
    public List<Detection> detectPoses(ImageData imageData) throws BaseVisionException {
        if (!initialized || poseEstimationModel == null) {
            throw new VisionBackendException(
                "Pose estimation model not initialized",
                "model_not_initialized",
                null
            );
        }

        String correlationId = generateCorrelationId();

        try {
            logger.debug("DJL pose estimation requested: correlationId={}", correlationId);

            Image djlImage = ImageFactory.getInstance()
                .fromInputStream(new ByteArrayInputStream(imageData.data()));

            List<Detection> results = withPredictor(poseEstimationModel, predictor -> {
                Joints joints = predictor.predict(djlImage);
                return convertJointsToDetections(joints);
            });

            logger.info("DJL pose estimation completed: {} poses detected, correlationId={}",
                results.size(), correlationId);

            return results;

        } catch (IOException e) {
            throw new VisionProcessingException(
                "Failed to load image for pose estimation",
                "image_load_failed",
                "POSE",
                e
            );
        } catch (TranslateException e) {
            throw new VisionProcessingException(
                "Failed to process pose estimation",
                "inference_failed",
                "POSE",
                e
            );
        } catch (Exception e) {
            throw new VisionBackendException(
                "Pose estimation failed",
                "pose_failed",
                null,
                e
            );
        }
    }

    private List<Detection> convertJointsToDetections(Joints joints) {
        List<Detection> results = new ArrayList<>();
        float threshold = properties.getPoseEstimation().getConfidenceThreshold();

        Map<String, Object> poseAttributes = new HashMap<>();
        poseAttributes.put("backend", BACKEND_ID);
        poseAttributes.put("model", poseEstimationModel.getName());

        // DJL API compatibility - use getJoints() instead of getNumberOfJoints()
        List<Joints.Joint> jointsList = joints.getJoints();
        poseAttributes.put("totalJoints", jointsList.size());

        List<Map<String, Object>> jointsData = new ArrayList<>();

        for (int i = 0; i < jointsList.size(); i++) {
            Joints.Joint joint = jointsList.get(i);

            if (joint.getConfidence() >= threshold) {
                Map<String, Object> jointData = new HashMap<>();
                jointData.put("index", i);
                jointData.put("type", getJointTypeName(i));
                jointData.put("x", joint.getX());
                jointData.put("y", joint.getY());
                jointData.put("confidence", joint.getConfidence());
                jointsData.add(jointData);
            }
        }

        poseAttributes.put("joints", jointsData);

        // Create a single detection representing the entire pose
        Detection poseDetection = new Detection(
            "person_pose",
            calculateAverageConfidence(jointsData),
            null, // No bounding box for individual joints
            poseAttributes
        );

        results.add(poseDetection);
        return results;
    }

    private String getJointTypeName(int index) {
        String[] jointNames = {
            "nose", "left_eye", "right_eye", "left_ear", "right_ear",
            "left_shoulder", "right_shoulder", "left_elbow", "right_elbow",
            "left_wrist", "right_wrist", "left_hip", "right_hip",
            "left_knee", "right_knee", "left_ankle", "right_ankle"
        };
        return index < jointNames.length ? jointNames[index] : "joint_" + index;
    }

    private double calculateAverageConfidence(List<Map<String, Object>> joints) {
        if (joints.isEmpty()) return 0.0;
        double sum = joints.stream()
            .mapToDouble(j -> ((Number) j.get("confidence")).doubleValue())
            .sum();
        return sum / joints.size();
    }

    // ==========================
    // Action Recognition Implementation
    // ==========================

    @Override
    public List<Detection> recognizeActions(ImageData imageData) throws BaseVisionException {
        if (!initialized || actionRecognitionModel == null) {
            throw new VisionBackendException(
                "Action recognition model not initialized",
                "model_not_initialized",
                null
            );
        }

        String correlationId = generateCorrelationId();

        try {
            logger.debug("DJL action recognition requested: correlationId={}", correlationId);

            Image djlImage = ImageFactory.getInstance()
                .fromInputStream(new ByteArrayInputStream(imageData.data()));

            float[] predictions = withPredictor(actionRecognitionModel, predictor -> predictor.predict(djlImage));

            List<Detection> results = convertActionPredictions(predictions);

            logger.info("DJL action recognition completed: {} actions detected, correlationId={}",
                results.size(), correlationId);

            return results;

        } catch (IOException e) {
            throw new VisionProcessingException(
                "Failed to load image for action recognition",
                "image_load_failed",
                "ACTION",
                e
            );
        } catch (TranslateException e) {
            throw new VisionProcessingException(
                "Failed to process action recognition",
                "inference_failed",
                "ACTION",
                e
            );
        } catch (Exception e) {
            throw new VisionBackendException(
                "Action recognition failed",
                "action_failed",
                null,
                e
            );
        }
    }

    private List<Detection> convertActionPredictions(float[] predictions) {
        List<Detection> results = new ArrayList<>();
        float threshold = properties.getActionRecognition().getConfidenceThreshold();

        // Common action labels (can be customized based on model)
        String[] actionLabels = {
            "walking", "running", "sitting", "standing", "jumping",
            "waving", "clapping", "reading", "writing", "eating"
        };

        for (int i = 0; i < Math.min(predictions.length, actionLabels.length); i++) {
            if (predictions[i] >= threshold) {
                Map<String, Object> attributes = new HashMap<>();
                attributes.put("backend", BACKEND_ID);
                attributes.put("model", actionRecognitionModel.getName());
                attributes.put("actionIndex", i);

                Detection detection = new Detection(
                    actionLabels[i],
                    predictions[i],
                    null,
                    attributes
                );
                results.add(detection);
            }
        }

        // Sort by confidence (highest first)
        results.sort((a, b) -> Double.compare(b.confidence(), a.confidence()));

        return results;
    }

    // ==========================
    // Segmentation Implementation
    // ==========================

    @Override
    public VisionResult segmentSemantic(ImageData imageData) throws BaseVisionException {
        if (!initialized || semanticSegmentationModel == null) {
            throw new VisionBackendException(
                "Semantic segmentation model not initialized",
                "model_not_initialized",
                null
            );
        }

        String correlationId = generateCorrelationId();

        try {
            logger.debug("DJL semantic segmentation requested: correlationId={}", correlationId);

            Image djlImage = ImageFactory.getInstance()
                .fromInputStream(new ByteArrayInputStream(imageData.data()));

            CategoryMask mask = withPredictor(semanticSegmentationModel, predictor -> predictor.predict(djlImage));

            VisionResult result = convertSemanticMaskToResult(mask, correlationId);

            logger.info("DJL semantic segmentation completed: correlationId={}", correlationId);

            return result;

        } catch (IOException e) {
            throw new VisionProcessingException(
                "Failed to load image for semantic segmentation",
                "image_load_failed",
                "SEGMENTATION",
                e
            );
        } catch (TranslateException e) {
            throw new VisionProcessingException(
                "Failed to process semantic segmentation",
                "inference_failed",
                "SEGMENTATION",
                e
            );
        } catch (Exception e) {
            throw new VisionBackendException(
                "Semantic segmentation failed",
                "segmentation_failed",
                null,
                e
            );
        }
    }

    @Override
    public VisionResult segmentInstances(ImageData imageData) throws BaseVisionException {
        if (!initialized || instanceSegmentationModel == null) {
            throw new VisionBackendException(
                "Instance segmentation model not initialized",
                "model_not_initialized",
                null
            );
        }

        String correlationId = generateCorrelationId();

        try {
            logger.debug("DJL instance segmentation requested: correlationId={}", correlationId);

            Image djlImage = ImageFactory.getInstance()
                .fromInputStream(new ByteArrayInputStream(imageData.data()));

            DetectedObjects detections = withPredictor(instanceSegmentationModel, predictor -> predictor.predict(djlImage));

            VisionResult result = convertInstanceDetectionsToResult(detections, correlationId);

            logger.info("DJL instance segmentation completed: {} instances, correlationId={}",
                detections.getNumberOfObjects(), correlationId);

            return result;

        } catch (IOException e) {
            throw new VisionProcessingException(
                "Failed to load image for instance segmentation",
                "image_load_failed",
                "SEGMENTATION",
                e
            );
        } catch (TranslateException e) {
            throw new VisionProcessingException(
                "Failed to process instance segmentation",
                "inference_failed",
                "SEGMENTATION",
                e
            );
        } catch (Exception e) {
            throw new VisionBackendException(
                "Instance segmentation failed",
                "segmentation_failed",
                null,
                e
            );
        }
    }

    private VisionResult convertSemanticMaskToResult(CategoryMask mask, String correlationId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("segmentationType", "semantic");
        metadata.put("backend", BACKEND_ID);
        metadata.put("model", semanticSegmentationModel.getName());
        metadata.put("classes", mask.getClasses());
        metadata.put("maskData", mask.getMask());
        metadata.put("correlationId", correlationId);

        return VisionResult.builder()
            .detectionType(DetectionType.SCENE)
            .detections(List.of())
            .processingTimeMs(0L)
            .metadata(metadata)
            .build();
    }

    private VisionResult convertInstanceDetectionsToResult(DetectedObjects detections, String correlationId) {
        List<Detection> results = new ArrayList<>();

        for (int i = 0; i < detections.getNumberOfObjects(); i++) {
            DetectedObjects.DetectedObject obj = detections.item(i);

            ai.djl.modality.cv.output.BoundingBox bbox = obj.getBoundingBox();
            ai.djl.modality.cv.output.Rectangle rect = bbox.getBounds();

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("backend", BACKEND_ID);
            attributes.put("model", instanceSegmentationModel.getName());
            attributes.put("instanceId", i);

            io.github.codesapienbe.springvision.core.BoundingBox svBbox =
                new io.github.codesapienbe.springvision.core.BoundingBox(
                    rect.getX(),
                    rect.getY(),
                    rect.getWidth(),
                    rect.getHeight()
                );

            Detection detection = new Detection(
                obj.getClassName(),
                obj.getProbability(),
                svBbox,
                attributes
            );

            results.add(detection);
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("correlationId", correlationId);
        metadata.put("segmentationType", "instance");

        return VisionResult.builder()
            .detectionType(DetectionType.SCENE)
            .detections(results)
            .processingTimeMs(0L)
            .metadata(metadata)
            .build();
    }

    // ==========================
    // Embedding Extraction Implementation
    // ==========================

    @Override
    public List<float[]> extractEmbeddings(ImageData imageData, DetectionCategory subject) throws BaseVisionException {
        if (subject == DetectionCategory.FACE) {
            return extractFaceEmbeddings(imageData);
        }

        throw new VisionBackendException(
            "Embedding extraction not supported for category: " + subject,
            "unsupported_category",
            null
        );
    }

    private List<float[]> extractFaceEmbeddings(ImageData imageData) throws BaseVisionException {
        if (!initialized) {
            throw new VisionBackendException(
                "Backend not initialized",
                "not_initialized",
                null
            );
        }

        // Ensure face recognition model is loaded
        if (faceRecognitionModel == null) {
            try {
                loadFaceRecognitionModel();
            } catch (Exception e) {
                throw new VisionBackendException(
                    "Face recognition model not available",
                    "model_not_initialized",
                    null,
                    e
                );
            }
        }

        String correlationId = generateCorrelationId();

        try {
            logger.debug("DJL face embedding extraction requested: correlationId={}", correlationId);

            Image djlImage = ImageFactory.getInstance()
                .fromInputStream(new ByteArrayInputStream(imageData.data()));

            // First, detect faces to get bounding boxes. Use detectFaces which will prefer a face detector.
            List<Detection> faceDetections = detectFaces(imageData);

            List<float[]> embeddings = new ArrayList<>();

            if (faceDetections.isEmpty()) {
                // No faces detected -- as last resort run recognition on full image
                float[] fullEmbedding = withPredictor(faceRecognitionModel, predictor -> predictor.predict(djlImage));
                embeddings.add(l2Normalize(fullEmbedding));
                logger.info("No face boxes found - extracted embedding from full image, correlationId={}", correlationId);
                return embeddings;
            }

            // Convert DJL Image to BufferedImage for cropping. DJL's getWrappedImage() may return Object
            // in some engine versions, so safely cast and fall back to reading the original bytes if needed.
            java.awt.image.BufferedImage wrapped;
            Object wrappedObj = djlImage.getWrappedImage();
            if (wrappedObj instanceof BufferedImage) {
                wrapped = (BufferedImage) wrappedObj;
            } else {
                // Fallback: decode from original image bytes
                wrapped = ImageIO.read(new ByteArrayInputStream(imageData.data()));
                if (wrapped == null) {
                    throw new IOException("Unable to obtain BufferedImage for embedding extraction");
                }
            }
            int imgW = wrapped.getWidth();
            int imgH = wrapped.getHeight();

            for (int i = 0; i < faceDetections.size(); i++) {
                Detection det = faceDetections.get(i);
                io.github.codesapienbe.springvision.core.BoundingBox bbox = det.boundingBox();
                if (bbox == null) continue;

                // bbox values are normalized 0..1; compute pixel coordinates with small padding
                int x = Math.max(0, (int) Math.floor(bbox.x() * imgW));
                int y = Math.max(0, (int) Math.floor(bbox.y() * imgH));
                int w = Math.max(1, (int) Math.ceil(bbox.width() * imgW));
                int h = Math.max(1, (int) Math.ceil(bbox.height() * imgH));

                // add padding ~10% around face box, up to image bounds
                int padX = (int) (w * 0.1);
                int padY = (int) (h * 0.1);
                int sx = Math.max(0, x - padX);
                int sy = Math.max(0, y - padY);
                int sw = Math.min(imgW - sx, w + padX * 2);
                int sh = Math.min(imgH - sy, h + padY * 2);

                try {
                    BufferedImage faceCrop = wrapped.getSubimage(sx, sy, sw, sh);

                    // Model-aware preprocessing: resize and apply normalization via Translator
                    String modelName = (properties.getFaceRecognition() != null ? properties.getFaceRecognition().getModel() : "");
                    int targetSize = determineFaceRecognitionInputSize(modelName);

                    BufferedImage resized = resizeImage(faceCrop, targetSize, targetSize);
                    Image faceImage = ImageFactory.getInstance().fromImage(resized);

                    Translator<Image, float[]> translator = createFaceEmbeddingTranslator(modelName);

                    try {
                        // Use centralized helper that manages concurrency and predictor lifecycle
                        float[] emb = withPredictor(faceRecognitionModel, translator, predictor -> predictor.predict(faceImage));
                        if (emb != null) {
                            embeddings.add(l2Normalize(emb));
                        }
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new VisionBackendException("Inference interrupted", "interrupted", null, ie);
                    } catch (Exception ex) {
                        // Log and continue with next face crop
                        logger.warn("Failed to compute embedding for face {}: {}", i, ex.getMessage());
                    }
                } catch (RasterFormatException rfe) {
                    logger.warn("Invalid crop for face {} - skipping: {}", i, rfe.getMessage());
                }
            }

            logger.info("DJL face embeddings extracted: faces={}, correlationId={}", embeddings.size(), correlationId);
            return embeddings;

        } catch (IOException e) {
            throw new VisionProcessingException(
                "Failed to load image for embedding extraction",
                "image_load_failed",
                "EMBEDDING",
                e
            );
        } catch (TranslateException e) {
            throw new VisionProcessingException(
                "Failed to extract embeddings",
                "inference_failed",
                "EMBEDDING",
                e
            );
        } catch (Exception e) {
            throw new VisionBackendException(
                "Embedding extraction failed",
                "extraction_failed",
                null,
                e
            );
        }
    }

    // Determine a reasonable input size for the face recognition model based on model name
    private int determineFaceRecognitionInputSize(String modelName) {
        if (modelName == null) return 160;
        String m = modelName.toLowerCase();
        if (m.contains("inception") || m.contains("facenet") || m.contains("resnet")) {
            return 160; // common for FaceNet/ResNet-based face embeddings
        }
        if (m.contains("arcface") || m.contains("sphereface")) {
            return 112; // some ArcFace models use 112x112
        }
        // default fallback
        return 160;
    }

    // Create a translator that normalizes the image to the model's expected range.
    private Translator<Image, float[]> createFaceEmbeddingTranslator(String modelName) {
        // Choose normalization parameters based on known models
        float[] mean;
        float[] std;
        String m = (modelName == null ? "" : modelName.toLowerCase());
        if (m.contains("facenet") || m.contains("inception")) {
            // FaceNet often expects inputs in [-1, 1]
            mean = new float[]{0.5f, 0.5f, 0.5f};
            std = new float[]{0.5f, 0.5f, 0.5f};
        } else {
            // Default to ImageNet normalization
            mean = new float[]{0.485f, 0.456f, 0.406f};
            std = new float[]{0.229f, 0.224f, 0.225f};
        }

        final float[] meanF = mean;
        final float[] stdF = std;

        return new Translator<>() {
            @Override
            public NDList processInput(TranslatorContext ctx, Image input) {
                NDManager manager = ctx.getNDManager();
                // Convert to NDArray (HWC)
                NDArray array = input.toNDArray(manager);
                // Convert to float and scale to [0,1]
                array = array.toType(DataType.FLOAT32, false).div(255f);
                // HWC -> CHW
                array = array.transpose(2, 0, 1);
                // normalize per-channel
                NDArray mean = manager.create(meanF).reshape(3, 1, 1);
                NDArray std = manager.create(stdF).reshape(3, 1, 1);
                array = array.sub(mean).div(std);
                // add batch dim
                array = array.expandDims(0);
                return new NDList(array);
            }

            @Override
            public float[] processOutput(TranslatorContext ctx, NDList list) {
                NDArray arr = list.getFirst();
                // Flatten and return as float[]
                NDArray flat = arr.flatten();
                return flat.toFloatArray();
            }

            @Override
            public Batchifier getBatchifier() {
                return null; // single prediction
            }
        };
    }

    private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resizedImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        g.dispose();
        return resizedImage;
    }

    // ==========================
    // OCR Implementation
    // ==========================

    @Override
    public List<OcrCapability.TextDetection> extractText(ImageData imageData) throws BaseVisionException {
        if (!initialized) {
            throw new VisionBackendException(
                "Backend not initialized",
                "not_initialized",
                null
            );
        }

        String correlationId = generateCorrelationId();

        try {
            logger.debug("DJL OCR requested: correlationId={}", correlationId);

            Image djlImage = ImageFactory.getInstance()
                .fromInputStream(new ByteArrayInputStream(imageData.data()));

            // Load OCR model on demand
            Criteria<Image, String> criteria = Criteria.builder()
                .optApplication(Application.CV.WORD_RECOGNITION)
                .setTypes(Image.class, String.class)
                .optEngine(properties.getEngine())
                .optDevice(device)
                .optProgress(properties.isShowProgress() ? new ProgressBar() : null)
                .build();

            // Use try-with-resources for the model so it is closed when done
            try (ZooModel<Image, String> ocrModel = criteria.loadModel()) {
                String extractedText = withPredictor(ocrModel, predictor -> predictor.predict(djlImage));

                Map<String, Object> attributes = new HashMap<>();
                attributes.put("backend", BACKEND_ID);
                attributes.put("model", ocrModel.getName());
                attributes.put("correlationId", correlationId);

                Map<String, Object> bbox = new HashMap<>();
                bbox.put("x", 0);
                bbox.put("y", 0);
                bbox.put("width", djlImage.getWidth());
                bbox.put("height", djlImage.getHeight());

                OcrCapability.TextDetection textDetection = new OcrCapability.TextDetection(
                    extractedText,
                    1.0, // High confidence for full image OCR
                    bbox,
                    attributes
                );

                logger.info("DJL OCR completed: {} characters extracted, correlationId={}",
                    extractedText.length(), correlationId);

                return List.of(textDetection);
            }

        } catch (IOException | TranslateException e) {
            // Try Tess4J as a fallback when DJL OCR failed
            logger.warn("DJL OCR failed, attempting Tess4J fallback: {}", e.getMessage());
            try {
                Class<?> tesseractClass = Class.forName("net.sourceforge.tess4j.Tesseract");
                Object tess = tesseractClass.getConstructor().newInstance();
                BufferedImage buf = ImageIO.read(new ByteArrayInputStream(imageData.data()));
                Method doOCR = tesseractClass.getMethod("doOCR", BufferedImage.class);
                String tessText = (String) doOCR.invoke(tess, buf);

                Map<String, Object> attributes = new HashMap<>();
                attributes.put("backend", BACKEND_ID);
                attributes.put("model", "tess4j");
                attributes.put("correlationId", correlationId);

                Map<String, Object> bbox = new HashMap<>();
                bbox.put("x", 0);
                bbox.put("y", 0);
                bbox.put("width", buf.getWidth());
                bbox.put("height", buf.getHeight());

                OcrCapability.TextDetection textDetection = new OcrCapability.TextDetection(
                    tessText,
                    1.0,
                    bbox,
                    attributes
                );

                logger.info("Tess4J OCR completed: {} characters extracted, correlationId={}",
                    tessText.length(), correlationId);

                return List.of(textDetection);
            } catch (ClassNotFoundException cnf) {
                throw new VisionProcessingException(
                    "OCR model not available and Tess4J not on classpath",
                    "ocr_unavailable",
                    "OCR",
                    e
                );
            } catch (Exception ex) {
                throw new VisionProcessingException(
                    "Fallback OCR failed",
                    "ocr_failed",
                    "OCR",
                    ex
                );
            }
        } catch (Exception e) {
            throw new VisionBackendException(
                "OCR failed",
                "ocr_failed",
                null,
                e
            );
        }
    }

    // ==========================
    // Image Classification Implementation
    // ==========================

    @Override
    public ImageClassificationCapability.ClassificationResult classifyImage(ImageData imageData, int topK) throws BaseVisionException {
        if (!initialized) {
            throw new VisionBackendException(
                "Backend not initialized",
                "not_initialized",
                null
            );
        }

        String correlationId = generateCorrelationId();

        try {
            logger.debug("DJL image classification requested: topK={}, correlationId={}", topK, correlationId);

            Image djlImage = ImageFactory.getInstance()
                .fromInputStream(new ByteArrayInputStream(imageData.data()));

            // Load image classification model on demand
            // Default to ResNet for general image classification
            Criteria<Image, ai.djl.modality.Classifications> criteria = Criteria.builder()
                .optApplication(Application.CV.IMAGE_CLASSIFICATION)
                .setTypes(Image.class, ai.djl.modality.Classifications.class)
                .optEngine(properties.getEngine())
                .optDevice(device)
                .optProgress(properties.isShowProgress() ? new ProgressBar() : null)
                .build();

            try (ZooModel<Image, ai.djl.modality.Classifications> classificationModel = criteria.loadModel()) {
                ai.djl.modality.Classifications classifications = withPredictor(classificationModel, predictor -> predictor.predict(djlImage));

                List<ImageClassificationCapability.Classification> results = new ArrayList<>();
                List<ai.djl.modality.Classifications.Classification> topKItems = classifications.topK(topK);

                for (ai.djl.modality.Classifications.Classification item : topKItems) {
                    Map<String, Object> attributes = new HashMap<>();
                    attributes.put("backend", BACKEND_ID);
                    attributes.put("model", classificationModel.getName());

                    ImageClassificationCapability.Classification classification =
                        new ImageClassificationCapability.Classification(
                            item.getClassName(),
                            item.getProbability(),
                            attributes
                        );
                    results.add(classification);
                }

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("correlationId", correlationId);
                metadata.put("topK", topK);
                metadata.put("totalClasses", classifications.getClassNames().size());

                logger.info("DJL image classification completed: {} classifications, correlationId={}",
                    results.size(), correlationId);

                return new ImageClassificationCapability.ClassificationResult(results, metadata);
            }

        } catch (IOException e) {
            throw new VisionProcessingException(
                "Failed to load image for classification",
                "image_load_failed",
                "CLASSIFICATION",
                e
            );
        } catch (TranslateException e) {
            throw new VisionProcessingException(
                "Failed to classify image",
                "inference_failed",
                "CLASSIFICATION",
                e
            );
        } catch (Exception e) {
            throw new VisionBackendException(
                "Image classification failed",
                "classification_failed",
                null,
                e
            );
        }
    }

    // ==========================
    // Utility Methods
    // ==========================

    private List<Detection> convertDetections(DetectedObjects detections, DetectionQuery query) {
        List<Detection> results = new ArrayList<>();

        // Iterate over detected objects
        List<ai.djl.modality.cv.output.DetectedObjects.DetectedObject> items = detections.items();
        for (ai.djl.modality.cv.output.DetectedObjects.DetectedObject obj : items) {
            // Apply confidence threshold filtering
            if (obj.getProbability() < query.getMinConfidence()) {
                continue;
            }

            ai.djl.modality.cv.output.BoundingBox bbox = obj.getBoundingBox();
            ai.djl.modality.cv.output.Rectangle rect = bbox.getBounds();

            // Convert to Spring Vision BoundingBox using normalized coordinates (0..1)
            io.github.codesapienbe.springvision.core.BoundingBox svBbox =
                new io.github.codesapienbe.springvision.core.BoundingBox(
                    rect.getX(),
                    rect.getY(),
                    rect.getWidth(),
                    rect.getHeight()
                );

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("backend", BACKEND_ID);
            String modelName = switch (query.getType()) {
                case OBJECT -> (objectDetectionModel != null ? objectDetectionModel.getName() : "unknown");
                case FACE -> (faceDetectionModel != null ? faceDetectionModel.getName() : "unknown");
                default -> "unknown";
            };
            attributes.put("model", modelName);

            Detection detection = new Detection(
                obj.getClassName(),
                obj.getProbability(),
                svBbox,
                attributes
            );

            results.add(detection);

            // Apply max detections limit
            if (query.getMaxDetections() > 0 && results.size() >= query.getMaxDetections()) {
                break;
            }
        }

        logger.debug("Converted {} detections with filtering (minConfidence={}, maxDetections={})",
            results.size(), query.getMinConfidence(), query.getMaxDetections());
        return results;
    }

    /**
     * Validates input parameters.
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
     * Generates a unique correlation ID for tracking requests.
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    // ==================== BarcodeCapability Implementation ====================

    /**
     * Detects and decodes barcodes in an image using ZXing library.
     *
     * @param imageData The image data to scan for barcodes
     * @return List of detected barcodes with type, content, and location
     */
    @Override
    public List<Detection> detectBarcodes(ImageData imageData) {
        Objects.requireNonNull(imageData, "ImageData cannot be null");
        logger.debug("Starting barcode detection");

        try {
            // Convert ImageData to BufferedImage
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageData.data()));
            if (bufferedImage == null) {
                throw new VisionProcessingException("Failed to decode image data");
            }

            // Create ZXing reader for multiple barcode detection
            LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            
            // Configure reader with multiple formats
            Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            hints.put(DecodeHintType.POSSIBLE_FORMATS, Arrays.asList(
                BarcodeFormat.QR_CODE,
                BarcodeFormat.DATA_MATRIX,
                BarcodeFormat.EAN_13,
                BarcodeFormat.EAN_8,
                BarcodeFormat.CODE_128,
                BarcodeFormat.CODE_39,
                BarcodeFormat.UPC_A,
                BarcodeFormat.UPC_E,
                BarcodeFormat.AZTEC,
                BarcodeFormat.PDF_417
            ));

            List<Detection> detections = new ArrayList<>();

            try {
                // Try to detect multiple barcodes
                GenericMultipleBarcodeReader multiReader = new GenericMultipleBarcodeReader(new MultiFormatReader());
                Result[] results = multiReader.decodeMultiple(bitmap, hints);
                
                for (Result result : results) {
                    detections.add(createBarcodeDetection(result, bufferedImage.getWidth(), bufferedImage.getHeight()));
                }
                
                logger.info("Detected {} barcode(s)", detections.size());
            } catch (NotFoundException e) {
                // No barcodes found, try single barcode detection
                try {
                    Reader singleReader = new MultiFormatReader();
                    Result singleResult = singleReader.decode(bitmap, hints);
                    detections.add(createBarcodeDetection(singleResult, bufferedImage.getWidth(), bufferedImage.getHeight()));
                    logger.info("Detected 1 barcode");
                } catch (NotFoundException e2) {
                    logger.debug("No barcodes detected in image");
                }
            }

            return detections;

        } catch (IOException e) {
            throw new VisionProcessingException("Failed to process image for barcode detection", e);
        } catch (Exception e) {
            throw new VisionProcessingException("Barcode detection failed", e);
        }
    }

    /**
     * Creates a Detection object from a ZXing Result.
     */
    private Detection createBarcodeDetection(Result result, int imageWidth, int imageHeight) {
        // Extract barcode information
        String format = result.getBarcodeFormat().toString();
        String content = result.getText();
        
        // Calculate bounding box from result points
        BoundingBox boundingBox = calculateBarcodeBoundingBox(result, imageWidth, imageHeight);
        
        // Create attributes
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("backend", BACKEND_ID);
        attributes.put("format", format);
        attributes.put("content", content);
        attributes.put("rawBytes", result.getRawBytes() != null ? result.getRawBytes().length : 0);
        
        // Add metadata if available
        if (result.getResultMetadata() != null) {
            result.getResultMetadata().forEach((key, value) -> 
                attributes.put("metadata_" + key.toString(), value.toString())
            );
        }
        
        // Return detection with format as label and confidence 1.0 (barcodes are deterministic)
        return new Detection(format, 1.0f, boundingBox, attributes);
    }

    /**
     * Calculates normalized bounding box from ZXing result points.
     */
    private BoundingBox calculateBarcodeBoundingBox(Result result, int imageWidth, int imageHeight) {
        ResultPoint[] points = result.getResultPoints();
        
        if (points == null || points.length == 0) {
            // Return default bounding box if no points available
            return new BoundingBox(0.0, 0.0, 1.0, 1.0);
        }
        
        // Find min/max coordinates
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;
        
        for (ResultPoint point : points) {
            if (point != null) {
                minX = Math.min(minX, point.getX());
                minY = Math.min(minY, point.getY());
                maxX = Math.max(maxX, point.getX());
                maxY = Math.max(maxY, point.getY());
            }
        }
        
        // Normalize to 0-1 range
        double x = minX / imageWidth;
        double y = minY / imageHeight;
        double width = (maxX - minX) / imageWidth;
        double height = (maxY - minY) / imageHeight;
        
        return new BoundingBox(x, y, width, height);
    }

    // ==================== MetaDataExtractionCapability Implementation ====================

    /**
     * Extracts EXIF, IPTC, and XMP metadata from an image.
     *
     * @param imageData The image data to extract metadata from
     * @return List of detections containing metadata information
     */
    @Override
    public List<Detection> extractMetaData(ImageData imageData) {
        Objects.requireNonNull(imageData, "ImageData cannot be null");
        logger.debug("Starting metadata extraction");

        List<Detection> detections = new ArrayList<>();

        try {
            // Read metadata from image bytes
            ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData.data());
            Metadata metadata = ImageMetadataReader.readMetadata(inputStream);

            // Group metadata by directory type
            Map<String, Map<String, Object>> groupedMetadata = new LinkedHashMap<>();
            
            for (Directory directory : metadata.getDirectories()) {
                String directoryName = directory.getName();
                Map<String, Object> directoryData = groupedMetadata.computeIfAbsent(
                    directoryName, k -> new LinkedHashMap<>()
                );

                // Add all tags from this directory
                for (Tag tag : directory.getTags()) {
                    String tagName = tag.getTagName();
                    String tagValue = tag.getDescription();
                    if (tagValue != null && !tagValue.isEmpty()) {
                        directoryData.put(tagName, tagValue);
                    }
                }

                // Handle errors
                if (directory.hasErrors()) {
                    List<String> errors = new ArrayList<>();
                    for (String error : directory.getErrors()) {
                        errors.add(error);
                    }
                    directoryData.put("_errors", errors);
                }
            }

            // Create GPS detection if GPS data exists
            GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            if (gpsDirectory != null) {
                Detection gpsDetection = createGpsDetection(gpsDirectory);
                if (gpsDetection != null) {
                    detections.add(gpsDetection);
                }
            }

            // Create EXIF detection if EXIF data exists
            ExifSubIFDDirectory exifDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (exifDirectory != null) {
                Detection exifDetection = createExifDetection(exifDirectory);
                if (exifDetection != null) {
                    detections.add(exifDetection);
                }
            }

            // Create general metadata detection for all other data
            if (!groupedMetadata.isEmpty()) {
                Detection generalDetection = createGeneralMetadataDetection(groupedMetadata);
                detections.add(generalDetection);
            }

            logger.info("Extracted {} metadata group(s)", detections.size());
            return detections;

        } catch (ImageProcessingException e) {
            logger.warn("Failed to process image metadata: {}", e.getMessage());
            // Return empty list if metadata extraction fails
            return detections;
        } catch (IOException e) {
            throw new VisionProcessingException("Failed to read image data for metadata extraction", e);
        } catch (Exception e) {
            throw new VisionProcessingException("Metadata extraction failed", e);
        }
    }

    /**
     * Creates a GPS detection from GPS directory.
     */
    private Detection createGpsDetection(GpsDirectory gpsDirectory) {
        Map<String, Object> gpsData = new HashMap<>();
        
        // Extract latitude
        if (gpsDirectory.getGeoLocation() != null) {
            gpsData.put("latitude", gpsDirectory.getGeoLocation().getLatitude());
            gpsData.put("longitude", gpsDirectory.getGeoLocation().getLongitude());
        }
        
        // Extract altitude
        if (gpsDirectory.containsTag(GpsDirectory.TAG_ALTITUDE)) {
            Double altitude = gpsDirectory.getDoubleObject(GpsDirectory.TAG_ALTITUDE);
            if (altitude != null) {
                gpsData.put("altitude", altitude);
            }
        }
        
        // Extract timestamp
        if (gpsDirectory.containsTag(GpsDirectory.TAG_TIME_STAMP)) {
            String timestamp = gpsDirectory.getDescription(GpsDirectory.TAG_TIME_STAMP);
            if (timestamp != null) {
                gpsData.put("timestamp", timestamp);
            }
        }
        
        if (gpsData.isEmpty()) {
            return null;
        }
        
        gpsData.put("backend", BACKEND_ID);
        gpsData.put("type", "gps");
        
        // Full image bounding box (normalized coordinates)
        BoundingBox bbox = new BoundingBox(0.0, 0.0, 1.0, 1.0);
        
        return new Detection("gps", 1.0f, bbox, gpsData);
    }

    /**
     * Creates an EXIF detection from EXIF directory.
     */
    private Detection createExifDetection(ExifSubIFDDirectory exifDirectory) {
        Map<String, Object> exifData = new HashMap<>();
        
        // Extract date/time
        if (exifDirectory.containsTag(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)) {
            String datetime = exifDirectory.getDescription(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
            if (datetime != null) {
                exifData.put("dateTimeOriginal", datetime);
            }
        }
        
        // Extract camera settings
        if (exifDirectory.containsTag(ExifSubIFDDirectory.TAG_FNUMBER)) {
            String fNumber = exifDirectory.getDescription(ExifSubIFDDirectory.TAG_FNUMBER);
            if (fNumber != null) {
                exifData.put("fNumber", fNumber);
            }
        }
        
        if (exifDirectory.containsTag(ExifSubIFDDirectory.TAG_EXPOSURE_TIME)) {
            String exposureTime = exifDirectory.getDescription(ExifSubIFDDirectory.TAG_EXPOSURE_TIME);
            if (exposureTime != null) {
                exifData.put("exposureTime", exposureTime);
            }
        }
        
        if (exifDirectory.containsTag(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT)) {
            Integer iso = exifDirectory.getInteger(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT);
            if (iso != null) {
                exifData.put("iso", iso);
            }
        }
        
        if (exifDirectory.containsTag(ExifSubIFDDirectory.TAG_FOCAL_LENGTH)) {
            String focalLength = exifDirectory.getDescription(ExifSubIFDDirectory.TAG_FOCAL_LENGTH);
            if (focalLength != null) {
                exifData.put("focalLength", focalLength);
            }
        }
        
        if (exifData.isEmpty()) {
            return null;
        }
        
        exifData.put("backend", BACKEND_ID);
        exifData.put("type", "exif");
        
        BoundingBox bbox = new BoundingBox(0.0, 0.0, 1.0, 1.0);
        
        return new Detection("exif", 1.0f, bbox, exifData);
    }

    /**
     * Creates a general metadata detection from grouped metadata.
     */
    private Detection createGeneralMetadataDetection(Map<String, Map<String, Object>> groupedMetadata) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("backend", BACKEND_ID);
        attributes.put("type", "metadata");
        attributes.put("directories", groupedMetadata);
        
        // Count total tags
        int tagCount = groupedMetadata.values().stream()
            .mapToInt(Map::size)
            .sum();
        attributes.put("tagCount", tagCount);
        
        BoundingBox bbox = new BoundingBox(0.0, 0.0, 1.0, 1.0);
        
        return new Detection("metadata", 1.0f, bbox, attributes);
    }

    // ==================== AnnotationCapability Implementation ====================

    /**
     * Obscures detections matching the filter by applying blur or pixelation.
     */
    @Override
    public ImageData obscure(ImageData imageData, java.util.function.Predicate<Detection> filter) throws BaseVisionException {
        Objects.requireNonNull(imageData, "ImageData cannot be null");
        Objects.requireNonNull(filter, "Filter cannot be null");
        
        logger.debug("Starting image obscuring");
        
        try {
            // Read image
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData.data()));
            if (image == null) {
                throw new VisionProcessingException("Failed to decode image data");
            }
            
            // Create graphics context
            Graphics2D g2d = image.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Note: This is a simplified implementation. In a full implementation,
            // you would first detect objects, then apply obscuring to matching detections.
            // For now, this returns the original image as-is.
            
            g2d.dispose();
            
            // Convert back to ImageData
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            ImageIO.write(image, imageData.format(), baos);
            
            return ImageData.fromBytes(baos.toByteArray(), imageData.mimeType());
            
        } catch (IOException e) {
            throw new VisionProcessingException("Failed to obscure image", e);
        }
    }

    /**
     * Annotates an image based on the annotation request.
     */
    @Override
    public ImageData annotate(ImageData imageData, AnnotationRequest request) throws BaseVisionException {
        Objects.requireNonNull(imageData, "ImageData cannot be null");
        Objects.requireNonNull(request, "AnnotationRequest cannot be null");
        
        logger.debug("Starting image annotation: action={}", request.getAction());
        
        try {
            // Read image
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData.data()));
            if (image == null) {
                throw new VisionProcessingException("Failed to decode image data");
            }
            
            // Create graphics context
            Graphics2D g2d = image.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            // Apply annotation based on action
            switch (request.getAction()) {
                case MARK -> drawMarkings(g2d, image.getWidth(), image.getHeight());
                case TAG -> drawTags(g2d, image.getWidth(), image.getHeight(), request.getLabel());
                case OBSCURE -> applyObscuring(g2d, image.getWidth(), image.getHeight());
            }
            
            g2d.dispose();
            
            // Convert back to ImageData
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            ImageIO.write(image, imageData.format(), baos);
            
            logger.info("Image annotation completed: action={}", request.getAction());
            return ImageData.fromBytes(baos.toByteArray(), imageData.mimeType());
            
        } catch (IOException e) {
            throw new VisionProcessingException("Failed to annotate image", e);
        }
    }

    /**
     * Draws bounding box markings on the image.
     */
    private void drawMarkings(Graphics2D g2d, int width, int height) {
        // Set drawing properties
        g2d.setColor(Color.GREEN);
        g2d.setStroke(new BasicStroke(3.0f));
        
        // This is a placeholder - in a real implementation, you would draw
        // bounding boxes for detected objects. For now, draw a sample box.
        int boxWidth = width / 4;
        int boxHeight = height / 4;
        int x = (width - boxWidth) / 2;
        int y = (height - boxHeight) / 2;
        
        g2d.drawRect(x, y, boxWidth, boxHeight);
    }

    /**
     * Draws text labels on the image.
     */
    private void drawTags(Graphics2D g2d, int width, int height, String label) {
        // Set font and color
        Font font = new Font("Arial", Font.BOLD, 24);
        g2d.setFont(font);
        g2d.setColor(Color.YELLOW);
        
        // Draw label in the center
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(label);
        int x = (width - textWidth) / 2;
        int y = height / 2;
        
        // Draw shadow
        g2d.setColor(Color.BLACK);
        g2d.drawString(label, x + 2, y + 2);
        
        // Draw label
        g2d.setColor(Color.YELLOW);
        g2d.drawString(label, x, y);
    }

    /**
     * Applies obscuring effects to regions of the image.
     */
    private void applyObscuring(Graphics2D g2d, int width, int height) {
        // Set semi-transparent overlay
        g2d.setColor(new Color(0, 0, 0, 128)); // 50% transparent black
        
        // This is a placeholder - in a real implementation, you would blur
        // specific regions. For now, draw a semi-transparent overlay.
        int boxWidth = width / 4;
        int boxHeight = height / 4;
        int x = (width - boxWidth) / 2;
        int y = (height - boxHeight) / 2;
        
        g2d.fillRect(x, y, boxWidth, boxHeight);
    }

    @PreDestroy
    @Override
    public void shutdown() {
        logger.info("Shutting down DJL vision backend");
        cleanup();
    }

    private void cleanup() {
        logger.info("Cleaning up DJL vision backend resources");

        modelCache.values().forEach(model -> {
            if (model instanceof ZooModel) {
                ((ZooModel<?, ?>) model).close();
            }
        });

        modelCache.clear();
        initialized = false;
        healthStatus = BackendHealthInfo.HealthStatus.UNKNOWN;
        logger.info("DJL vision backend cleanup completed");
    }

    // L2-normalize a float vector; returns a new array (safe to call with null)
    private float[] l2Normalize(float[] v) {
        if (v == null) return new float[0];
        double sum = 0.0;
        for (float f : v) sum += f * f;
        double norm = Math.sqrt(sum);
        if (norm == 0.0) return v;
        float[] out = new float[v.length];
        for (int i = 0; i < v.length; i++) out[i] = (float) (v[i] / norm);
        return out;
    }
}

