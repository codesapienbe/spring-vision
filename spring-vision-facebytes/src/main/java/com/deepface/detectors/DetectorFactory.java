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

    /** Returns the default detector backend instance (RetinaFace if available; falls back internally when missing). */
    public static FaceDetector createDefault() {
        return getRetinaFace();
    }

    /** Returns a detector instance for the requested backend, reusing singletons. */
    public static FaceDetector create(DetectorBackend backend) {
        if (backend == null) return createDefault();
        switch (backend) {
            case OPENCV:
                return getOpenCv();
            case RETINAFACE:
                return getRetinaFace();
            case DLIB:
            case MTCNN:
            default:
                throw new UnsupportedOperationException("Detector backend not implemented: " + backend);
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
                    inst = new RetinaFaceDetector();
                    RETINAFACE_SINGLETON = inst;
                }
            }
        }
        return inst;
    }
}
