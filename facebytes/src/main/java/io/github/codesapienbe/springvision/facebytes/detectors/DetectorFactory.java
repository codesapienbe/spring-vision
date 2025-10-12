package io.github.codesapienbe.springvision.facebytes.detectors;

import io.github.codesapienbe.springvision.facebytes.enums.DetectorBackend;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for face detectors with singleton caching to avoid repeated native/ONNX initializations.
 */
public final class DetectorFactory {

    private DetectorFactory() {
    }

    // Memory-friendly cache using weak references so detectors can be GC'd under memory pressure
    private static final Map<DetectorBackend, WeakReference<FaceDetector>> detectorCache = new ConcurrentHashMap<>();

    /**
     * Returns the default detector backend instance (RetinaFace if available; falls back internally when missing).
     * @return the default face detector instance
     */
    public static FaceDetector createDefault() {
        try {
            return getRetinaFace();
        } catch (Exception e) {
            // If all advanced detectors fail, fall back to OpenCV
            System.err.println("Default detector failed, using OpenCV fallback: " + e.getMessage());
            return getOpenCv();
        }
    }

    /**
     * Returns a detector instance for the requested backend, reusing cached instances when available.
     * @param backend the requested detector backend
     * @return a face detector instance for the specified backend
     */
    public static FaceDetector create(DetectorBackend backend) {
        if (backend == null) return createDefault();
        try {
            switch (backend) {
                case OPENCV:
                    return getOpenCv();
                case RETINAFACE:
                    return getRetinaFace();
                case DLIB:
                    return getDlib();
                case MTCNN:
                    return getMtcnn();
                default:
                    throw new io.github.codesapienbe.springvision.core.exception.VisionUnsupportedException(
                        "Unsupported or unknown detector backend: " + backend,
                        "create", backend == null ? null : backend.name());
            }
        } catch (Exception e) {
            // If the requested detector fails, fall back to OpenCV
            System.err.println("Requested detector " + backend + " failed, using OpenCV fallback: " + e.getMessage());
            return getOpenCv();
        }
    }

    @SuppressWarnings("unchecked")
    private static OpenCVDetector getOpenCv() {
        DetectorBackend key = DetectorBackend.OPENCV;
        FaceDetector inst = deref(key);
        if (inst == null) {
            synchronized (DetectorFactory.class) {
                inst = deref(key);
                if (inst == null) {
                    inst = new OpenCVDetector();
                    detectorCache.put(key, new WeakReference<>(inst));
                }
            }
        }
        return (OpenCVDetector) inst;
    }

    @SuppressWarnings("unchecked")
    private static RetinaFaceDetector getRetinaFace() {
        DetectorBackend key = DetectorBackend.RETINAFACE;
        FaceDetector inst = deref(key);
        if (inst == null) {
            synchronized (DetectorFactory.class) {
                inst = deref(key);
                if (inst == null) {
                    try {
                        inst = new RetinaFaceDetector();
                        detectorCache.put(key, new WeakReference<>(inst));
                    } catch (Exception e) {
                        // Log the error and throw exception - can't return different type
                        System.err.println("RetinaFace detector initialization failed: " + e.getMessage());
                        throw new RuntimeException("RetinaFace detector initialization failed", e);
                    }
                }
            }
        }
        return (RetinaFaceDetector) inst;
    }

    @SuppressWarnings("unchecked")
    private static DlibDetector getDlib() {
        DetectorBackend key = DetectorBackend.DLIB;
        FaceDetector inst = deref(key);
        if (inst == null) {
            synchronized (DetectorFactory.class) {
                inst = deref(key);
                if (inst == null) {
                    try {
                        inst = new DlibDetector();
                        detectorCache.put(key, new WeakReference<>(inst));
                    } catch (Exception e) {
                        // Log the error and throw exception - can't return different type
                        System.err.println("Dlib detector initialization failed: " + e.getMessage());
                        throw new RuntimeException("Dlib detector initialization failed", e);
                    }
                }
            }
        }
        return (DlibDetector) inst;
    }

    @SuppressWarnings("unchecked")
    private static MtcnnDetector getMtcnn() {
        DetectorBackend key = DetectorBackend.MTCNN;
        FaceDetector inst = deref(key);
        if (inst == null) {
            synchronized (DetectorFactory.class) {
                inst = deref(key);
                if (inst == null) {
                    try {
                        inst = new MtcnnDetector();
                        detectorCache.put(key, new WeakReference<>(inst));
                    } catch (Exception e) {
                        // Log the error and throw exception - can't return different type
                        System.err.println("MTCNN detector initialization failed: " + e.getMessage());
                        throw new RuntimeException("MTCNN detector initialization failed", e);
                    }
                }
            }
        }
        return (MtcnnDetector) inst;
    }

    /**
     * Clears the detector cache.
     * Should be called during test cleanup or when memory is low.
     */
    public static void clearCache() {
        detectorCache.clear();
        // Hint the GC; callers should not rely on immediate GC but this helps under tests
        System.gc();
        // Note: System.runFinalization() was removed as it's deprecated in Java 18+
        // and was never guaranteed to actually run finalizers anyway.
        // Modern Java applications should avoid relying on finalization.
    }

    /**
     * Gets the current cache size for monitoring. Cleans up cleared weak references first.
     *
     * @return the number of cached detectors
     */
    public static int getCacheSize() {
        // Remove entries whose weak references have been cleared
        detectorCache.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().get() == null);
        return detectorCache.size();
    }

    /**
     * Helper to dereference a detector from the cache.
     */
    private static FaceDetector deref(DetectorBackend key) {
        WeakReference<FaceDetector> ref = detectorCache.get(key);
        return ref == null ? null : ref.get();
    }
}
