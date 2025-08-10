package com.deepface.detectors;

import com.deepface.enums.DetectorBackend;

public final class DetectorFactory {

    private DetectorFactory() {}

    public static FaceDetector createDefault() {
        return new OpenCVDetector();
    }

    public static FaceDetector create(DetectorBackend backend) {
        if (backend == null) return createDefault();
        return switch (backend) {
            case OPENCV -> new OpenCVDetector();
            case DLIB, MTCNN, RETINAFACE -> throw new UnsupportedOperationException(
                "Detector backend not implemented: " + backend);
        };
    }
}
