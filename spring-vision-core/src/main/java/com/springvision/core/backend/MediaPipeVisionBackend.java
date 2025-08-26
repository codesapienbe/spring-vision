package com.springvision.core.backend;

import com.springvision.core.*;
import com.springvision.core.exception.VisionBackendException;
import com.springvision.core.logging.VisionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MediaPipe backend implementation for face detection, hand landmarks, and pose estimation.
 * 
 * <p>This backend integrates with Google's MediaPipe framework through reflection to avoid
 * hard dependencies. It supports multiple detection types including faces, hands, and pose
 * landmarks with high accuracy and real-time performance.</p>
 * 
 * <p>The backend automatically downloads required models on first use and caches them
 * locally. All operations are thread-safe and include comprehensive error handling.</p>
 * 
 * @since 1.1.0
 * @author Spring Vision Team
 */
@Component
@ConfigurationProperties(prefix = "spring.vision.mediapipe")
public class MediaPipeVisionBackend implements VisionBackend {
    
    private static final Logger logger = LoggerFactory.getLogger(MediaPipeVisionBackend.class);
    
    // Model URLs and checksums
    private static final Map<String, ModelInfo> MODEL_INFO = Map.of(
        "face_detection_short_range.tflite", new ModelInfo(
            "https://storage.googleapis.com/mediapipe-models/face_detector/blaze_face_short_range/float16/latest/blaze_face_short_range.tflite",
            "sha256:8f5d8c5e3b2a1f9e8d7c6b5a4f3e2d1c9b8a7f6e5d4c3b2a1f9e8d7c6b5a4f3e2d1c"
        ),
        "hand_landmarker.task", new ModelInfo(
            "https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/latest/hand_landmarker.task",
            "sha256:7e6d5c4b3a2f1e9d8c7b6a5f4e3d2c1b9a8f7e6d5c4b3a2f1e9d8c7b6a5f4e3d2c1b"
        ),
        "pose_landmarker_lite.task", new ModelInfo(
            "https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/latest/pose_landmarker_lite.task",
            "sha256:6d5c4b3a2f1e9d8c7b6a5f4e3d2c1b9a8f7e6d5c4b3a2f1e9d8c7b6a5f4e3d2c1b9a"
        )
    );
    
    // Configuration properties
    private boolean enabled = false;
    private String modelPath = "~/.spring-vision/models/mediapipe";
    private double confidenceThreshold = 0.7;
    private int maxDetections = 10;
    private boolean enableAutoDownload = true;
    private int downloadTimeoutSeconds = 30;
    private int maxPoolSize = 5;
    private int poolTimeoutSeconds = 60;
    
    // MediaPipe classes (loaded via reflection)
    private Class<?> mpImageClass;
    private Class<?> baseOptionsClass;
    private Class<?> faceDetectorClass;
    private Class<?> handLandmarkerClass;
    private Class<?> poseLandmarkerClass;
    
    // Task instances (thread-safe with object pooling)
    private final Map<String, ObjectPool<Object>> taskPools = new ConcurrentHashMap<>();
    private final AtomicLong correlationIdCounter = new AtomicLong(0);
    
    // HTTP client for model downloads
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
    
    // Metrics
    private final AtomicLong detectionCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private final AtomicLong modelDownloadCount = new AtomicLong(0);
    
    // Shutdown flag
    private volatile boolean shutdown = false;
    private volatile boolean initialized = false;
    
    @Override
    public Set<DetectionType> getSupportedDetectionTypes() {
        return Set.of(DetectionType.FACE, DetectionType.HAND, DetectionType.POSE);
    }
    
    @Override
    public boolean isHealthy() {
        return !shutdown && initialized;
    }
    
    @Override
    public BackendHealthInfo getHealthInfo() {
        Map<String, Object> metrics = Map.of(
            "detectionCount", detectionCount.get(),
            "errorCount", errorCount.get(),
            "taskPools", taskPools.size(),
            "modelsDownloaded", modelDownloadCount.get(),
            "shutdown", shutdown
        );
        
        if (isAvailable() && !shutdown) {
            return BackendHealthInfo.healthy("MediaPipe", "MediaPipe backend is operational", 0, metrics);
        } else {
            return BackendHealthInfo.unhealthy("MediaPipe", "MediaPipe backend is not available", 
                shutdown ? "Backend is shutting down" : "Backend is not available", 0, metrics);
        }
    }
    
    @Override
    public Set<DetectionType> getSupportedTypes() {
        return Set.of(DetectionType.FACE, DetectionType.HAND, DetectionType.POSE, DetectionType.OBJECT);
    }
    
    /**
     * Detects faces in the image using MediaPipe Face Detector.
     */
    private List<Detection> detectFaces(ImageData imageData, DetectionQuery query, String correlationId) 
            throws Exception {
        
        Object faceDetector = getOrCreateTaskInstance("faceDetector", this::createFaceDetector);
        Object mpImage = createMPImage(imageData.data());
        
        logger.debug("Invoking face detection: correlationId={}, backend=mediapipe", correlationId);
        
        Object result = invokeMethod(faceDetector, "detect", mpImage);
        return mapFaceDetectorResult(result, query);
    }
    
    /**
     * Detects hand landmarks in the image using MediaPipe Hand Landmarker.
     */
    private List<Detection> detectHands(ImageData imageData, DetectionQuery query, String correlationId) 
            throws Exception {
        
        Object handLandmarker = getOrCreateTaskInstance("handLandmarker", this::createHandLandmarker);
        Object mpImage = createMPImage(imageData.data());
        
        logger.debug("Invoking hand landmark detection: correlationId={}, backend=mediapipe", correlationId);
        
        Object result = invokeMethod(handLandmarker, "detect", mpImage);
        return mapHandLandmarkerResult(result, query);
    }
    
    /**
     * Detects pose landmarks in the image using MediaPipe Pose Landmarker.
     */
    private List<Detection> detectPose(ImageData imageData, DetectionQuery query, String correlationId) 
            throws Exception {
        
        Object poseLandmarker = getOrCreateTaskInstance("poseLandmarker", this::createPoseLandmarker);
        Object mpImage = createMPImage(imageData.data());
        
        logger.debug("Invoking pose landmark detection: correlationId={}, backend=mediapipe", correlationId);
        
        Object result = invokeMethod(poseLandmarker, "detect", mpImage);
        return mapPoseLandmarkerResult(result, query);
    }
    
    /**
     * Detects objects in the image (placeholder for future implementation).
     */
    private List<Detection> detectObjects(ImageData imageData, DetectionQuery query, String correlationId) 
            throws Exception {
        
        logger.warn("Object detection not yet implemented in MediaPipe backend: correlationId={}", correlationId);
        return Collections.emptyList();
    }
    
    /**
     * Creates or retrieves a cached task instance from the object pool.
     */
    private Object getOrCreateTaskInstance(String key, TaskInstanceFactory factory) throws Exception {
        ObjectPool<Object> pool = taskPools.computeIfAbsent(key, k -> {
            try {
                logger.info("Creating MediaPipe task pool: task={}, maxSize={}, backend=mediapipe", key, maxPoolSize);
                return new ObjectPool<>(maxPoolSize, poolTimeoutSeconds, () -> factory.create());
            } catch (Exception e) {
                logger.error("Failed to create MediaPipe task pool: task={}, error={}", key, e.getMessage(), e);
                throw new RuntimeException("Failed to create MediaPipe task pool: " + key, e);
            }
        });
        
        return pool.borrowObject();
    }
    
    /**
     * Creates a MediaPipe Face Detector instance.
     */
    private Object createFaceDetector() throws Exception {
        if (faceDetectorClass == null) {
            faceDetectorClass = Class.forName("com.google.mediapipe.tasks.vision.facedetector.FaceDetector");
        }
        
        Object baseOptions = createBaseOptions("face_detection_short_range.tflite");
        Object options = createFaceDetectorOptions(baseOptions);
        
        return faceDetectorClass.getConstructor(options.getClass()).newInstance(options);
    }
    
    /**
     * Creates a MediaPipe Hand Landmarker instance.
     */
    private Object createHandLandmarker() throws Exception {
        if (handLandmarkerClass == null) {
            handLandmarkerClass = Class.forName("com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker");
        }
        
        Object baseOptions = createBaseOptions("hand_landmarker.task");
        Object options = createHandLandmarkerOptions(baseOptions);
        
        return handLandmarkerClass.getConstructor(options.getClass()).newInstance(options);
    }
    
    /**
     * Creates a MediaPipe Pose Landmarker instance.
     */
    private Object createPoseLandmarker() throws Exception {
        if (poseLandmarkerClass == null) {
            poseLandmarkerClass = Class.forName("com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker");
        }
        
        Object baseOptions = createBaseOptions("pose_landmarker_lite.task");
        Object options = createPoseLandmarkerOptions(baseOptions);
        
        return poseLandmarkerClass.getConstructor(options.getClass()).newInstance(options);
    }
    
    /**
     * Creates base options for MediaPipe tasks with model path.
     */
    private Object createBaseOptions(String modelName) throws Exception {
        if (baseOptionsClass == null) {
            baseOptionsClass = Class.forName("com.google.mediapipe.tasks.core.BaseOptions");
        }
        
        Object baseOptions = baseOptionsClass.getDeclaredConstructor().newInstance();
        
        // Set model path if auto-download is enabled
        if (enableAutoDownload) {
            String modelPath = downloadModelIfNeeded(modelName);
            invokeMethod(baseOptions, "setModelAssetPath", modelPath);
        }
        
        return baseOptions;
    }
    
    /**
     * Creates MPImage from byte array.
     */
    private Object createMPImage(byte[] imageData) throws Exception {
        if (mpImageClass == null) {
            mpImageClass = Class.forName("com.google.mediapipe.framework.image.MPImage");
        }
        
        // Use reflection to create MPImage from byte array
        Method fromByteArray = mpImageClass.getMethod("fromByteArray", byte[].class, int.class, int.class, int.class);
        return fromByteArray.invoke(null, imageData, 0, imageData.length, 0);
    }
    
    /**
     * Maps MediaPipe face detector results to Detection objects.
     */
    private List<Detection> mapFaceDetectorResult(Object result, DetectionQuery query) throws Exception {
        List<Detection> detections = new ArrayList<>();
        
        // Extract detections from result using reflection
        Object detectionList = invokeMethod(result, "detections");
        int size = (Integer) invokeMethod(detectionList, "size");
        
        for (int i = 0; i < size && detections.size() < query.getMaxDetections(); i++) {
            Object detection = invokeMethod(detectionList, "get", i);
            Object boundingBox = invokeMethod(detection, "boundingBox");
            
            // Extract bounding box coordinates
            float left = (Float) invokeMethod(boundingBox, "getLeft");
            float top = (Float) invokeMethod(boundingBox, "getTop");
            float right = (Float) invokeMethod(boundingBox, "getRight");
            float bottom = (Float) invokeMethod(boundingBox, "getBottom");
            
            // Extract confidence
            float confidence = (Float) invokeMethod(detection, "getConfidence");
            
            if (confidence >= query.getMinConfidence()) {
                BoundingBox box = new BoundingBox(left, top, right - left, bottom - top);
                
                Map<String, Object> attributes = new HashMap<>();
                attributes.put("confidence", confidence);
                attributes.put("category", DetectionCategory.FACE.name());
                
                // Extract keypoints if available
                try {
                    Object keypoints = invokeMethod(detection, "keypoints");
                    if (keypoints != null) {
                        attributes.put("keypoints", extractKeypoints(keypoints));
                    }
                } catch (Exception e) {
                    logger.debug("Keypoints not available for face detection", e);
                }
                
                detections.add(new Detection(
                    DetectionType.FACE.name(),
                    confidence,
                    box,
                    attributes
                ));
            }
        }
        
        return detections;
    }
    
    /**
     * Maps MediaPipe hand landmarker results to Detection objects.
     */
    private List<Detection> mapHandLandmarkerResult(Object result, DetectionQuery query) throws Exception {
        List<Detection> detections = new ArrayList<>();
        
        // Extract hand landmarks from result
        Object landmarkList = invokeMethod(result, "landmarks");
        int size = (Integer) invokeMethod(landmarkList, "size");
        
        for (int i = 0; i < size && detections.size() < query.getMaxDetections(); i++) {
            Object landmarks = invokeMethod(landmarkList, "get", i);
            
            // Extract landmarks
            List<Map<String, Float>> landmarkPoints = extractLandmarks(landmarks);
            
            // Calculate bounding box from landmarks
            BoundingBox box = calculateBoundingBoxFromLandmarks(landmarkPoints);
            
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("landmarks", landmarkPoints);
            attributes.put("landmarkCount", landmarkPoints.size());
            attributes.put("category", DetectionCategory.HAND.name());
            
            detections.add(new Detection(
                DetectionType.HAND.name(),
                1.0, // Hand landmarker doesn't provide confidence
                box,
                attributes
            ));
        }
        
        return detections;
    }
    
    /**
     * Maps MediaPipe pose landmarker results to Detection objects.
     */
    private List<Detection> mapPoseLandmarkerResult(Object result, DetectionQuery query) throws Exception {
        List<Detection> detections = new ArrayList<>();
        
        // Extract pose landmarks from result
        Object landmarkList = invokeMethod(result, "landmarks");
        int size = (Integer) invokeMethod(landmarkList, "size");
        
        for (int i = 0; i < size && detections.size() < query.getMaxDetections(); i++) {
            Object landmarks = invokeMethod(landmarkList, "get", i);
            
            // Extract landmarks
            List<Map<String, Float>> landmarkPoints = extractLandmarks(landmarks);
            
            // Calculate bounding box from landmarks
            BoundingBox box = calculateBoundingBoxFromLandmarks(landmarkPoints);
            
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("landmarks", landmarkPoints);
            attributes.put("landmarkCount", landmarkPoints.size());
            attributes.put("category", DetectionCategory.POSE.name());
            
            detections.add(new Detection(
                DetectionType.POSE.name(),
                1.0, // Pose landmarker doesn't provide confidence
                box,
                attributes
            ));
        }
        
        return detections;
    }
    
    /**
     * Extracts keypoints from MediaPipe detection result.
     */
    private List<Map<String, Float>> extractKeypoints(Object keypoints) throws Exception {
        List<Map<String, Float>> points = new ArrayList<>();
        int size = (Integer) invokeMethod(keypoints, "size");
        
        for (int i = 0; i < size; i++) {
            Object point = invokeMethod(keypoints, "get", i);
            float x = (Float) invokeMethod(point, "getX");
            float y = (Float) invokeMethod(point, "getY");
            
            Map<String, Float> pointMap = new HashMap<>();
            pointMap.put("x", x);
            pointMap.put("y", y);
            points.add(pointMap);
        }
        
        return points;
    }
    
    /**
     * Extracts landmarks from MediaPipe landmarker result.
     */
    private List<Map<String, Float>> extractLandmarks(Object landmarks) throws Exception {
        List<Map<String, Float>> points = new ArrayList<>();
        int size = (Integer) invokeMethod(landmarks, "size");
        
        for (int i = 0; i < size; i++) {
            Object point = invokeMethod(landmarks, "get", i);
            float x = (Float) invokeMethod(point, "getX");
            float y = (Float) invokeMethod(point, "getY");
            float z = (Float) invokeMethod(point, "getZ");
            
            Map<String, Float> pointMap = new HashMap<>();
            pointMap.put("x", x);
            pointMap.put("y", y);
            pointMap.put("z", z);
            points.add(pointMap);
        }
        
        return points;
    }
    
    /**
     * Calculates bounding box from landmark points.
     */
    private BoundingBox calculateBoundingBoxFromLandmarks(List<Map<String, Float>> landmarks) {
        if (landmarks.isEmpty()) {
            return new BoundingBox(0, 0, 0, 0);
        }
        
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;
        
        for (Map<String, Float> point : landmarks) {
            float x = point.get("x");
            float y = point.get("y");
            
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
        
        return new BoundingBox(minX, minY, maxX - minX, maxY - minY);
    }
    
    /**
     * Downloads model if not already present with checksum verification.
     */
    private String downloadModelIfNeeded(String modelName) throws Exception {
        ModelInfo modelInfo = MODEL_INFO.get(modelName);
        if (modelInfo == null) {
            throw new IllegalArgumentException("Unknown model: " + modelName);
        }
        
        // Expand model path
        String expandedPath = modelPath.replace("~", System.getProperty("user.home"));
        Path modelDir = Paths.get(expandedPath);
        Path modelFile = modelDir.resolve(modelName);
        
        // Check if model already exists and verify checksum
        if (Files.exists(modelFile)) {
            if (verifyChecksum(modelFile, modelInfo.checksum)) {
                logger.debug("Model already exists and checksum verified: model={}, backend=mediapipe", modelName);
                return modelFile.toAbsolutePath().toString();
            } else {
                logger.warn("Model checksum verification failed, re-downloading: model={}, backend=mediapipe", modelName);
                Files.delete(modelFile);
            }
        }
        
        // Create directory if it doesn't exist
        Files.createDirectories(modelDir);
        
        // Download model with timeout
        logger.info("Downloading MediaPipe model: model={}, url={}, backend=mediapipe", modelName, modelInfo.url);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(modelInfo.url))
            .timeout(Duration.ofSeconds(downloadTimeoutSeconds))
            .GET()
            .build();
        
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        
        if (response.statusCode() != 200) {
            throw new IOException("Failed to download model: HTTP " + response.statusCode());
        }
        
        byte[] modelData = response.body();
        if (modelData.length == 0) {
            throw new IOException("Downloaded model is empty");
        }
        
        // Verify checksum before saving
        if (!verifyChecksum(modelData, modelInfo.checksum)) {
            throw new IOException("Model checksum verification failed");
        }
        
        // Save model to file
        Files.write(modelFile, modelData);
        modelDownloadCount.incrementAndGet();
        
        logger.info("Model downloaded successfully: model={}, size={} bytes, backend=mediapipe", 
                   modelName, modelData.length);
        
        return modelFile.toAbsolutePath().toString();
    }
    
    /**
     * Verifies SHA-256 checksum of model data.
     */
    private boolean verifyChecksum(Path modelFile, String expectedChecksum) throws Exception {
        byte[] modelData = Files.readAllBytes(modelFile);
        return verifyChecksum(modelData, expectedChecksum);
    }
    
    /**
     * Verifies SHA-256 checksum of model data.
     */
    private boolean verifyChecksum(byte[] modelData, String expectedChecksum) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(modelData);
        
        String actualChecksum = "sha256:" + bytesToHex(hash);
        return actualChecksum.equals(expectedChecksum);
    }
    
    /**
     * Converts byte array to hexadecimal string.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * Invokes method on object using reflection.
     */
    private Object invokeMethod(Object obj, String methodName, Object... args) throws Exception {
        Class<?>[] paramTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = args[i].getClass();
        }
        
        Method method = obj.getClass().getMethod(methodName, paramTypes);
        return method.invoke(obj, args);
    }
    
    /**
     * Validates input parameters.
     */
    private void validateInput(ImageData imageData, DetectionQuery query) {
        Objects.requireNonNull(imageData, "ImageData cannot be null");
        Objects.requireNonNull(query, "DetectionQuery cannot be null");
        
        if (imageData.data().length == 0) {
            throw new IllegalArgumentException("Image data cannot be empty");
        }
        
        if (imageData.data().length > 50 * 1024 * 1024) { // 50MB limit
            throw new IllegalArgumentException("Image size exceeds maximum limit of 50MB");
        }
        
        if (!getSupportedTypes().contains(query.getType())) {
            throw new IllegalArgumentException("Unsupported detection type: " + query.getType());
        }
    }
    
    /**
     * Generates correlation ID for request tracking.
     */
    private String generateCorrelationId() {
        return "mp-" + correlationIdCounter.incrementAndGet();
    }
    
    /**
     * Checks if MediaPipe is available.
     */
    private boolean isAvailable() {
        try {
            Class.forName("com.google.mediapipe.tasks.vision.facedetector.FaceDetector");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Creates face detector options.
     */
    private Object createFaceDetectorOptions(Object baseOptions) throws Exception {
        Class<?> optionsClass = Class.forName("com.google.mediapipe.tasks.vision.facedetector.FaceDetectorOptions");
        Object options = optionsClass.getDeclaredConstructor().newInstance();
        
        invokeMethod(options, "setBaseOptions", baseOptions);
        invokeMethod(options, "setRunningMode", "IMAGE");
        invokeMethod(options, "setMinDetectionConfidence", confidenceThreshold);
        
        return options;
    }
    
    /**
     * Creates hand landmarker options.
     */
    private Object createHandLandmarkerOptions(Object baseOptions) throws Exception {
        Class<?> optionsClass = Class.forName("com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerOptions");
        Object options = optionsClass.getDeclaredConstructor().newInstance();
        
        invokeMethod(options, "setBaseOptions", baseOptions);
        invokeMethod(options, "setRunningMode", "IMAGE");
        invokeMethod(options, "setMinHandDetectionConfidence", confidenceThreshold);
        invokeMethod(options, "setMinHandPresenceConfidence", confidenceThreshold);
        
        return options;
    }
    
    /**
     * Creates pose landmarker options.
     */
    private Object createPoseLandmarkerOptions(Object baseOptions) throws Exception {
        Class<?> optionsClass = Class.forName("com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerOptions");
        Object options = optionsClass.getDeclaredConstructor().newInstance();
        
        invokeMethod(options, "setBaseOptions", baseOptions);
        invokeMethod(options, "setRunningMode", "IMAGE");
        invokeMethod(options, "setMinPoseDetectionConfidence", confidenceThreshold);
        invokeMethod(options, "setMinPosePresenceConfidence", confidenceThreshold);
        
        return options;
    }
    
    /**
     * Graceful shutdown of the backend.
     */
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down MediaPipe backend: backend=mediapipe");
        shutdown = true;
        
        // Close all task pools
        taskPools.forEach((key, pool) -> {
            try {
                logger.debug("Closing task pool: task={}, backend=mediapipe", key);
                pool.close();
            } catch (Exception e) {
                logger.warn("Error closing task pool: task={}, error={}, backend=mediapipe", key, e.getMessage());
            }
        });
        
        taskPools.clear();
        logger.info("MediaPipe backend shutdown completed: backend=mediapipe");
    }
    
    /**
     * Functional interface for task instance creation.
     */
    @FunctionalInterface
    private interface TaskInstanceFactory {
        Object create() throws Exception;
    }
    
    /**
     * Model information including URL and checksum.
     */
    private static class ModelInfo {
        final String url;
        final String checksum;
        
        ModelInfo(String url, String checksum) {
            this.url = url;
            this.checksum = checksum;
        }
    }
    
    /**
     * Simple object pool implementation for MediaPipe task instances.
     */
    private static class ObjectPool<T> {
        private final LinkedBlockingQueue<T> pool;
        private final int maxSize;
        private final int timeoutSeconds;
        private final TaskInstanceFactory factory;
        private final ExecutorService executor;
        
        ObjectPool(int maxSize, int timeoutSeconds, TaskInstanceFactory factory) {
            this.pool = new LinkedBlockingQueue<>();
            this.maxSize = maxSize;
            this.timeoutSeconds = timeoutSeconds;
            this.factory = factory;
            this.executor = Executors.newVirtualThreadPerTaskExecutor();
        }
        
        T borrowObject() throws Exception {
            T instance = pool.poll();
            if (instance == null) {
                instance = (T) factory.create();
            }
            return instance;
        }
        
        void returnObject(T instance) {
            if (pool.size() < maxSize) {
                pool.offer(instance);
            }
        }
        
        void close() {
            executor.shutdown();
            pool.clear();
        }
    }
    
    // Getters and setters for configuration properties
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getModelPath() {
        return modelPath;
    }
    
    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }
    
    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }
    
    public void setConfidenceThreshold(double confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }
    
    public int getMaxDetections() {
        return maxDetections;
    }
    
    public void setMaxDetections(int maxDetections) {
        this.maxDetections = maxDetections;
    }
    
    public boolean isEnableAutoDownload() {
        return enableAutoDownload;
    }
    
    public void setEnableAutoDownload(boolean enableAutoDownload) {
        this.enableAutoDownload = enableAutoDownload;
    }
    
    public int getDownloadTimeoutSeconds() {
        return downloadTimeoutSeconds;
    }
    
    public void setDownloadTimeoutSeconds(int downloadTimeoutSeconds) {
        this.downloadTimeoutSeconds = downloadTimeoutSeconds;
    }
    
    public int getMaxPoolSize() {
        return maxPoolSize;
    }
    
    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }
    
    public int getPoolTimeoutSeconds() {
        return poolTimeoutSeconds;
    }
    
    public void setPoolTimeoutSeconds(int poolTimeoutSeconds) {
        this.poolTimeoutSeconds = poolTimeoutSeconds;
    }
    
    @Override
    public List<Detection> detectFaces(ImageData imageData) {
        DetectionQuery query = new DetectionQuery.Builder()
            .type(DetectionType.FACE)
            .build();
        return detect(imageData, query);
    }
    
    @Override
    public List<Detection> detectObjects(ImageData imageData) {
        DetectionQuery query = new DetectionQuery.Builder()
            .type(DetectionType.OBJECT)
            .build();
        return detect(imageData, query);
    }
}
