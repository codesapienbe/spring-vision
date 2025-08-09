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
public class MediaPipeVisionBackend implements VisionBackend {

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
        // Placeholder: real MediaPipe Face Detector integration to be implemented incrementally.
        throw new VisionBackendException(
            "Face detection not implemented for MediaPipe backend",
            "mediapipe_not_implemented",
            DetectionType.FACE.getCode()
        );
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
        throw new VisionBackendException(
            "Landmark detection not implemented for MediaPipe backend",
            "mediapipe_not_implemented",
            DetectionType.LANDMARK.getCode()
        );
    }

    @Override
    public VisionResult detectPoses(ImageData imageData) throws BaseVisionException {
        ensureReady(imageData, DetectionType.POSE);
        throw new VisionBackendException(
            "Pose detection not implemented for MediaPipe backend",
            "mediapipe_not_implemented",
            DetectionType.POSE.getCode()
        );
    }

    @Override
    public VisionResult detectHands(ImageData imageData) throws BaseVisionException {
        ensureReady(imageData, DetectionType.HAND);
        throw new VisionBackendException(
            "Hand detection not implemented for MediaPipe backend",
            "mediapipe_not_implemented",
            DetectionType.HAND.getCode()
        );
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
}
