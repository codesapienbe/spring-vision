package io.github.codesapienbe.springvision.facebytes.enums;

/**
 * Enumerates the supported face detector backends that can be used to locate faces in an image before recognition or analysis.
 * Different backends offer trade-offs between speed, accuracy, and dependency complexity.
 */
public enum DetectorBackend {
    /**
     * OpenCV's Haar Cascade or DNN-based face detectors. Generally fast and widely available.
     */
    OPENCV,
    /**
     * Dlib's HOG-based or CNN-based face detectors. Known for high accuracy, especially the CNN model.
     */
    DLIB,
    /**
     * Multi-task Cascaded Convolutional Networks (MTCNN), a popular and accurate deep learning-based detector that also provides facial landmarks.
     */
    MTCNN,
    /**
     * RetinaFace, a state-of-the-art deep learning model for face detection that performs well on faces of various scales and in crowded scenes.
     */
    RETINAFACE
}
