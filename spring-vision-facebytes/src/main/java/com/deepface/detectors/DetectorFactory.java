package com.deepface.detectors;

import com.deepface.enums.DetectorBackend;

/**
 * Factory for face detectors with singleton caching to avoid repeated native/ONNX initializations.
 */
public final class DetectorFactory {

    private DetectorFactory() {}

    // Cached singletons to stabilize ONNX/Native resource usage
    private static volatile OpenCVDetector OPENCV_SINGLETON;
    private static volatile RetinaFaceDetector RETINAFACE_SINGLETON;
    private static volatile DlibDetector DLIB_SINGLETON;
    private static volatile MtcnnDetector MTCNN_SINGLETON;

    /** Returns the default detector backend instance (RetinaFace if available; falls back internally when missing). */
    public static FaceDetector createDefault() {
        try {
            return getRetinaFace();
        } catch (Exception e) {
            // If all advanced detectors fail, fall back to OpenCV
            System.err.println("Default detector failed, using OpenCV fallback: " + e.getMessage());
            return getOpenCv();
        }
    }

    /** Returns a detector instance for the requested backend, reusing singletons. */
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
                    throw new UnsupportedOperationException("Detector backend not implemented: " + backend);
            }
        } catch (Exception e) {
            // If the requested detector fails, fall back to OpenCV
            System.err.println("Requested detector " + backend + " failed, using OpenCV fallback: " + e.getMessage());
            return getOpenCv();
        }
    }

    private static OpenCVDetector getOpenCv() {
        OpenCVDetector inst = OPENCV_SINGLETON;
        if (inst == null) {
            synchronized (DetectorFactory.class) {
                inst = OPENCV_SINGLETON;
                if (inst == null) {
                    inst = new OpenCVDetector();
                    OPENCV_SINGLETON = inst;
                }
            }
        }
        return inst;
    }

    private static RetinaFaceDetector getRetinaFace() {
        RetinaFaceDetector inst = RETINAFACE_SINGLETON;
        if (inst == null) {
            synchronized (DetectorFactory.class) {
                inst = RETINAFACE_SINGLETON;
                if (inst == null) {
                    try {
                        inst = new RetinaFaceDetector();
                        RETINAFACE_SINGLETON = inst;
                    } catch (Exception e) {
                        // Log the error and throw exception - can't return different type
                        System.err.println("RetinaFace detector initialization failed: " + e.getMessage());
                        throw new RuntimeException("RetinaFace detector initialization failed", e);
                    }
                }
            }
        }
        return inst;
    }

    private static DlibDetector getDlib() {
        DlibDetector inst = DLIB_SINGLETON;
        if (inst == null) {
            synchronized (DetectorFactory.class) {
                inst = DLIB_SINGLETON;
                if (inst == null) {
                    try {
                        inst = new DlibDetector();
                        DLIB_SINGLETON = inst;
                    } catch (Exception e) {
                        // Log the error and throw exception - can't return different type
                        System.err.println("Dlib detector initialization failed: " + e.getMessage());
                        throw new RuntimeException("Dlib detector initialization failed", e);
                    }
                }
            }
        }
        return inst;
    }

    private static MtcnnDetector getMtcnn() {
        MtcnnDetector inst = MTCNN_SINGLETON;
        if (inst == null) {
            synchronized (DetectorFactory.class) {
                inst = MTCNN_SINGLETON;
                if (inst == null) {
                    try {
                        inst = new MtcnnDetector();
                        MTCNN_SINGLETON = inst;
                    } catch (Exception e) {
                        // Log the error and throw exception - can't return different type
                        System.err.println("MTCNN detector initialization failed: " + e.getMessage());
                        throw new RuntimeException("MTCNN detector initialization failed", e);
                    }
                }
            }
        }
        return inst;
    }
}
