package com.deepface.detectors;

import com.deepface.enums.DetectorBackend;

public final class DetectorFactory {

    private DetectorFactory() {}

    public static FaceDetector createDefault() {
        return new RetinaFaceDetector();
    }

    public static FaceDetector create(DetectorBackend backend) {
        if (backend == null) return createDefault();
        switch (backend) {
            case OPENCV:
                return new OpenCVDetector();
            case RETINAFACE:
                return new RetinaFaceDetector();
            case DLIB:
            case MTCNN:
            default:
                throw new UnsupportedOperationException("Detector backend not implemented: " + backend);
        }
    }
}
