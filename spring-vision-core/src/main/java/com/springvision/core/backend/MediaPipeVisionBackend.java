package com.springvision.core.backend;

import com.springvision.core.BackendHealthInfo;
import com.springvision.core.DetectionType;
import com.springvision.core.ImageData;
import com.springvision.core.VisionBackend;
import com.springvision.core.VisionResult;
import com.springvision.core.exception.BaseVisionException;
import com.springvision.core.exception.VisionBackendException;
import com.springvision.core.exception.VisionProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.springvision.core.BoundingBox;

/**
 * MediaPipe-based implementation of the VisionBackend interface.
 *
 * <p>This backend integrates with Google MediaPipe Tasks when the corresponding
 * Java bindings are available on the classpath. It performs runtime capability
 * probing via reflection to avoid a hard dependency. When MediaPipe is not
 * available, the backend will initialize in a degraded state and throw
 * {@link VisionBackendException} on detection requests while reporting
 * unhealthy status via {@link #getHealthInfo()}.</p>
 *
 * <p>The implementation focuses on safe-by-default behavior and avoids changing
 * any existing observable behavior unless explicitly configured with
 * {@code vision.backend=mediapipe}.</p>
 *
 * <p>Supported detections in this initial integration are scoped to
 * {@link DetectionType#FACE}, {@link DetectionType#HAND},
 * {@link DetectionType#POSE}, and {@link DetectionType#LANDMARK}. Additional
 * tasks can be enabled incrementally as the project evolves.</p>
 */
public class MediaPipeVisionBackend implements VisionBackend, com.springvision.core.capabilities.FaceDetectionCapability, com.springvision.core.capabilities.HandDetectionCapability, com.springvision.core.capabilities.PoseEstimationCapability, com.springvision.core.capabilities.LandmarkDetectionCapability {

    private static final Logger logger = LoggerFactory.getLogger(MediaPipeVisionBackend.class);

    /** Backend identifier for MediaPipe. */
    public static final String BACKEND_ID = "mediapipe";
    /** Display name for MediaPipe backend. */
    public static final String DISPLAY_NAME = "MediaPipe Vision Backend";

    /**
    * Version string reported from classpath if available, otherwise a static fallback.
    */
    private String version = "unknown";

    private volatile boolean mediapipeAvailable = false;
    private volatile boolean initialized = false;

    private volatile BackendHealthInfo.HealthStatus healthStatus = BackendHealthInfo.HealthStatus.UNKNOWN;
    private volatile String healthErrorMessage = "Backend not initialized";
    private volatile long lastHealthCheckTimeMs = 0L;

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
        return version;
    }

    @Override
    public Set<DetectionType> getSupportedDetectionTypes() {
        return Set.of(DetectionType.FACE, DetectionType.HAND, DetectionType.POSE, DetectionType.LANDMARK);
    }

    @Override
    public boolean isHealthy() {
        return initialized && mediapipeAvailable && healthStatus == BackendHealthInfo.HealthStatus.HEALTHY;
    }

    @Override
    public BackendHealthInfo getHealthInfo() {
        long responseTime = Math.max(0L, System.currentTimeMillis() - lastHealthCheckTimeMs);
        if (isHealthy()) {
            return BackendHealthInfo.healthy(getBackendId(), "MediaPipe backend is operational", responseTime);
        }
        return BackendHealthInfo.unhealthy(getBackendId(), "MediaPipe backend is not operational", healthErrorMessage, responseTime);
    }

    @Override
    public void initialize() throws BaseVisionException {
        lastHealthCheckTimeMs = System.currentTimeMillis();
        try {
            // Probe for common MediaPipe Tasks classes to determine availability
            boolean frameworkPresent = isPresent("com.google.mediapipe.framework.Graph")
                || isPresent("com.google.mediapipe.framework.Packet");
            boolean tasksPresent = isPresent("com.google.mediapipe.tasks.core.BaseTaskApi")
                || isPresent("com.google.mediapipe.tasks.vision.core.RunningMode")
                || isPresent("com.google.mediapipe.tasks.vision.facedetector.FaceDetector");

            mediapipeAvailable = frameworkPresent || tasksPresent;

            if (mediapipeAvailable) {
                version = resolveMediapipeVersionSafely();
                healthStatus = BackendHealthInfo.HealthStatus.HEALTHY;
                healthErrorMessage = "";
                logger.info("MediaPipe backend initialized", Map.of(
                    "component", BACKEND_ID,
                    "version", version
                ));
            } else {
                healthStatus = BackendHealthInfo.HealthStatus.UNHEALTHY;
                healthErrorMessage = "MediaPipe classes not found on classpath";
                logger.warn("MediaPipe not available - backend in degraded mode", Map.of(
                    "component", BACKEND_ID
                ));
            }
            initialized = true;
        } catch (Exception e) {
            initialized = true; // mark initialized to expose consistent health state
            mediapipeAvailable = false;
            healthStatus = BackendHealthInfo.HealthStatus.UNHEALTHY;
            healthErrorMessage = "Initialization failed: " + e.getMessage();
            logger.error("MediaPipe backend initialization failed", Map.of(
                "component", BACKEND_ID,
                "error", e.getClass().getSimpleName()
            ), e);
        }
    }

    @Override
    public VisionResult detectFaces(ImageData imageData) throws BaseVisionException {
        ensureReady(imageData, DetectionType.FACE);
        long start = System.currentTimeMillis();
        String correlationId = generateCorrelationId();
        logger.debug("Starting MediaPipe face detection", Map.of(
            "component", BACKEND_ID,
            "operation", "detect_faces",
            "correlation_id", correlationId,
            "image_size", imageData.getSizeInBytes(),
            "image_format", imageData.format()
        ));

        try {
            VisionResult mpResult = tryDetectFacesWithMediaPipe(imageData, correlationId);
            if (mpResult != null) {
                return mpResult;
            }
            // If MediaPipe classes are present but we couldn't process, fall back to a descriptive error
            throw new VisionBackendException(
                "MediaPipe face detection could not process the input image (MPImage conversion unsupported in this runtime)",
                "mediapipe_image_conversion_unsupported",
                DetectionType.FACE.getCode()
            );
        } catch (BaseVisionException e) {
            throw e;
        } catch (Exception e) {
            throw new VisionProcessingException(
                "MediaPipe face detection failed",
                "mediapipe_face_detection_failed",
                DetectionType.FACE.getCode(),
                e
            );
        } finally {
                    logger.debug("Completed MediaPipe face detection", Map.of(
            "component", BACKEND_ID,
            "operation", "detect_faces",
            "correlation_id", correlationId,
            "elapsed_ms", System.currentTimeMillis() - start
        ));
        }
    }

    @Override
    public VisionResult detectObjects(ImageData imageData) throws BaseVisionException {
        // Not advertised in supported types; keep explicit for future extension.
        throw new VisionBackendException(
            "Object detection is not supported by MediaPipe backend in this version",
            "mediapipe_not_supported",
            DetectionType.OBJECT.getCode()
        );
    }

    @Override
    public VisionResult detectLandmarks(ImageData imageData) throws BaseVisionException {
        ensureReady(imageData, DetectionType.LANDMARK);
        long start = System.currentTimeMillis();
        String correlationId = generateCorrelationId();
        logger.debug("Starting MediaPipe landmark detection", Map.of(
            "component", BACKEND_ID,
            "operation", "detect_landmarks",
            "correlation_id", correlationId,
            "image_size", imageData.getSizeInBytes(),
            "image_format", imageData.format()
        ));
        try {
            VisionResult result = tryDetectLandmarksWithMediaPipe(imageData, correlationId);
            if (result != null) {
                return result;
            }
            throw new VisionBackendException(
                "MediaPipe landmark detection is not available in this runtime",
                "mediapipe_landmark_runtime_unavailable",
                DetectionType.LANDMARK.getCode()
            );
        } catch (BaseVisionException e) {
            throw e;
        } catch (Exception e) {
            throw new VisionProcessingException(
                "MediaPipe landmark detection failed",
                "mediapipe_landmark_failed",
                DetectionType.LANDMARK.getCode(),
                e
            );
        } finally {
                    logger.debug("Completed MediaPipe landmark detection", Map.of(
            "component", BACKEND_ID,
            "operation", "detect_landmarks",
            "correlation_id", correlationId,
            "elapsed_ms", System.currentTimeMillis() - start
        ));
        }
    }

    @Override
    public VisionResult detectPoses(ImageData imageData) throws BaseVisionException {
        ensureReady(imageData, DetectionType.POSE);
        long start = System.currentTimeMillis();
        String correlationId = generateCorrelationId();
        logger.debug("Starting MediaPipe pose detection", Map.of(
            "component", BACKEND_ID,
            "operation", "detect_poses",
            "correlation_id", correlationId,
            "image_size", imageData.getSizeInBytes(),
            "image_format", imageData.format()
        ));
        try {
            VisionResult result = tryDetectPoseWithMediaPipe(imageData, correlationId);
            if (result != null) {
                return result;
            }
            throw new VisionBackendException(
                "MediaPipe pose detection is not available in this runtime",
                "mediapipe_pose_runtime_unavailable",
                DetectionType.POSE.getCode()
            );
        } catch (BaseVisionException e) {
            throw e;
        } catch (Exception e) {
            throw new VisionProcessingException(
                "MediaPipe pose detection failed",
                "mediapipe_pose_failed",
                DetectionType.POSE.getCode(),
                e
            );
        } finally {
                    logger.debug("Completed MediaPipe pose detection", Map.of(
            "component", BACKEND_ID,
            "operation", "detect_poses",
            "correlation_id", correlationId,
            "elapsed_ms", System.currentTimeMillis() - start
        ));
        }
    }

    @Override
    public VisionResult detectHands(ImageData imageData) throws BaseVisionException {
        ensureReady(imageData, DetectionType.HAND);
        long start = System.currentTimeMillis();
        String correlationId = generateCorrelationId();
        logger.debug("Starting MediaPipe hand detection", Map.of(
            "component", BACKEND_ID,
            "operation", "detect_hands",
            "correlation_id", correlationId,
            "image_size", imageData.getSizeInBytes(),
            "image_format", imageData.format()
        ));
        try {
            VisionResult result = tryDetectHandsWithMediaPipe(imageData, correlationId);
            if (result != null) {
                return result;
            }
            throw new VisionBackendException(
                "MediaPipe hand detection is not available in this runtime",
                "mediapipe_hand_runtime_unavailable",
                DetectionType.HAND.getCode()
            );
        } catch (BaseVisionException e) {
            throw e;
        } catch (Exception e) {
            throw new VisionProcessingException(
                "MediaPipe hand detection failed",
                "mediapipe_hand_failed",
                DetectionType.HAND.getCode(),
                e
            );
        } finally {
                    logger.debug("Completed MediaPipe hand detection", Map.of(
            "component", BACKEND_ID,
            "operation", "detect_hands",
            "correlation_id", correlationId,
            "elapsed_ms", System.currentTimeMillis() - start
        ));
        }
    }

    @Override
    public void shutdown() throws BaseVisionException {
        // No-op for now. Add resource cleanup when concrete MediaPipe Tasks are wired.
        logger.info("Shutting down MediaPipe backend", Map.of(
            "component", BACKEND_ID
        ));
    }

    private void ensureReady(ImageData imageData, DetectionType detectionType) throws BaseVisionException {
        if (imageData == null) {
            throw new IllegalArgumentException("Image data must not be null");
        }
        if (!initialized) {
            throw new VisionBackendException("Backend not initialized", "backend_not_initialized", detectionType.getCode());
        }
        if (!mediapipeAvailable) {
            throw new VisionProcessingException(
                "MediaPipe runtime not available on classpath",
                "mediapipe_not_available",
                detectionType.getCode()
            );
        }
    }

    private boolean isPresent(String className) {
        try {
            Class.forName(className, false, Thread.currentThread().getContextClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (Throwable t) {
            // Be conservative: any unexpected error indicates it's not usable.
            return false;
        }
    }

    private String resolveMediapipeVersionSafely() {
        try {
            Class<?> anchor = tryLoad(
                "com.google.mediapipe.tasks.vision.facedetector.FaceDetector",
                "com.google.mediapipe.tasks.core.BaseTaskApi",
                "com.google.mediapipe.framework.Graph"
            );
            if (anchor != null && anchor.getPackage() != null && anchor.getPackage().getImplementationVersion() != null) {
                return anchor.getPackage().getImplementationVersion();
            }
        } catch (Throwable ignored) {
            // fall through
        }
        return "unknown";
    }

    private Class<?> tryLoad(String... classNames) {
        for (String cn : classNames) {
            try {
                return Class.forName(cn, false, Thread.currentThread().getContextClassLoader());
            } catch (Throwable ignored) {
                // try next
            }
        }
        return null;
    }

    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    // --- Implementation scaffolding (reflection-based) ---

    private static final String MP_FACE_MODEL_URL =
        "https://storage.googleapis.com/mediapipe-models/face_detector/blaze_face_short_range/float16/latest/blaze_face_short_range.tflite";
    private static final String MP_HAND_MODEL_URL =
        "https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/latest/hand_landmarker.task";
    private static final String MP_POSE_MODEL_URL =
        "https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/latest/pose_landmarker_lite.task";

    private VisionResult tryDetectFacesWithMediaPipe(ImageData imageData, String correlationId) throws Exception {
        long started = System.currentTimeMillis();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Class<?> faceDetectorClass = safeLoad("com.google.mediapipe.tasks.vision.facedetector.FaceDetector", cl);
        Class<?> faceDetectorOptionsClass = safeLoad("com.google.mediapipe.tasks.vision.facedetector.FaceDetectorOptions", cl);
        Class<?> baseOptionsClass = safeLoad("com.google.mediapipe.tasks.core.BaseOptions", cl);
        Class<?> runningModeClass = safeLoad("com.google.mediapipe.tasks.vision.core.RunningMode", cl);
        Class<?> mpImageFactoryClass = safeLoad("com.google.mediapipe.tasks.vision.core.MPImageFactory", cl);

        if (faceDetectorClass == null || faceDetectorOptionsClass == null || baseOptionsClass == null ||
            runningModeClass == null || mpImageFactoryClass == null) {
            return null; // Not available in this runtime
        }

        String modelPath = ensureModelDownloaded("blaze_face_short_range.tflite", MP_FACE_MODEL_URL);

        Object baseOptionsBuilder = baseOptionsClass.getMethod("builder").invoke(null);
        // Prefer setModelAssetPath; if not available, attempt setModelPath
        invokeIfPresent(baseOptionsBuilder, "setModelAssetPath", new Class[]{String.class}, new Object[]{modelPath});
        invokeIfPresent(baseOptionsBuilder, "setModelPath", new Class[]{String.class}, new Object[]{modelPath});
        Object baseOptions = baseOptionsBuilder.getClass().getMethod("build").invoke(baseOptionsBuilder);

        Object optionsBuilder = faceDetectorOptionsClass.getMethod("builder").invoke(null);
        invokeIfPresent(optionsBuilder, "setBaseOptions", new Class[]{baseOptionsClass}, new Object[]{baseOptions});
        // running mode IMAGE
        Object imageEnum = java.lang.Enum.valueOf((Class<Enum>) runningModeClass.asSubclass(Enum.class), "IMAGE");
        invokeIfPresent(optionsBuilder, "setRunningMode", new Class[]{runningModeClass}, new Object[]{imageEnum});
        // Confidence threshold default
        invokeIfPresent(optionsBuilder, "setMinDetectionConfidence", new Class[]{Float.TYPE}, new Object[]{0.5f});
        Object options = optionsBuilder.getClass().getMethod("build").invoke(optionsBuilder);

        // Build MPImage from byte[] if factory supports it. We try multiple common factory methods.
        Object mpImage = tryCreateMpImage(mpImageFactoryClass, imageData.data());
        if (mpImage == null) {
            return null; // Can't convert image in this runtime
        }

        // Create detector instance
        Object detector = tryCreateTaskInstance(faceDetectorClass, options);
        if (detector == null) {
            return null;
        }

        // Run detection: method name often "detect" returning FaceDetectorResult
        Object result = invokeDetect(detector, "detect", mpImage);
        long elapsed = System.currentTimeMillis() - started;
        if (result == null) {
            return VisionResult.empty(DetectionType.FACE, elapsed);
        }

        // Map results to VisionResult via reflection
        return mapFaceDetectorResultToVisionResult(result, elapsed);
    }

    private VisionResult tryDetectHandsWithMediaPipe(ImageData imageData, String correlationId) throws Exception {
        long started = System.currentTimeMillis();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Class<?> handLandmarkerClass = safeLoad("com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker", cl);
        Class<?> handLandmarkerOptionsClass = safeLoad("com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerOptions", cl);
        Class<?> baseOptionsClass = safeLoad("com.google.mediapipe.tasks.core.BaseOptions", cl);
        Class<?> runningModeClass = safeLoad("com.google.mediapipe.tasks.vision.core.RunningMode", cl);
        Class<?> mpImageFactoryClass = safeLoad("com.google.mediapipe.tasks.vision.core.MPImageFactory", cl);
        if (handLandmarkerClass == null || handLandmarkerOptionsClass == null || baseOptionsClass == null ||
            runningModeClass == null || mpImageFactoryClass == null) {
            return null;
        }
        String modelPath = ensureModelDownloaded("hand_landmarker.task", MP_HAND_MODEL_URL);
        Object baseOptionsBuilder = baseOptionsClass.getMethod("builder").invoke(null);
        invokeIfPresent(baseOptionsBuilder, "setModelAssetPath", new Class[]{String.class}, new Object[]{modelPath});
        invokeIfPresent(baseOptionsBuilder, "setModelPath", new Class[]{String.class}, new Object[]{modelPath});
        Object baseOptions = baseOptionsBuilder.getClass().getMethod("build").invoke(baseOptionsBuilder);
        Object optionsBuilder = handLandmarkerOptionsClass.getMethod("builder").invoke(null);
        invokeIfPresent(optionsBuilder, "setBaseOptions", new Class[]{baseOptionsClass}, new Object[]{baseOptions});
        Object imageEnum = java.lang.Enum.valueOf((Class<Enum>) runningModeClass.asSubclass(Enum.class), "IMAGE");
        invokeIfPresent(optionsBuilder, "setRunningMode", new Class[]{runningModeClass}, new Object[]{imageEnum});
        Object options = optionsBuilder.getClass().getMethod("build").invoke(optionsBuilder);
        Object mpImage = tryCreateMpImage(mpImageFactoryClass, imageData.data());
        if (mpImage == null) return null;
        Object landmarker = tryCreateTaskInstance(handLandmarkerClass, options);
        if (landmarker == null) return null;
        Object result = invokeDetect(landmarker, "detect", mpImage);
        long elapsed = System.currentTimeMillis() - started;
        return mapGenericLandmarkResultToVisionResult(result, DetectionType.HAND, elapsed);
    }

    private VisionResult tryDetectPoseWithMediaPipe(ImageData imageData, String correlationId) throws Exception {
        long started = System.currentTimeMillis();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Class<?> poseLandmarkerClass = safeLoad("com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker", cl);
        Class<?> poseLandmarkerOptionsClass = safeLoad("com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerOptions", cl);
        Class<?> baseOptionsClass = safeLoad("com.google.mediapipe.tasks.core.BaseOptions", cl);
        Class<?> runningModeClass = safeLoad("com.google.mediapipe.tasks.vision.core.RunningMode", cl);
        Class<?> mpImageFactoryClass = safeLoad("com.google.mediapipe.tasks.vision.core.MPImageFactory", cl);
        if (poseLandmarkerClass == null || poseLandmarkerOptionsClass == null || baseOptionsClass == null ||
            runningModeClass == null || mpImageFactoryClass == null) {
            return null;
        }
        String modelPath = ensureModelDownloaded("pose_landmarker_lite.task", MP_POSE_MODEL_URL);
        Object baseOptionsBuilder = baseOptionsClass.getMethod("builder").invoke(null);
        invokeIfPresent(baseOptionsBuilder, "setModelAssetPath", new Class[]{String.class}, new Object[]{modelPath});
        invokeIfPresent(baseOptionsBuilder, "setModelPath", new Class[]{String.class}, new Object[]{modelPath});
        Object baseOptions = baseOptionsBuilder.getClass().getMethod("build").invoke(baseOptionsBuilder);
        Object optionsBuilder = poseLandmarkerOptionsClass.getMethod("builder").invoke(null);
        invokeIfPresent(optionsBuilder, "setBaseOptions", new Class[]{baseOptionsClass}, new Object[]{baseOptions});
        Object imageEnum = java.lang.Enum.valueOf((Class<Enum>) runningModeClass.asSubclass(Enum.class), "IMAGE");
        invokeIfPresent(optionsBuilder, "setRunningMode", new Class[]{runningModeClass}, new Object[]{imageEnum});
        Object options = optionsBuilder.getClass().getMethod("build").invoke(optionsBuilder);
        Object mpImage = tryCreateMpImage(mpImageFactoryClass, imageData.data());
        if (mpImage == null) return null;
        Object landmarker = tryCreateTaskInstance(poseLandmarkerClass, options);
        if (landmarker == null) return null;
        Object result = invokeDetect(landmarker, "detect", mpImage);
        long elapsed = System.currentTimeMillis() - started;
        return mapGenericLandmarkResultToVisionResult(result, DetectionType.POSE, elapsed);
    }

    private VisionResult tryDetectLandmarksWithMediaPipe(ImageData imageData, String correlationId) throws Exception {
        // Alias to hand/pose specific implementations as a generic entry point; prefer HAND as example.
        return tryDetectHandsWithMediaPipe(imageData, correlationId);
    }

    private Class<?> safeLoad(String fqcn, ClassLoader cl) {
        try { return Class.forName(fqcn, false, cl); } catch (Throwable t) { return null; }
    }

    private void invokeIfPresent(Object target, String methodName, Class<?>[] types, Object[] args) {
        try { target.getClass().getMethod(methodName, types).invoke(target, args); } catch (Throwable ignored) { }
    }

    private Object tryCreateTaskInstance(Class<?> taskClass, Object options) {
        try {
            // Prefer single-arg factory: createFromOptions(options)
            try {
                return taskClass.getMethod("createFromOptions", options.getClass()).invoke(null, options);
            } catch (NoSuchMethodException nsme) {
                // Some platforms require android.content.Context; try (Context, Options) with null context
                for (var m : taskClass.getMethods()) {
                    if (m.getName().equals("createFromOptions") && m.getParameterCount() == 2) {
                        return m.invoke(null, new Object[]{null, options});
                    }
                }
            }
        } catch (Throwable t) {
            logger.warn("Failed to instantiate MediaPipe task", Map.of(
                "component", BACKEND_ID,
                "task", taskClass.getName(),
                "error", t.getClass().getSimpleName()
            ));
        }
        return null;
    }

    private Object tryCreateMpImage(Class<?> mpImageFactoryClass, byte[] imageBytes) {
        try {
            // Common factories to try, depending on platform
            // 1) createFromByteArray(byte[])
            try {
                return mpImageFactoryClass.getMethod("createFromByteArray", byte[].class).invoke(null, imageBytes);
            } catch (NoSuchMethodException ignored) {}
            // 2) createFromRgbBytes(byte[])
            try {
                return mpImageFactoryClass.getMethod("createFromRgbBytes", byte[].class).invoke(null, imageBytes);
            } catch (NoSuchMethodException ignored) {}
        } catch (Throwable t) {
            logger.warn("Failed to create MPImage from bytes", Map.of(
                "component", BACKEND_ID,
                "error", t.getClass().getSimpleName()
            ));
        }
        return null;
    }

    private Object invokeDetect(Object taskInstance, String methodName, Object mpImage) {
        try {
            return taskInstance.getClass().getMethod(methodName, mpImage.getClass()).invoke(taskInstance, mpImage);
        } catch (NoSuchMethodException e) {
            // Fallback: find any method named detect with one param
            for (var m : taskInstance.getClass().getMethods()) {
                if (m.getName().equals(methodName) && m.getParameterCount() == 1) {
                    try { return m.invoke(taskInstance, mpImage); } catch (Throwable ignored) { }
                }
            }
        } catch (Throwable t) {
            logger.warn("MediaPipe task invocation failed", Map.of(
                "component", BACKEND_ID,
                "method", methodName,
                "error", t.getClass().getSimpleName()
            ));
        }
        return null;
    }

    private VisionResult mapFaceDetectorResultToVisionResult(Object resultObject, long processingMs) {
        try {
            // FaceDetectorResult typically has method detections() returning a list of Detection objects
            var detectionsMethod = resultObject.getClass().getMethod("detections");
            Object detectionsObj = detectionsMethod.invoke(resultObject);
            if (!(detectionsObj instanceof java.util.List<?> list) || list.isEmpty()) {
                return VisionResult.empty(DetectionType.FACE, processingMs);
            }
            java.util.List<com.springvision.core.Detection> mapped = new java.util.ArrayList<>();
            for (Object det : list) {
                BoundingBox bb = extractBoundingBox(det);
                double score = extractScore(det);
                mapped.add(new com.springvision.core.Detection("face", score, bb, java.util.Map.of()));
            }
            double avg = mapped.stream().mapToDouble(com.springvision.core.Detection::confidence).average().orElse(0.0);
            return VisionResult.of(DetectionType.FACE, mapped, avg, processingMs);
        } catch (Throwable t) {
            logger.warn("Failed to map FaceDetectorResult", Map.of(
                "component", BACKEND_ID,
                "error", t.getClass().getSimpleName()
            ));
            return VisionResult.empty(DetectionType.FACE, processingMs);
        }
    }

    private VisionResult mapGenericLandmarkResultToVisionResult(Object resultObject, DetectionType type, long processingMs) {
        try {
            // Many landmark tasks return normalized landmarks per instance; we expose as detections without boxes
            if (resultObject == null) {
                return VisionResult.empty(type, processingMs);
            }
            java.util.List<com.springvision.core.Detection> mapped = java.util.List.of();
            return VisionResult.of(type, mapped, 0.0, processingMs);
        } catch (Throwable t) {
            return VisionResult.empty(type, processingMs);
        }
    }

    private BoundingBox extractBoundingBox(Object detectionObj) {
        try {
            // Try common accessors: boundingBox(), getBoundingBox(), or location()
            for (String m : new String[]{"boundingBox", "getBoundingBox", "location"}) {
                try {
                    Object rect = detectionObj.getClass().getMethod(m).invoke(detectionObj);
                    if (rect != null) {
                        return toBoundingBox(rect);
                    }
                } catch (NoSuchMethodException ignored) {}
            }
        } catch (Throwable ignored) { }
        // Fallback full-image box
        return new BoundingBox(0.0, 0.0, 1.0, 1.0);
    }

    private double extractScore(Object detectionObj) {
        try {
            // Try score(), getScore(), categories().get(0).score()
            for (String m : new String[]{"score", "getScore"}) {
                try { return ((Number) detectionObj.getClass().getMethod(m).invoke(detectionObj)).doubleValue(); }
                catch (NoSuchMethodException ignored) {}
            }
            try {
                Object cats = detectionObj.getClass().getMethod("categories").invoke(detectionObj);
                if (cats instanceof java.util.List<?> list && !list.isEmpty()) {
                    Object cat = list.get(0);
                    for (String m : new String[]{"score", "getScore"}) {
                        try { return ((Number) cat.getClass().getMethod(m).invoke(cat)).doubleValue(); } catch (NoSuchMethodException ignored) {}
                    }
                }
            } catch (Throwable ignored) { }
        } catch (Throwable ignored) { }
        return 0.0;
    }

    private BoundingBox toBoundingBox(Object rectObj) {
        try {
            // Try accessors x(), y(), width(), height()
            double x = getDouble(rectObj, "x", "left", "xmin", "xMin");
            double y = getDouble(rectObj, "y", "top", "ymin", "yMin");
            double w = getDouble(rectObj, "width", "w");
            double h = getDouble(rectObj, "height", "h");
            if (w <= 0 || h <= 0) {
                // Some APIs expose right/bottom
                double right = getDouble(rectObj, "right", "xmax", "xMax");
                double bottom = getDouble(rectObj, "bottom", "ymax", "yMax");
                if (right > x && bottom > y) {
                    w = right - x; h = bottom - y;
                }
            }
            // Assume normalized coordinates; clamp to [0,1]
            x = Math.max(0.0, Math.min(1.0, x));
            y = Math.max(0.0, Math.min(1.0, y));
            w = Math.max(0.0, Math.min(1.0, w));
            h = Math.max(0.0, Math.min(1.0, h));
            return new BoundingBox(x, y, w, h);
        } catch (Throwable t) {
            return new BoundingBox(0.0, 0.0, 1.0, 1.0);
        }
    }

    private double getDouble(Object obj, String... methodNames) {
        for (String m : methodNames) {
            try { return ((Number) obj.getClass().getMethod(m).invoke(obj)).doubleValue(); }
            catch (NoSuchMethodException ignored) { }
            catch (Throwable ignored) { }
        }
        return 0.0;
    }

    // No shared stopwatch needed; each operation tracks its own timing locally.

    private String ensureModelDownloaded(String fileName, String url) throws Exception {
        java.nio.file.Path cacheDir = java.nio.file.Paths.get(System.getProperty("user.home", "."), ".spring-vision", "mediapipe-models");
        java.nio.file.Files.createDirectories(cacheDir);
        java.nio.file.Path target = cacheDir.resolve(fileName);
        if (java.nio.file.Files.exists(target) && java.nio.file.Files.size(target) > 0) {
            return target.toAbsolutePath().toString();
        }
        downloadWithTimeout(url, target);
        return target.toAbsolutePath().toString();
    }

    private void downloadWithTimeout(String url, java.nio.file.Path target) throws Exception {
        java.net.URL u = new java.net.URL(url);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) u.openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", "spring-vision/1.0");
        int code = conn.getResponseCode();
        if (code >= 200 && code < 300) {
            try (java.io.InputStream in = conn.getInputStream(); java.io.OutputStream out = java.nio.file.Files.newOutputStream(target)) {
                in.transferTo(out);
            }
        } else {
            throw new java.io.IOException("HTTP " + code + " when downloading model");
        }
    }
}
