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
import ai.djl.engine.Engine;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    EmbeddingCapability {

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

    // Constructors
    public DjlVisionBackend() {
        this.properties = new DjlProperties();
        this.device = Device.fromName(properties.getDevice());
        logBackendInfo();
    }

    public DjlVisionBackend(DjlProperties properties) {
        this.properties = properties != null ? properties : new DjlProperties();
        this.device = Device.fromName(this.properties.getDevice());
        logBackendInfo();
    }

    private void logBackendInfo() {
        logger.info("Initializing DJL Vision Backend - Engine: {}, Device: {}, Version: {}",
            properties.getEngine(), properties.getDevice(), VERSION);
        logger.info("DJL Engine: {}, GPU Available: {}",
            Engine.getEngine(properties.getEngine()).getEngineName(),
            device.isGpu());
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
        details.put("device", device.getDeviceType());
        boolean gpuAvailable = false;
        try {
            gpuAvailable = !device.isGpu() || device.toString().contains("gpu");
        } catch (Exception e) {
            // GPU detection not available in this DJL version
        }
        details.put("gpuAvailable", gpuAvailable);
        details.put("modelsLoaded", modelCache.size());

        if (healthStatus == BackendHealthInfo.HealthStatus.HEALTHY) {
            return BackendHealthInfo.healthy(getBackendId(),
                "DJL backend is operational", responseTime);
        } else {
            return BackendHealthInfo.unhealthy(getBackendId(),
                "DJL backend is not operational", healthErrorMessage, responseTime);
        }
    }

    @Override
    public void initialize() throws BaseVisionException {
        logger.info("Initializing DJL vision backend with all capabilities");

        try {
            // Load core models
            loadFaceDetectionModel();
            loadObjectDetectionModel();

            // Load advanced models (lazy loading supported)
            if (properties.getPoseEstimation() != null) {
                loadPoseEstimationModel();
            }

            if (properties.getActionRecognition() != null) {
                loadActionRecognitionModel();
            }

            if (properties.getSegmentation() != null) {
                loadSegmentationModels();
            }

            if (properties.getFaceRecognition() != null) {
                loadFaceRecognitionModel();
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

    private void loadFaceDetectionModel() throws ModelNotFoundException, MalformedModelException, IOException {
        logger.info("Loading face detection model with DJL");

        Criteria<Image, DetectedObjects> criteria = Criteria.builder()
            .optApplication(Application.CV.OBJECT_DETECTION)
            .setTypes(Image.class, DetectedObjects.class)
            .optFilter("backbone", "resnet50")
            .optFilter("dataset", "wider_face")
            .optEngine(properties.getEngine())
            .optDevice(device)
            .optProgress(properties.isShowProgress() ? new ProgressBar() : null)
            .build();

        faceDetectionModel = criteria.loadModel();
        modelCache.put("face_detection", faceDetectionModel);
        logger.info("Face detection model loaded: {}", faceDetectionModel.getName());
    }

    private void loadFaceRecognitionModel() throws ModelNotFoundException, MalformedModelException, IOException {
        logger.info("Loading face recognition model with DJL");

        Criteria<Image, float[]> criteria = Criteria.builder()
            .optApplication(Application.CV.OBJECT_DETECTION)
            .setTypes(Image.class, float[].class)
            .optFilter("dataset", "vggface2")
            .optEngine(properties.getEngine())
            .optDevice(device)
            .optProgress(properties.isShowProgress() ? new ProgressBar() : null)
            .build();

        faceRecognitionModel = criteria.loadModel();
        modelCache.put("face_recognition", faceRecognitionModel);
        logger.info("Face recognition model loaded: {} (embedding size: {})",
            faceRecognitionModel.getName(), properties.getFaceRecognition().getEmbeddingSize());
    }

    private void loadObjectDetectionModel() throws ModelNotFoundException, MalformedModelException, IOException {
        logger.info("Loading object detection model with DJL");

        String backbone = properties.getObjectDetection().getBackbone();

        Criteria<Image, DetectedObjects> criteria = Criteria.builder()
            .optApplication(Application.CV.OBJECT_DETECTION)
            .setTypes(Image.class, DetectedObjects.class)
            .optFilter("backbone", backbone)
            .optFilter("dataset", "coco")
            .optEngine(properties.getEngine())
            .optDevice(device)
            .optProgress(properties.isShowProgress() ? new ProgressBar() : null)
            .build();

        objectDetectionModel = criteria.loadModel();
        modelCache.put("object_detection", objectDetectionModel);
        logger.info("Object detection model loaded: {} (backbone: {})",
            objectDetectionModel.getName(), backbone);
    }

    private void loadPoseEstimationModel() throws ModelNotFoundException, MalformedModelException, IOException {
        logger.info("Loading pose estimation model with DJL");

        Criteria<Image, Joints> criteria = Criteria.builder()
            .optApplication(Application.CV.POSE_ESTIMATION)
            .setTypes(Image.class, Joints.class)
            .optEngine(properties.getEngine())
            .optDevice(device)
            .optProgress(properties.isShowProgress() ? new ProgressBar() : null)
            .build();

        poseEstimationModel = criteria.loadModel();
        modelCache.put("pose_estimation", poseEstimationModel);
        logger.info("Pose estimation model loaded: {} ({} joints)",
            poseEstimationModel.getName(), properties.getPoseEstimation().getJoints());
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
        DetectionQuery query = new DetectionQuery.Builder()
            .type(DetectionType.FACE)
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

            // Run inference
            try (Predictor<Image, DetectedObjects> predictor = faceDetectionModel.newPredictor()) {
                DetectedObjects detections = predictor.predict(djlImage);

                // Convert DJL detections to Spring Vision detections
                List<Detection> results = convertDetections(detections, query);

                logger.info("DJL face detection completed: {} faces detected, correlationId={}, backend=djl",
                    results.size(), correlationId);

                return results;
            }

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

            try (Predictor<Image, DetectedObjects> predictor = objectDetectionModel.newPredictor()) {
                DetectedObjects detections = predictor.predict(djlImage);
                List<Detection> results = convertDetections(detections, query);

                logger.info("DJL object detection completed: {} objects detected, correlationId={}, backend=djl",
                    results.size(), correlationId);

                return results;
            }
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

            try (Predictor<Image, Joints> predictor = poseEstimationModel.newPredictor()) {
                Joints joints = predictor.predict(djlImage);

                List<Detection> results = convertJointsToDetections(joints);

                logger.info("DJL pose estimation completed: {} poses detected, correlationId={}",
                    results.size(), correlationId);

                return results;
            }

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

            try (Predictor<Image, float[]> predictor = actionRecognitionModel.newPredictor()) {
                float[] predictions = predictor.predict(djlImage);

                List<Detection> results = convertActionPredictions(predictions);

                logger.info("DJL action recognition completed: {} actions detected, correlationId={}",
                    results.size(), correlationId);

                return results;
            }

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

            try (Predictor<Image, CategoryMask> predictor = semanticSegmentationModel.newPredictor()) {
                CategoryMask mask = predictor.predict(djlImage);

                VisionResult result = convertSemanticMaskToResult(mask, correlationId);

                logger.info("DJL semantic segmentation completed: correlationId={}", correlationId);

                return result;
            }

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

            try (Predictor<Image, DetectedObjects> predictor = instanceSegmentationModel.newPredictor()) {
                DetectedObjects detections = predictor.predict(djlImage);

                VisionResult result = convertInstanceDetectionsToResult(detections, correlationId);

                logger.info("DJL instance segmentation completed: {} instances, correlationId={}",
                    detections.getNumberOfObjects(), correlationId);

                return result;
            }

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

            // Mask data check - safely handle if getMask() doesn't exist
            // Commenting out problematic API call
            // if (obj.getMask() != null) {
            //     attributes.put("mask", obj.getMask());
            // }

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
        if (!initialized || faceRecognitionModel == null) {
            // Lazy load if not already loaded
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

            try (Predictor<Image, float[]> predictor = faceRecognitionModel.newPredictor()) {
                float[] embedding = predictor.predict(djlImage);

                logger.info("DJL face embedding extracted: size={}, correlationId={}",
                    embedding.length, correlationId);

                return List.of(embedding);
            }

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
}
